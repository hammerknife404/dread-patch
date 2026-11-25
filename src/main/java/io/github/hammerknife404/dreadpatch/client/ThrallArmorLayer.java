package io.github.hammerknife404.dreadpatch.client;

import com.iafenvoy.iceandfire.entity.DreadThrallEntity;
import com.iafenvoy.iceandfire.registry.IafEntities;
import com.iafenvoy.iceandfire.render.entity.feature.BipedArmorFeatureRenderer;
import com.iafenvoy.iceandfire.render.model.DreadThrallModel;
import com.iafenvoy.uranus.client.model.basic.BasicModelPart;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.logging.LogUtils;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.HumanoidArmorLayer;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterials;
import net.minecraft.world.item.ItemStack;
import net.neoforged.fml.util.ObfuscationReflectionHelper;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import org.slf4j.Logger;

import java.util.List;

/**
 * A dread thrall's armour as itself (0.70.0, the owner, of a guard raised in her storm: "Weapons and shields are left
 * equiped. Armor drops on the ground"). Ice and Fire's thrall renderer draws whatever armour a thrall wears in its own
 * thrall textures ({@code BipedArmorFeatureRendererMultiple}: its chainmail variants), so the iron a guard rose in
 * looked like any thrall's rags while the guard's own dropped at its feet. This takes that layer's place on the thrall
 * renderer: chainmail (what Ice and Fire's own thralls wear) still goes to Ice and Fire's layer; any other armour is
 * drawn by vanilla's armour layer, on a humanoid model posed as the thrall's body, so it looks as it does on a player
 * (our recoloured dread dragonsteel too, through its client extensions).
 * <p>
 * Each layer sees only its own pieces: the others are taken out of the thrall's armour list for the draw and put back
 * straight after, on the render thread, so nothing of the entity changes.
 */
public final class ThrallArmorLayer extends RenderLayer<DreadThrallEntity, DreadThrallModel> {
    private static final Logger LOGGER = LogUtils.getLogger();

    private final RenderLayer<DreadThrallEntity, DreadThrallModel> theirs;
    private final HumanoidModel<DreadThrallEntity> posed;
    private final HumanoidArmorLayer<DreadThrallEntity, HumanoidModel<DreadThrallEntity>, HumanoidModel<DreadThrallEntity>> real;
    private final ItemStack[] held = new ItemStack[4];

    private ThrallArmorLayer(LivingEntityRenderer<DreadThrallEntity, DreadThrallModel> renderer, RenderLayer<DreadThrallEntity, DreadThrallModel> theirs,
                             EntityRenderersEvent.AddLayers event) {
        super(renderer);
        this.theirs = theirs;
        var context = event.getContext();
        this.posed = new HumanoidModel<>(context.bakeLayer(ModelLayers.ZOMBIE));
        RenderLayerParent<DreadThrallEntity, HumanoidModel<DreadThrallEntity>> parent = new RenderLayerParent<>() {
            @Override
            public HumanoidModel<DreadThrallEntity> getModel() {
                return ThrallArmorLayer.this.posed;
            }

            @Override
            public ResourceLocation getTextureLocation(DreadThrallEntity entity) {
                return renderer.getTextureLocation(entity);
            }
        };
        this.real = new HumanoidArmorLayer<>(parent, new HumanoidModel<>(context.bakeLayer(ModelLayers.ZOMBIE_INNER_ARMOR)),
                new HumanoidModel<>(context.bakeLayer(ModelLayers.ZOMBIE_OUTER_ARMOR)), context.getModelManager());
    }

