package io.github.hammerknife404.dreadpatch.magic;

/**
 * Game-free rules for webs (0.75.0, the owner's Feature_Dump_11-24): dread armour "partially reduce web
 * effects rather than granting full immunity so webs preserve tactical counterplay", and the scuttlers'
 * web shots "temporary/self-cleaning ... tuned to avoid frustrating movement stunlock".
 */
public final class WebRules {
    /** Each piece of dread armour (dragonscale, dragonsteel, her regalia) takes this much of a web's hold off. */
    public static final double EASE_PER_PIECE = 0.125;
    /** A scuttler's web holds less than a cobweb (0.25 across, 0.05 up and down). */
    public static final double DREAD_WEB_ACROSS = 0.4;
    public static final double DREAD_WEB_UP = 0.15;
    /** It lasts 3 s, then melts away. */
    public static final int LIFETIME_TICKS = 60;
    /** A scuttler spits one every 10-15 s, at what it hunts 4-12 blocks off, never at one already held. */
    public static final int COOLDOWN = 200;
    public static final int COOLDOWN_SPREAD = 101;
    public static final double MIN_RANGE = 4.0;
    public static final double MAX_RANGE = 12.0;

    private WebRules() {
    }

    /** How much of a web's hold {@code pieces} of dread armour take off: half for a full set. */
    public static double ease(int pieces) {
        return Math.max(0, Math.min(4, pieces)) * EASE_PER_PIECE;
    }

    /** A web's speed multiplier (one axis), eased by {@code ease} toward no hold (1). */
    public static double eased(double multiplier, double ease) {
        return multiplier + (1.0 - multiplier) * ease;
    }
}
