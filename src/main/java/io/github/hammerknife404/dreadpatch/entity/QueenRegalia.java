package io.github.hammerknife404.dreadpatch.entity;

import io.github.hammerknife404.dreadpatch.DreadPatch;
import io.github.hammerknife404.dreadpatch.magic.RegaliaDrops;
import io.github.hammerknife404.dreadpatch.registry.DreadGear;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.entity.living.LivingDropsEvent;
import net.neoforged.neoforge.event.entity.living.LivingEquipmentChangeEvent;
import com.iafenvoy.iceandfire.registry.IafItems;

import java.util.List;
import java.util.Random;

/**
 * The Dread Queen's regalia drops (0.43.0): a queen a player kills drops a piece of it that player is
 * missing ({@link RegaliaDrops}), what they have tracked by an advancement a piece
 * ({@code dreadpatch:regalia/<piece>}, granted the first time they hold it). Not from her loot table:
 * a loot table can't see a player's advancements.
 */
public final class QueenRegalia {
    static final String[] PIECES = {"crown", "bodice", "robe", "greaves"};

    private QueenRegalia() {
    }

    public static void register(IEventBus gameBus) {
        gameBus.addListener(QueenRegalia::onDrops);
        gameBus.addListener(QueenRegalia::onEquip);
    }

    /**
     * "There Must Always Be a Dread Queen" (the owner, 0.44.0): all four pieces worn, her sword and staff
     * in hand, either way round. Checked across every slot when, and only when, a player's equipment
     * changes (vanilla's own per-tick comparison raises the event; nothing polls), and granted by its
     * {@code impossible} criterion.
     */
    private static void onEquip(LivingEquipmentChangeEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        AdvancementHolder advancement = player.server.getAdvancements().get(DreadPatch.id("regalia/enthroned"));
        if (advancement == null || player.getAdvancements().getOrStartProgress(advancement).isDone()) {
            return;
        }
        boolean dressed = player.getItemBySlot(EquipmentSlot.HEAD).is(DreadGear.DREAD_QUEEN_CROWN.get())
                && player.getItemBySlot(EquipmentSlot.CHEST).is(DreadGear.DREAD_QUEEN_BODICE.get())
                && player.getItemBySlot(EquipmentSlot.LEGS).is(DreadGear.DREAD_QUEEN_ROBE.get())
                && player.getItemBySlot(EquipmentSlot.FEET).is(DreadGear.DREAD_QUEEN_GREAVES.get());
        if (!dressed) {
            return;
        }
        var main = player.getMainHandItem();
        var off = player.getOffhandItem();
        var sword = IafItems.DREAD_QUEEN_SWORD.get();
        var staff = IafItems.DREAD_QUEEN_STAFF.get();
        if (main.is(sword) && off.is(staff) || main.is(staff) && off.is(sword)) {
            player.getAdvancements().award(advancement, "enthroned");
        }
    }

    private static void onDrops(LivingDropsEvent event) {
        if (!(event.getEntity() instanceof DreadQueen queen) || !(event.getSource().getEntity() instanceof ServerPlayer player)) {
            return;
        }
        boolean[] owned = new boolean[RegaliaDrops.PIECES];
        for (int i = 0; i < PIECES.length; i++) {
            AdvancementHolder advancement = player.server.getAdvancements().get(DreadPatch.id("regalia/" + PIECES[i]));
            owned[i] = advancement != null && player.getAdvancements().getOrStartProgress(advancement).isDone();
        }
        int looting = EnchantmentHelper.getEnchantmentLevel(
                player.registryAccess().lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(Enchantments.LOOTING), player);
        List<Integer> drops = RegaliaDrops.pick(owned, looting, new Random(queen.getRandom().nextLong()));
        for (int piece : drops) {
            ItemStack stack = new ItemStack(DreadGear.queenArmor().get(piece).get());
            event.getDrops().add(new ItemEntity(queen.level(), queen.getX(), queen.getY(0.5), queen.getZ(), stack));
        }
    }
}
