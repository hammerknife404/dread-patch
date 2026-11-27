package io.github.hammerknife404.dreadpatch.registry;

import com.iafenvoy.iceandfire.item.block.PileBlock;
import io.github.hammerknife404.dreadpatch.DreadPatch;
import net.minecraft.world.item.BlockItem;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * A Rimefrost dragon's hoard (0.84.0): the hoard pile Ice and Fire lacks, iron (its {@link PileBlock}, as its gold,
 * silver and copper piles: eight layers, two nuggets a layer, a nugget in hand adds a layer).
 * <p>0.84.0's blacksteel pile and blacksteel dragon armour (L_Ender's Cataclysm's blacksteel) were taken out in 0.84.2
 * (the owner: "remove the blacksteel items, it assumes L_e_enderscataclysm is also installed. Enough dragon armors exists
 * already").
 */
public final class Hoard {
    private static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(DreadPatch.MODID);
    private static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(DreadPatch.MODID);

    public static final DeferredBlock<PileBlock> IRON_PILE = BLOCKS.register("iron_pile", PileBlock::new);
    public static final DeferredItem<BlockItem> IRON_PILE_ITEM = ITEMS.registerSimpleBlockItem(IRON_PILE);

    private Hoard() {
    }

    public static void register(IEventBus modBus) {
        BLOCKS.register(modBus);
        ITEMS.register(modBus);
    }
}
