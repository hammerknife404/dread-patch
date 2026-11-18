package io.github.hammerknife404.dreadpatch.effect;

import io.github.hammerknife404.dreadpatch.DreadPatch;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;

/**
 * Whiteout: caught in Whiteout Dominion. Slower in the drifts, and blind past a few blocks: a player
 * sees only thick snow-fog (drawn client-side, {@code WhiteoutClient}); a mob loses sight of anything
 * past half its follow range. The storm keeps renewing it on everyone inside.
 */
public class WhiteoutEffect extends MobEffect {
    /** Snow-light grey. */
    public static final int COLOR = 0xC8D2DC;
    public static final double SLOW = 0.2;
    public static final double SIGHT_LOST = 0.5;

    public WhiteoutEffect() {
        super(MobEffectCategory.HARMFUL, COLOR);
        this.addAttributeModifier(Attributes.MOVEMENT_SPEED, DreadPatch.id("whiteout_drifts"), -SLOW, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
        this.addAttributeModifier(Attributes.FOLLOW_RANGE, DreadPatch.id("whiteout_sight"), -SIGHT_LOST, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
    }
}
