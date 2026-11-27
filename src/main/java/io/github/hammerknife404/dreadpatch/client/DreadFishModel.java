package io.github.hammerknife404.dreadpatch.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import io.github.hammerknife404.dreadpatch.DreadPatch;
import io.github.hammerknife404.dreadpatch.entity.DreadFish;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.util.Mth;

/**
 * The Dread Fish (0.71.0), after the owner's picture: a big, boxy anglerfish head with its jaw hanging open on rows of
 * fangs, a lure on a tall stalk reaching over its snout with a lantern hanging from it (on one in six), fanned fins
 * behind, a jagged dorsal fin; a thick, ribbed serpent's body of eight segments curling round behind it into a ring,
 * to a tail fin (0.76.0, the owner's picture again). Its geometry and
 * texture come from {@code tools/models/make_dread_fish.py} ({@link DreadFishGeometry}). At rest the body curls; moving, it
 * straightens out behind (0.71.2); a wave runs down it on top of that, slight and slow in water, wider and quicker on land. Its jaw gapes wider to bite,
 * its lure sways, its fins beat. (The first model, a small angler the owner liked, is kept on the branch
 * {@code angler-model} for later.)
 */
public class DreadFishModel extends EntityModel<DreadFish> {
    public static final ModelLayerLocation LAYER = new ModelLayerLocation(DreadPatch.id("dread_fish"), "main");

    /** How much of its curl goes as it moves (all but a little), and where its head sits when it's straight. */
    static final float STRAIGHTENS = 0.9F;
    static final float STRAIGHT_X = 0.0F;
    static final float STRAIGHT_Z = -18.0F;
    /** Its lure drawn in, idle (0.76.0): added to the stalk's and arm's x turns (the script's {@code RETRACT}). */
    static final float RETRACT_STALK = -0.6F;
    static final float RETRACT_ARM = -0.1F;
    /** Its body's segments (the script's {@code SEGMENTS}). */
    static final int SEGMENTS = 8;

    private final ModelPart root;
    private final ModelPart head;
    private final ModelPart jaw;
    private final ModelPart stalk;
    private final ModelPart arm;
    private final ModelPart hook;
    private final ModelPart lantern;
    private final ModelPart leftFin;
    private final ModelPart rightFin;
    private final ModelPart[] body = new ModelPart[SEGMENTS];
    private final ModelPart tailFin;

    public DreadFishModel(ModelPart root) {
        super(RenderType::entityCutoutNoCull);
        this.root = root;
        this.head = root.getChild("head");
        this.jaw = this.head.getChild("jaw");
        this.stalk = this.head.getChild("stalk");
        this.arm = this.stalk.getChild("stalk_arm");
        this.hook = this.arm.getChild("stalk_hook");
        this.lantern = this.hook.getChild("lantern");
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
        return DreadFishGeometry.create();
    }

    @Override
    public void setupAnim(DreadFish fish, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {
        this.root.getAllParts().forEach(ModelPart::resetPose);
        boolean water = fish.isInWater();
        // Curled at rest; on the move its body straightens out behind it (0.71.2, the owner: "its tail is stuck in
        // that pose"), the head drawn forward and over so the straight fish stays on its middle. By its own eased
        // straightness (0.92.1), not vanilla's limb swing, which left it curling mid-swim and twitching.
        float moving = fish.straight(ageInTicks - fish.tickCount);
        float curl = 1.0F - moving * STRAIGHTENS;
        for (ModelPart part : this.body) {
            part.yRot *= curl;
        }
        this.body[0].xRot *= curl;
        this.body[1].xRot *= curl;
        this.head.x = Mth.lerp(curl, STRAIGHT_X, this.head.getInitialPose().x);
        this.head.z = Mth.lerp(curl, STRAIGHT_Z, this.head.getInitialPose().z);
        // A wave down its segments, on top of its curl: a fish's in water, a serpent's on land. Calmer since 0.92.1: chained
        // down eight segments, land's 0.4 a segment at over two beats a second thrashed.
        float rate = water ? 0.15F + 0.2F * moving : 0.2F + 0.25F * moving;
        float swing = water ? 0.06F + 0.1F * moving : 0.1F + 0.14F * moving;
        float time = ageInTicks * rate;
        for (int i = 0; i < this.body.length; i++) {
            this.body[i].yRot += swing * (i == 0 ? 0.5F : 1.0F) * Mth.sin(time - i * 0.8F);
        }
        this.tailFin.yRot += swing * 1.2F * Mth.sin(time - this.body.length * 0.8F);
        // Its head looks where it's going, a little; and bobs. Pitched as it swims, and in the air too (0.92.1): a breach's
        // or a leap's arc went flat the moment it left the water, and snapped back as it fell in.
        this.head.yRot += netHeadYaw * Mth.DEG_TO_RAD * 0.3F;
        this.head.xRot += (water || !fish.onGround() ? headPitch * Mth.DEG_TO_RAD * 0.5F : 0.0F) + 0.03F * Mth.sin(ageInTicks * 0.1F);
        // Its jaw hangs open, breathing a little, and gapes to bite.
        float bite = Mth.sin(this.attackTime * Mth.PI);
        // An alpha's wind-up (0.80.0): rearing back, head up, its jaw gaping wide, before the snap; plain to read.
        float rear = fish.rear(ageInTicks - fish.tickCount);
        this.head.xRot -= 0.45F * rear;
        this.head.y -= 2.0F * rear;
        this.jaw.xRot += 0.06F * Mth.sin(ageInTicks * 0.08F) + 0.5F * bite + 0.75F * rear;
        // Its lure (0.76.0): none at all without its lantern; reaching out as it hunts or swims about, drawn in over its
        // head while still; swaying. Its lantern hangs plumb under it, whatever the rest is doing, swinging a little
        // and trailing back as it moves.
        this.stalk.visible = fish.hasLantern();
        float drawnIn = 1.0F - fish.reach(ageInTicks - fish.tickCount);
        this.stalk.xRot += RETRACT_STALK * drawnIn + 0.06F * Mth.sin(ageInTicks * 0.09F) - 0.12F * bite;
        this.stalk.zRot += 0.05F * Mth.sin(ageInTicks * 0.07F);
        this.arm.xRot += RETRACT_ARM * drawnIn;
        this.lantern.xRot = -(this.head.xRot + this.stalk.xRot + this.arm.xRot + this.hook.xRot)
                + 0.12F * Mth.sin(ageInTicks * 0.13F + 1.0F) + 0.35F * moving;
        this.lantern.zRot = -this.stalk.zRot + 0.1F * Mth.sin(ageInTicks * 0.11F);
        float fin = 0.2F * Mth.sin(ageInTicks * (water ? 0.25F : 0.5F));
        this.leftFin.yRot += fin;
        this.rightFin.yRot -= fin;
    }

    @Override
    public void renderToBuffer(PoseStack poseStack, VertexConsumer buffer, int light, int overlay, int color) {
        this.head.render(poseStack, buffer, light, overlay, color);
    }
}
