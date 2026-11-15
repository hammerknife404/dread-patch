package io.github.hammerknife404.dreadpatch.entity;

import com.iafenvoy.iceandfire.entity.util.IDreadMob;
import io.github.hammerknife404.dreadpatch.magic.DreadDamage;
import io.github.hammerknife404.dreadpatch.magic.TombShape;
import io.github.hammerknife404.dreadpatch.registry.ModRegistries;
import io.redspace.ironsspellbooks.capabilities.magic.MagicManager;
import io.redspace.ironsspellbooks.damage.DamageSources;
import io.redspace.ironsspellbooks.damage.SpellDamageSource;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
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
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.Tags;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;

import java.util.UUID;

/**
 * Barrow-Glass Tomb: a ring of brittle black dread-glass spires (drawn by
 * {@code BarrowGlassTombRenderer}, nothing placed in the world) that roots everyone inside and puts
 * out fires within it. A Withering Rime-Bolt into someone it holds shatters it ({@link #shatter}).
 * Never saved: a tomb lasts seconds.
 */
public class BarrowGlassTomb extends Entity implements TraceableEntity {
    public static final float RADIUS = TombShape.RADIUS;
    /** Shrapnel when a bolt shatters it: flat damage plus a share of max health, and Withering Rime. */
    public static final float SHATTER_DAMAGE = 4.0F;
    public static final float SHATTER_MAX_HEALTH_SHARE = 0.06F;
    public static final double SHATTER_RADIUS = 3.0;
    static final int SHATTER_RIME_TICKS = 60;

    private static final EntityDataAccessor<Integer> LIFETIME = SynchedEntityData.defineId(BarrowGlassTomb.class, EntityDataSerializers.INT);
    private static final ParticleOptions CHARCOAL = new DustParticleOptions(new Vector3f(0.078F, 0.086F, 0.114F), 1.6F);
    private static final ParticleOptions BONE = new DustParticleOptions(new Vector3f(0.847F, 0.871F, 0.914F), 1.0F);
    private static final ParticleOptions GLASS = new BlockParticleOption(ParticleTypes.BLOCK, Blocks.BLACK_STAINED_GLASS.defaultBlockState());

    @Nullable
    private UUID ownerId;
    @Nullable
    private Entity cachedOwner;
    private boolean snuffed;

    public BarrowGlassTomb(EntityType<? extends BarrowGlassTomb> type, Level level) {
        super(type, level);
        this.noPhysics = true;
    }

    public static BarrowGlassTomb create(Level level, @Nullable LivingEntity owner, Vec3 at, int lifetime) {
        BarrowGlassTomb tomb = new BarrowGlassTomb(ModRegistries.BARROW_GLASS_TOMB.get(), level);
        tomb.setPos(at);
        tomb.setLifetime(lifetime);
        if (owner != null) {
            tomb.ownerId = owner.getUUID();
            tomb.cachedOwner = owner;
        }
        return tomb;
    }

    public boolean holds(Entity entity) {
        return TombShape.contains(entity.getX() - this.getX(), entity.getY() - this.getY(), entity.getZ() - this.getZ());
    }

    @Override
    public void tick() {
        super.tick();
        if (this.level().isClientSide) {
            this.squall();
            return;
        }
        if (!this.snuffed) {
            this.snuffFires();
            this.snuffed = true;
            this.level().playSound(null, this.getX(), this.getY(), this.getZ(), SoundEvents.GLASS_PLACE, SoundSource.HOSTILE, 1.5F, 0.5F);
        }
        if (this.tickCount >= this.getLifetime()) {
            this.shatter();
            return;
        }
        Entity owner = this.getOwner();
        for (LivingEntity victim : this.level().getEntitiesOfClass(LivingEntity.class, this.getBoundingBox().inflate(0.5, TombShape.DEPTH, 0.5),
                e -> e.isAlive() && this.holds(e) && canHarm(owner, e))) {
            victim.addEffect(new MobEffectInstance(ModRegistries.BARROW_GLASS, 10, 0, false, false, true), owner);
            victim.clearFire();
        }
    }

    /** Who her magic hits: not its caster or their allies, and the Dread don't hit the Dread. */
    static boolean canHarm(@Nullable Entity owner, Entity target) {
        if (owner == null) {
            return true;
        }
        return target != owner && !owner.isAlliedTo(target) && !target.isAlliedTo(owner)
                && !(owner instanceof IDreadMob && target instanceof IDreadMob);
    }

