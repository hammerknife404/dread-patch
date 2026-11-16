package io.github.hammerknife404.dreadpatch.effect;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;

/**
 * Withering Rime, the mark of pure Dread: cold that takes heat instead of being made of it. It
 * withers (damage that ignores armor), builds up freezing faster than the body thaws, and while it
 * lasts nothing heals the target ({@code DreadEvents}). The Dread Queen's own feeds her
 * ({@link RimeSiphon}). The Dread themselves don't take it.
 */
public class WitheringRimeEffect extends MobEffect {
    /** Abyssal indigo, the colour of her magic in the owner's notes. */
    public static final int COLOR = 0x1E2A4A;
    /** Ticks of freezing added each tick; the body thaws 2 a tick, so it builds 3 a tick. */
    static final int FREEZE_PER_TICK = 5;
    /** Ticks between withering hits at amplifier 0; each level halves it. Each takes 1. */
    static final int WITHER_INTERVAL = 20;

    public WitheringRimeEffect() {
        super(MobEffectCategory.HARMFUL, COLOR);
    }

    @Override
    public boolean shouldApplyEffectTickThisTick(int duration, int amplifier) {
        return true;
    }

    @Override
    public boolean applyEffectTick(LivingEntity entity, int amplifier) {
        if (entity.level().isClientSide) {
            return true;
        }
        if (entity.canFreeze()) {
            int max = entity.getTicksRequiredToFreeze() + 20;
            entity.setTicksFrozen(Math.min(max, entity.getTicksFrozen() + FREEZE_PER_TICK));
        }
        int interval = Math.max(5, WITHER_INTERVAL >> amplifier);
        if (entity.tickCount % interval == 0) {
            float before = entity.getHealth() + entity.getAbsorptionAmount();
            if (entity.hurt(entity.damageSources().wither(), 1.0F) && entity.getHealth() + entity.getAbsorptionAmount() < before) {
                RimeSiphon.feed(entity);
            }
        }
        return true;
    }
}
