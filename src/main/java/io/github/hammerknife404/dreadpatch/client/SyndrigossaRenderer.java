package io.github.hammerknife404.dreadpatch.client;

import com.iafenvoy.iceandfire.render.entity.DragonBaseEntityRenderer;
import io.github.hammerknife404.dreadpatch.entity.Syndrigossa;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;

/**
 * Syndrigossa: Ice and Fire's own dragon renderer (the ice dragon's GeckoLib model and animations,
 * its eyes and saddle layers), wearing her rotted skin ({@link UndeadDragonSkins}) instead of the
 * white ice dragon's. Dead, her body keeps it (eyes shut) until it's down to the bone, which Ice and
 * Fire draws with its ice dragon skeleton.
 */
public final class SyndrigossaRenderer extends DragonBaseEntityRenderer<Syndrigossa> {
    public SyndrigossaRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public ResourceLocation getTextureLocation(Syndrigossa dragon) {
        if (dragon.isModelDead()) {
            return dragon.isSkeletal() ? super.getTextureLocation(dragon) : UndeadDragonSkins.get(dragon.getDragonStage(), true);
        }
        return UndeadDragonSkins.get(dragon.getDragonStage(), dragon.isSleeping() || dragon.isBlinking());
    }
}
