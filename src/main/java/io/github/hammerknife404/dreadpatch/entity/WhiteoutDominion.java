package io.github.hammerknife404.dreadpatch.entity;

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
 * as a dread mob of her court ({@link #raise}), golems aside. Drawn by {@code WhiteoutRenderer} and
 * {@code WhiteoutClient}: driven snow, snow lying on the ground for as long as it stands, a wall of
 * it at the edge, and the fog. Never saved: it blows over in under a minute.
 */
public class WhiteoutDominion extends Entity implements TraceableEntity {
    /** Ticks of Whiteout each renewal gives, so it lingers a moment after leaving; renewed this often. */
    static final int EFFECT_TICKS = 40;
    static final int RENEW_EVERY = 10;
    /** How far from a death to look for a storm holding it: past any storm's radius. */
    static final double SEARCH = 64.0;

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

    public static void register(IEventBus gameBus) {
        // Last, and only if nothing has saved them.
        gameBus.addListener(EventPriority.LOWEST, WhiteoutDominion::onDeath);
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
        }
        if (this.tickCount >= this.getLifetime()) {
            this.discard();
            return;
        }
        Entity owner = this.getOwner();
        // Hers breaks when she falls (or is gone from the world): it clears.
        if (this.raisesDead && (owner == null || !owner.isAlive()) && this.getLifetime() > this.tickCount + WhiteoutStorm.BUILD_TICKS) {
            this.setLifetime(this.tickCount + WhiteoutStorm.BUILD_TICKS);
        }
        boolean renew = this.tickCount % RENEW_EVERY == 0;
        boolean freeze = WhiteoutStorm.freezesAt(this.tickCount);
        if (!renew && !freeze) {
            return;
        }
        DamageSource cold = freeze ? this.freezing(owner) : null;
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
        return (victim instanceof Mob || victim instanceof Player) && !(victim instanceof IDreadMob) && !(victim instanceof DragonBaseEntity)
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
        Entity risen = DreadMobEntity.necromancyEntity(victim);
        if (risen == null) {
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
            if (queen.getTarget() != null && queen.getTarget().isAlive()) {
                dread.setTarget(queen.getTarget());
            }
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
