package io.github.hammerknife404.dreadpatch.magic;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

class TomeRulesTest {
    @Test
    void eachStepTradesIceForRimefrost() {
        assertTrue(TomeRules.RIMEBOUND_ICE_POWER < TomeRules.ICE_BOOK_ICE_POWER);
        assertTrue(TomeRules.RIMEBOUND_RIMEFROST_POWER > TomeRules.ICE_BOOK_ICE_POWER);
        assertTrue(TomeRules.SOVEREIGN_RIMEFROST_POWER > TomeRules.RIMEBOUND_RIMEFROST_POWER + TomeRules.RIMEBOUND_ICE_POWER);
        assertTrue(TomeRules.SOVEREIGN_MANA > TomeRules.RIMEBOUND_MANA);
    }
}
