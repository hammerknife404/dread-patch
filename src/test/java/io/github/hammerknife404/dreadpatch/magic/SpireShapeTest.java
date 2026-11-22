package io.github.hammerknife404.dreadpatch.magic;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SpireShapeTest {
    @Test
    void itsSizesStayInTheirBounds() {
        for (long seed = 0; seed < 200; seed++) {
            SpireShape spire = SpireShape.roll(seed);
            assertTrue(spire.height >= SpireShape.MIN_HEIGHT && spire.height <= SpireShape.MAX_HEIGHT);
            assertTrue(spire.baseRadius > spire.topRadius);
            assertEquals(spire.baseRadius, spire.radius(0), 1e-9);
            assertEquals(spire.summitRadius, spire.radius(spire.height), 1e-9);
            assertTrue(spire.summitRadius >= spire.topRadius + 3.0 && spire.summitRadius <= spire.topRadius + 4.0, "flared 3-4 blocks");
            for (int y = 1; y < spire.height - SpireShape.FLARE_HEIGHT; y++) {
                assertTrue(spire.radius(y) <= spire.radius(y - 1), "it narrows upward, till its top flares");
            }
            for (int y = spire.height - SpireShape.FLARE_HEIGHT + 1; y <= spire.height; y++) {
                assertTrue(spire.radius(y) >= spire.radius(y - 1) - 0.05, "then widens to its summit");
            }
        }
    }

    @Test
    void nothingOfItReachesPastItsReach() {
        SpireShape spire = SpireShape.roll(7);
        int reach = spire.reach();
        for (int y = -SpireShape.FOUNDATION; y < spire.height + SpireShape.MAX_SPIKE; y++) {
            for (int dx = -reach - 3; dx <= reach + 3; dx++) {
                for (int dz = -reach - 3; dz <= reach + 3; dz++) {
                    if (Math.abs(dx) > reach || Math.abs(dz) > reach) {
                        assertFalse(spire.solid(dx, y, dz) || spire.ledge(dx, y, dz) || spire.headroom(dx, y, dz) || spire.spike(dx, y, dz),
                                "past its reach at " + dx + "," + y + "," + dz);
                    }
                }
            }
        }
    }

    @Test
    void itsSummitIsSolidAndFlat() {
        SpireShape spire = SpireShape.roll(11);
        int top = spire.height - 1;
        int solid = 0;
        for (int dx = -3; dx <= 3; dx++) {
            for (int dz = -3; dz <= 3; dz++) {
                if (spire.solid(dx, top, dz) || spire.ledge(dx, top, dz)) {
                    solid++;
                }
                assertFalse(spire.solid(dx, top + 1, dz), "nothing of its body over the summit");
            }
        }
        assertEquals(49, solid, "the middle of its summit is whole");
    }

    @Test
    void itsLedgeClimbsAllTheWayRoundAndUp() {
        SpireShape spire = SpireShape.roll(3);
        // Walking round the spire, the ledge's top rises a block at a time: no step higher than one.
        int last = Integer.MIN_VALUE;
        int climbed = 0;
        for (int step = 0; step < 2000; step++) {
            double angle = spire.ledgeStart + step * 2.0 * Math.PI / 400.0;
            int turn = step / 400;
            int y = Math.min(spire.height - 1, (int) Math.floor((step % 400) / 400.0 * SpireShape.LEDGE_PITCH) + turn * SpireShape.LEDGE_PITCH);
            double r = spire.radius(y) + 1.0;
            int dx = (int) Math.round(Math.cos(angle) * r);
            int dz = (int) Math.round(Math.sin(angle) * r);
            int top = topOfLedge(spire, dx, dz, y);
            if (top == Integer.MIN_VALUE) {
                continue;
            }
            if (last != Integer.MIN_VALUE) {
                assertTrue(top - last <= 1, "a step of " + (top - last) + " at " + step);
            }
            climbed = Math.max(climbed, top);
            last = top;
            for (int h = 1; h <= SpireShape.HEADROOM; h++) {
                assertFalse(spire.solid(dx, top + h, dz), "room over the ledge at " + dx + "," + (top + h) + "," + dz);
            }
        }
        assertTrue(climbed >= Math.min(spire.height - 1, 4 * SpireShape.LEDGE_PITCH), "it climbed to " + climbed + " of " + spire.height);
    }

    private static int topOfLedge(SpireShape spire, int dx, int dz, int near) {
        for (int y = near + 2; y >= near - 2; y--) {
            if (spire.ledge(dx, y, dz) && !spire.ledge(dx, y + 1, dz)) {
                return y;
            }
        }
        return Integer.MIN_VALUE;
    }

    @Test
    void itsCrownLeavesTheLedgeOpen() {
        for (long seed = 0; seed < 50; seed++) {
            SpireShape spire = SpireShape.roll(seed);
            double arrival = spire.ledgeAngle(spire.height - 1);
            int dx = (int) Math.round(Math.cos(arrival) * (spire.summitRadius - 0.5));
            int dz = (int) Math.round(Math.sin(arrival) * (spire.summitRadius - 0.5));
            assertFalse(spire.spike(dx, spire.height, dz), "a spike where the ledge comes up, seed " + seed);
        }
    }

    @Test
    void halfHoldTheEncounterMarkedAtTheSummit() {
        int encounters = 0;
        for (long seed = 0; seed < 2000; seed++) {
            SpireShape spire = SpireShape.roll(seed);
            boolean dressed = spire.summit(0, spire.height, 0) != null;
            assertEquals(spire.encounter, dressed, "a dais exactly where the encounter is, seed " + seed);
            if (spire.encounter) {
                encounters++;
                // Its torches stand on the summit, clear of the ledge.
                for (int dx : new int[]{-3, 3}) {
                    for (int dz : new int[]{-3, 3}) {
                        assertEquals(SpireShape.Material.DREAD_TORCH, spire.summit(dx, spire.height, dz));
                        assertTrue(spire.solid(dx, spire.height - 1, dz), "a torch's footing, seed " + seed);
                    }
                }
            }
        }
        assertEquals(0.5, encounters / 2000.0, 0.04);
    }

    @Test
    void itsSummitIsWhereItsTopIs() {
        SpireShape spire = SpireShape.roll(5);
        assertTrue(spire.onSummit(0.0, 0.0, 0.0));
        assertTrue(spire.onSummit(spire.summitRadius, 1.0, 0.0), "at the rim");
        assertFalse(spire.onSummit(0.0, -3.0, 0.0), "below it, on the ledge's last turn");
        assertFalse(spire.onSummit(spire.summitRadius + 3.0, 0.0, 0.0), "off it, in the air beside it");
        assertFalse(spire.onSummit(0.0, 12.0, 0.0), "high over it");
    }

    @Test
    void noPlainIceToMeltUnderATorch() {
        SpireShape spire = SpireShape.roll(9);
        for (int y = 0; y < spire.height; y++) {
            for (int dx = -3; dx <= 3; dx++) {
                assertTrue(spire.material(dx, y, 0) != null);
            }
        }
    }

    @Test
    void halfKeepAChestAndAThirdOfThoseALich() {
        int chests = 0;
        int liches = 0;
        for (long seed = 0; seed < 3000; seed++) {
            SpireShape spire = SpireShape.roll(seed);
            if (spire.chest) {
                chests++;
                int[] at = spire.chestAt();
                assertTrue(spire.solid(at[0], at[1] - 1, at[2]), "a chest stands on the summit, seed " + seed);
                assertTrue(spire.summit(at[0], at[1], at[2]) == null && !spire.spike(at[0], at[1], at[2]), "clear of the dais and the crown");
            }
            liches += spire.lich ? 1 : 0;
            assertTrue(!spire.lich || spire.chest, "a lich only by a chest");
        }
        assertEquals(0.5, chests / 3000.0, 0.03);
        assertEquals(1.0 / 6.0, liches / 3000.0, 0.03);
    }
}
