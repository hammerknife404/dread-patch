package io.github.hammerknife404.dreadpatch.client;

/**
 * Game-free: vanilla's phantom turned a dread phantom (0.45.0), made from vanilla's own textures as
 * they load (nothing of Mojang's ships with us, and a resource pack's phantom carries through).
 * <ul>
 * <li>Its blue hide goes the Dread's soot and slate, keeping a little of the phantom's blue; its pink
 * flesh goes a withered grey; its bones (the spine on its back, its belly) go the Dread's pale bone.</li>
 * <li>Its wings are torn through, more toward the trailing edge, round bat-like finger bones run from
 * the wrist to the edge, so the holes show bone. A hole is cut through both faces of the wing (and its
 * trailing edge), so it can be seen through.</li>
 * <li>Its eyes go her cyan, as the Dread's do.</li>
 * </ul>
 * Laid out for vanilla's 64×64 phantom (any whole multiple of it works; any other size is only
 * recoloured). Pixels as {@code NativeImage} keeps them: 0xAABBGGRR.
 */
public final class DreadPhantomSkin {
    /** Vanilla's phantom texture's size, and so the units {@link #WINGS} are in. */
    static final int SIZE = 64;
    static final int SOOT = abgr(0x14, 0x16, 0x1D);
    static final int SLATE = abgr(0x3E, 0x46, 0x5C);
    static final int FLESH_DARK = abgr(0x2A, 0x2B, 0x35);
    static final int FLESH = abgr(0x6E, 0x72, 0x82);
    static final int BONE_DIM = abgr(0x8C, 0x96, 0xAB);
    static final int BONE = abgr(0xD8, 0xDE, 0xE9);
    /** The Dread's eyes (Ice and Fire's thrall's): cyan, and brighter at the core. */
    static final int EYE = abgr(0x00, 0xCB, 0xFF);
    static final int EYE_BRIGHT = abgr(0x00, 0xFF, 0xFF);
    /** Its eyes as painted on its hide, under the glow. */
    static final int EYE_DIM = abgr(0x00, 0x8C, 0xB8);
    /** How much of the phantom's own colour its hide and flesh keep. */
    static final double KEEP = 0.3;
    /** How torn its wings are: past this much rot a pixel of membrane is a hole. Lower near the body and along a bone. */
    static final double TEAR = 0.42;
    private static final long SEED = 0x9A4_7_0_4D1L;

    /**
     * One wing face pair, in vanilla's layout: the box's texture corner, its span (x) and depth (z),
     * its thickness, and how far along the whole wing it starts (the base, then the tip).
     */
    record Wing(int u, int v, int span, int depth, int thickness, int start) {
        /** Its top face's corner (the box's {@code DOWN} face, drawn upward on an entity). */
        int topU() {
            return this.u + this.depth;
        }

        /** Its underside's corner ({@code UP}), laid out the same way, beside it. */
        int underU() {
            return this.u + this.depth + this.span;
        }

        /** Its trailing edge's strip ({@code SOUTH}), whose columns run against the span. */
        int trailingU() {
            return this.u + this.depth + this.span + this.depth;
        }
    }

    /** Vanilla's {@code PhantomModel}: the wing base (6×2×9 at 23,12) and its tip (13×1×9 at 16,24). */
    static final Wing[] WINGS = {new Wing(23, 12, 6, 9, 2, 0), new Wing(16, 24, 13, 9, 1, 6)};
    /** The whole wing's span, base and tip. */
    static final int SPAN = 19;
    static final int DEPTH = 9;
    /**
     * Its bones, along the whole wing ({@code x} out from the body, {@code z} back from the leading
     * edge): the arm along the leading edge, and four fingers fanning from the wrist (where the tip
     * meets the base) to the trailing edge.
     */
    static final int[][] FINGERS = {{6, 0, 1, 8}, {6, 0, 8, 8}, {7, 0, 13, 8}, {9, 0, 18, 6}};

    private DreadPhantomSkin() {
    }

