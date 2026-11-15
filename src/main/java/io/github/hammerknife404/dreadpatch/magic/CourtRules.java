package io.github.hammerknife404.dreadpatch.magic;

/**
 * Game-free rules for her Court of the Dread: which of Ice and Fire's dread mobs answer her call,
 * how often each, and how many rise at once.
 */
public final class CourtRules {
    /** The dread she raises and their weights: mostly thralls, ghouls next, knights and scuttlers rarer. */
    public enum Kind {
        THRALL(3), GHOUL(2), KNIGHT(1), SCUTTLER(1);

        public final int weight;

        Kind(int weight) {
            this.weight = weight;
        }
    }

    public static final int TOTAL_WEIGHT;

    static {
        int total = 0;
        for (Kind kind : Kind.values()) {
            total += kind.weight;
        }
        TOTAL_WEIGHT = total;
    }

    private CourtRules() {
    }

    /** The kind a roll in {@code [0, TOTAL_WEIGHT)} picks. */
    public static Kind pick(int roll) {
        int r = Math.floorMod(roll, TOTAL_WEIGHT);
        for (Kind kind : Kind.values()) {
            if (r < kind.weight) {
                return kind;
            }
            r -= kind.weight;
        }
        throw new AssertionError("weights don't sum to TOTAL_WEIGHT");
    }

    /** How many to raise: a wave, but never past the court's limit. */
    public static int toRaise(int current, int max, int wave) {
        return Math.max(0, Math.min(wave, max - current));
    }
}
