package io.github.hammerknife404.dreadpatch.entity;

import com.iafenvoy.iceandfire.registry.IafParticles;
import io.github.hammerknife404.dreadpatch.magic.DragonBreath;
import io.github.hammerknife404.dreadpatch.registry.ModRegistries;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.AbstractHurtingProjectile;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import io.redspace.ironsspellbooks.particle.BlastwaveParticleOptions;
import org.joml.Vector3f;
import org.jetbrains.annotations.Nullable;

/**
 * Shatter-Spine, Syndrigossa's charge in place of the ice dragon's: a core of frost-rimed bone and
 * black dread-glass that flies to a point {@link DragonBreath#AIRBURST_HEIGHT} blocks over its target
 * and bursts there into {@link ShatterSpineShard}s raining over the ground below. With nothing to aim
 * at it flies on until it hits something. It also bursts on anything it hits, or after
 * {@link #LIFETIME} ticks. Drawn as a burr of dark ice spikes ({@code ShatterSpineRenderer}), trailing
 * cold dread fire and soot, and its burst rings out (Iron's blastwave).
 */
public class ShatterSpineCore extends AbstractHurtingProjectile {
    static final int LIFETIME = 80;
    /** Horizontal speed range of the shards it bursts into: a spread of about 2 to 6 blocks over a 9-block fall. */
    static final double SHARD_SPEED_MIN = 0.06;
    static final double SHARD_SPEED_MAX = 0.22;

    private static final ParticleOptions ICE_SHARDS = new BlockParticleOption(ParticleTypes.BLOCK, Blocks.PACKED_ICE.defaultBlockState());
    private static final ParticleOptions BONE_DUST = new BlockParticleOption(ParticleTypes.BLOCK, Blocks.BONE_BLOCK.defaultBlockState());

    @Nullable
    private Vec3 aim;
    /** Ticks until it bursts by itself, if set (a summoned core's {@code Fuse}); -1 for none. */
    private int fuse = -1;
    private int shardDamageStage = 5;

    public ShatterSpineCore(EntityType<? extends ShatterSpineCore> type, Level level) {
        super(type, level);
    }

    /** From {@code from} toward {@code aim} if there is one, else along {@code direction}. */
    public static ShatterSpineCore launch(Level level, RimefrostDragon dragon, Vec3 from, Vec3 direction, @Nullable Vec3 aim) {
        Vec3 heading = aim != null ? aim.subtract(from) : direction;
        ShatterSpineCore core = new ShatterSpineCore(ModRegistries.SHATTER_SPINE.get(), level);
        core.setOwner(dragon);
        core.setPos(from);
        core.accelerationPower = 0.1;
        core.setDeltaMovement(heading.lengthSqr() < 1.0E-6 ? Vec3.ZERO : heading.normalize().scale(core.accelerationPower));
        core.aim = aim;
        core.shardDamageStage = dragon.getDragonStage();
        return core;
    }

    /** Where to burst over a dragon's target: high over its head. */
    @Nullable
    public static Vec3 aimOver(@Nullable LivingEntity target) {
        return target == null ? null : target.position().add(0.0, DragonBreath.AIRBURST_HEIGHT, 0.0);
    }

    @Override
    public void tick() {
        super.tick();
        if (this.level().isClientSide) {
            this.level().addParticle(ModRegistries.RIME_SMOKE.get(), this.getX(), this.getY(0.5), this.getZ(), 0.0, 0.0, 0.0);
            this.level().addParticle(ModRegistries.RIME_FLAME.get(), this.getRandomX(0.5), this.getY(0.5), this.getRandomZ(0.5), 0.0, 0.0, 0.0);
            if (this.tickCount % 2 == 0) {
                this.level().addParticle(BONE_DUST, this.getRandomX(0.6), this.getY(0.5), this.getRandomZ(0.6), 0.0, 0.0, 0.0);
            }
            return;
        }
        if (this.fuse > 0 && --this.fuse == 0) {
            this.burst();
            return;
        }
        if (this.aim != null) {
            Vec3 toAim = this.aim.subtract(this.position());
            // Close enough, or flown past it.
            if (toAim.lengthSqr() < 4.0 || toAim.dot(this.getDeltaMovement()) < 0.0) {
                this.burst();
                return;
            }
        }
        if (this.tickCount >= LIFETIME) {
            this.burst();
        }
    }

