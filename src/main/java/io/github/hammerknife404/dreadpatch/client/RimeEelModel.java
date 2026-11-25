package io.github.hammerknife404.dreadpatch.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import io.github.hammerknife404.dreadpatch.DreadPatch;
import io.github.hammerknife404.dreadpatch.entity.RimeEel;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.util.Mth;

/**
 * The Rime Eel (0.71.0), ours, after the owner's picture: an anglerfish's head (a heavy jaw, fangs, a lure on a
 * crooked stalk with a lantern at its end, spiny fins at its sides), on an eel's body in four tapering segments
 * with a dorsal ridge and a tail fin. 24 pixels from its lantern's stalk to its tail fin, head toward -z, for its
 * 64×64 texture ({@code tools/sprites/make_icons.py}). The body's segments hang off one another, so a wave down
 * them is a serpent's: slow and slight in water, wide and quick on land. Its jaw drops to bite.
 */
public class RimeEelModel extends EntityModel<RimeEel> {
    public static final ModelLayerLocation LAYER = new ModelLayerLocation(DreadPatch.id("rime_eel"), "main");

    private final ModelPart neck;
    private final ModelPart head;
    private final ModelPart jaw;
    private final ModelPart stalk;
    private final ModelPart leftFin;
    private final ModelPart rightFin;
    private final ModelPart[] segments;
    private final ModelPart tailFin;

    public RimeEelModel(ModelPart root) {
        super(RenderType::entityCutoutNoCull);
        this.neck = root.getChild("neck");
        this.head = this.neck.getChild("head");
        this.jaw = this.head.getChild("jaw");
        this.stalk = this.head.getChild("stalk");
        this.leftFin = this.head.getChild("left_fin");
        this.rightFin = this.head.getChild("right_fin");
        ModelPart second = this.neck.getChild("body");
        ModelPart third = second.getChild("body");
        ModelPart tail = third.getChild("tail");
        this.segments = new ModelPart[]{this.neck, second, third, tail};
        this.tailFin = tail.getChild("tail_fin");
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        // The first segment, the root of the rest: the head forward of it (-z), the body behind (+z).
        PartDefinition neck = root.addOrReplaceChild("neck",
                CubeListBuilder.create().texOffs(0, 0).addBox(-2.0F, -3.0F, 0.0F, 4.0F, 4.0F, 4.0F),
                PartPose.offset(0.0F, 22.0F, -4.0F));
        neck.addOrReplaceChild("dorsal",
                CubeListBuilder.create().texOffs(8, 24).addBox(0.0F, -3.0F, 0.0F, 0.0F, 3.0F, 4.0F),
                PartPose.offset(0.0F, -3.0F, 0.0F));

        PartDefinition head = neck.addOrReplaceChild("head",
                CubeListBuilder.create().texOffs(0, 8).addBox(-2.5F, -3.5F, -5.0F, 5.0F, 3.0F, 5.0F),
                PartPose.ZERO);
        head.addOrReplaceChild("upper_teeth",
                CubeListBuilder.create().texOffs(20, 8).addBox(-2.0F, -0.5F, -4.6F, 4.0F, 1.0F, 4.0F),
                PartPose.ZERO);
        PartDefinition jaw = head.addOrReplaceChild("jaw",
                CubeListBuilder.create().texOffs(0, 16).addBox(-2.5F, 0.0F, -5.5F, 5.0F, 2.0F, 5.0F),
                PartPose.offset(0.0F, -0.5F, 0.0F));
        jaw.addOrReplaceChild("lower_teeth",
                CubeListBuilder.create().texOffs(20, 13).addBox(-2.0F, -1.0F, -5.1F, 4.0F, 1.0F, 4.0F),
                PartPose.ZERO);
        // The lure: up from its brow, crooked forward, the lantern hanging from its end.
        PartDefinition stalk = head.addOrReplaceChild("stalk",
                CubeListBuilder.create().texOffs(36, 8).addBox(-0.5F, -4.0F, -0.5F, 1.0F, 4.0F, 1.0F),
                PartPose.offsetAndRotation(0.0F, -3.5F, -3.0F, -0.25F, 0.0F, 0.0F));
        PartDefinition tip = stalk.addOrReplaceChild("stalk_tip",
                CubeListBuilder.create().texOffs(40, 8).addBox(-0.5F, -0.5F, -4.0F, 1.0F, 1.0F, 4.0F),
                PartPose.offsetAndRotation(0.0F, -4.0F, 0.0F, 0.45F, 0.0F, 0.0F));
        tip.addOrReplaceChild("lantern",
                CubeListBuilder.create().texOffs(50, 8).addBox(-1.0F, 0.0F, -1.0F, 2.0F, 3.0F, 2.0F),
                PartPose.offsetAndRotation(0.0F, 0.0F, -3.5F, -0.2F, 0.0F, 0.0F));
        head.addOrReplaceChild("left_fin",
                CubeListBuilder.create().texOffs(24, 24).addBox(0.0F, 0.0F, 0.0F, 3.0F, 0.0F, 3.0F),
                PartPose.offsetAndRotation(2.5F, -1.0F, -2.0F, 0.0F, -0.5F, 0.6F));
        head.addOrReplaceChild("right_fin",
                CubeListBuilder.create().texOffs(24, 24).mirror().addBox(-3.0F, 0.0F, 0.0F, 3.0F, 0.0F, 3.0F),
                PartPose.offsetAndRotation(-2.5F, -1.0F, -2.0F, 0.0F, 0.5F, -0.6F));

        PartDefinition second = neck.addOrReplaceChild("body",
                CubeListBuilder.create().texOffs(20, 0).addBox(-1.5F, -2.5F, 0.0F, 3.0F, 3.0F, 4.0F),
                PartPose.offset(0.0F, 0.0F, 4.0F));
        second.addOrReplaceChild("dorsal",
                CubeListBuilder.create().texOffs(16, 24).addBox(0.0F, -2.0F, 0.0F, 0.0F, 2.0F, 4.0F),
                PartPose.offset(0.0F, -2.5F, 0.0F));
        PartDefinition third = second.addOrReplaceChild("body",
                CubeListBuilder.create().texOffs(34, 0).addBox(-1.0F, -2.0F, 0.0F, 2.0F, 2.0F, 4.0F),
                PartPose.offset(0.0F, 0.0F, 4.0F));
        PartDefinition tail = third.addOrReplaceChild("tail",
                CubeListBuilder.create().texOffs(46, 0).addBox(-0.5F, -1.5F, 0.0F, 1.0F, 1.0F, 3.0F),
                PartPose.offset(0.0F, 0.0F, 4.0F));
        tail.addOrReplaceChild("tail_fin",
                CubeListBuilder.create().texOffs(0, 24).addBox(0.0F, -2.5F, 0.0F, 0.0F, 5.0F, 4.0F),
                PartPose.offset(0.0F, -1.0F, 2.0F));
        return LayerDefinition.create(mesh, 64, 64);
    }

