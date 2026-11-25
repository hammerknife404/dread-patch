package io.github.hammerknife404.dreadpatch.magic;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TerrainRulesTest {
    private static final TerrainRules.Jitter NONE = (cx, cz, axis) -> 0.0;
    private static final TerrainRules.Jitter HASH = (cx, cz, axis) -> Math.sin(cx * 12.9898 + cz * 78.233 + axis * 37.719) * 0.9;

    @Test
    void unjitteredCellsAreASquareGrid() {
        // Points at each cell's middle: an edge is half way between two middles, the cell boundary itself.
        assertEquals(0.5, TerrainRules.edgeDistance(0.5, 0.5, 0.0, NONE), 1e-9, "a cell's heart");
        assertEquals(0.0, TerrainRules.edgeDistance(1.0, 0.5, 0.0, NONE), 1e-9, "on the edge between two cells");
    }

    @Test
    void edgesAreThinLinesAndContinuous() {
        int near = 0;
        int n = 0;
        double last = TerrainRules.edgeDistance(0.0, 3.3, 1.0, HASH);
        for (double x = 0.0; x < 40.0; x += 0.01) {
            double d = TerrainRules.edgeDistance(x, 3.3, 1.0, HASH);
            assertTrue(d >= 0.0 && d <= 1.0);
            assertTrue(Math.abs(d - last) < 0.05, "no jumps: walls without seams");
            last = d;
            near += d < 0.05 ? 1 : 0;
            n++;
        }
        // Edges are lines: a narrow width of them is a small share of the land (half the gap between the two nearest
        // points runs a little under the true distance, so a little more than the width itself).
        assertTrue(near > 0 && near < n / 3, near + " of " + n);
    }

    @Test
    void crustByWeight() {
        int total = 0;
        for (TerrainRules.Crust kind : TerrainRules.Crust.values()) {
            total += kind.weight;
        }
        assertEquals(TerrainRules.CRUST_WEIGHT, total);
        assertEquals(TerrainRules.Crust.CRYING_OBSIDIAN, TerrainRules.crust(TerrainRules.CRUST_WEIGHT - 1), "crying obsidian rarest");
    }

    @Test
    void heavedSlabRisesToItsLip() {
        assertEquals(0, TerrainRules.heave(5, 6, 0.5), "its far end on the floor");
        assertEquals(3, TerrainRules.heave(0, 6, 0.5), "its lip end raised");
        int last = Integer.MAX_VALUE;
        for (int t = 0; t < 6; t++) {
            int rise = TerrainRules.heave(t, 6, 0.9);
            assertTrue(rise <= last && last - rise <= 1 || last == Integer.MAX_VALUE, "a step at most a block, so it holds together");
            last = rise;
        }
    }

    @Test
    void pocketIsAnEllipsoid() {
        assertTrue(TerrainRules.inPocket(0, 0, 0, 3.0, 1.5));
        assertTrue(TerrainRules.inPocket(3, 0, 0, 3.0, 1.5));
        assertTrue(!TerrainRules.inPocket(0, 2, 0, 3.0, 1.5), "flat: not as tall as it's wide");
        assertTrue(!TerrainRules.inPocket(3, 1, 0, 3.0, 1.5));
    }

    @Test
    void pocketSitsInTheIceSheet() {
        // The sheet is 16 deep; the deepest pocket's bottom (its heart, plus at most 3 below) stays inside it.
        assertTrue(TerrainRules.POCKET_DEPTH + TerrainRules.POCKET_DEPTH_SPREAD + 1 + 3 < 16);
        assertTrue(TerrainRules.POCKET_DEPTH >= 4, "under the 4 blocks that risen spawning counts as the top");
    }
}
