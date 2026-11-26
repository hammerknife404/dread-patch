package io.github.hammerknife404.dreadpatch.magic;

/**
 * Game-free rules for Ice and Fire's dread knights (0.75.0, the owner's Feature_Dump_11-24: "Scaled 50%
 * larger, move faster, adjust step height to match size, and can duck into 2-block-tall hallways and still
 * attack"; then "should not be 50% faster, keep them the same speed or at most an 8th faster. They should
 * also duck like a player to 'squeez' into lower areas").
 */
public final class KnightRules {
    /** Half again as large: Ice and Fire's 0.6 x 1.8 becomes 0.9 x 2.7. */
    public static final double SCALE_BONUS = 0.5;
    /** An eighth faster, the owner's most. */
    public static final double SPEED_BONUS = 0.125;
    /** A step a full block high (a mob's 0.6, grown with it, and a little more, so it walks up blocks). */
    public static final double STEP_BONUS = 0.4;
    /** Crouched it stands this tall, fitting a 2-block passage, as a player crouches into 1.5. */
    public static final float CROUCH_HEIGHT = 1.8F;
    /** Its eyes, crouched: as a standing mob's, 0.85 of its height. */
    public static final float CROUCH_EYES = 0.85F;
    /** How it's drawn crouched: leaning forward, squashed, so it fits under what it ducks under. */
    public static final float LEAN_DEGREES = 25.0F;

    private KnightRules() {
    }

    /** Its crouched height, given its standing height: never more than standing. */
    public static float crouchedHeight(float standing) {
        return Math.min(standing, CROUCH_HEIGHT);
    }

    /** The vertical squash that, with the lean, draws a {@code standing}-tall body {@code crouched} tall. */
    public static float drawnSquash(float standing, float crouched) {
        if (standing <= 0.0F || crouched >= standing) {
            return 1.0F;
        }
        return (float) (crouched / (standing * Math.cos(Math.toRadians(LEAN_DEGREES))));
    }
}
