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
 * <li>the bodice: a dark breastplate, belt and buckle, layered pauldrons, vambraces (the winged bodice's wings are drawn by
 * {@link RegaliaWingsLayer});</li>
 * <li>the robe: navy leggings and a tabard half over each leg, swinging with it;</li>
 * <li>the greaves: dark greaves banded cyan.</li>
 * </ul>
 * The wearer's pose is copied onto it as onto any armour model (NeoForge's {@code copyModelProperties}). (0.43.0-0.80.0 drew her own outfit's boxes in Ice and Fire's texture.)
 */
public final class QueenArmorModels {
    /** Her regalia's sheet (the armour layer draws each piece in it). */
    public static final ResourceLocation TEXTURE = DreadPatch.id("textures/entity/regalia.png");
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

    /** After a resource reload: bake them again from what's loaded now. */
    static void clear() {
        MODELS.clear();
        PLAIN.clear();
    }
}
