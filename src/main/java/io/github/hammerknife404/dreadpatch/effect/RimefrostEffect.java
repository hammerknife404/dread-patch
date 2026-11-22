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
    public static final double SLOW = 0.25;

    public RimefrostEffect() {
        super(MobEffectCategory.HARMFUL, COLOR);
        this.addAttributeModifier(Attributes.MOVEMENT_SPEED, DreadPatch.id("rimefrost"), -SLOW, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
    }
}
