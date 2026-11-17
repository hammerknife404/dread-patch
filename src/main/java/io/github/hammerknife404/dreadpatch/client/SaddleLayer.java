package io.github.hammerknife404.dreadpatch.client;

import com.iafenvoy.iceandfire.entity.DreadQueenEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import io.github.hammerknife404.dreadpatch.entity.Syndrigossa;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
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
 * A Dread Queen in Syndrigossa's saddle, drawn with the dragon's upper body bone so she moves with it:
 * seated on its top ({@link SaddleSeat}), upright, facing along the dragon. Ice and Fire's own
 * attachment layer draws every passenger but a tame dragon's owner as prey in the jaws, a queen
 * included, and she'd also be drawn at her own position; both are skipped ({@link #onRenderLiving}).
 * (0.24.0 used Ice and Fire's rider transform, a near-zero offset and a half turn about Z, which with
 * GeckoLib handing layers the pose at the model's origin hung her upside down under the dragon.) Her
 * renderer turns her body by the dragon's body yaw, as it does for any rider, and that's undone here,
 * so she faces where the bone does; her head turns to what she's looking at.
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
            poseStack.pushPose();
            poseStack.translate(SaddleSeat.X, SaddleSeat.Y, SaddleSeat.Z);
            poseStack.mulPose(Axis.YP.rotationDegrees(Mth.rotLerp(partialTick, dragon.yBodyRotO, dragon.yBodyRot) - 180.0F));
            poseStack.scale(1.0F / dragonScale, 1.0F / dragonScale, 1.0F / dragonScale);
            poseStack.translate(0.0F, -SaddleSeat.HIPS, 0.0F);
            EntityRenderDispatcher dispatcher = Minecraft.getInstance().getEntityRenderDispatcher();
            DRAWING.add(passenger);
            // Her shadow would fall where her entity is, not under her here.
            dispatcher.setRenderShadow(false);
            try {
                dispatcher.render(passenger, 0.0, 0.0, 0.0, 0.0F, partialTick, poseStack, bufferSource, packedLight);
            } finally {
                dispatcher.setRenderShadow(Minecraft.getInstance().options.entityShadows().get());
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
