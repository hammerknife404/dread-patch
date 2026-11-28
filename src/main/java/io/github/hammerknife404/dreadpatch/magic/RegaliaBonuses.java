package io.github.hammerknife404.dreadpatch.magic;

/**
 * Game-free: what her regalia gives (0.51.0, the owner's second drop). Each piece: +150 max mana, 5%
 * off the mana an Ice spell costs (Rimefrost spells are Ice too), 5% more Rimefrost damage dealt and
 * 5% less taken (the draft: "−5% Rimefrost damage taken"; the owner, then: the regalia "is to add
 * rimefrost damage not lower it", and the wearer "taking less rimefire damage ... thats ok too"). The
 * full set: freeze immunity, and the Dread leave the wearer be. All four: a fifth off the mana, a fifth
 * more Rimefrost damage dealt and a fifth less taken.
 */
public final class RegaliaBonuses {
    public static final int PIECES = 4;
    public static final double MANA_PER_PIECE = 150.0;
    public static final float MANA_COST_PER_PIECE = 0.05F;
    public static final float RIMEFROST_DAMAGE_PER_PIECE = 0.05F;
    /**
     * Each piece's mage buffs (0.98.1, the owner's handoff: "Add additional per-piece mage buffs to each regalia piece
     * (such as Rimefrost Spell Power, Cooldown Reduction, and Mana Regen)"), Iron's multipliers on their base: every piece
     * Rimefrost spell power, and one of its own: the crown cooldowns, the bodice (winged too) spell power, the robe mana
     * regeneration, the greaves moving while casting. The full set: +20% Rimefrost spell power.
     */
    public static final double RIMEFROST_POWER_PER_PIECE = 0.05;
    public static final double CROWN_COOLDOWN_REDUCTION = 0.10;
    public static final double BODICE_SPELL_POWER = 0.05;
    public static final double ROBE_MANA_REGEN = 0.15;
    public static final double GREAVES_CASTING_MOVESPEED = 0.10;
    /** Each piece holds one spell, imbued at Iron's Arcane Anvil (the owner, asked about the handoff's sockets). */
    public static final int IMBUED_SPELLS = 1;

    private RegaliaBonuses() {
    }

    /** What an Ice (or Rimefrost) spell costing {@code cost} costs with {@code pieces} worn. */
    public static int manaCost(int cost, int pieces) {
        return Math.round(cost * (1.0F - MANA_COST_PER_PIECE * clamp(pieces)));
    }

    /** Rimefrost damage dealt by someone wearing {@code pieces}. */
    public static float rimefrostDamageDealt(float amount, int pieces) {
        return amount * (1.0F + RIMEFROST_DAMAGE_PER_PIECE * clamp(pieces));
    }

    /** Rimefrost damage taken by someone wearing {@code pieces}. */
    public static float rimefrostDamageTaken(float amount, int pieces) {
        return amount * (1.0F - RIMEFROST_DAMAGE_PER_PIECE * clamp(pieces));
    }

    public static boolean fullSet(int pieces) {
        return pieces >= PIECES;
    }

    private static int clamp(int pieces) {
        return Math.max(0, Math.min(PIECES, pieces));
    }
}
