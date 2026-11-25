package io.github.hammerknife404.dreadpatch.magic;

/**
 * Game-free: the Rime Eel (0.71.0, the owner's spec: "an elongated, serpentine dread-anglerfish ... scaling in size
 * from a standard fish up to a squid (~0.6 to 1.4 blocks long) with HP scaling roughly from 10–18 and 4 natural armor
 * points"; its aura "every few seconds" for "a few seconds", its lure "remaining off far more often than on", a Frenzy
 * "doubling the eel's attack speed", a bite with "a low chance" of Withering Rime, food healing "roughly 0.25 to 1 HP
 * per item"; spawning in dark water "(minimum 1x2 water blocks) at light level 6 or below"). Where the spec gave no
 * number, the number is ours.
 */
public final class RimeEelRules {
    /** Its model's length at scale 1, in blocks (24 pixels, head to tail fin). */
    public static final double MODEL_LENGTH = 1.5;
    /** How long it comes: 0.6 to 1.4 blocks (a fish's to a squid's). */
    public static final double LENGTH_MIN = 0.6;
    public static final double LENGTH_MAX = 1.4;
    /** Its health, with its length: 10 to 18. */
    public static final double HEALTH_MIN = 10.0;
    public static final double HEALTH_MAX = 18.0;
    public static final double ARMOR = 4.0;
    /** Its bite, with its length: 2 to 4 (ours). */
    public static final double BITE_MIN = 2.0;
    public static final double BITE_MAX = 4.0;

    /** Its attribute speed (as an axolotl's), times these in water and on land (ours: swift in water, a slither on land). */
    public static final double SPEED = 1.0;
    public static final float WATER_SPEED = 0.035F;
    public static final float LAND_SPEED = 0.2F;

    /** Ticks between bites, and in a Frenzy (twice as fast). */
    public static final int BITE_TICKS = 20;
    public static final int FRENZY_BITE_TICKS = 10;
    /** A bite's chance of Withering Rime, and how long it lasts (ours). */
    public static final float WITHERING_CHANCE = 0.1F;
    public static final int WITHERING_TICKS = 60;

    /** Its aura (in combat only): this far, every 3-5 s, its effect 4 s; a Frenzy 4 s once it catches anything (ours). */
    public static final double AURA_RADIUS = 4.0;
    public static final int AURA_MIN_TICKS = 60;
    public static final int AURA_SPREAD_TICKS = 40;
    public static final int AURA_EFFECT_TICKS = 80;
    public static final int FRENZY_TICKS = 80;

    /** On land, in combat: a lunge at a target 1.5-5 blocks off, then 2 s before the next (ours). */
    public static final double LUNGE_MIN = 1.5;
    public static final double LUNGE_MAX = 5.0;
    public static final double LUNGE_SPEED = 0.7;
    public static final double LUNGE_LIFT = 0.3;
    public static final int LUNGE_TICKS = 40;

    /** Out of water it looks for water this far round (and this far up or down). */
    public static final int WATER_SEARCH = 12;
    public static final int WATER_SEARCH_DEPTH = 4;
    /** It looks for food this far round, and eats one item every this many ticks. */
    public static final double FOOD_SEARCH = 8.0;
    public static final int EAT_TICKS = 10;

    /** Its lure flickers in steps of this many ticks, lit on about this share of them. */
    public static final int FLICKER_STEP = 3;
    public static final double LIT_SHARE = 0.28;
    /** And burns bright this long after a pulse. */
    public static final int FLARE_TICKS = 10;

    /** Spawning in the Dreadlands: no brighter than this (their sun is never past 6). */
    public static final int MAX_LIGHT = 6;
    public static final int SUN_CAP = 6;
    /** Each player's one-in-this chance every 10 s of one in dark water near them (on top of the cave and chasma rolls). */
    public static final int DARK_WATER_ONE_IN = 10;
    /** Never past this many within 64 of the player. */
    public static final int CAP = 4;
    /** Where: 8-32 blocks off, within 16 up or down. */
    public static final int SEARCH_MIN = 8;
    public static final int SEARCH_MAX = 32;
    public static final int SEARCH_DEPTH = 16;

    private RimeEelRules() {
    }

    /** Its {@code generic.scale}, from a roll in [0, 1): its length from 0.6 to 1.4 blocks. */
    public static double scale(double roll) {
        return (LENGTH_MIN + (LENGTH_MAX - LENGTH_MIN) * roll) / MODEL_LENGTH;
    }

    /** How far its scale is along its range: 0 the smallest, 1 the largest. */
    static double growth(double scale) {
        double length = scale * MODEL_LENGTH;
        return Math.max(0.0, Math.min(1.0, (length - LENGTH_MIN) / (LENGTH_MAX - LENGTH_MIN)));
    }

    /** Its health at {@code scale}, to the half heart: 10 to 18. */
    public static double health(double scale) {
        return Math.round(2.0 * (HEALTH_MIN + (HEALTH_MAX - HEALTH_MIN) * growth(scale))) / 2.0;
    }

    public static double bite(double scale) {
        return Math.round(2.0 * (BITE_MIN + (BITE_MAX - BITE_MIN) * growth(scale))) / 2.0;
    }

    /** Ticks till its next pulse, from a roll in [0, 1). */
    public static int auraTicks(double roll) {
        return AURA_MIN_TICKS + (int) (roll * AURA_SPREAD_TICKS);
    }

    /** What an item heals it: an eighth of its food's nutrition, 0.25 to 1 (rotten flesh 0.5, a steak 1, an ink sac 0.25). */
    public static float heal(int nutrition) {
        return (float) Math.max(0.25, Math.min(1.0, nutrition / 8.0));
    }

    /** Whether a lunge at a target {@code distance} off is on: on land, on the ground, in sight, its cooldown run. */
    public static boolean lunges(boolean inWater, boolean onGround, double distance, boolean seen, int cooldown) {
        return !inWater && onGround && seen && cooldown <= 0 && distance >= LUNGE_MIN && distance <= LUNGE_MAX;
    }

    /**
     * Whether its lure is lit at {@code tick}: erratic, by a hash of its own {@code seed} and the step, so each eel's
     * flickers its own way, and the same on every client. Off far more often than on.
     */
    public static boolean lureLit(int seed, int tick) {
        int step = Math.floorDiv(tick, FLICKER_STEP);
        double here = hash(seed, step);
        if (here >= LIT_SHARE) {
            return false;
        }
        // Lit steps stutter: a lit one sometimes drops out for a tick.
        return Math.floorMod(tick, FLICKER_STEP) != 1 || hash(seed ^ 0x5bd1e995, step) > 0.3;
    }

    /** A pseudo-random [0, 1) from two ints. */
    static double hash(int a, int b) {
        long x = a * 0x9E3779B97F4A7C15L + b * 0xC2B2AE3D27D4EB4FL;
        x ^= x >>> 33;
        x *= 0xFF51AFD7ED558CCDL;
        x ^= x >>> 33;
        x *= 0xC4CEB9FE1A85EC53L;
        x ^= x >>> 33;
        return (x >>> 11) * 0x1.0p-53;
    }

    /** Its light where it'd spawn: the block light, or the sky's, capped at the Dreadlands' sun. */
    public static int light(int block, int sky) {
        return Math.max(block, Math.min(sky, SUN_CAP));
    }

    public static boolean darkEnough(int block, int sky) {
        return light(block, sky) <= MAX_LIGHT;
    }
}
