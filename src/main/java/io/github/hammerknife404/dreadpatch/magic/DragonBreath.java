package io.github.hammerknife404.dreadpatch.magic;

/**
 * Game-free: Syndrigossa's numbers. Her breath hits as hard as an ice dragon's of her stage (Ice and
 * Fire's own formula, stage × its {@code attackDamageIce}, 2.1 in the owner's pack), each target at
 * most twice a second (the hit cooldown), leaving Withering Rime instead of frost. Every breath hit
 * also siphons {@link #SIPHON}, past armor, into her (the owner's 0.5). Her charge, Shatter-Spine,
 * bursts {@link #AIRBURST_HEIGHT} blocks over its target into {@link #SHARDS_MIN}-{@link #SHARDS_MAX}
 * shards, each half a breath hit.
 */
public final class DragonBreath {
    public static final float SIPHON = 0.5F;
    /** Withering Rime from a breath hit or a shard. */
    public static final int RIME_TICKS = 60;
    public static final double AIRBURST_HEIGHT = 9.0;
    public static final int SHARDS_MIN = 5;
    public static final int SHARDS_MAX = 7;
    /** How far from where a shard lands it cuts. */
    public static final double SHARD_RADIUS = 1.5;
    /** Her breath is a cone from her head, this wide either side of its axis (the owner asked for wider than Ice and Fire's stream). */
    public static final double CONE_HALF_ANGLE = Math.toRadians(20.0);
    /** The furthest it reaches past where she aims it, and at all. */
    public static final double CONE_OVERSHOOT = 2.0;
    public static final double CONE_MAX_LENGTH = 30.0;
    /** Ticks of breathing (Ice and Fire's {@code burnProgress}) for the cone to reach its full length; Ice and Fire's stream takes 40. */
    public static final int CONE_GROWTH_TICKS = 10;
    /**
     * How far one of her breath particles goes for each block a tick it's sent at: about 16, slowing
     * by 4-5% a tick over its 22-40 ticks ({@code RimeBreathParticle}).
     */
    public static final double PARTICLE_TRAVEL = 16.0;

    private DragonBreath() {
    }

    /** One breath hit at a dragon stage (1-5), with Ice and Fire's ice breath multiplier. */
    public static float breathDamage(int stage, double attackDamageIce) {
        return (float) (clampStage(stage) * Math.max(0.0, attackDamageIce));
    }

    /** How far from where the breath lands it reaches: 1.5 blocks at stage 1, 3.5 at stage 5. */
    public static double breathRadius(int stage) {
        return 1.0 + 0.5 * clampStage(stage);
    }

    public static float shardDamage(int stage, double attackDamageIce) {
        return 0.5F * breathDamage(stage, attackDamageIce);
    }

    /**
     * Whether a point at {@code (dx, dy, dz)} from the cone's apex lies in a cone along the unit
     * vector {@code axis}, {@code length} long, {@code pad} wider all round (a body's half-width).
     */
    public static boolean inCone(double dx, double dy, double dz, double ax, double ay, double az, double length, double pad) {
        double along = dx * ax + dy * ay + dz * az;
        if (along <= 0.0 || along > length + pad) {
            return false;
        }
        double across2 = dx * dx + dy * dy + dz * dz - along * along;
        double reach = along * Math.tan(CONE_HALF_ANGLE) + pad;
        return across2 <= reach * reach;
    }

    /**
     * How far the cone reaches this tick: grown over {@link #CONE_GROWTH_TICKS} of breathing, up to just
     * past its aim. (Grown over Ice and Fire's 40, a short burst only ever showed its first stretch.)
     */
    public static double coneLength(double toAim, int burnProgress) {
        double full = Math.min(CONE_MAX_LENGTH, toAim + CONE_OVERSHOOT);
        return full * Math.max(0.3, Math.min(1.0, burnProgress / (double) CONE_GROWTH_TICKS));
    }

    /** The speed (blocks a tick) to send a breath particle at for it to carry {@code distance} blocks. */
    public static double particleSpeed(double distance) {
        return Math.max(0.0, distance) / PARTICLE_TRAVEL;
    }

    private static int clampStage(int stage) {
        return Math.max(1, Math.min(5, stage));
    }
}
