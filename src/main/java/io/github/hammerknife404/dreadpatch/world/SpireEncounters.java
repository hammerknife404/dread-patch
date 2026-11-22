package io.github.hammerknife404.dreadpatch.world;

import io.github.hammerknife404.dreadpatch.DreadPatchConfig;
import io.github.hammerknife404.dreadpatch.entity.DreadGate;
import io.github.hammerknife404.dreadpatch.entity.DreadQueen;
import io.github.hammerknife404.dreadpatch.entity.Syndrigossa;
import io.github.hammerknife404.dreadpatch.magic.EncounterRules;
import io.github.hammerknife404.dreadpatch.magic.SpireShape;
import io.github.hammerknife404.dreadpatch.registry.ModRegistries;
import io.redspace.ironsspellbooks.api.util.Utils;
import com.iafenvoy.iceandfire.registry.IafBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.Difficulty;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.EventHooks;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent;
import org.jetbrains.annotations.Nullable;

/**
 * The Queen and Syndrigossa's encounter atop a spire (0.47.0). The owner: "each [spire] with a chance
 * of the Queen and Syndrigossa encounter at its summit; per player, once, but resetting after a lengthy
 * time like several IRL hours", and home through "a return gate where the encounter's defeated".
 * <ul>
 * <li>Half the spires hold it ({@link SpireShape#encounter}), marked by a dais and dread torches.</li>
 * <li>A player (not spectating) on such a summit, not on Peaceful, who hasn't met this spire's encounter
 * within the server config's hours ({@link DreadPatchConfig#encounterResetMillis}), with none of its
 * queens about, calls it: she comes as the boss, riding an ancient Syndrigossa that takes wing in the
 * open air beside the summit (on foot on the summit if there's no room), and turns on them. Everyone on
 * the summit then (or while she's about) has met it.</li>
 * <li>When she falls, a Dread Gate frame rises where she fell, open home for {@link #RETURN_TICKS}.</li>
 * </ul>
 */
public final class SpireEncounters {
    /** On her: the spire she came to (its base, packed), so she's known for its and her fall opens the way home. */
    public static final String SPIRE_KEY = "dreadpatch:spire";
    static final int CHECK_INTERVAL = 20;
    /** How far round a summit a queen of its still counts as about. */
    static final double ACTIVE_RANGE = 256.0;
    /** Where she comes in: this far out from the summit's middle past its rim, and this high over it. */
    static final double ALOFT_OUT = 12.0;
    /** High: an ancient Syndrigossa taking wing sinks some 18 blocks before she holds (260 to 242, in the smoke test). */
    static final double ALOFT_UP = 16.0;
    /** The return gate (ours): open 5 minutes, time to take what she dropped (and her dragon's corpse). */
    public static final int RETURN_TICKS = 6000;

    private SpireEncounters() {
    }

    public static void register(IEventBus gameBus) {
        gameBus.addListener(SpireEncounters::onLevelTick);
        gameBus.addListener(SpireEncounters::onDeath);
    }

    private static void onLevelTick(LevelTickEvent.Post event) {
        if (!(event.getLevel() instanceof ServerLevel level) || !Dreadlands.is(level) || level.getGameTime() % CHECK_INTERVAL != 0) {
            return;
        }
        for (ServerPlayer player : level.players()) {
            if (player.isSpectator() || !player.isAlive()) {
                continue;
            }
            SpirePiece spire = spireAt(level, player.blockPosition());
            if (spire == null || !spire.shape().encounter) {
                continue;
            }
            BlockPos base = spire.base();
            SpireShape shape = spire.shape();
            int summit = base.getY() + shape.height;
            if (!shape.onSummit(player.getX() - (base.getX() + 0.5), player.getY() - summit, player.getZ() - (base.getZ() + 0.5))) {
                continue;
            }
            meet(level, spire, player);
        }
    }

    /** The spire whose piece is at {@code pos}, or null. */
    @Nullable
    static SpirePiece spireAt(ServerLevel level, BlockPos pos) {
        StructureStart start = level.structureManager().getStructureWithPieceAt(pos, Dreadlands.SPIRES);
        if (!start.isValid()) {
            return null;
        }
        for (StructurePiece piece : start.getPieces()) {
            if (piece instanceof SpirePiece spire) {
                return spire;
            }
        }
        return null;
    }

    /** {@code player} is on the summit: the encounter comes, or they're in it, or it's not yet theirs again. */
    private static void meet(ServerLevel level, SpirePiece spire, ServerPlayer player) {
        long key = spire.base().asLong();
        DreadlandsData data = DreadlandsData.get(level.getServer());
        long now = System.currentTimeMillis();
        if (queenAbout(level, spire, key)) {
            if (!EncounterRules.mayMeet(data.lastEncounter(key, player.getUUID()), now, DreadPatchConfig.encounterResetMillis())) {
                return;
            }
            // Joining a fight already under way: it's theirs too.
            data.recordEncounter(key, player.getUUID(), now);
            return;
        }
        if (level.getDifficulty() == Difficulty.PEACEFUL || !EncounterRules.mayMeet(data.lastEncounter(key, player.getUUID()), now, DreadPatchConfig.encounterResetMillis())) {
            return;
        }
        if (call(level, spire, player)) {
            data.recordEncounter(key, player.getUUID(), now);
        }
    }

