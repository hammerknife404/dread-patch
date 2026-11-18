package io.github.hammerknife404.dreadpatch.magic;

/**
 * Game-free: Whiteout Dominion's numbers and the space it covers. A storm is a tall cylinder round
 * the ground it was raised on, reaching {@link #DEPTH} below it (down a slope, into a hollow) and
 * {@link #HEIGHT} above (hills, and her dragon overhead).
 */
public final class WhiteoutStorm {
    /** Hers: 32 blocks round, 45 s, every 120 s (the owner's choice). */
    public static final float QUEEN_RADIUS = 32.0F;
    public static final int QUEEN_LIFETIME = 900;
    public static final int QUEEN_COOLDOWN = 2400;
    /** How far below and above its base it reaches. */
    public static final double DEPTH = 16.0;
    public static final double HEIGHT = 48.0;
    /** Ticks between its freezing hits, and how much each takes. */
    public static final int FREEZE_INTERVAL = 60;
    public static final float FREEZE_DAMAGE = 1.0F;
    /** Ticks it takes to build up, and to clear, on screen. */
    public static final int BUILD_TICKS = 60;
    /** The most of the dead a storm of hers raises. */
    public static final int MAX_RAISED = 8;
    /** Ticks its snow takes to cover the ground; it melts as the storm clears ({@link #BUILD_TICKS}). */
    public static final int COVER_TICKS = 200;
    /** A dread beast's size for each block of the dead's width, and the smallest and biggest it rises. */
    public static final float BEAST_SIZE_PER_WIDTH = 1.0F / 1.2F;
    public static final float BEAST_SIZE_MIN = 0.4F;
    public static final float BEAST_SIZE_MAX = 2.5F;

    private WhiteoutStorm() {
    }

    /** A caster's storm: 16 blocks round at level 1 up to hers, 32, at level 5. */
    public static float radius(int spellLevel) {
        return 12.0F + 4.0F * spellLevel;
    }

    /** A caster's storm, in ticks: 25 s at level 1 up to hers, 45 s, at level 5. */
    public static int lifetime(int spellLevel) {
        return (20 + 5 * spellLevel) * 20;
    }

    /** Whether a point, relative to a storm's base, is inside a storm of this radius. */
    public static boolean contains(double dx, double dy, double dz, double radius) {
        return dx * dx + dz * dz <= radius * radius && dy >= -DEPTH && dy <= HEIGHT;
    }

    /** Whether a storm this old (in ticks) freezes everyone in it this tick: every {@link #FREEZE_INTERVAL}, not on its first. */
    public static boolean freezesAt(int age) {
        return age > 0 && age % FREEZE_INTERVAL == 0;
    }

    /**
     * How much of the ground its snow covers, 0 to 1: drifting over its first {@link #COVER_TICKS},
     * melting as it clears (over its last {@link #BUILD_TICKS}, so a storm cut short melts from wherever it got to).
     */
    public static float cover(float age, int lifetime) {
        float drifting = age / COVER_TICKS;
        float melting = (lifetime - age) / BUILD_TICKS;
        return Math.max(0.0F, Math.min(1.0F, Math.min(drifting, melting)));
    }

    /**
     * How settled the snow on one block is, 0 to 1, with the storm's {@link #cover} at {@code cover}:
     * each block takes its share at its own point ({@code patch}, 0 to 1, fixed for the block), so it
     * drifts in and melts away in patches, and all of it lies whole at full cover.
     */
    public static float settled(float cover, float patch) {
        return Math.max(0.0F, Math.min(1.0F, (cover - patch * 0.6F) / 0.4F));
    }

    /**
     * What a dead thing Ice and Fire's own necromancy leaves alone rises as: something standing
     * upright (at least a block tall and half again as tall as it's wide: villagers, illagers,
     * witches, creepers, endermen, blazes, wardens) as a thrall; anything else as a dread beast.
     */
    public static boolean risesAsThrall(float width, float height) {
        return height >= 1.0F && height >= 1.5F * width;
    }

    /** A dread beast's size for the dead's width, as Ice and Fire sizes them, kept between {@link #BEAST_SIZE_MIN} and {@link #BEAST_SIZE_MAX}. */
    public static float beastSize(float width) {
        return Math.max(BEAST_SIZE_MIN, Math.min(BEAST_SIZE_MAX, width * BEAST_SIZE_PER_WIDTH));
    }

    /** How thick it looks, 0 to 1: building over its first {@link #BUILD_TICKS} and clearing over its last. */
    public static float strength(float age, int lifetime) {
        float rising = age / BUILD_TICKS;
        float clearing = (lifetime - age) / BUILD_TICKS;
        return Math.max(0.0F, Math.min(1.0F, Math.min(rising, clearing)));
    }
}
