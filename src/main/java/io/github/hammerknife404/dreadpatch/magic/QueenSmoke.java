package io.github.hammerknife404.dreadpatch.magic;

/**
 * Smoke from her eyes and her crown's top (0.98.1, the owner's handoff: "Add subtle, small smoke particles drifting from
 * the Dread Queen's eyes and the top of her crown"). Where they are on her model (a humanoid, its head's pivot
 * {@link #NECK} above her feet; her eyes {@link #EYE_UP} up its face, {@link #EYE_APART} each side of its middle and
 * {@link #EYE_FORWARD} forward; her crown's top {@link #CROWN_UP} up) as her head turns and tips, and how often each
 * puffs: one tick in {@link #EYE_ONE_IN} each eye, one in {@link #CROWN_ONE_IN} the crown.
 */
public final class QueenSmoke {
    public static final double NECK = 1.5;
    public static final double EYE_UP = 3.5 / 16.0;
    public static final double EYE_APART = 2.0 / 16.0;
    public static final double EYE_FORWARD = 4.3 / 16.0;
    public static final double CROWN_UP = 12.0 / 16.0;
    public static final int EYE_ONE_IN = 9;
    public static final int CROWN_ONE_IN = 5;

    private QueenSmoke() {
    }

    /**
     * A point on her head, {@code right}, {@code up} and {@code forward} of its pivot, from her feet (x, y, z) as her head
     * faces {@code yawDegrees} (Minecraft's: 0 is +z) and tips {@code pitchDegrees} (down positive).
     */
    public static double[] headPoint(float yawDegrees, float pitchDegrees, double right, double up, double forward) {
        double yaw = Math.toRadians(yawDegrees);
        double pitch = Math.toRadians(pitchDegrees);
        double upTipped = up * Math.cos(pitch) - forward * Math.sin(pitch);
        double forwardTipped = forward * Math.cos(pitch) + up * Math.sin(pitch);
        double x = -Math.sin(yaw) * forwardTipped - Math.cos(yaw) * right;
        double z = Math.cos(yaw) * forwardTipped - Math.sin(yaw) * right;
        return new double[]{x, NECK + upTipped, z};
    }
}
