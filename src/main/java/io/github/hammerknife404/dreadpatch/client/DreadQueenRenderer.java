package io.github.hammerknife404.dreadpatch.client;

import io.github.hammerknife404.dreadpatch.entity.DreadQueen;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.client.renderer.entity.layers.EyesLayer;
import net.minecraft.resources.ResourceLocation;

/** Her model with Ice and Fire's textures, her regalia over it (0.81.0), glowing eyes, and her sword and staff in hand. */
public final class DreadQueenRenderer extends HumanoidMobRenderer<DreadQueen, DreadQueenModel<DreadQueen>> {
    static final ResourceLocation TEXTURE = iceAndFire("textures/entity/dread/dread_queen.png");
    static final RenderType EYES = RenderType.eyes(iceAndFire("textures/entity/dread/dread_queen_eyes.png"));

    public DreadQueenRenderer(EntityRendererProvider.Context context) {
        super(context, new DreadQueenModel<>(context.bakeLayer(DreadQueenModel.LAYER)), 0.5F);
        this.addLayer(new RegaliaLayer(this));
        this.addLayer(new EyesLayer<>(this) {
            @Override
            public RenderType renderType() {
                return EYES;
            }
        });
    }

    @Override
    public ResourceLocation getTextureLocation(DreadQueen entity) {
        return TEXTURE;
    }

    static ResourceLocation iceAndFire(String path) {
        return ResourceLocation.fromNamespaceAndPath("iceandfire", path);
    }

    /** Her regalia on her, each piece as a player's armour layer draws it, posed as she is. */
    static final class RegaliaLayer extends net.minecraft.client.renderer.entity.layers.RenderLayer<DreadQueen, DreadQueenModel<DreadQueen>> {
        RegaliaLayer(net.minecraft.client.renderer.entity.RenderLayerParent<DreadQueen, DreadQueenModel<DreadQueen>> parent) {
            super(parent);
        }

        @Override
        public void render(com.mojang.blaze3d.vertex.PoseStack poseStack, net.minecraft.client.renderer.MultiBufferSource buffers, int light, DreadQueen queen,
                           float limbSwing, float limbSwingAmount, float partialTick, float ageInTicks, float netHeadYaw, float headPitch) {
            if (queen.isInvisible()) {
                return;
            }
            var buffer = buffers.getBuffer(RenderType.armorCutoutNoCull(QueenArmorModels.TEXTURE));
            int overlay = net.minecraft.client.renderer.entity.LivingEntityRenderer.getOverlayCoords(queen, 0.0F);
            for (var slot : new net.minecraft.world.entity.EquipmentSlot[]{net.minecraft.world.entity.EquipmentSlot.HEAD,
                    net.minecraft.world.entity.EquipmentSlot.CHEST, net.minecraft.world.entity.EquipmentSlot.LEGS, net.minecraft.world.entity.EquipmentSlot.FEET}) {
                @SuppressWarnings("unchecked")
                var model = (net.minecraft.client.model.HumanoidModel<DreadQueen>) (net.minecraft.client.model.HumanoidModel<?>)
                        QueenArmorModels.plain(QueenArmorModels.layerFor(slot, false));
                this.getParentModel().copyPropertiesTo(model);
                model.renderToBuffer(poseStack, buffer, light, overlay, -1);
            }
        }
    }
}
