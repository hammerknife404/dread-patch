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
}
