package io.github.hammerknife404.dreadpatch.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.logging.LogUtils;
import io.github.hammerknife404.dreadpatch.DreadPatch;
import io.github.hammerknife404.dreadpatch.registry.DreadGear;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.CapeLayer;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.fml.util.ObfuscationReflectionHelper;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import org.slf4j.Logger;

import java.util.List;

/**
 * Draws the winged bodice's wings (0.82.0) on whoever wears it (players, armour stands, any humanoid mob), off the
 * wearer's body: the bones and the half see-through membrane translucent, then their frost glints lit full
 * ({@code regalia_wings_glow.png}). Vanilla draws a cape over them (its cape layer only steps aside for its own elytra),
 * so a player's cape layer is swapped for one that steps aside for the wings too (no mixin: the renderer's layer list,
 * as {@link ThrallArmorLayer} does for Ice and Fire's thralls). Its elytra layer draws only vanilla's elytra already.
 */
public final class RegaliaWingsLayer<T extends LivingEntity, M extends HumanoidModel<T>> extends RenderLayer<T, M> {
    private static final Logger LOGGER = LogUtils.getLogger();
    static final ResourceLocation TEXTURE = DreadPatch.id("textures/entity/regalia_wings.png");
    static final ResourceLocation GLOW = DreadPatch.id("textures/entity/regalia_wings_glow.png");

    private static final ResourceLocation ELYTRA = ResourceLocation.withDefaultNamespace("textures/entity/elytra.png");

    private final RegaliaWingsModel wings;
    private final IdleElytra<T> elytra;

    public RegaliaWingsLayer(RenderLayerParent<T, M> parent, RegaliaWingsModel wings, IdleElytra<T> elytra) {
        super(parent);
        this.wings = wings;
        this.elytra = elytra;
    }

    /** Whether it wears the winged bodice. */
    public static boolean winged(LivingEntity entity) {
        return entity.getItemBySlot(EquipmentSlot.CHEST).is(DreadGear.DREAD_QUEEN_WINGED_BODICE.get());
    }

