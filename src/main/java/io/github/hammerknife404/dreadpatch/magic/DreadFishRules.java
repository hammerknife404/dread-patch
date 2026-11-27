package io.github.hammerknife404.dreadpatch.magic;

/**
 * Game-free: the Dread Fish (0.71.0, the owner's spec: "an elongated, serpentine dread-anglerfish ... scaling in size
 * from a standard fish up to a squid (~0.6 to 1.4 blocks long) with HP scaling roughly from 10–18 and 4 natural armor
 * points"; its aura "every few seconds" for "a few seconds", its lure "remaining off far more often than on", a Frenzy
 * "doubling the fish's attack speed", a bite with "a low chance" of Withering Rime, food healing "roughly 0.25 to 1 HP
 * per item"; spawning in dark water "(minimum 1x2 water blocks) at light level 6 or below"). Where the spec gave no
 * number, the number is ours.
 */
public final class DreadFishRules {
    /** Its model's length at scale 1, in blocks: curled at rest, about 2 across (tools/models/make_dread_fish.py prints it). */
    public static final double MODEL_LENGTH = 2.0;
    /**
     * How big it comes, across its curl: 0.6 to 2.5 blocks (0.71.1, the owner: "up to about 2.5 on average with a rare
     * alpha or matron getting that large say up to 3.5 or 4"; 0.71.0's spec had 0.6-1.4); and one in {@link #ALPHA_ONE_IN}
     * an alpha or matron, 2.5 to 4.
     */
    public static final double LENGTH_MIN = 0.6;
    public static final double LENGTH_MAX = 2.5;
    public static final double ALPHA_LENGTH_MAX = 4.0;
    /** Ours: rare. */
    public static final int ALPHA_ONE_IN = 20;
    /** Its health, with its size: 10 to 18 (the spec's), and on up the same line for an alpha, to 24.5. */
    public static final double HEALTH_MIN = 10.0;
    public static final double HEALTH_MAX = 18.0;
    public static final double ARMOR = 4.0;
    /** Its bite, with its size: 2 to 4 (ours), an alpha's up to 5.5. */
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

    /** Its aura (in combat only): this far (4, 5 since 0.91.0 at the owner's word), every 3-5 s, its effect 4 s; a Frenzy 4 s once it catches anything (ours). */
    public static final double AURA_RADIUS = 5.0;
    public static final int AURA_MIN_TICKS = 60;
    public static final int AURA_SPREAD_TICKS = 40;
    public static final int AURA_EFFECT_TICKS = 80;
    public static final int FRENZY_TICKS = 80;
    /**
     * A Glowing target sends it into a Frenzy (0.76.0, the owner: "Highlighted creatures also send dread fish into
     * frenzies"), kept up this many ticks at a time while it glows.
     */
    public static final int GLOW_FRENZY_TICKS = 20;
    /**
     * One in this many spawns with its lantern (0.76.0, the owner: "Only about 10-25% spawn with the lantern"; ours 1 in 6,
     * about 17%). Without one it has no lure at all ("no fish horns"), and no pulse (asked: "No pulse without it").
     */
    public static final int LANTERN_ONE_IN = 6;
    /**
     * Its lure reaching out while it hunts or swims about, drawn in while still (0.76.0, the owner: "It retracts closer
     * when idle and extends out when searching"): how much of the way a tick (0 drawn in, 1 out).
     */
    public static final float REACH_STEP = 0.08F;
    /**
     * Where its lantern's glass hangs reaching out and drawn in, in model pixels from its feet ({@code x} to its left, {@code y}
     * up, {@code z} back), at scale 1: {@code tools/models/make_dread_fish.py} prints them.
     */
    public static final double[] LANTERN_OUT = {9.0, 24.0 - 10.6, -15.1};
    public static final double[] LANTERN_IN = {9.0, 24.0 - 4.9, -11.6};

    /** On land, in combat: a lunge at a target 1.5-5 blocks off, then 2 s before the next (ours). */
    public static final double LUNGE_MIN = 1.5;
    public static final double LUNGE_MAX = 7.0;  // 5 until 0.90.1, when its lunge became the leap
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
    /**
     * In the Rime Chasma (0.84.1, the owner: "Rime fish can spawn naturally on the surface of chasmas, but prefer water"):
     * each player's one-in-this chance every 10 s of one more there: in dark water near them if there's any, else out on
     * the chasma's open floor.
     */
    public static final int CHASMA_SURFACE_ONE_IN = 20;
    /** Never past this many within 64 of the player. */
    public static final int CAP = 4;
    /** Where: 8-32 blocks off, within 16 up or down. */
    public static final int SEARCH_MIN = 8;
    public static final int SEARCH_MAX = 32;
    public static final int SEARCH_DEPTH = 16;

