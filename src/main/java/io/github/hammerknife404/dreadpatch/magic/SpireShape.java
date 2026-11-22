package io.github.hammerknife404.dreadpatch.magic;

import java.util.Random;

/**
 * Game-free: one of the Dreadlands' spires (0.46.0), in blocks round its base centre ({@code dx},
 * {@code dz}) and up from the ground ({@code y}). The owner: "rare tall spires (dreadstone, the ice
 * types, snow, stone)", with the Queen and Syndrigossa's encounter at the summit. Ours:
 * <ul>
 * <li>A rough tower {@link #height} tall, tapering from {@link #baseRadius} to a flat summit of
 * {@link #topRadius} (the encounter's floor), on a foundation sunk {@link #FOUNDATION} into the ground.</li>
 * <li>A ledge winds up the outside, a turn every {@link #LEDGE_PITCH} blocks, cut a block into the
 * spire with {@link #HEADROOM} clear above it, from the ground to the summit's rim.</li>
 * <li>A crown of ice spikes round the summit's rim, open where the ledge arrives.</li>
 * <li>Stone and dreadstone at the foot, dreadstone and its bricks through the middle banded with
 * packed and blue ice, packed ice and snow toward the top. No plain ice (0.47.0): torches by it, the
 * encounter's or a player's, would melt it into water running down the spire.</li>
 * <li>Half of them ({@link #ENCOUNTER_CHANCE}; the owner: "each with a chance") hold the Queen and
 * Syndrigossa's encounter (0.47.0), marked by a dreadstone dais at the summit's middle and dread
 * torches round it, burning cyan.</li>
 * </ul>
 * Deterministic from its seed, so every chunk of it agrees.
 */
public final class SpireShape {
    public enum Material {
        STONE, DREAD_STONE, DREAD_BRICKS, DREAD_BRICKS_CRACKED, DREAD_BRICKS_CHISELED, DREAD_TILE, DREAD_TORCH, PACKED_ICE, BLUE_ICE, SNOW
    }

    /** How deep its foundation goes under the ground it stands on. */
    public static final int FOUNDATION = 12;
    /** The ledge: how far out from the spire's face it reaches, how far it climbs a turn, and the room above it. */
    public static final double LEDGE_WIDTH = 2.6;
    public static final int LEDGE_PITCH = 22;
    public static final int HEADROOM = 3;
    public static final int MIN_HEIGHT = 64;
    public static final int MAX_HEIGHT = 100;
    /** How far its roughness may push its face out. */
    static final double ROUGHNESS = 2.0;
    static final int SPIKES = 8;
    static final int MAX_SPIKE = 6;
    /** How many spires hold the encounter. */
    public static final double ENCOUNTER_CHANCE = 0.5;
    /** Its dread torches, this far out from the middle along the diagonals. */
    static final int TORCH_OFFSET = 3;

    public final long seed;
    public final int height;
    public final double baseRadius;
    public final double topRadius;
    /** Where the ledge starts, round from +x. */
    final double ledgeStart;
    /** Whether the Queen and Syndrigossa come to its summit. */
    public final boolean encounter;
    private final int[] spikeX;
    private final int[] spikeZ;
    private final int[] spikeLength;

    public SpireShape(long seed, int height, double baseRadius, double topRadius) {
        this.seed = seed;
        this.height = height;
        this.baseRadius = baseRadius;
        this.topRadius = topRadius;
        Random random = new Random(seed ^ 0x5B1BE5L);
        this.ledgeStart = random.nextDouble() * 2.0 * Math.PI;
        this.encounter = hash(0, 0, 0, seed ^ 0xE4C0L) < ENCOUNTER_CHANCE;
        // The crown: spikes round the rim, none where the ledge comes up onto it.
        double arrival = this.ledgeAngle(height - 1);
        this.spikeX = new int[SPIKES];
        this.spikeZ = new int[SPIKES];
        this.spikeLength = new int[SPIKES];
        for (int i = 0; i < SPIKES; i++) {
            double angle = 2.0 * Math.PI * (i + 0.5 * random.nextDouble()) / SPIKES;
            double rim = topRadius - 0.5;
            boolean onLedge = Math.abs(wrap(angle - arrival)) < 0.7;
            this.spikeX[i] = (int) Math.round(Math.cos(angle) * rim);
            this.spikeZ[i] = (int) Math.round(Math.sin(angle) * rim);
            this.spikeLength[i] = onLedge ? 0 : 3 + random.nextInt(MAX_SPIKE - 2);
        }
    }

