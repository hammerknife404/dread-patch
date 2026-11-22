package io.github.hammerknife404.dreadpatch.client;

import net.minecraft.client.model.PhantomModel;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.PhantomRenderer;
import net.minecraft.client.renderer.entity.layers.EyesLayer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.monster.Phantom;

/**
 * The dread phantom: vanilla's phantom, model and all, in its own skin, torn wings and bone, with the
 * Dread's cyan eyes glowing in place of a phantom's green ({@link DreadPhantomSkins}).
 */
public class DreadPhantomRenderer extends PhantomRenderer {
    public DreadPhantomRenderer(EntityRendererProvider.Context context) {
        super(context);
        // Vanilla's eyes layer, swapped for the Dread's.
        this.layers.clear();
        this.addLayer(new EyesLayer<Phantom, PhantomModel<Phantom>>(this) {
            @Override
            public RenderType renderType() {
                return RenderType.eyes(DreadPhantomSkins.eyes());
            }
        });
    }

    @Override
    public ResourceLocation getTextureLocation(Phantom entity) {
        return DreadPhantomSkins.skin();
    }
}
