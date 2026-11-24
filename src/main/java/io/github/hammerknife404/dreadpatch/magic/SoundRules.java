package io.github.hammerknife404.dreadpatch.magic;

/**
 * Game-free: the Dread's weather muffling sounds, and our sounds' highs (0.64.0, the owner: "Dread weather
 * severity should muffle sounds around the player faster the further the source is away"; "clip the top end a
 * tiny bit ... on all sounds"; the numbers ours).
 */
public final class SoundRules {
    /** Our sounds keep this much of their highs (OpenAL's low-pass high-frequency gain). */
    public static final float OUR_HIGHS = 0.8F;
    /** At severity 1, a sound this far off is at half. */
    public static final double HALF_AT = 8.0;
    /** The weather's severity: plain snow, a heavy snowstorm, a whiteout; the defeat storm. */
    public static final float SNOW = 0.35F;
    public static final float THUNDER = 0.65F;
    public static final float WHITEOUT = 1.0F;
    public static final float DEFEAT_STORM = 0.35F;

    private SoundRules() {
    }

    /** What's left of a sound {@code distance} off at {@code severity} (0-1): falling faster the further, 1 with none. */
    public static float muffle(float severity, double distance) {
        double d = distance / HALF_AT;
        return (float) (1.0 / (1.0 + severity * d * d));
    }
}
