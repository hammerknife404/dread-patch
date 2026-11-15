package io.github.hammerknife404.dreadpatch.effect;

import io.github.hammerknife404.dreadpatch.DreadPatch;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;

/**
 * Rimebound: feet frozen fast inside a Rimefire Crown. No walking and no jumping while it lasts; the
 * crown keeps renewing it on everyone inside, and a Withering Rime-Bolt shatters it.
 */
public class RimeboundEffect extends MobEffect {
    /** Her dread-fire cyan. */
    public static final int COLOR = 0x00CBFF;

    public RimeboundEffect() {
        super(MobEffectCategory.HARMFUL, COLOR);
        this.addAttributeModifier(Attributes.MOVEMENT_SPEED, DreadPatch.id("rimebound_root"), -1.0, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
        this.addAttributeModifier(Attributes.JUMP_STRENGTH, DreadPatch.id("rimebound_root"), -1.0, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
    }
}