    /** Its skin, from vanilla's phantom texture ({@code width × height}). */
    public static int[] skin(int[] phantom, int width, int height) {
        int[] out = new int[phantom.length];
        for (int i = 0; i < phantom.length; i++) {
            out[i] = hide(phantom[i]);
        }
        int scale = width / SIZE;
        if (scale < 1 || width != SIZE * scale || height != SIZE * scale) {
            return out;
        }
        boolean[][] bones = bones();
        boolean[][] holes = holes(bones);
        for (Wing wing : WINGS) {
            for (int x = 0; x < wing.span(); x++) {
                for (int z = 0; z < wing.depth(); z++) {
                    int along = wing.start() + x;
                    int row = wing.v() + wing.depth() - 1 - z;
                    for (int faceU : new int[]{wing.topU(), wing.underU()}) {
                        if (bones[along][z]) {
                            // The arm along the leading edge stands out; the fingers are dimmer, older bone.
                            double pale = (z == 0 ? 0.3 : 0.05) + 0.4 * noise(along, z, 3, SEED + 7);
                            int bone = lerp(BONE_DIM, BONE, pale);
                            fill(out, width, scale, faceU + x, row, bone);
                        } else if (holes[along][z]) {
                            fill(out, width, scale, faceU + x, row, 0);
                        }
                    }
                    if (z == wing.depth() - 1 && holes[along][z]) {
                        // Its trailing edge's strip, under a hole at the edge, goes too.
                        for (int t = 0; t < wing.thickness(); t++) {
                            fill(out, width, scale, wing.trailingU() + wing.span() - 1 - x, wing.v() + wing.depth() + t, 0);
                        }
                    }
                }
            }
        }
        return out;
    }

    /** Its glowing eyes, from vanilla's phantom eyes texture: her cyan where the phantom's are green. */
    public static int[] eyes(int[] phantomEyes) {
        int[] out = new int[phantomEyes.length];
        for (int i = 0; i < phantomEyes.length; i++) {
            int c = phantomEyes[i];
            out[i] = alpha(c) == 0 ? c : withAlpha(lerp(EYE, EYE_BRIGHT, (luminance(c) - 0.5) / 0.35), alpha(c));
        }
        return out;
    }

    /** One pixel of hide, flesh or bone, recoloured; eyes and black keep their place. */
    static int hide(int c) {
        int a = alpha(c);
        if (a == 0) {
            return c;
        }
        int r = c & 0xFF;
        int g = c >> 8 & 0xFF;
        int b = c >> 16 & 0xFF;
        double light = luminance(c);
        if (g > r + 60 && g > b + 60) {
            return withAlpha(EYE_DIM, a);
        }
        if (light < 0.08) {
            return withAlpha(lerp(c, SOOT, 0.5), a);
        }
        if (b > r + 20) {
            // Its blue hide: soot to slate, still faintly blue.
            return withAlpha(lerp(lerp(SOOT, SLATE, (light - 0.12) / 0.27), c, KEEP), a);
        }
        if ((r - g) - (g - b) > 8) {
            // Its pink flesh, withered grey.
            return withAlpha(lerp(lerp(FLESH_DARK, FLESH, (light - 0.3) / 0.35), c, KEEP * 0.5), a);
        }
        return withAlpha(lerp(BONE_DIM, BONE, (light - 0.4) / 0.45), a);
    }

    /** Where the wing's bones run, {@code [x][z]} over the whole wing. */
    static boolean[][] bones() {
        boolean[][] bones = new boolean[SPAN][DEPTH];
        for (int x = 0; x < SPAN; x++) {
            bones[x][0] = true;
        }
        for (int[] finger : FINGERS) {
            line(bones, finger[0], finger[1], finger[2], finger[3]);
        }
        return bones;
    }

    /**
     * Where the membrane is torn, {@code [x][z]}: rot by noise, against a bar that drops toward the
     * trailing edge and out along the wing, and rises beside a bone and right by the body.
     */
    static boolean[][] holes(boolean[][] bones) {
        boolean[][] holes = new boolean[SPAN][DEPTH];
        for (int x = 0; x < SPAN; x++) {
            for (int z = 0; z < DEPTH; z++) {
                if (bones[x][z]) {
                    continue;
                }
                double bar = TEAR + 0.25 - 0.3 * z / (DEPTH - 1.0) - 0.1 * x / (SPAN - 1.0);
                if (besideBone(bones, x, z)) {
                    bar += 0.15;
                }
                if (x < 2) {
                    bar += 0.3;
                }
                double rot = 0.65 * noise(x, z, 3, SEED) + 0.35 * noise(x, z, 1, SEED + 1);
                holes[x][z] = rot > bar;
            }
        }
        return holes;
    }