    @Override
    protected boolean canHitEntity(Entity target) {
        return super.canHitEntity(target) && !(target instanceof ShatterSpineShard)
                && !(this.getOwner() != null && !RimefireCrown.canHarm(this.getOwner(), target));
    }

    @Override
    protected void onHit(HitResult hit) {
        super.onHit(hit);
        if (!this.level().isClientSide) {
            this.burst();
        }
    }

    /** Bursts into shards flung outward and down, and is gone. */
    void burst() {
        if (this.level() instanceof ServerLevel level && !this.isRemoved()) {
            int shards = DragonBreath.SHARDS_MIN + this.random.nextInt(DragonBreath.SHARDS_MAX - DragonBreath.SHARDS_MIN + 1);
            for (int i = 0; i < shards; i++) {
                double angle = this.random.nextDouble() * Math.PI * 2.0;
                double speed = SHARD_SPEED_MIN + this.random.nextDouble() * (SHARD_SPEED_MAX - SHARD_SPEED_MIN);
                ShatterSpineShard shard = new ShatterSpineShard(ModRegistries.SHATTER_SPINE_SHARD.get(), level);
                shard.setOwner(this.getOwner());
                shard.setStage(this.shardDamageStage);
                shard.setPos(this.getX(), this.getY(), this.getZ());
                shard.setDeltaMovement(Math.cos(angle) * speed, -0.05, Math.sin(angle) * speed);
                level.addFreshEntity(shard);
            }
            level.sendParticles(new BlastwaveParticleOptions(new Vector3f(0.25F, 0.75F, 1.0F), 6.0F), this.getX(), this.getY(), this.getZ(), 1, 0.0, 0.0, 0.0, 0.0);
            level.sendParticles(ModRegistries.RIME_FLAME.get(), this.getX(), this.getY(), this.getZ(), 40, 0.6, 0.6, 0.6, 0.35);
            level.sendParticles(ModRegistries.RIME_SMOKE.get(), this.getX(), this.getY(), this.getZ(), 20, 0.8, 0.6, 0.8, 0.12);
            level.sendParticles(ICE_SHARDS, this.getX(), this.getY(), this.getZ(), 60, 1.0, 1.0, 1.0, 0.3);
            level.sendParticles(BONE_DUST, this.getX(), this.getY(), this.getZ(), 30, 0.6, 0.6, 0.6, 0.2);
            level.playSound(null, this.getX(), this.getY(), this.getZ(), SoundEvents.GLASS_BREAK, SoundSource.HOSTILE, 3.0F, 0.5F);
            level.playSound(null, this.getX(), this.getY(), this.getZ(), SoundEvents.GENERIC_EXPLODE.value(), SoundSource.HOSTILE, 1.0F, 1.4F);
        }
        this.discard();
    }

    @Override
    protected boolean shouldBurn() {
        return false;
    }

    @Override
    protected ParticleOptions getTrailParticle() {
        return IafParticles.DREAD_TORCH.get();
    }

    @Override
    public boolean isPickable() {
        return false;
    }

    /** Never saved: it lasts seconds, and one that strays into a chunk that stops ticking would otherwise wait there for good. */
    @Override
    public boolean shouldBeSaved() {
        return false;
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putInt("Fuse", this.fuse);
        tag.putInt("Stage", this.shardDamageStage);
        if (this.aim != null) {
            tag.putDouble("AimX", this.aim.x);
            tag.putDouble("AimY", this.aim.y);
            tag.putDouble("AimZ", this.aim.z);
        }
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        if (tag.contains("Fuse")) {
            this.fuse = tag.getInt("Fuse");
        }
        if (tag.contains("Stage")) {
            this.shardDamageStage = tag.getInt("Stage");
        }
        if (tag.contains("AimY")) {
            this.aim = new Vec3(tag.getDouble("AimX"), tag.getDouble("AimY"), tag.getDouble("AimZ"));
        }
    }
}
