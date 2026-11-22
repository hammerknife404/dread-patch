package io.github.hammerknife404.dreadpatch.entity;

import com.iafenvoy.iceandfire.registry.IafItems;
import com.iafenvoy.iceandfire.registry.IafSounds;
import io.github.hammerknife404.dreadpatch.block.DreadGateBlock;
import io.github.hammerknife404.dreadpatch.magic.GateShape;
import io.github.hammerknife404.dreadpatch.magic.GateTiming;
import io.github.hammerknife404.dreadpatch.registry.ModRegistries;
import io.github.hammerknife404.dreadpatch.world.Dreadlands;
import io.github.hammerknife404.dreadpatch.world.DreadlandsData;
import io.redspace.ironsspellbooks.api.util.Utils;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.Difficulty;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.EventHooks;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

/**
 * A Dread Gate's ritual. A Gravemaster's Key (Ice and Fire's {@code dread_key}, which also opens a
 * Mausoleum's sealed door) used on a dreadstone frame round an opening ({@link GateShape}: 2-5 wide,
 * 3-7 tall; the four arches of a Mausoleum's ice forge shrine are ready-made) is used up, and the
 * opening fills with the gate ({@link DreadGateBlock}). For {@link GateTiming#RITUAL_TICKS} it pours
 * cold dread fire and snow; then the Dread Queen comes through and the gate shatters, its frame left
 * for another key. She comes as the boss: riding an ancient Syndrigossa where there's room for one in
 * front of the gate (on the opener's side, else the other), otherwise on foot, rising out of the
 * ground in the gate itself (in a Mausoleum, where the shrine's hall is too low for her dragon).
 * Breaking the frame meanwhile collapses the gate, and nothing comes. Saved, so a ritual outlasts a
 * restart. Summoned by hand (the smoke test), it finds the gate round where it stands.
 * <p>
 * The way to the Dreadlands (0.46.0): a player who steps into the gate during its ritual goes through
 * to the Dreadlands ({@link Dreadlands#enter}), and the queen doesn't come. The gate then stays open
 * for {@link GateTiming#openTicks} for anyone to follow (to the same place), fading in and out faster
 * and faster over its last {@link GateTiming#FADE_TICKS} ({@link GateTiming#shown}), and closes. A gate
 * opened in the Dreadlands is the way home: it opens at once, for as long, and takes whoever steps in
 * back to where they came in ({@link Dreadlands#goHome}).
 */
public class DreadGate extends Entity {
    /** The ritual's length: 10 s (5 s before 0.46.0). */
    public static final int RITUAL_TICKS = GateTiming.RITUAL_TICKS;
    /** How far either side of the gate's plane counts as stepping in. */
    static final double THRESHOLD = 0.3;
    /** Room left between her dragon's flank and the gate. */
    static final double DRAGON_CLEARANCE = 1.5;

    @Nullable
    private Direction.Axis axis;
    /** Which side of the gate its opener stood: +1 or -1 along the gate's normal. */
    private int side = 1;
    @Nullable
    private UUID activator;
    private int age;
    private boolean opened;
    private BlockPos origin = BlockPos.ZERO;
    private int width;
    private int height;
    /** Opened in the Dreadlands: the way home. */
    private boolean homeward;
    /** Ticks left of the way open; -1 until someone's stepped in (or it opened homeward). */
    private int openFor = -1;
    /** Whether its panes are up (they come and go as it fades). */
    private boolean shown = true;
    /** Where those who step in arrive in the Dreadlands, found for the first and kept for the rest. */
    @Nullable
    private Dreadlands.Arrival arrival;

    public DreadGate(EntityType<? extends DreadGate> type, Level level) {
        super(type, level);
        this.noPhysics = true;
    }

    public static void register(IEventBus gameBus) {
        gameBus.addListener(DreadGate::onUseKey);
    }

