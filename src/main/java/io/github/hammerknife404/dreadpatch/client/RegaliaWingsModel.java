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
 * <li>idle: folded like a perched dragon's, tucked down the back, breathing slowly (0.82.2);</li>
 * <li>walking: half folded, bobbing with the steps, flaring open when sprinting;</li>
 * <li>falling: pushed up and trailing against the air, the hands fluttering;</li>
 * <li>gliding: spread in a shallow V, banking into turns (a roll from how fast the wearer turns), and by how it flies
 * (0.82.2): swept back and tucked with speed, tighter diving, opened up and beating when climbing or slow, a flutter
 * growing with speed;</li>
 * <li>a creative player's flight: beating.</li>
 * </ul>
 * Turns are the right wing's; the left mirrors them (y and z negated), but banking rolls both the same way.
 */
public final class RegaliaWingsModel extends Model {
    public static final ModelLayerLocation LAYER = new ModelLayerLocation(DreadPatch.id("regalia_wings"), "main");
    /** Faster than this (blocks a tick) a glide sweeps the wings back, and trails frost: above a glide's cruise. */
    public static final float FAST = 1.6F;
    /** How fast it eases toward a state: this share of the way a tick. */
    static final float EASE = 0.15F;

    private final ModelPart root;
    private final ModelPart[] roots = new ModelPart[2];
    private final ModelPart[] hands = new ModelPart[2];
    private final ModelPart[] arms = new ModelPart[2];
    private final Map<LivingEntity, State> states = new WeakHashMap<>();

    public RegaliaWingsModel(ModelPart root) {
        super(RenderType::entityTranslucent);
        this.root = root;
        String[] names = {"right_wing", "left_wing"};
        for (int i = 0; i < 2; i++) {
            this.roots[i] = root.getChild(names[i]);
            this.arms[i] = this.roots[i].getChild(names[i] + "_arm");
            this.hands[i] = this.arms[i].getChild(names[i] + "_hand");
        }
    }

