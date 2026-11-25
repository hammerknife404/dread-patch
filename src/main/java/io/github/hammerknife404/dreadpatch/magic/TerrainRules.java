package io.github.hammerknife404.dreadpatch.magic;

/**
 * Game-free: the Dreadlands' terrain's own maths (0.66.0, the owner's dump's three biomes, and "Caves should be much
 * more sheered in the cracked biomes"; the numbers ours).
 */
public final class TerrainRules {
    /** A cell point's jitter at cell {@code cx}, {@code cz}, for {@code axis} 0 (x) or 1 (z): -1 to 1. */
    @FunctionalInterface
    public interface Jitter {
        double at(int cx, int cz, int axis);
    }

    /** What a lava pocket's crust is, by weight: packed ice 3, blue ice 2, basalt 2, dreadstone 2, obsidian 2, crying obsidian 1. */
    public enum Crust {
        PACKED_ICE(3), BLUE_ICE(2), BASALT(2), DREADSTONE(2), OBSIDIAN(2), CRYING_OBSIDIAN(1);

        public final int weight;

        Crust(int weight) {
            this.weight = weight;
        }
    }

    public static final int CRUST_WEIGHT = 12;
    /** A grave patch on the plains: one chunk in this many; a dead tree in it at this chance. */
    public static final int GRAVE_PATCH_ONE_IN = 6;
    public static final float TREE_CHANCE = 0.4F;

    /** The Rime Chasma's floor (0.68.0): its top block's height, as the terrain script's FLOOR_Y - 1. */
    public static final int CHASMA_TOP = 30;
    /** A water pocket sits this far under the floor's top at least, and this much more at most. */
    public static final int POCKET_DEPTH = 5;
    public static final int POCKET_DEPTH_SPREAD = 4;
    /** A crack is a drop of at least this many blocks beside a column. */
    public static final int LIP_DROP = 5;

    private TerrainRules() {
    }

    /**
     * How far (in cells) {@code x}, {@code z} (in cells) is from the nearest edge of the pattern whose cell points sit
     * at each cell's middle, jittered by up to {@code jitter} of a cell each way: half the gap between the distances to
     * the two nearest points (0 on an edge, about 0.5 at a cell's heart).
     */
    public static double edgeDistance(double x, double z, double jitter, Jitter at) {
        int cx = (int) Math.floor(x);
        int cz = (int) Math.floor(z);
        double first = Double.MAX_VALUE;
        double second = Double.MAX_VALUE;
        for (int i = -1; i <= 1; i++) {
            for (int j = -1; j <= 1; j++) {
                double px = cx + i + 0.5 + 0.5 * jitter * clamp(at.at(cx + i, cz + j, 0));
                double pz = cz + j + 0.5 + 0.5 * jitter * clamp(at.at(cx + i, cz + j, 1));
                double d = Math.hypot(px - x, pz - z);
                if (d < first) {
                    second = first;
                    first = d;
                } else if (d < second) {
                    second = d;
                }
            }
        }
        return (second - first) / 2.0;
    }

    /** A hexagonal pattern's row spacing, in cells: its points one cell apart in every direction. */
    static final double HEX_ROW = Math.sqrt(3.0) / 2.0;

    /**
     * As {@link #edgeDistance}, but with the points on a hexagonal lattice (0.69.0, the owner: the plates "not nearly
     * as sharply defined as a hexagon"): rows {@link #HEX_ROW} apart, every other one shifted half a cell, so unjittered
     * cells are regular hexagons a cell across their flats; {@code jitter} moves each point up to that much of a cell
     * each way, so they stay recognisably hexagons with a little jitter. In cells; about 0.5 at a cell's heart.
     */
    public static double hexEdgeDistance(double x, double z, double jitter, Jitter at) {
        int row = (int) Math.floor(z / HEX_ROW);
        double first = Double.MAX_VALUE;
        double second = Double.MAX_VALUE;
        for (int j = row - 2; j <= row + 2; j++) {
            double shift = Math.floorMod(j, 2) * 0.5;
            int col = (int) Math.floor(x - shift);
            for (int i = col - 2; i <= col + 2; i++) {
                double px = i + shift + 0.5 * jitter * clamp(at.at(i, j, 0));
                double pz = j * HEX_ROW + 0.5 * jitter * clamp(at.at(i, j, 1));
                double d = Math.hypot(px - x, pz - z);
                if (d < first) {
                    second = first;
                    first = d;
                } else if (d < second) {
                    second = d;
                }
            }
        }
        return (second - first) / 2.0;
    }

    private static double clamp(double v) {
        return Math.max(-1.0, Math.min(1.0, v));
    }

    /**
     * A heaved slab's rise over the floor at {@code t} blocks back from the crack's lip (0 at the lip) of a slab
     * {@code length} long: its far end rests on the floor, its lip end raised {@code slope} a block back, rounded.
     */
    public static int heave(int t, int length, double slope) {
        return (int) Math.round(Math.max(0, length - 1 - t) * slope);
    }

    /** Whether an offset is inside a water pocket of these radii (an ellipsoid). */
    public static boolean inPocket(int dx, int dy, int dz, double across, double up) {
        return (dx * dx + dz * dz) / (across * across) + dy * dy / (up * up) <= 1.0;
    }

    public static Crust crust(int roll) {
        for (Crust kind : Crust.values()) {
            if (roll < kind.weight) {
                return kind;
            }
            roll -= kind.weight;
        }
        return Crust.PACKED_ICE;
    }
}