    /**
     * A Gravemaster's Key on a dreadstone frame block, at the face toward the opening: if that opening
     * is a gate, it opens and the key is spent (kept in creative). On Peaceful it opens all the same,
     * a way to the Dreadlands, but the queen won't come through (she wouldn't stay).
     */
    private static void onUseKey(PlayerInteractEvent.RightClickBlock event) {
        ItemStack key = event.getItemStack();
        Direction face = event.getFace();
        Level level = event.getLevel();
        if (face == null || !key.is(IafItems.DREAD_KEY.get()) || !level.getBlockState(event.getPos()).is(DreadGateBlock.FRAME)) {
            return;
        }
        BlockPos inside = event.getPos().relative(face);
        if (!level.getBlockState(inside).isAir()) {
            return;
        }
        Player player = event.getEntity();
        // The face clicked tells the gate's plane: a side face lies across it; a top or bottom, either way.
        Direction.Axis[] axes = face.getAxis().isHorizontal() ? new Direction.Axis[]{face.getAxis()} : new Direction.Axis[]{Direction.Axis.X, Direction.Axis.Z};
        Direction.Axis found = null;
        for (Direction.Axis axis : axes) {
            if (find(level, inside, axis) != null) {
                found = axis;
                break;
            }
        }
        if (found == null) {
            if (!level.isClientSide) {
                player.displayClientMessage(Component.translatable("message.dreadpatch.dread_gate.no_gate",
                        GateShape.MIN_WIDTH, GateShape.MAX_WIDTH, GateShape.MIN_HEIGHT, GateShape.MAX_HEIGHT), true);
            }
            return;
        }
        event.setCanceled(true);
        event.setCancellationResult(InteractionResult.sidedSuccess(level.isClientSide));
        if (level.isClientSide) {
            return;
        }
        DreadGate gate = new DreadGate(ModRegistries.DREAD_GATE.get(), level);
        gate.moveTo(inside.getX() + 0.5, inside.getY(), inside.getZ() + 0.5);
        gate.axis = found;
        double across = found == Direction.Axis.X ? player.getZ() - (inside.getZ() + 0.5) : player.getX() - (inside.getX() + 0.5);
        gate.side = across < 0.0 ? -1 : 1;
        gate.activator = player.getUUID();
        if (gate.open()) {
            level.addFreshEntity(gate);
            key.consume(1, player);
        }
    }

    /**
     * A gate home opened by the Dreadlands themselves (a spire's encounter won, 0.47.0): the gate round
     * {@code inside}, open {@code ticks} (longer than a keyed one's), its last {@link GateTiming#FADE_TICKS}
     * fading. False if there's no gate there.
     */
    public static boolean openReturn(ServerLevel level, BlockPos inside, int ticks) {
        DreadGate gate = new DreadGate(ModRegistries.DREAD_GATE.get(), level);
        gate.moveTo(inside.getX() + 0.5, inside.getY(), inside.getZ() + 0.5);
        gate.axis = Direction.Axis.X;
        if (!gate.open()) {
            return false;
        }
        gate.homeward = true;
        gate.openFor = ticks;
        return level.addFreshEntity(gate);
    }

    /** The gate round {@code start} in the plane along {@code axis}, in plane coordinates from it. */
    @Nullable
    static GateShape.Found find(Level level, BlockPos start, Direction.Axis axis) {
        BlockPos.MutableBlockPos at = new BlockPos.MutableBlockPos();
        return GateShape.find((a, y) -> {
            at.set(start.getX() + (axis == Direction.Axis.X ? a : 0), start.getY() + y, start.getZ() + (axis == Direction.Axis.Z ? a : 0));
            BlockState state = level.getBlockState(at);
            return state.isAir() ? GateShape.Cell.OPEN : state.is(DreadGateBlock.FRAME) ? GateShape.Cell.FRAME : GateShape.Cell.OTHER;
        });
    }

    /** Finds the gate round where it stands (along its axis if it has one) and fills it. False if there's none. */
    private boolean open() {
        BlockPos start = this.blockPosition();
        for (Direction.Axis axis : this.axis != null ? new Direction.Axis[]{this.axis} : new Direction.Axis[]{Direction.Axis.X, Direction.Axis.Z}) {
            GateShape.Found found = find(this.level(), start, axis);
            if (found != null) {
                this.axis = axis;
                this.origin = start.offset(axis == Direction.Axis.X ? found.left() : 0, found.bottom(), axis == Direction.Axis.Z ? found.left() : 0);
                this.width = found.width();
                this.height = found.height();
                BlockState pane = ModRegistries.DREAD_GATE_BLOCK.get().defaultBlockState().setValue(DreadGateBlock.AXIS, axis);
                // As a Nether portal's: no shape updates while it fills, or the first panes would see open air and fall.
                this.forEachPane(pos -> this.level().setBlock(pos, pane, Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE));
                Vec3 center = this.center();
                this.moveTo(center.x, this.origin.getY(), center.z);
                this.opened = true;
                if (Dreadlands.is(this.level())) {
                    // The way home: open at once, no ritual, no queen.
                    this.homeward = true;
                    this.openFor = GateTiming.openTicks(this.random.nextDouble());
                }
                this.level().playSound(null, center.x, center.y, center.z, IafSounds.DREAD_LICH_SUMMON.get(), SoundSource.HOSTILE, 2.0F, 0.6F);
                return true;
            }
        }
        return false;
    }

