package io.github.hammerknife404.dreadpatch.client;

import io.github.hammerknife404.dreadpatch.DreadPatch;
import com.iafenvoy.iceandfire.registry.IafItems;
import io.github.hammerknife404.dreadpatch.item.DreadQueenStaff;
import io.github.hammerknife404.dreadpatch.item.DreadQueenSword;
import io.github.hammerknife404.dreadpatch.magic.GateShape;
import io.github.hammerknife404.dreadpatch.magic.PermafrostConduit;
import io.github.hammerknife404.dreadpatch.registry.DreadForge;
import io.github.hammerknife404.dreadpatch.registry.ModRegistries;
import net.minecraft.ChatFormatting;
import net.minecraft.client.renderer.entity.NoopRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.server.packs.resources.ResourceManagerReloadListener;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.gui.ConfigurationScreen;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;
import net.neoforged.neoforge.client.event.RegisterClientReloadListenersEvent;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;
import net.neoforged.neoforge.client.event.RegisterParticleProvidersEvent;
import net.neoforged.neoforge.client.event.RenderLivingEvent;
import net.neoforged.neoforge.client.extensions.common.RegisterClientExtensionsEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;

import java.util.List;

/** Client-only entry point: renderers, models and screens are registered from here. */
@Mod(value = DreadPatch.MODID, dist = Dist.CLIENT)
public final class DreadPatchClient {
    public DreadPatchClient(IEventBus modBus, ModContainer container) {
        modBus.addListener(DreadPatchClient::onRegisterLayers);
        modBus.addListener(DreadPatchClient::onRegisterRenderers);
        modBus.addListener(DreadPatchClient::onClientExtensions);
        modBus.addListener(DreadPatchClient::onReloadListeners);
        modBus.addListener(DreadPatchClient::onParticles);
        modBus.addListener(DreadPatchClient::onMenuScreens);
        modBus.addListener(DreadRecolors::onRegisterSpriteSources);
        modBus.addListener(DreadRecolors::onClientExtensions);
        modBus.addListener(QueenArmorModels::onRegisterLayers);
        modBus.addListener(QueenArmorModels::onClientExtensions);
        RegaliaWeapons.register(modBus);
        modBus.addListener(EntityRenderersEvent.AddLayers.class, RegaliaWingsLayer::onAddLayers);
        NeoForge.EVENT_BUS.addListener((net.neoforged.neoforge.client.event.ClientTickEvent.Post event) -> RegaliaWingsLayer.tickFrost(net.minecraft.client.Minecraft.getInstance()));
        modBus.addListener(EntityRenderersEvent.AddLayers.class, SaddleLayer::onAddLayers);
        modBus.addListener(EntityRenderersEvent.AddLayers.class, ThrallArmorLayer::onAddLayers);
        NeoForge.EVENT_BUS.addListener(DreadPatchClient::onTooltip);
        NeoForge.EVENT_BUS.addListener(RenderLivingEvent.Pre.class, SaddleLayer::onRenderLiving);
        WhiteoutClient.register(NeoForge.EVENT_BUS);
        StaffTooltip.register(NeoForge.EVENT_BUS);
        // The server config in the mods list's Config button (0.50.0): NeoForge's own screen; every value is read
        // where it's used, so a change applies at once.
        container.registerExtensionPoint(IConfigScreenFactory.class, ConfigurationScreen::new);
        DreadlandsClient.register(NeoForge.EVENT_BUS);
        DreadlandsAmbience.register(NeoForge.EVENT_BUS);
        DreadlandsMusicClient.register(NeoForge.EVENT_BUS);
        BossMusicClient.register(NeoForge.EVENT_BUS);
        DreadKnightCrouch.register(NeoForge.EVENT_BUS);
        DefeatStormClient.register(NeoForge.EVENT_BUS);
        RisenDreadClient.register(NeoForge.EVENT_BUS);
        SoundFilters.register(NeoForge.EVENT_BUS);
        DreadlandsSky.register(modBus, NeoForge.EVENT_BUS);
    }

