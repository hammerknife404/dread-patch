package io.github.hammerknife404.dreadpatch.effect;

import io.github.hammerknife404.dreadpatch.DreadPatch;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;

/**
 * Brittle: what's left after her sword's Glass-Rime Shatter breaks the frost on someone. Their armor
 * is a tenth weaker while it lasts.
 */
public class BrittleEffect extends MobEffect {
    /** Ice blue. */
    public static final int COLOR = 0xA9CDF5;
    public static final double ARMOR_LOSS = 0.1;

    public BrittleEffect() {
        super(MobEffectCategory.HARMFUL, COLOR);
        this.addAttributeModifier(Attributes.ARMOR, DreadPatch.id("brittle_armor"), -ARMOR_LOSS, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
    }
}
