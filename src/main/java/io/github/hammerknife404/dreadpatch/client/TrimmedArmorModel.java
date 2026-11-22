package io.github.hammerknife404.dreadpatch.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.SpriteCoordinateExpander;
import net.minecraft.world.entity.LivingEntity;

/**
 * Armour trims on armour with a model of its own (0.44.0). The armour layer draws a trim with whatever
 * model the item hands it, reading the trim's sprite (vanilla's 64×32 armour layout) through that
 * model's texture coordinates; ours are laid out for other textures (the queen's 128×64 sheet, Ice and
 * Fire's 64×64 armour), so a trim came out scrambled. This model is vanilla's armour shape, as the layer's
 * own (outer, or inner for leggings), carrying the real one: the trim pass, which vanilla always draws
 * through a sprite-wrapped buffer ({@code TextureAtlasSprite.wrap}, a {@link SpriteCoordinateExpander}),
 * is drawn on the vanilla shape, as on vanilla armour; every other pass (the armour, its glint) on the
 * real model, posed and shown as this one is. No mixin.
 */
public final class TrimmedArmorModel extends HumanoidModel<LivingEntity> {
    private final HumanoidModel<LivingEntity> armour;

    @SuppressWarnings("unchecked")
    public TrimmedArmorModel(HumanoidModel<?> armour, boolean inner) {
        super(Minecraft.getInstance().getEntityModels().bakeLayer(inner ? ModelLayers.PLAYER_INNER_ARMOR : ModelLayers.PLAYER_OUTER_ARMOR));
        this.armour = (HumanoidModel<LivingEntity>) armour;
    }

    @Override
    public void renderToBuffer(PoseStack poseStack, VertexConsumer buffer, int packedLight, int packedOverlay, int color) {
        if (buffer instanceof SpriteCoordinateExpander) {
            super.renderToBuffer(poseStack, buffer, packedLight, packedOverlay, color);
            return;
        }
        this.copyPropertiesTo(this.armour);
        this.armour.head.visible = this.head.visible;
        this.armour.hat.visible = this.hat.visible;
        this.armour.body.visible = this.body.visible;
        this.armour.rightArm.visible = this.rightArm.visible;
        this.armour.leftArm.visible = this.leftArm.visible;
        this.armour.rightLeg.visible = this.rightLeg.visible;
        this.armour.leftLeg.visible = this.leftLeg.visible;
        this.armour.renderToBuffer(poseStack, buffer, packedLight, packedOverlay, color);
    }
}
