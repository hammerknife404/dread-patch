package io.github.hammerknife404.dreadpatch.magic;

import org.junit.jupiter.api.Test;

import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EscalationRulesTest {
    @Test
    void aStormLastsThirtyToSixtySecondsVarying() {
        Random random = new Random(5);
        int shortest = Integer.MAX_VALUE;
        int longest = 0;
        for (int i = 0; i < 20000; i++) {
            int ticks = EscalationRules.stormTicks(random::nextInt);
            shortest = Math.min(shortest, ticks);
            longest = Math.max(longest, ticks);
        }
        assertEquals(30 * 20, shortest);
        assertEquals(60 * 20, longest);
    }

    @Test
    void oneStormACooldown() {
        long hours = 6L * 3600_000L;
        assertTrue(EscalationRules.stormDue(1000L, 0L, hours), "the first ever");
        assertFalse(EscalationRules.stormDue(1000L + hours - 1, 1000L, hours));
        assertTrue(EscalationRules.stormDue(1000L + hours, 1000L, hours));
        assertTrue(EscalationRules.stormDue(5000L, 1000L, 0L), "a cooldown of 0: every defeat");
    }

    @Test
    void aKillCommandIsNoDefeat() {
        assertTrue(EscalationRules.defeat(false, false));
        assertFalse(EscalationRules.defeat(true, false));
        assertFalse(EscalationRules.defeat(false, true));
    }
}
