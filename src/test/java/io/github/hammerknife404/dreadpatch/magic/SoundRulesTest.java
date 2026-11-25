package io.github.hammerknife404.dreadpatch.magic;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SoundRulesTest {
    @Test
    void noWeatherNoMuffling() {
        assertEquals(1.0F, SoundRules.muffle(0.0F, 100.0), 1e-6);
        assertEquals(1.0F, SoundRules.muffle(1.0F, 0.0), 1e-6);
    }

    @Test
    void fasterTheFurtherAndTheWorse() {
        assertEquals(0.5F, SoundRules.muffle(1.0F, SoundRules.HALF_AT), 1e-6);
        float near = SoundRules.muffle(1.0F, 8.0);
        float far = SoundRules.muffle(1.0F, 16.0);
        // Twice as far keeps less than half as much: 0.5, then 0.2.
        assertEquals(0.2F, far, 1e-6);
        assertTrue(far < near / 2.0F);
        assertTrue(SoundRules.muffle(SoundRules.SNOW, 16.0) > SoundRules.muffle(SoundRules.THUNDER, 16.0));
        assertTrue(SoundRules.muffle(SoundRules.THUNDER, 16.0) > SoundRules.muffle(SoundRules.WHITEOUT, 16.0));
    }

    @Test
    void musicIsMuffledEvenClose() {
        assertEquals(SoundRules.MUSIC_AT, SoundRules.heardAt(true, 0.0), 1e-9);
        assertEquals(20.0, SoundRules.heardAt(true, 20.0), 1e-9, "a jukebox far off is as far as it is");
        assertEquals(2.0, SoundRules.heardAt(false, 2.0), 1e-9, "anything else is where it is");
        assertEquals(0.5F, SoundRules.muffle(SoundRules.WHITEOUT, SoundRules.heardAt(true, 0.0)), 1e-6, "half in a whiteout");
    }
}
