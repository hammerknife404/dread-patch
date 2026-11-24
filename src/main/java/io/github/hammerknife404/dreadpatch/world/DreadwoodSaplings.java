package io.github.hammerknife404.dreadpatch.world;

import com.iafenvoy.iceandfire.registry.IafBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.event.level.BlockGrowFeatureEvent;

/**
 * In the Dreadlands a dreadwood sapling takes only graveyard soil (0.60.0, the owner: "Dreadlands only.
 * Farmin' 'aint easy."). Elsewhere it's Ice and Fire's, as before. Placed on anything else there, it's
 * refused (with a hint); one already standing on anything else never grows (random ticks and bone meal
 * both grow a tree through {@link BlockGrowFeatureEvent}).
 * <p>Vanilla's sapling takes only {@code #minecraft:dirt}, so graveyard soil is in that tag
 * ({@code data/minecraft/tags/block/dirt.json}). Ice and Fire's graveyard soil raises its ghosts at night
 * under anything that doesn't cover it, a sapling included. And dreadwood leaves drop saplings now (a loot
 * table of ours at Ice and Fire's id: it ships none, so they dropped nothing).
 */
public final class DreadwoodSaplings {
    private DreadwoodSaplings() {
    }

    public static void register(IEventBus gameBus) {
        gameBus.addListener(DreadwoodSaplings::onPlace);
        gameBus.addListener(DreadwoodSaplings::onGrow);
    }

    private static void onPlace(BlockEvent.EntityPlaceEvent event) {
        if (event.getLevel() instanceof Level level && Dreadlands.is(level) && event.getPlacedBlock().is(IafBlocks.DREADWOOD_SAPLING.get())
                && !rooted(level, event.getPos())) {
            event.setCanceled(true);
            if (event.getEntity() instanceof Player player) {
                player.displayClientMessage(Component.translatable("message.dreadpatch.dreadwood_sapling"), true);
            }
        }
    }

    private static void onGrow(BlockGrowFeatureEvent event) {
        LevelAccessor level = event.getLevel();
        if (level instanceof Level real && Dreadlands.is(real) && level.getBlockState(event.getPos()).is(IafBlocks.DREADWOOD_SAPLING.get())
                && !rooted(level, event.getPos())) {
            event.setCanceled(true);
        }
    }

    /** Whether the sapling at {@code pos} stands on graveyard soil. */
    static boolean rooted(LevelAccessor level, BlockPos pos) {
        return level.getBlockState(pos.below()).is(IafBlocks.GRAVEYARD_SOIL.get());
    }
}
