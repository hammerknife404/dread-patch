package io.github.hammerknife404.dreadpatch.magic;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EncounterRulesTest {
    private static final long HOUR = 3_600_000L;

    @Test
    void onceAPlayerThenAgainAfterTheHours() {
        long now = 1_000_000_000_000L;
        assertTrue(EncounterRules.mayMeet(null, now, 6 * HOUR), "never met");
        assertFalse(EncounterRules.mayMeet(now - HOUR, now, 6 * HOUR), "an hour ago");
        assertFalse(EncounterRules.mayMeet(now - 6 * HOUR + 1, now, 6 * HOUR), "not quite six hours");
        assertTrue(EncounterRules.mayMeet(now - 6 * HOUR, now, 6 * HOUR), "six hours on");
        assertTrue(EncounterRules.mayMeet(now, now, 0), "no wait configured");
    }
}
