package io.github.hammerknife404.dreadpatch.registry;

import io.github.hammerknife404.dreadpatch.DreadPatch;
import io.github.hammerknife404.dreadpatch.block.RimefrostForgeBlockEntity;
import io.github.hammerknife404.dreadpatch.block.RimefrostForgeBrickBlock;
import io.github.hammerknife404.dreadpatch.block.RimefrostForgeCoreBlock;
import io.github.hammerknife404.dreadpatch.block.RimefrostForgeInputBlock;
import io.github.hammerknife404.dreadpatch.block.RimefrostForgeInputBlockEntity;
import io.github.hammerknife404.dreadpatch.block.RimefrostForgeMenu;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.properties.NoteBlockInstrument;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.items.wrapper.InvWrapper;
import net.neoforged.neoforge.items.wrapper.SidedInvWrapper;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.List;

/**
 * The Rimefrost dragonforge (0.59.0) and the block of Rimefrost dragon scales its bricks are made of:
 * Ice and Fire's ice dragonforge and its blocks of dragon scales, made Dread. The forge's blocks have
 * Ice and Fire's forge blocks' numbers (40 hardness, 500 blast resistance, metal), the scale block its
 * scale blocks' (30, 500, stone, needing a pickaxe).
 */
public final class DreadForge {
    private static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(DreadPatch.MODID);
    private static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(DreadPatch.MODID);
    private static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES = DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, DreadPatch.MODID);
    private static final DeferredRegister<MenuType<?>> MENUS = DeferredRegister.create(Registries.MENU, DreadPatch.MODID);

    public static final DeferredBlock<RimefrostForgeCoreBlock> CORE = BLOCKS.register("rimefrost_forge_core",
            () -> new RimefrostForgeCoreBlock(forge().mapColor(MapColor.METAL)
                    .lightLevel(state -> state.getValue(RimefrostForgeCoreBlock.LIT) ? RimefrostForgeCoreBlock.LIGHT : 0)));
    public static final DeferredBlock<RimefrostForgeBrickBlock> BRICK = BLOCKS.register("rimefrost_forge_brick",
            () -> new RimefrostForgeBrickBlock(forge()));
    public static final DeferredBlock<RimefrostForgeInputBlock> INPUT = BLOCKS.register("rimefrost_forge_input",
            () -> new RimefrostForgeInputBlock(forge()));
    public static final DeferredBlock<Block> SCALE_BLOCK = BLOCKS.register("rimefrost_dragonscale_block",
            () -> new Block(BlockBehaviour.Properties.of().mapColor(MapColor.STONE).instrument(NoteBlockInstrument.BASEDRUM)
                    .strength(30.0F, 500.0F).sound(SoundType.STONE).requiresCorrectToolForDrops()));

    public static final DeferredItem<BlockItem> CORE_ITEM = ITEMS.registerSimpleBlockItem(CORE, new Item.Properties().rarity(Rarity.RARE));
    public static final DeferredItem<BlockItem> BRICK_ITEM = ITEMS.registerSimpleBlockItem(BRICK, new Item.Properties().rarity(Rarity.UNCOMMON));
    public static final DeferredItem<BlockItem> INPUT_ITEM = ITEMS.registerSimpleBlockItem(INPUT, new Item.Properties().rarity(Rarity.UNCOMMON));
    public static final DeferredItem<BlockItem> SCALE_BLOCK_ITEM = ITEMS.registerSimpleBlockItem(SCALE_BLOCK, new Item.Properties().rarity(Rarity.UNCOMMON));

    @SuppressWarnings("DataFlowIssue")
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<RimefrostForgeBlockEntity>> CORE_ENTITY = BLOCK_ENTITIES.register("rimefrost_forge_core",
            () -> BlockEntityType.Builder.of(RimefrostForgeBlockEntity::new, CORE.get()).build(null));
    @SuppressWarnings("DataFlowIssue")
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<RimefrostForgeInputBlockEntity>> INPUT_ENTITY = BLOCK_ENTITIES.register("rimefrost_forge_input",
            () -> BlockEntityType.Builder.of(RimefrostForgeInputBlockEntity::new, INPUT.get()).build(null));

    public static final DeferredHolder<MenuType<?>, MenuType<RimefrostForgeMenu>> MENU = MENUS.register("rimefrost_forge",
            () -> new MenuType<>(RimefrostForgeMenu::new, FeatureFlags.DEFAULT_FLAGS));

    private DreadForge() {
    }

    private static BlockBehaviour.Properties forge() {
        return BlockBehaviour.Properties.of().mapColor(MapColor.STONE).instrument(NoteBlockInstrument.BASEDRUM)
                .strength(40.0F, 500.0F).sound(SoundType.METAL);
    }

    public static void register(IEventBus modBus) {
        BLOCKS.register(modBus);
        ITEMS.register(modBus);
        BLOCK_ENTITIES.register(modBus);
        MENUS.register(modBus);
        modBus.addListener(DreadForge::onCapabilities);
    }

    /** The forge's blocks, in the order the creative tab shows them. */
    public static List<DeferredItem<BlockItem>> forgeItems() {
        return List.of(CORE_ITEM, BRICK_ITEM, INPUT_ITEM);
    }

    /**
     * Hoppers and pipes: the core's slots by side (in from above, blood from the sides, out from below),
     * and the same through any brick touching a core, as Ice and Fire's bricks hand on its core's.
     */
    private static void onCapabilities(RegisterCapabilitiesEvent event) {
        event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, CORE_ENTITY.get(),
                (forge, side) -> side == null ? new InvWrapper(forge) : new SidedInvWrapper(forge, side));
        event.registerBlock(Capabilities.ItemHandler.BLOCK, (level, pos, state, entity, side) -> {
            RimefrostForgeBlockEntity forge = RimefrostForgeBrickBlock.core(level, pos);
            if (forge == null) {
                return null;
            }
            return side == null ? new InvWrapper(forge) : new SidedInvWrapper(forge, side);
        }, BRICK.get());
    }
}
