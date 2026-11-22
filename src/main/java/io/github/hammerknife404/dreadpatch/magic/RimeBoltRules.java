package io.github.hammerknife404.dreadpatch.magic;

/**
 * Game-free: the share of a target's max health a Withering Rime-Bolt takes (0.51.0, the owner's
 * second drop): a player's, by its level, from 3% (3% at level 1, a point a level, 7% at 5); the
 * queen's, by difficulty, 5% on Easy and Normal, 8% on Hard. Still capped at 20 ({@link DreadDamage}).
 * Until 0.51.0 a player's was 8% and hers 15%.
 */
public final class RimeBoltRules {
    public static final float SPELL_BASE_SHARE = 0.02F;
    public static final float SPELL_SHARE_PER_LEVEL = 0.01F;
    public static final float QUEEN_SHARE = 0.05F;
    public static final float QUEEN_HARD_SHARE = 0.08F;
    /** Vanilla's Hard difficulty id. */
    static final int HARD = 3;

    private RimeBoltRules() {
    }

    /** A player's bolt at {@code level}. */
    public static float spellShare(int level) {
        return SPELL_BASE_SHARE + SPELL_SHARE_PER_LEVEL * Math.max(1, level);
    }

    /** The queen's bolt on a difficulty (its id: 0 Peaceful to 3 Hard). */
    public static float queenShare(int difficulty) {
        return difficulty >= HARD ? QUEEN_HARD_SHARE : QUEEN_SHARE;
    }
}
