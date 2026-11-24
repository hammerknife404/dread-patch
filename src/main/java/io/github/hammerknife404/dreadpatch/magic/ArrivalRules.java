package io.github.hammerknife404.dreadpatch.magic;

/**
 * Game-free: where a Dread Gate sets someone down in the Dreadlands (0.60.0). The owner: "Arrival
 * lands the player somewhere random, but never on a tower or within 48 blocks from one. And prefer
 * the surface, but caves or similar are ok." (Until then: 28-36 blocks from the nearest spire's
 * middle, facing it.)
 */
public final class ArrivalRules {
    /** Ours: anywhere within this many blocks of the gate's own x and z. */
    public static final double RANGE = 256.0;
    /** The owner's: never within this of a spire. Measured from the farthest any spire reaches. */
    public static final double CLEARANCE = 48.0;
    /** So a spot this far from a spire's middle is clear of it. */
    public static final double FROM_MIDDLE = SpireShape.MAX_REACH + CLEARANCE;
    /** Spots tried on the surface before any lower down (a cave). */
    public static final int TRIES = 24;

    private ArrivalRules() {
    }

    /** Whether a spot {@code dx}, {@code dz} from a spire's middle is clear of it. */
    public static boolean clearOfSpire(double dx, double dz) {
        return dx * dx + dz * dz >= FROM_MIDDLE * FROM_MIDDLE;
    }

    /** A spot within {@link #RANGE}, spread evenly over the disc, from two uniform rolls in [0, 1): {x, z} offsets. */
    public static double[] offset(double u, double v) {
        double r = RANGE * Math.sqrt(u);
        double angle = v * Math.PI * 2.0;
        return new double[]{r * Math.cos(angle), r * Math.sin(angle)};
    }
}
