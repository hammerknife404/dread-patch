package io.github.hammerknife404.dreadpatch.magic;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RimeComboTest {
    private static final UUID A = new UUID(0, 1);
    private static final UUID B = new UUID(0, 2);
    /** A strike every 13 ticks: a full swing at 1.6 attack speed. */
    private static final long SWING = 13;

    @Test
    void everyThirdStrikeOnAChilledTargetShatters() {
        RimeCombo combo = new RimeCombo();
        // The first strike finds it unchilled; its Withering Rime chills it for the rest.
        assertFalse(combo.strike(A, 0, true, false, false));
        assertFalse(combo.strike(A, SWING, true, true, false));
        assertTrue(combo.strike(A, 2 * SWING, true, true, false));
        assertFalse(combo.strike(A, 3 * SWING, true, true, false));
        assertFalse(combo.strike(A, 4 * SWING, true, true, false));
        assertTrue(combo.strike(A, 5 * SWING, true, true, false));
    }

    @Test
    void waitsForTheFrost() {
        RimeCombo combo = new RimeCombo();
        assertFalse(combo.strike(A, 0, true, false, false));
        assertFalse(combo.strike(A, SWING, true, false, false));
        assertFalse(combo.strike(A, 2 * SWING, true, false, false));
        assertTrue(combo.strike(A, 3 * SWING, true, true, false));
    }

    @Test
    void weakSwingsDontCount() {
        RimeCombo combo = new RimeCombo();
        assertFalse(combo.strike(A, 0, true, true, false));
        assertFalse(combo.strike(A, 5, false, true, false));
        assertFalse(combo.strike(A, 10, false, true, false));
        assertFalse(combo.strike(A, SWING, true, true, false));
        assertTrue(combo.strike(A, 2 * SWING, true, true, false));
    }

    @Test
    void countsEachTargetApart() {
        // A Better Combat swing through two targets strikes both, one after the other, every swing.
        RimeCombo combo = new RimeCombo();
        for (int swing = 0; swing < 2; swing++) {
            assertFalse(combo.strike(A, swing * SWING, true, true, false));
            assertFalse(combo.strike(B, swing * SWING, true, true, false));
        }
        assertTrue(combo.strike(A, 2 * SWING, true, true, false));
        assertTrue(combo.strike(B, 2 * SWING, true, true, false));
    }

    @Test
    void aCountLapses() {
        RimeCombo combo = new RimeCombo();
        assertFalse(combo.strike(A, 0, true, true, false));
        assertFalse(combo.strike(A, SWING, true, true, false));
        long later = SWING + RimeCombo.WINDOW + 1;
        assertFalse(combo.strike(A, later, true, true, false));
        assertFalse(combo.strike(A, later + SWING, true, true, false));
        assertTrue(combo.strike(A, later + 2 * SWING, true, true, false));
    }

    @Test
    void keepsALimitedNumberOfTargets() {
        RimeCombo combo = new RimeCombo();
        assertFalse(combo.strike(A, 0, true, true, false));
        assertFalse(combo.strike(A, 1, true, true, false));
        for (int i = 0; i < RimeCombo.MAX_TARGETS; i++) {
            combo.strike(new UUID(1, i), 2, true, true, false);
        }
        // A was the longest unstruck, so its count went to make room.
        assertFalse(combo.strike(A, 3, true, true, false));
    }

    @Test
    void aChargedBladeShattersAtOnceAndStartsOver() {
        RimeCombo combo = new RimeCombo();
        assertFalse(combo.strike(A, 0, true, true, false));
        assertTrue(combo.strike(A, SWING, true, false, true));
        assertFalse(combo.strike(A, 2 * SWING, true, true, false));
        assertFalse(combo.strike(A, 3 * SWING, true, true, false));
        assertTrue(combo.strike(A, 4 * SWING, true, true, false));
    }

    @Test
    void aChargedBladeWaitsForAFullStrike() {
        RimeCombo combo = new RimeCombo();
        assertFalse(combo.strike(A, 0, false, true, true));
        assertTrue(combo.strike(A, 1, true, true, true));
    }
}
