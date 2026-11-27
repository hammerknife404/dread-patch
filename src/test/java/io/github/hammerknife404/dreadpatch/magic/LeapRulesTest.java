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
}
