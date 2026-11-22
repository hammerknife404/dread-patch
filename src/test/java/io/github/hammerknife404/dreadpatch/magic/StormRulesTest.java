package io.github.hammerknife404.dreadpatch.magic;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertTrue;

class StormRulesTest {
    @Test
    void aStrikesPatchIsADiscRoundIt() {
        Random random = new Random(4);
        for (int i = 0; i < 200; i++) {
            List<int[]> cells = StormRules.patch(random);
            assertTrue(cells.stream().anyMatch(c -> c[0] == 0 && c[1] == 0), "the strike's own block always burns");
            for (int[] c : cells) {
                assertTrue(c[0] * c[0] + c[1] * c[1] <= StormRules.PATCH_RADIUS * StormRules.PATCH_RADIUS, "within its radius");
            }
            // 21 blocks in the disc; most of them.
            assertTrue(cells.size() >= 6 && cells.size() <= 21, "a patch of " + cells.size());
        }
    }

    @Test
    void itsFogIsAWarningNotAWhiteout() {
        assertTrue(StormRules.FOG_FAR > 10.0F, "further than Whiteout Dominion's 10");
        assertTrue(StormRules.FOG_FAR < 48.0F, "but close: heavy fog");
    }
}