    @Override
    public void tick() {
        super.tick();
        if (!(this.level() instanceof ServerLevel level)) {
            return;
        }
        if (!this.opened && !this.open()) {
            this.discard();
            return;
        }
        if (!this.intact()) {
            this.shatter(level, false);
            return;
        }
        this.age++;
        Vec3 center = this.center();
        if (this.shown && this.age % 2 == 0) {
            // Cold fire and snow pour out of both faces, harder as it goes on.
            Vec3 normal = this.normal();
            int count = 2 + this.age * 6 / RITUAL_TICKS;
            for (int i = 0; i < count; i++) {
                Vec3 at = this.randomPane(level);
                double out = (this.random.nextBoolean() ? 1 : -1) * (0.05 + this.random.nextDouble() * 0.1);
                level.sendParticles(this.random.nextInt(4) == 0 ? ParticleTypes.SNOWFLAKE : ModRegistries.RIME_FLAME.get(),
                        at.x, at.y, at.z, 0, normal.x * out, 0.02, normal.z * out, 1.0);
            }
        }
        if (this.shown && this.age % 40 == 20) {
            level.playSound(null, center.x, center.y, center.z, SoundEvents.PORTAL_AMBIENT, SoundSource.HOSTILE, 1.5F, 0.5F);
        }
        if (this.shown) {
            // Anyone stepping in goes through; during the ritual, that opens the way and the queen doesn't come.
            this.passThrough(level);
        }
        if (this.openFor >= 0) {
            this.openFor--;
            boolean show = GateTiming.shown(this.openFor);
            if (show != this.shown) {
                this.setShown(level, show);
            }
            if (this.openFor <= 0) {
                this.shatter(level, false);
            }
            return;
        }
        if (this.age >= RITUAL_TICKS) {
            if (level.getDifficulty() != Difficulty.PEACEFUL) {
                this.summon(level);
            } else if (this.activator != null && level.getPlayerByUUID(this.activator) instanceof Player opener) {
                opener.displayClientMessage(Component.translatable("message.dreadpatch.dread_gate.peaceful"), true);
            }
            this.shatter(level, true);
        }
    }

    /** Players in the gate's plane go through: into the Dreadlands, or home from them. */
    private void passThrough(ServerLevel level) {
        for (ServerPlayer player : level.getEntitiesOfClass(ServerPlayer.class, this.plane(), p -> p.isAlive() && !p.isSpectator())) {
            if (this.homeward) {
                this.keepOpen(level);
                Dreadlands.goHome(player);
                continue;
            }
            if (this.arrival == null) {
                this.arrival = Dreadlands.arrival(level.getServer(), this.blockPosition(), this.random);
            }
            if (this.arrival == null) {
                return;
            }
            this.keepOpen(level);
            Dreadlands.enter(player, this.arrival, this.cameFrom(player));
        }
    }

    /** The first through holds the way open for 15-30 s more. */
    private void keepOpen(ServerLevel level) {
        if (this.openFor < 0) {
            this.openFor = GateTiming.openTicks(this.random.nextDouble());
            Vec3 center = this.center();
            level.playSound(null, center.x, center.y, center.z, SoundEvents.PORTAL_TRIGGER, SoundSource.HOSTILE, 1.0F, 0.6F);
        }
    }

    /** Where {@code player} stepped in from: in front of the gate on the side they came from, facing away from it. */
    private DreadlandsData.Origin cameFrom(ServerPlayer player) {
        Vec3 normal = this.normal();
        Vec3 center = this.center();
        double across = this.axis == Direction.Axis.X ? player.zo - center.z : player.xo - center.x;
        int side = across < 0.0 ? -1 : 1;
        Vec3 out = normal.scale(side);
        Vec3 pos = new Vec3(center.x, this.origin.getY(), center.z).add(out.scale(1.5));
        float yaw = (float) (Mth.atan2(-out.x, out.z) * Mth.RAD_TO_DEG);
        return new DreadlandsData.Origin(this.level().dimension(), pos, yaw);
    }

    /** The slab of air in its opening that counts as stepping into it. */
    private AABB plane() {
        boolean alongX = this.axis == Direction.Axis.X;
        Vec3 center = this.center();
        return alongX
                ? new AABB(this.origin.getX(), this.origin.getY(), center.z - THRESHOLD, this.origin.getX() + this.width, this.origin.getY() + this.height, center.z + THRESHOLD)
                : new AABB(center.x - THRESHOLD, this.origin.getY(), this.origin.getZ(), center.x + THRESHOLD, this.origin.getY() + this.height, this.origin.getZ() + this.width);
    }

