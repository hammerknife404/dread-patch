package io.github.hammerknife404.dreadpatch.effect;

import com.iafenvoy.iceandfire.entity.util.IDreadMob;
import io.github.hammerknife404.dreadpatch.registry.ModRegistries;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.entity.living.LivingHealEvent;
import net.neoforged.neoforge.event.entity.living.MobEffectEvent;

/** Game-bus rules for her effects. */
public final class DreadEvents {
    private DreadEvents() {
    }

    public static void register(IEventBus gameBus) {
        gameBus.addListener(DreadEvents::onHeal);
        gameBus.addListener(DreadEvents::onEffectApplicable);
        RimeSiphon.register(gameBus);
        DreadHits.register(gameBus);
    }

    /** Nothing heals under Withering Rime: regeneration, potions, food and mod healing alike. */
    private static void onHeal(LivingHealEvent event) {
        if (event.getEntity().hasEffect(ModRegistries.WITHERING_RIME)) {
            event.setCanceled(true);
        }
    }

    /**
     * Pure Dread doesn't wither the Dread (the queen, her court and Ice and Fire's dread mobs), and
     * they see and walk through its storms, and its cold doesn't slow them (Rimefrost): they're of them.
     */
    private static void onEffectApplicable(MobEffectEvent.Applicable event) {
        if (io.github.hammerknife404.dreadpatch.entity.RisenDread.dread(event.getEntity())
                && (event.getEffectInstance().is(ModRegistries.WITHERING_RIME) || event.getEffectInstance().is(ModRegistries.WHITEOUT)
                || event.getEffectInstance().is(ModRegistries.RIMEFROST))) {
            event.setResult(MobEffectEvent.Applicable.Result.DO_NOT_APPLY);
        }
    }
}
