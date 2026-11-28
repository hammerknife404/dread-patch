package io.github.hammerknife404.dreadpatch.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import io.github.hammerknife404.dreadpatch.block.DreadCampfireBlock;
import io.github.hammerknife404.dreadpatch.block.DreadCampfireBlockEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;

/** The food on a Dread Campfire (0.98.4), laid as vanilla's {@code CampfireRenderer} lays a campfire's. */
public class DreadCampfireRenderer implements BlockEntityRenderer<DreadCampfireBlockEntity> {
    private final ItemRenderer items;

    public DreadCampfireRenderer(BlockEntityRendererProvider.Context context) {
        this.items = context.getItemRenderer();
    }

    @Override
    public void render(DreadCampfireBlockEntity campfire, float partialTick, PoseStack pose, MultiBufferSource buffers, int light, int overlay) {
        Direction facing = campfire.getBlockState().getValue(DreadCampfireBlock.FACING);
        int seed = (int) campfire.getBlockPos().asLong();
        for (int i = 0; i < campfire.getItems().size(); i++) {
            ItemStack stack = campfire.getItems().get(i);
            if (stack.isEmpty()) {
                continue;
            }
            pose.pushPose();
            pose.translate(0.5F, 0.44921875F, 0.5F);
            Direction side = Direction.from2DDataValue((i + facing.get2DDataValue()) % 4);
            pose.mulPose(Axis.YP.rotationDegrees(-side.toYRot()));
            pose.mulPose(Axis.XP.rotationDegrees(90.0F));
            pose.translate(-0.3125F, -0.3125F, 0.0F);
            pose.scale(0.375F, 0.375F, 0.375F);
            this.items.renderStatic(stack, ItemDisplayContext.FIXED, light, overlay, pose, buffers, campfire.getLevel(), seed + i);
            pose.popPose();
        }
    }
}
