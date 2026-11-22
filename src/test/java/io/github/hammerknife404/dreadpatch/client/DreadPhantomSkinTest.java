package io.github.hammerknife404.dreadpatch.client;

import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DreadPhantomSkinTest {
    /** A phantom-blue texture, every pixel opaque. */
    private static int[] solid(int size) {
        int[] pixels = new int[size * size];
        Arrays.fill(pixels, DreadPhantomSkin.abgr(67, 81, 138));
        return pixels;
    }

    @Test
    void holesGoThroughBothFacesAndBonesHold() {
        int[] skin = DreadPhantomSkin.skin(solid(64), 64, 64);
        boolean[][] bones = DreadPhantomSkin.bones();
        boolean[][] holes = DreadPhantomSkin.holes(bones);
        int holeCount = 0;
        int membrane = 0;
        for (DreadPhantomSkin.Wing wing : DreadPhantomSkin.WINGS) {
            for (int x = 0; x < wing.span(); x++) {
                for (int z = 0; z < wing.depth(); z++) {
                    int along = wing.start() + x;
                    int row = wing.v() + wing.depth() - 1 - z;
                    int top = skin[row * 64 + wing.topU() + x];
                    int under = skin[row * 64 + wing.underU() + x];
                    if (!bones[along][z]) {
                        membrane++;
                    }
                    if (bones[along][z]) {
                        assertEquals(0xFF, DreadPhantomSkin.alpha(top), "bone shows on top at " + along + "," + z);
                        assertEquals(0xFF, DreadPhantomSkin.alpha(under), "bone shows under at " + along + "," + z);
                    } else if (holes[along][z]) {
                        assertEquals(0, DreadPhantomSkin.alpha(top), "a hole through the top at " + along + "," + z);
                        assertEquals(0, DreadPhantomSkin.alpha(under), "and through the underside");
                        holeCount++;
                    }
                }
            }
        }
        // Torn well past vanilla's ragged edge (which comes on top): at least a quarter of the membrane, but far from all.
        assertTrue(holeCount * 4 >= membrane && holeCount * 2 < membrane, "its wings are torn: " + holeCount + " holes of " + membrane);
        // The leading edge is whole bone along the whole wing.
        for (int x = 0; x < DreadPhantomSkin.SPAN; x++) {
            assertTrue(bones[x][0]);
        }
    }

    @Test
    void aBiggerTextureIsTornTheSameWay() {
        int[] small = DreadPhantomSkin.skin(solid(64), 64, 64);
        int[] big = DreadPhantomSkin.skin(solid(128), 128, 128);
        for (int y = 0; y < 64; y++) {
            for (int x = 0; x < 64; x++) {
                assertEquals(DreadPhantomSkin.alpha(small[y * 64 + x]), DreadPhantomSkin.alpha(big[(2 * y + 1) * 128 + 2 * x + 1]), "at " + x + "," + y);
            }
        }
    }

    @Test
    void itsBlueGoesDreadAndItsEyesCyan() {
        int blue = DreadPhantomSkin.abgr(67, 81, 138);
        int hide = DreadPhantomSkin.hide(blue);
        assertNotEquals(blue, hide);
        assertTrue(DreadPhantomSkin.luminance(hide) < DreadPhantomSkin.luminance(blue), "darker");
        int green = DreadPhantomSkin.abgr(136, 255, 0);
        int eye = DreadPhantomSkin.eyes(new int[]{green})[0];
        assertEquals(0, eye & 0xFF, "no red in its eyes");
        assertTrue((eye >> 16 & 0xFF) == 0xFF, "full blue");
        assertEquals(0, DreadPhantomSkin.eyes(new int[]{0})[0], "transparent stays so");
    }
}
