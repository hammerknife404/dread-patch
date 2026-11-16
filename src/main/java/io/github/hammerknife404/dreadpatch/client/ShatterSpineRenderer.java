package io.github.hammerknife404.dreadpatch.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;

/**
 * Shatter-Spine, drawn so it can be seen in flight (until 0.18.0 it was only its particle trail): the
 * core a slowly turning burr of {@link #CORE_SPIKES} dark ice spikes, a shard one pale spike pointing
 * the way it falls. Both full-bright, from the crown's {@link IceSpike}s and vanilla's packed ice.
 */
final class ShatterSpineRenderer<T extends Entity> extends EntityRenderer<T> {
    static final int CORE_SPIKES = 10;
    private static final ResourceLocation PACKED_ICE = ResourceLocation.withDefaultNamespace("block/packed_ice");
    private static final float GOLDEN_ANGLE = (float) (Math.PI * (3.0 - Math.sqrt(5.0)));

    private final boolean core;

    private ShatterSpineRenderer(EntityRendererProvider.Context context, boolean core) {
        super(context);
        this.core = core;
        this.shadowRadius = 0.0F;
    }

    static <T extends Entity> ShatterSpineRenderer<T> core(EntityRendererProvider.Context context) {
        return new ShatterSpineRenderer<>(context, true);
    }

    static <T extends Entity> ShatterSpineRenderer<T> shard(EntityRendererProvider.Context context) {
        return new ShatterSpineRenderer<>(context, false);
    }

    @Override
    public void render(T entity, float yaw, float partialTick, PoseStack pose, MultiBufferSource buffers, int light) {
        TextureAtlasSprite ice = Minecraft.getInstance().getTextureAtlas(InventoryMenu.BLOCK_ATLAS).apply(PACKED_ICE);
        VertexConsumer out = buffers.getBuffer(RenderType.entityCutoutNoCull(InventoryMenu.BLOCK_ATLAS));
        pose.pushPose();
        pose.translate(0.0F, entity.getBbHeight() / 2.0F, 0.0F);
        if (this.core) {
            float age = entity.tickCount + partialTick;
            pose.mulPose(new Quaternionf().rotationYXZ(age * 0.15F, age * 0.07F, 0.0F));
            for (int i = 0; i < CORE_SPIKES; i++) {
                // Spread evenly over a sphere.
                float y = 1.0F - 2.0F * (i + 0.5F) / CORE_SPIKES;
                float r = Mth.sqrt(1.0F - y * y);
                float phi = i * GOLDEN_ANGLE;
                this.spike(pose, out, ice, Mth.cos(phi) * r, y, Mth.sin(phi) * r, 1.1F + 0.25F * (i % 3), 0.22F, 0.30F, 0.32F, 0.48F);
            }
        } else {
            Vec3 heading = entity.getDeltaMovement();
            if (heading.lengthSqr() < 1.0E-6) {
                heading = new Vec3(0, -1, 0);
            }
            heading = heading.normalize();
            pose.translate(-heading.x * 0.45, -heading.y * 0.45, -heading.z * 0.45);
            this.spike(pose, out, ice, (float) heading.x, (float) heading.y, (float) heading.z, 0.9F, 0.15F, 0.92F, 0.96F, 1.0F);
        }
        pose.popPose();
        super.render(entity, yaw, partialTick, pose, buffers, light);
    }

    private void spike(PoseStack pose, VertexConsumer out, TextureAtlasSprite ice, float dx, float dy, float dz, float length, float half,
                       float r, float g, float b) {
        pose.pushPose();
        pose.mulPose(new Quaternionf().rotationTo(0.0F, 1.0F, 0.0F, dx, dy, dz));
        IceSpike.draw(pose.last(), out, ice.getU0(), ice.getU1(), ice.getV0(), ice.getV1(), length, half, LightTexture.FULL_BRIGHT, r, g, b);
        pose.popPose();
    }

    @Override
    public ResourceLocation getTextureLocation(T entity) {
        return InventoryMenu.BLOCK_ATLAS;
    }
}
