package io.github.hammerknife404.dreadpatch.client;

import io.github.hammerknife404.dreadpatch.DreadPatch;
import io.github.hammerknife404.dreadpatch.registry.DreadGear;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.extensions.common.IClientItemExtensions;
import net.neoforged.neoforge.client.extensions.common.RegisterClientExtensionsEvent;

import java.util.HashMap;
import java.util.Map;

/**
 * The Dread Queen's regalia as worn: since 0.81.0 drawn after the owner's pictures ({@link RegaliaGeometry} and
 * {@code textures/entity/regalia.png}, both from {@code tools/models/make_regalia.py}), one model a piece so a slot shows
 * only its own (vanilla shows a whole part per slot: the body for leggings too):
 * <ul>
 * <li>the crown: a dark helm open at the face under a crown of ice spikes;</li>
 * <li>the bodice: a dark breastplate, belt and buckle, frosted pauldrons, vambraces; the winged bodice adds its wings;</li>
 * <li>the robe: navy leggings and a tabard half over each leg, swinging with it;</li>
 * <li>the greaves: dark greaves banded cyan.</li>
 * </ul>
 * The wearer's pose is copied onto it as onto any armour model (NeoForge's {@code copyModelProperties}); the wings spread
 * while the wearer glides and fold behind otherwise. (0.43.0-0.80.0 drew her own outfit's boxes in Ice and Fire's texture.)
 */
public final class QueenArmorModels {
    /** Her regalia's sheet (the armour layer draws each piece in it). */
    public static final ResourceLocation TEXTURE = DreadPatch.id("textures/entity/regalia.png");
    private static final ModelLayerLocation HEAD = layer("head");
    private static final ModelLayerLocation CHEST = layer("chest");
    private static final ModelLayerLocation CHEST_WINGED = layer("chest_winged");
    private static final ModelLayerLocation LEGS = layer("legs");
    private static final ModelLayerLocation FEET = layer("feet");
    private static final Map<ModelLayerLocation, HumanoidModel<LivingEntity>> MODELS = new HashMap<>();
    private static final Map<ModelLayerLocation, HumanoidModel<LivingEntity>> PLAIN = new HashMap<>();

    private QueenArmorModels() {
    }

    private static ModelLayerLocation layer(String name) {
        return new ModelLayerLocation(DreadPatch.id("dread_queen_armor"), name);
    }

    static void onRegisterLayers(EntityRenderersEvent.RegisterLayerDefinitions event) {
        event.registerLayerDefinition(HEAD, RegaliaGeometry::head);
        event.registerLayerDefinition(CHEST, RegaliaGeometry::chest);
        event.registerLayerDefinition(CHEST_WINGED, RegaliaGeometry::chestWinged);
        event.registerLayerDefinition(LEGS, RegaliaGeometry::legs);
        event.registerLayerDefinition(FEET, RegaliaGeometry::feet);
    }

    static void onClientExtensions(RegisterClientExtensionsEvent event) {
        for (var item : DreadGear.regalia()) {
            boolean winged = item == DreadGear.DREAD_QUEEN_WINGED_BODICE;
            event.registerItem(new IClientItemExtensions() {
                @Override
                public HumanoidModel<?> getHumanoidArmorModel(LivingEntity entity, ItemStack stack, EquipmentSlot slot, HumanoidModel<?> original) {
                    ModelLayerLocation at = layerFor(slot, winged);
                    HumanoidModel<LivingEntity> model = MODELS.computeIfAbsent(at, l -> new TrimmedArmorModel(plain(l), slot == EquipmentSlot.LEGS));
                    if (winged) {
                        poseWings(plain(at), entity);
                    }
                    return model;
                }
            }, item.get());
        }
    }

    static ModelLayerLocation layerFor(EquipmentSlot slot, boolean winged) {
        return switch (slot) {
            case HEAD -> HEAD;
            case CHEST -> winged ? CHEST_WINGED : CHEST;
            case LEGS -> LEGS;
            default -> FEET;
        };
    }

