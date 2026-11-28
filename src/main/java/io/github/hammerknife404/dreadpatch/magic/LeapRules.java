package io.github.hammerknife404.dreadpatch.magic;

/**
 * The Dread leaping gaps (0.97.3, the owner: "Ghouls, beasts and dread knights should now be able to jump over at least one
 * block, and can jump while wondering or patroling. The slower thralls remain the same, to slow to jump"; then "jump
 * horizontally over say a 1 or two block wide trench"). Vanilla's pathfinding never crosses a gap: a mob walks round it
 * or falls in. Heading somewhere (chasing, wandering, on patrol), one at a gap 1-{@link #MAX_GAP} wide right ahead, deeper
 * than a step, with ground to land on at its own height (or one lower) past it, leaps it: up as a jump, and across as fast
 * as it needs to land {@link #MARGIN} past the far edge, by vanilla's own flight (gravity, air drag).
 */
public final class LeapRules {
    /** The widest gap it leaps (blocks), and how far past the far edge it means to land. */
    public static final int MAX_GAP = 2;
    public static final double MARGIN = 0.6;
    /** Its leap's rise: a little over a jump's 0.42, for the time aloft. */
    public static final double RISE = 0.5;
    /** Ticks between leaps. */
    public static final int COOLDOWN = 20;
    /** Vanilla's flight: gravity a tick, and what's kept of the speed each tick, up and across. */
    static final double GRAVITY = 0.08;
    static final double DRAG_Y = 0.98;
    static final double DRAG_XZ = 0.91;
    /** The tick it leaves the ground, the ground's friction (a block's 0.6, by 0.91) is what's kept instead. */
    static final double GROUND_DRAG = 0.6 * 0.91;

    private LeapRules() {
    }

    /**
     * How far across a leap at {@code speed} (blocks a tick) carries before it's back down to where it started less
     * {@code drop} blocks (0, or 1 to land a block lower).
     */
    public static double reach(double speed, double drop) {
        double y = 0.0;
        double vy = RISE;
        double x = 0.0;
        double vx = speed;
        for (int tick = 0; tick < 200; tick++) {
            y += vy;
            x += vx;
            vy = (vy - GRAVITY) * DRAG_Y;
            vx *= tick == 0 ? GROUND_DRAG : DRAG_XZ;
            if (vy < 0 && y <= -drop) {
                return x;
            }
        }
        return x;
    }

    /** The speed across that carries {@code distance} blocks (to land {@code drop} lower), found by halving. */
    public static double speedFor(double distance, double drop) {
        double low = 0.0;
        double high = 2.0;
        for (int i = 0; i < 30; i++) {
            double mid = (low + high) / 2;
            if (reach(mid, drop) < distance) {
                low = mid;
            } else {
                high = mid;
            }
        }
        return high;
    }

    /**
     * The queen's hops in a fight (0.98.0, the owner's handoff: "Give the Dread Queen active jumping in combat"): circling
     * or giving ground at casting range, now and then she hops {@link #HOP_DISTANCE} aside, the way she's circling, and
     * back from her target as much again when giving ground; one in {@link #HOP_ONE_IN} of her strafing ticks once
     * {@link #HOP_MIN}-{@link #HOP_MAX} ticks have passed since the last.
     */
    public static final double HOP_DISTANCE = 3.0;
    public static final int HOP_ONE_IN = 15;
    public static final int HOP_MIN = 50;
    public static final int HOP_MAX = 110;

    /**
     * Which way she hops, flat and of length 1: aside from {@code (toX, toZ)} (toward her target), to its left when
     * {@code clockwise}, and back as much again when {@code back}.
     */
    public static double[] hop(double toX, double toZ, boolean clockwise, boolean back) {
        double length = Math.sqrt(toX * toX + toZ * toZ);
        if (length < 1.0E-6) {
            return new double[]{0.0, 0.0};
        }
        double fx = toX / length;
        double fz = toZ / length;
        // Aside: the forward turned a quarter (left in Minecraft's x/z when clockwise).
        double sx = clockwise ? fz : -fz;
        double sz = clockwise ? -fx : fx;
        double x = sx - (back ? fx : 0.0);
        double z = sz - (back ? fz : 0.0);
        double out = Math.sqrt(x * x + z * z);
        return new double[]{x / out, z / out};
    }

    /** Whether a gap {@code gap} blocks wide (counted from right ahead) is one it leaps. */
    public static boolean leaps(int gap) {
        return gap >= 1 && gap <= MAX_GAP;
    }
}
