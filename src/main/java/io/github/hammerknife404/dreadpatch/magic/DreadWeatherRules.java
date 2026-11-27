package io.github.hammerknife404.dreadpatch.magic;

/**
 * Game-free: the Dread's weather in the overworld (0.89.0, the owner's Feature_Dump_11-27: "Add two config toggles (both
 * false by default): cold biomes only, and any Overworld biome (reduced chance in hot climates)"; "Visually identical
 * storms with Overworld Rime Lightning, temporary snow layers, zero Whiteout Dominion damage, standard spawns in cold
 * biomes, small mixed lesser Dread spawns in non-cold biomes, and Nether/End unaffected"; and a warning as it starts).
 * <ul>
 * <li>With either toggle on, one overworld rain in {@link #STORM_ONE_IN} (the config's) is a dread storm, for as long as
 * it rains; one in {@link #HOT_REACH_ONE_IN} of those reaches the hot lands too ({@link #reaches}).</li>
 * <li>Round each player it reaches: {@link #SNOW_PER_SECOND} snow layers a second within {@link #SNOW_RADIUS}, at most
 * {@link #SNOW_MAX} laid at once, melted {@link #MELT_PER_TICK} a tick once it's over.</li>
 * <li>Away from the cold, {@link #LESSER_MIN} to {@link #LESSER_MAX} of the lesser Dread at a time.</li>
 * <li>As it starts, one of {@link #ALERT_LINES} warnings, shown {@link #ALERT_TICKS} ticks, fading in and out.</li>
 * </ul>
 */
public final class DreadWeatherRules {
    public static final int STORM_ONE_IN = 3;
    public static final int HOT_REACH_ONE_IN = 3;
    public static final int SNOW_RADIUS = 24;
    public static final int SNOW_PER_SECOND = 4;
    public static final int SNOW_MAX = 2000;
    public static final int MELT_PER_TICK = 8;
    public static final int LESSER_MIN = 1;
    public static final int LESSER_MAX = 2;
    public static final int ALERT_LINES = 10;
    public static final int ALERT_TICKS = 160;
    public static final int ALERT_FADE_IN = 20;
    public static final int ALERT_FADE_OUT = 50;

    private DreadWeatherRules() {
    }

    /**
     * Whether a dread storm reaches a place: in the cold with either toggle; elsewhere only with "anywhere", and in the
     * hot lands only the storms that reach them.
     */
    public static boolean reaches(boolean cold, boolean hot, boolean coldToggle, boolean anywhere, boolean hotReach) {
        if (cold) {
            return coldToggle || anywhere;
        }
        return anywhere && (!hot || hotReach);
    }

    /** Whether the dread weather can come at all. */
    public static boolean enabled(boolean coldToggle, boolean anywhere) {
        return coldToggle || anywhere;
    }

    /** A warning's opacity, {@code age} ticks after it's shown: fading in, held, fading out; 0 once it's done. */
    public static float alertAlpha(int age) {
        if (age < 0 || age >= ALERT_TICKS) {
            return 0.0F;
        }
        if (age < ALERT_FADE_IN) {
            return age / (float) ALERT_FADE_IN;
        }
        int left = ALERT_TICKS - age;
        return left < ALERT_FADE_OUT ? left / (float) ALERT_FADE_OUT : 1.0F;
    }
}
