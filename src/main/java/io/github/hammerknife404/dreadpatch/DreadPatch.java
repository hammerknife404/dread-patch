package io.github.hammerknife404.dreadpatch;

import com.iafenvoy.iceandfire.entity.DragonBaseEntity;
import com.iafenvoy.iceandfire.entity.DreadQueenEntity;
import com.iafenvoy.iceandfire.registry.IafAttributes;
import com.mojang.logging.LogUtils;
import io.github.hammerknife404.dreadpatch.effect.DreadEvents;
import io.github.hammerknife404.dreadpatch.entity.BarrowHusk;
import io.github.hammerknife404.dreadpatch.entity.DragonCorpses;
import io.github.hammerknife404.dreadpatch.entity.DreadGate;
import io.github.hammerknife404.dreadpatch.entity.DreadPhantom;
import io.github.hammerknife404.dreadpatch.entity.PhantomCalling;
import io.github.hammerknife404.dreadpatch.entity.DreadQueen;
import io.github.hammerknife404.dreadpatch.entity.QueenRegalia;
import io.github.hammerknife404.dreadpatch.entity.ScuttlerBolts;
import io.github.hammerknife404.dreadpatch.entity.ScuttlerClimbing;
import io.github.hammerknife404.dreadpatch.entity.Syndrigossa;
import io.github.hammerknife404.dreadpatch.entity.SummonedCourt;
import io.github.hammerknife404.dreadpatch.entity.SyndrigossaEvents;
import io.github.hammerknife404.dreadpatch.entity.WhiteoutDominion;
import io.github.hammerknife404.dreadpatch.item.DragonArmor;
import io.github.hammerknife404.dreadpatch.item.DreadDragonsteel;
import io.github.hammerknife404.dreadpatch.item.DreadQueenStaff;
import io.github.hammerknife404.dreadpatch.item.DreadQueenSword;
import io.github.hammerknife404.dreadpatch.item.DreadRegalia;
import io.github.hammerknife404.dreadpatch.registry.DreadForge;
import io.github.hammerknife404.dreadpatch.registry.DreadGear;
import io.github.hammerknife404.dreadpatch.registry.ModRegistries;
import io.github.hammerknife404.dreadpatch.world.Dreadlands;
import io.github.hammerknife404.dreadpatch.world.DreadlandsStorms;
import io.github.hammerknife404.dreadpatch.world.DreadlandsSpawns;
import io.github.hammerknife404.dreadpatch.world.DreadlandsWhiteout;
import io.github.hammerknife404.dreadpatch.entity.DreadPatrols;
import io.github.hammerknife404.dreadpatch.world.SpireEncounters;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import net.neoforged.neoforge.event.entity.EntityAttributeModificationEvent;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import org.slf4j.Logger;

@Mod(DreadPatch.MODID)
public final class DreadPatch {
    public static final String MODID = "dreadpatch";
    public static final Logger LOGGER = LogUtils.getLogger();

