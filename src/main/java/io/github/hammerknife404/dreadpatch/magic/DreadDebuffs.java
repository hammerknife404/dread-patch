package io.github.hammerknife404.dreadpatch.magic;

/**
 * Game-free rules for what a Dread mob's melee hit leaves (0.73.0, the owner's Feature_Dump_11-24), each
 * rolled apart, so both can land:
 * <ul>
 * <li>a dread scuttler: Withering Rime II (as Syndrigossa's bite) at a low {@link #SCUTTLER_WITHERING},
 * and a 2 s Rimefrost that refreshes, never stacks, at the phantom's odds;</li>
 * <li>a dread knight: Withering Rime at a low {@link #KNIGHT_WITHERING};</li>
 * <li>every other Dread mob: the dread phantom's bite ({@link PhantomRules}).</li>
 * </ul>
 * The queen, the Rimefrost dragons, the phantoms and the Rime Eel keep their own.
 */
public final class DreadDebuffs {
    public static final float SCUTTLER_WITHERING = 0.08F;
    public static final float KNIGHT_WITHERING = 0.08F;
    /** Withering Rime II's amplifier. */
    public static final int WITHERING_II = 1;
    /** A scuttler's frost: 2 s. */
    public static final int SCUTTLER_RIMEFROST_TICKS = 40;

    public enum Kind { SCUTTLER, KNIGHT, OTHER }

    /** What lands: a duration of 0 is none. */
    public record Hit(int witheringTicks, int witheringAmplifier, int rimefrostTicks) {
        public static final Hit NONE = new Hit(0, 0, 0);
    }

    private DreadDebuffs() {
    }

    /** From two rolls in {@code [0, 1)}: one for Withering Rime, one for Rimefrost. */
    public static Hit roll(Kind kind, float witheringRoll, float rimefrostRoll) {
        return switch (kind) {
            case SCUTTLER -> new Hit(witheringRoll < SCUTTLER_WITHERING ? PhantomRules.WITHERING_TICKS : 0, WITHERING_II,
                    PhantomRules.rimefrost(rimefrostRoll) ? SCUTTLER_RIMEFROST_TICKS : 0);
            case KNIGHT -> new Hit(witheringRoll < KNIGHT_WITHERING ? PhantomRules.WITHERING_TICKS : 0, 0, 0);
            case OTHER -> new Hit(PhantomRules.witheringRime(witheringRoll) ? PhantomRules.WITHERING_TICKS : 0, 0,
                    PhantomRules.rimefrost(rimefrostRoll) ? PhantomRules.RIMEFROST_TICKS : 0);
        };
    }

    /**
     * The one-source cap on players: whether a new laying of Withering Rime or Rimefrost by {@code source} lands on a
     * player who has it already from {@code current} (null: none known). The same source may renew it; the queen and
     * Syndrigossa ({@code overriding}) always may, so a pack never keeps her siphon from her.
     */
    public static boolean lands(Object current, Object source, boolean overriding) {
        return current == null || source == null || overriding || current.equals(source);
    }

    /**
     * Her siphon over several targets (0.73.0, the owner: "capping, splitting, or uncapping per legal target",
     * left to the config): what one withering tick on a target heals her. {@code nearer} of her targets are
     * nearer her than this one, of {@code feeding} in all; {@code maxTargets} 0 lets every one feed her; with
     * {@code split}, each heals her its share, so all of them heal what one would.
     */
    public static float siphon(float heal, int nearer, int feeding, int maxTargets, boolean split) {
        if (maxTargets > 0 && nearer >= maxTargets) {
            return 0.0F;
        }
        if (!split) {
            return heal;
        }
        int counted = Math.max(1, maxTargets > 0 ? Math.min(feeding, maxTargets) : feeding);
        return heal / counted;
    }
}