    @Override
    public void render(PoseStack poseStack, MultiBufferSource buffers, int light, T entity, float limbSwing, float limbSwingAmount,
                       float partialTick, float ageInTicks, float netHeadYaw, float headPitch) {
        if (!winged(entity) || entity.isInvisible()) {
            return;
        }
        if (DreadPatchClientConfig.wings() == DreadPatchClientConfig.Wings.ELYTRA) {
            // A vanilla elytra's wings (0.98.7, the client config), as vanilla's ElytraLayer draws them.
            poseStack.pushPose();
            poseStack.translate(0.0F, 0.0F, 0.125F);
            this.getParentModel().copyPropertiesTo(this.elytra);
            this.elytra.setupAnim(entity, limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch);
            this.elytra.idle(entity, ageInTicks);
            this.elytra.renderToBuffer(poseStack, net.minecraft.client.renderer.entity.ItemRenderer.getArmorFoilBuffer(buffers,
                    RenderType.armorCutoutNoCull(ELYTRA), entity.getItemBySlot(EquipmentSlot.CHEST).hasFoil()), light, OverlayTexture.NO_OVERLAY);
            poseStack.popPose();
            return;
        }
        poseStack.pushPose();
        this.getParentModel().body.translateAndRotate(poseStack);
        this.wings.setupAnim(entity, limbSwing, limbSwingAmount, ageInTicks);
        this.wings.renderToBuffer(poseStack, buffers.getBuffer(RenderType.entityTranslucent(TEXTURE)), light,
                LivingEntityRenderer.getOverlayCoords(entity, 0.0F), -1);
        this.wings.renderToBuffer(poseStack, buffers.getBuffer(RenderType.eyes(GLOW)), 0xF000F0, OverlayTexture.NO_OVERLAY, -1);
        poseStack.popPose();
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    static void onAddLayers(EntityRenderersEvent.AddLayers event) {
        RegaliaWingsModel model = new RegaliaWingsModel(event.getEntityModels().bakeLayer(RegaliaWingsModel.LAYER));
        net.minecraft.client.model.geom.ModelPart elytra = event.getEntityModels().bakeLayer(net.minecraft.client.model.geom.ModelLayers.ELYTRA);
        for (var skin : event.getSkins()) {
            if (event.getSkin(skin) instanceof LivingEntityRenderer renderer && renderer.getModel() instanceof PlayerModel) {
                renderer.addLayer(new RegaliaWingsLayer(renderer, model, new IdleElytra(elytra)));
                hideCape(renderer);
            }
        }
        for (EntityType<?> type : event.getEntityTypes()) {
            EntityRenderer<?> found = event.getRenderer(type);
            if (found instanceof LivingEntityRenderer renderer && renderer.getModel() instanceof HumanoidModel) {
                renderer.addLayer(new RegaliaWingsLayer(renderer, model, new IdleElytra(elytra)));
            }
        }
    }

    /** The player's cape layer, swapped for one that steps aside for the wings. */
    @SuppressWarnings({"unchecked", "rawtypes"})
    private static void hideCape(LivingEntityRenderer renderer) {
        try {
            List<RenderLayer> layers = ObfuscationReflectionHelper.getPrivateValue(LivingEntityRenderer.class, renderer, "layers");
            for (int i = 0; i < layers.size(); i++) {
                if (layers.get(i) instanceof CapeLayer && !(layers.get(i) instanceof WingedCapeLayer)) {
                    layers.set(i, new WingedCapeLayer(renderer));
                    return;
                }
            }
        } catch (RuntimeException e) {
            LOGGER.warn("Dread Patch: a cape still shows under the winged bodice's wings: {}", e.toString());
        }
    }

    /** Vanilla's cape, but not under the wings. */
    static final class WingedCapeLayer extends CapeLayer {
        WingedCapeLayer(RenderLayerParent<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> parent) {
            super(parent);
        }

        @Override
        public void render(PoseStack poseStack, MultiBufferSource buffers, int light, AbstractClientPlayer player, float limbSwing,
                           float limbSwingAmount, float partialTick, float ageInTicks, float netHeadYaw, float headPitch) {
            if (!winged(player)) {
                super.render(poseStack, buffers, light, player, limbSwing, limbSwingAmount, partialTick, ageInTicks, netHeadYaw, headPitch);
            }
        }
    }

    /** Frost shed off the wings: a trail while gliding, now and then a flake at rest. On each client tick. */
    static void tickFrost(Minecraft minecraft) {
        if (minecraft.level == null || minecraft.isPaused()) {
            return;
        }
        var random = minecraft.level.getRandom();
        for (var player : minecraft.level.players()) {
            if (!winged(player) || player.isInvisible() || player.distanceToSqr(minecraft.gameRenderer.getMainCamera().getPosition()) > 64 * 64) {
                continue;
            }
            float yaw = player.yBodyRot * net.minecraft.util.Mth.DEG_TO_RAD;
            double rx = Math.cos(yaw), rz = Math.sin(yaw);
            double speed = player.getDeltaMovement().length();
            // The trail only going fast (0.82.5, with the wings' sweep: RegaliaWingsModel.FAST), more the faster; a few
            // flakes off them gliding slower.
            if (player.isFallFlying() && (speed > RegaliaWingsModel.FAST * 0.85 || random.nextInt(6) == 0)) {
                for (int side = -1; side <= 1; side += 2) {
                    // Off the wingtips: out to either side of the body, a little behind.
                    double x = player.getX() + rx * 1.7 * side, z = player.getZ() + rz * 1.7 * side;
                    minecraft.level.addParticle(random.nextInt(3) == 0 ? net.minecraft.core.particles.ParticleTypes.SNOWFLAKE
                                    : io.github.hammerknife404.dreadpatch.registry.ModRegistries.RIME_FLAME_SMALL.get(),
                            x, player.getY() + 0.4, z, -player.getDeltaMovement().x * 0.2, 0.0, -player.getDeltaMovement().z * 0.2);
                }
            } else if (random.nextInt(25) == 0) {
                int side = random.nextBoolean() ? 1 : -1;
                minecraft.level.addParticle(net.minecraft.core.particles.ParticleTypes.SNOWFLAKE,
                        player.getX() + rx * 0.9 * side - Math.sin(yaw) * 0.3, player.getY() + 1.2 + random.nextDouble() * 0.6,
                        player.getZ() + rz * 0.9 * side + Math.cos(yaw) * 0.3, 0.0, -0.02, 0.0);
            }
        }
    }

    /**
     * A vanilla elytra's wings (0.98.7) with the same small idle the dragon wings have: not gliding, a slow sway and now
     * and then a resettle ({@link RegaliaWingsModel#idleSettle}), on the wings vanilla poses.
     */
    static final class IdleElytra<T extends LivingEntity> extends net.minecraft.client.model.ElytraModel<T> {
        IdleElytra(net.minecraft.client.model.geom.ModelPart root) {
            super(root);
        }

        void idle(T entity, float time) {
            if (entity.isFallFlying()) {
                return;
            }
            float sway = 0.04F * net.minecraft.util.Mth.sin(time * 0.1F) + 0.12F * RegaliaWingsModel.idleSettle(time);
            int i = 0;
            for (net.minecraft.client.model.geom.ModelPart wing : this.bodyParts()) {
                // Vanilla's order: the left wing, then the right; each opens outward from the back.
                float side = i++ == 0 ? 1.0F : -1.0F;
                wing.zRot += sway * side;
                wing.yRot += 0.5F * sway * side;
            }
        }
    }
}
