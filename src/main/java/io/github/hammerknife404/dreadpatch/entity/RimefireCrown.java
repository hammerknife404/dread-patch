package io.github.hammerknife404.dreadpatch.entity;

import com.iafenvoy.iceandfire.entity.util.IDreadMob;
import com.iafenvoy.iceandfire.registry.IafParticles;
import io.github.hammerknife404.dreadpatch.magic.CrownShape;
import io.github.hammerknife404.dreadpatch.magic.DreadDamage;
import io.github.hammerknife404.dreadpatch.registry.ModRegistries;
import io.redspace.ironsspellbooks.capabilities.magic.MagicManager;
import io.redspace.ironsspellbooks.damage.DamageSources;
import io.redspace.ironsspellbooks.damage.SpellDamageSource;
import io.redspace.ironsspellbooks.particle.FogParticleOptions;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.TraceableEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CampfireBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;

import java.util.UUID;

/**
 * Rimefire Crown: a ring of ice spikes wreathed in cold dread fire (drawn by
 * {@code RimefireCrownRenderer}, nothing placed in the world) over cyan and black smoke. It roots
 * everyone inside (Rimebound), and its dread fire, which steals heat rather than giving it, drinks
 * every other fire within it. A Withering Rime-Bolt into someone it holds shatters it
 * ({@link #shatterAround}). Never saved: a crown lasts seconds.
 */
public class RimefireCrown extends Entity implements TraceableEntity {
    public static final float RADIUS = CrownShape.RADIUS;
    /** Shrapnel when a bolt shatters it: flat damage plus a share of max health, and Withering Rime. */
    public static final float SHATTER_DAMAGE = 4.0F;
    public static final float SHATTER_MAX_HEALTH_SHARE = 0.06F;
    public static final double SHATTER_RADIUS = 3.0;
    static final int SHATTER_RIME_TICKS = 60;

    private static final EntityDataAccessor<Integer> LIFETIME = SynchedEntityData.defineId(RimefireCrown.class, EntityDataSerializers.INT);
    private static final ParticleOptions ICE_SHARDS = new BlockParticleOption(ParticleTypes.BLOCK, Blocks.PACKED_ICE.defaultBlockState());
    /** The smoke it stands in: Iron's ground fog, in her dread-fire cyan and in soot. */
    private static final Vector3f CYAN_SMOKE = new Vector3f(0.0F, 0.80F, 1.0F);
    private static final Vector3f BLACK_SMOKE = new Vector3f(0.02F, 0.024F, 0.035F);

    @Nullable
    private UUID ownerId;
    @Nullable
    private Entity cachedOwner;
    private boolean kindled;

    public RimefireCrown(EntityType<? extends RimefireCrown> type, Level level) {
        super(type, level);
        this.noPhysics = true;
    }

    public static RimefireCrown create(Level level, @Nullable LivingEntity owner, Vec3 at, int lifetime) {
        RimefireCrown crown = new RimefireCrown(ModRegistries.RIMEFIRE_CROWN.get(), level);
        crown.setPos(at);
        crown.setLifetime(lifetime);
        crown.setOwner(owner);
        return crown;
    }

    protected void setOwner(@Nullable LivingEntity owner) {
        this.ownerId = owner == null ? null : owner.getUUID();
        this.cachedOwner = owner;
    }

    /** Blocks from its centre to its ring. */
    public float radius() {
        return RADIUS;
    }

    /** How far above its base it holds. */
    public double height() {
        return CrownShape.HEIGHT;
    }

    public boolean holds(Entity entity) {
        return CrownShape.contains(entity.getX() - this.getX(), entity.getY() - this.getY(), entity.getZ() - this.getZ(), this.radius(), this.height());
    }

    /** What it does, each tick, to everyone inside it may harm: roots them (Rimebound) and puts out their burning. */
    protected void hold(LivingEntity victim, @Nullable Entity owner) {
        victim.addEffect(new MobEffectInstance(ModRegistries.RIMEBOUND, 10, 0, false, false, true), owner);
        victim.clearFire();
    }

    /** The two colours of the smoke it stands in. */
    protected Vector3f[] smoke() {
        return new Vector3f[]{CYAN_SMOKE, BLACK_SMOKE};
    }

