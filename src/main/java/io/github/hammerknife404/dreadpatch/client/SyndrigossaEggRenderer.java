package io.github.hammerknife404.dreadpatch.client;

import com.mojang.blaze3d.vertex.PoseStack;
import io.github.hammerknife404.dreadpatch.entity.SyndrigossaEggEntity;
import io.github.hammerknife404.dreadpatch.registry.ModRegistries;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;

/** Her egg, laid: its item's sprite standing on the ground, turned to whoever looks at it, its size. */
public final class SyndrigossaEggRenderer extends EntityRenderer<SyndrigossaEggEntity> {
    private static final float SIZE = 0.7F;
    private final ItemRenderer items;
    private ItemStack egg = ItemStack.EMPTY;

    public SyndrigossaEggRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.items = context.getItemRenderer();
        this.shadowRadius = 0.25F;
    }

    @Override
    public void render(SyndrigossaEggEntity entity, float entityYaw, float partialTick, PoseStack pose, MultiBufferSource buffers, int light) {
        if (this.egg.isEmpty()) {
            this.egg = new ItemStack(ModRegistries.SYNDRIGOSSA_EGG.get());
        }
        pose.pushPose();
        pose.translate(0.0F, SIZE / 2.0F, 0.0F);
        pose.mulPose(this.entityRenderDispatcher.cameraOrientation());
        pose.scale(SIZE, SIZE, SIZE);
        this.items.renderStatic(this.egg, ItemDisplayContext.FIXED, light, OverlayTexture.NO_OVERLAY, pose, buffers, entity.level(), entity.getId());
        pose.popPose();
        super.render(entity, entityYaw, partialTick, pose, buffers, light);
    }

    @Override
    public ResourceLocation getTextureLocation(SyndrigossaEggEntity entity) {
        return InventoryMenu.BLOCK_ATLAS;
    }
}
