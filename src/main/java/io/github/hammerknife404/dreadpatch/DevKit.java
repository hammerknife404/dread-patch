package io.github.hammerknife404.dreadpatch;

import com.iafenvoy.iceandfire.registry.IafItems;
import io.github.hammerknife404.dreadpatch.registry.DreadGear;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

/**
 * Dev runs only (the owner, 2026-11-24: "make it so the dev client start me with the full dread queen set + a hydra
 * heart - this is for dev only right now"): the first time a player joins a world in a dev run, they're given her
 * regalia worn, her sword and staff in hand, and Ice and Fire's hydra heart. {@link FMLEnvironment#production} is
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
        wear(player, EquipmentSlot.HEAD, new ItemStack(DreadGear.DREAD_QUEEN_CROWN.get()));
        wear(player, EquipmentSlot.CHEST, new ItemStack(DreadGear.DREAD_QUEEN_BODICE.get()));
        wear(player, EquipmentSlot.LEGS, new ItemStack(DreadGear.DREAD_QUEEN_ROBE.get()));
        wear(player, EquipmentSlot.FEET, new ItemStack(DreadGear.DREAD_QUEEN_GREAVES.get()));
        wear(player, EquipmentSlot.MAINHAND, new ItemStack(IafItems.DREAD_QUEEN_SWORD.get()));
        wear(player, EquipmentSlot.OFFHAND, new ItemStack(IafItems.DREAD_QUEEN_STAFF.get()));
        player.getInventory().add(new ItemStack(BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath("iceandfire", "hydra_heart"))));
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
