package io.github.hammerknife404.dreadpatch.entity;

import java.util.Set;
import java.util.HashSet;
import com.iafenvoy.iceandfire.entity.DragonBaseEntity;
import com.iafenvoy.iceandfire.entity.DreadBeastEntity;
import com.iafenvoy.iceandfire.entity.DreadMobEntity;
import com.iafenvoy.iceandfire.entity.DreadThrallEntity;
import com.iafenvoy.iceandfire.entity.util.IDreadMob;
import com.iafenvoy.iceandfire.registry.IafEntities;
import com.iafenvoy.iceandfire.registry.IafSounds;
import io.github.hammerknife404.dreadpatch.magic.WhiteoutStorm;
import io.github.hammerknife404.dreadpatch.registry.ModRegistries;
import io.redspace.ironsspellbooks.capabilities.magic.MagicManager;
import it.unimi.dsi.fastutil.longs.LongIterator;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.EntityTypeTags;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.TraceableEntity;
import net.minecraft.world.entity.animal.AbstractGolem;
import net.minecraft.world.entity.monster.Shulker;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.FrostedIceBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.Tags;
import net.neoforged.neoforge.event.EventHooks;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;

import java.util.UUID;

/**
 * Whiteout Dominion: a blizzard over the ground it was raised on, {@link #getRadius} blocks round,
 * standing where it was cast ({@link WhiteoutStorm} has its shape and numbers). Everyone inside it
 * may harm is kept in a {@code Whiteout} (slowed, and blind past a few blocks) and, every few
 * seconds, takes a point of freezing damage. In hers, the Dread Queen's, whatever dies rises again
 * as a dread mob of her court ({@link #raise}), golems aside; a bat or parrot as a miniature dread
 * phantom, a water creature as itself, Dread ({@link RisenDread}, 0.63.0). Its water freezes over as under Frost
 * Walker ({@link #frost}), in patches as its snow drifts in, and thaws as it clears. Drawn by
 * {@code WhiteoutRenderer} and {@code WhiteoutClient}: driven snow, snow lying on the ground for as
 * long as it stands, a wall of it at the edge, and the fog. Never saved: it blows over in under a minute.
 */
public class WhiteoutDominion extends Entity implements TraceableEntity {
    /** Ticks of Whiteout each renewal gives, so it lingers a moment after leaving; renewed this often. */
    static final int EFFECT_TICKS = 40;
    static final int RENEW_EVERY = 10;
    /** How far from a death to look for a storm holding it: past any storm's radius. */
    static final double SEARCH = 64.0;
    /** Ticks between its looks over its water: freezing, holding and thawing it. */
    static final int FROST_EVERY = 10;

