package io.github.hammerknife404.dreadpatch.magic;

/**
 * Game-free: what one tick of the Dread Queen's own Withering Rime takes (and gives her): a share of
 * the victim's max health set by the difficulty, at least {@link #FLOOR} (so a player still withers
 * as under anyone's Rime) and at most {@link #CAP} (so a dragon or a 320-health golem doesn't make
 * her unkillable).
 */
public final class RimeLeech {
    public static final float FLOOR = 1.0F;
    public static final float CAP = 10.0F;
    /** The share of the victim's max health a tick takes, on Easy (and Peaceful), Normal and Hard. */
    public static final float EASY = 0.01F;
    public static final float NORMAL = 0.015F;
    public static final float HARD = 0.02F;

    private RimeLeech() {
    }

    public static float perTick(float maxHealth, float share) {
        return Math.min(CAP, Math.max(FLOOR, maxHealth * share));
    }
}