    private static boolean besideBone(boolean[][] bones, int x, int z) {
        int[][] steps = {{1, 0}, {-1, 0}, {0, 1}, {0, -1}};
        for (int[] step : steps) {
            int nx = x + step[0];
            int nz = z + step[1];
            if (nx >= 0 && nz >= 0 && nx < SPAN && nz < DEPTH && bones[nx][nz]) {
                return true;
            }
        }
        return false;
    }

    /** A bone from one point to another, a pixel wide (Bresenham). */
    private static void line(boolean[][] grid, int x0, int z0, int x1, int z1) {
        int dx = Math.abs(x1 - x0);
        int dz = -Math.abs(z1 - z0);
        int sx = x0 < x1 ? 1 : -1;
        int sz = z0 < z1 ? 1 : -1;
        int error = dx + dz;
        int x = x0;
        int z = z0;
        while (true) {
            if (x >= 0 && z >= 0 && x < SPAN && z < DEPTH) {
                grid[x][z] = true;
            }
            if (x == x1 && z == z1) {
                return;
            }
            int twice = 2 * error;
            if (twice >= dz) {
                error += dz;
                x += sx;
            }
            if (twice <= dx) {
                error += dx;
                z += sz;
            }
        }
    }

    private static void fill(int[] pixels, int width, int scale, int u, int v, int color) {
        for (int dy = 0; dy < scale; dy++) {
            for (int dx = 0; dx < scale; dx++) {
                pixels[(v * scale + dy) * width + u * scale + dx] = color;
            }
        }
    }

    private static double noise(int x, int y, int cell, long seed) {
        int cx = Math.floorDiv(x, cell);
        int cy = Math.floorDiv(y, cell);
        double fx = smooth((x - cx * cell) / (double) cell);
        double fy = smooth((y - cy * cell) / (double) cell);
        double top = hash(cx, cy, seed) + (hash(cx + 1, cy, seed) - hash(cx, cy, seed)) * fx;
        double bottom = hash(cx, cy + 1, seed) + (hash(cx + 1, cy + 1, seed) - hash(cx, cy + 1, seed)) * fx;
        return top + (bottom - top) * fy;
    }

    private static double smooth(double t) {
        return t * t * (3.0 - 2.0 * t);
    }

    private static double hash(int x, int y, long seed) {
        long h = seed ^ (x * 0x9E3779B97F4A7C15L) ^ (y * 0xC2B2AE3D27D4EB4FL);
        h = (h ^ (h >>> 30)) * 0xBF58476D1CE4E5B9L;
        h = (h ^ (h >>> 27)) * 0x94D049BB133111EBL;
        h ^= h >>> 31;
        return (h >>> 11) * 0x1.0p-53;
    }

    static int abgr(int r, int g, int b) {
        return 0xFF000000 | b << 16 | g << 8 | r;
    }

    static int alpha(int c) {
        return c >>> 24;
    }

    static double luminance(int c) {
        return (0.299 * (c & 0xFF) + 0.587 * (c >> 8 & 0xFF) + 0.114 * (c >> 16 & 0xFF)) / 255.0;
    }

    private static int withAlpha(int c, int a) {
        return (c & 0x00FFFFFF) | a << 24;
    }

    private static int lerp(int from, int to, double t) {
        double k = Math.max(0.0, Math.min(1.0, t));
        int r = (int) Math.round((from & 0xFF) + ((to & 0xFF) - (from & 0xFF)) * k);
        int g = (int) Math.round((from >> 8 & 0xFF) + ((to >> 8 & 0xFF) - (from >> 8 & 0xFF)) * k);
        int b = (int) Math.round((from >> 16 & 0xFF) + ((to >> 16 & 0xFF) - (from >> 16 & 0xFF)) * k);
        return abgr(r, g, b);
    }
}