    private static final EntityDataAccessor<Integer> LIFETIME = SynchedEntityData.defineId(WhiteoutDominion.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Float> RADIUS = SynchedEntityData.defineId(WhiteoutDominion.class, EntityDataSerializers.FLOAT);
    private static final ParticleOptions CHARCOAL = new DustParticleOptions(new Vector3f(0.078F, 0.086F, 0.114F), 1.6F);

    @Nullable
    private UUID ownerId;
    @Nullable
    private Entity cachedOwner;
    /** Hers: whatever dies inside rises in her court. */
    private boolean raisesDead;
    private int raised;
    /** The water it froze (server side), so it thaws that and only that. */
    private final LongOpenHashSet frozen = new LongOpenHashSet();

    public WhiteoutDominion(EntityType<? extends WhiteoutDominion> type, Level level) {
        super(type, level);
        this.noPhysics = true;
    }

    public static WhiteoutDominion create(Level level, @Nullable LivingEntity owner, Vec3 at, float radius, int lifetime, boolean raisesDead) {
        WhiteoutDominion storm = new WhiteoutDominion(ModRegistries.WHITEOUT_DOMINION.get(), level);
        storm.setPos(at);
        storm.setRadius(radius);
        storm.setLifetime(lifetime);
        storm.ownerId = owner == null ? null : owner.getUUID();
        storm.cachedOwner = owner;
        storm.raisesDead = raisesDead;
        return storm;
    }

    /** Its lifetime when it rose, and Syndrigossa's eggs in it since then. Never saved, as the storm isn't. */
    private int fullLifetime = -1;
    private final Set<UUID> eggs = new HashSet<>();

    public static void register(IEventBus gameBus) {
        // Last, and only if nothing has saved them.
        gameBus.addListener(EventPriority.LOWEST, WhiteoutDominion::onDeath);
    }

    /**
     * Its whole course run (its lifetime not cut short, as hers is when she falls): every egg it rose
     * over and that stayed in it soaks it up ({@link SyndrigossaEggEntity#soak}). One discarded early
     * (a recast, {@code /kill}) never gets here.
     */
    private void soakEggs() {
        if (this.getLifetime() != this.fullLifetime || !(this.level() instanceof ServerLevel server)) {
            return;
        }
        for (UUID id : this.eggs) {
            if (server.getEntity(id) instanceof SyndrigossaEggEntity egg && this.holds(egg)) {
                egg.soak();
            }
        }
    }

    public boolean holds(Entity entity) {
        return this.holds(entity.position());
    }

    public boolean holds(Vec3 point) {
        return WhiteoutStorm.contains(point.x - this.getX(), point.y - this.getY(), point.z - this.getZ(), this.getRadius());
    }

    /** How thick it looks now, 0 to 1: it builds up, then clears. */
    public float strength(float partialTick) {
        return WhiteoutStorm.strength(this.tickCount + partialTick, this.getLifetime());
    }

    @Override
    public void tick() {
        super.tick();
        if (this.level().isClientSide) {
            return;
        }
        if (this.tickCount == 1) {
            this.level().playSound(null, this.getX(), this.getY(), this.getZ(), SoundEvents.ILLUSIONER_PREPARE_BLINDNESS, SoundSource.HOSTILE, 4.0F, 0.5F);
            // Syndrigossa's eggs it rises over: a full storm, start to end, is one step to hatching.
            this.fullLifetime = this.getLifetime();
            for (SyndrigossaEggEntity egg : this.level().getEntitiesOfClass(SyndrigossaEggEntity.class, this.area(), this::holds)) {
                this.eggs.add(egg.getUUID());
            }
        }
        if (this.tickCount >= this.getLifetime()) {
            this.soakEggs();
            this.discard();
            return;
        }
        if (this.tickCount % 20 == 0 && !this.eggs.isEmpty() && this.level() instanceof ServerLevel server) {
            this.eggs.removeIf(id -> !(server.getEntity(id) instanceof SyndrigossaEggEntity egg) || !this.holds(egg));
        }
        Entity owner = this.getOwner();
        // Hers breaks when she falls (or is gone from the world): it clears.
        if (this.raisesDead && (owner == null || !owner.isAlive()) && this.getLifetime() > this.tickCount + WhiteoutStorm.BUILD_TICKS) {
            this.setLifetime(this.tickCount + WhiteoutStorm.BUILD_TICKS);
        }
        if (this.tickCount % FROST_EVERY == 0) {
            this.frost();
        }
        boolean renew = this.tickCount % RENEW_EVERY == 0;
        boolean freeze = WhiteoutStorm.freezesAt(this.tickCount);
        if (!renew && !freeze) {
            return;
        }
        DamageSource cold = freeze ? this.freezing(owner) : null;
        if (renew) {
            // Its cold puts out every fire inside, hers too (0.64.0, the owner: she kept burning in her own storm).
            for (Entity burning : this.level().getEntitiesOfClass(Entity.class, this.area(), e -> e.isOnFire() && this.holds(e))) {
                burning.clearFire();
            }
        }
        for (LivingEntity victim : this.level().getEntitiesOfClass(LivingEntity.class, this.area(),
                e -> e.isAlive() && !e.isSpectator() && this.holds(e) && RimefireCrown.canHarm(owner, e))) {
            if (renew) {
                victim.addEffect(new MobEffectInstance(ModRegistries.WHITEOUT, EFFECT_TICKS, 0, false, false, true), owner);
            }
            if (cold != null && !victim.getType().is(EntityTypeTags.FREEZE_IMMUNE_ENTITY_TYPES)) {
                victim.hurt(cold, WhiteoutStorm.FREEZE_DAMAGE);
            }
        }
    }

    /**
     * Its water: still water at the top of each column in it (open to the air, nothing living or a boat
     * on it: {@link Rimewalk#canFreeze}) freezes to vanilla's frosted ice where its snow has begun to
     * settle ({@link WhiteoutStorm#ices}: in patches, as the snow drifts in), and stays whole while it
     * lies (its melting set back, as under her Rimewalk). As the storm clears it breaks up patch by
     * patch, a crack a look, and back to water; whatever is left thaws when it ends ({@link #remove}).
     * Only loaded chunks. Laid and held without block updates: vanilla melts frosted ice outright when
     * a neighbouring block turns to frosted ice and it has fewer than two such neighbours, so patchy ice
     * laid with updates would melt as fast as it was laid.
     */
    private void frost() {
        Level level = this.level();
        float cover = WhiteoutStorm.cover(this.tickCount, this.getLifetime());
        float radius = this.getRadius();
        int r = Mth.ceil(radius);
        int cx = Mth.floor(this.getX());
        int cz = Mth.floor(this.getZ());
        double floor = this.getY() - WhiteoutStorm.DEPTH;
        double ceiling = this.getY() + WhiteoutStorm.HEIGHT;
        BlockState ice = Blocks.FROSTED_ICE.defaultBlockState();
        BlockPos.MutableBlockPos at = new BlockPos.MutableBlockPos();
        for (int z = cz - r; z <= cz + r; z++) {
            for (int x = cx - r; x <= cx + r; x++) {
                double dx = x + 0.5 - this.getX();
                double dz = z + 0.5 - this.getZ();
                if (dx * dx + dz * dz > radius * radius || !level.hasChunk(x >> 4, z >> 4)) {
                    continue;
                }
                int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING, x, z) - 1;
                if (y < floor || y > ceiling) {
                    continue;
                }
                at.set(x, y, z);
                BlockState state = level.getBlockState(at);
                boolean lies = WhiteoutStorm.ices(cover, WhiteoutStorm.patch(x, z));
                if (state.is(Blocks.FROSTED_ICE)) {
                    if (!this.frozen.contains(at.asLong())) {
                        continue;
                    }
                    int age = state.getValue(FrostedIceBlock.AGE);
                    if (lies && age > 0) {
                        level.setBlock(at, ice, Block.UPDATE_CLIENTS);
                    } else if (!lies && age < FrostedIceBlock.MAX_AGE) {
                        level.setBlock(at, state.setValue(FrostedIceBlock.AGE, age + 1), Block.UPDATE_CLIENTS);
                    } else if (!lies) {
                        thaw(level, at);
                        this.frozen.remove(at.asLong());
                    }
                } else if (lies && Rimewalk.canFreeze(level, at, state) && level.setBlock(at, ice, Block.UPDATE_CLIENTS)) {
                    this.frozen.add(at.asLong());
                }
            }
        }
    }