    /** A spire from a seed: 64–100 tall, 11–14 wide at the base, a summit 6–8 across its radius. */
    public static SpireShape roll(long seed) {
        Random random = new Random(seed);
        int height = MIN_HEIGHT + random.nextInt(MAX_HEIGHT - MIN_HEIGHT + 1);
        double base = 11.0 + random.nextDouble() * 3.0;
        double top = 6.0 + random.nextDouble() * 2.0;
        return new SpireShape(seed, height, base, top);
    }

    /** How far from its centre anything of it reaches. */
    public int reach() {
        return (int) Math.ceil(this.baseRadius + 1.0 + ROUGHNESS + LEDGE_WIDTH) + 1;
    }

    /** Its radius at height {@code y}: concave, wide at the foot, narrowing fast and then slowly to the summit. */
    public double radius(int y) {
        if (y < 0) {
            return this.baseRadius + 1.0;
        }
        double t = Math.min(1.0, y / (double) this.height);
        return this.topRadius + (this.baseRadius - this.topRadius) * Math.pow(1.0 - t, 1.6);
    }

    /** Whether its body stands at this block (its ledge's headroom cut out). */
    public boolean solid(int dx, int y, int dz) {
        if (y < -FOUNDATION || y >= this.height || this.headroom(dx, y, dz)) {
            return false;
        }
        return Math.hypot(dx, dz) <= this.radius(y) + this.rough(dx, y, dz);
    }

    /** Whether its ledge runs here: two blocks thick, its top at the ledge's height for this side of the spire. */
    public boolean ledge(int dx, int y, int dz) {
        if (y < -1 || y >= this.height) {
            return false;
        }
        int top = this.ledgeTop(dx, dz, y, -1, 0);
        return top != Integer.MIN_VALUE && this.onLedgeBand(dx, dz, top);
    }

    /** The open air over its ledge. */
    public boolean headroom(int dx, int y, int dz) {
        if (y < 1 || y > this.height + HEADROOM) {
            return false;
        }
        int top = this.ledgeTop(dx, dz, y, 1, HEADROOM);
        return top != Integer.MIN_VALUE && this.onLedgeBand(dx, dz, top);
    }

    /** Whether one of its crown's ice spikes stands here, over the summit. */
    public boolean spike(int dx, int y, int dz) {
        int above = y - this.height;
        if (above < 0 || above >= MAX_SPIKE) {
            return false;
        }
        for (int i = 0; i < SPIKES; i++) {
            int length = this.spikeLength[i];
            if (above >= length) {
                continue;
            }
            int off = Math.abs(dx - this.spikeX[i]) + Math.abs(dz - this.spikeZ[i]);
            // A cross at its foot, a single column above.
            if (off == 0 || off == 1 && above < length / 2) {
                return true;
            }
        }
        return false;
    }

    /**
     * An encounter spire's summit dressing, standing on the summit, or null: a 3×3 dais of dreadstone
     * tiles round chiseled bricks, and a dread torch on each diagonal.
     */
    public Material summit(int dx, int y, int dz) {
        if (!this.encounter || y != this.height) {
            return null;
        }
        if (Math.abs(dx) <= 1 && Math.abs(dz) <= 1) {
            return dx == 0 && dz == 0 ? Material.DREAD_BRICKS_CHISELED : Material.DREAD_TILE;
        }
        return Math.abs(dx) == TORCH_OFFSET && Math.abs(dz) == TORCH_OFFSET ? Material.DREAD_TORCH : null;
    }

    /** Whether a spot {@code dx}, {@code dz} from its middle and {@code above} its summit's top block is on its summit. */
    public boolean onSummit(double dx, double above, double dz) {
        return above >= -1.0 && above <= 8.0 && Math.hypot(dx, dz) <= this.topRadius + 2.0;
    }

    /** What its body is made of here. */
    public Material material(int dx, int y, int dz) {
        double grain = hash(dx, y, dz, this.seed + 3);
        double t = y / (double) this.height;
        if (t > 0.82) {
            return grain < 0.6 ? Material.PACKED_ICE : grain < 0.8 ? Material.SNOW : grain < 0.9 ? Material.BLUE_ICE : Material.DREAD_STONE;
        }
        // Ice strata through the stone, wavering a little round the spire.
        double band = noise(dx * 0.08, y * 0.35, dz * 0.08, this.seed + 4);
        if (y > 4 && band > 0.66) {
            return grain < 0.75 ? Material.PACKED_ICE : Material.BLUE_ICE;
        }
        if (t < 0.15) {
            return grain < 0.45 ? Material.STONE : grain < 0.85 ? Material.DREAD_STONE : Material.DREAD_BRICKS_CRACKED;
        }
        return grain < 0.5 ? Material.DREAD_STONE : grain < 0.78 ? Material.DREAD_BRICKS : grain < 0.9 ? Material.DREAD_BRICKS_CRACKED : Material.STONE;
    }

