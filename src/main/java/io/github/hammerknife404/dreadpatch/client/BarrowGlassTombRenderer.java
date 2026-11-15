package io.github.hammerknife404.dreadpatch.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import io.github.hammerknife404.dreadpatch.entity.BarrowGlassTomb;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.client.model.data.ModelData;

/**
 * The tomb's spires: black stained glass shafts with bone caps, standing round the ring and leaning
 * a little, each its own height. They shoot up over its first ticks and sink over its last.
 */
public class BarrowGlassTombRenderer extends EntityRenderer<BarrowGlassTomb> {
    private static final int SPIRES = 9;
    private static final BlockState GLASS = Blocks.BLACK_STAINED_GLASS.defaultBlockState();
    private static final BlockState BONE = Blocks.BONE_BLOCK.defaultBlockState();

    private final BlockRenderDispatcher blocks;

    public BarrowGlassTombRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.blocks = context.getBlockRenderDispatcher();
        this.shadowRadius = 0.0F;
    }

    @Override
    public void render(BarrowGlassTomb tomb, float yaw, float partialTick, PoseStack pose, MultiBufferSource buffers, int light) {
        float age = tomb.tickCount + partialTick;
        float grow = Math.min(Mth.clamp(age / 6.0F, 0.0F, 1.0F), Mth.clamp((tomb.getLifetime() - age) / 4.0F, 0.0F, 1.0F));
        if (grow > 0.0F) {
            for (int i = 0; i < SPIRES; i++) {
                this.renderSpire(i, grow, pose, buffers, light);
            }
        }
        super.render(tomb, yaw, partialTick, pose, buffers, light);
    }

    private void renderSpire(int i, float grow, PoseStack pose, MultiBufferSource buffers, int light) {
        double angle = Math.PI * 2 * i / SPIRES + (i % 2) * 0.17;
        float height = (1.6F + 0.9F * ((i * 37) % 10) / 10.0F) * grow;
        float width = 0.28F + 0.08F * (i % 3);
        float lean = ((i * 13) % 11) - 5.0F;
        pose.pushPose();
        pose.translate(Math.cos(angle) * BarrowGlassTomb.RADIUS, 0.0, Math.sin(angle) * BarrowGlassTomb.RADIUS);
        pose.mulPose(Axis.YP.rotationDegrees(i * 47.0F));
        pose.mulPose(Axis.ZP.rotationDegrees(lean));
        pose.pushPose();
        pose.scale(width, height, width);
        pose.translate(-0.5, 0.0, -0.5);
        this.blocks.renderSingleBlock(GLASS, pose, buffers, light, OverlayTexture.NO_OVERLAY, ModelData.EMPTY, null);
        pose.popPose();
        float cap = width * 0.6F;
        pose.translate(0.0, height, 0.0);
        pose.scale(cap, cap * 0.8F, cap);
        pose.translate(-0.5, 0.0, -0.5);
        this.blocks.renderSingleBlock(BONE, pose, buffers, light, OverlayTexture.NO_OVERLAY, ModelData.EMPTY, null);
        pose.popPose();
    }

    @Override
    public ResourceLocation getTextureLocation(BarrowGlassTomb tomb) {
        return InventoryMenu.BLOCK_ATLAS;
    }
}
