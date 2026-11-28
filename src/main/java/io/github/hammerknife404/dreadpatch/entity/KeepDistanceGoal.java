package io.github.hammerknife404.dreadpatch.entity;

import io.github.hammerknife404.dreadpatch.magic.LeapRules;

import io.github.hammerknife404.dreadpatch.magic.Spacing;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.util.DefaultRandomPos;
import net.minecraft.world.phys.Vec3;

import java.util.EnumSet;

/**
 * How she moves in a fight, in place of Ice and Fire's {@code MeleeAttackGoal} (which walked her
 * straight into melee): she keeps her distance like Iron's necromancer and Ice and Fire's Dread
 * Lich ({@link Spacing}), circling at casting range, giving ground when pressed and breaking away
 * when cornered: by Barrow-Step when she can, else on foot. Her sword is for whoever still reaches her. Her spells and her court act from
 * {@link DreadQueen#customServerAiStep}, on whatever she's targeting.
 */
final class KeepDistanceGoal extends Goal {
    /** Her speed while breaking away (a multiple of her movement speed), how far she runs, and how long until she can again. */
    static final double FLEE_SPEED = 1.2;
    static final int FLEE_DISTANCE = 16;
    static final int FLEE_COOLDOWN = 60;
    /** The longest a single flight lasts before she turns to face her target again. */
    static final int FLEE_TICKS = 40;
    /** Ticks between sword swings, as for Ice and Fire's melee goal. */
    static final int MELEE_COOLDOWN = 20;
    /** She holds her ground only once she's had her target in sight this long. */
    static final int SIGHT_TICKS = 5;
    /** Strafing: forward speed while giving ground and while circling, sideways speed. */
    static final float BACK_OFF = -0.6F;
    static final float CIRCLE = 0.1F;
    static final float SIDEWAYS = 0.6F;

    private final DreadQueen queen;
    private int seeTime;
    private int strafeTime;
    private boolean clockwise;
    private int fleeCooldown;
    private int fleeing;
    private int meleeCooldown;
    /** Ticks until she may hop again ({@link LeapRules#hop}). */
    private int hopCooldown = LeapRules.HOP_MIN;

    KeepDistanceGoal(DreadQueen queen) {
        this.queen = queen;
        this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        LivingEntity target = this.queen.getTarget();
        // In the saddle her dragon moves her; she only casts (DreadQueen.customServerAiStep).
        return target != null && target.isAlive() && this.queen.canAttack(target) && !this.queen.isPassenger();
    }

    @Override
    public void start() {
        this.queen.setAggressive(true);
    }

    @Override
    public void stop() {
        this.queen.setAggressive(false);
        this.seeTime = 0;
        this.fleeing = 0;
        this.queen.getMoveControl().strafe(0.0F, 0.0F);
        this.queen.getNavigation().stop();
    }

    @Override
    public boolean requiresUpdateEveryTick() {
        return true;
    }

    @Override
    public void tick() {
        LivingEntity target = this.queen.getTarget();
        if (target == null) {
            return;
        }
        if (this.fleeCooldown > 0) {
            this.fleeCooldown--;
        }
        if (this.meleeCooldown > 0) {
            this.meleeCooldown--;
        }
        if (this.hopCooldown > 0) {
            this.hopCooldown--;
        }
        boolean seen = this.queen.getSensing().hasLineOfSight(target);
        this.seeTime = seen ? Math.max(this.seeTime, 0) + 1 : Math.min(this.seeTime, 0) - 1;
        this.queen.getLookControl().setLookAt(target, 30.0F, 30.0F);
        if (this.fleeing > 0) {
            this.fleeing--;
            if (!this.queen.getNavigation().isDone()) {
                this.swingIfReached(target, seen);
                return;
            }
            this.fleeing = 0;
        }
        switch (Spacing.move(this.queen.distanceTo(target), this.seeTime >= SIGHT_TICKS, this.fleeCooldown == 0)) {
            case FLEE -> this.flee(target);
            case BACK_OFF -> {
                this.strafe(target, BACK_OFF);
                this.maybeHop(target, true);
            }
            case CIRCLE -> {
                this.strafe(target, CIRCLE);
                this.maybeHop(target, false);
            }
            case APPROACH -> {
                if (this.queen.tickCount % 5 == 0 || this.queen.getNavigation().isDone()) {
                    this.queen.getNavigation().moveTo(target, 1.0);
                }
            }
        }
        this.swingIfReached(target, seen);
    }

    /** Barrow-Steps away from her target, or runs; where there's nowhere to run, gives ground instead. */
    private void flee(LivingEntity target) {
        this.fleeCooldown = FLEE_COOLDOWN;
        if (this.queen.barrowStep(target)) {
            return;
        }
        Vec3 away = DefaultRandomPos.getPosAway(this.queen, FLEE_DISTANCE, 7, target.position());
        if (away != null && this.queen.getNavigation().moveTo(away.x, away.y, away.z, FLEE_SPEED)) {
            this.fleeing = FLEE_TICKS;
        } else {
            this.strafe(target, BACK_OFF);
        }
    }

    /** Circles her target facing it, now and then changing direction. */
    private void strafe(LivingEntity target, float forward) {
        this.queen.getNavigation().stop();
        if (++this.strafeTime >= 25 && this.queen.getRandom().nextFloat() < 0.1F) {
            this.clockwise = !this.clockwise;
            this.strafeTime = 0;
        }
        this.queen.getMoveControl().strafe(forward, this.clockwise ? SIDEWAYS : -SIDEWAYS);
        this.queen.lookAt(target, 30.0F, 30.0F);
    }

    /**
     * Now and then, circling or giving ground, she hops aside the way she's circling (and back, giving ground), where
     * there's room and ground to land on (0.98.0, {@link LeapRules#hop}).
     */
    private void maybeHop(LivingEntity target, boolean back) {
        if (this.hopCooldown > 0 || !this.queen.onGround() || this.queen.isInWater()
                || this.queen.getRandom().nextInt(LeapRules.HOP_ONE_IN) != 0) {
            return;
        }
        double[] way = LeapRules.hop(target.getX() - this.queen.getX(), target.getZ() - this.queen.getZ(), this.clockwise, back);
        Vec3 toward = new Vec3(way[0], 0.0, way[1]);
        if (toward.lengthSqr() < 0.5) {
            return;
        }
        Vec3 leap = DreadLeaps.hop(this.queen, toward, LeapRules.HOP_DISTANCE);
        if (leap == null) {
            // Not that way: the other, next time.
            this.clockwise = !this.clockwise;
            return;
        }
        this.hopCooldown = LeapRules.HOP_MIN + this.queen.getRandom().nextInt(LeapRules.HOP_MAX - LeapRules.HOP_MIN + 1);
        DreadLeaps.launch(this.queen, leap, target.position().subtract(this.queen.position()));
    }

    private void swingIfReached(LivingEntity target, boolean seen) {
        if (this.meleeCooldown == 0 && seen && this.queen.isWithinMeleeAttackRange(target)) {
            this.meleeCooldown = MELEE_COOLDOWN;
            this.queen.swing(InteractionHand.MAIN_HAND);
            this.queen.doHurtTarget(target);
        }
    }
}
