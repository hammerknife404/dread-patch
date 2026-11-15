package io.github.hammerknife404.dreadpatch.magic;

/** Game-free: the space a Rimefire Crown holds, a cylinder round its base. */
public final class CrownShape {
    /** Blocks from the centre to the spikes: a six-block ring. */
    public static final float RADIUS = 3.0F;
    /** How far above and below its base it holds. */
    public static final double HEIGHT = 2.5;
    public static final double DEPTH = 1.0;

    private CrownShape() {
    }

    /** Whether a point, relative to the crown's base, is inside it. */
    public static boolean contains(double dx, double dy, double dz) {
        return dx * dx + dz * dz <= RADIUS * RADIUS && dy >= -DEPTH && dy <= HEIGHT;
    }
}