    /** Back to water, as ice melts (vanilla's {@code IceBlock.melt}). */
    private static void thaw(Level level, BlockPos at) {
        level.setBlockAndUpdate(at, Blocks.WATER.defaultBlockState());
        level.neighborChanged(at, Blocks.WATER, at);
    }

    /**
     * Ended (its time up, its caster's next storm, {@code /kill}): what's left of its ice thaws. Not
     * when its chunk unloads (it isn't saved): that ice is left to melt as Frost Walker's does, by day.
     */
    @Override
    public void remove(Entity.RemovalReason reason) {
        if (!this.level().isClientSide && reason.shouldDestroy()) {
            for (LongIterator it = this.frozen.iterator(); it.hasNext(); ) {
                BlockPos at = BlockPos.of(it.nextLong());
                if (this.level().isLoaded(at) && this.level().getBlockState(at).is(Blocks.FROSTED_ICE)) {
                    thaw(this.level(), at);
                }
            }
            this.frozen.clear();
        }
        super.remove(reason);
    }

    /** Its cold, as its caster's (death messages, kill credit, who the hurt turn on); vanilla's freezing without one. */
    private DamageSource freezing(@Nullable Entity owner) {
        return owner == null ? this.damageSources().freeze() : ModRegistries.damageSource(this.level(), ModRegistries.WHITEOUT_DAMAGE, this, owner);
    }

