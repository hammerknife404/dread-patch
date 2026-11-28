package io.github.hammerknife404.dreadpatch.client;

import io.github.hammerknife404.dreadpatch.DreadPatch;
import io.github.hammerknife404.dreadpatch.registry.DreadGear;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
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
 * <li>the bodice: a dark breastplate, belt and buckle, layered pauldrons, vambraces (the winged bodice's wings are drawn by
 * {@link RegaliaWingsLayer});</li>
 * <li>the robe: navy leggings and a tabard half over each leg, swinging with it;</li>
 * <li>the greaves: dark greaves banded cyan.</li>
 * </ul>
 * The wearer's pose is copied onto it as onto any armour model (NeoForge's {@code copyModelProperties}). (0.43.0-0.80.0 drew her own outfit's boxes in Ice and Fire's texture: since 0.98.7 the classic look again, by default, {@link DreadPatchClientConfig}.)
 */
public final class QueenArmorModels {
    /** Her regalia's sheet (the armour layer draws each piece in it). */
    public static final ResourceLocation TEXTURE = DreadPatch.id("textures/entity/regalia.png");
    /** The classic look's (0.98.7; 0.43.0-0.80.0): her own Ice and Fire texture. */
    public static final ResourceLocation CLASSIC_TEXTURE = ResourceLocation.fromNamespaceAndPath("iceandfire", "textures/entity/dread/dread_queen.png");
    private static final java.util.Map<EquipmentSlot, ModelLayerLocation> CLASSIC = new java.util.EnumMap<>(EquipmentSlot.class);

    static {
        for (EquipmentSlot slot : new EquipmentSlot[]{EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET}) {
            CLASSIC.put(slot, new ModelLayerLocation(DreadPatch.id("dread_queen_armor_classic"), slot.getName()));
        }
    }

    private static final CubeDeformation CROWN_FIT = new CubeDeformation(0.1F);
    private static final CubeDeformation PAULDRON_FIT = new CubeDeformation(0.3F);
    private static final CubeDeformation GREAVES_FIT = new CubeDeformation(0.35F);
    private static final CubeDeformation ROBE_FIT = new CubeDeformation(0.5F, 0.0F, 0.0F);


