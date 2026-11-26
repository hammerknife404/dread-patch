package io.github.hammerknife404.dreadpatch.magic;

/**
 * Game-free: Ice and Fire's ice dragon art turned Dread (0.42.0). The Rimefrost scale, its blood, dread
 * dragonsteel and their armour and tools are drawn from Ice and Fire's ice originals, recoloured as
 * they load (nothing of Ice and Fire's ships with us): by brightness into her palette, dark to soot,
 * middle to slate, light to bone-ash, and what's brightly, coldly blue in the original (frost, gems,
 * the ice's glint) to her cyan dread fire. Alpha is kept. Pixels are ARGB.
 */
public final class DreadRecolor {
    static final int SOOT = 0x14161D;
    static final int SLATE = 0x3E465C;
    static final int ASH = 0x8C96AB;
    static final int BONE = 0xE4E9F1;
    static final int CYAN = 0x00CBFF;
    static final int CYAN_LIGHT = 0x9CF0FF;

    private DreadRecolor() {
    }

    /** One pixel, ARGB in and out. */
    public static int recolor(int argb) {
        int a = argb >>> 24;
        if (a == 0) {
            return argb;
        }
        int r = argb >> 16 & 0xFF;
        int g = argb >> 8 & 0xFF;
        int b = argb & 0xFF;
        double light = (0.299 * r + 0.587 * g + 0.114 * b) / 255.0;
        double blue = (b - (r + g) / 2.0) / 255.0;
        int rgb;
        if (blue > 0.18 && light > 0.45) {
            // Frost and glint: her dread fire, brighter where the original is.
            rgb = mix(CYAN, CYAN_LIGHT, Math.min(1.0, (light - 0.45) / 0.45));
        } else {
            // Darkened a little overall (the Dread are darker than ice): soot, slate, ash, bone.
            double t = Math.pow(light, 1.25);
            if (t < 0.33) {
                rgb = mix(SOOT, SLATE, t / 0.33);
            } else if (t < 0.7) {
                rgb = mix(SLATE, ASH, (t - 0.33) / 0.37);
            } else {
                rgb = mix(ASH, BONE, (t - 0.7) / 0.3);
            }
        }
        return a << 24 | rgb;
    }

    /**
     * The hoard's metals (0.84.0), recoloured from Ice and Fire's silver pile and netherite dragon armour by brightness:
     * iron's greys, and blacksteel's (Cataclysm's) near-black blue-greys, its brightness stretched (netherite's art is
     * dark) so its edges still catch the light.
     */
    public enum Palette {
        DREAD(0, 0, 0, 0, 1.0),
        IRON(0x3A3A3E, 0x6E6E72, 0xA8A8AC, 0xE6E6E8, 1.0),
        BLACKSTEEL(0x0E0F14, 0x24262F, 0x454956, 0x7E8494, 1.7);

        final int[] stops;
        final double stretch;

        Palette(int a, int b, int c, int d, double stretch) {
            this.stops = new int[]{a, b, c, d};
            this.stretch = stretch;
        }
    }

    /** One pixel by {@code palette}, ARGB in and out ({@link Palette#DREAD} is {@link #recolor(int)}). */
    public static int recolor(int argb, Palette palette) {
        if (palette == Palette.DREAD) {
            return recolor(argb);
        }
        int a = argb >>> 24;
        if (a == 0) {
            return argb;
        }
        double light = (0.299 * (argb >> 16 & 0xFF) + 0.587 * (argb >> 8 & 0xFF) + 0.114 * (argb & 0xFF)) / 255.0;
        double t = Math.min(1.0, light * palette.stretch) * 3.0;
        int i = Math.min(2, (int) t);
        return a << 24 | mix(palette.stops[i], palette.stops[i + 1], t - i);
    }

    /** Recolours a whole image in place. */
    public static void recolor(int[] pixels) {
        for (int i = 0; i < pixels.length; i++) {
            pixels[i] = recolor(pixels[i]);
        }
    }

    static int mix(int from, int to, double t) {
        double k = Math.max(0.0, Math.min(1.0, t));
        int r = (int) Math.round((from >> 16 & 0xFF) + ((to >> 16 & 0xFF) - (from >> 16 & 0xFF)) * k);
        int g = (int) Math.round((from >> 8 & 0xFF) + ((to >> 8 & 0xFF) - (from >> 8 & 0xFF)) * k);
        int b = (int) Math.round((from & 0xFF) + ((to & 0xFF) - (from & 0xFF)) * k);
        return r << 16 | g << 8 | b;
    }
}
