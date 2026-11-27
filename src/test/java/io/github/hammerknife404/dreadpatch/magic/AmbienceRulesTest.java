package io.github.hammerknife404.dreadpatch.magic;

import org.junit.jupiter.api.Test;

import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AmbienceRulesTest {
    @Test
    void aCrackEverySixtyToTwoHundredAndFortySeconds() {
        Random random = new Random(1);
        int least = Integer.MAX_VALUE;
        int most = 0;
        for (int i = 0; i < 20000; i++) {
            int delay = AmbienceRules.crackDelay(random::nextInt);
            least = Math.min(least, delay);
            most = Math.max(most, delay);
        }
        assertEquals(60 * 20, least);
        assertEquals(240 * 20, most);
    }

    @Test
    void halfAloneHalfInBurstsOfTwoOrThree() {
        Random random = new Random(2);
        int[] sizes = new int[4];
        for (int i = 0; i < 60000; i++) {
            int size = AmbienceRules.burstSize(random::nextInt);
            assertTrue(size >= 1 && size <= 3);
            sizes[size]++;
        }
        assertEquals(0.5, sizes[1] / 60000.0, 0.01);
        assertEquals(1.0 / 3.0, sizes[2] / 60000.0, 0.01);
        assertEquals(1.0 / 6.0, sizes[3] / 60000.0, 0.01);
    }

    @Test
    void aBurstsCracksComeBackToBack() {
        Random random = new Random(3);
        for (int i = 0; i < 1000; i++) {
            int gap = AmbienceRules.burstGap(random::nextInt);
            assertTrue(gap >= 3 && gap <= 10, "a few ticks apart: " + gap);
        }
    }
}
