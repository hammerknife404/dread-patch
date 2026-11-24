package io.github.hammerknife404.dreadpatch.magic;

/**
 * Game-free: dread lights as sources of cold (0.65.0, the owner: "Dread torches can freeze water or compatible
 * fluids or act as sources of cold. Capable of freezing water to blue ice in about a minecraft day (sleeping
 * cannot skip this)", and "dread lanterns included in the liquid freezing"). Water within {@link #RADIUS}
 * across, {@link #DOWN} below to {@link #UP} above a dread torch or lantern turns to ice, packed ice, then blue ice,
 * a stage each third of the freezing time (by default a Minecraft day, 20 minutes, of game time, which sleeping
 * doesn't skip: it sets only the day's time).
 */
public final class ColdRules {
    public static final int RADIUS = 2;
    public static final int DOWN = 2;
    public static final int UP = 1;
    /** How often (ticks) the cold is counted. */
    public static final int CHECK_EVERY = 100;
    /** Ours: a Minecraft day. */
    public static final double DEFAULT_MINUTES = 20.0;
    public static final int STAGES = 3;

    /** What cold turns water to, stage by stage. */
    public enum Stage {
        WATER, ICE, PACKED_ICE, BLUE_ICE;

        /** The next colder, or itself at blue ice. */
        public Stage next() {
            return this == BLUE_ICE ? BLUE_ICE : values()[this.ordinal() + 1];
        }
    }

    private ColdRules() {
    }

    /** Ticks of cold a stage takes, for a whole freezing time of {@code minutes}; 0 never. */
    public static long stageTicks(double minutes) {
        return minutes <= 0.0 ? 0L : Math.max(1L, Math.round(minutes * 60.0 * 20.0 / STAGES));
    }

    /**
     * Cold counted on a block: {@code had} ticks so far, {@code elapsed} more since it was last counted. Whether it
     * goes a stage colder now (its count then starting over).
     */
    public static boolean colder(long had, long elapsed, long stageTicks) {
        return stageTicks > 0L && had + elapsed >= stageTicks;
    }
}
