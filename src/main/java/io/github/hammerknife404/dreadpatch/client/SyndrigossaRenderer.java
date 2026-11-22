package io.github.hammerknife404.dreadpatch.client;

import io.github.hammerknife404.dreadpatch.entity.Syndrigossa;
import net.minecraft.client.renderer.entity.EntityRendererProvider;

/**
 * Syndrigossa: drawn as every Rimefrost dragon is ({@link RimefrostDragonRenderer}, in white, her
 * colour), and a queen riding her is drawn in the saddle ({@link SaddleLayer}).
 */
public final class SyndrigossaRenderer extends RimefrostDragonRenderer<Syndrigossa> {
    public SyndrigossaRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.addRenderLayer(new SaddleLayer(this));
    }
}