    @Override
    public void tick() {
        super.tick();
        if (this.level().isClientSide) {
            this.smoulder();
            return;
        }
        if (!this.kindled) {
            this.drinkFires();
            this.kindled = true;
            this.level().playSound(null, this.getX(), this.getY(), this.getZ(), SoundEvents.GLASS_PLACE, SoundSource.HOSTILE, 1.5F, 0.5F);
            this.level().playSound(null, this.getX(), this.getY(), this.getZ(), SoundEvents.FIRECHARGE_USE, SoundSource.HOSTILE, 1.0F, 0.5F);
        }
        if (this.tickCount >= this.getLifetime()) {
            this.shatter();
            return;
        }
        Entity owner = this.getOwner();
        for (LivingEntity victim : this.level().getEntitiesOfClass(LivingEntity.class, this.getBoundingBox().inflate(0.5, CrownShape.DEPTH, 0.5),
                e -> e.isAlive() && this.holds(e) && canHarm(owner, e))) {
            this.hold(victim, owner);
        }
    }

    /** Who her magic hits: not its caster or their allies (their summoned court too), and the Dread don't hit the Dread. */
    static boolean canHarm(@Nullable Entity owner, Entity target) {
        if (owner == null) {
            return true;
        }
        return target != owner && !owner.isAlliedTo(target) && !target.isAlliedTo(owner)
                && !(owner instanceof IDreadMob && RisenDread.dread(target))
                && !SummonedCourt.serves(target, owner) && !SummonedCourt.serves(owner, target);
    }

    /** Dread fire steals heat: fire blocks and lit campfires inside it go out. */
    private void drinkFires() {
        int r = (int) Math.ceil(this.radius());
        BlockPos base = this.blockPosition();
        for (BlockPos pos : BlockPos.betweenClosed(base.offset(-r, -1, -r), base.offset(r, (int) Math.ceil(this.height()), r))) {
            if (!CrownShape.contains(pos.getX() + 0.5 - this.getX(), pos.getY() - this.getY(), pos.getZ() + 0.5 - this.getZ(), this.radius(), this.height())) {
                continue;
            }
            drinkFire(this.level(), pos);
        }
    }

    /** Dread fire drinking the fire at one spot: a fire block goes out, a lit campfire is snuffed. Her sword's guard does it too. */
    public static void drinkFire(Level level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        if (state.is(BlockTags.FIRE)) {
            level.removeBlock(pos, false);
        } else if (state.hasProperty(CampfireBlock.LIT) && state.getValue(CampfireBlock.LIT)) {
            level.setBlockAndUpdate(pos, state.setValue(CampfireBlock.LIT, false));
        }
    }

    /** Breaks the crown: its ice bursts, its fire gutters, and it's gone. */
    public void shatter() {
        if (this.level() instanceof ServerLevel) {
            float radius = this.radius();
            MagicManager.spawnParticles(this.level(), ICE_SHARDS, this.getX(), this.getY() + 1.0, this.getZ(), Math.round(20 * radius), radius * 0.6, 0.8, radius * 0.6, 0.2, false);
            MagicManager.spawnParticles(this.level(), IafParticles.DREAD_TORCH.get(), this.getX(), this.getY() + 0.4, this.getZ(), Math.round(10 * radius), radius * 0.7, 0.3, radius * 0.7, 0.05, false);
            this.level().playSound(null, this.getX(), this.getY(), this.getZ(), SoundEvents.GLASS_BREAK, SoundSource.HOSTILE, 1.5F, 0.6F);
        }
        this.discard();
    }

