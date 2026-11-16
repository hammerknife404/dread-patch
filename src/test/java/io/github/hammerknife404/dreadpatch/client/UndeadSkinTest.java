package io.github.hammerknife404.dreadpatch.client;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class UndeadSkinTest {
    private static final int WHITE = UndeadSkin.abgr(255, 240, 240, 240);
    private static final int BONE = UndeadSkin.abgr(255, 200, 200, 190);
    private static final int CLEAR = 0;

    @Test
    void whereThereIsNoSkinThereIsNone() {
        assertEquals(CLEAR, UndeadSkin.pixel(CLEAR, BONE, 1.0, 0));
        assertEquals(CLEAR, UndeadSkin.pixel(CLEAR, CLEAR, 0.0, UndeadSkin.MEMBRANE_DISTANCE));
    }

    @Test
    void scalesGoDarkWhereTheyAreWhole() {
        int scale = UndeadSkin.pixel(WHITE, CLEAR, 0.0, 0);
        assertEquals(255, UndeadSkin.alpha(scale));
        assertTrue(UndeadSkin.luminance(scale) < UndeadSkin.luminance(UndeadSkin.SLATE) + 0.01, "a white scale goes no paler than slate");
        assertTrue(UndeadSkin.pixel(WHITE, CLEAR, UndeadSkin.SCALES_DARKEN, 0) != scale, "rot darkens it further");
    }

    @Test
    void bonesShowOnlyWhereThereAreBonesAndRot() {
        int bone = UndeadSkin.pixel(WHITE, BONE, UndeadSkin.BONES_SHOW, 0);
        assertTrue(UndeadSkin.luminance(bone) >= UndeadSkin.luminance(UndeadSkin.BONE_DIM) - 0.01, "rotted through to the bone");
        assertNotEquals(bone, UndeadSkin.pixel(WHITE, BONE, UndeadSkin.BONES_SHOW - 0.01, 0), "still covered");
        assertNotEquals(bone, UndeadSkin.pixel(WHITE, CLEAR, 1.0, 0), "no bone there to show");
    }

    @Test
    void onlyMembraneTears() {
        assertEquals(CLEAR, UndeadSkin.pixel(WHITE, CLEAR, UndeadSkin.MEMBRANE_TEARS, UndeadSkin.MEMBRANE_DISTANCE), "membrane, far from bone");
        assertNotEquals(CLEAR, UndeadSkin.pixel(WHITE, CLEAR, 1.0, UndeadSkin.MEMBRANE_DISTANCE - 1), "flesh near a bone holds");
        assertNotEquals(CLEAR, UndeadSkin.pixel(WHITE, CLEAR, UndeadSkin.MEMBRANE_TEARS - 0.01, UndeadSkin.MEMBRANE_DISTANCE), "not rotted enough");
    }

    @Test
    void measuresDistanceToTheNearestBone() {
        int width = 20;
        int[] bones = new int[width];
        bones[0] = BONE;
        int[] distance = UndeadSkin.distanceToBone(bones, width, 1);
        assertEquals(0, distance[0]);
        assertEquals(3, distance[3]);
        assertEquals(UndeadSkin.MEMBRANE_DISTANCE, distance[15], "capped");
    }

    @Test
    void rotsTheSameWayEveryTime() {
        int width = 64;
        int height = 32;
        int[] body = new int[width * height];
        int[] bones = new int[width * height];
        java.util.Arrays.fill(body, WHITE);
        for (int i = 0; i < bones.length; i += 7) {
            bones[i] = BONE;
        }
        assertArrayEquals(UndeadSkin.compose(body, bones, width, height), UndeadSkin.compose(body, bones, width, height));
        for (int y = 0; y < height; y += 5) {
            for (int x = 0; x < width; x += 5) {
                double rot = UndeadSkin.rot(x, y);
                assertTrue(rot >= 0.0 && rot <= 1.0, "rot in range at " + x + "," + y);
            }
        }
    }
}
