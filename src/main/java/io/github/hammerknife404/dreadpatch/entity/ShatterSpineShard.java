package io.github.hammerknife404.dreadpatch.entity;

import com.iafenvoy.iceandfire.registry.IafParticles;
import io.github.hammerknife404.dreadpatch.magic.DragonBreath;
import io.github.hammerknife404.dreadpatch.registry.ModRegistries;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.ThrowableProjectile;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import io.redspace.ironsspellbooks.particle.BlastwaveParticleOptions;
import org.joml.Vector3f;

/**
 * A shard of Shatter-Spine falling from its burst. Where it lands (on someone or the ground) it cuts
 * everyone within {@link DragonBreath#SHARD_RADIUS} blocks its thrower may harm, lightly, and leaves
 * Withering Rime. Each shard counts on its own (hit cooldowns are cleared for it), so a rain of them
 * on one spot adds up. Drawn as a pale ice spike ({@code ShatterSpineRenderer}) trailing cold dread fire.
 */
public class ShatterSpineShard extends ThrowableProjectile {
    private static final ParticleOptions ICE_SHARDS = new BlockParticleOption(ParticleTypes.BLOCK, Blocks.PACKED_ICE.defaultBlockState());

    /** The stage of the dragon that loosed it, which sets how hard it cuts. */
    private int stage = 5;

    public ShatterSpineShard(EntityType<? extends ShatterSpineShard> type, Level level) {
        super(type, level);
    }

    void setStage(int stage) {
        this.stage = stage;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
    }

    @Override
    protected double getDefaultGravity() {
        return 0.04;
    }

    @Override
    public void tick() {
        super.tick();
        if (this.level().isClientSide) {
            if (this.tickCount % 2 == 0) {
                this.level().addParticle(ModRegistries.RIME_FLAME.get(), this.getX(), this.getY(), this.getZ(), 0.0, 0.0, 0.0);
            }
            if (this.tickCount % 3 == 0) {
                this.level().addParticle(ParticleTypes.SNOWFLAKE, this.getX(), this.getY(), this.getZ(), 0.0, 0.0, 0.0);
            }
        } else if (this.tickCount > 200) {
            this.discard();
        }
    }

    @Override
    protected boolean canHitEntity(Entity target) {
        return super.canHitEntity(target) && !(target instanceof ShatterSpineShard) && !(target instanceof ShatterSpineCore)
                && !(this.getOwner() != null && !RimefireCrown.canHarm(this.getOwner(), target));
    }

    @Override
    protected void onHit(HitResult hit) {
        super.onHit(hit);
        if (this.level() instanceof ServerLevel level && !this.isRemoved()) {
            Entity owner = this.getOwner();
            DamageSource source = ModRegistries.damageSource(level, ModRegistries.SHATTER_SPINE_DAMAGE, this, owner);
            float damage = DragonBreath.shardDamage(this.stage, RimefrostDragon.attackDamageIce());
            Entity struck = hit instanceof EntityHitResult entityHit ? entityHit.getEntity() : null;
            // Measured to the nearest point of each one's box: a shard striking a golem's head is 2.7 blocks from its feet.
            for (LivingEntity victim : level.getEntitiesOfClass(LivingEntity.class, this.getBoundingBox().inflate(DragonBreath.SHARD_RADIUS),
                    e -> e.isAlive() && e != owner && RimefireCrown.canHarm(owner, e)
                            && (e == struck || e.getBoundingBox().distanceToSqr(this.position()) <= DragonBreath.SHARD_RADIUS * DragonBreath.SHARD_RADIUS))) {
                victim.invulnerableTime = 0;
                if (victim.hurt(source, damage) && victim.isAlive()) {
                    victim.addEffect(new MobEffectInstance(ModRegistries.WITHERING_RIME, DragonBreath.RIME_TICKS), owner);
                }
            }
            level.sendParticles(new BlastwaveParticleOptions(new Vector3f(0.25F, 0.75F, 1.0F), (float) (DragonBreath.SHARD_RADIUS * 1.4)),
                    this.getX(), this.getY() + 0.1, this.getZ(), 1, 0.0, 0.0, 0.0, 0.0);
            level.sendParticles(ICE_SHARDS, this.getX(), this.getY(), this.getZ(), 25, 0.5, 0.3, 0.5, 0.2);
            level.sendParticles(ModRegistries.RIME_SMOKE.get(), this.getX(), this.getY(), this.getZ(), 6, 0.5, 0.2, 0.5, 0.04);
            level.sendParticles(IafParticles.DREAD_TORCH.get(), this.getX(), this.getY(), this.getZ(), 6, 0.4, 0.2, 0.4, 0.03);
            level.playSound(null, this.getX(), this.getY(), this.getZ(), SoundEvents.GLASS_BREAK, SoundSource.HOSTILE, 0.8F, 1.2F);
            this.discard();
        }
    }

    /** Never saved: it lasts seconds, and one that strays into a chunk that stops ticking would otherwise wait there for good. */
    @Override
    public boolean shouldBeSaved() {
        return false;
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putInt("Stage", this.stage);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        if (tag.contains("Stage")) {
            this.stage = tag.getInt("Stage");
        }
    }
}
