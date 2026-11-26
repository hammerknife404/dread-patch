package io.github.hammerknife404.dreadpatch.magic;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SublimateRulesTest {
    @Test
    void reachAndFrostGrowByLevel() {
        assertEquals(8.0, SublimateRules.range(1));
        assertEquals(12.0, SublimateRules.range(5));
        assertEquals(60, SublimateRules.rimefrostTicks(1));
        assertEquals(140, SublimateRules.rimefrostTicks(5));
        assertEquals(20, SublimateRules.whiteoutTicks(1));
        assertEquals(40, SublimateRules.whiteoutTicks(5));
    }

    @Test
    void theCone() {
        // Straight ahead along +z.
        assertTrue(SublimateRules.inCone(0, 0, 5, 0, 0, 1, 8, 0.3));
        assertTrue(SublimateRules.inCone(1.5, 0, 6, 0, 0, 1, 8, 0.3));
        assertFalse(SublimateRules.inCone(4, 0, 4, 0, 0, 1, 8, 0.3));
        assertFalse(SublimateRules.inCone(0, 0, 9, 0, 0, 1, 8, 0.3));
        assertFalse(SublimateRules.inCone(0, 0, -3, 0, 0, 1, 8, 0.3));
        // Right at the tip, a body counts.
        assertTrue(SublimateRules.inCone(0.2, 0, 0, 0, 0, 1, 8, 0.3));
    }
}
