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
}
