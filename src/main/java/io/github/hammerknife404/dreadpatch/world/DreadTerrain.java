package io.github.hammerknife404.dreadpatch.world;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.hammerknife404.dreadpatch.DreadPatch;
import io.github.hammerknife404.dreadpatch.magic.TerrainRules;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.util.KeyDispatchDataCodec;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.DensityFunction;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * The Dreadlands' terrain's own pieces (0.66.0; the rest is data, {@code worldgen/noise_settings/dreadlands.json}):
 * <ul>
 * <li>{@link CellEdges} ({@code dreadpatch:cell_edges}): how far a column is from the nearest edge of a jittered
 * cellular (Voronoi) pattern, in blocks: 0 on an edge. Vanilla's density functions have no cellular noise; the Rime
 * Chasma's polygonal trenches and fissures are where it's under their width.</li>
 * <li>{@link LavaPocket} ({@code dreadpatch:crusted_lava_pocket}): a small blob of lava sealed in a crust of packed
 * ice, blue ice, basalt, dreadstone, obsidian and now and then crying obsidian.</li>
 * </ul>
 */
public final class DreadTerrain {
    private static final DeferredRegister<MapCodec<? extends DensityFunction>> DENSITY_FUNCTIONS =
            DeferredRegister.create(Registries.DENSITY_FUNCTION_TYPE, DreadPatch.MODID);
    private static final DeferredRegister<Feature<?>> FEATURES = DeferredRegister.create(Registries.FEATURE, DreadPatch.MODID);

    public static final DeferredHolder<MapCodec<? extends DensityFunction>, MapCodec<CellEdges>> CELL_EDGES =
            DENSITY_FUNCTIONS.register("cell_edges", () -> CellEdges.CODEC.codec());
    public static final DeferredHolder<Feature<?>, LavaPocket> CRUSTED_LAVA_POCKET = FEATURES.register("crusted_lava_pocket", LavaPocket::new);

    private DreadTerrain() {
    }

    public static void register(IEventBus modBus) {
        DENSITY_FUNCTIONS.register(modBus);
        FEATURES.register(modBus);
    }

    /**
     * Distance (blocks) from a column to the nearest edge of a jittered cell pattern, {@code size} blocks a cell:
     * half the gap between its two nearest cell points' distances ({@link TerrainRules#edgeDistance}). Each cell's
     * point is jittered by {@code noise} sampled at the cell, so it's the world seed's.
     */
    public record CellEdges(DensityFunction.NoiseHolder noise, double size, double jitter) implements DensityFunction.SimpleFunction {
        public static final KeyDispatchDataCodec<CellEdges> CODEC = KeyDispatchDataCodec.of(RecordCodecBuilder.mapCodec(instance -> instance.group(
                DensityFunction.NoiseHolder.CODEC.fieldOf("noise").forGetter(CellEdges::noise),
                Codec.doubleRange(1.0, 4096.0).fieldOf("size").forGetter(CellEdges::size),
                Codec.doubleRange(0.0, 1.0).fieldOf("jitter").forGetter(CellEdges::jitter)).apply(instance, CellEdges::new)));

        @Override
        public double compute(FunctionContext context) {
            return TerrainRules.edgeDistance(context.blockX() / this.size, context.blockZ() / this.size, this.jitter,
                    // A noise sampled at scattered points stays small (about ±0.3): tripled, so the cells are irregular polygons, not a grid.
                    (cx, cz, axis) -> 3.0 * this.noise.getValue(cx * 17.31 + axis * 101.7, axis * 53.9, cz * 11.73 - axis * 37.3)) * this.size;
        }

        @Override
        public DensityFunction mapAll(Visitor visitor) {
            return visitor.apply(new CellEdges(visitor.visitNoise(this.noise), this.size, this.jitter));
        }

        @Override
        public double minValue() {
            return 0.0;
        }

        @Override
        public double maxValue() {
            return this.size;
        }

        @Override
        public KeyDispatchDataCodec<? extends DensityFunction> codec() {
            return CODEC;
        }
    }

    /** A pocket of lava, 1.5-2.5 across its radius, in a crust 1-2 thick of cold and hard stone. */
    public static final class LavaPocket extends Feature<NoneFeatureConfiguration> {
        LavaPocket() {
            super(NoneFeatureConfiguration.CODEC);
        }

        @Override
        public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context) {
            WorldGenLevel level = context.level();
            BlockPos origin = context.origin();
            RandomSource random = context.random();
            double radius = 1.5 + random.nextDouble();
            double crust = radius + 1.0 + random.nextDouble();
            int r = (int) Math.ceil(crust);
            // Only in solid ground: never hanging in a cave or breaking into the open.
            if (!level.getBlockState(origin).isSolidRender(level, origin)) {
                return false;
            }
            for (BlockPos pos : BlockPos.betweenClosed(origin.offset(-r, -r, -r), origin.offset(r, r, r))) {
                double d = Math.sqrt(pos.distSqr(origin));
                if (d > crust) {
                    continue;
                }
                BlockState here = level.getBlockState(pos);
                if (here.is(Blocks.BEDROCK)) {
                    continue;
                }
                level.setBlock(pos, d <= radius ? Blocks.LAVA.defaultBlockState() : crustBlock(random), Block.UPDATE_CLIENTS);
            }
            return true;
        }

        private static BlockState crustBlock(RandomSource random) {
            return switch (TerrainRules.crust(random.nextInt(TerrainRules.CRUST_WEIGHT))) {
                case PACKED_ICE -> Blocks.PACKED_ICE.defaultBlockState();
                case BLUE_ICE -> Blocks.BLUE_ICE.defaultBlockState();
                case BASALT -> Blocks.BASALT.defaultBlockState();
                case DREADSTONE -> com.iafenvoy.iceandfire.registry.IafBlocks.DREAD_STONE.get().defaultBlockState();
                case OBSIDIAN -> Blocks.OBSIDIAN.defaultBlockState();
                case CRYING_OBSIDIAN -> Blocks.CRYING_OBSIDIAN.defaultBlockState();
            };
        }
    }

    private static final class Block {
        static final int UPDATE_CLIENTS = net.minecraft.world.level.block.Block.UPDATE_CLIENTS;
    }
}
