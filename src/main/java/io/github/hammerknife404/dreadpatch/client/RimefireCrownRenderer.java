package io.github.hammerknife404.dreadpatch.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import io.github.hammerknife404.dreadpatch.DreadPatch;
import io.github.hammerknife404.dreadpatch.entity.RimefireCrown;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.inventory.InventoryMenu;

import java.util.function.Function;

/**
 * The crown: clusters of ice spikes round the ring, leaning outward, each its own height, with dread
 * fire burning between and before them. The spikes ({@link IceSpike}) are drawn from vanilla's packed
 * and blue ice textures, lit cyan at the base by the fire; the fire is our own
 * animated texture ({@code tools/sprites/make_icons.py}) on crossed planes, full-bright, flickering.
 * The spikes shoot up over its first ticks and sink over its last; the fire flares and gutters with
 * them. The cyan and black smoke on the ground is particles, from the entity. Sovereign's Eclipse is
 * drawn the same way in its own {@link Style}, round its own radius.
 */
public class RimefireCrownRenderer extends EntityRenderer<RimefireCrown> {
    /**
     * How a ring is drawn: how many spike clusters and flames round it, how big each is, the spikes'
     * tint (multiplying the ice's own colour) and the flames' (ARGB, multiplying the dread fire).
     */
    public record Style(int clusters, int flames, float spikeScale, float flameScale, float spikeR, float spikeG, float spikeB, int flameColor) {
    }

    public static final Style CROWN = new Style(9, 18, 1.0F, 1.0F, 1.0F, 1.0F, 1.0F, -1);
    /** Sovereign's Eclipse: nearly three times the spires round its wider ring, half again as tall, dark as black glass; its fire indigo. */
    public static final Style ECLIPSE = new Style(24, 44, 1.6F, 1.4F, 0.30F, 0.32F, 0.48F, 0xFF8070FF);

    private final Style style;
    private static final ResourceLocation PACKED_ICE = ResourceLocation.withDefaultNamespace("block/packed_ice");
    private static final ResourceLocation BLUE_ICE = ResourceLocation.withDefaultNamespace("block/blue_ice");
    private static final ResourceLocation[] DREAD_FIRE = {DreadPatch.id("block/dread_fire_0"), DreadPatch.id("block/dread_fire_1")};

    public RimefireCrownRenderer(EntityRendererProvider.Context context) {
        this(context, CROWN);
    }

    public RimefireCrownRenderer(EntityRendererProvider.Context context, Style style) {
        super(context);
        this.style = style;
        this.shadowRadius = 0.0F;
    }

    @Override
    public void render(RimefireCrown crown, float yaw, float partialTick, PoseStack pose, MultiBufferSource buffers, int light) {
        float age = crown.tickCount + partialTick;
        float grow = Math.min(Mth.clamp(age / 6.0F, 0.0F, 1.0F), Mth.clamp((crown.getLifetime() - age) / 4.0F, 0.0F, 1.0F));
        if (grow > 0.0F) {
            Function<ResourceLocation, TextureAtlasSprite> atlas = Minecraft.getInstance().getTextureAtlas(InventoryMenu.BLOCK_ATLAS);
            // Both the ice and the fire: block-atlas sprites, cut out, seen from both sides.
            VertexConsumer out = buffers.getBuffer(RenderType.entityCutoutNoCull(InventoryMenu.BLOCK_ATLAS));
            // The ice is lit by its own fire, however dark it is round it.
            int fireLit = LightTexture.pack(Math.max(LightTexture.block(light), 13), LightTexture.sky(light));
            float radius = crown.radius();
            for (int i = 0; i < this.style.clusters(); i++) {
                this.renderCluster(i, radius, grow, pose, out, atlas.apply(i % 3 == 1 ? BLUE_ICE : PACKED_ICE), atlas.apply(PACKED_ICE), fireLit);
            }
            for (int k = 0; k < this.style.flames(); k++) {
                this.renderFlame(k, radius, age, grow, pose, out, atlas.apply(DREAD_FIRE[k % 2]));
            }
        }
        super.render(crown, yaw, partialTick, pose, buffers, light);
    }

