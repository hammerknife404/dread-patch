package io.github.hammerknife404.dreadpatch.registry;

import com.iafenvoy.iceandfire.registry.IafItems;
import io.github.hammerknife404.dreadpatch.DreadPatch;
import io.github.hammerknife404.dreadpatch.effect.RimefrostEffect;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.Potion;
import net.minecraft.world.item.alchemy.Potions;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.brewing.RegisterBrewingRecipesEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * Potions of Rimefrost and Withering Rime (0.95.0, the owner: "rimefrost and withering rime potions are craftable"). Brewed
 * from an awkward potion with a Rimefrost Dragon Scale (Rimefrost) or a dread shard (Withering Rime); redstone makes each
 * longer, glowstone stronger. Vanilla's own recipes make their splash and lingering potions and tipped arrows, which the
 * creative tabs list with the rest. Ours: Rimefrost as Slowness is (I 1:30, long 4:00; strong II, the heavy slow, 0:45);
 * Withering Rime shorter than poison, since nothing heals through it (I 0:30, long 1:00; strong II, withering twice as
 * fast, 0:15). A Dread mob won't take either ({@code DreadEvents}).
 */
public final class DreadPotions {
    private static final DeferredRegister<Potion> POTIONS = DeferredRegister.create(Registries.POTION, DreadPatch.MODID);

    public static final DeferredHolder<Potion, Potion> RIMEFROST = POTIONS.register("rimefrost",
            () -> new Potion("rimefrost", new MobEffectInstance(ModRegistries.RIMEFROST, 1800)));
    public static final DeferredHolder<Potion, Potion> LONG_RIMEFROST = POTIONS.register("long_rimefrost",
            () -> new Potion("rimefrost", new MobEffectInstance(ModRegistries.RIMEFROST, 4800)));
    public static final DeferredHolder<Potion, Potion> STRONG_RIMEFROST = POTIONS.register("strong_rimefrost",
            () -> new Potion("rimefrost", new MobEffectInstance(ModRegistries.RIMEFROST, 900, RimefrostEffect.HEAVY)));
    public static final DeferredHolder<Potion, Potion> WITHERING_RIME = POTIONS.register("withering_rime",
            () -> new Potion("withering_rime", new MobEffectInstance(ModRegistries.WITHERING_RIME, 600)));
    public static final DeferredHolder<Potion, Potion> LONG_WITHERING_RIME = POTIONS.register("long_withering_rime",
            () -> new Potion("withering_rime", new MobEffectInstance(ModRegistries.WITHERING_RIME, 1200)));
    public static final DeferredHolder<Potion, Potion> STRONG_WITHERING_RIME = POTIONS.register("strong_withering_rime",
            () -> new Potion("withering_rime", new MobEffectInstance(ModRegistries.WITHERING_RIME, 300, 1)));

    private DreadPotions() {
    }

    public static void register(IEventBus modBus) {
        POTIONS.register(modBus);
        NeoForge.EVENT_BUS.addListener(DreadPotions::onBrewing);
    }

    private static void onBrewing(RegisterBrewingRecipesEvent event) {
        mixes(event, Potions.AWKWARD, DreadGear.RIMEFROST_DRAGON_SCALE.get(), RIMEFROST, LONG_RIMEFROST, STRONG_RIMEFROST);
        mixes(event, Potions.AWKWARD, IafItems.DREAD_SHARD.get(), WITHERING_RIME, LONG_WITHERING_RIME, STRONG_WITHERING_RIME);
    }

    private static void mixes(RegisterBrewingRecipesEvent event, Holder<Potion> base, net.minecraft.world.item.Item ingredient,
                              Holder<Potion> plain, Holder<Potion> longer, Holder<Potion> stronger) {
        event.getBuilder().addMix(base, ingredient, plain);
        event.getBuilder().addMix(plain, Items.REDSTONE, longer);
        event.getBuilder().addMix(plain, Items.GLOWSTONE_DUST, stronger);
    }
}
