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
