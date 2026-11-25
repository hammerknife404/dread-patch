package io.github.hammerknife404.dreadpatch;

import io.github.hammerknife404.dreadpatch.registry.DreadGear;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.resources.ResourceKey;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

/**
 * Dev runs only (the owner, 2026-11-24: "make it so the dev client start me with the full dread queen set + a hydra
 * heart - this is for dev only right now"; then, the same day: "Instead give the player a full set of Dread dragonsteel
 * armor and a diamond sword + hydra heart. Give each armor piece the general best enchantments for each piece and the
 * sword"): the first time a player joins a world in a dev run, they're given dread dragonsteel worn, a diamond sword in
 * hand, each enchanted as is usual at best, and Ice and Fire's hydra heart. {@link FMLEnvironment#production} is
 * true in any built jar, so a release never registers it.
 */
final class DevKit {
    private static final String GIVEN = "dreadpatch:dev_kit";

    private DevKit() {
    }

    static void register(IEventBus gameBus) {
        if (!FMLEnvironment.production) {
            gameBus.addListener(DevKit::onLogin);
        }
    }

    private static void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        CompoundTag kept = player.getPersistentData().getCompound(Player.PERSISTED_NBT_TAG);
        if (kept.getBoolean(GIVEN)) {
            return;
        }
        kept.putBoolean(GIVEN, true);
        player.getPersistentData().put(Player.PERSISTED_NBT_TAG, kept);
        Registry<Enchantment> enchantments = player.registryAccess().registryOrThrow(Registries.ENCHANTMENT);
        wear(player, EquipmentSlot.HEAD, enchanted(enchantments, new ItemStack(DreadGear.DREAD_DRAGONSTEEL_HELMET.get()),
                Enchantments.PROTECTION, 4, Enchantments.UNBREAKING, 3, Enchantments.MENDING, 1, Enchantments.RESPIRATION, 3,
                Enchantments.AQUA_AFFINITY, 1));
        wear(player, EquipmentSlot.CHEST, enchanted(enchantments, new ItemStack(DreadGear.DREAD_DRAGONSTEEL_CHESTPLATE.get()),
                Enchantments.PROTECTION, 4, Enchantments.UNBREAKING, 3, Enchantments.MENDING, 1));
        wear(player, EquipmentSlot.LEGS, enchanted(enchantments, new ItemStack(DreadGear.DREAD_DRAGONSTEEL_LEGGINGS.get()),
                Enchantments.PROTECTION, 4, Enchantments.UNBREAKING, 3, Enchantments.MENDING, 1, Enchantments.SWIFT_SNEAK, 3));
        wear(player, EquipmentSlot.FEET, enchanted(enchantments, new ItemStack(DreadGear.DREAD_DRAGONSTEEL_BOOTS.get()),
                Enchantments.PROTECTION, 4, Enchantments.UNBREAKING, 3, Enchantments.MENDING, 1, Enchantments.FEATHER_FALLING, 4,
                Enchantments.DEPTH_STRIDER, 3, Enchantments.SOUL_SPEED, 3));
        wear(player, EquipmentSlot.MAINHAND, enchanted(enchantments, new ItemStack(Items.DIAMOND_SWORD),
                Enchantments.SHARPNESS, 5, Enchantments.LOOTING, 3, Enchantments.FIRE_ASPECT, 2, Enchantments.SWEEPING_EDGE, 3,
                Enchantments.UNBREAKING, 3, Enchantments.MENDING, 1));
        player.getInventory().add(new ItemStack(BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath("iceandfire", "hydra_heart"))));
    }

    /** The stack with each (enchantment, level) pair given after it. */
    @SuppressWarnings("unchecked")
    private static ItemStack enchanted(Registry<Enchantment> enchantments, ItemStack stack, Object... pairs) {
        for (int i = 0; i < pairs.length; i += 2) {
            stack.enchant(enchantments.getHolderOrThrow((ResourceKey<Enchantment>) pairs[i]), (Integer) pairs[i + 1]);
        }
        return stack;
    }

    /** Into the slot, or the inventory if something's already there. */
    private static void wear(ServerPlayer player, EquipmentSlot slot, ItemStack stack) {
        if (player.getItemBySlot(slot).isEmpty()) {
            player.setItemSlot(slot, stack);
        } else {
            player.getInventory().add(stack);
        }
    }
}
