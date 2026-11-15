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
}
