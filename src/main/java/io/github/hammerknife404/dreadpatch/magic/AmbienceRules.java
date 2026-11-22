package io.github.hammerknife404.dreadpatch.magic;

import java.util.function.IntUnaryOperator;

/**
 * The Dreadlands' ice cracking, paced (0.54.1, the owner, after 0.54.0's one in 240 a tick: "The cracking
 * loop is too quick. Every 30ish to 120ish, can go in several bursts of two or three back to back"): a crack
 * every 30-120 s, half the time on its own and half the time a burst of two or three, close together.
 * {@code roll} gives 0 to n − 1 for an n.
 */
public final class AmbienceRules {
    /** Ticks from one crack (or burst) to the next: 30 to 120 s. */
    public static final int CRACK_MIN = 600;
    public static final int CRACK_MAX = 2400;
    /** Ticks between the cracks of a burst. */
    public static final int GAP_MIN = 3;
    public static final int GAP_MAX = 10;
    /** A burst's cracks land within this many blocks of its first. */
    public static final int BURST_SPREAD = 3;

    private AmbienceRules() {
    }

    public static int crackDelay(IntUnaryOperator roll) {
        return CRACK_MIN + roll.applyAsInt(CRACK_MAX - CRACK_MIN + 1);
    }

    /** How many cracks this time: one half the time, else two (two in three of those) or three. */
    public static int burstSize(IntUnaryOperator roll) {
        int r = roll.applyAsInt(6);
        return r < 3 ? 1 : r < 5 ? 2 : 3;
    }

    public static int burstGap(IntUnaryOperator roll) {
        return GAP_MIN + roll.applyAsInt(GAP_MAX - GAP_MIN + 1);
    }
}