    @Override
    public void setupAnim(RimeEel eel, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {
        boolean water = eel.isInWater();
        float moving = Mth.clamp(limbSwingAmount * (water ? 4.0F : 2.0F), 0.0F, 1.0F);
        // A wave down its segments, each turned from the one before: a serpent's on land, an eel's in water.
        float rate = water ? 0.25F + 0.25F * moving : 0.35F + 0.45F * moving;
        float swing = water ? 0.12F + 0.18F * moving : 0.2F + 0.35F * moving;
        float time = ageInTicks * rate;
        for (int i = 0; i < this.segments.length; i++) {
            this.segments[i].yRot = swing * (i == 0 ? 0.4F : 1.0F) * Mth.sin(time - i * 0.9F);
        }
        this.tailFin.yRot = swing * 0.8F * Mth.sin(time - 4 * 0.9F);
        // The head turns against its body, so it keeps its eyes on its way; pitched as it swims up or down.
        this.head.yRot = -this.segments[0].yRot * 0.8F + netHeadYaw * Mth.DEG_TO_RAD * 0.3F;
        this.neck.xRot = water ? headPitch * Mth.DEG_TO_RAD : 0.0F;
        // Its jaw hangs a little, and drops to bite.
        float bite = Mth.sin(this.attackTime * Mth.PI);
        this.jaw.xRot = 0.12F + 0.04F * Mth.sin(ageInTicks * 0.08F) + 0.75F * bite;
        this.stalk.xRot = -0.25F + 0.08F * Mth.sin(ageInTicks * 0.11F) - 0.15F * bite;
        float fin = 0.6F + 0.25F * Mth.sin(ageInTicks * (water ? 0.3F : 0.6F));
        this.leftFin.zRot = fin;
        this.rightFin.zRot = -fin;
    }

    @Override
    public void renderToBuffer(PoseStack poseStack, VertexConsumer buffer, int light, int overlay, int color) {
        this.neck.render(poseStack, buffer, light, overlay, color);
    }
}
