package io.github.hammerknife404.dreadpatch.magic;

/**
 * Game-free timing for a Dread Gate's way to the Dreadlands (0.46.0). The owner: "its ritual 10 s; a
 * player stepping in cancels the overworld summoning, and the gate stays open a random 15-30 s, its
 * blocks fading in and out faster and faster over the last ~5 s".
 */
public final class GateTiming {
    /** The ritual, before the queen comes through. */
    public static final int RITUAL_TICKS = 200;
    /** How long the way stays open once someone's stepped in (or once a gate opens in the Dreadlands). */
    public static final int OPEN_MIN = 300;
    public static final int OPEN_MAX = 600;
    /** Its last stretch, fading in and out. */
    public static final int FADE_TICKS = 100;
    /** The fade's first blink, and how much shorter each blink is than the last. */
    static final double FIRST_BLINK = 16.0;
    static final double QUICKENING = 0.86;

    private GateTiming() {
    }

    /** How long the way stays open, from a roll in {@code [0, 1)}. */
    public static int openTicks(double roll) {
        return OPEN_MIN + (int) (Math.max(0.0, Math.min(0.999999, roll)) * (OPEN_MAX - OPEN_MIN + 1));
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
