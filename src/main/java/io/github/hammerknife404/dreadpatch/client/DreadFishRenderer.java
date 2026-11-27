package io.github.hammerknife404.dreadpatch.client;

import com.mojang.blaze3d.vertex.PoseStack;
import io.github.hammerknife404.dreadpatch.DreadPatch;
import io.github.hammerknife404.dreadpatch.entity.DreadFish;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;

/**
 * The Dread Fish (0.71.0): {@link DreadFishModel} in its own skin, its eyes glowing the Dread's cyan always, and its
 * lure's lantern only while lit ({@link DreadFish#lureLit}: its flicker, off far more often than on, and a flare
 * as it pulses). Its textures are ours, from {@code tools/sprites/make_icons.py}.
 */
public class DreadFishRenderer extends MobRenderer<DreadFish, DreadFishModel> {
    static final ResourceLocation SKIN = DreadPatch.id("textures/entity/dread_fish.png");
    static final ResourceLocation EYES = DreadPatch.id("textures/entity/dread_fish_eyes.png");
    static final ResourceLocation LURE = DreadPatch.id("textures/entity/dread_fish_lure.png");

    public DreadFishRenderer(EntityRendererProvider.Context context) {
        super(context, new DreadFishModel(context.bakeLayer(DreadFishModel.LAYER)), 0.4F);
        this.addLayer(new Glow(this, EYES, false));
        this.addLayer(new Glow(this, LURE, true));
    }

    @Override
    public ResourceLocation getTextureLocation(DreadFish fish) {
        return SKIN;
    }

    /** A glowing part of it, drawn full bright; the lure's only while lit. */
    private static final class Glow extends RenderLayer<DreadFish, DreadFishModel> {
        private final RenderType type;
        private final boolean lure;

        Glow(DreadFishRenderer parent, ResourceLocation texture, boolean lure) {
            super(parent);
            this.type = RenderType.eyes(texture);
            this.lure = lure;
        }

        @Override
        public void render(PoseStack poseStack, MultiBufferSource buffers, int light, DreadFish fish, float limbSwing, float limbSwingAmount,
                           float partialTick, float ageInTicks, float netHeadYaw, float headPitch) {
            if (fish.isInvisible() || this.lure && !fish.lureLit()) {
                return;
            }
            this.getParentModel().renderToBuffer(poseStack, buffers.getBuffer(this.type), 0xF000F0, OverlayTexture.NO_OVERLAY);
        }
    }
}
