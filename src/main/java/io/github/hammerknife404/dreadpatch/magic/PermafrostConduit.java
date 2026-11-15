package io.github.hammerknife404.dreadpatch.magic;

/** Game-free: her staff's Permafrost Conduit, which takes a fifth off the mana an Ice spell costs. */
public final class PermafrostConduit {
    public static final float MANA_DISCOUNT = 0.2F;

    private PermafrostConduit() {
    }

    /** What a spell that costs {@code cost} mana costs through the conduit. */
    public static int manaCost(int cost) {
        return Math.round(cost * (1.0F - MANA_DISCOUNT));
    }
}
