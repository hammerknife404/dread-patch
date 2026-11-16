package io.github.hammerknife404.dreadpatch;

import com.iafenvoy.iceandfire.entity.DragonBaseEntity;
import com.iafenvoy.iceandfire.entity.DreadQueenEntity;
import com.mojang.logging.LogUtils;
import io.github.hammerknife404.dreadpatch.effect.DreadEvents;
import io.github.hammerknife404.dreadpatch.entity.BarrowHusk;
import io.github.hammerknife404.dreadpatch.entity.DreadQueen;
import io.github.hammerknife404.dreadpatch.entity.ScuttlerBolts;
import io.github.hammerknife404.dreadpatch.entity.SyndrigossaEvents;
import io.github.hammerknife404.dreadpatch.item.DreadQueenStaff;
import io.github.hammerknife404.dreadpatch.item.DreadQueenSword;
import io.github.hammerknife404.dreadpatch.registry.ModRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.CreativeModeTabs;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
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
        modBus.addListener(DreadPatch::onEntityAttributes);
        modBus.addListener(DreadPatch::onCreativeTabs);
        DreadEvents.register(NeoForge.EVENT_BUS);
        DreadQueenStaff.register(modBus, NeoForge.EVENT_BUS);
        DreadQueenSword.register(modBus, NeoForge.EVENT_BUS);
        SyndrigossaEvents.register(NeoForge.EVENT_BUS);
        ScuttlerBolts.register(NeoForge.EVENT_BUS);
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
    }

    private static void onCreativeTabs(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey() == CreativeModeTabs.SPAWN_EGGS) {
            event.accept(ModRegistries.DREAD_QUEEN_SPAWN_EGG.get());
            event.accept(ModRegistries.SYNDRIGOSSA_SPAWN_EGG.get());
        }
    }
}
