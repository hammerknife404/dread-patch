package io.github.hammerknife404.dreadpatch.magic;

import org.junit.jupiter.api.Test;

import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ArrivalRulesTest {
    @Test
    void everySpireReachesNoFurtherThanItsMost() {
        for (long seed = 0; seed < 2000; seed++) {
            assertTrue(SpireShape.roll(seed).reach() <= SpireShape.MAX_REACH, "seed " + seed);
        }
    }

    @Test
    void neverWithinFortyEightBlocksOfASpire() {
        for (long seed = 0; seed < 200; seed++) {
            SpireShape spire = SpireShape.roll(seed);
            // Just clear: 48 blocks past the farthest this spire reaches is clear.
            double edge = spire.reach() + ArrivalRules.CLEARANCE;
            assertTrue(ArrivalRules.clearOfSpire(edge + (SpireShape.MAX_REACH - spire.reach()), 0.0));
            // On it, or 47 blocks off its edge: not clear.
            assertFalse(ArrivalRules.clearOfSpire(0.0, 0.0));
            assertFalse(ArrivalRules.clearOfSpire(spire.reach() + 47.0, 0.0));
        }
        assertTrue(ArrivalRules.clearOfSpire(ArrivalRules.FROM_MIDDLE, 0.0));
        assertFalse(ArrivalRules.clearOfSpire(ArrivalRules.FROM_MIDDLE * 0.7, ArrivalRules.FROM_MIDDLE * 0.7));
    }

    @Test
    void spotsSpreadEvenlyWithinRange() {
        Random random = new Random(3);
        int inner = 0;
        int n = 40000;
        for (int i = 0; i < n; i++) {
            double[] o = ArrivalRules.offset(random.nextDouble(), random.nextDouble());
            double r = Math.hypot(o[0], o[1]);
            assertTrue(r <= ArrivalRules.RANGE + 1e-9);
            inner += r < ArrivalRules.RANGE / 2.0 ? 1 : 0;
        }
        // Evenly over the disc: a quarter of them in the inner half of its radius.
        assertEquals(0.25, inner / (double) n, 0.01);
    }
}
