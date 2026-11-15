package io.github.hammerknife404.dreadpatch.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import io.github.hammerknife404.dreadpatch.entity.BarrowHusk;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FastColor;
import net.minecraft.util.Mth;

/**
 * Barrow Husk: her model and texture as rime-glass, iced blue and half clear, standing still where
 * she stood, her eyes still lit. It fades over its last half second.
 */
public final class BarrowHuskRenderer extends EntityRenderer<BarrowHusk> {
    /** Her colours iced over, and how clear the glass is (alpha, of 255). */
    private static final int ICE_R = 175;
    private static final int ICE_G = 205;
    private static final int ICE_B = 255;
    private static final int ALPHA = 165;
    private static final int FADE_TICKS = 10;

    private final DreadQueenModel<BarrowHusk> model;

    public BarrowHuskRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.model = new DreadQueenModel<>(context.bakeLayer(DreadQueenModel.LAYER));
        this.shadowRadius = 0.4F;
    }

    @Override
    public void render(BarrowHusk husk, float entityYaw, float partialTick, PoseStack pose, MultiBufferSource buffers, int light) {
        float left = BarrowHusk.LIFETIME - (husk.tickCount + partialTick);
        float fade = Mth.clamp(left / FADE_TICKS, 0.0F, 1.0F);
        pose.pushPose();
        // As LivingEntityRenderer places a humanoid: turned to its body's facing, flipped, lifted.
        pose.mulPose(Axis.YP.rotationDegrees(180.0F - husk.getYRot()));
        pose.scale(-1.0F, -1.0F, 1.0F);
        pose.translate(0.0F, -1.501F, 0.0F);
        this.model.setupAnim(husk, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F);
        int glass = FastColor.ARGB32.color(Math.round(ALPHA * fade), ICE_R, ICE_G, ICE_B);
        this.model.renderToBuffer(pose, buffers.getBuffer(RenderType.entityTranslucent(DreadQueenRenderer.TEXTURE)), light, OverlayTexture.NO_OVERLAY, glass);
        int eyes = FastColor.ARGB32.color(Math.round(255 * fade), 255, 255, 255);
        this.model.renderToBuffer(pose, buffers.getBuffer(DreadQueenRenderer.EYES), LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY, eyes);
        pose.popPose();
        super.render(husk, entityYaw, partialTick, pose, buffers, light);
    }

    @Override
    public ResourceLocation getTextureLocation(BarrowHusk husk) {
        return DreadQueenRenderer.TEXTURE;
    }
}
