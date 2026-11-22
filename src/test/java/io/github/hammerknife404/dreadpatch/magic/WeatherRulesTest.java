package io.github.hammerknife404.dreadpatch.magic;

import org.junit.jupiter.api.Test;

import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WeatherRulesTest {
    /** Runs {@code storms} storms through, each seen starting and ending. How many were whiteouts. */
    private static int whiteouts(WeatherRules.Storms storms, int count, int oneIn, boolean queenAbout, Random random) {
        int whiteouts = 0;
        boolean last = storms.lastWhiteout;
        for (int i = 0; i < count; i++) {
            storms.update(true, queenAbout, oneIn, random::nextInt);
            assertFalse(last && storms.whiteout, "never two in a row");
            last = storms.whiteout;
            whiteouts += storms.whiteout ? 1 : 0;
            storms.update(false, queenAbout, oneIn, random::nextInt);
            assertEquals(last, storms.lastWhiteout);
        }
        return whiteouts;
    }

    @Test
    void aboutOneStormInSixNeverTwoInARow() {
        int n = whiteouts(new WeatherRules.Storms(), 60000, WeatherRules.WHITEOUT_ONE_IN, false, new Random(1));
        assertEquals(1.0 / 6.0, n / 60000.0, 0.01);
    }

    @Test
    void twoOrLessIsEveryOtherStormAndNoneIsNever() {
        assertEquals(500, whiteouts(new WeatherRules.Storms(), 1000, 2, false, new Random(2)));
        assertEquals(500, whiteouts(new WeatherRules.Storms(), 1000, 1, false, new Random(3)));
        assertEquals(0, whiteouts(new WeatherRules.Storms(), 1000, 0, false, new Random(4)));
    }

    @Test
    void neverStartsWhileAQueenIsAbout() {
        assertEquals(0, whiteouts(new WeatherRules.Storms(), 1000, 2, true, new Random(5)));
    }

    @Test
    void herFightBreaksItAndItStillCountsAsOne() {
        WeatherRules.Storms storms = new WeatherRules.Storms();
        storms.update(true, false, 2, n -> 0);
        assertTrue(storms.whiteout);
        assertTrue(storms.breakWhiteout(), "her fight breaks it");
        assertFalse(storms.whiteout);
        assertFalse(storms.breakWhiteout(), "once");
        storms.update(false, false, 2, n -> 0);
        assertTrue(storms.lastWhiteout, "a broken whiteout still keeps the next storm plain");
        storms.update(true, false, 2, n -> 0);
        assertFalse(storms.whiteout);
    }

    @Test
    void theWeatherHoldingChangesNothing() {
        WeatherRules.Storms storms = new WeatherRules.Storms();
        assertTrue(storms.update(true, false, 2, n -> 0));
        assertFalse(storms.update(true, false, 2, n -> 0));
        assertTrue(storms.whiteout, "rolled once, as the storm starts");
    }
}
