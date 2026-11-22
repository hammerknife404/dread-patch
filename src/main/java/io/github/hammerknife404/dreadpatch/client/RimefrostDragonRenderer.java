package io.github.hammerknife404.dreadpatch.client;

import com.iafenvoy.iceandfire.render.entity.DragonBaseEntityRenderer;
import io.github.hammerknife404.dreadpatch.entity.RimefrostDragon;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;

/**
 * A Rimefrost dragon: Ice and Fire's own dragon renderer (the ice dragon's GeckoLib model and
 * animations, its eyes and saddle layers), wearing its rotted skin ({@link UndeadDragonSkins}) made
 * from its own ice dragon colour instead of that colour's. Dead, its body keeps it (eyes shut) until
 * it's down to the bone, which Ice and Fire draws with its ice dragon skeleton.
 */
public class RimefrostDragonRenderer<T extends RimefrostDragon> extends DragonBaseEntityRenderer<T> {
    public RimefrostDragonRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public ResourceLocation getTextureLocation(T dragon) {
        if (dragon.isModelDead()) {
            return dragon.isSkeletal() ? super.getTextureLocation(dragon) : UndeadDragonSkins.get(dragon.getVariant(), dragon.getDragonStage(), true);
        }
        return UndeadDragonSkins.get(dragon.getVariant(), dragon.getDragonStage(), dragon.isSleeping() || dragon.isBlinking());
    }
}
