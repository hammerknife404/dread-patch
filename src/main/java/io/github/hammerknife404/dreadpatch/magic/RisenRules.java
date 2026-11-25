package io.github.hammerknife404.dreadpatch.magic;

/**
 * Game-free: creatures risen Dread (0.63.0; the owner's dump: water beasts "whose max HP scales directly with the
 * base creature's HP pool (keeping small fish as fast ankle-biters while turning large ... aquatic creatures into
 * high-speed frozen leviathans)"; the numbers ours), and how often they stir on their own (the owner: "rarely spawn
 * in caves, same as the aquatic beast variant in the rare chance of underground water").
 */
public final class RisenRules {
    /** Its health, times its kind's. */
    public static final double HEALTH_TIMES = 2.0;
    /** Its speed, times its kind's. */
    public static final double SPEED_TIMES = 1.6;
    public static final int BITE_TICKS = 20;
    /** A bite: 1 and 15% of its kind's health (a cod's 3: 1.45; a dolphin's 10: 2.5; an elder guardian's 80: 13). */
    public static final double BITE_BASE = 1.0;
    public static final double BITE_SHARE = 0.15;

    /** Stirring on their own: each player a one-in-this chance every 10 s of one, in a cave or underground water near them. */
    public static final int CAVE_PHANTOM_ONE_IN = 40;
    public static final int CAVE_WATER_ONE_IN = 40;
    /** In the Rime Chasma, the same for its water pockets (0.68.0): searched for there, so found far more often. */
    public static final int CHASMA_WATER_ONE_IN = 8;
    /** Never past this many risen (of either) within 64 of the player. */
    public static final int CAVE_CAP = 3;
    /** Where: 12-32 blocks off, within 12 up or down. */
    public static final int CAVE_MIN = 12;
    public static final int CAVE_MAX = 32;
    public static final int CAVE_DEPTH = 12;
    /** Underground water's kinds, by weight: glow squid 3 (it lives there), squid 1, cod 1, salmon 1. */
    public enum WaterKind {
        GLOW_SQUID(3), SQUID(1), COD(1), SALMON(1);

        public final int weight;

        WaterKind(int weight) {
            this.weight = weight;
        }
    }

    public static final int WATER_WEIGHT = 6;

    private RisenRules() {
    }

    public static float bite(double kindHealth) {
        return (float) (BITE_BASE + BITE_SHARE * kindHealth);
    }

    public static WaterKind waterKind(int roll) {
        for (WaterKind kind : WaterKind.values()) {
            if (roll < kind.weight) {
                return kind;
            }
            roll -= kind.weight;
        }
        return WaterKind.GLOW_SQUID;
    }
}
