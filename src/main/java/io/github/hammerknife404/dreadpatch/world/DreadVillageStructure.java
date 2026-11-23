package io.github.hammerknife404.dreadpatch.world;

import com.iafenvoy.iceandfire.registry.IafBlocks;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.hammerknife404.dreadpatch.registry.ModRegistries;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureType;
import net.minecraft.world.level.levelgen.structure.pieces.PiecesContainer;
import net.minecraft.world.level.levelgen.structure.structures.JigsawStructure;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

/**
 * An abandoned village of the Dreadlands (0.53.0, the owner's second drop: "Abandoned Villages ...
 * generate with appropriate Dread block replacements"): vanilla's snowy zombie village (its zombie town
 * centres only, so all of it is the abandoned kind), built by the jigsaw structure it holds
 * ({@code JigsawStructure} is final), then, chunk by chunk as its pieces go down ({@link #afterPlace}),
 * its wood and stone turned Dread: planks to dreadwood planks, logs to dreadwood logs, cobblestone to
 * dreadstone, stone bricks to dreadstone bricks (mossy and cracked kept), their stairs and slabs to
 * dreadstone's; its trees' leaves gone, leaving dead dreadwood trunks. Spruce stairs, slabs, fences and
 * doors stay (Ice and Fire has no dreadwood ones). Its
 * chests are vanilla's, with vanilla's village loot tables: Lootr's to make per-player. Only here:
 * vanilla's villages in the overworld are untouched.
 */
public class DreadVillageStructure extends Structure {
    public static final MapCodec<DreadVillageStructure> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            settingsCodec(i),
            JigsawStructure.CODEC.codec().fieldOf("village").forGetter(s -> s.village)
    ).apply(i, DreadVillageStructure::new));

    /** How far round and over the village's pieces its trees may reach. */
    private static final int TREE_REACH = 4;
    private static final int TREE_HEADROOM = 16;

    private final JigsawStructure village;

    public DreadVillageStructure(StructureSettings settings, JigsawStructure village) {
        super(settings);
        this.village = village;
    }

    @Override
    protected Optional<GenerationStub> findGenerationPoint(GenerationContext context) {
        return this.village.findValidGenerationPoint(context);
    }

    @Override
    public void afterPlace(WorldGenLevel level, StructureManager structures, ChunkGenerator generator, RandomSource random, BoundingBox chunkBox,
                           ChunkPos chunkPos, PiecesContainer pieces) {
        // The whole village's bounds, with room over it: its trees (feature pieces a block in size) grow
        // well past their own pieces' boxes. Nothing of the Dreadlands' own ground is wood, cobblestone or bricks.
        BoundingBox box = pieces.calculateBoundingBox();
        BlockPos.MutableBlockPos at = new BlockPos.MutableBlockPos();
        for (int x = Math.max(box.minX() - TREE_REACH, chunkBox.minX()); x <= Math.min(box.maxX() + TREE_REACH, chunkBox.maxX()); x++) {
            for (int z = Math.max(box.minZ() - TREE_REACH, chunkBox.minZ()); z <= Math.min(box.maxZ() + TREE_REACH, chunkBox.maxZ()); z++) {
                for (int y = Math.max(box.minY(), chunkBox.minY()); y <= Math.min(box.maxY() + TREE_HEADROOM, chunkBox.maxY()); y++) {
                    at.set(x, y, z);
                    BlockState state = level.getBlockState(at);
                    BlockState dread = state.is(BlockTags.LEAVES) ? Blocks.AIR.defaultBlockState() : dread(state);
                    if (dread != null) {
                        level.setBlock(at, dread, Block.UPDATE_CLIENTS);
                    }
                }
            }
        }
    }

    /** The Dread's block for a village's own, its shape kept; null to leave it. */
    @Nullable
    static BlockState dread(BlockState state) {
        BlockState flame = DreadFlames.dread(state);
        if (flame != null) {
            // Its lanterns and torches the Dread's (0.57.0).
            return flame;
        }
        if (state.is(BlockTags.PLANKS)) {
            return IafBlocks.DREADWOOD_PLANKS.get().defaultBlockState();
        }
        if (state.is(BlockTags.LOGS)) {
            return shaped(state, IafBlocks.DREADWOOD_LOG.get().defaultBlockState());
        }
        if (state.is(Blocks.COBBLESTONE) || state.is(Blocks.STONE_BRICKS)) {
            return (state.is(Blocks.COBBLESTONE) ? IafBlocks.DREAD_STONE : IafBlocks.DREAD_STONE_BRICKS).get().defaultBlockState();
        }
        if (state.is(Blocks.MOSSY_COBBLESTONE) || state.is(Blocks.MOSSY_STONE_BRICKS)) {
            return IafBlocks.DREAD_STONE_BRICKS_MOSSY.get().defaultBlockState();
        }
        if (state.is(Blocks.CRACKED_STONE_BRICKS)) {
            return IafBlocks.DREAD_STONE_BRICKS_CRACKED.get().defaultBlockState();
        }
        if (state.is(Blocks.COBBLESTONE_STAIRS) || state.is(Blocks.STONE_BRICK_STAIRS) || state.is(Blocks.MOSSY_COBBLESTONE_STAIRS)
                || state.is(Blocks.MOSSY_STONE_BRICK_STAIRS) || state.is(Blocks.STONE_STAIRS)) {
            return shaped(state, IafBlocks.DREAD_STONE_BRICKS_STAIRS.get().defaultBlockState());
        }
        if (state.is(Blocks.COBBLESTONE_SLAB) || state.is(Blocks.STONE_BRICK_SLAB) || state.is(Blocks.MOSSY_COBBLESTONE_SLAB)
                || state.is(Blocks.MOSSY_STONE_BRICK_SLAB) || state.is(Blocks.STONE_SLAB)) {
            return shaped(state, IafBlocks.DREAD_STONE_BRICKS_SLAB.get().defaultBlockState());
        }
        return null;
    }

    /** {@code to}, with every property it shares with {@code from} (facing, half, shape, axis, type, waterlogged) copied. */
    private static BlockState shaped(BlockState from, BlockState to) {
        BlockState result = to;
        for (Property<?> property : from.getProperties()) {
            if (result.hasProperty(property)) {
                result = copy(from, result, property);
            }
        }
        return result;
    }

    private static <T extends Comparable<T>> BlockState copy(BlockState from, BlockState to, Property<T> property) {
        return to.setValue(property, from.getValue(property));
    }

    @Override
    public StructureType<?> type() {
        return ModRegistries.DREAD_VILLAGE.get();
    }
}
