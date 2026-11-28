package io.github.hammerknife404.dreadpatch.registry;

import io.github.hammerknife404.dreadpatch.DreadPatch;
import io.github.hammerknife404.dreadpatch.block.DreadCampfireBlock;
import io.github.hammerknife404.dreadpatch.block.DreadCampfireBlockEntity;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

/** The Dread Campfire (0.98.4; {@link DreadCampfireBlock}): a soul campfire's make, a dread light's glow. */
public final class DreadCampfire {
    private static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(DreadPatch.MODID);
    private static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(DreadPatch.MODID);
    private static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES = DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, DreadPatch.MODID);

    public static final DeferredBlock<DreadCampfireBlock> BLOCK = BLOCKS.register("dread_campfire",
            () -> new DreadCampfireBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.SOUL_CAMPFIRE).lightLevel(DreadCampfireBlock::light)));
    public static final DeferredItem<BlockItem> ITEM = ITEMS.registerSimpleBlockItem(BLOCK, new Item.Properties());
    @SuppressWarnings("DataFlowIssue")
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<DreadCampfireBlockEntity>> BLOCK_ENTITY = BLOCK_ENTITIES.register("dread_campfire",
            () -> BlockEntityType.Builder.of(DreadCampfireBlockEntity::new, BLOCK.get()).build(null));

    private DreadCampfire() {
    }

    public static void register(IEventBus modBus) {
        BLOCKS.register(modBus);
        ITEMS.register(modBus);
        BLOCK_ENTITIES.register(modBus);
    }
}