    /** Its panes up or down, fading; quietly, as it filled. */
    private void setShown(ServerLevel level, boolean show) {
        BlockState state = show ? ModRegistries.DREAD_GATE_BLOCK.get().defaultBlockState().setValue(DreadGateBlock.AXIS, this.axis) : Blocks.AIR.defaultBlockState();
        this.forEachPane(pos -> level.setBlock(pos, state, Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE));
        this.shown = show;
        if (!show) {
            Vec3 center = this.center();
            level.sendParticles(ModRegistries.RIME_FLAME_SMALL.get(), center.x, center.y, center.z, 8, this.width * 0.25, this.height * 0.25, 0.1, 0.01);
        }
    }

    /**
     * The Dread Queen, as the boss: on her dragon where an ancient Syndrigossa fits in front of the
     * gate, else on foot out of the gate itself. She turns on whoever opened it.
     */
    private void summon(ServerLevel level) {
        DreadQueen queen = ModRegistries.DREAD_QUEEN.get().create(level);
        if (queen == null) {
            return;
        }
        Vec3 base = new Vec3(this.center().x, this.origin.getY(), this.center().z);
        Vec3 normal = this.normal();
        Vec3 mounted = this.dragonRoom(level, base, normal);
        Vec3 at = mounted != null ? mounted : base;
        Vec3 facing = mounted != null ? mounted.subtract(base) : normal.scale(this.side);
        float yaw = (float) (Mth.atan2(-facing.x, facing.z) * Mth.RAD_TO_DEG);
        queen.moveTo(at.x, at.y, at.z, yaw, 0.0F);
        queen.setYHeadRot(yaw);
        queen.yBodyRot = yaw;
        // As the world spawns the boss: she'd rise on her dragon (DragonAge.spawnsAncient). On foot if there's no room.
        EventHooks.finalizeMobSpawn(queen, level, level.getCurrentDifficultyAt(BlockPos.containing(at)), MobSpawnType.EVENT, null);
        queen.setWithDragon(mounted != null);
        Player opener = this.activator != null ? level.getPlayerByUUID(this.activator) : null;
        if (opener != null && opener.isAlive() && queen.canAttack(opener)) {
            queen.setTarget(opener);
        }
        level.addFreshEntity(queen);
        LightningBolt bolt = EntityType.LIGHTNING_BOLT.create(level);
        if (bolt != null) {
            bolt.moveTo(base.x, base.y, base.z);
            bolt.setVisualOnly(true);
            level.addFreshEntity(bolt);
        }
        level.playSound(null, base.x, base.y, base.z, SoundEvents.WITHER_SPAWN, SoundSource.HOSTILE, 2.5F, 0.7F);
    }

    /**
     * Where an ancient Syndrigossa (a probe, never added) fits on the ground in front of the gate, its
     * flank {@link #DRAGON_CLEARANCE} from it: on the opener's side, else the other. Null if neither.
     */
    @Nullable
    private Vec3 dragonRoom(ServerLevel level, Vec3 base, Vec3 normal) {
        Syndrigossa probe = ModRegistries.SYNDRIGOSSA.get().create(level);
        if (probe == null) {
            return null;
        }
        probe.makeAncient();
        double out = probe.getBbWidth() / 2.0 + DRAGON_CLEARANCE;
        for (int s : new int[]{this.side, -this.side}) {
            Vec3 spot = Utils.moveToRelativeGroundLevel(level, base.add(normal.scale(s * out)), 8).add(0.0, 0.05, 0.0);
            probe.moveTo(spot.x, spot.y, spot.z, 0.0F, 0.0F);
            if (level.noCollision(probe)) {
                return spot;
            }
        }
        return null;
    }

