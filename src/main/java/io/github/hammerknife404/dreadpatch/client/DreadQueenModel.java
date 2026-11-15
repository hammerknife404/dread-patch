package io.github.hammerknife404.dreadpatch.client;

import com.iafenvoy.iceandfire.entity.DreadQueenEntity;
import io.github.hammerknife404.dreadpatch.DreadPatch;
import io.github.hammerknife404.dreadpatch.entity.DreadQueen;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;

/**
 * The Dread Queen: the geometry of Ice and Fire's ModelDreadQueen (by Alexthe666 and contributors,
 * LGPL-3.0; never rendered, last seen on branch 1.18.2 of AlexModGuy/Ice_and_Fire), rebuilt as a
 * vanilla humanoid for her 128×64 texture. Same boxes, texture offsets and offsets from the parent
 * part; head, arms and legs sit at the root (the original hung them off the body, which only ever
 * moves as a whole, so they land in the same place). Ice and Fire CE's own dread model base can't
 * be extended from outside its package, so its spawn animation is redone here from {@link RisePose}.
 */
public final class DreadQueenModel extends HumanoidModel<DreadQueen> {
    public static final ModelLayerLocation LAYER = new ModelLayerLocation(DreadPatch.id("dread_queen"), "main");
    /** The original pose angles each arm this far (radians) away from the body. */
    private static final float ARM_FLARE = 0.10000737F;

    public DreadQueenModel(ModelPart root) {
        super(root);
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();

        PartDefinition head = root.addOrReplaceChild("head",
                CubeListBuilder.create().texOffs(0, 0).addBox(-4.0F, -8.0F, -4.0F, 8.0F, 8.0F, 8.0F),
                PartPose.ZERO);
        head.addOrReplaceChild("headware",
                CubeListBuilder.create().texOffs(58, 0).addBox(-4.5F, -12.5F, -4.5F, 9.0F, 13.0F, 9.0F),
                PartPose.offset(0.0F, 1.0F, 0.0F));
        // HumanoidModel needs a hat; the headware stands in for it.
        root.addOrReplaceChild("hat", CubeListBuilder.create(), PartPose.ZERO);

        PartDefinition body = root.addOrReplaceChild("body",
                CubeListBuilder.create().texOffs(16, 16).addBox(-4.0F, 0.0F, -2.0F, 8.0F, 12.0F, 4.0F),
                PartPose.ZERO);
        PartDefinition chestplate = body.addOrReplaceChild("chestplate",
                CubeListBuilder.create().texOffs(1, 32).addBox(-4.5F, -0.3F, -2.5F, 9.0F, 12.0F, 5.0F),
                PartPose.offset(0.0F, 0.1F, 0.0F));
        chestplate.addOrReplaceChild("necklace",
                CubeListBuilder.create().texOffs(1, 52).addBox(-4.5F, -0.3F, -2.8F, 9.0F, 7.0F, 5.0F),
                PartPose.offsetAndRotation(0.0F, 0.1F, 0.0F, 0.032114058F, 0.0F, 0.0F));
        body.addOrReplaceChild("cloak",
                CubeListBuilder.create().texOffs(81, 37).addBox(-4.5F, 0.0F, 2.3F, 9.0F, 21.0F, 1.0F),
                PartPose.offsetAndRotation(0.0F, 0.1F, 0.0F, 0.045553094F, 0.0F, 0.0F));

        PartDefinition rightArm = root.addOrReplaceChild("right_arm",
                CubeListBuilder.create().texOffs(40, 16).addBox(-2.5F, -2.0F, -2.0F, 3.0F, 12.0F, 4.0F),
                PartPose.offsetAndRotation(-5.0F, 2.0F, 0.0F, 0.0F, 0.0F, ARM_FLARE));
        rightArm.addOrReplaceChild("right_sleeve",
                CubeListBuilder.create().texOffs(36, 33).addBox(-4.5F, -2.1F, -2.4F, 5.0F, 6.0F, 5.0F),
                PartPose.offset(0.5F, -0.1F, 0.0F));
        PartDefinition leftArm = root.addOrReplaceChild("left_arm",
                CubeListBuilder.create().texOffs(40, 16).mirror().addBox(-0.5F, -2.0F, -2.0F, 3.0F, 12.0F, 4.0F),
                PartPose.offsetAndRotation(5.0F, 2.0F, 0.0F, 0.0F, 0.0F, -ARM_FLARE));
        leftArm.addOrReplaceChild("left_sleeve",
                CubeListBuilder.create().texOffs(36, 33).mirror().addBox(-0.5F, -2.1F, -2.4F, 5.0F, 6.0F, 5.0F),
                PartPose.offset(-0.5F, -0.1F, 0.0F));

        PartDefinition rightLeg = root.addOrReplaceChild("right_leg",
                CubeListBuilder.create().texOffs(0, 16).addBox(-2.0F, 0.0F, -2.0F, 4.0F, 12.0F, 4.0F),
                PartPose.offset(-1.9F, 12.0F, 0.1F));
        rightLeg.addOrReplaceChild("right_robe",
                CubeListBuilder.create().texOffs(58, 33).mirror().addBox(-2.1F, 0.0F, -2.5F, 4.0F, 7.0F, 5.0F),
                PartPose.offset(0.0F, -0.2F, 0.0F));
        PartDefinition leftLeg = root.addOrReplaceChild("left_leg",
                CubeListBuilder.create().texOffs(0, 16).mirror().addBox(-2.0F, 0.0F, -2.0F, 4.0F, 12.0F, 4.0F),
                PartPose.offset(1.9F, 12.0F, 0.1F));
        leftLeg.addOrReplaceChild("left_robe",
                CubeListBuilder.create().texOffs(58, 33).addBox(-1.9F, 0.0F, -2.5F, 4.0F, 7.0F, 5.0F),
                PartPose.offset(0.0F, -0.2F, 0.0F));

        return LayerDefinition.create(mesh, 128, 64);
    }

