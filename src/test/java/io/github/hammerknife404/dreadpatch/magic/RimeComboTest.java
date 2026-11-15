package io.github.hammerknife404.dreadpatch.magic;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RimeComboTest {
    private static final UUID A = new UUID(0, 1);
    private static final UUID B = new UUID(0, 2);

    @Test
    void everyThirdStrikeOnAChilledTargetShatters() {
        RimeCombo combo = new RimeCombo();
        // The first strike finds it unchilled; its Withering Rime chills it for the rest.
        assertFalse(combo.strike(A, true, false, false));
        assertFalse(combo.strike(A, true, true, false));
        assertTrue(combo.strike(A, true, true, false));
        assertFalse(combo.strike(A, true, true, false));
        assertFalse(combo.strike(A, true, true, false));
        assertTrue(combo.strike(A, true, true, false));
    }

    @Test
    void waitsForTheFrost() {
        RimeCombo combo = new RimeCombo();
        assertFalse(combo.strike(A, true, false, false));
        assertFalse(combo.strike(A, true, false, false));
        assertFalse(combo.strike(A, true, false, false));
        assertTrue(combo.strike(A, true, true, false));
    }

    @Test
    void weakSwingsDontCount() {
        RimeCombo combo = new RimeCombo();
        assertFalse(combo.strike(A, true, true, false));
        assertFalse(combo.strike(A, false, true, false));
        assertFalse(combo.strike(A, false, true, false));
        assertFalse(combo.strike(A, true, true, false));
        assertTrue(combo.strike(A, true, true, false));
    }

    @Test
    void anotherTargetStartsItOver() {
        RimeCombo combo = new RimeCombo();
        assertFalse(combo.strike(A, true, true, false));
        assertFalse(combo.strike(A, true, true, false));
        assertFalse(combo.strike(B, true, true, false));
        assertFalse(combo.strike(A, true, true, false));
        assertFalse(combo.strike(A, true, true, false));
        assertTrue(combo.strike(A, true, true, false));
    }

    @Test
    void aChargedBladeShattersAtOnceAndStartsOver() {
        RimeCombo combo = new RimeCombo();
        assertFalse(combo.strike(A, true, true, false));
        assertTrue(combo.strike(A, true, false, true));
        assertFalse(combo.strike(A, true, true, false));
        assertFalse(combo.strike(A, true, true, false));
        assertTrue(combo.strike(A, true, true, false));
    }

    @Test
    void aChargedBladeWaitsForAFullStrike() {
        RimeCombo combo = new RimeCombo();
        assertFalse(combo.strike(A, false, true, true));
        assertTrue(combo.strike(A, true, true, true));
    }
}
