package io.github.hammerknife404.dreadpatch.magic;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ColdRulesTest {
    @Test
    void waterToBlueIceInAMinecraftDay() {
        long stage = ColdRules.stageTicks(ColdRules.DEFAULT_MINUTES);
        assertEquals(24000L, stage * ColdRules.STAGES, "three stages, a day of game time");
        ColdRules.Stage s = ColdRules.Stage.WATER;
        long had = 0;
        long ticks = 0;
        while (s != ColdRules.Stage.BLUE_ICE) {
            ticks += ColdRules.CHECK_EVERY;
            if (ColdRules.colder(had, ColdRules.CHECK_EVERY, stage)) {
                s = s.next();
                had = 0;
            } else {
                had += ColdRules.CHECK_EVERY;
            }
        }
        assertEquals(24000L, ticks);
        assertEquals(ColdRules.Stage.BLUE_ICE, s.next(), "blue ice stays");
    }

    @Test
    void zeroNeverFreezes() {
        assertEquals(0L, ColdRules.stageTicks(0.0));
        assertFalse(ColdRules.colder(1_000_000L, 1L, 0L));
        assertTrue(ColdRules.colder(0L, 1L, 1L));
    }
}
