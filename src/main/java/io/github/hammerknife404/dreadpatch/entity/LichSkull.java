package io.github.hammerknife404.dreadpatch.entity;

import com.iafenvoy.iceandfire.entity.DreadLichSkullEntity;
import io.github.hammerknife404.dreadpatch.magic.SkullRules;
import io.github.hammerknife404.dreadpatch.registry.ModRegistries;
import io.redspace.ironsspellbooks.damage.DamageSources;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

/**
 * A skull of the Lich Skull spell (0.98.5; {@link SkullRules}): Ice and Fire's lich skull, drawn by its renderer, but
 * steered by us at the one target it was given, so a volley spreads over different ones. Ice and Fire's steers at its
 * owner's target, the same for every skull, so it's given no owner: its caster is kept here, and its hit is ours, a
 * spell's damage (Iron's, so spell power and resistances count) at its caster's credit. It passes by its caster and
 * what's on their side (their summons, their allies, friendly fire as Iron's has it), and goes when it hits anything
 * or after {@link SkullRules#LIFETIME}.
 */
public class LichSkull extends DreadLichSkullEntity {
    @Nullable
    private UUID casterId;
    @Nullable
    private LivingEntity caster;
    @Nullable
    private LivingEntity seek;
    private float damage = SkullRules.DAMAGE;

    public LichSkull(EntityType<? extends LichSkull> type, Level level) {
        super(type, level);
        this.setNoGravity(true);
    }

    public LichSkull(Level level, LivingEntity caster, @Nullable LivingEntity seek, float damage) {
        this(ModRegistries.LICH_SKULL.get(), level);
        this.caster = caster;
        this.casterId = caster.getUUID();
        this.seek = seek;
        this.damage = damage;
    }

    @Nullable
    private LivingEntity caster() {
        if (this.caster == null && this.casterId != null && this.level() instanceof ServerLevel level
                && level.getEntity(this.casterId) instanceof LivingEntity found) {
            this.caster = found;
        }
        return this.caster;
    }

    @Override
    public void tick() {
        super.tick();
        if (this.level().isClientSide) {
            return;
        }
        if (this.tickCount > SkullRules.LIFETIME) {
            this.discard();
            return;
        }
        if (this.seek != null && this.seek.isAlive() && this.seek.level() == this.level()) {
            Vec3 toward = this.seek.getBoundingBox().getCenter().subtract(this.position());
            double turn = SkullRules.turn(toward.length());
            Vec3 heading = this.getDeltaMovement().normalize().scale(1.0 - turn).add(toward.normalize().scale(turn));
            this.setDeltaMovement(level(heading.normalize()).scale(SkullRules.SPEED));
        }
    }

    /**
     * A heading kept off the straight up and down (0.98.13, a review): Ice and Fire's skull discards itself, unhit, once
     * its speed across falls under 0.1 a tick, so a skull steered at a foe almost straight below or above vanished.
     */
    static Vec3 level(Vec3 heading) {
        double across = Math.sqrt(heading.x * heading.x + heading.z * heading.z);
        if (across >= SkullRules.MIN_ACROSS || across < 1.0E-6) {
            return across < 1.0E-6 ? new Vec3(SkullRules.MIN_ACROSS, Math.signum(heading.y) * Math.sqrt(1.0 - SkullRules.MIN_ACROSS * SkullRules.MIN_ACROSS), 0.0) : heading;
        }
        double scale = SkullRules.MIN_ACROSS / across;
        double up = Math.signum(heading.y) * Math.sqrt(1.0 - SkullRules.MIN_ACROSS * SkullRules.MIN_ACROSS);
        return new Vec3(heading.x * scale, up, heading.z * scale);
    }

    @Override
    protected boolean canHitEntity(Entity target) {
        LivingEntity caster = this.caster();
        // Not its caster's mount, nor anything they ride with, nor their pets, nor a part of one (0.98.13, a review: with
        // no owner, vanilla's own checks for these were gone, and a skull cast from horseback struck the horse).
        Entity whole = target instanceof net.neoforged.neoforge.entity.PartEntity<?> part ? part.getParent() : target;
        if (caster != null && (caster.isPassengerOfSameVehicle(whole)
                || whole instanceof net.minecraft.world.entity.OwnableEntity owned && caster.equals(owned.getOwner())
                || whole instanceof com.iafenvoy.iceandfire.entity.MultipartPartEntity iafPart && iafPart.getParent() != null
                && (caster.isPassengerOfSameVehicle(iafPart.getParent()) || iafPart.getParent() instanceof net.minecraft.world.entity.OwnableEntity o && caster.equals(o.getOwner())))) {
            return false;
        }
        if (!target.canBeHitByProjectile()) {
            return false;
        }
        if (target == caster || caster == null && this.casterId != null && this.casterId.equals(target.getUUID())) {
            return false;
        }
        if (caster != null && (SummonedCourt.serves(target, caster) || target.isAlliedTo(caster)
                || DamageSources.isFriendlyFireBetween(caster, target))) {
            return false;
        }
        return target.isAlive() && target.isPickable() && !target.isSpectator() && !(target instanceof LichSkull);
    }

    @Override
    protected void onHitEntity(EntityHitResult hit) {
        if (this.level().isClientSide) {
            return;
        }
        Entity target = hit.getEntity();
        LivingEntity caster = this.caster();
        DamageSources.applyDamage(target, this.damage, ModRegistries.LICH_SKULL_SPELL.get().getDamageSource(this, caster));
        this.burst();
        this.discard();
    }

    @Override
    protected void onHitBlock(BlockHitResult hit) {
        if (!this.level().isClientSide) {
            this.burst();
            this.discard();
        }
    }

    private void burst() {
        if (this.level() instanceof ServerLevel level) {
            level.sendParticles(ParticleTypes.SOUL, this.getX(), this.getY(), this.getZ(), 8, 0.2, 0.2, 0.2, 0.03);
            level.sendParticles(ModRegistries.RIME_SMOKE_SMALL.get(), this.getX(), this.getY(), this.getZ(), 6, 0.2, 0.2, 0.2, 0.02);
        }
    }

    /** Not saved: a skull in flight is gone on a reload. */
    @Override
    public boolean shouldBeSaved() {
        return false;
    }
}
