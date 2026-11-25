package io.github.hammerknife404.dreadpatch.magic;

/**
 * Game-free: the Dreadlands' sun doesn't burn the undead (0.60.0). Vanilla lights a sun-sensitive mob
 * from inside its own tick ({@code Mob.isSunBurnTick}, protected, with no event to stop it), so
 * {@code world/DreadlandsUndead} compares its fire before and after that tick: fire that rose with no
 * fire or lava touching it came from the sun, and is put back.
 */
public final class UndeadSun {
    private UndeadSun() {
    }

    /**
     * The fire ticks an undead mob keeps after its own tick in the Dreadlands: {@code after}, unless they
     * rose from {@code before} with nothing burning touching it. Then {@code before}, a tick burnt down if
     * it was already alight (its tick counts its fire down first).
     */
    public static int keptFire(int before, int after, boolean touchingFire) {
        if (after <= before || touchingFire) {
            return after;
        }
        return before > 0 ? before - 1 : before;
    }
}
