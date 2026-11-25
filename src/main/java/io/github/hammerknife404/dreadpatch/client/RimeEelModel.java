package io.github.hammerknife404.dreadpatch.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import io.github.hammerknife404.dreadpatch.DreadPatch;
import io.github.hammerknife404.dreadpatch.entity.RimeEel;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.util.Mth;

/**
 * The Rime Eel (0.71.0), after the owner's picture: a big, boxy anglerfish head with its jaw hanging open on rows of
 * fangs, a jagged crest, a lure on a tall stalk arching over its snout with a lantern hanging from it, fanned fins
 * behind; a thick, ribbed serpent's body of seven segments curling round behind it to a tail fin. Its geometry and
 * texture come from {@code tools/models/make_rime_eel.py} ({@link RimeEelGeometry}). At rest the body curls; moving, it
 * straightens out behind (0.71.2); a wave runs down it on top of that, slight and slow in water, wider and quicker on land. Its jaw gapes wider to bite,
 * its lure sways, its fins beat. (The first model, a small angler the owner liked, is kept on the branch
 * {@code angler-model} for later.)
 */
public class RimeEelModel extends EntityModel<RimeEel> {
    public static final ModelLayerLocation LAYER = new ModelLayerLocation(DreadPatch.id("rime_eel"), "main");

    /** How much of its curl goes as it moves (all but a little), and where its head sits when it's straight. */
    static final float STRAIGHTENS = 0.9F;
    static final float STRAIGHT_X = 0.0F;
    static final float STRAIGHT_Z = -12.0F;

    private final ModelPart root;
    private final ModelPart head;
    private final ModelPart jaw;
    private final ModelPart stalk;
    private final ModelPart lantern;
    private final ModelPart leftFin;
    private final ModelPart rightFin;
    private final ModelPart[] body = new ModelPart[7];
    private final ModelPart tailFin;

    public RimeEelModel(ModelPart root) {
        super(RenderType::entityCutoutNoCull);
        this.root = root;
        this.head = root.getChild("head");
        this.jaw = this.head.getChild("jaw");
        this.stalk = this.head.getChild("stalk");
        this.lantern = this.stalk.getChild("stalk_arm").getChild("stalk_hook").getChild("lantern");
        this.leftFin = this.head.getChild("left_fin");
        this.rightFin = this.head.getChild("right_fin");
        ModelPart part = this.head;
        for (int i = 0; i < this.body.length; i++) {
            part = part.getChild("body" + (i + 1));
            this.body[i] = part;
        }
        this.tailFin = part.getChild("tail_fin");
    }

    public static LayerDefinition createBodyLayer() {
        return RimeEelGeometry.create();
    }

    @Override
    public void setupAnim(RimeEel eel, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {
        this.root.getAllParts().forEach(ModelPart::resetPose);
        boolean water = eel.isInWater();
        float moving = Mth.clamp(limbSwingAmount * (water ? 4.0F : 2.0F), 0.0F, 1.0F);
        // Curled at rest; on the move its body straightens out behind it (0.71.2, the owner: "its tail is stuck in
        // that pose"), the head drawn forward and over so the straight eel stays on its middle.
        float curl = 1.0F - Mth.clamp(limbSwingAmount * 6.0F, 0.0F, 1.0F) * STRAIGHTENS;
        for (ModelPart part : this.body) {
            part.yRot *= curl;
        }
        this.body[0].xRot *= curl;
        this.body[1].xRot *= curl;
        this.head.x = Mth.lerp(curl, STRAIGHT_X, this.head.getInitialPose().x);
        this.head.z = Mth.lerp(curl, STRAIGHT_Z, this.head.getInitialPose().z);
        // A wave down its segments, on top of its curl: an eel's in water, a serpent's on land.
        float rate = water ? 0.2F + 0.2F * moving : 0.3F + 0.4F * moving;
        float swing = water ? 0.08F + 0.14F * moving : 0.15F + 0.25F * moving;
        float time = ageInTicks * rate;
        for (int i = 0; i < this.body.length; i++) {
            this.body[i].yRot += swing * (i == 0 ? 0.5F : 1.0F) * Mth.sin(time - i * 0.8F);
        }
        this.tailFin.yRot += swing * 1.2F * Mth.sin(time - this.body.length * 0.8F);
        // Its head looks where it's going, a little; and bobs.
        this.head.yRot += netHeadYaw * Mth.DEG_TO_RAD * 0.3F;
        this.head.xRot += (water ? headPitch * Mth.DEG_TO_RAD * 0.5F : 0.0F) + 0.03F * Mth.sin(ageInTicks * 0.1F);
        // Its jaw hangs open, breathing a little, and gapes to bite.
        float bite = Mth.sin(this.attackTime * Mth.PI);
        this.jaw.xRot += 0.06F * Mth.sin(ageInTicks * 0.08F) + 0.5F * bite;
        // Its lure sways, and its lantern swings under it.
        this.stalk.xRot += 0.06F * Mth.sin(ageInTicks * 0.09F) - 0.12F * bite;
        this.stalk.zRot += 0.05F * Mth.sin(ageInTicks * 0.07F);
        this.lantern.xRot += 0.15F * Mth.sin(ageInTicks * 0.13F + 1.0F);
        this.lantern.zRot += 0.1F * Mth.sin(ageInTicks * 0.11F);
        float fin = 0.2F * Mth.sin(ageInTicks * (water ? 0.25F : 0.5F));
        this.leftFin.yRot += fin;
        this.rightFin.yRot -= fin;
    }

    @Override
    public void renderToBuffer(PoseStack poseStack, VertexConsumer buffer, int light, int overlay, int color) {
        this.head.render(poseStack, buffer, light, overlay, color);
    }
}
