package io.github.hammerknife404.dreadpatch.magic;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GateTimingTest {
    @Test
    void openFifteenToThirtySeconds() {
        assertEquals(300, GateTiming.openTicks(0.0));
        assertEquals(600, GateTiming.openTicks(0.9999999));
        for (double roll = 0.0; roll < 1.0; roll += 0.01) {
            int ticks = GateTiming.openTicks(roll);
            assertTrue(ticks >= 300 && ticks <= 600, "open " + ticks);
        }
    }

    @Test
    void itFadesFasterAndFasterOverTheLastFiveSeconds() {
        for (int left = GateTiming.FADE_TICKS; left < 700; left++) {
            assertTrue(GateTiming.shown(left), "whole until the fade, at " + left);
        }
        assertFalse(GateTiming.shown(0), "gone at the end");
        // Blink lengths, from the fade's start to its end.
        List<Integer> blinks = new ArrayList<>();
        boolean last = GateTiming.shown(GateTiming.FADE_TICKS - 1);
        int run = 0;
        for (int left = GateTiming.FADE_TICKS - 1; left >= 1; left--) {
            boolean now = GateTiming.shown(left);
            if (now != last) {
                blinks.add(run);
                run = 0;
                last = now;
            }
            run++;
        }
        assertTrue(blinks.size() >= 8, "blinks " + blinks);
        for (int i = 1; i < blinks.size(); i++) {
            assertTrue(blinks.get(i) <= blinks.get(i - 1), "each blink no longer than the last: " + blinks);
        }
        assertTrue(blinks.get(0) >= 12 && blinks.get(blinks.size() - 1) <= 3, "from slow to fast: " + blinks);
    }
}