    /**
     * Under the name: her staff's Permafrost Conduit (Iron's adds her spells), her sword's three
     * properties and its charge, the Gravemaster's Key's uses, and what Syndrigossa is (her spawn eggs,
     * her egg). The attributes show themselves.
     */
    private static void onTooltip(ItemTooltipEvent event) {
        List<Component> tooltip = event.getToolTip();
        int at = Math.min(1, tooltip.size());
        if (DreadQueenStaff.is(event.getItemStack())) {
            tooltip.add(at, title("permafrost_conduit"));
            tooltip.add(at + 1, line("tooltip.dreadpatch.permafrost_conduit.desc", Math.round(PermafrostConduit.MANA_DISCOUNT * 100)));
        } else if (DreadQueenSword.is(event.getItemStack())) {
            tooltip.addAll(at, List.of(
                    title("thermodynamic_theft"),
                    line("tooltip.dreadpatch.thermodynamic_theft.desc", DreadQueenSword.THEFT_TICKS / 20.0F),
                    title("glass_rime_shatter"),
                    line("tooltip.dreadpatch.glass_rime_shatter.desc"),
                    line("tooltip.dreadpatch.glass_rime_shatter.desc2", Math.round(DreadQueenSword.SHATTER_SHARE * 100)),
                    title("fire_drinking_guard"),
                    line("tooltip.dreadpatch.fire_drinking_guard.desc"),
                    line("tooltip.dreadpatch.fire_drinking_guard.desc2")));
            if (DreadQueenSword.charged(event.getItemStack())) {
                tooltip.add(at, Component.translatable("tooltip.dreadpatch.frost_charged").withStyle(ChatFormatting.DARK_AQUA));
            }
        } else if (event.getItemStack().is(IafItems.DREAD_KEY.get())) {
            // Ice and Fire's key says nothing of what it opens: a Mausoleum's door, and now a Dread Gate.
            tooltip.addAll(at, List.of(
                    line("tooltip.dreadpatch.gravemasters_key.lock"),
                    line("tooltip.dreadpatch.gravemasters_key.gate", GateShape.MIN_WIDTH, GateShape.MAX_WIDTH, GateShape.MIN_HEIGHT, GateShape.MAX_HEIGHT),
                    line("tooltip.dreadpatch.gravemasters_key.gate2"),
                    line("tooltip.dreadpatch.gravemasters_key.spent")));
        } else if (event.getItemStack().is(ModRegistries.SYNDRIGOSSA_SPAWN_EGG.get())) {
            // What she is, in the owner's taxonomy: a unique, named dragon of the Rimefrost frost dragons.
            tooltip.add(at, line("tooltip.dreadpatch.syndrigossa"));
        } else if (event.getItemStack().is(ModRegistries.RIMEFROST_DRAGON_SPAWN_EGG.get())) {
            tooltip.add(at, line("tooltip.dreadpatch.rimefrost_dragon"));
        } else if (event.getItemStack().is(DreadForge.CORE_ITEM.get()) || event.getItemStack().is(DreadForge.INPUT_ITEM.get())) {
            // Ice and Fire's forges say nothing of how they're built or lit; ours differs from theirs in what lights it.
            tooltip.addAll(at, List.of(
                    line("tooltip.dreadpatch.rimefrost_forge"),
                    line("tooltip.dreadpatch.rimefrost_forge.lit")));
        } else if (event.getItemStack().is(ModRegistries.SYNDRIGOSSA_EGG.get())) {
            tooltip.addAll(at, List.of(
                    line("tooltip.dreadpatch.syndrigossa_egg"),
                    line("tooltip.dreadpatch.syndrigossa_egg.hatch"),
                    line("tooltip.dreadpatch.syndrigossa_egg.hatch2")));
        }
    }

    private static Component title(String key) {
        return Component.translatable("tooltip.dreadpatch." + key).withStyle(ChatFormatting.AQUA);
    }

    private static Component line(String key, Object... args) {
        return Component.literal(" ").append(Component.translatable(key, args)).withStyle(ChatFormatting.GRAY);
    }

    /** Her sword's guard pose. Ice and Fire registers none for it; should another mod, the guard still works, unposed. */
    private static void onClientExtensions(RegisterClientExtensionsEvent event) {
        try {
            event.registerItem(new SwordGuardPose(), IafItems.DREAD_QUEEN_SWORD.get());
        } catch (IllegalStateException e) {
            DreadPatch.LOGGER.warn("Another mod already draws {} in hand; her sword's guard won't show its pose", IafItems.DREAD_QUEEN_SWORD.getId(), e);
        }
    }

