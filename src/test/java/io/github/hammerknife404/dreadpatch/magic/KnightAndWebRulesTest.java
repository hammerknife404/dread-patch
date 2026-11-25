package io.github.hammerknife404.dreadpatch.magic;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class KnightAndWebRulesTest {
    @Test
    void aKnightCrouchesIntoTwoBlocks() {
        assertEquals(1.8F, KnightRules.crouchedHeight(2.7F));
        assertEquals(1.5F, KnightRules.crouchedHeight(1.5F));
        float squash = KnightRules.drawnSquash(2.7F, 1.8F);
        double drawn = 2.7 * squash * Math.cos(Math.toRadians(KnightRules.LEAN_DEGREES));
        assertEquals(1.8, drawn, 1e-4);
        assertEquals(1.0F, KnightRules.drawnSquash(1.5F, 1.5F));
        assertTrue(KnightRules.SPEED_BONUS <= 0.125);
    }

    @Test
    void dreadArmourEasesWebsByHalfAtMost() {
        assertEquals(0.0, WebRules.ease(0));
        assertEquals(0.5, WebRules.ease(4));
        assertEquals(0.5, WebRules.ease(6));
        assertEquals(0.625, WebRules.eased(0.25, 0.5), 1e-9);
        assertEquals(0.25, WebRules.eased(0.25, 0.0), 1e-9);
    }
}
