package io.github.hammerknife404.dreadpatch.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import io.github.hammerknife404.dreadpatch.DreadPatch;
import net.minecraft.client.model.Model;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

import java.util.Map;
import java.util.WeakHashMap;

/**
 * The winged bodice's wings (0.82.0, the owner's spec and example videos: a scaled-down Syndrigossa's tattered ice
 * wings), {@link RegaliaWingsGeometry}'s chains, posed by state and eased between them, each wearer's own:
 * <ul>
 * <li>idle: held up in a V behind the back, the hand folded a little, breathing slowly;</li>
 * <li>walking: swept back, bobbing with the steps, flaring open a little when sprinting;</li>
 * <li>falling: pushed up and trailing against the air, the hands fluttering;</li>
 * <li>gliding: spread wide and nearly level, banking into turns (a roll from how fast the wearer turns);</li>
 * <li>a creative player's flight: beating.</li>
 * </ul>
 * Turns are the right wing's; the left mirrors them (y and z negated), but banking rolls both the same way.
 */
public final class RegaliaWingsModel extends Model {
    public static final ModelLayerLocation LAYER = new ModelLayerLocation(DreadPatch.id("regalia_wings"), "main");
    /** How fast it eases toward a state: this share of the way a tick. */
    static final float EASE = 0.15F;

    private final ModelPart root;
    private final ModelPart[] roots = new ModelPart[2];
    private final ModelPart[] hands = new ModelPart[2];
    private final Map<LivingEntity, State> states = new WeakHashMap<>();

    public RegaliaWingsModel(ModelPart root) {
        super(RenderType::entityTranslucent);
        this.root = root;
        String[] names = {"right_wing", "left_wing"};
        for (int i = 0; i < 2; i++) {
            this.roots[i] = root.getChild(names[i]);
            this.hands[i] = this.roots[i].getChild(names[i] + "_arm").getChild(names[i] + "_hand");
        }
    }

    /** Its pose for this wearer now: {@code time} in ticks, partial ticks in. */
    public void setupAnim(LivingEntity entity, float limbSwing, float limbSwingAmount, float time) {
        this.root.getAllParts().forEach(ModelPart::resetPose);
        State state = this.states.computeIfAbsent(entity, e -> new State(time));
        float step = Mth.clamp((time - state.time) * EASE, 0.0F, 1.0F);
        state.time = time;
        boolean beating = entity instanceof Player player && player.getAbilities().flying;
        boolean gliding = entity.isFallFlying();
        boolean falling = !gliding && !beating && !entity.onGround() && entity.getDeltaMovement().y < -0.35;
        boolean walking = !gliding && !beating && !falling && limbSwingAmount > 0.08F;
        state.glide += ((gliding ? 1.0F : 0.0F) - state.glide) * step;
        state.beat += ((beating ? 1.0F : 0.0F) - state.beat) * step;
        state.fall += ((falling ? 1.0F : 0.0F) - state.fall) * step;
        state.walk += ((walking ? 1.0F : 0.0F) - state.walk) * step;
        state.sprint += ((walking && entity.isSprinting() ? 1.0F : 0.0F) - state.sprint) * step;
        // Banking: how fast the wearer's body turns, as a roll, eased.
        float turn = Mth.wrapDegrees(entity.yBodyRot - state.lastYaw);
        state.lastYaw = entity.yBodyRot;
        state.bank += (Mth.clamp(turn * 0.05F, -0.45F, 0.45F) * state.glide - state.bank) * step;

        float breathe = Mth.sin(time * 0.1F);
        // Each state: the root's x, y, z turns and the hand's fold (a y turn).
        float[] idle = {-0.1F + 0.04F * breathe, 0.3F, 0.2F + 0.03F * breathe, -0.25F};
        float bob = Mth.sin(limbSwing * 0.6662F) * Math.min(1.0F, limbSwingAmount);
        float[] walk = {0.18F + 0.06F * bob, 0.8F - 0.3F * state.sprint, -0.05F + 0.25F * state.sprint, -0.75F + 0.3F * state.sprint};
        float[] fall = {-0.2F, 0.3F, 0.35F + 0.06F * Mth.sin(time * 1.1F), -0.15F + 0.12F * Mth.sin(time * 1.3F)};
        float[] glide = {0.0F, 0.1F, -0.35F + 0.03F * Mth.sin(time * 0.15F), 0.1F};
        float phase = time * (Mth.TWO_PI / 22.0F);
        float[] beat = {0.0F, 0.2F, -0.3F + 0.7F * Mth.sin(phase), 0.3F * Mth.sin(phase - 0.8F)};
        float[] pose = new float[4];
        for (int k = 0; k < 4; k++) {
            float v = Mth.lerp(state.walk, idle[k], walk[k]);
            v = Mth.lerp(state.fall, v, fall[k]);
            v = Mth.lerp(state.glide, v, glide[k]);
            pose[k] = Mth.lerp(state.beat, v, beat[k]);
        }
        for (int i = 0; i < 2; i++) {
            float m = i == 0 ? 1.0F : -1.0F;
            this.roots[i].xRot = pose[0];
            this.roots[i].yRot = pose[1] * m;
            this.roots[i].zRot = pose[2] * m + state.bank;
            this.hands[i].yRot = pose[3] * m;
        }
    }

    @Override
    public void renderToBuffer(PoseStack poseStack, VertexConsumer buffer, int light, int overlay, int color) {
        this.root.render(poseStack, buffer, light, overlay, color);
    }

    private static final class State {
        float time;
        float glide;
        float beat;
        float fall;
        float walk;
        float sprint;
        float bank;
        float lastYaw;

        State(float time) {
            this.time = time;
        }
    }
}
