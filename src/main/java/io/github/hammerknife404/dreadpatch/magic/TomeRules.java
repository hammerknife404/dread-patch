package io.github.hammerknife404.dreadpatch.magic;

/**
 * The Rimefrost spellbooks' numbers (0.98.3, the owner's handoff). Iron's best books hold 12 spells with +200 max mana and
 * one 10-20% bonus (its Ice Spell Book: +10% Ice spell power). The Rimebound Tome "reduces generic Ice bonuses in favor of
 * stronger Rimefrost school bonuses": half its Ice power, +15% Rimefrost. The Dread Sovereign's Tome is "heavily focused
 * on Rimefrost school bonuses": no Ice, +30% Rimefrost spell power and +10% Rimefrost resistance, and more mana.
 * Rimefrost spells are Ice too, so Ice power adds to theirs.
 */
public final class TomeRules {
    public static final int SLOTS = 12;
    public static final double ICE_BOOK_ICE_POWER = 0.10;
    public static final double RIMEBOUND_ICE_POWER = 0.05;
    public static final double RIMEBOUND_RIMEFROST_POWER = 0.15;
    public static final double RIMEBOUND_MANA = 200.0;
    public static final double SOVEREIGN_RIMEFROST_POWER = 0.30;
    public static final double SOVEREIGN_RIMEFROST_RESIST = 0.10;
    public static final double SOVEREIGN_MANA = 300.0;

    private TomeRules() {
    }
}
