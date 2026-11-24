package io.github.hammerknife404.dreadpatch.magic;

import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ForgeRulesTest {
    /** A whole forge round (0, 0, 0): bone corners top and bottom, bricks everywhere else, an aperture (OTHER) on the north side. */
    private static Map<String, ForgeRules.Part> whole() {
        Map<String, ForgeRules.Part> parts = new HashMap<>();
        for (int dx = -1; dx <= 1; dx++) {
            for (int dy = -1; dy <= 1; dy++) {
                for (int dz = -1; dz <= 1; dz++) {
                    boolean boneCorner = dy != 0 && dx != 0 && dz != 0;
                    parts.put(dx + "," + dy + "," + dz, boneCorner ? ForgeRules.Part.BONE : ForgeRules.Part.BRICK);
                }
            }
        }
        parts.put("0,0,0", ForgeRules.Part.OTHER);
        parts.put("0,0,-1", ForgeRules.Part.OTHER);
        return parts;
    }

    private static boolean assembled(Map<String, ForgeRules.Part> parts) {
        return ForgeRules.assembled((dx, dy, dz) -> parts.getOrDefault(dx + "," + dy + "," + dz, ForgeRules.Part.OTHER));
    }

    @Test
    void aWholeCubeIsAssembled() {
        assertTrue(assembled(whole()));
    }

    @Test
    void allFourSidesBricksIsAssembledToo() {
        Map<String, ForgeRules.Part> parts = whole();
        parts.put("0,0,-1", ForgeRules.Part.BRICK);
        assertTrue(assembled(parts));
    }

    @Test
    void twoSidesOpenIsNot() {
        Map<String, ForgeRules.Part> parts = whole();
        parts.put("1,0,0", ForgeRules.Part.OTHER);
        assertFalse(assembled(parts));
    }

    @Test
    void everyCornerAndEdgeCounts() {
        for (String spot : whole().keySet()) {
            if (spot.equals("0,0,0") || spot.equals("0,0,-1")) {
                continue;
            }
            Map<String, ForgeRules.Part> parts = whole();
            parts.put(spot, ForgeRules.Part.OTHER);
            assertFalse(assembled(parts), "missing " + spot);
        }
    }

    @Test
    void bricksWhereBonesGoDoNotCount() {
        Map<String, ForgeRules.Part> parts = whole();
        parts.put("1,1,1", ForgeRules.Part.BRICK);
        assertFalse(assembled(parts));
        parts = whole();
        parts.put("1,0,1", ForgeRules.Part.BONE);
        assertFalse(assembled(parts));
    }

    @Test
    void aBreathStartsTheCookAndItRunsWhileLit() {
        double cook = ForgeRules.heat(0, 0.025, 750, true);
        assertEquals(0.025, cook, 1e-9);
        cook = ForgeRules.advance(cook, true, true);
        assertEquals(1.025, cook, 1e-9);
    }

    @Test
    void unlitItHolds() {
        assertEquals(300, ForgeRules.advance(300, false, true), 1e-9);
    }

    @Test
    void notStartedItWaits() {
        assertEquals(0, ForgeRules.advance(0, true, true), 1e-9);
    }

    @Test
    void withNothingToMakeTheCookIsLost() {
        assertEquals(0, ForgeRules.advance(300, true, false), 1e-9);
        assertEquals(0, ForgeRules.heat(300, 0.025, 750, false), 1e-9);
    }

    @Test
    void heatStopsATickPastDone() {
        assertEquals(751, ForgeRules.heat(750.99, 5, 750, true), 1e-9);
        assertTrue(ForgeRules.done(750, 750));
        assertFalse(ForgeRules.done(749.9, 750));
    }

    @Test
    void aFullCookTakesItsTimeLit() {
        double cook = ForgeRules.heat(0, 0.025, 750, true);
        int ticks = 0;
        while (!ForgeRules.done(cook, 750)) {
            cook = ForgeRules.advance(cook, true, true);
            ticks++;
        }
        assertEquals(750, ticks);
    }

    @Test
    void breathReachesFurtherForOlderDragons() {
        assertEquals(1, ForgeRules.breathReach(1));
        assertEquals(1, ForgeRules.breathReach(3));
        assertEquals(2, ForgeRules.breathReach(4));
        assertEquals(3, ForgeRules.breathReach(5));
    }

    @Test
    void iceAndFiresRanges() {
        assertTrue(ForgeRules.withinBreath(574, 5));
        assertFalse(ForgeRules.withinBreath(575, 5));
        assertTrue(ForgeRules.seesAperture(9.9, 0));
        assertFalse(ForgeRules.seesAperture(14, 1.5));
    }
}
