package io.github.hammerknife404.dreadpatch.registry;

import com.iafenvoy.iceandfire.registry.IafBlocks;
import io.github.hammerknife404.dreadpatch.DreadPatch;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.DoubleHighBlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.ButtonBlock;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.FenceBlock;
import net.minecraft.world.level.block.FenceGateBlock;
import net.minecraft.world.level.block.PressurePlateBlock;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.TrapDoorBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.properties.BlockSetType;
import net.minecraft.world.level.block.state.properties.WoodType;
import net.minecraft.world.level.material.PushReaction;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.List;
import java.util.function.Supplier;

/**
 * Dreadwood's block set (0.65.0, the owner: "Add Dreadwood block variants, stairs, slabs, fencing etc."): stairs,
 * slab, fence, fence gate, door, trapdoor, button and pressure plate, from Ice and Fire's dreadwood planks
 * (its properties, its texture), made as oak's are. Their JSON is {@code tools/datagen/make_dreadwood.py}'s, from
 * vanilla's oak files. Abandoned villages' spruce ones turn to these.
 */
public final class Dreadwood {
    private static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(DreadPatch.MODID);
    private static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(DreadPatch.MODID);

    /** Oak's sounds and timings. */
    public static final BlockSetType SET = BlockSetType.register(new BlockSetType(DreadPatch.id("dreadwood").toString()));
    public static final WoodType WOOD = WoodType.register(new WoodType(DreadPatch.id("dreadwood").toString(), SET));

    public static final DeferredBlock<StairBlock> STAIRS = BLOCKS.register("dreadwood_stairs",
            () -> new StairBlock(IafBlocks.DREADWOOD_PLANKS.get().defaultBlockState(), planks()));
    public static final DeferredBlock<SlabBlock> SLAB = BLOCKS.register("dreadwood_slab", () -> new SlabBlock(planks()));
    public static final DeferredBlock<FenceBlock> FENCE = BLOCKS.register("dreadwood_fence", () -> new FenceBlock(planks()));
    public static final DeferredBlock<FenceGateBlock> FENCE_GATE = BLOCKS.register("dreadwood_fence_gate", () -> new FenceGateBlock(WOOD, planks()));
    public static final DeferredBlock<DoorBlock> DOOR = BLOCKS.register("dreadwood_door",
            () -> new DoorBlock(SET, planks().noOcclusion().pushReaction(PushReaction.DESTROY)));
    public static final DeferredBlock<TrapDoorBlock> TRAPDOOR = BLOCKS.register("dreadwood_trapdoor",
            () -> new TrapDoorBlock(SET, planks().noOcclusion().isValidSpawn((state, level, pos, type) -> false)));
    public static final DeferredBlock<ButtonBlock> BUTTON = BLOCKS.register("dreadwood_button",
            () -> new ButtonBlock(SET, 30, BlockBehaviour.Properties.of().noCollission().strength(0.5F).pushReaction(PushReaction.DESTROY)));
    public static final DeferredBlock<PressurePlateBlock> PRESSURE_PLATE = BLOCKS.register("dreadwood_pressure_plate",
            () -> new PressurePlateBlock(SET, planks().forceSolidOn().noCollission().strength(0.5F).pushReaction(PushReaction.DESTROY)));

    public static final DeferredItem<BlockItem> STAIRS_ITEM = ITEMS.registerSimpleBlockItem(STAIRS);
    public static final DeferredItem<BlockItem> SLAB_ITEM = ITEMS.registerSimpleBlockItem(SLAB);
    public static final DeferredItem<BlockItem> FENCE_ITEM = ITEMS.registerSimpleBlockItem(FENCE);
    public static final DeferredItem<BlockItem> FENCE_GATE_ITEM = ITEMS.registerSimpleBlockItem(FENCE_GATE);
    public static final DeferredItem<DoubleHighBlockItem> DOOR_ITEM = ITEMS.register("dreadwood_door",
            () -> new DoubleHighBlockItem(DOOR.get(), new Item.Properties()));
    public static final DeferredItem<BlockItem> TRAPDOOR_ITEM = ITEMS.registerSimpleBlockItem(TRAPDOOR);
    public static final DeferredItem<BlockItem> BUTTON_ITEM = ITEMS.registerSimpleBlockItem(BUTTON);
    public static final DeferredItem<BlockItem> PRESSURE_PLATE_ITEM = ITEMS.registerSimpleBlockItem(PRESSURE_PLATE);

    private Dreadwood() {
    }

    public static void register(IEventBus modBus) {
        BLOCKS.register(modBus);
        ITEMS.register(modBus);
    }

    /** For the Building Blocks tab, in vanilla's order. */
    public static List<Supplier<? extends Item>> items() {
        return List.of(STAIRS_ITEM, SLAB_ITEM, FENCE_ITEM, FENCE_GATE_ITEM, DOOR_ITEM, TRAPDOOR_ITEM, PRESSURE_PLATE_ITEM, BUTTON_ITEM);
    }

    private static BlockBehaviour.Properties planks() {
        return BlockBehaviour.Properties.ofFullCopy(IafBlocks.DREADWOOD_PLANKS.get());
    }
}