    /** One tall spike leaning outward from the ring, with a smaller one or two splayed off its sides. */
    private void renderCluster(int i, float radius, float grow, PoseStack pose, VertexConsumer out, TextureAtlasSprite ice, TextureAtlasSprite sideIce, int light) {
        float angle = Mth.TWO_PI * i / this.style.clusters() + (i % 2) * 0.17F;
        float height = (1.7F + 0.9F * ((i * 37) % 10) / 10.0F) * grow * this.style.spikeScale();
        float half = (0.26F + 0.05F * (i % 3)) * this.style.spikeScale();
        float r = this.style.spikeR();
        float g = this.style.spikeG();
        float b = this.style.spikeB();
        float lean = 8.0F + (i * 13) % 11;
        pose.pushPose();
        // Local +x points out from the centre; the spikes stand a little into the ground.
        pose.translate(Mth.cos(angle) * radius, -0.25F, Mth.sin(angle) * radius);
        pose.mulPose(Axis.YP.rotation(-angle));
        pose.mulPose(Axis.ZP.rotationDegrees(-lean));
        pose.pushPose();
        pose.mulPose(Axis.YP.rotationDegrees(i * 47.0F));
        IceSpike.draw(pose.last(), out, ice.getU0(), ice.getU1(), ice.getV0(), ice.getV1(), height, half, light, r, g, b);
        pose.popPose();
        for (int side = -1; side <= 1; side += 2) {
            if (side == 1 && i % 2 == 0) {
                continue;
            }
            pose.pushPose();
            pose.translate(0.05F, 0.0F, side * (half + 0.1F));
            pose.mulPose(Axis.XP.rotationDegrees(side * (16.0F + (i * 7) % 9)));
            pose.mulPose(Axis.ZP.rotationDegrees(-10.0F));
            IceSpike.draw(pose.last(), out, sideIce.getU0(), sideIce.getU1(), sideIce.getV0(), sideIce.getV1(), height * (0.45F + 0.04F * (i % 4)), half * 0.6F, light, r, g, b);
            pose.popPose();
        }
        pose.popPose();
    }

    /** A flame on the ring, between or before the spikes: two crossed planes, each flickering on its own beat. */
    private void renderFlame(int k, float ringRadius, float age, float grow, PoseStack pose, VertexConsumer out, TextureAtlasSprite fire) {
        float angle = Mth.TWO_PI * (k + 0.5F) / this.style.flames();
        float radius = ringRadius + ((k * 5) % 7 - 3) * 0.08F;
        float flicker = 0.5F + 0.5F * Mth.sin(age * 0.35F + k * 1.7F);
        float height = (0.8F + 0.3F * ((k * 11) % 5) / 4.0F + 0.25F * flicker) * grow * this.style.flameScale();
        float width = 0.9F * Math.min(1.0F, grow * 1.5F) * this.style.flameScale();
        int color = this.style.flameColor();
        // Every other flame mirrored, so neighbours don't match.
        float u0 = k % 4 < 2 ? fire.getU0() : fire.getU1();
        float u1 = k % 4 < 2 ? fire.getU1() : fire.getU0();
        pose.pushPose();
        pose.translate(Mth.cos(angle) * radius, -0.05F, Mth.sin(angle) * radius);
        pose.mulPose(Axis.YP.rotationDegrees(k * 29.0F));
        for (int plane = 0; plane < 2; plane++) {
            PoseStack.Pose last = pose.last();
            flameVertex(last, out, -width / 2, 0.0F, u0, fire.getV1(), color);
            flameVertex(last, out, width / 2, 0.0F, u1, fire.getV1(), color);
            flameVertex(last, out, width / 2, height, u1, fire.getV0(), color);
            flameVertex(last, out, -width / 2, height, u0, fire.getV0(), color);
            pose.mulPose(Axis.YP.rotationDegrees(90.0F));
        }
        pose.popPose();
    }

    /** Full-bright, and facing up for the shading, as vanilla draws the fire on a burning entity. */
    private static void flameVertex(PoseStack.Pose pose, VertexConsumer out, float x, float y, float u, float v, int color) {
        out.addVertex(pose, x, y, 0.0F)
                .setColor(color)
                .setUv(u, v)
                .setOverlay(OverlayTexture.NO_OVERLAY)
                .setLight(LightTexture.FULL_BRIGHT)
                .setNormal(pose, 0.0F, 1.0F, 0.0F);
    }

    @Override
    public ResourceLocation getTextureLocation(RimefireCrown crown) {
        return InventoryMenu.BLOCK_ATLAS;
    }
}
