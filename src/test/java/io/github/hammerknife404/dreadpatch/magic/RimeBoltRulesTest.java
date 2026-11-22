package io.github.hammerknife404.dreadpatch.magic;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class RimeBoltRulesTest {
    @Test
    void aPlayersBoltScalesFromThreePercent() {
        assertEquals(0.03F, RimeBoltRules.spellShare(1), 1e-6F);
        assertEquals(0.05F, RimeBoltRules.spellShare(3), 1e-6F);
        assertEquals(0.07F, RimeBoltRules.spellShare(5), 1e-6F);
    }

    @Test
    void hersByDifficulty() {
        assertEquals(0.05F, RimeBoltRules.queenShare(1), 1e-6F);
        assertEquals(0.05F, RimeBoltRules.queenShare(2), 1e-6F);
        assertEquals(0.08F, RimeBoltRules.queenShare(3), 1e-6F);
    }
}