    /** What a crown spike is made of here. */
    public Material spikeMaterial(int dx, int y, int dz) {
        return hash(dx, y, dz, this.seed + 5) < 0.7 ? Material.PACKED_ICE : Material.BLUE_ICE;
    }

    /** The angle of the ledge's ground plan at height {@code y}. */
    double ledgeAngle(int y) {
        return this.ledgeStart + 2.0 * Math.PI * Math.floorMod(y, LEDGE_PITCH) / LEDGE_PITCH;
    }

    /**
     * The ledge's top on this column's side of the spire with {@code y} between {@code low} and
     * {@code high} above it, or {@link Integer#MIN_VALUE}. On a given side it's at the same height
     * every turn, {@link #LEDGE_PITCH} apart (more than any span asked for, so there's at most one).
     */
    int ledgeTop(int dx, int dz, int y, int low, int high) {
        double turn = wrapPositive(Math.atan2(dz, dx) - this.ledgeStart) / (2.0 * Math.PI);
        int first = Math.min(LEDGE_PITCH - 1, (int) Math.floor(turn * LEDGE_PITCH));
        int k = Math.max(0, -Math.floorDiv(-(y - high - first), LEDGE_PITCH));
        int top = first + k * LEDGE_PITCH;
        int above = y - top;
        return above >= low && above <= high && top <= this.height - 1 ? top : Integer.MIN_VALUE;
    }

    /** Whether a column is in the band the ledge runs along at that height: a block into the face, out to its width. */
    private boolean onLedgeBand(int dx, int dz, int top) {
        double distance = Math.hypot(dx, dz);
        double face = this.radius(top);
        return distance > face - 1.0 && distance <= face + LEDGE_WIDTH;
    }

    /** How far its face is pushed in or out here: rough at the foot, smoother up top. */
    double rough(int dx, int y, int dz) {
        double t = Math.max(0.0, Math.min(1.0, y / (double) this.height));
        double amplitude = 0.5 + (ROUGHNESS - 0.5) * (1.0 - t);
        return amplitude * (noise(dx / 4.5, y / 6.0, dz / 4.5, this.seed + 1) * 2.0 - 1.0);
    }

    private static double wrap(double angle) {
        double a = wrapPositive(angle);
        return a > Math.PI ? a - 2.0 * Math.PI : a;
    }

    private static double wrapPositive(double angle) {
        double a = angle % (2.0 * Math.PI);
        return a < 0 ? a + 2.0 * Math.PI : a;
    }

    /** Smooth 3D value noise in [0, 1). */
    static double noise(double x, double y, double z, long seed) {
        int x0 = (int) Math.floor(x);
        int y0 = (int) Math.floor(y);
        int z0 = (int) Math.floor(z);
        double fx = smooth(x - x0);
        double fy = smooth(y - y0);
        double fz = smooth(z - z0);
        double c00 = lerp(hash(x0, y0, z0, seed), hash(x0 + 1, y0, z0, seed), fx);
        double c10 = lerp(hash(x0, y0 + 1, z0, seed), hash(x0 + 1, y0 + 1, z0, seed), fx);
        double c01 = lerp(hash(x0, y0, z0 + 1, seed), hash(x0 + 1, y0, z0 + 1, seed), fx);
        double c11 = lerp(hash(x0, y0 + 1, z0 + 1, seed), hash(x0 + 1, y0 + 1, z0 + 1, seed), fx);
        return lerp(lerp(c00, c10, fy), lerp(c01, c11, fy), fz);
    }

    private static double smooth(double t) {
        return t * t * (3.0 - 2.0 * t);
    }

    private static double lerp(double a, double b, double t) {
        return a + (b - a) * t;
    }

    static double hash(int x, int y, int z, long seed) {
        long h = seed ^ (x * 0x9E3779B97F4A7C15L) ^ (y * 0xC2B2AE3D27D4EB4FL) ^ (z * 0x165667B19E3779F9L);
        h = (h ^ (h >>> 30)) * 0xBF58476D1CE4E5B9L;
        h = (h ^ (h >>> 27)) * 0x94D049BB133111EBL;
        h ^= h >>> 31;
        return (h >>> 11) * 0x1.0p-53;
    }
}