    public DreadPatch(IEventBus modBus, ModContainer container) {
        ModRegistries.register(modBus);
        DreadGear.register(modBus);
        DreadForge.register(modBus);
        io.github.hammerknife404.dreadpatch.registry.Hoard.register(modBus);
        io.github.hammerknife404.dreadpatch.registry.DreadDragonArmor.register(modBus);
        io.github.hammerknife404.dreadpatch.registry.Dreadwood.register(modBus);
        io.github.hammerknife404.dreadpatch.world.DreadTerrain.register(modBus);
        container.registerConfig(ModConfig.Type.SERVER, DreadPatchConfig.SPEC);
        modBus.addListener(DreadPatch::onEntityAttributes);
        modBus.addListener(DreadPatch::onEntityAttributeModification);
        modBus.addListener(DreadPatch::onCreativeTabs);
        DreadEvents.register(NeoForge.EVENT_BUS);
        DreadQueenStaff.register(modBus, NeoForge.EVENT_BUS);
        io.github.hammerknife404.dreadpatch.item.DreadLichStaff.register(modBus);
        DreadQueenSword.register(modBus, NeoForge.EVENT_BUS);
        DreadRegalia.register(modBus, NeoForge.EVENT_BUS);
        SyndrigossaEvents.register(NeoForge.EVENT_BUS);
        ScuttlerBolts.register(NeoForge.EVENT_BUS);
        ScuttlerClimbing.register(NeoForge.EVENT_BUS);
        io.github.hammerknife404.dreadpatch.entity.ScuttlerWebs.register(NeoForge.EVENT_BUS);
        io.github.hammerknife404.dreadpatch.entity.DreadKnights.register(NeoForge.EVENT_BUS);
        WhiteoutDominion.register(NeoForge.EVENT_BUS);
        DreadGate.register(NeoForge.EVENT_BUS);
        SummonedCourt.register(NeoForge.EVENT_BUS);
        DragonCorpses.register(NeoForge.EVENT_BUS);
        DragonArmor.register(NeoForge.EVENT_BUS);
        DreadDragonsteel.register(NeoForge.EVENT_BUS);
        QueenRegalia.register(NeoForge.EVENT_BUS);
        PhantomCalling.register(NeoForge.EVENT_BUS);
        Dreadlands.register(NeoForge.EVENT_BUS);
        SpireEncounters.register(NeoForge.EVENT_BUS);
        DreadlandsStorms.register(NeoForge.EVENT_BUS);
        DreadlandsSpawns.register(NeoForge.EVENT_BUS);
        DreadlandsWhiteout.register(NeoForge.EVENT_BUS);
        io.github.hammerknife404.dreadpatch.command.DreadCommands.register(NeoForge.EVENT_BUS);
        io.github.hammerknife404.dreadpatch.world.DreadFlames.register(NeoForge.EVENT_BUS);
        DreadPatrols.register(NeoForge.EVENT_BUS);
        io.github.hammerknife404.dreadpatch.world.RimefrostNests.register(NeoForge.EVENT_BUS);
        io.github.hammerknife404.dreadpatch.entity.DreadFeeding.register(NeoForge.EVENT_BUS);
        io.github.hammerknife404.dreadpatch.entity.ThrallWeapons.register(NeoForge.EVENT_BUS);
        io.github.hammerknife404.dreadpatch.entity.BeastVoices.register(NeoForge.EVENT_BUS);
        io.github.hammerknife404.dreadpatch.entity.PhantomHits.register(NeoForge.EVENT_BUS);
        DevKit.register(NeoForge.EVENT_BUS);
        io.github.hammerknife404.dreadpatch.world.DreadlandsMusic.register(modBus, NeoForge.EVENT_BUS);
        io.github.hammerknife404.dreadpatch.world.BossMusic.register(modBus, NeoForge.EVENT_BUS);
        io.github.hammerknife404.dreadpatch.world.DreadlandsUndead.register(NeoForge.EVENT_BUS);
        io.github.hammerknife404.dreadpatch.world.DreadwoodSaplings.register(NeoForge.EVENT_BUS);
        io.github.hammerknife404.dreadpatch.world.Escalation.register(modBus, NeoForge.EVENT_BUS);
        io.github.hammerknife404.dreadpatch.entity.RisenDread.register(modBus, NeoForge.EVENT_BUS);
        io.github.hammerknife404.dreadpatch.world.RisenSpawns.register(NeoForge.EVENT_BUS);
        io.github.hammerknife404.dreadpatch.world.DreadCold.register(NeoForge.EVENT_BUS);
    }

