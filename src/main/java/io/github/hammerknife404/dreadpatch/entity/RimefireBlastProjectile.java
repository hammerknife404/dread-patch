package io.github.hammerknife404.dreadpatch.entity;

import io.github.hammerknife404.dreadpatch.registry.ModRegistries;
import io.redspace.ironsspellbooks.damage.DamageSources;
import io.redspace.ironsspellbooks.entity.spells.AbstractConeProjectile;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;

/**
 * Rimefire Blast's cone (0.51.0): Iron's Cone of Cold's ({@link AbstractConeProjectile}: it follows the
 * caster's look, its four parts reaching some 7 blocks), breathing cold cyan dread fire and soot as
 * the Rimefrost dragons do, at a caster's scale. Each hit, Rimefrost damage, and Withering Rime
 * renewed on whoever it touches.
 */
public class RimefireBlastProjectile extends AbstractConeProjectile {
    public static final int RIME_TICKS = 60;

    public RimefireBlastProjectile(EntityType<? extends AbstractConeProjectile> type, Level level) {
        super(type, level);
    }

    public RimefireBlastProjectile(Level level, LivingEntity caster) {
        super(ModRegistries.RIMEFIRE_BLAST_PROJECTILE.get(), level, caster);
    }

    @Override
    public void spawnParticles() {
        Entity owner = this.getOwner();
        if (!this.level().isClientSide || owner == null) {
            return;
        }
        Vec3 look = owner.getLookAngle().normalize();
        Vec3 from = owner.position().add(look.scale(1.2)).add(0.0, owner.getEyeHeight() * 0.85, 0.0);
        for (int i = 0; i < 8; i++) {
            double speed = 0.15 + this.random.nextDouble() * 0.55;
            Vec3 spread = new Vec3(this.random.nextDouble() * 1.6 - 0.8, this.random.nextDouble() * 1.6 - 0.8, this.random.nextDouble() * 1.6 - 0.8).normalize();
            Vec3 motion = look.scale(3.0).add(spread).normalize().scale(speed);
            float pick = this.random.nextFloat();
            this.level().addParticle(pick < 0.55F ? ModRegistries.RIME_FLAME_SMALL.get() : pick < 0.85F ? ModRegistries.RIME_SMOKE_SMALL.get() : ParticleTypes.SNOWFLAKE,
                    from.x + this.random.nextGaussian() * 0.1, from.y + this.random.nextGaussian() * 0.1, from.z + this.random.nextGaussian() * 0.1,
                    motion.x, motion.y, motion.z);
        }
    }

    @Override
    protected void onHitEntity(EntityHitResult hit) {
        Entity target = hit.getEntity();
        boolean hurt = DamageSources.applyDamage(target, this.damage, ModRegistries.RIMEFIRE_BLAST_SPELL.get().getDamageSource(this, this.getOwner()));
        if (hurt && target instanceof LivingEntity living) {
            living.addEffect(new MobEffectInstance(ModRegistries.WITHERING_RIME, RIME_TICKS), this.getOwner());
        }
    }
}
