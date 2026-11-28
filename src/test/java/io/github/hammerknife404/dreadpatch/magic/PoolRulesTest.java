package io.github.hammerknife404.dreadpatch.magic;

import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PoolRulesTest {
    /** A plate: floor within {@code r} (a diamond, near enough a hexagon), ridged round, or a crack at one point of its rim. */
    private static PoolRules.Columns plate(int r, boolean crack) {
        return (dx, dz) -> {
            int d = Math.abs(dx) + Math.abs(dz);
            if (d <= r) {
                return PoolRules.Column.FLOOR;
            }
            if (crack && dx == r + 1 && dz == 0) {
                return PoolRules.Column.CRACK;
            }
            return d == r + 1 ? PoolRules.Column.RIDGE : PoolRules.Column.FLOOR;
        };
    }

    @Test
    void aRidgedPlateFills() {
        Set<Long> cells = PoolRules.fill(plate(6, false));
        assertNotNull(cells);
        assertEquals(85, cells.size());
        assertTrue(PoolRules.deep(cells, 0, 0));
        assertFalse(PoolRules.deep(cells, 6, 0));
        assertTrue(PoolRules.edge(cells, 6, 0));
        assertFalse(PoolRules.edge(cells, 0, 0));
    }

    @Test
    void aCrackDrainsIt() {
        assertNull(PoolRules.fill(plate(6, true)));
    }

    @Test
    void tooSmallOrOpenIsNone() {
        assertNull(PoolRules.fill(plate(2, false)));
        // No ridge at all: it runs past its reach.
        assertNull(PoolRules.fill((dx, dz) -> PoolRules.Column.FLOOR));
        // Not on the floor: none.
        assertNull(PoolRules.fill((dx, dz) -> PoolRules.Column.RIDGE));
    }

    @Test
    void packingRoundTrips() {
        for (int[] p : new int[][]{{0, 0}, {-13, 7}, {13, -13}, {-1, -1}}) {
            long packed = PoolRules.pack(p[0], p[1]);
            assertEquals(p[0], PoolRules.x(packed));
            assertEquals(p[1], PoolRules.z(packed));
        }
    }

    @Test
    void aHolesBottomNotACaveOrFlat() {
        // Seen in the dev world: a dip at 69, five sides 72 or more at 12 out.
        int[] walls = {70, 70, 69, 74, 73, 73, 73, 74};
        assertTrue(PoolRules.inHole(69, walls));
        // Flat round it: no hole.
        assertFalse(PoolRules.inHole(70, new int[]{71, 70, 72, 70, 71, 71, 70, 70}));
        // Deep in a cave's mouth: not its pond.
        assertFalse(PoolRules.inHole(40, walls));
    }

    @Test
    void aPondIsRaggedRound() {
        double[] smooth = new double[8];
        assertTrue(PoolRules.inPond(0, 0, 4, smooth));
        assertTrue(PoolRules.inPond(4, 0, 4, smooth));
        assertFalse(PoolRules.inPond(4, 1, 4, smooth));
        double[] ragged = {1, 1, 1, 1, 1, 1, 1, 1};
        assertFalse(PoolRules.inPond(4, 0, 4, ragged));
        assertTrue(PoolRules.inPond(2, 0, 4, ragged));
    }
}
