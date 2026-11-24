package io.github.hammerknife404.dreadpatch.magic;

import java.util.function.IntUnaryOperator;

/**
 * Game-free: what the Dread Queen's defeat stirs in the overworld (0.62.0, the owner's feature dump,
 * "Post-Defeat Overworld Escalation"):
 * <ul>
 * <li>her first defeat in a world, for good, raises the Dread round its Mausoleums: in cold biomes, on
 * snow, ice or dreadstone, within {@link #MAUSOLEUM_REACH} of one;</li>
 * <li>each defeat, unless one came within the cooldown (the server config's, by default her encounter's
 * 6 hours), brings a dry storm over the cold lands for 30 to 60 seconds (the owner: "about 30-60 seconds
 * varying"): a warning, its fog, wind, rime and a mild Rimefrost, and nothing worse.</li>
 * </ul>
 */
public final class EscalationRules {
    public static final int STORM_MIN_TICKS = 600;
    public static final int STORM_MAX_TICKS = 1200;
    /** The dump's "100-to-128-block radius" round a Mausoleum: ours, within 128 of its middle. */
    public static final double MAUSOLEUM_REACH = 128.0;
    /** Ours: the Dread rise round a Mausoleum as on a clear day in the Dreadlands, one in 12 a player every 10 s. */
    public static final int MAUSOLEUM_ONE_IN = 12;
    /** The storm's cooldown, by default: her encounter's reset. */
    public static final double DEFAULT_STORM_HOURS = 6.0;

    private EscalationRules() {
    }

    /** How long a storm lasts, from a roll in 0 to {@code bound} - 1. */
    public static int stormTicks(IntUnaryOperator nextInt) {
        return STORM_MIN_TICKS + nextInt.applyAsInt(STORM_MAX_TICKS - STORM_MIN_TICKS + 1);
    }

    /** Whether a defeat at {@code now} brings a storm, the last having come at {@code last} (0: never). */
    public static boolean stormDue(long now, long last, long cooldownMillis) {
        return last == 0L || now - last >= cooldownMillis;
    }

    /** Whether a death is a defeat: anything but a command's {@code /kill} or falling out of the world. */
    public static boolean defeat(boolean killCommand, boolean outOfWorld) {
        return !killCommand && !outOfWorld;
    }
}