    /** Its pose for this wearer now: {@code time} in ticks, partial ticks in. */
    public void setupAnim(LivingEntity entity, float limbSwing, float limbSwingAmount, float time) {
        this.root.getAllParts().forEach(ModelPart::resetPose);
        State state = this.states.computeIfAbsent(entity, e -> new State(time));
        // Its clock only goes forward (0.98.10, the owner: in the inventory the wings looked doubled and lagged; not in F5):
        // the inventory draws its wearer at a partial tick of 1 while the world behind draws it at the real one, so each
        // frame the clock ran back and forth, the flap's phase jittering between two poses and the easing never settling.
        // A drawing behind the clock now takes the state as it is.
        boolean ahead = time > state.time;
        float step = ahead ? Mth.clamp((time - state.time) * EASE, 0.0F, 1.0F) : 0.0F;
        if (ahead) {
            state.time = time;
        }
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
        float turn = ahead ? Mth.wrapDegrees(entity.yBodyRot - state.lastYaw) : 0.0F;
        if (ahead) {
            state.lastYaw = entity.yBodyRot;
        }
        state.bank += (Mth.clamp(turn * 0.05F, -0.45F, 0.45F) * state.glide - state.bank) * step;

        float breathe = Mth.sin(time * 0.1F);
        // Each state: the root's x, y, z turns and the hand's fold (a y turn).
        // Idle (0.82.2, the owner: "fold/collapse in more, like a dragons wings when sitting"): the arm bones tucked down
        // the back, the wrists up at the shoulders, the hands folded down behind, the membranes draped; breathing.
        // 0.98.7 (the owner: "Allow the regalia wings a small amount of idle animation when the player is not gliding"):
        // the breathing deeper, and now and then (about every 9 s, for about a second) a resettle, a small shrug and refold.
        float settle = idleSettle(time);
        float[] idle = {0.1F + 0.06F * breathe + 0.06F * settle, 1.1F - 0.12F * settle, 0.1F + 0.05F * breathe + 0.1F * settle,
                -2.0F + 0.1F * breathe + 0.35F * settle};
        float bob = Mth.sin(limbSwing * 0.6662F) * Math.min(1.0F, limbSwingAmount);
        // Walking: half folded, bobbing with the steps; sprinting, flaring open.
        float[] walk = {0.15F + 0.06F * bob, 1.0F - 0.4F * state.sprint, 0.0F + 0.2F * state.sprint, -1.7F + 0.9F * state.sprint};
        float[] fall = {-0.2F, 0.3F, 0.35F + 0.06F * Mth.sin(time * 1.1F), -0.15F + 0.12F * Mth.sin(time * 1.3F)};

        // Gliding, by how it's flying (0.82.2, the owner: "more flight animations in general, dynamic with speeds and
        // tilt"): faster, swept back and tucked; diving, tucked tighter; climbing or slow, opened up and beating to hold
        // the air (harder the slower); a flutter growing with speed.
        net.minecraft.world.phys.Vec3 motion = entity.getDeltaMovement();
        float speed = (float) motion.length();
        state.speed += (speed - state.speed) * step;
        state.climb += ((speed > 0.05F ? Mth.clamp((float) motion.y / speed, -1.0F, 1.0F) : 0.0F) - state.climb) * step;
        // 0.82.5 (the owner: "the wings still stay folded back at all glide speeds"): a glide cruises at about 1-1.5 blocks
        // a tick, sinking gently, so the sweep starts past that (a firework's speed) and only a steep dive tucks them.
        float sweep = Mth.clamp((state.speed - FAST) / 1.0F, 0.0F, 1.0F);
        float dive = Mth.clamp((-state.climb - 0.4F) * 2.0F, 0.0F, 1.0F);
        float climb = Mth.clamp(state.climb * 3.0F, 0.0F, 1.0F);
        float slow = Mth.clamp(1.0F - state.speed / 1.0F, 0.0F, 1.0F);
        // Cresting up and slowing, they open right out to catch the air (0.82.3, the owner: "spread out and open up more
        // when gliding and the player is cresting up and slowing down").
        float open = Math.max(climb, slow);
        float flap = 0.75F * Math.max(slow, 0.7F * climb) * state.glide;
        if (time > state.flapTime) {
            state.flapPhase += (time - state.flapTime) * (0.3F + 0.25F * flap);
            state.flapTime = time;
        }
        float beatGlide = flap * Mth.sin(state.flapPhase);
        // The stroke (0.82.4, the owner: "less up and down to the grownd but to the players body", asked: both): it sweeps
        // the wings back toward the wearer's back and out again (a y turn on the body), a little lift with it; all of it
        // on the body, so it pitches and rolls with the wearer.
        float[] glide = {0.6F - 0.25F * dive - 0.25F * open, 0.1F + 0.7F * sweep * (1.0F - open) + 0.4F * dive - 0.4F * open + 0.55F * beatGlide,
                -0.35F + 0.25F * open + 0.15F * beatGlide + 0.03F * Mth.sin(time * 0.15F),
                0.1F - 0.6F * sweep * (1.0F - open) - 0.5F * dive + 0.5F * open + 0.3F * flap * Mth.sin(state.flapPhase - 0.9F)};
        float flutter = 0.05F * Math.min(1.0F, state.speed) * Mth.sin(time * 2.3F) * state.glide;
        // Lifted (0.82.6, the owner: "The wings should go up/out a bit more during slowing or slower banking", the wrist
        // circled): slowing, or banking at less than a fast glide's speed, raises the shoulders and the arm bones, the
        // wrists going up and out.
        float lift = state.glide * Mth.clamp(open + 1.5F * Math.abs(state.bank) * (1.0F - sweep), 0.0F, 1.0F);
        float phase = time * (Mth.TWO_PI / 22.0F);
        float[] beat = {0.0F, 0.35F + 0.7F * Mth.sin(phase), -0.3F + 0.15F * Mth.sin(phase), 0.3F * Mth.sin(phase - 0.8F)};
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
            this.hands[i].zRot += flutter * m;
            this.roots[i].zRot += 0.3F * lift * m;
            this.roots[i].yRot -= 0.2F * lift * m;
            this.arms[i].zRot += 0.25F * lift * m;
            this.hands[i].zRot -= 0.25F * lift * m;
        }
    }

    /** The idle resettle's strength at {@code time} (ticks): 0 most of the time, rising to 1 for about a second every 9 or so. */
    static float idleSettle(float time) {
        float wave = Mth.sin(time * 0.035F);
        return wave <= 0.0F ? 0.0F : (float) Math.pow(wave, 12.0);
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
        float speed;
        float climb;
        float flapPhase;
        float flapTime;

        State(float time) {
            this.time = time;
        }
    }
}
