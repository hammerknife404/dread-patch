package io.github.hammerknife404.dreadpatch.magic;

/**
 * Her magic's share-of-max-health damage: what makes her as dangerous to a 1,000-health dragon or a
 * 320-health golem as to a 20-health player. Against bosses ({@code #c:bosses}) the share is capped,
 * or a player casting her bolt would take hundreds off one with every cast.
 */
public final class DreadDamage {
    /** The most a share-of-max-health hit takes off a boss. */
    public static final float BOSS_CAP = 20.0F;

    private DreadDamage() {
    }

    public static float maxHealthShare(float maxHealth, float fraction, boolean boss) {
        float share = Math.max(0.0F, maxHealth) * Math.max(0.0F, fraction);
        return boss ? Math.min(share, BOSS_CAP) : share;
    }
}
