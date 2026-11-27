package io.github.hammerknife404.dreadpatch.magic;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DreadRecolorTest {
    private static int light(int rgb) {
        return (int) (0.299 * (rgb >> 16 & 0xFF) + 0.587 * (rgb >> 8 & 0xFF) + 0.114 * (rgb & 0xFF));
    }

    @Test
    void clearStaysClearAndAlphaIsKept() {
        assertEquals(0x00123456, DreadRecolor.recolor(0x00123456));
        assertEquals(0x80, DreadRecolor.recolor(0x80A0B0C0) >>> 24);
    }

    @Test
    void darkGoesToSootAndLightToBone() {
        assertEquals(DreadRecolor.SOOT, DreadRecolor.recolor(0xFF000000) & 0xFFFFFF);
        assertEquals(DreadRecolor.BONE, DreadRecolor.recolor(0xFFFFFFFF) & 0xFFFFFF);
    }

    @Test
    void brightColdBlueBecomesHerCyan() {
        int frost = DreadRecolor.recolor(0xFF60B8FF) & 0xFFFFFF;
        assertTrue((frost & 0xFF) > 200 && (frost >> 16 & 0xFF) < 160, "cyan, from " + Integer.toHexString(frost));
    }

    @Test
    void brightnessOrderIsKeptForGreys() {
        int last = -1;
        for (int v = 0; v <= 255; v += 15) {
            int out = DreadRecolor.recolor(0xFF000000 | v << 16 | v << 8 | v) & 0xFFFFFF;
            assertTrue(light(out) >= last, "grey " + v);
            last = light(out);
        }
    }

    @Test
    void metalsKeepAlphaAndBrightnessOrder() {
        for (DreadRecolor.Palette palette : new DreadRecolor.Palette[]{DreadRecolor.Palette.IRON}) {
            assertEquals(0x00123456, DreadRecolor.recolor(0x00123456, palette));
            assertEquals(0x80, DreadRecolor.recolor(0x80A0B0C0, palette) >>> 24);
            int last = -1;
            for (int v = 0; v < 256; v += 5) {
                int out = DreadRecolor.recolor(0xFF000000 | v << 16 | v << 8 | v, palette);
                assertTrue(light(out) >= last, palette + " grey " + v);
                last = light(out);
            }
        }
        assertEquals(DreadRecolor.recolor(0xFF6080A0), DreadRecolor.recolor(0xFF6080A0, DreadRecolor.Palette.DREAD));
    }
}