    /** Dread-glass drinks fire: fire blocks and lit campfires inside it go out. */
    private void snuffFires() {
        int r = (int) Math.ceil(RADIUS);
        BlockPos base = this.blockPosition();
        for (BlockPos pos : BlockPos.betweenClosed(base.offset(-r, -1, -r), base.offset(r, (int) Math.ceil(TombShape.HEIGHT), r))) {
            if (!TombShape.contains(pos.getX() + 0.5 - this.getX(), pos.getY() - this.getY(), pos.getZ() + 0.5 - this.getZ())) {
                continue;
            }
            BlockState state = this.level().getBlockState(pos);
            if (state.is(BlockTags.FIRE)) {
                this.level().removeBlock(pos, false);
            } else if (state.hasProperty(CampfireBlock.LIT) && state.getValue(CampfireBlock.LIT)) {
                this.level().setBlockAndUpdate(pos, state.setValue(CampfireBlock.LIT, false));
            }
        }
    }

    /** Breaks the tomb: glass bursts and it's gone. */
    public void shatter() {
        if (this.level() instanceof net.minecraft.server.level.ServerLevel) {
            MagicManager.spawnParticles(this.level(), GLASS, this.getX(), this.getY() + 1.0, this.getZ(), 60, RADIUS * 0.6, 0.8, RADIUS * 0.6, 0.2, false);
            this.level().playSound(null, this.getX(), this.getY(), this.getZ(), SoundEvents.GLASS_BREAK, SoundSource.HOSTILE, 1.5F, 0.6F);
        }
        this.discard();
    }

    /**
     * A Withering Rime-Bolt has hit someone the tombs hold: those tombs shatter, and their shrapnel
     * cuts everyone near the target (the target included), leaving Withering Rime.
     */
    public static void shatterAround(Level level, Entity bolt, @Nullable Entity owner, LivingEntity target) {
        target.removeEffect(ModRegistries.BARROW_GLASS);
        for (BarrowGlassTomb tomb : level.getEntitiesOfClass(BarrowGlassTomb.class, target.getBoundingBox().inflate(RADIUS + 1.0), t -> t.holds(target))) {
            tomb.shatter();
        }
        SpellDamageSource source = ModRegistries.BARROW_GLASS_TOMB_SPELL.get().getDamageSource(bolt, owner);
        source.setIFrames(0);
        for (LivingEntity victim : level.getEntitiesOfClass(LivingEntity.class, target.getBoundingBox().inflate(SHATTER_RADIUS),
                e -> e.isAlive() && canHarm(owner, e))) {
            float amount = SHATTER_DAMAGE + DreadDamage.maxHealthShare(victim.getMaxHealth(), SHATTER_MAX_HEALTH_SHARE, victim.getType().is(Tags.EntityTypes.BOSSES));
            victim.invulnerableTime = 0;
            if (DamageSources.applyDamage(victim, amount, source) && victim.isAlive()) {
                victim.addEffect(new MobEffectInstance(ModRegistries.WITHERING_RIME, SHATTER_RIME_TICKS), owner);
            }
        }
        MagicManager.spawnParticles(level, BONE, target.getX(), target.getY() + 1.0, target.getZ(), 30, 1.2, 0.8, 1.2, 0.1, false);
        MagicManager.spawnParticles(level, GLASS, target.getX(), target.getY() + 1.0, target.getZ(), 40, 1.0, 0.8, 1.0, 0.3, false);
        level.playSound(null, target.getX(), target.getY(), target.getZ(), SoundEvents.GLASS_BREAK, SoundSource.HOSTILE, 2.0F, 0.5F);
    }

    /** Squall fog: charcoal haze, bone flecks and snow swirling low inside the ring. */
    private void squall() {
        for (int i = 0; i < 4; i++) {
            double a = this.random.nextDouble() * Math.PI * 2;
            double r = Math.sqrt(this.random.nextDouble()) * RADIUS;
            double x = this.getX() + Math.cos(a) * r;
            double z = this.getZ() + Math.sin(a) * r;
            double y = this.getY() + this.random.nextDouble() * 1.2;
            ParticleOptions particle = i == 0 ? ParticleTypes.SNOWFLAKE : i == 1 ? BONE : CHARCOAL;
            this.level().addParticle(particle, x, y, z, -Math.sin(a) * 0.05, 0.0, Math.cos(a) * 0.05);
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
        if (this.cachedOwner == null && this.ownerId != null && this.level() instanceof net.minecraft.server.level.ServerLevel server) {
            this.cachedOwner = server.getEntity(this.ownerId);
        }
        return this.cachedOwner;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(LIFETIME, 100);
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
        this.setLifetime(tag.getInt("Lifetime"));
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
