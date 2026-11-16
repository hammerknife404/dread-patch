package io.github.hammerknife404.dreadpatch.magic;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CrownShapeTest {
    @Test
    void holdsTheRing() {
        assertTrue(CrownShape.contains(0, 0, 0));
        assertTrue(CrownShape.contains(2.9, 0, 0));
        assertTrue(CrownShape.contains(2.1, 1.0, 2.1));     // 2.97 from the centre, diagonally
        assertFalse(CrownShape.contains(2.2, 0, 2.2));      // 3.11: just outside
        assertFalse(CrownShape.contains(3.1, 0, 0));
    }

    @Test
    void holdsFromJustBelowItsBaseToAboveHeadHeight() {
        assertTrue(CrownShape.contains(0, -1.0, 0));        // a target a block down a slope
        assertTrue(CrownShape.contains(0, 2.5, 0));
        assertFalse(CrownShape.contains(0, -1.1, 0));
        assertFalse(CrownShape.contains(0, 2.6, 0));        // something flying over it
    }

    @Test
    void aBiggerRingHoldsWhatTheCrownDoesNot() {
        // Sovereign's Eclipse: radius 8, 4 high.
        assertTrue(CrownShape.contains(7.5, 0.0, 0.0, 8.0, 4.0));
        assertTrue(CrownShape.contains(5.0, 3.9, 5.0, 8.0, 4.0));
        assertFalse(CrownShape.contains(7.5, 0.0, 0.0));
        assertFalse(CrownShape.contains(6.0, 0.0, 6.0, 8.0, 4.0), "8.5 out");
        assertFalse(CrownShape.contains(0.0, 4.1, 0.0, 8.0, 4.0), "above it");
        assertFalse(CrownShape.contains(0.0, -1.1, 0.0, 8.0, 4.0), "below it");
    }
}
