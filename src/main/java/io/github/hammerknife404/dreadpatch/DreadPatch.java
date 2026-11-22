package io.github.hammerknife404.dreadpatch;

import com.iafenvoy.iceandfire.entity.DragonBaseEntity;
import com.iafenvoy.iceandfire.entity.DreadQueenEntity;
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
import io.github.hammerknife404.dreadpatch.registry.DreadGear;
import io.github.hammerknife404.dreadpatch.registry.ModRegistries;
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
import org.slf4j.Logger;

@Mod(DreadPatch.MODID)
public final class DreadPatch {
    public static final String MODID = "dreadpatch";
    public static final Logger LOGGER = LogUtils.getLogger();

    public DreadPatch(IEventBus modBus, ModContainer container) {
        ModRegistries.register(modBus);
        DreadGear.register(modBus);
        container.registerConfig(ModConfig.Type.SERVER, DreadPatchConfig.SPEC);
        modBus.addListener(DreadPatch::onEntityAttributes);
        modBus.addListener(DreadPatch::onCreativeTabs);
        DreadEvents.register(NeoForge.EVENT_BUS);
        DreadQueenStaff.register(modBus, NeoForge.EVENT_BUS);
        DreadQueenSword.register(modBus, NeoForge.EVENT_BUS);
        SyndrigossaEvents.register(NeoForge.EVENT_BUS);
        ScuttlerBolts.register(NeoForge.EVENT_BUS);
        ScuttlerClimbing.register(NeoForge.EVENT_BUS);
        WhiteoutDominion.register(NeoForge.EVENT_BUS);
        DreadGate.register(NeoForge.EVENT_BUS);
        SummonedCourt.register(NeoForge.EVENT_BUS);
        DragonCorpses.register(NeoForge.EVENT_BUS);
        DragonArmor.register(NeoForge.EVENT_BUS);
        DreadDragonsteel.register(NeoForge.EVENT_BUS);
        QueenRegalia.register(NeoForge.EVENT_BUS);
        PhantomCalling.register(NeoForge.EVENT_BUS);
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
        event.put(ModRegistries.SYNDRIGOSSA.get(), DragonBaseEntity.bakeAttributes().build());
        event.put(ModRegistries.RIMEFROST_DRAGON.get(), DragonBaseEntity.bakeAttributes().build());
        event.put(ModRegistries.DREAD_PHANTOM.get(), DreadPhantom.createAttributes().build());
    }

    private static void onCreativeTabs(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey() == CreativeModeTabs.SPAWN_EGGS) {
            event.accept(ModRegistries.DREAD_QUEEN_SPAWN_EGG.get());
            event.accept(queenWithSyndrigossaEgg());
            event.accept(ModRegistries.SYNDRIGOSSA_SPAWN_EGG.get());
            event.accept(ancientSyndrigossaEgg());
            event.accept(ModRegistries.RIMEFROST_DRAGON_SPAWN_EGG.get());
            event.accept(ModRegistries.DREAD_PHANTOM_SPAWN_EGG.get());
        } else if (event.getTabKey() == CreativeModeTabs.INGREDIENTS) {
            event.accept(ModRegistries.SYNDRIGOSSA_EGG.get());
            event.accept(DreadGear.RIMEFROST_DRAGON_SCALE.get());
            event.accept(DreadGear.RIMEFROST_DRAGON_BLOOD.get());
            event.accept(DreadGear.DREAD_DRAGONSTEEL_INGOT.get());
        } else if (event.getTabKey() == CreativeModeTabs.COMBAT) {
            DreadGear.armor().forEach(item -> event.accept(item.get()));
            DreadGear.queenArmor().forEach(item -> event.accept(item.get()));
            event.accept(DreadGear.DREAD_DRAGONSTEEL_SWORD.get());
            event.accept(DreadGear.DREAD_DRAGONSTEEL_AXE.get());
        } else if (event.getTabKey() == CreativeModeTabs.TOOLS_AND_UTILITIES) {
            event.accept(DreadGear.DREAD_DRAGONSTEEL_PICKAXE.get());
            event.accept(DreadGear.DREAD_DRAGONSTEEL_AXE.get());
            event.accept(DreadGear.DREAD_DRAGONSTEEL_SHOVEL.get());
            event.accept(DreadGear.DREAD_DRAGONSTEEL_HOE.get());
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
