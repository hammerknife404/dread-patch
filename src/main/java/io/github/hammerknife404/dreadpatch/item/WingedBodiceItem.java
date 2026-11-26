package io.github.hammerknife404.dreadpatch.item;

import net.minecraft.core.Holder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.ItemStack;

/**
 * Her winged bodice (0.81.0, the owner's pictures: "the optional elytra version crafted with the elytra, armor and
 * rimefrost or ice dragon scale"): her bodice in every way (a piece of her regalia, counted in her set), and it glides as
 * an elytra does (NeoForge's {@code canElytraFly}), its wings drawn by {@code client/QueenArmorModels}. Her regalia is
 * unbreakable, so its flight never wears it, nor runs out.
 */
public class WingedBodiceItem extends DreadArmorItem {
    public WingedBodiceItem(Holder<ArmorMaterial> material, Properties properties) {
        super(material, Type.CHESTPLATE, Kind.QUEEN, properties);
    }

    @Override
    public boolean canElytraFly(ItemStack stack, LivingEntity entity) {
        return true;
    }

    @Override
    public boolean elytraFlightTick(ItemStack stack, LivingEntity entity, int flightTicks) {
        return true;
    }
}