    /** The Rimefrost dragonforge's screen (0.59.0). */
    private static void onMenuScreens(RegisterMenuScreensEvent event) {
        event.register(DreadForge.MENU.get(), RimefrostForgeScreen::new);
    }

    private static void onParticles(RegisterParticleProvidersEvent event) {
        event.registerSpriteSet(ModRegistries.RIME_FLAME.get(), RimeBreathParticle::flame);
        event.registerSpriteSet(ModRegistries.RIME_SMOKE.get(), RimeBreathParticle::smoke);
        event.registerSpriteSet(ModRegistries.RIME_FLAME_SMALL.get(), RimeBreathParticle::smallFlame);
        event.registerSpriteSet(ModRegistries.RIME_SMOKE_SMALL.get(), RimeBreathParticle::smallSmoke);
    }

    /** Rimefrost dragons' skins are made from Ice and Fire's textures, the dread phantom's from vanilla's: remake them when textures reload. */
    private static void onReloadListeners(RegisterClientReloadListenersEvent event) {
        event.registerReloadListener((ResourceManagerReloadListener) resources -> {
            UndeadDragonSkins.clear();
            DreadPhantomSkins.clear();
            DreadRecolors.clear();
            QueenArmorModels.clear();
        });
    }

    private static void onRegisterLayers(EntityRenderersEvent.RegisterLayerDefinitions event) {
        event.registerLayerDefinition(DreadQueenModel.LAYER, DreadQueenModel::createBodyLayer);
        event.registerLayerDefinition(RimeEelModel.LAYER, RimeEelModel::createBodyLayer);
    }

    private static void onRegisterRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(ModRegistries.DREAD_QUEEN.get(), DreadQueenRenderer::new);
        // Drawn entirely by its trail particles.
        event.registerEntityRenderer(ModRegistries.WITHERING_RIME_BOLT.get(), NoopRenderer::new);
        event.registerEntityRenderer(ModRegistries.SCUTTLER_WEB.get(), net.minecraft.client.renderer.entity.ThrownItemRenderer::new);
        event.registerEntityRenderer(ModRegistries.RIMEFIRE_CROWN.get(), RimefireCrownRenderer::new);
        event.registerEntityRenderer(ModRegistries.SOVEREIGNS_ECLIPSE.get(), context -> new RimefireCrownRenderer(context, RimefireCrownRenderer.ECLIPSE));
        event.registerEntityRenderer(ModRegistries.BARROW_HUSK.get(), BarrowHuskRenderer::new);
        event.registerEntityRenderer(ModRegistries.SYNDRIGOSSA.get(), SyndrigossaRenderer::new);
        event.registerEntityRenderer(ModRegistries.RIMEFROST_DRAGON.get(), RimefrostDragonRenderer::new);
        event.registerEntityRenderer(ModRegistries.SYNDRIGOSSA_EGG_ENTITY.get(), SyndrigossaEggRenderer::new);
        event.registerEntityRenderer(ModRegistries.SHATTER_SPINE.get(), ShatterSpineRenderer::core);
        event.registerEntityRenderer(ModRegistries.SHATTER_SPINE_SHARD.get(), ShatterSpineRenderer::shard);
        event.registerEntityRenderer(ModRegistries.WHITEOUT_DOMINION.get(), WhiteoutRenderer::new);
        event.registerEntityRenderer(ModRegistries.DREAD_GATE.get(), NoopRenderer::new);
        event.registerEntityRenderer(ModRegistries.DREAD_PHANTOM.get(), DreadPhantomRenderer::new);
        event.registerEntityRenderer(ModRegistries.MINIATURE_DREAD_PHANTOM.get(), DreadPhantomRenderer::new);
        event.registerEntityRenderer(ModRegistries.RIME_EEL.get(), RimeEelRenderer::new);
        // Drawn by its particles, as Iron's draws Cone of Cold.
        event.registerEntityRenderer(ModRegistries.RIMEFIRE_BLAST_PROJECTILE.get(), NoopRenderer::new);
    }
}
