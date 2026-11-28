package io.github.hammerknife404.dreadpatch.magic;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class QueenSmokeTest {
    @Test
    void herEyesAreInFrontOfHerFace() {
        // Facing +z, level: her eyes ahead in +z, either side in x, above the neck.
        double[] right = QueenSmoke.headPoint(0.0F, 0.0F, QueenSmoke.EYE_APART, QueenSmoke.EYE_UP, QueenSmoke.EYE_FORWARD);
        assertEquals(QueenSmoke.EYE_FORWARD, right[2], 1.0E-9);
        assertEquals(-QueenSmoke.EYE_APART, right[0], 1.0E-9);
        assertEquals(QueenSmoke.NECK + QueenSmoke.EYE_UP, right[1], 1.0E-9);
        // Facing +x (yaw -90): ahead is +x.
        double[] east = QueenSmoke.headPoint(-90.0F, 0.0F, 0.0, 0.0, 1.0);
        assertEquals(1.0, east[0], 1.0E-9);
        assertEquals(0.0, east[2], 1.0E-9);
    }

    @Test
    void lookingDownTipsThemDown() {
        double[] level = QueenSmoke.headPoint(0.0F, 0.0F, 0.0, QueenSmoke.EYE_UP, QueenSmoke.EYE_FORWARD);
        double[] down = QueenSmoke.headPoint(0.0F, 45.0F, 0.0, QueenSmoke.EYE_UP, QueenSmoke.EYE_FORWARD);
        assertTrue(down[1] < level[1]);
        // The crown's top tips forward when she looks down.
        double[] crown = QueenSmoke.headPoint(0.0F, 45.0F, 0.0, QueenSmoke.CROWN_UP, 0.0);
        assertTrue(crown[2] > 0.0);
        assertEquals(QueenSmoke.CROWN_UP, Math.hypot(crown[1] - QueenSmoke.NECK, crown[2]), 1.0E-9);
    }
}