    /**
     * 0.86.0 (the owner: "Double aggro range on glowing/highlighted targets, swim slowly in a straight line while idle,
     * and add a Sea Serpent-style water leap attack/feed (few blocks high, 3–8s cooldown)"): a Glowing target is noticed
     * {@link #GLOW_RANGE_FACTOR} times as far off; idle, it cruised slowly in straight lines until 0.90.1 (now it swims
     * as a dolphin, below); and from just under the surface (within
     * {@link #LEAP_DEPTH}) it leaps at prey or food up to {@link #LEAP_MAX_HEIGHT} over the water and {@link #LEAP_REACH}
     * off, every {@link #LEAP_MIN_COOLDOWN} to {@link #LEAP_MAX_COOLDOWN} ticks at most.
     */
    public static final double GLOW_RANGE_FACTOR = 2.0;
    /** Idle, it swims as a dolphin does (0.90.1, the owner: "dread fish need to swim and mimic dolphins in the water"):
     * at its full pace (0.5 until then, which crept), a new way every {@link #SWIM_INTERVAL} ticks or so, breaching now
     * and then near the surface (one chance in {@link #BREACH_INTERVAL} a tick). */
    public static final double CRUISE_SPEED = 1.0;
    public static final int SWIM_INTERVAL = 10;
    public static final int BREACH_INTERVAL = 40;
    public static final double LEAP_REACH = 7.0;
    public static final double LEAP_MAX_HEIGHT = 4.0;
    public static final double LEAP_DEPTH = 2.0;
    public static final int LEAP_MIN_COOLDOWN = 60;
    public static final int LEAP_MAX_COOLDOWN = 160;
    /** A mob's gravity, a tick, and its air drag (vanilla's). */
    static final double GRAVITY = 0.08;
    static final double DRAG = 0.98;

    /** A leap's cooldown from a roll in [0, 1): 3 to 8 s. */
    public static int leapCooldown(double roll) {
        return LEAP_MIN_COOLDOWN + (int) (roll * (LEAP_MAX_COOLDOWN - LEAP_MIN_COOLDOWN + 1));
    }

    /**
     * A leap's launch, to clear {@code height} blocks over its start (a block over its prey, at most
     * {@link #LEAP_MAX_HEIGHT}) and come down {@code distance} blocks off: its upward speed and its speed across, a tick.
     */
    public static double[] leap(double distance, double height) {
        double rise = Math.max(1.0, Math.min(LEAP_MAX_HEIGHT, height));
        double up = Math.sqrt(2.0 * GRAVITY * rise) / Math.sqrt(DRAG);
        double airborne = 2.0 * up / GRAVITY;
        double across = Math.min(1.2, distance / Math.max(1.0, airborne));
        return new double[] {up, across};
    }

    /** Whether something {@code dx} across and {@code dy} up is in a leap's reach from under the surface. */
    public static boolean inLeapReach(double dx, double dy) {
        return dx <= LEAP_REACH && dy >= -0.5 && dy <= LEAP_MAX_HEIGHT;
    }

    private DreadFishRules() {
    }

    /** Its {@code generic.scale}, from a roll in [0, 1): 0.6 to 2.5 blocks across. */
    public static double scale(double roll) {
        return (LENGTH_MIN + (LENGTH_MAX - LENGTH_MIN) * roll) / MODEL_LENGTH;
    }

    /** As it spawns: one in {@link #ALPHA_ONE_IN} ({@code alpha} rolled under it) an alpha, 2.5 to 4 across, by {@code roll}. */
    public static double scale(double roll, int alpha) {
        if (alpha != 0) {
            return scale(roll);
        }
        return (LENGTH_MAX + (ALPHA_LENGTH_MAX - LENGTH_MAX) * roll) / MODEL_LENGTH;
    }

