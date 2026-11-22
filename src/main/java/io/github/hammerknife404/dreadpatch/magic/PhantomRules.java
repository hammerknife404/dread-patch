package io.github.hammerknife404.dreadpatch.magic;

/**
 * Game-free rules for the dread phantom (0.45.0): what its bite leaves, how often it dives, and how
 * many come when they're called.
 * <ul>
 * <li>Its bite rolls Rimefrost (the slow) and Withering Rime apart, so both can land (the owner: "it
 * would still be possible for both to apply"; ours: 35% and 15%).</li>
 * <li>Its aggression scales with difficulty: vanilla's 8–11 s between dives on Easy, less above it,
 * and a new target is dived on sooner.</li>
 * <li>The queen, a Dread Lich and Syndrigossa call 1–2 every 45 s while they've a target, at most 4
 * of theirs at once; the spell calls 2 for 60 s.</li>
 * </ul>
 */
public final class PhantomRules {
    public static final float RIMEFROST_CHANCE = 0.35F;
    public static final float WITHERING_CHANCE = 0.15F;
    public static final int RIMEFROST_TICKS = 100;
    public static final int WITHERING_TICKS = 60;

    /** A Dread summoner's first call this long into a fight, then one every {@link #CALL_COOLDOWN}; blocked, it tries again after {@link #CALL_RETRY}. */
    public static final int FIRST_CALL = 300;
    public static final int CALL_COOLDOWN = 900;
    public static final int CALL_RETRY = 20;
    public static final int MAX_STANDING = 4;

    /** The spell (scroll only): how many, and for how long. */
    public static final int SPELL_COUNT = 2;
    public static final int SPELL_SECONDS = 60;

    /** Vanilla's difficulty ids. */
    public static final int EASY = 1;
    public static final int NORMAL = 2;
    public static final int HARD = 3;

    private PhantomRules() {
    }

    /** Whether a bite leaves Rimefrost, from a roll in {@code [0, 1)}. */
    public static boolean rimefrost(float roll) {
        return roll < RIMEFROST_CHANCE;
    }

    /** Whether a bite leaves Withering Rime, from its own roll in {@code [0, 1)}. */
    public static boolean witheringRime(float roll) {
        return roll < WITHERING_CHANCE;
    }

    /** How many a summoner calls: as many as it wants (1–2), but never past {@link #MAX_STANDING} of its own. */
    public static int toCall(int standing, int wanted) {
        return Math.max(0, Math.min(wanted, MAX_STANDING - standing));
    }

    /**
     * Ticks between dives, from a roll in {@code [0, 1)}: vanilla's 8–11 s on Easy (and Peaceful,
     * though none stay), 6–8 s on Normal, 4–6 s on Hard.
     */
    public static int swoopTicks(int difficulty, double roll) {
        double r = Math.max(0.0, Math.min(1.0, roll));
        if (difficulty >= HARD) {
            return 80 + (int) (r * 40);
        }
        if (difficulty == NORMAL) {
            return 120 + (int) (r * 40);
        }
        return 160 + (int) (r * 60);
    }

    /** Ticks before it dives on a target it's just taken (revenge, or its summoner's): vanilla's 10 on Easy, sooner above it. */
    public static int firstSwoopTicks(int difficulty) {
        if (difficulty >= HARD) {
            return 2;
        }
        return difficulty == NORMAL ? 5 : 10;
    }
}
