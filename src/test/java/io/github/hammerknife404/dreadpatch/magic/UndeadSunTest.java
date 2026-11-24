package io.github.hammerknife404.dreadpatch.magic;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class UndeadSunTest {
    /** Vanilla's sun lights a mob for 8 s; a mob that's never burnt has -1 fire ticks. */
    private static final int SUN = 160;

    @Test
    void theSunsFireIsPutBack() {
        assertEquals(-1, UndeadSun.keptFire(-1, SUN, false));
        assertEquals(0, UndeadSun.keptFire(0, SUN, false));
    }

    @Test
    void aBurningMobKeepsBurningDown() {
        // Lit by a sword's Fire Aspect, then the sun tops it up to 8 s: it burns on as it was, a tick less.
        assertEquals(59, UndeadSun.keptFire(60, SUN, false));
    }

    @Test
    void realFireIsKept() {
        assertEquals(SUN, UndeadSun.keptFire(-1, SUN, true));
        assertEquals(300, UndeadSun.keptFire(10, 300, true));
    }

    @Test
    void fireBurningDownIsLeftAlone() {
        assertEquals(99, UndeadSun.keptFire(100, 99, false));
        assertEquals(-1, UndeadSun.keptFire(-1, -1, false));
    }
}
