package io.github.hammerknife404.dreadpatch.magic;

/**
 * Campfires in the Dreadlands (0.98.4, the owner's handoff: "Campfires follow the same thermal progression as torches and
 * lanterns in the Dreadlands, fading to Soul Campfire and then to Dread Campfire, taking longer to cycle due to higher
 * thermal mass and core temperatures"; "Food placed on a Dread Campfire sublimates into ash instead of cooking"). A
 * campfire's steps take {@link #STEP_TIMES} a torch's (the server's {@code flame_fade_*_seconds}, 30-45 s by default:
 * so 1.5-2.25 minutes each). On a lit Dread Campfire, food (anything that's food) holds {@link #SUBLIMATE_TICKS}, then is
 * gone to ash: {@link #ASH} of Ice and Fire's.
 */
public final class CampfireRules {
    public static final int STEP_TIMES = 3;
    public static final int SLOTS = 4;
    public static final int SUBLIMATE_TICKS = 100;
    public static final int ASH = 1;

    private CampfireRules() {
    }

    /** Ticks to a flame's next step, {@code ticks} a torch's: a campfire's take longer. */
    public static int step(int ticks, boolean campfire) {
        return campfire ? ticks * STEP_TIMES : ticks;
    }

    /** Whether food that's lain {@code ticks} on a lit Dread Campfire has gone to ash. */
    public static boolean sublimated(int ticks) {
        return ticks >= SUBLIMATE_TICKS;
    }
}
