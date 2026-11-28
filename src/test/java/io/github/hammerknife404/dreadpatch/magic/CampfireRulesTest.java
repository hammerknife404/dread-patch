package io.github.hammerknife404.dreadpatch.magic;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CampfireRulesTest {
    @Test
    void campfiresFadeMoreSlowly() {
        assertEquals(600, CampfireRules.step(600, false));
        assertTrue(CampfireRules.step(600, true) > 600);
    }

    @Test
    void foodGoesToAshInItsTime() {
        assertFalse(CampfireRules.sublimated(CampfireRules.SUBLIMATE_TICKS - 1));
        assertTrue(CampfireRules.sublimated(CampfireRules.SUBLIMATE_TICKS));
    }
}
