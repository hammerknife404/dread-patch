package io.github.hammerknife404.dreadpatch.client;

import io.github.hammerknife404.dreadpatch.DreadPatch;
import io.github.hammerknife404.dreadpatch.item.DreadQueenStaff;
import io.github.hammerknife404.dreadpatch.magic.PermafrostConduit;
import io.github.hammerknife404.dreadpatch.registry.ModRegistries;
import net.minecraft.ChatFormatting;
import net.minecraft.client.renderer.entity.NoopRenderer;
import net.minecraft.network.chat.Component;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;

/** Client-only entry point: renderers, models and screens are registered from here. */
@Mod(value = DreadPatch.MODID, dist = Dist.CLIENT)
public final class DreadPatchClient {
    public DreadPatchClient(IEventBus modBus, ModContainer container) {
        modBus.addListener(DreadPatchClient::onRegisterLayers);
        modBus.addListener(DreadPatchClient::onRegisterRenderers);
        NeoForge.EVENT_BUS.addListener(DreadPatchClient::onTooltip);
    }

    /** Her staff's Permafrost Conduit, under its name (Iron's adds her spells and the attributes show themselves). */
    private static void onTooltip(ItemTooltipEvent event) {
        if (DreadQueenStaff.is(event.getItemStack())) {
            int at = Math.min(1, event.getToolTip().size());
            event.getToolTip().add(at, Component.translatable("tooltip.dreadpatch.permafrost_conduit").withStyle(ChatFormatting.AQUA));
            event.getToolTip().add(at + 1, Component.literal(" ").append(Component.translatable("tooltip.dreadpatch.permafrost_conduit.desc",
                    Math.round(PermafrostConduit.MANA_DISCOUNT * 100))).withStyle(ChatFormatting.GRAY));
        }
    }

    private static void onRegisterLayers(EntityRenderersEvent.RegisterLayerDefinitions event) {
        event.registerLayerDefinition(DreadQueenModel.LAYER, DreadQueenModel::createBodyLayer);
    }

    private static void onRegisterRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(ModRegistries.DREAD_QUEEN.get(), DreadQueenRenderer::new);
        // Drawn entirely by its trail particles.
        event.registerEntityRenderer(ModRegistries.WITHERING_RIME_BOLT.get(), NoopRenderer::new);
        event.registerEntityRenderer(ModRegistries.RIMEFIRE_CROWN.get(), RimefireCrownRenderer::new);
    }
}