    private static boolean queenAbout(ServerLevel level, SpirePiece spire, long key) {
        Vec3 summit = summit(spire);
        return !level.getEntitiesOfClass(DreadQueen.class, new AABB(summit, summit).inflate(ACTIVE_RANGE),
                queen -> queen.isAlive() && queen.getPersistentData().getLong(SPIRE_KEY) == key && queen.getPersistentData().contains(SPIRE_KEY)).isEmpty();
    }

    /** The middle of the summit's top: where one stands on it. */
    static Vec3 summit(SpirePiece spire) {
        return new Vec3(spire.base().getX() + 0.5, spire.base().getY() + spire.shape().height, spire.base().getZ() + 0.5);
    }

    /** She comes: riding an ancient Syndrigossa in the air beside the summit, else on foot upon it. */
    private static boolean call(ServerLevel level, SpirePiece spire, ServerPlayer player) {
        DreadQueen queen = ModRegistries.DREAD_QUEEN.get().create(level);
        if (queen == null) {
            return false;
        }
        Vec3 summit = summit(spire);
        Vec3 aloft = aloft(level, spire, player);
        Vec3 at = aloft != null ? aloft : summit;
        float yaw = (float) (Mth.atan2(-(summit.x - at.x), summit.z - at.z) * Mth.RAD_TO_DEG);
        queen.moveTo(at.x, at.y, at.z, yaw, 0.0F);
        queen.setYHeadRot(yaw);
        queen.yBodyRot = yaw;
        EventHooks.finalizeMobSpawn(queen, level, level.getCurrentDifficultyAt(BlockPos.containing(at)), MobSpawnType.EVENT, null);
        queen.setWithDragon(aloft != null);
        queen.getPersistentData().putLong(SPIRE_KEY, spire.base().asLong());
        if (queen.canAttack(player)) {
            queen.setTarget(player);
        }
        if (!level.addFreshEntity(queen)) {
            return false;
        }
        LightningBolt bolt = EntityType.LIGHTNING_BOLT.create(level);
        if (bolt != null) {
            bolt.moveTo(summit.x, summit.y, summit.z);
            bolt.setVisualOnly(true);
            level.addFreshEntity(bolt);
        }
        level.playSound(null, summit.x, summit.y, summit.z, SoundEvents.WITHER_SPAWN, SoundSource.HOSTILE, 3.0F, 0.7F);
        for (ServerPlayer near : level.getEntitiesOfClass(ServerPlayer.class, new AABB(summit, summit).inflate(48.0))) {
            near.displayClientMessage(Component.translatable("message.dreadpatch.dreadlands.encounter"), true);
        }
        return true;
    }

    /**
     * Open air for an ancient Syndrigossa (a probe, never added) beside the summit, out past its rim and
     * over it: the side away from {@code player} first, then round. Null if there's none.
     */
    @Nullable
    private static Vec3 aloft(ServerLevel level, SpirePiece spire, ServerPlayer player) {
        Syndrigossa probe = ModRegistries.SYNDRIGOSSA.get().create(level);
        if (probe == null) {
            return null;
        }
        probe.makeAncient();
        Vec3 summit = summit(spire);
        double out = spire.shape().topRadius + ALOFT_OUT;
        double away = Math.atan2(summit.z - player.getZ(), summit.x - player.getX());
        for (int i = 0; i < 8; i++) {
            // Straight away from them, then 45° either side, 90°, and so on round.
            double angle = away + (i % 2 == 1 ? 1 : -1) * ((i + 1) / 2) * Math.PI / 4.0;
            Vec3 spot = summit.add(Math.cos(angle) * out, ALOFT_UP, Math.sin(angle) * out);
            probe.moveTo(spot.x, spot.y, spot.z, 0.0F, 0.0F);
            if (level.noCollision(probe)) {
                return spot;
            }
        }
        return null;
    }

    /** A spire's queen fallen: the way home opens where she fell. */
    private static void onDeath(LivingDeathEvent event) {
        if (event.isCanceled() || !(event.getEntity() instanceof DreadQueen queen) || !(queen.level() instanceof ServerLevel level)
                || !Dreadlands.is(level) || !queen.getPersistentData().contains(SPIRE_KEY)) {
            return;
        }
        Vec3 ground = Utils.moveToRelativeGroundLevel(level, queen.position(), 64);
        BlockPos foot = BlockPos.containing(ground.x, ground.y + 0.01, ground.z);
        if (openReturnGate(level, foot)) {
            for (ServerPlayer near : level.getEntitiesOfClass(ServerPlayer.class, new AABB(foot).inflate(64.0))) {
                near.displayClientMessage(Component.translatable("message.dreadpatch.dreadlands.return_gate"), true);
            }
        }
    }

    /**
     * Raises a Dread Gate's frame standing on {@code foot} (5 wide, 6 tall, round a 3×4 opening, along x),
     * clears the opening and a block before and behind it, and opens it home.
     */
    public static boolean openReturnGate(ServerLevel level, BlockPos foot) {
        BlockState frame = IafBlocks.DREAD_STONE_BRICKS.get().defaultBlockState();
        for (int a = -2; a <= 2; a++) {
            for (int y = 0; y <= 5; y++) {
                BlockPos at = foot.offset(a, y, 0);
                boolean edge = Math.abs(a) == 2 || y == 0 || y == 5;
                level.setBlock(at, edge ? frame : Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
                if (!edge) {
                    level.setBlock(at.north(), Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
                    level.setBlock(at.south(), Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
                }
            }
        }
        return DreadGate.openReturn(level, foot.above(), RETURN_TICKS);
    }
}
