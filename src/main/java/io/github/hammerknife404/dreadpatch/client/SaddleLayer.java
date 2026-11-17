package io.github.hammerknife404.dreadpatch.client;

import com.iafenvoy.iceandfire.entity.DreadQueenEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import io.github.hammerknife404.dreadpatch.entity.Syndrigossa;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.neoforge.client.event.RenderLivingEvent;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.renderer.GeoRenderer;
import software.bernie.geckolib.renderer.layer.GeoRenderLayer;

import java.util.HashSet;
import java.util.Set;

/**
 * A Dread Queen in Syndrigossa's saddle, drawn on the dragon's upper body bone so she moves with it,
 * where Ice and Fire draws a tame dragon's rider. Ice and Fire's own attachment layer draws every
 * other passenger as prey in the jaws, a queen included, and she'd also be drawn at her own position;
 * both are skipped ({@link #onRenderLiving}). Her body faces along the dragon
 * ({@code Syndrigossa.positionRider}); her head turns to what she's looking at.
 */
public final class SaddleLayer extends GeoRenderLayer<Syndrigossa> {
    /** Queens being drawn in a saddle right now: drawn anywhere else while they ride, they're skipped. */
    private static final Set<Entity> DRAWING = new HashSet<>();

    public SaddleLayer(GeoRenderer<Syndrigossa> renderer) {
        super(renderer);
    }

    @Override
    public void renderForBone(PoseStack poseStack, Syndrigossa dragon, GeoBone bone, RenderType renderType, MultiBufferSource bufferSource,
            VertexConsumer buffer, float partialTick, int packedLight, int packedOverlay) {
        if (!bone.getName().equals("BodyUpper")) {
            return;
        }
        float dragonScale = dragon.getRenderSize() / 3.0F;
        for (Entity passenger : dragon.getPassengers()) {
            if (!(passenger instanceof DreadQueenEntity)) {
                continue;
            }
            // Ice and Fire's transform for a rider (rather than prey) on this bone.
            poseStack.pushPose();
            poseStack.translate(0.0F, -0.01F, -0.04F);
            poseStack.mulPose(Axis.ZP.rotationDegrees(180.0F));
            poseStack.mulPose(Axis.YP.rotationDegrees(Mth.lerp(partialTick, passenger.yRotO, passenger.getYRot()) + 180.0F));
            poseStack.scale(1.0F / dragonScale, 1.0F / dragonScale, 1.0F / dragonScale);
            DRAWING.add(passenger);
            try {
                Minecraft.getInstance().getEntityRenderDispatcher().render(passenger, 0.0, 0.0, 0.0, 0.0F, partialTick, poseStack, bufferSource, packedLight);
            } finally {
                DRAWING.remove(passenger);
                poseStack.popPose();
            }
        }
    }

    /** A queen riding Syndrigossa is drawn only by this layer. */
    static void onRenderLiving(RenderLivingEvent.Pre<?, ?> event) {
        LivingEntity entity = event.getEntity();
        if (entity instanceof DreadQueenEntity && entity.getVehicle() instanceof Syndrigossa && !DRAWING.contains(entity)) {
            event.setCanceled(true);
        }
    }
}
