package io.github.hammerknife404.dreadpatch.magic;

import java.util.function.IntUnaryOperator;

/**
 * The Dreadlands' Whiteout Dominion weather (0.54.0, the owner's second drop: "a rare Whiteout Dominion
 * weather ... about one storm in six ... and never back to back", "never starting while the Dread Queen's
 * alive", "turning into a heavy thunderstorm if her fight starts during it"). A storm there is any snowfall
 * (the overworld's rain); each one, as it starts, may be a whiteout.
 */
public final class WeatherRules {
    /** About one storm in this many is a whiteout (the server config's default). */
    public static final int WHITEOUT_ONE_IN = 6;
    /** How often its Whiteout is renewed on those out in it (with {@link #EFFECT_TICKS}), as the storm's Rimefrost is. */
    public static final int EFFECT_EVERY = 20;
    public static final int EFFECT_TICKS = 60;
    /** A point of freezing damage this often under the open sky: Whiteout Dominion's. */
    public static final int FREEZE_EVERY = 60;
    public static final float FREEZE_DAMAGE = 1.0F;
    /** The chance a mob it kills rises, wild Dread (0.92.0, the owner's 25%). */
    public static final float RISE_CHANCE = 0.25F;
    /** The thunderstorm her fight turns it into lasts at least this long (or the storm's own time left). */
    public static final int THUNDER_AT_LEAST = 6000;

    private WeatherRules() {
    }

    /**
     * The one-in-how-many a storm is rolled at when a whiteout may come, so that, with none two in a row,
     * about one storm in {@code oneIn} is one: a whiteout is followed by a plain storm, so a rate p of the
     * storms allowed one gives p / (1 + p) of all; 1 / (oneIn − 1) gives 1 / oneIn. 2 or less: every one
     * allowed (every other storm). 0: never.
     */
    public static int rollOneIn(int oneIn) {
        if (oneIn <= 0) {
            return 0;
        }
        return Math.max(1, oneIn - 1);
    }

    /** The weather's memory, saved with the Dreadlands' data. */
    public static final class Storms {
        /** Whether it's snowing there, as last seen. */
        public boolean storming;
        /** Whether this storm is a whiteout now. */
        public boolean whiteout;
        /** Whether this storm was one, even if her fight broke it. */
        public boolean wasWhiteout;
        /** Whether the last storm was one. */
        public boolean lastWhiteout;

        /**
         * Follows the weather: as a storm starts, rolls whether it's a whiteout ({@code roll} gives 0 to
         * n − 1 for an n), never after one, nor while a queen's about; as it ends, remembers. Whether
         * anything changed.
         */
        public boolean update(boolean raining, boolean queenAbout, int oneIn, IntUnaryOperator roll) {
            if (raining == this.storming) {
                return false;
            }
            this.storming = raining;
            if (raining) {
                int n = rollOneIn(oneIn);
                this.whiteout = n > 0 && !this.lastWhiteout && !queenAbout && roll.applyAsInt(n) == 0;
                this.wasWhiteout = this.whiteout;
            } else {
                this.lastWhiteout = this.wasWhiteout;
                this.whiteout = false;
                this.wasWhiteout = false;
            }
            return true;
        }

        /** Her fight starting: a whiteout breaks (into thunder). Whether there was one to break. */
        public boolean breakWhiteout() {
            if (!this.whiteout) {
                return false;
            }
            this.whiteout = false;
            return true;
        }
    }
}