    private static final ModelLayerLocation HEAD = layer("head");
    private static final ModelLayerLocation CHEST = layer("chest");
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
        event.registerLayerDefinition(RegaliaWingsModel.LAYER, RegaliaWingsGeometry::create);
        event.registerLayerDefinition(LEGS, RegaliaGeometry::legs);
        event.registerLayerDefinition(FEET, RegaliaGeometry::feet);
        for (var classic : CLASSIC.entrySet()) {
            event.registerLayerDefinition(classic.getValue(), () -> createLayer(classic.getKey()));
        }
    }

    /** The texture its pieces are drawn in: the classic look's or the redrawn one's ({@link DreadPatchClientConfig}). */
    public static ResourceLocation texture() {
        return DreadPatchClientConfig.classicArmor() ? CLASSIC_TEXTURE : TEXTURE;
    }

    static void onClientExtensions(RegisterClientExtensionsEvent event) {
        for (var item : DreadGear.regalia()) {
            boolean winged = item == DreadGear.DREAD_QUEEN_WINGED_BODICE;
            event.registerItem(new IClientItemExtensions() {
                @Override
                public HumanoidModel<?> getHumanoidArmorModel(LivingEntity entity, ItemStack stack, EquipmentSlot slot, HumanoidModel<?> original) {
                    ModelLayerLocation at = layerFor(slot, winged);
                    return MODELS.computeIfAbsent(at, l -> new TrimmedArmorModel(plain(l), slot == EquipmentSlot.LEGS));
                }
            }, item.get());
        }
    }

    static ModelLayerLocation layerFor(EquipmentSlot slot, boolean winged) {
        if (DreadPatchClientConfig.classicArmor()) {
            return CLASSIC.get(slot == EquipmentSlot.HEAD || slot == EquipmentSlot.CHEST || slot == EquipmentSlot.LEGS ? slot : EquipmentSlot.FEET);
        }
        return switch (slot) {
            case HEAD -> HEAD;
            case CHEST -> CHEST;  // the winged bodice's wings are RegaliaWingsLayer's (0.82.0)
            case LEGS -> LEGS;
            default -> FEET;
        };
    }

    /** The piece's own model (no trim wrapper), as the queen's layer draws it too. */
    static HumanoidModel<LivingEntity> plain(ModelLayerLocation layer) {
        return PLAIN.computeIfAbsent(layer, l -> new HumanoidModel<>(Minecraft.getInstance().getEntityModels().bakeLayer(l)));
    }

    /**
     * The classic look's piece (0.43.0-0.80.0, back as an option in 0.98.7): a humanoid with no cubes of its own (vanilla's
     * model needs every part), carrying only {@code slot}'s piece of her outfit, bust and all.
     */
    static LayerDefinition createLayer(EquipmentSlot slot) {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        PartDefinition head = root.addOrReplaceChild("head", CubeListBuilder.create(), PartPose.ZERO);
        root.addOrReplaceChild("hat", CubeListBuilder.create(), PartPose.ZERO);
        PartDefinition body = root.addOrReplaceChild("body", CubeListBuilder.create(), PartPose.ZERO);
        PartDefinition rightArm = root.addOrReplaceChild("right_arm", CubeListBuilder.create(), PartPose.offset(-5.0F, 2.0F, 0.0F));
        PartDefinition leftArm = root.addOrReplaceChild("left_arm", CubeListBuilder.create(), PartPose.offset(5.0F, 2.0F, 0.0F));
        PartDefinition rightLeg = root.addOrReplaceChild("right_leg", CubeListBuilder.create(), PartPose.offset(-1.9F, 12.0F, 0.0F));
        PartDefinition leftLeg = root.addOrReplaceChild("left_leg", CubeListBuilder.create(), PartPose.offset(1.9F, 12.0F, 0.0F));
        switch (slot) {
            case HEAD -> head.addOrReplaceChild("headware",
                    CubeListBuilder.create().texOffs(58, 0).addBox(-4.5F, -12.5F, -4.5F, 9.0F, 13.0F, 9.0F, CROWN_FIT),
                    PartPose.offset(0.0F, 1.0F, 0.0F));
            case CHEST -> {
                PartDefinition chestplate = body.addOrReplaceChild("chestplate",
                        CubeListBuilder.create().texOffs(1, 32).addBox(-4.5F, -0.3F, -2.5F, 9.0F, 12.0F, 5.0F),
                        PartPose.offset(0.0F, 0.1F, 0.0F));
                chestplate.addOrReplaceChild("right_bust",
                        CubeListBuilder.create().texOffs(3, 39).addBox(-2.0F, 0.0F, -3.0F, 4.0F, 4.0F, 3.0F),
                        PartPose.offsetAndRotation(-2.4F, 1.9F, -1.5F, DreadQueenModel.BUST_TILT, DreadQueenModel.BUST_SPLAY, 0.0F));
                chestplate.addOrReplaceChild("left_bust",
                        CubeListBuilder.create().texOffs(3, 39).mirror().addBox(-2.0F, 0.0F, -3.0F, 4.0F, 4.0F, 3.0F),
                        PartPose.offsetAndRotation(2.4F, 1.9F, -1.5F, DreadQueenModel.BUST_TILT, -DreadQueenModel.BUST_SPLAY, 0.0F));
                chestplate.addOrReplaceChild("necklace",
                        CubeListBuilder.create().texOffs(1, 52).addBox(-4.5F, -0.3F, -2.8F, 9.0F, 7.0F, 5.0F),
                        PartPose.offsetAndRotation(0.0F, 0.1F, 0.0F, 0.032114058F, 0.0F, 0.0F));
                body.addOrReplaceChild("cloak",
                        CubeListBuilder.create().texOffs(81, 37).addBox(-4.5F, 0.0F, 2.3F, 9.0F, 21.0F, 1.0F),
                        PartPose.offsetAndRotation(0.0F, 0.1F, 0.0F, 0.045553094F, 0.0F, 0.0F));
                rightArm.addOrReplaceChild("right_sleeve",
                        CubeListBuilder.create().texOffs(36, 33).addBox(-4.5F, -2.1F, -2.4F, 5.0F, 6.0F, 5.0F, PAULDRON_FIT),
                        PartPose.offset(0.5F, -0.1F, 0.0F));
                leftArm.addOrReplaceChild("left_sleeve",
                        CubeListBuilder.create().texOffs(36, 33).mirror().addBox(-0.5F, -2.1F, -2.4F, 5.0F, 6.0F, 5.0F, PAULDRON_FIT),
                        PartPose.offset(-0.5F, -0.1F, 0.0F));
            }
            case LEGS -> {
                rightLeg.addOrReplaceChild("right_robe",
                        CubeListBuilder.create().texOffs(58, 33).mirror().addBox(-2.1F, 0.0F, -2.5F, 4.0F, 7.0F, 5.0F, ROBE_FIT),
                        PartPose.offset(0.0F, -0.2F, 0.0F));
                leftLeg.addOrReplaceChild("left_robe",
                        CubeListBuilder.create().texOffs(58, 33).addBox(-1.9F, 0.0F, -2.5F, 4.0F, 7.0F, 5.0F, ROBE_FIT),
                        PartPose.offset(0.0F, -0.2F, 0.0F));
            }
            default -> {
                rightLeg.addOrReplaceChild("right_wraps",
                        CubeListBuilder.create().texOffs(0, 16).addBox(-2.0F, 0.0F, -2.0F, 4.0F, 12.0F, 4.0F, GREAVES_FIT),
                        PartPose.ZERO);
                leftLeg.addOrReplaceChild("left_wraps",
                        CubeListBuilder.create().texOffs(0, 16).mirror().addBox(-2.0F, 0.0F, -2.0F, 4.0F, 12.0F, 4.0F, GREAVES_FIT),
                        PartPose.ZERO);
            }
        }
        return LayerDefinition.create(mesh, 128, 64);
    }

    /** After a resource reload: bake them again from what's loaded now. */
    static void clear() {
        MODELS.clear();
        PLAIN.clear();
    }
}
