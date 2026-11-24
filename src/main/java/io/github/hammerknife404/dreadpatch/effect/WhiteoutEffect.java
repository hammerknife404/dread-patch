package io.github.hammerknife404.dreadpatch.effect;

import io.github.hammerknife404.dreadpatch.DreadPatch;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.neoforged.neoforge.common.Tags;

/**
 * Whiteout: caught in Whiteout Dominion. Slower in the drifts, and blind past a few blocks: a player
 * sees only thick snow-fog (drawn client-side, {@code WhiteoutClient}); a mob loses sight of anything
 * past half its follow range, but a boss doesn't. The storm keeps renewing it on everyone inside.
 */
public class WhiteoutEffect extends MobEffect {
    /** Snow-light grey. */
    public static final int COLOR = 0xC8D2DC;
    public static final double SLOW = 0.2;
    public static final double SIGHT_LOST = 0.5;

    private static final ResourceLocation SIGHT = DreadPatch.id("whiteout_sight");

    public WhiteoutEffect() {
        super(MobEffectCategory.HARMFUL, COLOR);
        this.addAttributeModifier(Attributes.MOVEMENT_SPEED, DreadPatch.id("whiteout_drifts"), -SLOW, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
        this.addAttributeModifier(Attributes.FOLLOW_RANGE, SIGHT, -SIGHT_LOST, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
    }

    /**
     * Bosses ({@code #c:bosses}) keep their sight, and only slow (0.60.0): with half its follow range a
     * boss lost anyone past it, so inside a storm 32 wide a player could fight one from where it gave up.
     * Vanilla calls this on every application, renewals too, just after putting the modifiers back.
     */
    @Override
    public void onEffectStarted(LivingEntity entity, int amplifier) {
        if (entity.getType().is(Tags.EntityTypes.BOSSES)) {
            AttributeInstance range = entity.getAttribute(Attributes.FOLLOW_RANGE);
            if (range != null) {
                range.removeModifier(SIGHT);
            }
        }
    }
}