    /** The box round its cylinder. */
    private AABB area() {
        double r = this.getRadius();
        return new AABB(this.getX() - r, this.getY() - WhiteoutStorm.DEPTH, this.getZ() - r,
                this.getX() + r, this.getY() + WhiteoutStorm.HEIGHT, this.getZ() + r);
    }

    /** A death inside one of her storms: the dead rise in her court. */
    private static void onDeath(LivingDeathEvent event) {
        LivingEntity victim = event.getEntity();
        if (!(victim.level() instanceof ServerLevel level) || !canRise(victim)) {
            return;
        }
        for (WhiteoutDominion storm : level.getEntitiesOfClass(WhiteoutDominion.class, victim.getBoundingBox().inflate(SEARCH, WhiteoutStorm.HEIGHT + WhiteoutStorm.DEPTH, SEARCH),
                s -> s.raisesDead && s.raised < WhiteoutStorm.MAX_RAISED && s.holds(victim))) {
            if (storm.getOwner() instanceof DreadQueen queen && queen.isAlive() && storm.raise(level, victim, queen)) {
                return;
            }
        }
    }

    /**
     * Mobs and players: not armor stands or her husks; not the Dread (her court stays dead), dragons
     * (as Ice and Fire's own necromancy spares them), bosses, or golems (the owner's call).
     */
    static boolean canRise(LivingEntity victim) {
        return (victim instanceof Mob || victim instanceof Player) && !RisenDread.dread(victim) && !(victim instanceof DragonBaseEntity)
                && !victim.getType().is(Tags.EntityTypes.BOSSES) && !isGolem(victim);
    }

    /** Vanilla's golems (a shulker is one in code only) and any mod's whose id says golem. */
    static boolean isGolem(LivingEntity victim) {
        return victim instanceof AbstractGolem && !(victim instanceof Shulker)
                || BuiltInRegistries.ENTITY_TYPE.getKey(victim.getType()).getPath().contains("golem");
    }

    /**
     * The dead rising where they fell, a dread mob of her court: Ice and Fire's own necromancy (which
     * nothing in Ice and Fire calls) picks what by what they were (a scuttler from an arthropod, a
     * ghoul from a zombie, a thrall in their gear from a player or the undead, a dread horse from a
     * horse, a dread beast from an animal); anything else rises by its shape
     * ({@link WhiteoutStorm#risesAsThrall}): upright, a thrall in its gear; otherwise a dread beast its
     * size. What they carried was copied, never moved: the dead still drop their own, and the risen
     * never drop theirs.
     */
    private boolean raise(ServerLevel level, LivingEntity victim, DreadQueen queen) {
        // A bat or parrot rises a miniature dread phantom, a water creature as itself, Dread (0.63.0).
        boolean ours = RisenDread.risesAs(victim);
        Entity risen = ours ? RisenDread.rise(level, victim, queen) : DreadMobEntity.necromancyEntity(victim);
        if (risen == null && !ours) {
            risen = WhiteoutStorm.risesAsThrall(victim.getBbWidth(), victim.getBbHeight()) ? this.thrall(level, victim) : this.beast(level, victim);
        }
        if (risen == null) {
            return false;
        }
        risen.moveTo(victim.getX(), victim.getY(), victim.getZ(), victim.getYRot(), 0.0F);
        if (risen instanceof Mob mob) {
            for (EquipmentSlot slot : EquipmentSlot.values()) {
                // Ice and Fire hands over the dead's own stacks; theirs drop, so the risen wear copies.
                mob.setItemSlot(slot, mob.getItemBySlot(slot).copy());
                mob.setDropChance(slot, 0.0F);
            }
        }
        if (risen instanceof DreadMobEntity dread) {
            dread.setCommanderId(queen.getUUID());
        }
        if (risen instanceof Mob mob && queen.getTarget() != null && queen.getTarget().isAlive()) {
            mob.setTarget(queen.getTarget());
        }
        if (!level.addFreshEntity(risen)) {
            return false;
        }
        this.raised++;
        MagicManager.spawnParticles(level, CHARCOAL, victim.getX(), victim.getY() + 0.2, victim.getZ(), 20, 0.4, 0.1, 0.4, 0.02, false);
        MagicManager.spawnParticles(level, ParticleTypes.SOUL, victim.getX(), victim.getY() + 0.5, victim.getZ(), 8, 0.3, 0.3, 0.3, 0.03, false);
        level.playSound(null, victim.getX(), victim.getY(), victim.getZ(), IafSounds.DREAD_LICH_SUMMON.get(), SoundSource.HOSTILE, 1.0F, 0.8F);
        return true;
    }

