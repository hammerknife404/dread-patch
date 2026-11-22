package io.github.hammerknife404.dreadpatch.entity;

import io.github.hammerknife404.dreadpatch.DreadPatch;
import io.github.hammerknife404.dreadpatch.magic.RegaliaDrops;
import io.github.hammerknife404.dreadpatch.registry.DreadGear;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.entity.living.LivingDropsEvent;

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
