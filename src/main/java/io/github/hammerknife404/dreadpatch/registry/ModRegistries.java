package io.github.hammerknife404.dreadpatch.registry;

import io.github.hammerknife404.dreadpatch.DreadPatch;
import io.github.hammerknife404.dreadpatch.block.DreadGateBlock;
import io.github.hammerknife404.dreadpatch.block.RimefireBlock;
import net.minecraft.world.level.material.MapColor;
import io.github.hammerknife404.dreadpatch.effect.BrittleEffect;
import io.github.hammerknife404.dreadpatch.effect.RimefrostEffect;
import io.github.hammerknife404.dreadpatch.effect.RimeboundEffect;
import io.github.hammerknife404.dreadpatch.effect.WhiteoutEffect;
import io.github.hammerknife404.dreadpatch.effect.WitheringRimeEffect;
import io.github.hammerknife404.dreadpatch.entity.BarrowHusk;
import io.github.hammerknife404.dreadpatch.entity.DreadGate;
import io.github.hammerknife404.dreadpatch.entity.DreadPhantom;
import io.github.hammerknife404.dreadpatch.entity.DreadQueen;
import io.github.hammerknife404.dreadpatch.entity.RimefireCrown;
import io.github.hammerknife404.dreadpatch.entity.ShatterSpineCore;
import io.github.hammerknife404.dreadpatch.entity.SovereignsEclipse;
import io.github.hammerknife404.dreadpatch.entity.ShatterSpineShard;
import io.github.hammerknife404.dreadpatch.entity.RimefrostDragon;
import io.github.hammerknife404.dreadpatch.entity.Syndrigossa;
import io.github.hammerknife404.dreadpatch.entity.SyndrigossaEggEntity;
import io.github.hammerknife404.dreadpatch.item.SyndrigossaEggItem;
import io.github.hammerknife404.dreadpatch.entity.WhiteoutDominion;
import io.github.hammerknife404.dreadpatch.entity.WitheringRimeBolt;
import io.github.hammerknife404.dreadpatch.spell.BarrowStepSpell;
import io.github.hammerknife404.dreadpatch.spell.RimefireCrownSpell;
import io.github.hammerknife404.dreadpatch.spell.CreateRimefrostDragonSpell;
import io.github.hammerknife404.dreadpatch.spell.SummonCourtSpell;
import io.github.hammerknife404.dreadpatch.spell.SummonDreadPhantomsSpell;
import io.github.hammerknife404.dreadpatch.spell.RimefireBlastSpell;
import io.github.hammerknife404.dreadpatch.spell.RimefrostSchool;
import io.github.hammerknife404.dreadpatch.entity.RimefireBlastProjectile;
import io.redspace.ironsspellbooks.api.attribute.MagicPercentAttribute;
import io.redspace.ironsspellbooks.api.registry.SchoolRegistry;
import io.redspace.ironsspellbooks.api.spells.SchoolType;
import io.redspace.ironsspellbooks.registries.SoundRegistry;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.ai.attributes.Attribute;
import io.github.hammerknife404.dreadpatch.spell.WhiteoutDominionSpell;
import io.github.hammerknife404.dreadpatch.spell.WitheringRimeBoltSpell;
import io.github.hammerknife404.dreadpatch.world.SpirePiece;
import io.github.hammerknife404.dreadpatch.world.SpireStructure;
import io.github.hammerknife404.dreadpatch.world.DreadVillageStructure;
import net.minecraft.world.level.levelgen.structure.StructureType;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceType;
import io.redspace.ironsspellbooks.api.registry.SpellRegistry;
import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.sounds.SoundEvent;
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
import net.minecraft.world.item.JukeboxSong;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.PushReaction;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.DeferredSpawnEggItem;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModRegistries {
    private static final DeferredRegister<EntityType<?>> ENTITY_TYPES = DeferredRegister.create(Registries.ENTITY_TYPE, DreadPatch.MODID);
    private static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(DreadPatch.MODID);
    private static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(DreadPatch.MODID);
    private static final DeferredRegister<MobEffect> MOB_EFFECTS = DeferredRegister.create(Registries.MOB_EFFECT, DreadPatch.MODID);
    private static final DeferredRegister<AbstractSpell> SPELLS = DeferredRegister.create(SpellRegistry.SPELL_REGISTRY_KEY, DreadPatch.MODID);
    private static final DeferredRegister<ParticleType<?>> PARTICLE_TYPES = DeferredRegister.create(Registries.PARTICLE_TYPE, DreadPatch.MODID);
    private static final DeferredRegister.DataComponents DATA_COMPONENTS = DeferredRegister.createDataComponents(Registries.DATA_COMPONENT_TYPE, DreadPatch.MODID);
    private static final DeferredRegister<SoundEvent> SOUND_EVENTS = DeferredRegister.create(Registries.SOUND_EVENT, DreadPatch.MODID);
    private static final DeferredRegister<Attribute> ATTRIBUTES = DeferredRegister.create(Registries.ATTRIBUTE, DreadPatch.MODID);
    private static final DeferredRegister<SchoolType> SCHOOLS = DeferredRegister.create(SchoolRegistry.SCHOOL_REGISTRY_KEY, DreadPatch.MODID);

    /** Rimefrost's spell power and magic resistance (0.51.0), as Iron's schools' are (1 = none; a multiplier). */
    public static final DeferredHolder<Attribute, MagicPercentAttribute> RIMEFROST_SPELL_POWER = ATTRIBUTES.register("rimefrost_spell_power",
            () -> (MagicPercentAttribute) new MagicPercentAttribute("attribute.dreadpatch.rimefrost_spell_power", 1.0, -100.0, 100.0).setSyncable(true));
    public static final DeferredHolder<Attribute, MagicPercentAttribute> RIMEFROST_MAGIC_RESIST = ATTRIBUTES.register("rimefrost_magic_resist",
            () -> (MagicPercentAttribute) new MagicPercentAttribute("attribute.dreadpatch.rimefrost_magic_resist", 1.0, -100.0, 100.0).setSyncable(true));
    /** Rimefrost's focus at the Scroll Forge: a dread shard. */
    public static final TagKey<Item> RIMEFROST_FOCUS = TagKey.create(Registries.ITEM, DreadPatch.id("rimefrost_focus"));
    /** Rimefrost magic, the school's damage (data/dreadpatch/damage_type). */
    public static final ResourceKey<DamageType> RIMEFROST_MAGIC_DAMAGE = ResourceKey.create(Registries.DAMAGE_TYPE, DreadPatch.id("rimefrost_magic"));
    /** Rimefrost (0.51.0), her spells' school, a subtype of Ice ({@link RimefrostSchool}). Her cyan. */
    public static final DeferredHolder<SchoolType, RimefrostSchool> RIMEFROST_SCHOOL = SCHOOLS.register("rimefrost",
            () -> new RimefrostSchool(DreadPatch.id("rimefrost"), RIMEFROST_FOCUS,
                    Component.translatable("school.dreadpatch.rimefrost").withStyle(Style.EMPTY.withColor(0x5CE1FF)),
                    RIMEFROST_SPELL_POWER, RIMEFROST_MAGIC_RESIST, SoundRegistry.ICE_CAST, RIMEFROST_MAGIC_DAMAGE));
    private static final DeferredRegister<StructureType<?>> STRUCTURE_TYPES = DeferredRegister.create(Registries.STRUCTURE_TYPE, DreadPatch.MODID);
    private static final DeferredRegister<StructurePieceType> STRUCTURE_PIECES = DeferredRegister.create(Registries.STRUCTURE_PIECE, DreadPatch.MODID);

    /** The Dreadlands' spires (0.46.0): a structure of ours, one piece drawn from its shape. */
    public static final DeferredHolder<StructureType<?>, StructureType<SpireStructure>> SPIRE = STRUCTURE_TYPES.register("spire", () -> () -> SpireStructure.CODEC);
    /** The Dreadlands' abandoned villages (0.53.0): vanilla's snowy zombie village, turned Dread. */
    public static final DeferredHolder<StructureType<?>, StructureType<DreadVillageStructure>> DREAD_VILLAGE = STRUCTURE_TYPES.register("dread_village",
            () -> () -> DreadVillageStructure.CODEC);
    public static final DeferredHolder<StructurePieceType, StructurePieceType> SPIRE_PIECE = STRUCTURE_PIECES.register("spire",
            () -> (StructurePieceType.ContextlessType) SpirePiece::new);

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

    /** The dread phantom (0.45.0): vanilla's phantom's box and tracking. */
    public static final DeferredHolder<EntityType<?>, EntityType<DreadPhantom>> DREAD_PHANTOM = ENTITY_TYPES.register("dread_phantom",
            () -> EntityType.Builder.<DreadPhantom>of(DreadPhantom::new, MobCategory.MONSTER)
                    .sized(0.9F, 0.5F)
                    .eyeHeight(0.175F)
                    .passengerAttachments(0.3375F)
                    .ridingOffset(-0.125F)
                    .clientTrackingRange(8)
                    .build(DreadPatch.id("dread_phantom").toString()));
    /** The miniature dread phantom (0.63.0): the dread phantom's box, which its scale of 0.5 halves. */
    public static final DeferredHolder<EntityType<?>, EntityType<io.github.hammerknife404.dreadpatch.entity.MiniatureDreadPhantom>> MINIATURE_DREAD_PHANTOM =
            ENTITY_TYPES.register("miniature_dread_phantom",
                    () -> EntityType.Builder.<io.github.hammerknife404.dreadpatch.entity.MiniatureDreadPhantom>of(
                                    io.github.hammerknife404.dreadpatch.entity.MiniatureDreadPhantom::new, MobCategory.MONSTER)
                            .sized(0.9F, 0.5F)
                            .eyeHeight(0.175F)
                            .clientTrackingRange(8)
                            .build(DreadPatch.id("miniature_dread_phantom").toString()));

    /** Rimefire Blast's cone (0.51.0): Iron's Cone of Cold's size and tracking; it draws itself in particles. */
    public static final DeferredHolder<EntityType<?>, EntityType<RimefireBlastProjectile>> RIMEFIRE_BLAST_PROJECTILE = ENTITY_TYPES.register("rimefire_blast",
            () -> EntityType.Builder.<RimefireBlastProjectile>of(RimefireBlastProjectile::new, MobCategory.MISC)
                    .sized(1.0F, 1.0F)
                    .clientTrackingRange(64)
                    .build(DreadPatch.id("rimefire_blast").toString()));

    /** Syndrigossa's egg, laid: Ice and Fire's egg's size. */
    public static final DeferredHolder<EntityType<?>, EntityType<SyndrigossaEggEntity>> SYNDRIGOSSA_EGG_ENTITY = ENTITY_TYPES.register("syndrigossa_egg",
            () -> EntityType.Builder.<SyndrigossaEggEntity>of(SyndrigossaEggEntity::new, MobCategory.MISC)
                    .sized(0.45F, 0.55F)
                    .clientTrackingRange(8)
                    .build(DreadPatch.id("syndrigossa_egg").toString()));

    /** A Rimefrost dragon (0.37.0), and Syndrigossa, the unique one: an Ice and Fire ice dragon's base size and category; they grow by stage. */
    public static final DeferredHolder<EntityType<?>, EntityType<RimefrostDragon>> RIMEFROST_DRAGON = ENTITY_TYPES.register("rimefrost_dragon",
            () -> EntityType.Builder.<RimefrostDragon>of(RimefrostDragon::new, MobCategory.CREATURE)
                    .sized(0.78F, 1.2F)
                    .clientTrackingRange(10)
                    .build(DreadPatch.id("rimefrost_dragon").toString()));
    public static final DeferredHolder<EntityType<?>, EntityType<Syndrigossa>> SYNDRIGOSSA = ENTITY_TYPES.register("syndrigossa",
            () -> EntityType.Builder.<Syndrigossa>of(Syndrigossa::new, MobCategory.CREATURE)
                    .sized(0.78F, 1.2F)
                    .clientTrackingRange(10)
                    .build(DreadPatch.id("syndrigossa").toString()));

    /**
     * Whiteout Dominion: a point where it was raised (it reaches far past its box, and says so for
     * culling); tracked from as far as the queen, since its snow wall shows from outside it.
     */
    public static final DeferredHolder<EntityType<?>, EntityType<WhiteoutDominion>> WHITEOUT_DOMINION = ENTITY_TYPES.register("whiteout_dominion",
            () -> EntityType.Builder.<WhiteoutDominion>of(WhiteoutDominion::new, MobCategory.MISC)
                    .sized(1.0F, 1.0F)
                    .clientTrackingRange(10)
                    .updateInterval(20)
                    .build(DreadPatch.id("whiteout_dominion").toString()));

    /** A Dread Gate's ritual: a point at the foot of the gate, invisible; it draws itself in blocks and particles. */
    public static final DeferredHolder<EntityType<?>, EntityType<DreadGate>> DREAD_GATE = ENTITY_TYPES.register("dread_gate",
            () -> EntityType.Builder.<DreadGate>of(DreadGate::new, MobCategory.MISC)
                    .sized(0.5F, 0.5F)
                    .clientTrackingRange(8)
                    .updateInterval(20)
                    .build(DreadPatch.id("dread_gate").toString()));

    /** A Dread Gate's open face: no item, no drops, unbreakable but in creative, lit as every dread light ({@link DreadLights}). */
    public static final DeferredBlock<DreadGateBlock> DREAD_GATE_BLOCK = BLOCKS.register("dread_gate",
            () -> new DreadGateBlock(BlockBehaviour.Properties.of()
                    .noCollission()
                    .strength(-1.0F, 3600000.0F)
                    .sound(SoundType.GLASS)
                    .lightLevel(state -> DreadLights.LIGHT)
                    .pushReaction(PushReaction.BLOCK)
                    .noLootTable()));

    /** Rimefire (0.48.0): the cold dread fire lightning leaves in the Dreadlands' storms. Lit like soul fire; no item, no drops. */
    public static final DeferredBlock<RimefireBlock> RIMEFIRE = BLOCKS.register("rimefire",
            () -> new RimefireBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_LIGHT_BLUE)
                    .replaceable()
                    .noCollission()
                    .instabreak()
                    .lightLevel(state -> RimefireBlock.LIGHT)
                    .sound(SoundType.WOOL)
                    .pushReaction(PushReaction.DESTROY)
                    .noLootTable()));

    /** The Dread's lantern (0.57.0): a soul lantern's shape and light, its flame her cyan. Fading lanterns and the villages' end as it. */
    public static final DeferredBlock<net.minecraft.world.level.block.LanternBlock> DREAD_LANTERN = BLOCKS.register("dread_lantern",
            () -> new net.minecraft.world.level.block.LanternBlock(BlockBehaviour.Properties.ofFullCopy(net.minecraft.world.level.block.Blocks.SOUL_LANTERN)
                    .lightLevel(state -> DreadLights.LIGHT)));
    public static final DeferredItem<net.minecraft.world.item.BlockItem> DREAD_LANTERN_ITEM = ITEMS.registerSimpleBlockItem("dread_lantern", DREAD_LANTERN);

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

    /** Syndrigossa's breath, big enough to see: a cyan flame and a soot cloud (vanilla sprites, our particle). */
    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> RIME_FLAME = PARTICLE_TYPES.register("rime_flame", () -> new SimpleParticleType(false));
    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> RIME_SMOKE = PARTICLE_TYPES.register("rime_smoke", () -> new SimpleParticleType(false));
    /** The same at vanilla's size: the Withering Rime-Bolt's trail and burst. */
    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> RIME_FLAME_SMALL = PARTICLE_TYPES.register("rime_flame_small", () -> new SimpleParticleType(false));
    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> RIME_SMOKE_SMALL = PARTICLE_TYPES.register("rime_smoke_small", () -> new SimpleParticleType(false));

    /** Syndrigossa's breath and her Shatter-Spine's shards (data/dreadpatch/damage_type). */
    public static final ResourceKey<DamageType> WITHERING_BREATH_DAMAGE = ResourceKey.create(Registries.DAMAGE_TYPE, DreadPatch.id("withering_breath"));
    public static final ResourceKey<DamageType> SHATTER_SPINE_DAMAGE = ResourceKey.create(Registries.DAMAGE_TYPE, DreadPatch.id("shatter_spine"));
    /** Whiteout Dominion's cold. */
    public static final ResourceKey<DamageType> WHITEOUT_DAMAGE = ResourceKey.create(Registries.DAMAGE_TYPE, DreadPatch.id("whiteout"));
    /** Standing in rimefire (0.55.0), not of the Dread. */
    public static final ResourceKey<DamageType> RIMEFIRE_DAMAGE = ResourceKey.create(Registries.DAMAGE_TYPE, DreadPatch.id("rimefire"));

    /** Ice and Fire's dread egg base, spotted with the queen's eye glow. */
    public static final DeferredItem<DeferredSpawnEggItem> DREAD_QUEEN_SPAWN_EGG = ITEMS.register("dread_queen_spawn_egg",
            () -> new DeferredSpawnEggItem(DREAD_QUEEN, 0xE0E6E6, 0x00CBFF, new Item.Properties()));

    /** Soot spotted with bone. */
    public static final DeferredItem<DeferredSpawnEggItem> SYNDRIGOSSA_SPAWN_EGG = ITEMS.register("syndrigossa_spawn_egg",
            () -> new DeferredSpawnEggItem(SYNDRIGOSSA, 0x14161D, 0xD8DEE9, new Item.Properties()));
    /** A Rimefrost dragon's: soot spotted with her cyan, where Syndrigossa's is soot and bone. */
    public static final DeferredItem<DeferredSpawnEggItem> RIMEFROST_DRAGON_SPAWN_EGG = ITEMS.register("rimefrost_dragon_spawn_egg",
            () -> new DeferredSpawnEggItem(RIMEFROST_DRAGON, 0x14161D, 0x00CBFF, new Item.Properties()));

    /** A phantom's slate-blue spotted with her cyan. */
    public static final DeferredItem<DeferredSpawnEggItem> DREAD_PHANTOM_SPAWN_EGG = ITEMS.register("dread_phantom_spawn_egg",
            () -> new DeferredSpawnEggItem(DREAD_PHANTOM, 0x2B3247, 0x00CBFF, new Item.Properties()));
    public static final DeferredItem<DeferredSpawnEggItem> MINIATURE_DREAD_PHANTOM_SPAWN_EGG = ITEMS.register("miniature_dread_phantom_spawn_egg",
            () -> new DeferredSpawnEggItem(MINIATURE_DREAD_PHANTOM, 0x3E465C, 0x9CF0FF, new Item.Properties()));

    /**
     * Syndrigossa's egg: only an ancient one (the boss) leaves it, from her corpse where a dragon's
     * heart would be. Laid on dreadstone, three full Whiteout Dominion storms over it hatch it (0.39.0).
     */
    public static final DeferredItem<SyndrigossaEggItem> SYNDRIGOSSA_EGG = ITEMS.register("syndrigossa_egg",
            () -> new SyndrigossaEggItem(new Item.Properties().stacksTo(1).rarity(Rarity.EPIC)));

    public static final DeferredHolder<MobEffect, WitheringRimeEffect> WITHERING_RIME = MOB_EFFECTS.register("withering_rime", WitheringRimeEffect::new);

    public static final DeferredHolder<MobEffect, RimeboundEffect> RIMEBOUND = MOB_EFFECTS.register("rimebound", RimeboundEffect::new);

    public static final DeferredHolder<MobEffect, BrittleEffect> BRITTLE = MOB_EFFECTS.register("brittle", BrittleEffect::new);

    public static final DeferredHolder<MobEffect, WhiteoutEffect> WHITEOUT = MOB_EFFECTS.register("whiteout", WhiteoutEffect::new);

    /** The slow without the withering (0.45.0): the dread phantom's bite, and the Dreadlands' storms to come. */
    public static final DeferredHolder<MobEffect, RimefrostEffect> RIMEFROST = MOB_EFFECTS.register("rimefrost", RimefrostEffect::new);

    /**
     * The ghostlier layer under the dread phantom's sounds (0.45.0): each plays alongside vanilla's phantom
     * sound, from vanilla's own ghast, vex and soul sand valley sounds (assets/dreadpatch/sounds.json).
     */
    public static final DeferredHolder<SoundEvent, SoundEvent> DREAD_PHANTOM_WAIL = sound("entity.dread_phantom.wail");
    public static final DeferredHolder<SoundEvent, SoundEvent> DREAD_PHANTOM_HURT = sound("entity.dread_phantom.hurt");
    public static final DeferredHolder<SoundEvent, SoundEvent> DREAD_PHANTOM_DEATH = sound("entity.dread_phantom.death");
    public static final DeferredHolder<SoundEvent, SoundEvent> DREAD_PHANTOM_SWOOP = sound("entity.dread_phantom.swoop");
    /** The Dreadlands' ambience (0.54.0): its loop and whispers a little louder than the soul sand valley's, ice cracking, rumbles below. */
    public static final DeferredHolder<SoundEvent, SoundEvent> DREADLANDS_LOOP = sound("ambient.dreadlands.loop");
    public static final DeferredHolder<SoundEvent, SoundEvent> DREADLANDS_ADDITIONS = sound("ambient.dreadlands.additions");
    public static final DeferredHolder<SoundEvent, SoundEvent> DREADLANDS_ICE_CRACK = sound("ambient.dreadlands.ice_crack");
    public static final DeferredHolder<SoundEvent, SoundEvent> DREADLANDS_RUMBLE = sound("ambient.dreadlands.rumble");
    /** Syndrigossa, far off (0.58.0): Ice and Fire's ice dragon's roars and calls. */
    public static final DeferredHolder<SoundEvent, SoundEvent> DREADLANDS_DISTANT_ROAR = sound("ambient.dreadlands.distant_roar");
    /**
     * The Dreadlands' music (0.60.0): the owner's "Entombed In Rimefrost" (tools/music/make_music.py), the only
     * music played there (the biome's music, kept so by {@code client/DreadlandsMusic}). Its disc plays a mono
     * copy, so a jukebox's fades with distance.
     */
    public static final DeferredHolder<SoundEvent, SoundEvent> DREADLANDS_MUSIC = sound("music.dreadlands");
    public static final DeferredHolder<SoundEvent, SoundEvent> ENTOMBED_IN_RIMEFROST_RECORD = sound("music_disc.entombed_in_rimefrost");
    public static final ResourceKey<JukeboxSong> ENTOMBED_IN_RIMEFROST = ResourceKey.create(Registries.JUKEBOX_SONG, DreadPatch.id("entombed_in_rimefrost"));
    public static final DeferredItem<Item> MUSIC_DISC_ENTOMBED_IN_RIMEFROST = ITEMS.registerSimpleItem("music_disc_entombed_in_rimefrost",
            new Item.Properties().stacksTo(1).rarity(Rarity.RARE).jukeboxPlayable(ENTOMBED_IN_RIMEFROST));

    /** On her sword: its guard has charged the blade, and its next full strike shatters. Saved and sent to clients (the tooltip shows it). */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Unit>> FROST_CHARGE = DATA_COMPONENTS.registerComponentType("frost_charge",
            builder -> builder.persistent(Unit.CODEC).networkSynchronized(StreamCodec.unit(Unit.INSTANCE)));

    public static final DeferredHolder<AbstractSpell, WitheringRimeBoltSpell> WITHERING_RIME_BOLT_SPELL = SPELLS.register("withering_rime_bolt", WitheringRimeBoltSpell::new);
    public static final DeferredHolder<AbstractSpell, RimefireCrownSpell> RIMEFIRE_CROWN_SPELL = SPELLS.register("rimefire_crown", RimefireCrownSpell::new);
    public static final DeferredHolder<AbstractSpell, BarrowStepSpell> BARROW_STEP_SPELL = SPELLS.register("barrow_step", BarrowStepSpell::new);
    public static final DeferredHolder<AbstractSpell, WhiteoutDominionSpell> WHITEOUT_DOMINION_SPELL = SPELLS.register("whiteout_dominion", WhiteoutDominionSpell::new);
    public static final DeferredHolder<AbstractSpell, SummonCourtSpell> SUMMON_COURT_SPELL = SPELLS.register("summon_court", SummonCourtSpell::new);
    public static final DeferredHolder<AbstractSpell, CreateRimefrostDragonSpell> CREATE_RIMEFROST_DRAGON_SPELL = SPELLS.register("create_rimefrost_dragon", CreateRimefrostDragonSpell::new);
    public static final DeferredHolder<AbstractSpell, SummonDreadPhantomsSpell> SUMMON_DREAD_PHANTOMS_SPELL = SPELLS.register("summon_dread_phantoms", SummonDreadPhantomsSpell::new);
    public static final DeferredHolder<AbstractSpell, RimefireBlastSpell> RIMEFIRE_BLAST_SPELL = SPELLS.register("rimefire_blast", RimefireBlastSpell::new);

    private ModRegistries() {
    }

    private static DeferredHolder<SoundEvent, SoundEvent> sound(String name) {
        return SOUND_EVENTS.register(name, () -> SoundEvent.createVariableRangeEvent(DreadPatch.id(name)));
    }

    public static DamageSource damageSource(Level level, ResourceKey<DamageType> type, @Nullable Entity direct, @Nullable Entity causing) {
        return new DamageSource(level.registryAccess().registryOrThrow(Registries.DAMAGE_TYPE).getHolderOrThrow(type), direct, causing);
    }

    public static void register(IEventBus modBus) {
        ENTITY_TYPES.register(modBus);
        BLOCKS.register(modBus);
        ITEMS.register(modBus);
        MOB_EFFECTS.register(modBus);
        SPELLS.register(modBus);
        DATA_COMPONENTS.register(modBus);
        PARTICLE_TYPES.register(modBus);
        SOUND_EVENTS.register(modBus);
        STRUCTURE_TYPES.register(modBus);
        ATTRIBUTES.register(modBus);
        SCHOOLS.register(modBus);
        STRUCTURE_PIECES.register(modBus);
    }
}