    /** A dread beast the dead's size, as Ice and Fire raises an animal. */
    @Nullable
    private DreadBeastEntity beast(ServerLevel level, LivingEntity victim) {
        DreadBeastEntity beast = IafEntities.DREAD_BEAST.get().create(level);
        if (beast == null) {
            return null;
        }
        beast.moveTo(victim.getX(), victim.getY(), victim.getZ(), victim.getYRot(), 0.0F);
        EventHooks.finalizeMobSpawn(beast, level, level.getCurrentDifficultyAt(victim.blockPosition()), MobSpawnType.MOB_SUMMONED, null);
        beast.setSize(WhiteoutStorm.beastSize(victim.getBbWidth()));
        return beast;
    }

    /** A thrall in the dead's gear, as Ice and Fire raises a player or the undead. */
    @Nullable
    private DreadThrallEntity thrall(ServerLevel level, LivingEntity victim) {
        DreadThrallEntity thrall = IafEntities.DREAD_THRALL.get().create(level);
        if (thrall == null) {
            return null;
        }
        thrall.moveTo(victim.getX(), victim.getY(), victim.getZ(), victim.getYRot(), 0.0F);
        // Through NeoForge, as her court rises: its rise animation and its own gear, which theirs replaces.
        EventHooks.finalizeMobSpawn(thrall, level, level.getCurrentDifficultyAt(victim.blockPosition()), MobSpawnType.MOB_SUMMONED, null);
        thrall.setCustomArmorHead(false);
        thrall.setCustomArmorChest(false);
        thrall.setCustomArmorLegs(false);
        thrall.setCustomArmorFeet(false);
        for (EquipmentSlot slot : EquipmentSlot.values()) {
            thrall.setItemSlot(slot, victim.getItemBySlot(slot).copy());
        }
        return thrall;
    }

    public float getRadius() {
        return this.entityData.get(RADIUS);
    }

    public void setRadius(float radius) {
        this.entityData.set(RADIUS, radius);
    }

    public int getLifetime() {
        return this.entityData.get(LIFETIME);
    }

    public void setLifetime(int lifetime) {
        this.entityData.set(LIFETIME, lifetime);
    }

    /** Whether this storm is the caster's own. */
    public boolean ownedBy(Entity caster) {
        return caster.getUUID().equals(this.ownerId);
    }

    @Nullable
    @Override
    public Entity getOwner() {
        if (this.cachedOwner == null && this.ownerId != null && this.level() instanceof ServerLevel server) {
            this.cachedOwner = server.getEntity(this.ownerId);
        }
        return this.cachedOwner;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(LIFETIME, WhiteoutStorm.QUEEN_LIFETIME);
        builder.define(RADIUS, WhiteoutStorm.QUEEN_RADIUS);
    }

    /** Drawn wherever any of it is on screen: its snow wall stands at its edge, its drifts round whoever's in it. */
    @Override
    public AABB getBoundingBoxForCulling() {
        return this.area();
    }

    /** Seen from well outside it, as weather is. */
    @Override
    public boolean shouldRenderAtSqrDistance(double distance) {
        double reach = this.getRadius() + 96.0;
        return distance < reach * reach;
    }

    @Override
    public boolean shouldBeSaved() {
        return false;
    }

    @Override
    public boolean isPickable() {
        return false;
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        if (tag.contains("Lifetime")) {
            this.setLifetime(tag.getInt("Lifetime"));
        }
        if (tag.contains("Radius")) {
            this.setRadius(tag.getFloat("Radius"));
        }
        if (tag.hasUUID("Owner")) {
            this.ownerId = tag.getUUID("Owner");
        }
        this.raisesDead = tag.getBoolean("RaisesDead");
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        tag.putInt("Lifetime", this.getLifetime());
        tag.putFloat("Radius", this.getRadius());
        if (this.ownerId != null) {
            tag.putUUID("Owner", this.ownerId);
        }
        tag.putBoolean("RaisesDead", this.raisesDead);
    }
}
