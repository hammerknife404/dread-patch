package io.github.hammerknife404.dreadpatch.magic;

import io.github.hammerknife404.dreadpatch.magic.Spacing.Move;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SpacingTest {
    @Test
    void closesToCastingRange() {
        assertEquals(Move.APPROACH, Spacing.move(30.0, true, true));
        assertEquals(Move.APPROACH, Spacing.move(16.5, true, true));
        assertEquals(Move.CIRCLE, Spacing.move(16.0, true, true));
    }

    @Test
    void closesInOnWhatSheCantSee() {
        // Her spells need line of sight; out of sight she comes round to it, however near.
        assertEquals(Move.APPROACH, Spacing.move(12.0, false, true));
        assertEquals(Move.APPROACH, Spacing.move(3.0, false, true));
    }

    @Test
    void circlesThenGivesGroundThenRuns() {
        assertEquals(Move.CIRCLE, Spacing.move(12.0, true, true));
        assertEquals(Move.CIRCLE, Spacing.move(9.0, true, true));
        assertEquals(Move.BACK_OFF, Spacing.move(8.9, true, true));
        assertEquals(Move.BACK_OFF, Spacing.move(5.0, true, true));
        assertEquals(Move.FLEE, Spacing.move(4.9, true, true));
        assertEquals(Move.FLEE, Spacing.move(0.5, true, true));
    }

    @Test
    void givesGroundWhileHerFlightIsOnCooldown() {
        assertEquals(Move.BACK_OFF, Spacing.move(2.0, true, false));
        assertEquals(Move.CIRCLE, Spacing.move(12.0, true, false));
    }
}
