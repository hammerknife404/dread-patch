package io.github.hammerknife404.dreadpatch.magic;

/**
 * Game-free: Create Rimefrost Dragon's numbers. A raised dragon rises with half (rounded down) of its
 * caster's missing health, the owner's rule (0.38.0): the more hurt the caster, the stronger it rises.
 * At least 1 (it's raised, not dead), at most its own max.
 */
public final class RimefrostRaising {
    public static final float HEALTH_SHARE = 0.5F;
    /** The queen raises one only once she's lost at least this share of her health (else it would rise all but dead). */
    public static final float QUEEN_RAISES_BELOW = 0.75F;

    private RimefrostRaising() {
    }

    public static float risingHealth(float casterMax, float casterHealth, float dragonMax) {
        float missing = Math.max(0.0F, casterMax - casterHealth);
        float health = (float) Math.floor(missing * HEALTH_SHARE);
        return Math.max(1.0F, Math.min(dragonMax, health));
    }
}
