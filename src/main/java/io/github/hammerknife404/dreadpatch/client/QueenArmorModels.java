package io.github.hammerknife404.dreadpatch.client;

import io.github.hammerknife404.dreadpatch.DreadPatch;
import io.github.hammerknife404.dreadpatch.registry.DreadGear;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.extensions.common.IClientItemExtensions;
import net.neoforged.neoforge.client.extensions.common.RegisterClientExtensionsEvent;

import java.util.EnumMap;
import java.util.Map;

/**
 * The Dread Queen's regalia as worn (0.43.0, the owner: "replicate as close as possible"): her own
 * outfit's boxes from {@link DreadQueenModel}, at the same texture offsets, in her own Ice and Fire
 * texture, one model a piece so a slot shows only its own (vanilla shows a whole part per slot: the
 * body for leggings too):
 * <ul>
 * <li>the crown: her headware, the ice crown;</li>
 * <li>the bodice: her chestplate with her bust and necklace, her cloak, and her sleeves (silver pauldrons);</li>
 * <li>the robe: her robe over each leg;</li>
 * <li>the greaves: her legs' dark banded wraps, over the wearer's legs.</li>
 * </ul>
 * The wearer's pose is copied onto it as onto any armour model (NeoForge's {@code copyModelProperties}).
 */
public final class QueenArmorModels {
    /*
     * Her boxes as they are, but for fitting over a player's skin, whose outer layers stand 0.5 px off
     * the head and 0.25 px off the body and limbs: an armour face lying on one flickers. So the crown
     * stands 0.1 px past the hat layer, the pauldrons (her sleeves) 0.3 px out, the greaves 0.35 px
     * (past the trouser layer), and the robe 0.5 px wider so it still covers the greaves.
     */
    private static final CubeDeformation CROWN_FIT = new CubeDeformation(0.1F);
    private static final CubeDeformation PAULDRON_FIT = new CubeDeformation(0.3F);
    private static final CubeDeformation GREAVES_FIT = new CubeDeformation(0.35F);
    private static final CubeDeformation ROBE_FIT = new CubeDeformation(0.5F, 0.0F, 0.0F);

    private static final Map<EquipmentSlot, ModelLayerLocation> LAYERS = new EnumMap<>(EquipmentSlot.class);
    private static final Map<EquipmentSlot, HumanoidModel<LivingEntity>> MODELS = new EnumMap<>(EquipmentSlot.class);

    static {
        LAYERS.put(EquipmentSlot.HEAD, new ModelLayerLocation(DreadPatch.id("dread_queen_armor"), "head"));
        LAYERS.put(EquipmentSlot.CHEST, new ModelLayerLocation(DreadPatch.id("dread_queen_armor"), "chest"));
        LAYERS.put(EquipmentSlot.LEGS, new ModelLayerLocation(DreadPatch.id("dread_queen_armor"), "legs"));
        LAYERS.put(EquipmentSlot.FEET, new ModelLayerLocation(DreadPatch.id("dread_queen_armor"), "feet"));
    }

    private QueenArmorModels() {
    }

    static void onRegisterLayers(EntityRenderersEvent.RegisterLayerDefinitions event) {
        for (Map.Entry<EquipmentSlot, ModelLayerLocation> layer : LAYERS.entrySet()) {
            event.registerLayerDefinition(layer.getValue(), () -> createLayer(layer.getKey()));
        }
    }

    static void onClientExtensions(RegisterClientExtensionsEvent event) {
        for (var item : DreadGear.queenArmor()) {
            event.registerItem(new IClientItemExtensions() {
                @Override
                public HumanoidModel<?> getHumanoidArmorModel(LivingEntity entity, ItemStack stack, EquipmentSlot slot, HumanoidModel<?> original) {
                    return MODELS.computeIfAbsent(slot, s -> new TrimmedArmorModel(new HumanoidModel<>(Minecraft.getInstance().getEntityModels().bakeLayer(LAYERS.get(s))), s == EquipmentSlot.LEGS));
                }
            }, item.get());
        }
    }

    /** After a resource reload: bake them again from what's loaded now. */
    static void clear() {
        MODELS.clear();
    }

    /** A humanoid with no cubes of its own (vanilla's model needs every part), carrying only {@code slot}'s piece of her outfit. */
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
}
