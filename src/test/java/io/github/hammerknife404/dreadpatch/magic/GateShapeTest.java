package io.github.hammerknife404.dreadpatch.magic;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

class GateShapeTest {
    /** A plane drawn top row first: '#' frame, '.' open, 'S' open where the search starts, anything else not. */
    private static GateShape.Found find(String... rows) {
        int sx = -1;
        int sy = -1;
        for (int r = 0; r < rows.length; r++) {
            int c = rows[r].indexOf('S');
            if (c >= 0) {
                sx = c;
                sy = r;
            }
        }
        int startX = sx;
        int startY = sy;
        return GateShape.find((a, y) -> {
            int r = startY - y;
            int c = startX + a;
            if (r < 0 || r >= rows.length || c < 0 || c >= rows[r].length()) {
                return GateShape.Cell.OTHER;
            }
            return switch (rows[r].charAt(c)) {
                case '#' -> GateShape.Cell.FRAME;
                case '.', 'S' -> GateShape.Cell.OPEN;
                default -> GateShape.Cell.OTHER;
            };
        });
    }

    @Test
    void aFramedOpeningIsAGate() {
        GateShape.Found gate = find(
                "#####",
                "#...#",
                "#...#",
                "#.S.#",
                "#...#",
                "#####");
        assertNotNull(gate);
        assertEquals(3, gate.width());
        assertEquals(4, gate.height());
        assertEquals(-1, gate.left());
        assertEquals(-1, gate.bottom());
    }

    @Test
    void itsCornersNeedntBeThere() {
        assertNotNull(find(
                " ## ",
                "#..#",
                "#S.#",
                "#..#",
                " ## "));
    }

    @Test
    void aMausoleumShrineArchIsOne() {
        // The ice forge shrine's arches: 3 wide, 6 tall, dreadstone pillars, floor and dome.
        GateShape.Found gate = find(
                "#####",
                "#...#",
                "#...#",
                "#...#",
                "#...#",
                "#...#",
                "#S..#",
                "#####");
        assertNotNull(gate);
        assertEquals(3, gate.width());
        assertEquals(6, gate.height());
    }

    @Test
    void aGapInTheFrameIsNot() {
        assertNull(find(
                "#####",
                "#...#",
                "....#",
                "#.S.#",
                "#####"), "a side open");
        assertNull(find(
                "## ##",
                "#...#",
                "#.S.#",
                "#...#",
                "#####"), "the top open");
        assertNull(find(
                "#####",
                "#...#",
                "#.S.#",
                "#...#",
                "##.##"), "the bottom open");
    }

    @Test
    void anythingElseInsideIsNot() {
        assertNull(find(
                "#####",
                "#.x.#",
                "#.S.#",
                "#...#",
                "#####"));
    }

    @Test
    void itMustBeTheRightSize() {
        assertNull(find(
                "###",
                "#.#",
                "#S#",
                "#.#",
                "###"), "one wide");
        assertNull(find(
                "####",
                "#S.#",
                "#..#",
                "####"), "two tall");
        assertNull(find(
                "########",
                "#......#",
                "#..S...#",
                "#......#",
                "########"), "six wide");
        assertNotNull(find(
                "#######",
                "#.....#",
                "#..S..#",
                "#.....#",
                "#######"), "five wide");
        assertNull(find(
                "####",
                "#..#", "#..#", "#..#", "#..#", "#..#", "#..#", "#..#",
                "#S.#",
                "####"), "eight tall");
        assertNotNull(find(
                "####",
                "#..#", "#..#", "#..#", "#..#", "#..#", "#..#",
                "#S.#",
                "####"), "seven tall");
    }

    @Test
    void itStartsInsideOrNotAtAll() {
        assertNull(find(
                "#####",
                "#...#",
                "#...#",
                "##S##"), "not from a frame");
    }
}