    @Override
    public void setupAnim(DreadQueen queen, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {
        super.setupAnim(queen, limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch);
        // HumanoidModel straightens the arms every frame; put the original's flare back on top.
        this.rightArm.zRot += ARM_FLARE;
        this.leftArm.zRot -= ARM_FLARE;
        if (queen.getAnimation() == DreadQueenEntity.ANIMATION_SPAWN) {
            this.rise(queen.getAnimationTick() + (ageInTicks - queen.tickCount), ageInTicks);
        }
    }

    /** Ice and Fire's dread spawn animation: up out of the ground, arms raised and waving. */
    private void rise(float tick, float ageInTicks) {
        float depth = RisePose.depth(tick);
        for (ModelPart part : new ModelPart[]{this.head, this.hat, this.body, this.rightArm, this.leftArm, this.rightLeg, this.leftLeg}) {
            part.y += depth;
        }
        float up = RisePose.armsUp(tick);
        if (up <= 0.0F) {
            return;
        }
        // Straight overhead (Ice and Fire rotates them -180°), waving as its DreadBaseModel does:
        // Uranus flap (z) and walk (x), cos(age · 0.5 + offset) · 0.5 + weight. Ice and Fire stops
        // the waving dead at tick 30; here it fades out with the arms instead of snapping.
        float zWave = (float) Math.cos(ageInTicks * 0.5F + 2.0F) * 0.5F - 0.7F;
        float xWave = (float) Math.cos(ageInTicks * 0.5F + 1.0F) * 0.5F;
        this.rightArm.xRot -= up * ((float) Math.PI + xWave);
        this.leftArm.xRot -= up * ((float) Math.PI + xWave);
        this.rightArm.zRot += up * zWave;
        this.leftArm.zRot -= up * zWave;
    }
}
