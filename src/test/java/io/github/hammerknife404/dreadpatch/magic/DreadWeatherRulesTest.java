package io.github.hammerknife404.dreadpatch.magic;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DreadWeatherRulesTest {
    @Test
    void itReachesTheColdWithEitherToggleAndTheRestOnlyWithAnywhere() {
        assertFalse(DreadWeatherRules.enabled(false, false), "both off by default: none");
        assertTrue(DreadWeatherRules.reaches(true, false, true, false, false));
        assertTrue(DreadWeatherRules.reaches(true, false, false, true, false));
        assertFalse(DreadWeatherRules.reaches(false, false, true, false, true), "cold only: not the temperate lands");
        assertTrue(DreadWeatherRules.reaches(false, false, false, true, false));
        assertFalse(DreadWeatherRules.reaches(false, true, false, true, false), "the hot lands only now and then");
        assertTrue(DreadWeatherRules.reaches(false, true, false, true, true));
    }

    @Test
    void itsWarningFadesInHoldsAndFadesOut() {
        assertEquals(0.0F, DreadWeatherRules.alertAlpha(0));
        assertEquals(1.0F, DreadWeatherRules.alertAlpha(60));
        assertTrue(DreadWeatherRules.alertAlpha(DreadWeatherRules.ALERT_TICKS - 10) < 0.5F);
        assertEquals(0.0F, DreadWeatherRules.alertAlpha(DreadWeatherRules.ALERT_TICKS));
    }
}
