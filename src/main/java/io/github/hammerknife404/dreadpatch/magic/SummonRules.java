package io.github.hammerknife404.dreadpatch.magic;

/**
 * Summoning Dread Fish and dread beasts (0.93.0, the owner's list of 2026-11-27):
 * <ul>
 * <li><b>Summon Dread Fish</b>, a spell: "Craftable, summons 1 - 5 pending rank. Lantern variants and spawn rate are kept the
 * same." One fish a level, each sized and lit as a wild one is; a summoned court's for {@link #FISH_SPELL_SECONDS}.</li>
 * <li><b>Syndrigossa</b>: "can also summon 1-2 dread fish and 1-2 dread beasts. Each has a roughly 30 second cooldown and
 * despawns after about 25 seconds, to hopefully avoid over crowding. She should prefer spreading their usage throughout a
 * fight." Ours: each kind on its own countdown of 25-35 s ({@link #cooldown}), the first fish 8-15 s into a fight and the
 * first beasts 18-25 s, never two of her summons within {@link #GAP} of each other (the later one waits), and never
 * more than {@link #MAX_STANDING} of a kind of hers about at once; they go after {@link #LIFETIME}.</li>
 * </ul>
 */
public final class SummonRules {
    /** The spell's fish last a minute, as the phantoms' spell's. */
    public static final int FISH_SPELL_SECONDS = 60;
    public static final int FISH_SPELL_MAX_LEVEL = 5;
    /** Where a summoned fish rises: water this far round (across, and up or down) first, else the ground. */
    public static final int WATER_REACH = 6;
    public static final int WATER_DEPTH = 3;

    /** Syndrigossa's summons last 25 s, ... */
    public static final int LIFETIME = 500;
    /** ... come again 25-35 s after (each kind its own), ... */
    public static final int COOLDOWN_MIN = 500;
    public static final int COOLDOWN_MAX = 700;
    /** ... the first fish 8-15 s into a fight, the first beasts 18-25 s, ... */
    public static final int FIRST_FISH_MIN = 160;
    public static final int FIRST_FISH_MAX = 300;
    public static final int FIRST_BEASTS_MIN = 360;
    public static final int FIRST_BEASTS_MAX = 500;
    /** ... never within 8 s of her last, ... */
    public static final int GAP = 160;
    /** ... at most 2 of a kind of hers about, 1-2 at a time, ... */
    public static final int MAX_STANDING = 2;
    /** ... while her target is this near her. */
    public static final double RANGE = 48.0;

    private SummonRules() {
    }

    /** How many fish the spell summons at {@code spellLevel}: one a level, 1 to 5. */
    public static int fishCount(int spellLevel) {
        return Math.max(1, Math.min(FISH_SPELL_MAX_LEVEL, spellLevel));
    }

    /** A countdown in {@code [min, max]}, from a roll in [0, 1). */
    static int between(int min, int max, double roll) {
        return min + (int) Math.floor(roll * (max - min + 1));
    }

    /** Her next countdown for a kind, after summoning it. */
    public static int cooldown(double roll) {
        return between(COOLDOWN_MIN, COOLDOWN_MAX, roll);
    }

    /** Her first countdown in a fight: fish, or beasts. */
    public static int first(boolean fish, double roll) {
        return fish ? between(FIRST_FISH_MIN, FIRST_FISH_MAX, roll) : between(FIRST_BEASTS_MIN, FIRST_BEASTS_MAX, roll);
    }

    /** Whether a kind whose countdown is done may come now, {@code sinceLast} ticks after her last summon of any kind. */
    public static boolean mayCome(int countdown, int sinceLast) {
        return countdown <= 0 && sinceLast >= GAP;
    }

    /** How many to summon: 1 or 2 (by {@code twoRoll}), no more than leaves her at {@link #MAX_STANDING} with {@code standing}. */
    public static int toSummon(int standing, boolean twoRoll) {
        return Math.max(0, Math.min(twoRoll ? 2 : 1, MAX_STANDING - standing));
    }
}
