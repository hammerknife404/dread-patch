package io.github.hammerknife404.dreadpatch.client;

/**
 * Game-free: where a Dread Queen sits on Syndrigossa, in the ice dragon model's own units (blocks at
 * the model's scale; Ice and Fire draws a dragon at its render size / 3, so 10x full-grown). The top
 * of Ice and Fire's {@code BodyUpper} cube (y 7.2 + 7 = 14.2 px), over its middle toward the neck (its
 * cube runs z -10 to -3 px; the head is toward -z). {@code SaddleSeatTest} checks it against the model.
 * GeckoLib hands a layer the pose at the model's origin, turned with the bone, not at the bone itself,
 * so the seat is measured from the origin.
 */
public final class SaddleSeat {
    public static final float X = 0.0F;
    public static final float Y = 14.2F / 16.0F;
    public static final float Z = -7.0F / 16.0F;
    /** From the seat down to a sitting rider's feet, at her own scale: vanilla seats a player 0.6 below what it rides on. */
    public static final float HIPS = 0.6F;

    private SaddleSeat() {
    }
}
