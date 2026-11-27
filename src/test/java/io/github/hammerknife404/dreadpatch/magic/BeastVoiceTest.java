package io.github.hammerknife404.dreadpatch.magic;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BeastVoiceTest {
    @Test
    void aLargerBeastSoundsDeeperAndALittleLouder() {
        assertEquals(1.0F, BeastVoice.pitch(1.0F), 1e-6);
        assertEquals(1.0F, BeastVoice.volume(1.0F), 1e-6);
        assertTrue(BeastVoice.pitch(1.35F) < 1.0F && BeastVoice.pitch(0.85F) > 1.0F);
        assertTrue(BeastVoice.volume(1.35F) > 1.0F && BeastVoice.volume(0.85F) < 1.0F);
        assertTrue(BeastVoice.pitch(1.35F) > 0.85F, "slightly: " + BeastVoice.pitch(1.35F));
        assertEquals((float) BeastVoice.MIN, BeastVoice.pitch(5.0F), 1e-6);
    }
}
