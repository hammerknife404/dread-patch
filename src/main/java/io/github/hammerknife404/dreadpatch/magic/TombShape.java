package io.github.hammerknife404.dreadpatch.magic;

/** Game-free: the space a Barrow-Glass Tomb holds, a cylinder round its base. */
public final class TombShape {
    /** Blocks from the centre to the spires: a six-block ring. */
    public static final float RADIUS = 3.0F;
    /** How far above and below its base it holds. */
    public static final double HEIGHT = 2.5;
    public static final double DEPTH = 1.0;

    private TombShape() {
    }

    /** Whether a point, relative to the tomb's base, is inside it. */
    public static boolean contains(double dx, double dy, double dz) {
        return dx * dx + dz * dz <= RADIUS * RADIUS && dy >= -DEPTH && dy <= HEIGHT;
    }
}
