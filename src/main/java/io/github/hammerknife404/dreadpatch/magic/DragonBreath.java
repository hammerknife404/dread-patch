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

    private static int clampStage(int stage) {
        return Math.max(1, Math.min(5, stage));
    }
}
