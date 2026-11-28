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
    /**
     * Pools at the bottom of the Sastrugi Wastes' holes (0.98.6, the owner: "The same kind of freezing shallow rime chasma
     * pools can also spawn at the bottom of sastrugi holes. These can also spawn dread fish"). The holes are gentle dips
     * (2-4 blocks over 10-20, the dev world's), too open for a ridge to close a pool in, so one is dug into the bottom
     * instead: downhill from its spot ({@link #HOLE_WALK}), where the ground is a hole's bottom ({@link #inHole}: of eight spots {@link #HOLE_RIM} out, at least {@link #HOLE_SIDES} are
     * {@link #HOLE_DEPTH} or more higher; and between {@link #HOLE_LOW} and {@link #HOLE_HIGH}, so not a cave's mouth), a
     * ragged round pond {@link #POND_MIN}-{@link #POND_MAX} in radius ({@link #inPond}) one block into it, a second under
     * every column with all eight round it in it, as a plate's pool. Every chunk of the wastes tries ({@link #HOLE_CHUNK_ONE_IN}): most spots aren't in a hole, so about one chunk in 200 gets one (5 of 1,024 tried in the dev world).
     */
    public static final int HOLE_CHUNK_ONE_IN = 1;
    public static final int HOLE_MIN_CELLS = 12;
    public static final int HOLE_DEPTH = 2;
    public static final int HOLE_RIM = 12;
    public static final int HOLE_SIDES = 5;
    /** How far (blocks) it walks downhill from its spot to a hole's bottom first. */
    public static final int HOLE_WALK = 8;
    /** Ground in its pond up to this much over the water is dug away (a cut bank), higher stays. */
    public static final int POND_BANK = 2;
    public static final int HOLE_LOW = 55;
    public static final int HOLE_HIGH = 80;
    public static final int POND_MIN = 3;
    public static final int POND_MAX = 6;
    /** How much its edge may come in from its radius (in each of 8 sectors). */
    public static final double POND_RAGGED = 1.5;

    /** Whether ground at {@code bottom}, with the ground at eight spots round it {@code rim}, is a hole's bottom. */
    public static boolean inHole(int bottom, int[] rim) {
        if (bottom < HOLE_LOW || bottom > HOLE_HIGH) {
            return false;
        }
        int higher = 0;
        for (int top : rim) {
            if (top >= bottom + HOLE_DEPTH) {
                higher++;
            }
        }
        return higher >= HOLE_SIDES;
    }

    /** Whether {@code (dx, dz)} is in a pond of {@code radius}, its edge in each of 8 sectors in by {@code ragged[sector]} (0-1 of {@link #POND_RAGGED}). */
    public static boolean inPond(int dx, int dz, int radius, double[] ragged) {
        double distance = Math.sqrt(dx * dx + dz * dz);
        int sector = (int) Math.floor((Math.atan2(dz, dx) + Math.PI) / (Math.PI / 4.0)) & 7;
        return distance <= radius - ragged[sector] * POND_RAGGED;
    }

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
        return fill(columns, MIN_CELLS);
    }

    /** As {@link #fill(Columns)}, with its least size {@code minCells}. */
    public static Set<Long> fill(Columns columns, int minCells) {
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
        return cells.size() < minCells ? null : cells;
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
