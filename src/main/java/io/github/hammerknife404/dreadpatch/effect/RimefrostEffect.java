package io.github.hammerknife404.dreadpatch.effect;

import io.github.hammerknife404.dreadpatch.DreadPatch;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;

/**
 * Rimefrost (0.45.0): the Dread's cold in the bones, a slow and nothing more (no withering, unlike
 * Withering Rime). The dread phantom's bite leaves it; the Dreadlands' storms will too. The owner:
 * "the slow, no withering"; ours, −25% movement. The Dread refuse it.
 */
public class RimefrostEffect extends MobEffect {
    /** Pale rime blue. */
    public static final int COLOR = 0xA8DCF0;
    /** Rimefrost II and up: a heavy snowstorm's, a dread phantom's bite, rimefire. */
    public static final double SLOW = 0.25;
    /** Rimefrost I: plain snow in the Dreadlands (0.56.0, the owner: "normal snow should be a modest 10%. Heavy Should be the current"). */
    public static final double SLOW_MILD = 0.10;
    /** The level (amplifier) that slows by {@link #SLOW}. */
    public static final int HEAVY = 1;
    private static final net.minecraft.resources.ResourceLocation ID = DreadPatch.id("rimefrost");

    public RimefrostEffect() {
        super(MobEffectCategory.HARMFUL, COLOR);
        // NeoForge's level curve: I mild, II and up heavy (not vanilla's level times the amount).
        this.addAttributeModifier(Attributes.MOVEMENT_SPEED, ID, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL, amplifier -> -slow(amplifier));
    }

    /** How much it slows at {@code amplifier}: I mild, II and up heavy (not vanilla's level times the amount). */
    public static double slow(int amplifier) {
        return amplifier <= 0 ? SLOW_MILD : SLOW;
    }
}
