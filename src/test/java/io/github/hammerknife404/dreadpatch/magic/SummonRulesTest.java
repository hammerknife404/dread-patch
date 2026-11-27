package io.github.hammerknife404.dreadpatch.magic;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SummonRulesTest {
    @Test
    void theSpellSummonsOneFishALevel() {
        assertEquals(1, SummonRules.fishCount(1));
        assertEquals(3, SummonRules.fishCount(3));
        assertEquals(5, SummonRules.fishCount(5));
        assertEquals(5, SummonRules.fishCount(9));
        assertEquals(1, SummonRules.fishCount(0));
    }

    @Test
    void herCooldownsAreAboutThirtySecondsAndOutliveNoSummons() {
        assertEquals(500, SummonRules.cooldown(0.0));
        assertEquals(700, SummonRules.cooldown(0.99999));
        // Her summons go before she can summon that kind again, so a kind never stacks past one wave.
        assertTrue(SummonRules.LIFETIME <= SummonRules.COOLDOWN_MIN);
        assertTrue(Math.abs(SummonRules.LIFETIME - 25 * 20) <= 20);
    }

    @Test
    void sheSpreadsThemThroughAFight() {
        // The first fish come before the first beasts (which wait out the gap if they're close behind).
        assertTrue(SummonRules.first(true, 0.99999) < SummonRules.first(false, 0.0));
        // Ready, but too soon after her last: it waits.
        assertFalse(SummonRules.mayCome(0, SummonRules.GAP - 1));
        assertTrue(SummonRules.mayCome(0, SummonRules.GAP));
        assertFalse(SummonRules.mayCome(1, 10_000));
    }

    @Test
    void oneOrTwoNeverPastTwoStanding() {
        assertEquals(1, SummonRules.toSummon(0, false));
        assertEquals(2, SummonRules.toSummon(0, true));
        assertEquals(1, SummonRules.toSummon(1, true));
        assertEquals(0, SummonRules.toSummon(2, true));
        assertEquals(0, SummonRules.toSummon(5, false));
    }
}
