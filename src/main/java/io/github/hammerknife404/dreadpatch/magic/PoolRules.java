package io.github.hammerknife404.dreadpatch.magic;

import java.util.ArrayDeque;
import java.util.HashSet;
import java.util.Set;

/**
 * A pool in a Rime Chasma plate (0.97.0, the owner: "sometimes 1 or two block deep pools of water fill an occassional
 * hexagon feature in a rime chasma - the idea is to show players they spawn here at all and from water mostly while
 * showing water actively freezing"). A plate is the floor (its top at {@link TerrainRules#CHASMA_TOP}) closed in by ridges
 * (higher) or cracks (lower): it's filled only if ridges close it all round, found by a flood fill from a column of its
 * floor that never meets a crack and stays within {@link #REACH}; then its top is water, a second layer under every
 * column with all eight neighbours in it.
 */
public final class PoolRules {
    /** Its columns: the plate's floor, a ridge round it, a crack (which would drain it). */
    public enum Column { FLOOR, RIDGE, CRACK }

    /** How far from its first column a pool may reach (a plate's 18 across, jittered). */
    public static final int REACH = 13;
    /** Pools smaller (a corner pinched off) or larger (two plates run together) than these aren't made. */
    public static final int MIN_CELLS = 30;
    public static final int MAX_CELLS = 360;
    /** One chunk in this many on a chasma's floor tries for a pool (the placed feature's rarity). */
    public static final int CHUNK_ONE_IN = 6;
    /** At its edge (by a ridge) a column starts frozen this often, and in the open this often: freezing from the edges. */
    public static final float EDGE_ICE = 0.35F;
    public static final float OPEN_ICE = 0.04F;
    /** One pool in this many starts with a Dread Fish in it. */
    public static final int FISH_ONE_IN = 3;

    /** Reads a column at an offset from the first. */
    @FunctionalInterface
    public interface Columns {
        Column at(int dx, int dz);
    }

    private PoolRules() {
    }

    /** The pool's columns (packed offsets, {@link #pack}), or null where there's none to fill. */
    public static Set<Long> fill(Columns columns) {
        if (columns.at(0, 0) != Column.FLOOR) {
            return null;
        }
        Set<Long> cells = new HashSet<>();
        ArrayDeque<int[]> open = new ArrayDeque<>();
        cells.add(pack(0, 0));
        open.add(new int[]{0, 0});
        int[][] steps = {{1, 0}, {-1, 0}, {0, 1}, {0, -1}};
        while (!open.isEmpty()) {
            int[] at = open.poll();
            for (int[] step : steps) {
                int x = at[0] + step[0];
                int z = at[1] + step[1];
                if (cells.contains(pack(x, z))) {
                    continue;
                }
                Column column = columns.at(x, z);
                if (column == Column.RIDGE) {
                    continue;
                }
                if (column == Column.CRACK || Math.abs(x) > REACH || Math.abs(z) > REACH || cells.size() >= MAX_CELLS) {
                    return null;
                }
                cells.add(pack(x, z));
                open.add(new int[]{x, z});
            }
        }
        return cells.size() < MIN_CELLS ? null : cells;
    }

    /** Whether a column of the pool is two deep: all eight round it in it too. */
    public static boolean deep(Set<Long> cells, int x, int z) {
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                if (!cells.contains(pack(x + dx, z + dz))) {
                    return false;
                }
            }
        }
        return true;
    }

    /** Whether a column is at its edge: one of its four sides out of it (a ridge). */
    public static boolean edge(Set<Long> cells, int x, int z) {
        return !cells.contains(pack(x + 1, z)) || !cells.contains(pack(x - 1, z)) || !cells.contains(pack(x, z + 1)) || !cells.contains(pack(x, z - 1));
    }

    public static long pack(int x, int z) {
        return ((long) x << 32) | (z & 0xFFFFFFFFL);
    }

    public static int x(long packed) {
        return (int) (packed >> 32);
    }

    public static int z(long packed) {
        return (int) packed;
    }
}
