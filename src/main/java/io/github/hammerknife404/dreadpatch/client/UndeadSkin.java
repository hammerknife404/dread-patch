package io.github.hammerknife404.dreadpatch.client;

import java.util.ArrayDeque;

/**
 * Game-free: rots a dragon's skin into an undead one, from Ice and Fire's body texture and the
 * skeleton texture it draws dead dragons with (same layout: the bones sit under the scales). Scales
 * go soot-and-slate; in rotted patches the bones show through where there are bones, and wing
 * membrane (flesh far from any bone) tears open. Deterministic, so every stage rots the same way.
 * Pixels are packed as {@code NativeImage} keeps them: 0xAABBGGRR.
 */
public final class UndeadSkin {
    /** Scales, from their darkest to their palest. */
    static final int SOOT = abgr(255, 0x0B, 0x0C, 0x10);
    static final int SLATE = abgr(255, 0x4C, 0x55, 0x6B);
    /** Bone, dim to bright. */
    static final int BONE_DIM = abgr(255, 0x8C, 0x96, 0xAB);
    static final int BONE = abgr(255, 0xD8, 0xDE, 0xE9);
    /** Rot at or above which bone shows, flesh far from bone tears, and scales darken. */
    static final double BONES_SHOW = 0.56;
    static final double MEMBRANE_TEARS = 0.66;
    static final double SCALES_DARKEN = 0.48;
    /** Flesh this many pixels or more from any bone counts as membrane. */
    static final int MEMBRANE_DISTANCE = 10;
    private static final long SEED = 0x5D2_160_55AL;

    private UndeadSkin() {
    }

    /** The undead skin, {@code width × height}, from the body and skeleton textures of that size. */
    public static int[] compose(int[] body, int[] bones, int width, int height) {
        int[] distance = distanceToBone(bones, width, height);
        int[] out = new int[body.length];
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int i = y * width + x;
                out[i] = pixel(body[i], bones[i], rot(x, y), distance[i]);
            }
        }
        return out;
    }

    /** One pixel of the skin: what the body shows there, given the bone under it, the rot, and how far the nearest bone is. */
    static int pixel(int body, int bone, double rot, int distance) {
        if (alpha(body) == 0) {
            return 0;
        }
        if (alpha(bone) != 0 && rot >= BONES_SHOW) {
            return withAlpha(lerp(BONE_DIM, BONE, luminance(bone)), alpha(body));
        }
        if (alpha(bone) == 0 && distance >= MEMBRANE_DISTANCE && rot >= MEMBRANE_TEARS) {
            return 0;
        }
        double shade = Math.pow(luminance(body), 2.0);
        if (rot >= SCALES_DARKEN) {
            shade *= 0.7;
        }
        return withAlpha(lerp(SOOT, SLATE, shade), alpha(body));
    }

    /** How rotted the skin is at a pixel, 0 to 1: coarse patches with ragged edges. */
    static double rot(int x, int y) {
        return 0.7 * valueNoise(x, y, 16, SEED) + 0.3 * valueNoise(x, y, 5, SEED + 1);
    }

    /** Steps to the nearest bone pixel (4-way), capped at {@link #MEMBRANE_DISTANCE}. */
    static int[] distanceToBone(int[] bones, int width, int height) {
        int[] distance = new int[bones.length];
        ArrayDeque<Integer> queue = new ArrayDeque<>();
        for (int i = 0; i < bones.length; i++) {
            if (alpha(bones[i]) != 0) {
                queue.add(i);
            } else {
                distance[i] = MEMBRANE_DISTANCE;
            }
        }
        while (!queue.isEmpty()) {
            int i = queue.poll();
            int next = distance[i] + 1;
            if (next >= MEMBRANE_DISTANCE) {
                continue;
            }
            int x = i % width;
            int y = i / width;
            int[][] steps = {{1, 0}, {-1, 0}, {0, 1}, {0, -1}};
            for (int[] step : steps) {
                int nx = x + step[0];
                int ny = y + step[1];
                if (nx >= 0 && ny >= 0 && nx < width && ny < height && distance[ny * width + nx] > next) {
                    distance[ny * width + nx] = next;
                    queue.add(ny * width + nx);
                }
            }
        }
        return distance;
    }

    private static double valueNoise(int x, int y, int cell, long seed) {
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

    static int abgr(int a, int r, int g, int b) {
        return (a << 24) | (b << 16) | (g << 8) | r;
    }

    static int alpha(int c) {
        return c >>> 24;
    }

    static double luminance(int c) {
        int r = c & 0xFF;
        int g = (c >> 8) & 0xFF;
        int b = (c >> 16) & 0xFF;
        return (0.299 * r + 0.587 * g + 0.114 * b) / 255.0;
    }

    private static int withAlpha(int c, int a) {
        return (c & 0x00FFFFFF) | (a << 24);
    }

    private static int lerp(int from, int to, double t) {
        double k = Math.max(0.0, Math.min(1.0, t));
        int r = (int) Math.round((from & 0xFF) + ((to & 0xFF) - (from & 0xFF)) * k);
        int g = (int) Math.round(((from >> 8) & 0xFF) + (((to >> 8) & 0xFF) - ((from >> 8) & 0xFF)) * k);
        int b = (int) Math.round(((from >> 16) & 0xFF) + (((to >> 16) & 0xFF) - ((from >> 16) & 0xFF)) * k);
        return abgr(255, r, g, b);
    }
}
