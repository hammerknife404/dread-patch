package io.github.hammerknife404.dreadpatch.registry;

import io.github.hammerknife404.dreadpatch.DreadPatch;
import io.github.hammerknife404.dreadpatch.effect.RimeboundEffect;
import io.github.hammerknife404.dreadpatch.effect.WitheringRimeEffect;
import io.github.hammerknife404.dreadpatch.entity.DreadQueen;
import io.github.hammerknife404.dreadpatch.entity.RimefireCrown;
import io.github.hammerknife404.dreadpatch.entity.WitheringRimeBolt;
import io.github.hammerknife404.dreadpatch.spell.RimefireCrownSpell;
import io.github.hammerknife404.dreadpatch.spell.WitheringRimeBoltSpell;
import io.redspace.ironsspellbooks.api.registry.SpellRegistry;
import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.item.Item;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.DeferredSpawnEggItem;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModRegistries {
    private static final DeferredRegister<EntityType<?>> ENTITY_TYPES = DeferredRegister.create(Registries.ENTITY_TYPE, DreadPatch.MODID);
    private static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(DreadPatch.MODID);
    private static final DeferredRegister<MobEffect> MOB_EFFECTS = DeferredRegister.create(Registries.MOB_EFFECT, DreadPatch.MODID);
    private static final DeferredRegister<AbstractSpell> SPELLS = DeferredRegister.create(SpellRegistry.SPELL_REGISTRY_KEY, DreadPatch.MODID);

    /**
     * Ice and Fire's Dread Queen (which it never registers), through our {@link DreadQueen}. A
     * full-size humanoid (the zombie's box); tracked from as far as the Wither, so her boss bar
     * shows from afar.
     */
    public static final DeferredHolder<EntityType<?>, EntityType<DreadQueen>> DREAD_QUEEN = ENTITY_TYPES.register("dread_queen",
            () -> EntityType.Builder.<DreadQueen>of(DreadQueen::new, MobCategory.MONSTER)
                    .sized(0.6F, 1.95F)
                    .clientTrackingRange(10)
                    .build(DreadPatch.id("dread_queen").toString()));

    public static final DeferredHolder<EntityType<?>, EntityType<WitheringRimeBolt>> WITHERING_RIME_BOLT = ENTITY_TYPES.register("withering_rime_bolt",
            () -> EntityType.Builder.<WitheringRimeBolt>of(WitheringRimeBolt::new, MobCategory.MISC)
                    .sized(0.5F, 0.5F)
                    .clientTrackingRange(8)
                    .build(DreadPatch.id("withering_rime_bolt").toString()));

    /** As wide as its ring, so its spikes aren't culled while the centre is off screen. */
    public static final DeferredHolder<EntityType<?>, EntityType<RimefireCrown>> RIMEFIRE_CROWN = ENTITY_TYPES.register("rimefire_crown",
            () -> EntityType.Builder.<RimefireCrown>of(RimefireCrown::new, MobCategory.MISC)
                    .sized(RimefireCrown.RADIUS * 2, 2.5F)
                    .clientTrackingRange(8)
                    .build(DreadPatch.id("rimefire_crown").toString()));

    /** Ice and Fire's dread egg base, spotted with the queen's eye glow. */
    public static final DeferredItem<DeferredSpawnEggItem> DREAD_QUEEN_SPAWN_EGG = ITEMS.register("dread_queen_spawn_egg",
            () -> new DeferredSpawnEggItem(DREAD_QUEEN, 0xE0E6E6, 0x00CBFF, new Item.Properties()));

    public static final DeferredHolder<MobEffect, WitheringRimeEffect> WITHERING_RIME = MOB_EFFECTS.register("withering_rime", WitheringRimeEffect::new);

    public static final DeferredHolder<MobEffect, RimeboundEffect> RIMEBOUND = MOB_EFFECTS.register("rimebound", RimeboundEffect::new);

    public static final DeferredHolder<AbstractSpell, WitheringRimeBoltSpell> WITHERING_RIME_BOLT_SPELL = SPELLS.register("withering_rime_bolt", WitheringRimeBoltSpell::new);
    public static final DeferredHolder<AbstractSpell, RimefireCrownSpell> RIMEFIRE_CROWN_SPELL = SPELLS.register("rimefire_crown", RimefireCrownSpell::new);

    private ModRegistries() {
    }

    public static void register(IEventBus modBus) {
        ENTITY_TYPES.register(modBus);
        ITEMS.register(modBus);
        MOB_EFFECTS.register(modBus);
        SPELLS.register(modBus);
    }
}
