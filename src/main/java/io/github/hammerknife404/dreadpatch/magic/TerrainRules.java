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

    private static double clamp(double v) {
        return Math.max(-1.0, Math.min(1.0, v));
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