    /**
     * A Withering Rime-Bolt has hit someone the crowns hold: those crowns shatter, and their ice
     * cuts everyone near the target (the target included), leaving Withering Rime.
     */
    public static void shatterAround(Level level, Entity bolt, @Nullable Entity owner, LivingEntity target) {
        target.removeEffect(ModRegistries.RIMEBOUND);
        // Only Rimefire Crowns: a Sovereign's Eclipse doesn't shatter.
        for (RimefireCrown crown : level.getEntitiesOfClass(RimefireCrown.class, target.getBoundingBox().inflate(RADIUS + 1.0),
                c -> c.getType() == ModRegistries.RIMEFIRE_CROWN.get() && c.holds(target))) {
            crown.shatter();
        }
        SpellDamageSource source = ModRegistries.RIMEFIRE_CROWN_SPELL.get().getDamageSource(bolt, owner);
        source.setIFrames(0);
        for (LivingEntity victim : level.getEntitiesOfClass(LivingEntity.class, target.getBoundingBox().inflate(SHATTER_RADIUS),
                e -> e.isAlive() && canHarm(owner, e))) {
            float amount = SHATTER_DAMAGE + DreadDamage.maxHealthShare(victim.getMaxHealth(), SHATTER_MAX_HEALTH_SHARE);
            victim.invulnerableTime = 0;
            if (DamageSources.applyDamage(victim, amount, source) && victim.isAlive()) {
                victim.addEffect(new MobEffectInstance(ModRegistries.WITHERING_RIME, SHATTER_RIME_TICKS), owner);
            }
        }
        MagicManager.spawnParticles(level, ICE_SHARDS, target.getX(), target.getY() + 1.0, target.getZ(), 50, 1.0, 0.8, 1.0, 0.3, false);
        MagicManager.spawnParticles(level, IafParticles.DREAD_TORCH.get(), target.getX(), target.getY() + 1.0, target.getZ(), 25, 1.2, 0.8, 1.2, 0.1, false);
        level.playSound(null, target.getX(), target.getY(), target.getZ(), SoundEvents.GLASS_BREAK, SoundSource.HOSTILE, 2.0F, 0.5F);
    }

    /**
     * Cyan and black smoke pooling on the ground inside the ring (Iron's fog, which sinks and
     * spreads), dread-fire sparks rising off the ring, and a little snow.
     */
    private void smoulder() {
        float radius = this.radius();
        // A crown's worth of each per tick, more for a bigger ring (not in proportion to its area: Iron's fog lingers).
        int scale = Math.max(1, Math.round(radius / RADIUS));
        Vector3f[] smoke = this.smoke();
        for (int i = 0; i < scale; i++) {
            double a = this.random.nextDouble() * Math.PI * 2;
            double r = Math.sqrt(this.random.nextDouble()) * (radius + 0.3);
            this.level().addParticle(new FogParticleOptions(smoke[this.random.nextInt(smoke.length)], 0.35F + this.random.nextFloat() * 0.1F),
                    this.getX() + Math.cos(a) * r, this.getY() + 0.1, this.getZ() + Math.sin(a) * r, 0.0, 0.0, 0.0);
        }
        for (int i = 0; i < 2 * scale; i++) {
            double f = this.random.nextDouble() * Math.PI * 2;
            double rf = radius + (this.random.nextDouble() - 0.5) * 0.6;
            this.level().addParticle(IafParticles.DREAD_TORCH.get(), this.getX() + Math.cos(f) * rf, this.getY() + 0.1 + this.random.nextDouble() * 0.7,
                    this.getZ() + Math.sin(f) * rf, 0.0, 0.03, 0.0);
        }
        for (int i = 0; i < scale; i++) {
            if (scale == 1 && this.tickCount % 2 != 0) {
                break;
            }
            double s = this.random.nextDouble() * Math.PI * 2;
            double rs = Math.sqrt(this.random.nextDouble()) * radius;
            this.level().addParticle(ParticleTypes.SNOWFLAKE, this.getX() + Math.cos(s) * rs, this.getY() + this.random.nextDouble() * 2.0,
                    this.getZ() + Math.sin(s) * rs, -Math.sin(s) * 0.04, -0.01, Math.cos(s) * 0.04);
        }
    }

    public int getLifetime() {
        return this.entityData.get(LIFETIME);
    }

    public void setLifetime(int lifetime) {
        this.entityData.set(LIFETIME, lifetime);
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
        builder.define(LIFETIME, 100);
    }

    /** Its spikes lean out past its box; keep drawing them while only their tips are on screen. */
    @Override
    public AABB getBoundingBoxForCulling() {
        return this.getBoundingBox().inflate(1.0, 0.5, 1.0);
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
        if (tag.hasUUID("Owner")) {
            this.ownerId = tag.getUUID("Owner");
        }
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        tag.putInt("Lifetime", this.getLifetime());
        if (this.ownerId != null) {
            tag.putUUID("Owner", this.ownerId);
        }
    }
}