    /** The piece's own model (no trim wrapper), as the queen's layer draws it too. */
    static HumanoidModel<LivingEntity> plain(ModelLayerLocation layer) {
        return PLAIN.computeIfAbsent(layer, l -> new HumanoidModel<>(Minecraft.getInstance().getEntityModels().bakeLayer(l)));
    }

    /**
     * The winged bodice's wings (0.81.2, after Cataclysm: Spellbooks' winged Cursium set, at the owner's word): three
     * segments a wing, posed every time the armour layer asks for the model. At rest folded behind like an accordion,
     * breathing (the root lifting and settling 10 degrees about every 1.4 s); gliding spread, tilted back, each segment
     * flexing on its own beat; flying (a creative player's flight) beating. It eases from one to the next over about
     * 6 ticks, each wearer's own. Turns are the right wing's; the left mirrors them.
     */
    static void poseWings(HumanoidModel<?> model, LivingEntity entity) {
        float time = entity.tickCount + Minecraft.getInstance().getTimer().getGameTimeDeltaPartialTick(true);
        boolean flying = entity instanceof net.minecraft.world.entity.player.Player player && player.getAbilities().flying;
        boolean gliding = entity.isFallFlying();
        WingState state = WINGS.computeIfAbsent(entity, e -> new WingState(time));
        float step = Math.max(0.0F, Math.min(1.0F, (time - state.time) * WING_EASE));
        state.time = time;
        state.glide += ((gliding || flying ? 1.0F : 0.0F) - state.glide) * step;
        state.beat += ((flying && !gliding ? 1.0F : 0.0F) - state.beat) * step;
        float[][] rest = {{-0.1F + rad(-10.0F * Mth.sin(time * 0.218F)), 1.1F, -0.2F}, {0.0F, -2.1F + 0.05F * Mth.sin(time * 0.218F), 0.0F}, {0.0F, 1.9F, 0.0F}};
        float[][] glide = {{0.52F + rad(5.0F * Mth.sin(time * 0.131F)), 0.15F + rad(-5.0F * Mth.sin(time * 0.218F)), 0.0F},
                {0.0F, rad(5.0F * Mth.sin(time * 0.131F)), 0.0F}, {0.0F, rad(5.0F * Mth.sin(time * 0.044F)), 0.0F}};
        // A wingbeat about once a second (Cursium's 1.08 s), down and up, each segment lagging the one before.
        float phase = time * (Mth.TWO_PI / 22.0F);
        float[][] beat = {{0.3F, 0.15F, rad(35.0F * Mth.sin(phase))}, {0.0F, 0.0F, rad(25.0F * Mth.sin(phase - 0.6F))}, {0.0F, 0.0F, rad(20.0F * Mth.sin(phase - 1.2F))}};
        String[] segments = {"", "_mid", "_tip"};
        ModelPart right = model.body;
        ModelPart left = model.body;
        for (int i = 0; i < 3; i++) {
            right = right.getChild("right_wing" + segments[i]);
            left = left.getChild("left_wing" + segments[i]);
            float x = Mth.lerp(state.beat, Mth.lerp(state.glide, rest[i][0], glide[i][0]), beat[i][0]);
            float y = Mth.lerp(state.beat, Mth.lerp(state.glide, rest[i][1], glide[i][1]), beat[i][1]);
            float z = Mth.lerp(state.beat, Mth.lerp(state.glide, rest[i][2], glide[i][2]), beat[i][2]);
            right.setRotation(x, y, z);
            left.setRotation(x, -y, -z);
        }
    }

    private static float rad(float degrees) {
        return degrees * Mth.DEG_TO_RAD;
    }

    /** How fast the wings ease between poses: this share of the way a tick. */
    static final float WING_EASE = 0.18F;
    /** Each wearer's wings: how far toward gliding and toward beating, and when last posed. */
    private static final Map<LivingEntity, WingState> WINGS = new java.util.WeakHashMap<>();

    private static final class WingState {
        float glide;
        float beat;
        float time;

        WingState(float time) {
            this.time = time;
        }
    }

    /** After a resource reload: bake them again from what's loaded now. */
    static void clear() {
        MODELS.clear();
        PLAIN.clear();
    }
}
