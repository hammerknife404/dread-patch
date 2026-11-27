package io.github.hammerknife404.dreadpatch.magic;

import org.junit.jupiter.api.Test;

import java.util.EnumMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class NestRulesTest {
    @Test
    void everyMetalTurnsUp() {
        Map<NestRules.Metal, Integer> seen = new EnumMap<>(NestRules.Metal.class);
        for (int i = 0; i < 1000; i++) {
            seen.merge(NestRules.metal(i / 1000.0), 1, Integer::sum);
        }
        assertEquals(NestRules.Metal.values().length, seen.size(), "each of them: " + seen);
        assertEquals(340, seen.get(NestRules.Metal.GOLD), 2);
        assertEquals(NestRules.Metal.IRON, NestRules.metal(0.9999));
    }

    @Test
    void pilesAreDeepestInTheMiddle() {
        for (int r = NestRules.MIN_RADIUS; r < NestRules.MIN_RADIUS + NestRules.RADII; r++) {
            assertTrue(NestRules.layers(0, r, 0.5) >= 7, "a heap in the middle");
            assertTrue(NestRules.layers(r, r, 0.0) <= 2, "thin at the rim");
            for (double roll = 0; roll < 1; roll += 0.1) {
                int layers = NestRules.layers(r * roll, r, roll);
                assertTrue(layers >= 1 && layers <= 8);
            }
            assertEquals(3, NestRules.sink(0, r));
            assertEquals(0, NestRules.sink(r, r));
        }
    }

    @Test
    void dragonsAreStageTwoToFiveAndNowAndThenAncient() {
        int[] stages = new int[7];
        int ancient = 0;
        for (int i = 0; i < 1000; i++) {
            int days = NestRules.ageDays(i / 1000.0, (i * 7 % 1000) / 1000.0);
            assertTrue(days >= 25 && days < NestRules.ANCIENT_DAYS + 25, "age " + days);
            stages[NestRules.stage(days)]++;
            ancient += days >= NestRules.ANCIENT_DAYS ? 1 : 0;
        }
        assertEquals(0, stages[1], "never a hatchling");
        for (int s = 2; s <= 5; s++) {
            assertTrue(stages[s] > 0, "stage " + s);
        }
        assertEquals(50, ancient, 2, "one in twenty ancient");
    }
}
