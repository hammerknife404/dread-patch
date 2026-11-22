package io.github.hammerknife404.dreadpatch.magic;

import org.jetbrains.annotations.Nullable;

/**
 * Game-free: a Dread Gate's shape, found as a Nether portal's is. A rectangle of open space from
 * {@link #MIN_WIDTH}×{@link #MIN_HEIGHT} to {@link #MAX_WIDTH}×{@link #MAX_HEIGHT} inside, with frame
 * along all four sides (its corners needn't be), found from any open cell in it. Coordinates are in
 * the gate's plane, relative to the cell the search starts from: {@code a} along the gate, {@code y} up.
 * The arches of a Mausoleum's ice forge shrine are 3×6 inside, framed in dreadstone: gates already.
 */
public final class GateShape {
    public static final int MIN_WIDTH = 2;
    /** Vanilla's Nether portal's limits (0.55.1, the owner: "follows the sizes like the vanilla nether portal frames"; 5 × 7 before). */
    public static final int MAX_WIDTH = 21;
    public static final int MIN_HEIGHT = 3;
    public static final int MAX_HEIGHT = 21;

    public enum Cell { OPEN, FRAME, OTHER }

    /** What's at a cell of the plane. */
    @FunctionalInterface
    public interface Plane {
        Cell at(int a, int y);
    }

    /** A gate: its lowest, first open cell (relative to where the search began), and its size inside. */
    public record Found(int left, int bottom, int width, int height) {
    }

    private GateShape() {
    }

    /** The gate round the open cell at (0, 0), or null if it isn't inside one. */
    @Nullable
    public static Found find(Plane plane) {
        if (plane.at(0, 0) != Cell.OPEN) {
            return null;
        }
        int bottom = 0;
        while (plane.at(0, bottom - 1) == Cell.OPEN) {
            if (--bottom <= -MAX_HEIGHT) {
                return null;
            }
        }
        int left = 0;
        while (plane.at(left - 1, bottom) == Cell.OPEN) {
            if (--left <= -MAX_WIDTH) {
                return null;
            }
        }
        int width = 0;
        while (plane.at(left + width, bottom) == Cell.OPEN) {
            if (++width > MAX_WIDTH) {
                return null;
            }
        }
        if (width < MIN_WIDTH || plane.at(left - 1, bottom) != Cell.FRAME || plane.at(left + width, bottom) != Cell.FRAME) {
            return null;
        }
        for (int i = 0; i < width; i++) {
            if (plane.at(left + i, bottom - 1) != Cell.FRAME) {
                return null;
            }
        }
        for (int height = 0; height <= MAX_HEIGHT; height++) {
            int y = bottom + height;
            if (row(plane, left, width, y, Cell.FRAME)) {
                return height >= MIN_HEIGHT ? new Found(left, bottom, width, height) : null;
            }
            if (!row(plane, left, width, y, Cell.OPEN) || plane.at(left - 1, y) != Cell.FRAME || plane.at(left + width, y) != Cell.FRAME) {
                return null;
            }
        }
        return null;
    }

    private static boolean row(Plane plane, int left, int width, int y, Cell cell) {
        for (int i = 0; i < width; i++) {
            if (plane.at(left + i, y) != cell) {
                return false;
            }
        }
        return true;
    }
}
