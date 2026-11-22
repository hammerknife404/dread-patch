package io.github.hammerknife404.dreadpatch.client;

import com.iafenvoy.iceandfire.entity.DragonBaseEntity;
import com.iafenvoy.iceandfire.entity.DreadQueenEntity;
import com.iafenvoy.iceandfire.render.entity.DragonBaseEntityRenderer;
import com.iafenvoy.iceandfire.render.entity.feature.DragonRiderRenderState;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import io.github.hammerknife404.dreadpatch.entity.Syndrigossa;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RenderLivingEvent;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.renderer.GeoRenderer;
import software.bernie.geckolib.renderer.layer.GeoRenderLayer;

import java.util.HashSet;
import java.util.Set;

/**
 * A dragon's rider in its saddle, drawn with the dragon's upper body bone so they move with it: seated
 * on its top ({@link SaddleSeat}), upright, facing along the dragon. Who: the player steering any Ice
 * and Fire dragon (0.49.0: fire, ice, lightning and the Rimefrost; the owner wanted it for a public
 * release), and a Dread Queen on Syndrigossa (0.24.1).
 * <p>
 * Ice and Fire's own attachment layer ({@code DragonGeoAttachmentLayer}) draws every passenger with a
 * near-zero offset and a half turn about Z; GeckoLib 4.9 hands a layer the pose at the model's origin,
 * turned with the bone, not at the bone, so that hung riders upside down at the dragon's feet. Ice and
 * Fire also hides a player riding a dragon everywhere but its layer (and in first person). So: this
 * layer is added to every dragon renderer ({@link #onAddLayers}); while it draws a rider it lists them
 * in Ice and Fire's {@code DragonRiderRenderState.RENDERING_RIDERS} (so Ice and Fire lets the draw through,
 * still hiding the local player in first person) and in its own {@link #DRAWING}; any other draw of
 * that rider, Ice and Fire's layer's or at their own position, is cancelled ({@link #onRenderLiving}).
 * Prey in the jaws (any other passenger) is left to Ice and Fire.
 * <p>
 * The rider's renderer turns their body by the dragon's body yaw, as it does for any rider, and that's
 * undone here, so they face where the bone does; their head turns to what they're looking at.
 */
public final class SaddleLayer<T extends DragonBaseEntity> extends GeoRenderLayer<T> {
    /** Riders being drawn in a saddle right now: drawn anywhere else while they ride, they're skipped. */
    private static final Set<Entity> DRAWING = new HashSet<>();

    public SaddleLayer(GeoRenderer<T> renderer) {
        super(renderer);
    }

    /** Whether {@code rider} is drawn in {@code dragon}'s saddle: the player steering it, or a queen on Syndrigossa. */
    static boolean seated(Entity rider, Entity dragon) {
        if (!(dragon instanceof DragonBaseEntity mount) || rider.getVehicle() != dragon) {
            return false;
        }
        if (rider instanceof DreadQueenEntity) {
            return dragon instanceof Syndrigossa;
        }
        return rider instanceof Player && mount.getControllingPassenger() == rider;
    }

    @Override
    public void renderForBone(PoseStack poseStack, T dragon, GeoBone bone, RenderType renderType, MultiBufferSource bufferSource,
            VertexConsumer buffer, float partialTick, int packedLight, int packedOverlay) {
        if (!bone.getName().equals("BodyUpper")) {
            return;
        }
        float dragonScale = dragon.getRenderSize() / 3.0F;
        for (Entity passenger : dragon.getPassengers()) {
            if (!seated(passenger, dragon)) {
                continue;
            }
            poseStack.pushPose();
            poseStack.translate(SaddleSeat.X, SaddleSeat.Y, SaddleSeat.Z);
            poseStack.mulPose(Axis.YP.rotationDegrees(Mth.rotLerp(partialTick, dragon.yBodyRotO, dragon.yBodyRot) - 180.0F));
            poseStack.scale(1.0F / dragonScale, 1.0F / dragonScale, 1.0F / dragonScale);
            poseStack.translate(0.0F, -SaddleSeat.HIPS, 0.0F);
            EntityRenderDispatcher dispatcher = Minecraft.getInstance().getEntityRenderDispatcher();
            DRAWING.add(passenger);
            DragonRiderRenderState.RENDERING_RIDERS.add(passenger);
            // Their shadow would fall where their entity is, not under them here.
            dispatcher.setRenderShadow(false);
            try {
                dispatcher.render(passenger, 0.0, 0.0, 0.0, 0.0F, partialTick, poseStack, bufferSource, packedLight);
            } finally {
                dispatcher.setRenderShadow(Minecraft.getInstance().options.entityShadows().get());
                DragonRiderRenderState.RENDERING_RIDERS.remove(passenger);
                DRAWING.remove(passenger);
                poseStack.popPose();
            }
        }
    }

    /** A seated rider is drawn only by this layer. */
    static void onRenderLiving(RenderLivingEvent.Pre<?, ?> event) {
        LivingEntity entity = event.getEntity();
        if (entity.getVehicle() != null && seated(entity, entity.getVehicle()) && !DRAWING.contains(entity)) {
            event.setCanceled(true);
        }
    }

    /** Onto every dragon renderer (Ice and Fire's and ours), each time the renderers are built. */
    static void onAddLayers(EntityRenderersEvent.AddLayers event) {
        for (EntityType<?> type : event.getEntityTypes()) {
            EntityRenderer<?> renderer = event.getRenderer(type);
            if (renderer instanceof DragonBaseEntityRenderer<?> dragon) {
                add(dragon);
            }
        }
    }

    private static <T extends DragonBaseEntity> void add(DragonBaseEntityRenderer<T> renderer) {
        renderer.addRenderLayer(new SaddleLayer<>(renderer));
    }
}
