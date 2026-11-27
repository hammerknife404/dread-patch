package io.github.hammerknife404.dreadpatch.magic;

/**
 * Game-free: a dread beast's voice by its size (0.85.0, the owner: "scale sound pitch/volume slightly by entity size").
 * Ice and Fire's beasts are 0.85 to 1.35 (the storm raises them to its dead's size); its sounds (a wolf's) are
 * pitched down and a little louder the larger it is, about {@link #PITCH} and {@link #VOLUME} a size from 1, within
 * {@link #MIN} to {@link #MAX} of it.
 */
public final class BeastVoice {
    public static final double PITCH = 0.35;
    public static final double VOLUME = 0.25;
    public static final double MIN = 0.7;
    public static final double MAX = 1.3;

    private BeastVoice() {
    }

    /** What its pitch is multiplied by: lower the larger it is. */
    public static float pitch(float size) {
        return (float) clamp(1.0 - PITCH * (size - 1.0));
    }

    /** What its volume is multiplied by: louder the larger it is. */
    public static float volume(float size) {
        return (float) clamp(1.0 + VOLUME * (size - 1.0));
    }

    private static double clamp(double value) {
        return Math.max(MIN, Math.min(MAX, value));
    }
}
