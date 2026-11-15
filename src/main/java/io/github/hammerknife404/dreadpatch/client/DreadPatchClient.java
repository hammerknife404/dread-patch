package io.github.hammerknife404.dreadpatch.client;

import io.github.hammerknife404.dreadpatch.DreadPatch;
import io.github.hammerknife404.dreadpatch.registry.ModRegistries;
import net.minecraft.client.renderer.entity.NoopRenderer;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;

/** Client-only entry point: renderers, models and screens are registered from here. */
@Mod(value = DreadPatch.MODID, dist = Dist.CLIENT)
public final class DreadPatchClient {
    public DreadPatchClient(IEventBus modBus, ModContainer container) {
        modBus.addListener(DreadPatchClient::onRegisterLayers);
        modBus.addListener(DreadPatchClient::onRegisterRenderers);
    }

    private static void onRegisterLayers(EntityRenderersEvent.RegisterLayerDefinitions event) {
        event.registerLayerDefinition(DreadQueenModel.LAYER, DreadQueenModel::createBodyLayer);
    }

    private static void onRegisterRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(ModRegistries.DREAD_QUEEN.get(), DreadQueenRenderer::new);
        // Drawn entirely by its trail particles.
        event.registerEntityRenderer(ModRegistries.WITHERING_RIME_BOLT.get(), NoopRenderer::new);
    }
}
