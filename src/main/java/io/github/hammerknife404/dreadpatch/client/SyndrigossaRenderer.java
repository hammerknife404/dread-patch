package io.github.hammerknife404.dreadpatch.client;

import io.github.hammerknife404.dreadpatch.entity.Syndrigossa;
import net.minecraft.client.renderer.entity.EntityRendererProvider;

/**
 * Syndrigossa: drawn as every Rimefrost dragon is ({@link RimefrostDragonRenderer}, in white, her
 * colour). A queen riding her is drawn in the saddle by the {@link SaddleLayer} every dragon renderer
 * gets ({@link SaddleLayer#onAddLayers}).
 */
public final class SyndrigossaRenderer extends RimefrostDragonRenderer<Syndrigossa> {
    public SyndrigossaRenderer(EntityRendererProvider.Context context) {
        super(context);
    }
}