    /** Whether one this size is an alpha or matron: past the usual most. */
    /**
     * An alpha's or matron's way of fighting (0.80.0, the owner: "A mix of heavy natural armor and slow easy to read
     * attacks. Hit hrd and with knockback."): heavy armour and toughness, a heavy body hard to shove; a bite every
     * {@link #ALPHA_BITE_TICKS} (a Frenzy's {@link #ALPHA_FRENZY_BITE_TICKS}), each after a wind-up of
     * {@link #ALPHA_WINDUP_TICKS} rearing back, its jaw gaping (in reach as it starts, and still in reach as it ends, or it
     * snaps on air); its bite {@link #ALPHA_BITE_FACTOR} times a size's, with knockback; its lunges half as often.
     */
    public static final double ALPHA_ARMOR = 14.0;
    public static final double ALPHA_TOUGHNESS = 4.0;
    public static final double ALPHA_KNOCKBACK_RESISTANCE = 0.6;
    public static final double ALPHA_KNOCKBACK = 1.5;
    public static final double ALPHA_BITE_FACTOR = 1.75;
    public static final int ALPHA_BITE_TICKS = 50;
    public static final int ALPHA_FRENZY_BITE_TICKS = 30;
    public static final int ALPHA_WINDUP_TICKS = 16;
    public static final int ALPHA_LUNGE_TICKS = 80;

    public static boolean alpha(double scale) {
        return scale * MODEL_LENGTH > LENGTH_MAX + 1e-9;
    }

    /** How far its size is along the usual range: 0 the smallest, 1 the usual largest, past 1 an alpha's (to 4's). */
    static double growth(double scale) {
        double length = Math.max(LENGTH_MIN, Math.min(ALPHA_LENGTH_MAX, scale * MODEL_LENGTH));
        return (length - LENGTH_MIN) / (LENGTH_MAX - LENGTH_MIN);
    }

    /** Its health at {@code scale}, to the half heart: 10 to 18, an alpha's up to 24.5. */
    public static double health(double scale) {
        return Math.round(2.0 * (HEALTH_MIN + (HEALTH_MAX - HEALTH_MIN) * growth(scale))) / 2.0;
    }

    /** An fish's bite at its size: an alpha's {@link #ALPHA_BITE_FACTOR} times what the size's line gives, to the half. */
    public static double fullBite(double scale) {
        double bite = bite(scale);
        return alpha(scale) ? Math.round(2.0 * bite * ALPHA_BITE_FACTOR) / 2.0 : bite;
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
     * Whether its lure is lit at {@code tick}: erratic, by a hash of its own {@code seed} and the step, so each fish's
     * flickers its own way, and the same on every client. Off far more often than on.
     */
    /** Whether a spawn, rolling {@code roll} in {@code [0, LANTERN_ONE_IN)}, has its lantern. */
    public static boolean spawnsWithLantern(int roll) {
        return roll == 0;
    }

    /**
     * How straight its body is drawn (0.92.1, the owner: "they curl up or spazz out still"): 0 curled at rest, 1 straight
     * out behind. Until then the curl followed vanilla's limb swing, which counts only movement across the ground and
     * jumps from tick to tick, so a fish swimming up or down, breaching or leaping curled up mid-move, and one swimming
     * unevenly flicked between curled and straight. Now it's its whole speed (blocks a tick, up and down too): straight
     * from {@link #STRAIGHT_AT}, curled under {@link #CURL_UNDER}, always straight off the ground and out of water (a
     * leap, a breach, a fall); quick to straighten, slow to curl back.
     */
    public static final double CURL_UNDER = 0.01;
    public static final double STRAIGHT_AT = 0.05;
    public static final float STRAIGHTEN_STEP = 0.2F;
    public static final float CURL_STEP = 0.04F;

    /** Its straightness next tick, from this tick's and how fast it moved ({@link #CURL_UNDER}). */
    public static float straightToward(float straight, double speed, boolean airborne) {
        float target = airborne ? 1.0F : (float) Math.max(0.0, Math.min(1.0, (speed - CURL_UNDER) / (STRAIGHT_AT - CURL_UNDER)));
        return straight + Math.max(-CURL_STEP, Math.min(STRAIGHTEN_STEP, target - straight));
    }

    /** Its lure's reach next tick: a step toward out (searching) or in. */
    public static float reachToward(float reach, boolean searching) {
        return searching ? Math.min(1.0F, reach + REACH_STEP) : Math.max(0.0F, reach - REACH_STEP);
    }

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
