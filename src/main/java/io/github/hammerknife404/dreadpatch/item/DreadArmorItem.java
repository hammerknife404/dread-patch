package io.github.hammerknife404.dreadpatch.item;

import net.minecraft.ChatFormatting;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.function.BiFunction;

/**
 * Rimefrost dragonscale and dread dragonsteel armour (0.42.0): Ice and Fire's ice dragonscale and ice
 * dragonsteel sets, Dread. Their numbers are Ice and Fire's ({@code DreadGear}); they're drawn with Ice
 * and Fire's own ice armour models in textures recoloured from its own as they load
 * ({@link #textures}, set by the client); against dragon breath, Ice and Fire's and a Rimefrost
 * dragon's, each piece takes off as much as Ice and Fire's do ({@link DragonArmor}).
 */
public class DreadArmorItem extends ArmorItem {
    public enum Kind {
        /** From Ice and Fire's white dragonscale armour texture. */
        SCALE("dragon_scales_white"),
        /** From its ice dragonsteel armour texture. */
        STEEL("dragonsteel_ice");

        /** The Ice and Fire armour texture it's recoloured from ({@code textures/models/armor/<this>_layer_1|2}). */
        public final String iceAndFireTexture;

        Kind(String iceAndFireTexture) {
            this.iceAndFireTexture = iceAndFireTexture;
        }
    }

    /** Set by the client: where a kind's recoloured armour texture is (inner, the leggings' layer). Unset, the material's own. */
    public static BiFunction<Kind, Boolean, ResourceLocation> textures = (kind, inner) -> null;

    private final Kind kind;

    public DreadArmorItem(Holder<ArmorMaterial> material, Type type, Kind kind, Properties properties) {
        super(material, type, properties);
        this.kind = kind;
    }

    public Kind kind() {
        return this.kind;
    }

    @Nullable
    @Override
    public ResourceLocation getArmorTexture(ItemStack stack, Entity entity, EquipmentSlot slot, ArmorMaterial.Layer layer, boolean innerModel) {
        ResourceLocation texture = textures.apply(this.kind, innerModel);
        return texture != null ? texture : super.getArmorTexture(stack, entity, slot, layer, innerModel);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, context, tooltip, flag);
        tooltip.add(Component.translatable("tooltip.dreadpatch.dragon_armor", Math.round(DragonArmor.share(this.getEquipmentSlot()) * 100))
                .withStyle(ChatFormatting.AQUA));
    }
}
