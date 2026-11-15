package io.github.hammerknife404.dreadpatch.client;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RisePoseTest {
    private static final float EPSILON = 1.0E-4F;

    @Test
    void startsFullyUndergroundWithArmsUp() {
        assertEquals(RisePose.START_DEPTH, RisePose.depth(0.0F), EPSILON);
        assertEquals(1.0F, RisePose.armsUp(0.0F), EPSILON);
    }

    @Test
    void risesSteadilyAndEasesOutAtTheTop() {
        float previous = RisePose.depth(0.0F);
        float previousStep = Float.MAX_VALUE;
        for (float tick = 0.5F; tick <= RisePose.RISE_TICKS; tick += 0.5F) {
            float depth = RisePose.depth(tick);
            float step = previous - depth;
            assertTrue(step > 0.0F, "still rising at tick " + tick);
            assertTrue(step <= previousStep + EPSILON, "slowing down at tick " + tick);
            previous = depth;
            previousStep = step;
        }
    }

    @Test
    void reachesTheSurfaceAsTheArmsStartToLower() {
        assertEquals(0.0F, RisePose.depth(RisePose.RISE_TICKS - 0.001F), 0.01F);
        assertEquals(0.0F, RisePose.depth(RisePose.RISE_TICKS), EPSILON);
        assertEquals(1.0F, RisePose.armsUp(RisePose.RISE_TICKS - 0.001F), EPSILON);
        assertEquals(1.0F, RisePose.armsUp(RisePose.RISE_TICKS), EPSILON);
    }

    @Test
    void lowersTheArmsOverFiveTicksWithoutAJump() {
        float end = RisePose.RISE_TICKS + RisePose.LOWER_TICKS;
        float previous = 1.0F;
        for (float tick = RisePose.RISE_TICKS; tick <= end; tick += 0.25F) {
            float up = RisePose.armsUp(tick);
            assertTrue(up <= previous + EPSILON && previous - up < 0.2F, "smooth at tick " + tick);
            previous = up;
        }
        assertEquals(0.0F, RisePose.armsUp(end - 0.001F), 0.01F);
        assertEquals(0.0F, RisePose.armsUp(end), EPSILON);
        assertEquals(0.0F, RisePose.depth(end), EPSILON);
    }
}
