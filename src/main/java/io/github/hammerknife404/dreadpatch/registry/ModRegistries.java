package io.github.hammerknife404.dreadpatch.registry;

import io.github.hammerknife404.dreadpatch.DreadPatch;
import io.github.hammerknife404.dreadpatch.effect.BrittleEffect;
import io.github.hammerknife404.dreadpatch.effect.RimeboundEffect;
import io.github.hammerknife404.dreadpatch.effect.WitheringRimeEffect;
import io.github.hammerknife404.dreadpatch.entity.BarrowHusk;
import io.github.hammerknife404.dreadpatch.entity.DreadQueen;
import io.github.hammerknife404.dreadpatch.entity.RimefireCrown;
import io.github.hammerknife404.dreadpatch.entity.ShatterSpineCore;
import io.github.hammerknife404.dreadpatch.entity.SovereignsEclipse;
import io.github.hammerknife404.dreadpatch.entity.ShatterSpineShard;
import io.github.hammerknife404.dreadpatch.entity.Syndrigossa;
import io.github.hammerknife404.dreadpatch.entity.WitheringRimeBolt;
import io.github.hammerknife404.dreadpatch.spell.BarrowStepSpell;
import io.github.hammerknife404.dreadpatch.spell.RimefireCrownSpell;
import io.github.hammerknife404.dreadpatch.spell.WitheringRimeBoltSpell;
import io.redspace.ironsspellbooks.api.registry.SpellRegistry;
import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.util.Unit;
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
    private static final DeferredRegister.DataComponents DATA_COMPONENTS = DeferredRegister.createDataComponents(Registries.DATA_COMPONENT_TYPE, DreadPatch.MODID);

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

    /** Barrow-Step's husk: her shape, her size. */
    public static final DeferredHolder<EntityType<?>, EntityType<BarrowHusk>> BARROW_HUSK = ENTITY_TYPES.register("barrow_husk",
            () -> EntityType.Builder.<BarrowHusk>of(BarrowHusk::new, MobCategory.MISC)
                    .sized(0.6F, 1.95F)
                    .clientTrackingRange(8)
                    .build(DreadPatch.id("barrow_husk").toString()));

    /** Syndrigossa (proof of concept): an Ice and Fire ice dragon's base size and category; it grows by stage. */
    public static final DeferredHolder<EntityType<?>, EntityType<Syndrigossa>> SYNDRIGOSSA = ENTITY_TYPES.register("syndrigossa",
            () -> EntityType.Builder.<Syndrigossa>of(Syndrigossa::new, MobCategory.CREATURE)
                    .sized(0.78F, 1.2F)
                    .clientTrackingRange(10)
                    .build(DreadPatch.id("syndrigossa").toString()));

    /** Syndrigossa's Rimefire Crown: as wide as its ring, like the crown. */
    public static final DeferredHolder<EntityType<?>, EntityType<SovereignsEclipse>> SOVEREIGNS_ECLIPSE = ENTITY_TYPES.register("sovereigns_eclipse",
            () -> EntityType.Builder.<SovereignsEclipse>of(SovereignsEclipse::new, MobCategory.MISC)
                    .sized(SovereignsEclipse.RADIUS * 2, (float) SovereignsEclipse.HEIGHT)
                    .clientTrackingRange(10)
                    .build(DreadPatch.id("sovereigns_eclipse").toString()));

    /** Shatter-Spine, Syndrigossa's charge, and the shards it bursts into. Drawn by their trails. */
    public static final DeferredHolder<EntityType<?>, EntityType<ShatterSpineCore>> SHATTER_SPINE = ENTITY_TYPES.register("shatter_spine",
            () -> EntityType.Builder.<ShatterSpineCore>of(ShatterSpineCore::new, MobCategory.MISC)
                    .sized(0.9F, 0.9F)
                    .clientTrackingRange(8)
                    .build(DreadPatch.id("shatter_spine").toString()));
    public static final DeferredHolder<EntityType<?>, EntityType<ShatterSpineShard>> SHATTER_SPINE_SHARD = ENTITY_TYPES.register("shatter_spine_shard",
            () -> EntityType.Builder.<ShatterSpineShard>of(ShatterSpineShard::new, MobCategory.MISC)
                    .sized(0.4F, 0.4F)
                    .clientTrackingRange(8)
                    .build(DreadPatch.id("shatter_spine_shard").toString()));

    /** Syndrigossa's breath and her Shatter-Spine's shards (data/dreadpatch/damage_type). */
    public static final ResourceKey<DamageType> WITHERING_BREATH_DAMAGE = ResourceKey.create(Registries.DAMAGE_TYPE, DreadPatch.id("withering_breath"));
    public static final ResourceKey<DamageType> SHATTER_SPINE_DAMAGE = ResourceKey.create(Registries.DAMAGE_TYPE, DreadPatch.id("shatter_spine"));

    /** Ice and Fire's dread egg base, spotted with the queen's eye glow. */
    public static final DeferredItem<DeferredSpawnEggItem> DREAD_QUEEN_SPAWN_EGG = ITEMS.register("dread_queen_spawn_egg",
            () -> new DeferredSpawnEggItem(DREAD_QUEEN, 0xE0E6E6, 0x00CBFF, new Item.Properties()));

    /** Soot spotted with bone. */
    public static final DeferredItem<DeferredSpawnEggItem> SYNDRIGOSSA_SPAWN_EGG = ITEMS.register("syndrigossa_spawn_egg",
            () -> new DeferredSpawnEggItem(SYNDRIGOSSA, 0x14161D, 0xD8DEE9, new Item.Properties()));

    public static final DeferredHolder<MobEffect, WitheringRimeEffect> WITHERING_RIME = MOB_EFFECTS.register("withering_rime", WitheringRimeEffect::new);

    public static final DeferredHolder<MobEffect, RimeboundEffect> RIMEBOUND = MOB_EFFECTS.register("rimebound", RimeboundEffect::new);

    public static final DeferredHolder<MobEffect, BrittleEffect> BRITTLE = MOB_EFFECTS.register("brittle", BrittleEffect::new);

    /** On her sword: its guard has charged the blade, and its next full strike shatters. Saved and sent to clients (the tooltip shows it). */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Unit>> FROST_CHARGE = DATA_COMPONENTS.registerComponentType("frost_charge",
            builder -> builder.persistent(Unit.CODEC).networkSynchronized(StreamCodec.unit(Unit.INSTANCE)));

    public static final DeferredHolder<AbstractSpell, WitheringRimeBoltSpell> WITHERING_RIME_BOLT_SPELL = SPELLS.register("withering_rime_bolt", WitheringRimeBoltSpell::new);
    public static final DeferredHolder<AbstractSpell, RimefireCrownSpell> RIMEFIRE_CROWN_SPELL = SPELLS.register("rimefire_crown", RimefireCrownSpell::new);
    public static final DeferredHolder<AbstractSpell, BarrowStepSpell> BARROW_STEP_SPELL = SPELLS.register("barrow_step", BarrowStepSpell::new);

    private ModRegistries() {
    }

    public static DamageSource damageSource(Level level, ResourceKey<DamageType> type, @Nullable Entity direct, @Nullable Entity causing) {
        return new DamageSource(level.registryAccess().registryOrThrow(Registries.DAMAGE_TYPE).getHolderOrThrow(type), direct, causing);
    }

    public static void register(IEventBus modBus) {
        ENTITY_TYPES.register(modBus);
        ITEMS.register(modBus);
        MOB_EFFECTS.register(modBus);
        SPELLS.register(modBus);
        DATA_COMPONENTS.register(modBus);
    }
}
