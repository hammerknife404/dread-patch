package io.github.hammerknife404.dreadpatch.magic;

/**
 * Game-free: the Glacial Plains' Rimefrost dragon nests (0.84.0, the owner: "This is where wild rimefrost dragons also
 * make their nests. Their nests consists of any nugget metal from vanilla plus blacksteel"; asked, hoard piles, a dragon
 * of stage 2-5 with now and then an ancient one; 0.84.2, the owner: blacksteel removed, "Make the rime dragon roosts more
 * sparse").
 * <ul>
 * <li>A nest: one chunk in {@link #ONE_IN} of the Glacial Plains, only up on the ice (from {@link #MIN_Y}), a bowl
 * {@link #MIN_RADIUS} to {@link #MIN_RADIUS} + {@link #RADII} - 1 blocks across, heaped with hoard piles: deepest in
 * the middle ({@link #layers}).</li>
 * <li>Its metal, each pile ({@link #metal}): gold, silver, copper, iron.</li>
 * <li>Its dragon's age ({@link #ageDays}): stage 2-5, or (one in twenty) ancient.</li>
 * </ul>
 */
public final class NestRules {
    public static final int ONE_IN = 400;  // 160 until 0.84.2
    public static final int MIN_Y = 170;
    public static final int MIN_RADIUS = 6;
    public static final int RADII = 4;
    /** How uneven the ice may be under a nest: its rim's highest and lowest columns at most this far apart. */
    public static final int MAX_UNEVEN = 6;
    /** How far from its nest a pile's taker wakes its dragon. */
    public static final int GUARD_RANGE = 48;

    public enum Metal { GOLD, SILVER, COPPER, IRON }

    /** Weights for gold, silver, copper, iron. */
    static final int[] METAL_WEIGHTS = {34, 21, 21, 24};
    /** The dragon's stage, by weight: stage 2, 3, 4, 5, ancient. */
    static final int[] AGE_WEIGHTS = {20, 30, 28, 17, 5};
    /** Ice and Fire's stages: 25 days each from stage 2 at 25 days; an ancient one is 125 or more. */
    public static final int ANCIENT_DAYS = 125;

    private NestRules() {
    }

    /** A pile's metal from a roll in [0, 1). */
    public static Metal metal(double roll) {
        int kinds = METAL_WEIGHTS.length;
        int sum = 0;
        for (int i = 0; i < kinds; i++) {
            sum += METAL_WEIGHTS[i];
        }
        double at = roll * sum;
        for (int i = 0; i < kinds; i++) {
            at -= METAL_WEIGHTS[i];
            if (at < 0) {
                return Metal.values()[i];
            }
        }
        return Metal.values()[kinds - 1];
    }

    /** A pile's layers (1-8): deepest in the bowl's middle, a little uneven ({@code roll} in [0, 1)). */
    public static int layers(double distance, int radius, double roll) {
        double heart = 1.0 - Math.min(1.0, distance / radius);
        return Math.max(1, Math.min(8, (int) Math.round(1 + 6.5 * heart + 2.0 * roll - 0.5)));
    }

    /** How deep the bowl is sunk here: 3 in the middle, to 0 at its rim. */
    public static int sink(double distance, int radius) {
        double t = Math.min(1.0, distance / radius);
        return (int) Math.round(3.0 * (1.0 - t * t));
    }

    /** The dragon's age in days, from two rolls in [0, 1): which stage, and where in it. */
    public static int ageDays(double stageRoll, double withinRoll) {
        int sum = 0;
        for (int w : AGE_WEIGHTS) {
            sum += w;
        }
        double at = stageRoll * sum;
        int stage = AGE_WEIGHTS.length - 1;
        for (int i = 0; i < AGE_WEIGHTS.length; i++) {
            at -= AGE_WEIGHTS[i];
            if (at < 0) {
                stage = i;
                break;
            }
        }
        if (stage == AGE_WEIGHTS.length - 1) {
            return ANCIENT_DAYS + (int) (withinRoll * 25);
        }
        return 25 * (stage + 1) + (int) (withinRoll * 25);
    }

    /** Ice and Fire's stage for an age in days (1 under 25, 5 from 100). */
    public static int stage(int days) {
        return Math.min(5, 1 + days / 25);
    }
}