    /** In place of Ice and Fire's armour layer on the thrall's renderer. */
    @SuppressWarnings("unchecked")
    static void onAddLayers(EntityRenderersEvent.AddLayers event) {
        if (!(event.getRenderer(IafEntities.DREAD_THRALL.get()) instanceof LivingEntityRenderer<?, ?> found)) {
            return;
        }
        LivingEntityRenderer<DreadThrallEntity, DreadThrallModel> renderer = (LivingEntityRenderer<DreadThrallEntity, DreadThrallModel>) found;
        try {
            List<RenderLayer<DreadThrallEntity, DreadThrallModel>> layers =
                    ObfuscationReflectionHelper.getPrivateValue(LivingEntityRenderer.class, renderer, "layers");
            for (int i = 0; i < layers.size(); i++) {
                if (layers.get(i) instanceof BipedArmorFeatureRenderer<?, ?, ?>) {
                    layers.set(i, new ThrallArmorLayer(renderer, layers.get(i), event));
                    return;
                }
            }
            LOGGER.warn("Dread Patch: no armour layer on Ice and Fire's thrall renderer; thralls' armour stays as it draws it");
        } catch (RuntimeException e) {
            LOGGER.warn("Dread Patch: thralls' armour stays as Ice and Fire draws it: {}", e.toString());
        }
    }

    /** Drawn as itself: armour that isn't chainmail (which Ice and Fire's thralls wear, in their own look). */
    static boolean asItself(ItemStack stack) {
        return stack.getItem() instanceof ArmorItem armor && armor.getMaterial().value() != ArmorMaterials.CHAIN.value();
    }

    @Override
    @SuppressWarnings("unchecked")
    public void render(PoseStack pose, MultiBufferSource buffers, int light, DreadThrallEntity thrall, float limbSwing, float limbSwingAmount,
                       float partialTick, float ageInTicks, float netHeadYaw, float headPitch) {
        if (!(thrall.getArmorSlots() instanceof List<?> list) || list.size() != this.held.length) {
            this.theirs.render(pose, buffers, light, thrall, limbSwing, limbSwingAmount, partialTick, ageInTicks, netHeadYaw, headPitch);
            return;
        }
        List<ItemStack> armor = (List<ItemStack>) list;
        boolean anyReal = false;
        boolean anyTheirs = false;
        for (int i = 0; i < this.held.length; i++) {
            this.held[i] = armor.get(i);
            anyReal |= asItself(this.held[i]);
            anyTheirs |= !this.held[i].isEmpty() && !asItself(this.held[i]);
        }
        try {
            if (anyTheirs) {
                this.keep(armor, false);
                this.theirs.render(pose, buffers, light, thrall, limbSwing, limbSwingAmount, partialTick, ageInTicks, netHeadYaw, headPitch);
            }
            if (anyReal) {
                this.keep(armor, true);
                this.pose(this.getParentModel());
                this.real.render(pose, buffers, light, thrall, limbSwing, limbSwingAmount, partialTick, ageInTicks, netHeadYaw, headPitch);
            }
        } finally {
            for (int i = 0; i < this.held.length; i++) {
                armor.set(i, this.held[i]);
            }
        }
    }

    /** Leaves in the list only the pieces drawn as themselves ({@code real}), or only the others. */
    private void keep(List<ItemStack> armor, boolean real) {
        for (int i = 0; i < this.held.length; i++) {
            armor.set(i, asItself(this.held[i]) == real ? this.held[i] : ItemStack.EMPTY);
        }
    }

    /** The humanoid model in the thrall's pose: its parts are each at the model's root, as a humanoid's are. */
    private void pose(DreadThrallModel from) {
        copy(from.head, this.posed.head);
        copy(from.head, this.posed.hat);
        copy(from.body, this.posed.body);
        copy(from.armRight, this.posed.rightArm);
        copy(from.armLeft, this.posed.leftArm);
        copy(from.legRight, this.posed.rightLeg);
        copy(from.legLeft, this.posed.leftLeg);
        this.posed.crouching = from.isSneak;
        this.posed.young = false;
        this.posed.attackTime = from.attackTime;
        this.posed.riding = from.riding;
    }

    private static void copy(BasicModelPart from, ModelPart to) {
        to.x = from.rotationPointX;
        to.y = from.rotationPointY;
        to.z = from.rotationPointZ;
        to.xRot = from.rotateAngleX;
        to.yRot = from.rotateAngleY;
        to.zRot = from.rotateAngleZ;
    }
}
