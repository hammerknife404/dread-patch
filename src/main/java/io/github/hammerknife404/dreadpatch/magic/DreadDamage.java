package io.github.hammerknife404.dreadpatch.magic;

/**
 * Her magic's share-of-max-health damage: what makes her as dangerous to a 20-health player as her
 * plain damage alone wouldn't. It's capped against everything, since 0.14.0: uncapped, her bolt took
 * 75 off a 500-health warden and 150 off a 1,000-health dragon, and she cut through wardens in
 * seconds (until then the cap held only against {@code #c:bosses}).
 */
public final class DreadDamage {
    /** The most a share-of-max-health hit takes off anything. */
    public static final float CAP = 20.0F;

    private DreadDamage() {
    }

    public static float maxHealthShare(float maxHealth, float fraction) {
        return Math.min(CAP, Math.max(0.0F, maxHealth) * Math.max(0.0F, fraction));
    }
}
