package io.github.hammerknife404.dreadpatch.client;

import io.github.hammerknife404.dreadpatch.entity.DreadQueen;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.client.renderer.entity.layers.EyesLayer;
import net.minecraft.resources.ResourceLocation;

/** Her model with Ice and Fire's textures, glowing eyes, and her sword and staff in hand. */
public final class DreadQueenRenderer extends HumanoidMobRenderer<DreadQueen, DreadQueenModel<DreadQueen>> {
    static final ResourceLocation TEXTURE = iceAndFire("textures/entity/dread/dread_queen.png");
    static final RenderType EYES = RenderType.eyes(iceAndFire("textures/entity/dread/dread_queen_eyes.png"));

    public DreadQueenRenderer(EntityRendererProvider.Context context) {
        super(context, new DreadQueenModel<>(context.bakeLayer(DreadQueenModel.LAYER)), 0.5F);
        this.addLayer(new EyesLayer<>(this) {
            @Override
            public RenderType renderType() {
                return EYES;
            }
        });
    }

    @Override
    public ResourceLocation getTextureLocation(DreadQueen entity) {
        return TEXTURE;
    }

    static ResourceLocation iceAndFire(String path) {
        return ResourceLocation.fromNamespaceAndPath("iceandfire", path);
    }
}
