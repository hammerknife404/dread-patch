package io.github.hammerknife404.dreadpatch.magic;

/**
 * Game-free rules for what a Dread mob's melee hit leaves (0.73.0, the owner's Feature_Dump_11-24), each
 * rolled apart, so both can land:
 * <ul>
 * <li>a dread scuttler: Withering Rime II (as Syndrigossa's bite) at a low {@link #SCUTTLER_WITHERING},
 * and a 2 s Rimefrost that refreshes, never stacks, at the phantom's odds;</li>
 * <li>a dread knight: Withering Rime at a low {@link #KNIGHT_WITHERING}, and (0.73.1) 3 s of Rimefrost at
 * {@link #KNIGHT_RIMEFROST};</li>
 * <li>every other Dread mob: each at {@link #OTHER_CHANCE} (0.73.1, the owner: "5%"; 0.73.0 had the phantom's
 * odds), for the phantom's times;</li>
 * <li>an unarmed dread thrall (0.85.0): each at {@link ThrallArms#CLAW_EFFECT_CHANCE}, for the phantom's times;</li>
 * </ul>
 * The queen, the Rimefrost dragons, the phantoms and the Dread Fish keep their own, but (0.85.0, the owner: "Ensure all
 * Dread and Rime-type mobs have at least a 5% chance to apply Rimefrost on hit") a melee hit of the queen's, a Rimefrost
 * dragon's or a Dread Fish's, which lay none of their own, rolls {@link #RIMEFROST_FLOOR} for it ({@link #floor}).
 */
public final class DreadDebuffs {
    public static final float SCUTTLER_WITHERING = 0.08F;
    public static final float KNIGHT_WITHERING = 0.08F;
    public static final float KNIGHT_RIMEFROST = 0.13F;
    public static final int KNIGHT_RIMEFROST_TICKS = 60;
    public static final float OTHER_CHANCE = 0.05F;
    /** Withering Rime II's amplifier. */
    public static final int WITHERING_II = 1;
    /** A scuttler's frost: 2 s. */
    public static final int SCUTTLER_RIMEFROST_TICKS = 40;

    /** The least chance of Rimefrost any of the Dread's melee hits carries. */
    public static final float RIMEFROST_FLOOR = 0.05F;

    public enum Kind { SCUTTLER, KNIGHT, UNARMED_THRALL, OTHER }

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
            case KNIGHT -> new Hit(witheringRoll < KNIGHT_WITHERING ? PhantomRules.WITHERING_TICKS : 0, 0,
                    rimefrostRoll < KNIGHT_RIMEFROST ? KNIGHT_RIMEFROST_TICKS : 0);
            case UNARMED_THRALL -> new Hit(witheringRoll < ThrallArms.CLAW_EFFECT_CHANCE ? PhantomRules.WITHERING_TICKS : 0, 0,
                    rimefrostRoll < ThrallArms.CLAW_EFFECT_CHANCE ? PhantomRules.RIMEFROST_TICKS : 0);
            case OTHER -> new Hit(witheringRoll < OTHER_CHANCE ? PhantomRules.WITHERING_TICKS : 0, 0,
                    rimefrostRoll < OTHER_CHANCE ? PhantomRules.RIMEFROST_TICKS : 0);
        };
    }

    /** The floor's Rimefrost, for a Dread mob that lays none of its own on a melee hit: its duration, or 0 for none. */
    public static int floor(float roll) {
        return roll < RIMEFROST_FLOOR ? PhantomRules.RIMEFROST_TICKS : 0;
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
