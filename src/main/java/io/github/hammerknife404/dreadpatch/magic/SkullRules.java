package io.github.hammerknife404.dreadpatch.magic;

/**
 * Lich Skull (0.98.5, the owner: "Turn the innate spell ability of the dread lich staff into its own spell. At higher
 * levels it fires up to 5 total projectiles that first try to seek different targets"): Ice and Fire's lich skull, one a
 * level up to {@link #MAX_SKULLS}. Each is given a target, the different ones first ({@link #targetFor}): with fewer
 * targets than skulls, they're shared round again.
 */
public final class SkullRules {
    public static final int MAX_SKULLS = 5;
    /**
     * How far it looks for targets, and how hard a skull turns toward its own: its heading each tick a {@link #TURN}
     * blend of its flight and the way to its target, straight at it within {@link #CLOSE} (a looser turn circled its
     * target, never closing).
     */
    public static final double RANGE = 24.0;
    public static final double TURN = 0.35;
    public static final double CLOSE = 3.0;
    /** The least of its heading kept across, not up or down (0.98.13): Ice and Fire's skull goes under 0.1 a tick across. */
    public static final double MIN_ACROSS = 0.15;
    /** Its speed (blocks a tick), how wide the volley fans out (degrees each side), and how long a skull flies (ticks). */
    public static final double SPEED = 1.2;
    public static final float FAN = 20.0F;
    public static final int LIFETIME = 100;
    /** Its damage, Ice and Fire's skull's (scaled by spell power). */
    public static final float DAMAGE = 6.0F;

    private SkullRules() {
    }

    /** How much of its heading, at {@code distance} from its target, a skull turns toward it a tick. */
    public static double turn(double distance) {
        return distance <= CLOSE ? 1.0 : TURN;
    }

    public static int skulls(int spellLevel) {
        return Math.max(1, Math.min(MAX_SKULLS, spellLevel));
    }

    /** Which of {@code targets} (best first) skull {@code skull} seeks: each a different one while there are, then round again; -1 if none. */
    public static int targetFor(int skull, int targets) {
        return targets <= 0 ? -1 : skull % targets;
    }

    /** Skull {@code skull} of {@code count}'s turn from straight ahead (degrees), the volley fanned evenly. */
    public static float fan(int skull, int count) {
        return count <= 1 ? 0.0F : -FAN + 2.0F * FAN * skull / (count - 1);
    }
}
