package io.github.hammerknife404.dreadpatch.magic;

/**
 * Game-free: how the queen keeps her distance in a fight, the way Iron's necromancer
 * ({@code WizardAttackGoal}) and Ice and Fire's Dread Lich do. She closes to casting range, circles
 * there, gives ground when pressed, and breaks away when cornered.
 */
public final class Spacing {
    /** Beyond this, or with her target out of sight, she closes in. Inside her bolt's and her crown's range. */
    public static final double CASTING_RANGE = 16.0;
    /** Closer than this she gives ground as she circles. */
    public static final double BACK_OFF_WITHIN = 9.0;
    /** Closer than this she runs, when she can (her flight has a cooldown). */
    public static final double FLEE_WITHIN = 5.0;

    public enum Move {
        APPROACH, CIRCLE, BACK_OFF, FLEE
    }

    private Spacing() {
    }

    /** Her move with her target {@code distance} blocks away. */
    public static Move move(double distance, boolean inSight, boolean canFlee) {
        if (!inSight || distance > CASTING_RANGE) {
            return Move.APPROACH;
        }
        if (distance < FLEE_WITHIN && canFlee) {
            return Move.FLEE;
        }
        return distance < BACK_OFF_WITHIN ? Move.BACK_OFF : Move.CIRCLE;
    }
}
