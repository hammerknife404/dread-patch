package io.github.hammerknife404.dreadpatch.effect;

import io.github.hammerknife404.dreadpatch.DreadPatch;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;

/**
 * Barrow-Glass: feet locked in brittle black dread-glass. No walking and no jumping while it lasts;
 * a Barrow-Glass Tomb keeps renewing it on everyone inside, and a Withering Rime-Bolt shatters it.
 */
public class BarrowGlassEffect extends MobEffect {
    /** Soot black. */
    public static final int COLOR = 0x0B0C10;

    public BarrowGlassEffect() {
        super(MobEffectCategory.HARMFUL, COLOR);
        this.addAttributeModifier(Attributes.MOVEMENT_SPEED, DreadPatch.id("barrow_glass_root"), -1.0, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
        this.addAttributeModifier(Attributes.JUMP_STRENGTH, DreadPatch.id("barrow_glass_root"), -1.0, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
    }
}
