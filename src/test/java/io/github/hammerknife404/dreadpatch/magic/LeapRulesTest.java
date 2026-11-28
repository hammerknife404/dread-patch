package io.github.hammerknife404.dreadpatch.magic;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LeapRulesTest {
    @Test
    void itsSpeedCarriesItExactlyAsFar() {
        for (double distance : new double[]{1.5, 2.6, 3.6}) {
            double speed = LeapRules.speedFor(distance, 0.0);
            assertEquals(distance, LeapRules.reach(speed, 0.0), 0.05);
            // Landing a block lower, it carries further at the same speed.
            assertTrue(LeapRules.reach(speed, 1.0) > distance);
        }
    }

    @Test
    void aTwoBlockTrenchTakesAPounceNotAFling() {
        // Off its edge over 2 blocks and its margin: about half a block a tick as it leaves the ground (whose friction
        // takes near half of it that tick), a pounce rather than a fling.
        double speed = LeapRules.speedFor(0.5 + LeapRules.MAX_GAP + LeapRules.MARGIN, 0.0);
        assertTrue(speed > 0.3 && speed < 0.7, "speed " + speed);
    }

    @Test
    void onlyNarrowGaps() {
        assertFalse(LeapRules.leaps(0));
        assertTrue(LeapRules.leaps(1));
        assertTrue(LeapRules.leaps(2));
        assertFalse(LeapRules.leaps(3));
    }

    @Test
    void herHopsGoAsideAndBack() {
        // Her target due +x: aside is along z, and back is toward -x; always of length 1.
        double[] aside = LeapRules.hop(5.0, 0.0, true, false);
        assertEquals(0.0, aside[0], 1.0E-9);
        assertEquals(1.0, Math.abs(aside[1]), 1.0E-9);
        double[] other = LeapRules.hop(5.0, 0.0, false, false);
        assertEquals(-aside[1], other[1], 1.0E-9);
        double[] back = LeapRules.hop(5.0, 0.0, true, true);
        assertTrue(back[0] < 0.0);
        assertEquals(1.0, Math.hypot(back[0], back[1]), 1.0E-9);
        assertEquals(Math.signum(aside[1]), Math.signum(back[1]));
        double[] none = LeapRules.hop(0.0, 0.0, true, true);
        assertEquals(0.0, Math.hypot(none[0], none[1]), 1.0E-9);
    }

    @Test
    void herHopIsAPounceToo() {
        double speed = LeapRules.speedFor(LeapRules.HOP_DISTANCE, 0.0);
        assertTrue(speed > 0.3 && speed < 0.8, "speed " + speed);
        assertTrue(LeapRules.HOP_MIN < LeapRules.HOP_MAX);
    }
}