    public static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(MODID, path);
    }

    /** Ice and Fire's attributes for her (speed, attack, follow range), with her own health, armor and toughness. */
    private static void onEntityAttributes(EntityAttributeCreationEvent event) {
        event.put(ModRegistries.DREAD_QUEEN.get(), DreadQueenEntity.bakeAttributes()
                .add(Attributes.MAX_HEALTH, DreadQueen.MAX_HEALTH)
                .add(Attributes.ARMOR, DreadQueen.ARMOR)
                .add(Attributes.ARMOR_TOUGHNESS, DreadQueen.ARMOR_TOUGHNESS)
                .build());
        event.put(ModRegistries.BARROW_HUSK.get(), BarrowHusk.createAttributes().build());
        // Ice and Fire adds its dragon forge speed to its own dragons only; ours light the Rimefrost dragonforge (0.59.0).
        event.put(ModRegistries.SYNDRIGOSSA.get(), DragonBaseEntity.bakeAttributes().add(IafAttributes.DRAGON_FORGE_SPEED).build());
        event.put(ModRegistries.RIMEFROST_DRAGON.get(), DragonBaseEntity.bakeAttributes().add(IafAttributes.DRAGON_FORGE_SPEED).build());
        event.put(ModRegistries.DREAD_PHANTOM.get(), DreadPhantom.createAttributes().build());
        event.put(ModRegistries.MINIATURE_DREAD_PHANTOM.get(), io.github.hammerknife404.dreadpatch.entity.MiniatureDreadPhantom.createAttributes().build());
        event.put(ModRegistries.DREAD_FISH.get(), io.github.hammerknife404.dreadpatch.entity.DreadFish.createAttributes().build());
    }

    /** Rimefrost's spell power and resistance on everything living (0.51.0), as Iron's gives its schools'. */
    private static void onEntityAttributeModification(EntityAttributeModificationEvent event) {
        for (EntityType<? extends LivingEntity> type : event.getTypes()) {
            for (var attribute : java.util.List.of(ModRegistries.RIMEFROST_SPELL_POWER, ModRegistries.RIMEFROST_MAGIC_RESIST)) {
                if (!event.has(type, attribute)) {
                    event.add(type, attribute);
                }
            }
        }
    }

    private static void onCreativeTabs(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey() == CreativeModeTabs.SPAWN_EGGS) {
            event.accept(ModRegistries.DREAD_QUEEN_SPAWN_EGG.get());
            event.accept(queenWithSyndrigossaEgg());
            event.accept(ModRegistries.SYNDRIGOSSA_SPAWN_EGG.get());
            event.accept(ancientSyndrigossaEgg());
            event.accept(ModRegistries.RIMEFROST_DRAGON_SPAWN_EGG.get());
            event.accept(ModRegistries.DREAD_PHANTOM_SPAWN_EGG.get());
            event.accept(ModRegistries.MINIATURE_DREAD_PHANTOM_SPAWN_EGG.get());
            event.accept(ModRegistries.DREAD_FISH_SPAWN_EGG.get());
        } else if (event.getTabKey() == CreativeModeTabs.FUNCTIONAL_BLOCKS) {
            event.accept(ModRegistries.DREAD_LANTERN_ITEM.get());
            DreadForge.forgeItems().forEach(item -> event.accept(item.get()));
        } else if (event.getTabKey() == CreativeModeTabs.BUILDING_BLOCKS) {
            event.accept(DreadForge.SCALE_BLOCK_ITEM.get());
            io.github.hammerknife404.dreadpatch.registry.Dreadwood.items().forEach(item -> event.accept(item.get()));
            event.accept(io.github.hammerknife404.dreadpatch.registry.Hoard.IRON_PILE_ITEM.get());
            event.accept(io.github.hammerknife404.dreadpatch.registry.DreadDragonArmor.DREAD_DRAGONSTEEL_BLOCK_ITEM.get());
        } else if (event.getTabKey() == CreativeModeTabs.INGREDIENTS) {
            event.accept(ModRegistries.SYNDRIGOSSA_EGG.get());
            event.accept(DreadGear.RIMEFROST_DRAGON_SCALE.get());
            event.accept(DreadGear.RIMEFROST_DRAGON_BLOOD.get());
            event.accept(DreadGear.DREAD_DRAGONSTEEL_INGOT.get());
        } else if (event.getTabKey() == CreativeModeTabs.COMBAT) {
            DreadGear.armor().forEach(item -> event.accept(item.get()));
            DreadGear.regalia().forEach(item -> event.accept(item.get()));
            event.accept(DreadGear.DREAD_DRAGONSTEEL_SWORD.get());
            event.accept(DreadGear.DREAD_DRAGONSTEEL_AXE.get());
            io.github.hammerknife404.dreadpatch.registry.DreadDragonArmor.ARMOR.forEach(item -> event.accept(item.get()));
        } else if (event.getTabKey() == CreativeModeTabs.TOOLS_AND_UTILITIES) {
            event.accept(DreadGear.DREAD_DRAGONSTEEL_PICKAXE.get());
            event.accept(DreadGear.DREAD_DRAGONSTEEL_AXE.get());
            event.accept(DreadGear.DREAD_DRAGONSTEEL_SHOVEL.get());
            event.accept(DreadGear.DREAD_DRAGONSTEEL_HOE.get());
            event.accept(ModRegistries.MUSIC_DISC_ENTOMBED_IN_RIMEFROST.get());
            event.accept(ModRegistries.MUSIC_DISC_BENEATH_THE_PERMAFROST.get());
        }
    }

    /**
     * The queen's spawn egg carrying {@code WithSyndrigossa:1b}: she rises riding an ancient
     * Syndrigossa, as the boss will, where the plain egg gives her alone.
     */
    private static ItemStack queenWithSyndrigossaEgg() {
        ItemStack egg = new ItemStack(ModRegistries.DREAD_QUEEN_SPAWN_EGG.get());
        CompoundTag data = new CompoundTag();
        data.putString("id", ModRegistries.DREAD_QUEEN.getId().toString());
        data.putBoolean(DreadQueen.WITH_DRAGON_KEY, true);
        egg.set(DataComponents.ENTITY_DATA, CustomData.of(data));
        egg.set(DataComponents.ITEM_NAME, Component.translatable("item.dreadpatch.dread_queen_and_syndrigossa_spawn_egg"));
        return egg;
    }

    /**
     * Her spawn egg carrying {@code Ancient:1b}: an ancient Syndrigossa, the boss at full strength,
     * where the plain egg gives Ice and Fire's random age. Named by {@code item_name}, which (unlike a
     * custom name) isn't passed on to what it spawns.
     */
    private static ItemStack ancientSyndrigossaEgg() {
        ItemStack egg = new ItemStack(ModRegistries.SYNDRIGOSSA_SPAWN_EGG.get());
        CompoundTag data = new CompoundTag();
        data.putString("id", ModRegistries.SYNDRIGOSSA.getId().toString());
        data.putBoolean(Syndrigossa.ANCIENT_KEY, true);
        egg.set(DataComponents.ENTITY_DATA, CustomData.of(data));
        egg.set(DataComponents.ITEM_NAME, Component.translatable("item.dreadpatch.ancient_syndrigossa_spawn_egg"));
        return egg;
    }
}
