package io.github.hammerknife404.dreadpatch.magic;

import java.util.Random;

/**
 * Game-free rules for what comes on its own in the Dreadlands (0.52.0, the owner's second drop):
 * <ul>
 * <li><b>The Dread</b> (Ice and Fire's thralls, ghouls, knights and scuttlers, her court's mix), day and
 * night, more in snow and a little more again in a heavy snowstorm (thunder): every
 * {@link #SPAWN_INTERVAL} each player has a one-in-N chance, N by the weather (the server config's,
 * defaults {@link #CLEAR_ONE_IN} / {@link #SNOW_ONE_IN} / {@link #STORM_ONE_IN}), of 1-3 rising 24-48
 * blocks off, never past {@link #CAP} of them within {@link #CAP_RANGE}.</li>
 * <li><b>Patrols</b>, small, day and night: every {@link #PATROL_INTERVAL}, one in {@link #PATROL_ONE_IN}
 * a player, a dread knight on foot leading 2-4 more, 32-48 blocks off, walking the waste.</li>
 * <li><b>Dread phantoms</b> come more often near a spire: one in {@link #PHANTOM_NEAR_SPIRE_ONE_IN}
 * instead of {@link #PHANTOM_ONE_IN} every 30 s.</li>
 * </ul>
 */
public final class DreadlandsSpawning {
    public enum Weather { CLEAR, SNOW, STORM }

    public static final int SPAWN_INTERVAL = 200;
    public static final int CLEAR_ONE_IN = 12;
    public static final int SNOW_ONE_IN = 6;
    public static final int STORM_ONE_IN = 5;
    public static final int CAP = 8;
    public static final double CAP_RANGE = 64.0;
    public static final double MIN_DISTANCE = 24.0;
    public static final double MAX_DISTANCE = 48.0;

    public static final int PATROL_INTERVAL = 2400;
    public static final int PATROL_ONE_IN = 3;
    public static final double PATROL_MIN_DISTANCE = 32.0;
    public static final double PATROL_MAX_DISTANCE = 48.0;
    /** A patrol's next waypoint, this far from where its leader stands. */
    public static final double PATROL_LEG_MIN = 64.0;
    public static final double PATROL_LEG_MAX = 128.0;

    public static final int PHANTOM_ONE_IN = 8;
    public static final int PHANTOM_NEAR_SPIRE_ONE_IN = 3;
    /** How near a spire's middle counts as near it. */
    public static final double SPIRE_REACH = 96.0;
    /**
     * Vanilla's undead strayed in (0.60.0, the owner's dump: "Add vanilla undead and cold variants ... at low weights";
     * ours): a one-in-this chance per player every {@link #SPAWN_INTERVAL} of 1-2, never past {@link #UNDEAD_CAP}
     * of them within {@link #CAP_RANGE}.
     */
    public static final int UNDEAD_ONE_IN = 16;
    public static final int UNDEAD_CAP = 4;
    /** Which, by weight: stray 3, zombie 2, skeleton 2, zombie villager 1 (cold kinds first; no husks, a desert's). */
    public enum Undead {
        STRAY(3), ZOMBIE(2), SKELETON(2), ZOMBIE_VILLAGER(1);

        public final int weight;

        Undead(int weight) {
            this.weight = weight;
        }
    }

    public static final int UNDEAD_WEIGHT = 8;

    /** The undead kind for a roll in 0 to {@link #UNDEAD_WEIGHT} - 1. */
    public static Undead undead(int roll) {
        for (Undead kind : Undead.values()) {
            if (roll < kind.weight) {
                return kind;
            }
            roll -= kind.weight;
        }
        return Undead.STRAY;
    }

    private DreadlandsSpawning() {
    }

    /** One in how many, for the weather, from the three settings. Zero or less means never. */
    public static int oneIn(Weather weather, int clear, int snow, int storm) {
        return switch (weather) {
            case CLEAR -> clear;
            case SNOW -> snow;
            case STORM -> storm;
        };
    }

    /** Whether a one-in-{@code oneIn} chance comes up. Zero or less never does. */
    public static boolean comesUp(int oneIn, Random random) {
        return oneIn > 0 && random.nextInt(oneIn) == 0;
    }

    /** How many rise together: 1-3, never past the cap. */
    public static int groupSize(int standing, int cap, Random random) {
        return Math.max(0, Math.min(1 + random.nextInt(3), cap - standing));
    }

    /** A patrol: its leader and 2-4 more. */
    public static int patrolFollowers(Random random) {
        return 2 + random.nextInt(3);
    }
}
