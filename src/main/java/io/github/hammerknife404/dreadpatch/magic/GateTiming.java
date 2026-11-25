package io.github.hammerknife404.dreadpatch.magic;

/**
 * Game-free timing for a Dread Gate's way to the Dreadlands (0.46.0). The owner: "its ritual 10 s; a
 * player stepping in cancels the overworld summoning, and the gate stays open a random 15-30 s, its
 * blocks fading in and out faster and faster over the last ~5 s". Since 0.70.0 (the owner: "Leaving a dread
 * portal open no longer summons the Dread Queen or Syndragossa. It remains just a portal and does not despawn if
 * players go in. It should slowly build a few layers of snow layers around it up to 3-4 blocks out") a keyed gate
 * stays open till its frame breaks, and only the gate home at her fall fades; and an open gate lays snow.
 */
public final class GateTiming {
    /** How long its cold fire and snow take to pour at their fullest (once the ritual, before the queen came through). */
    public static final int RITUAL_TICKS = 200;
    /** How often an open gate lays a layer of snow round itself: ticks (one try). */
    public static final int SNOW_TICKS = 40;
    /** How far out from the foot of its opening it lays snow. */
    public static final double SNOW_REACH = 4.0;
    /** Its last stretch, fading in and out. */
    public static final int FADE_TICKS = 100;
    /** The fade's first blink, and how much shorter each blink is than the last. */
    static final double FIRST_BLINK = 16.0;
    static final double QUICKENING = 0.86;

    private GateTiming() {
    }

    /**
     * The most snow layers an open gate lays {@code distance} blocks from the foot of its opening: 3 beside it, 2 a
     * little out, 1 at 3-4 blocks, none past {@link #SNOW_REACH}.
     */
    public static int snowLayers(double distance) {
        return distance > SNOW_REACH ? 0 : distance <= 1.5 ? 3 : distance <= 2.5 ? 2 : 1;
    }

    /**
     * Whether its panes show with {@code left} ticks of the way left: always, until the fade; then out,
     * in, out, each blink shorter than the last (16 ticks down to 2), and gone at the end.
     */
    public static boolean shown(int left) {
        if (left >= FADE_TICKS) {
            return true;
        }
        if (left <= 0) {
            return false;
        }
        int into = FADE_TICKS - left;
        boolean on = false;
        int at = 0;
        double blink = FIRST_BLINK;
        while (true) {
            int span = Math.max(1, (int) Math.round(blink));
            if (into < at + span) {
                return on;
            }
            at += span;
            on = !on;
            blink *= QUICKENING;
        }
    }
}