    /** It breaks: its panes go (with a burst of ice), its frame stays. {@code done}: she came through. */
    private void shatter(ServerLevel level, boolean done) {
        BlockState ice = Blocks.PACKED_ICE.defaultBlockState();
        this.forEachPane(pos -> {
            if (level.getBlockState(pos).is(ModRegistries.DREAD_GATE_BLOCK.get())) {
                level.setBlock(pos, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
            }
            level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, ice), pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, 6, 0.3, 0.3, 0.3, 0.1);
            level.sendParticles(done ? ModRegistries.RIME_FLAME.get() : ParticleTypes.LARGE_SMOKE, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, 3, 0.3, 0.3, 0.3, 0.05);
        });
        Vec3 center = this.center();
        level.playSound(null, center.x, center.y, center.z, SoundEvents.GLASS_BREAK, SoundSource.BLOCKS, 2.0F, done ? 0.6F : 0.4F);
        this.discard();
    }

    /** Whether every pane is still there; faded out, whether its frame still stands whole round the opening. */
    private boolean intact() {
        if (!this.shown) {
            GateShape.Found frame = find(this.level(), this.origin, this.axis);
            return frame != null && frame.left() == 0 && frame.bottom() == 0 && frame.width() == this.width && frame.height() == this.height;
        }
        boolean[] whole = {true};
        this.forEachPane(pos -> whole[0] &= this.level().getBlockState(pos).is(ModRegistries.DREAD_GATE_BLOCK.get()));
        return whole[0];
    }

    private void forEachPane(java.util.function.Consumer<BlockPos> action) {
        for (int y = 0; y < this.height; y++) {
            for (int a = 0; a < this.width; a++) {
                action.accept(this.origin.offset(this.axis == Direction.Axis.X ? a : 0, y, this.axis == Direction.Axis.Z ? a : 0));
            }
        }
    }

    /** The middle of its open face. */
    private Vec3 center() {
        boolean alongX = this.axis == Direction.Axis.X;
        return new Vec3(this.origin.getX() + (alongX ? this.width / 2.0 : 0.5), this.origin.getY() + this.height / 2.0,
                this.origin.getZ() + (alongX ? 0.5 : this.width / 2.0));
    }

    /** Across the gate: a unit step out of its plane. */
    private Vec3 normal() {
        return this.axis == Direction.Axis.X ? new Vec3(0.0, 0.0, 1.0) : new Vec3(1.0, 0.0, 0.0);
    }

    private Vec3 randomPane(ServerLevel level) {
        boolean alongX = this.axis == Direction.Axis.X;
        double a = this.random.nextDouble() * this.width;
        double y = this.random.nextDouble() * this.height;
        return new Vec3(this.origin.getX() + (alongX ? a : 0.5), this.origin.getY() + y, this.origin.getZ() + (alongX ? 0.5 : a));
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
    }

    @Override
    public boolean isPickable() {
        return false;
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        String axis = tag.getString("Axis");
        this.axis = "x".equals(axis) ? Direction.Axis.X : "z".equals(axis) ? Direction.Axis.Z : null;
        this.side = tag.getInt("Side") < 0 ? -1 : 1;
        this.activator = tag.hasUUID("Activator") ? tag.getUUID("Activator") : null;
        this.age = tag.getInt("Age");
        this.opened = tag.getBoolean("Opened");
        this.origin = new BlockPos(tag.getInt("OriginX"), tag.getInt("OriginY"), tag.getInt("OriginZ"));
        this.width = tag.getInt("Width");
        this.height = tag.getInt("Height");
        this.homeward = tag.getBoolean("Homeward");
        this.openFor = tag.contains("OpenFor") ? tag.getInt("OpenFor") : -1;
        this.shown = !tag.contains("Shown") || tag.getBoolean("Shown");
        this.arrival = tag.contains("ArrivalX") ? new Dreadlands.Arrival(
                new Vec3(tag.getDouble("ArrivalX"), tag.getDouble("ArrivalY"), tag.getDouble("ArrivalZ")), tag.getFloat("ArrivalYaw")) : null;
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        if (this.axis != null) {
            tag.putString("Axis", this.axis.getName());
        }
        tag.putInt("Side", this.side);
        if (this.activator != null) {
            tag.putUUID("Activator", this.activator);
        }
        tag.putInt("Age", this.age);
        tag.putBoolean("Opened", this.opened);
        tag.putInt("OriginX", this.origin.getX());
        tag.putInt("OriginY", this.origin.getY());
        tag.putInt("OriginZ", this.origin.getZ());
        tag.putInt("Width", this.width);
        tag.putInt("Height", this.height);
        tag.putBoolean("Homeward", this.homeward);
        tag.putInt("OpenFor", this.openFor);
        tag.putBoolean("Shown", this.shown);
        if (this.arrival != null) {
            tag.putDouble("ArrivalX", this.arrival.pos().x);
            tag.putDouble("ArrivalY", this.arrival.pos().y);
            tag.putDouble("ArrivalZ", this.arrival.pos().z);
            tag.putFloat("ArrivalYaw", this.arrival.yaw());
        }
    }
}
