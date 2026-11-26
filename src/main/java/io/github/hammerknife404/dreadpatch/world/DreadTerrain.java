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
 * <li>{@link ChasmaPocket} ({@code dreadpatch:chasma_pocket}) and {@link HeavedSlabs} ({@code dreadpatch:heaved_slabs}):
 * the Rime Chasma's water sealed in its ice, and slabs heaved up at its cracks' lips (0.68.0).</li>
 * <li>{@link DragonSkeleton} ({@code dreadpatch:dragon_skeleton}): an ice dragon's bones lying out (0.76.0).</li>
 * <li>{@link DeepWater} ({@code dreadpatch:deep_water}): the open air just over bedrock flooded (0.77.0).</li>
 * </ul>
 */
public final class DreadTerrain {
    private static final DeferredRegister<MapCodec<? extends DensityFunction>> DENSITY_FUNCTIONS =
            DeferredRegister.create(Registries.DENSITY_FUNCTION_TYPE, DreadPatch.MODID);
    private static final DeferredRegister<Feature<?>> FEATURES = DeferredRegister.create(Registries.FEATURE, DreadPatch.MODID);

    public static final DeferredHolder<MapCodec<? extends DensityFunction>, MapCodec<CellEdges>> CELL_EDGES =
            DENSITY_FUNCTIONS.register("cell_edges", () -> CellEdges.CODEC.codec());
    public static final DeferredHolder<Feature<?>, LavaPocket> CRUSTED_LAVA_POCKET = FEATURES.register("crusted_lava_pocket", LavaPocket::new);
    public static final DeferredHolder<Feature<?>, GravePatch> GRAVE_PATCH = FEATURES.register("grave_patch", GravePatch::new);
    public static final DeferredHolder<Feature<?>, ChasmaPocket> CHASMA_POCKET = FEATURES.register("chasma_pocket", ChasmaPocket::new);
    public static final DeferredHolder<Feature<?>, HeavedSlabs> HEAVED_SLABS = FEATURES.register("heaved_slabs", HeavedSlabs::new);
    public static final DeferredHolder<Feature<?>, DragonSkeleton> DRAGON_SKELETON = FEATURES.register("dragon_skeleton", DragonSkeleton::new);
    public static final DeferredHolder<Feature<?>, DeepWater> DEEP_WATER = FEATURES.register("deep_water", DeepWater::new);
    /** A wild Rimefrost dragon's nest on the Glacial Plains (0.84.0). */
    public static final DeferredHolder<Feature<?>, RimefrostNests.Nest> RIMEFROST_NEST = FEATURES.register("rimefrost_nest", RimefrostNests.Nest::new);

    private DreadTerrain() {
    }

    public static void register(IEventBus modBus) {
        DENSITY_FUNCTIONS.register(modBus);
        FEATURES.register(modBus);
    }

    /**
     * Distance (blocks) from a column to the nearest edge of a jittered cell pattern, {@code size} blocks a cell:
     * half the gap between its two nearest cell points' distances ({@link TerrainRules#edgeDistance}). Each cell's
     * point is jittered by {@code noise} sampled at the cell, so it's the world seed's. With {@code hex} (0.69.0) the
     * points sit on a hexagonal lattice ({@link TerrainRules#hexEdgeDistance}), so the cells are hexagons.
     */
    public record CellEdges(DensityFunction.NoiseHolder noise, double size, double jitter, boolean hex) implements DensityFunction.SimpleFunction {
        public static final KeyDispatchDataCodec<CellEdges> CODEC = KeyDispatchDataCodec.of(RecordCodecBuilder.mapCodec(instance -> instance.group(
                DensityFunction.NoiseHolder.CODEC.fieldOf("noise").forGetter(CellEdges::noise),
                Codec.doubleRange(1.0, 4096.0).fieldOf("size").forGetter(CellEdges::size),
                Codec.doubleRange(0.0, 1.0).fieldOf("jitter").forGetter(CellEdges::jitter),
                Codec.BOOL.optionalFieldOf("hex", false).forGetter(CellEdges::hex)).apply(instance, CellEdges::new)));

        @Override
        public double compute(FunctionContext context) {
            // A noise sampled at scattered points stays small (about ±0.3): tripled, so the cells are irregular polygons, not a grid.
            TerrainRules.Jitter at = (cx, cz, axis) -> 3.0 * this.noise.getValue(cx * 17.31 + axis * 101.7, axis * 53.9, cz * 11.73 - axis * 37.3);
            double x = context.blockX() / this.size;
            double z = context.blockZ() / this.size;
            return (this.hex ? TerrainRules.hexEdgeDistance(x, z, this.jitter, at) : TerrainRules.edgeDistance(x, z, this.jitter, at)) * this.size;
        }

        @Override
        public DensityFunction mapAll(Visitor visitor) {
            return visitor.apply(new CellEdges(visitor.visitNoise(this.noise), this.size, this.jitter, this.hex));
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

    /**
     * An ice dragon's skeleton lying out (0.76.0, the owner: "ice dragon skeleton corpses spawn in the dreadlands"): Ice and
     * Fire's own, as its {@code DragonSkeletonSpawnFeature} lays them in the overworld's snow (an ice dragon 10-109 days old,
     * dead and down to bones, any colour, facing anywhere), but placed by our own data ({@code placed_feature/dragon_skeleton}),
     * so Ice and Fire's {@code generateSkeletons} and its chance don't touch it. Never on a spire's or a village's stone
     * or wood: structures are laid before features, and the heightmap would find their tops.
     */
    public static final class DragonSkeleton extends Feature<NoneFeatureConfiguration> {
        DragonSkeleton() {
            super(NoneFeatureConfiguration.CODEC);
        }

        @Override
        public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context) {
            WorldGenLevel level = context.level();
            RandomSource random = context.random();
            BlockPos at = context.origin();
            BlockState floor = level.getBlockState(at.below());
            if (floor.is(io.github.hammerknife404.dreadpatch.block.DreadGateBlock.FRAME) && !floor.is(com.iafenvoy.iceandfire.registry.IafBlocks.DREAD_STONE.get())
                    || floor.is(net.minecraft.tags.BlockTags.PLANKS) || floor.is(net.minecraft.tags.BlockTags.LOGS)
                    || floor.is(net.minecraft.tags.BlockTags.STONE_BRICKS)) {
                return false;
            }
            com.iafenvoy.iceandfire.entity.IceDragonEntity dragon = com.iafenvoy.iceandfire.registry.IafEntities.ICE_DRAGON.get().create(level.getLevel());
            if (dragon == null) {
                return false;
            }
            dragon.setPos(at.getX() + 0.5, at.getY(), at.getZ() + 0.5);
            int age = TerrainRules.SKELETON_MIN_DAYS + random.nextInt(TerrainRules.SKELETON_DAYS);
            dragon.growDragon(age);
            dragon.modelDeadProgress = 20.0F;
            dragon.setModelDead(true);
            dragon.setDeathStage(age / 10);
            dragon.setYRot(random.nextInt(360));
            java.util.List<com.iafenvoy.iceandfire.data.DragonColor> colours = dragon.dragonType.colors();
            dragon.setVariant(colours.get(random.nextInt(colours.size())).getName());
            level.addFreshEntity(dragon);
            return true;
        }
    }

    /**
     * The deeps flooded (0.77.0, the owner: "Water should fill most of the open air spaces just above the worlds bedrock
     * layers"): in each chunk, every air block from the world's floor up to {@link TerrainRules#DEEP_WATER_TOP} turned to
     * still water, so the caves and cracks that reach the bottom end in black water, level across the chunk and the next.
     * Vanilla's own flooding (sea level, aquifers) can't do it: the disabled aquifer puts lava under y −54.
     */
    public static final class DeepWater extends Feature<NoneFeatureConfiguration> {
        DeepWater() {
            super(NoneFeatureConfiguration.CODEC);
        }

        @Override
        public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context) {
            WorldGenLevel level = context.level();
            BlockPos corner = context.origin();
            BlockState water = Blocks.WATER.defaultBlockState();
            BlockPos.MutableBlockPos at = new BlockPos.MutableBlockPos();
            boolean placed = false;
            int x0 = corner.getX() & ~15;
            int z0 = corner.getZ() & ~15;
            for (int x = x0; x < x0 + 16; x++) {
                for (int z = z0; z < z0 + 16; z++) {
                    for (int y = level.getMinBuildHeight() + 1; y <= TerrainRules.DEEP_WATER_TOP; y++) {
                        at.set(x, y, z);
                        if (level.getBlockState(at).isAir()) {
                            level.setBlock(at, water, Block.UPDATE_CLIENTS);
                            placed = true;
                        }
                    }
                }
            }
            return placed;
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

    /**
     * A grave patch on the plains (0.67.0, the owner's dump: "sparse Wither Roses, and sparse gnarled dreadwood trees with
     * rare dread leaf blocks"): the ground's top 1-2 blocks round turned to Ice and Fire's graveyard soil (the only soil a
     * wither rose or, in the Dreadlands, a dreadwood sapling takes), 1-3 wither roses on it, and now and then
     * ({@link TerrainRules#TREE_CHANCE}) a dead dreadwood tree in its middle, 3-6 logs, a leaf or two near its top at most.
     */
    public static final class GravePatch extends Feature<NoneFeatureConfiguration> {
        GravePatch() {
            super(NoneFeatureConfiguration.CODEC);
        }

        @Override
        public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context) {
            WorldGenLevel level = context.level();
            RandomSource random = context.random();
            BlockPos ground = context.origin().below();
            if (!level.getBlockState(ground).isSolidRender(level, ground) || !level.getBlockState(context.origin()).isAir()) {
                return false;
            }
            BlockState soil = com.iafenvoy.iceandfire.registry.IafBlocks.GRAVEYARD_SOIL.get().defaultBlockState();
            int radius = 1 + random.nextInt(2);
            java.util.List<BlockPos> soiled = new java.util.ArrayList<>();
            for (BlockPos at : BlockPos.betweenClosed(ground.offset(-radius, -1, -radius), ground.offset(radius, 1, radius))) {
                if (at.distSqr(ground) <= radius * radius + 1 && level.getBlockState(at).isSolidRender(level, at) && level.getBlockState(at.above()).isAir()
                        && random.nextFloat() < 0.85F) {
                    level.setBlock(at, soil, Block.UPDATE_CLIENTS);
                    soiled.add(at.immutable());
                }
            }
            if (soiled.isEmpty()) {
                return false;
            }
            BlockState rose = Blocks.WITHER_ROSE.defaultBlockState();
            for (int i = 1 + random.nextInt(3); i > 0; i--) {
                BlockPos on = soiled.get(random.nextInt(soiled.size())).above();
                if (level.getBlockState(on).isAir() && rose.canSurvive(level, on)) {
                    level.setBlock(on, rose, Block.UPDATE_CLIENTS);
                }
            }
            if (random.nextFloat() < TerrainRules.TREE_CHANCE && level.getBlockState(ground).is(soil.getBlock())) {
                BlockState log = com.iafenvoy.iceandfire.registry.IafBlocks.DREADWOOD_LOG.get().defaultBlockState();
                BlockState leaves = com.iafenvoy.iceandfire.registry.IafBlocks.DREADWOOD_LEAVES.get().defaultBlockState();
                int height = 3 + random.nextInt(4);
                BlockPos top = ground;
                for (int y = 1; y <= height; y++) {
                    BlockPos at = ground.above(y);
                    if (!level.getBlockState(at).isAir() && !level.getBlockState(at).is(Blocks.WITHER_ROSE)) {
                        break;
                    }
                    level.setBlock(at, log, Block.UPDATE_CLIENTS);
                    top = at;
                }
                // A gnarled stub of a branch, and a leaf or two near the top at most.
                net.minecraft.core.Direction side = net.minecraft.core.Direction.Plane.HORIZONTAL.getRandomDirection(random);
                BlockPos branch = top.below(random.nextInt(2)).relative(side);
                if (top != ground && level.getBlockState(branch).isAir()) {
                    level.setBlock(branch, log.setValue(net.minecraft.world.level.block.RotatedPillarBlock.AXIS, side.getAxis()), Block.UPDATE_CLIENTS);
                }
                for (int i = random.nextInt(3); i > 0; i--) {
                    BlockPos leaf = top.relative(net.minecraft.core.Direction.getRandom(random));
                    if (level.getBlockState(leaf).isAir()) {
                        level.setBlock(leaf, leaves, Block.UPDATE_CLIENTS);
                    }
                }
            }
            return true;
        }
    }

    /**
     * A pocket of still water sealed in the Rime Chasma's ice (0.68.0, the owner: "Include pockets of water within the
     * ice here at the basin to naturaly spawn some of the aquatic dread beast"): under the floor's top by
     * {@link TerrainRules#POCKET_DEPTH} or a few more, 5-11 across and 3-5 high, only where everything round it is
     * solid, so it never spills into a crack or a cave. {@link RisenSpawns} finds it.
     */
    public static final class ChasmaPocket extends Feature<NoneFeatureConfiguration> {
        ChasmaPocket() {
            super(NoneFeatureConfiguration.CODEC);
        }

        @Override
        public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context) {
            WorldGenLevel level = context.level();
            RandomSource random = context.random();
            int top = context.origin().getY() - 1;
            if (Math.abs(top - TerrainRules.CHASMA_TOP) > 2) {
                return false;  // only under the floor itself, not its slopes or ridges
            }
            BlockPos heart = context.origin().below(1 + TerrainRules.POCKET_DEPTH + random.nextInt(TerrainRules.POCKET_DEPTH_SPREAD + 1));
            double across = 2.5 + random.nextDouble() * 3.0;
            double up = 1.2 + random.nextDouble();
            int r = (int) Math.ceil(across) + 1;
            int h = (int) Math.ceil(up) + 1;
            java.util.List<BlockPos> water = new java.util.ArrayList<>();
            for (BlockPos pos : BlockPos.betweenClosed(heart.offset(-r, -h, -r), heart.offset(r, h, r))) {
                int dx = pos.getX() - heart.getX(), dy = pos.getY() - heart.getY(), dz = pos.getZ() - heart.getZ();
                if (TerrainRules.inPocket(dx, dy, dz, across, up)) {
                    water.add(pos.immutable());
                } else if (TerrainRules.inPocket(dx, dy, dz, across + 1.0, up + 1.0) && !solid(level, pos)) {
                    return false;  // its shell isn't whole
                }
            }
            for (BlockPos pos : water) {
                if (!solid(level, pos)) {
                    return false;
                }
            }
            for (BlockPos pos : water) {
                level.setBlock(pos, Blocks.WATER.defaultBlockState(), Block.UPDATE_CLIENTS);
            }
            return true;
        }
    }

    /**
     * A slab of the Rime Chasma's ice heaved up at a crack's lip (0.68.0, the owner's pictures; "Tilted ice slabs"):
     * where a column beside the origin drops {@link TerrainRules#LIP_DROP} or more, a slab 3-6 long and 2-4 wide lies
     * back from the lip, two thick (snow over packed ice), its far end on the floor and its lip end raised
     * ({@link TerrainRules#heave}), now and then jutting a block over the crack.
     */
    public static final class HeavedSlabs extends Feature<NoneFeatureConfiguration> {
        HeavedSlabs() {
            super(NoneFeatureConfiguration.CODEC);
        }

        @Override
        public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context) {
            WorldGenLevel level = context.level();
            RandomSource random = context.random();
            // Looks round the origin for a lip on the floor itself (not a crack's bottom).
            BlockPos ground = null;
            net.minecraft.core.Direction toCrack = null;
            for (int attempt = 0; attempt < 24 && toCrack == null; attempt++) {
                int x = context.origin().getX() + random.nextInt(15) - 7;
                int z = context.origin().getZ() + random.nextInt(15) - 7;
                ground = new BlockPos(x, level.getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.WORLD_SURFACE_WG, x, z) - 1, z);
                if (Math.abs(ground.getY() - TerrainRules.CHASMA_TOP) > 2 || !solid(level, ground)) {
                    continue;
                }
                for (net.minecraft.core.Direction side : net.minecraft.core.Direction.Plane.HORIZONTAL.shuffledCopy(random)) {
                    if (drops(level, ground.relative(side))) {
                        toCrack = side;
                        break;
                    }
                }
            }
            if (toCrack == null) {
                return false;
            }
            net.minecraft.core.Direction back = toCrack.getOpposite();
            net.minecraft.core.Direction across = toCrack.getClockWise();
            int length = 3 + random.nextInt(4);
            int width = 2 + random.nextInt(3);
            double slope = 0.4 + random.nextDouble() * 0.5;
            int from = random.nextFloat() < 0.4F ? -1 : 0;  // now and then jutting over the crack
            BlockState top = Blocks.SNOW_BLOCK.defaultBlockState();
            BlockState under = Blocks.PACKED_ICE.defaultBlockState();
            boolean placed = false;
            for (int t = from; t < length; t++) {
                int rise = TerrainRules.heave(Math.max(t, 0), length, slope) + (t < 0 ? 1 : 0);
                for (int w = -width / 2; w < width - width / 2; w++) {
                    BlockPos at = ground.relative(back, t).relative(across, w).above(1 + rise);
                    for (BlockPos pos : new BlockPos[] {at, at.below()}) {
                        if (pos.getY() > ground.getY() && level.getBlockState(pos).isAir()) {
                            level.setBlock(pos, pos == at ? top : under, Block.UPDATE_CLIENTS);
                            placed = true;
                        }
                    }
                }
            }
            return placed;
        }

        /** Whether the column at the floor's height there is air for {@link TerrainRules#LIP_DROP} blocks down. */
        private static boolean drops(WorldGenLevel level, BlockPos pos) {
            for (int i = 0; i < TerrainRules.LIP_DROP; i++) {
                if (!level.getBlockState(pos.below(i)).isAir()) {
                    return false;
                }
            }
            return true;
        }
    }

    /** A whole, dry, solid block (ice and snow included; isSolidRender isn't sure of blue ice). */
    private static boolean solid(WorldGenLevel level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        return !state.isAir() && state.getFluidState().isEmpty() && state.isCollisionShapeFullBlock(level, pos);
    }

    private static final class Block {
        static final int UPDATE_CLIENTS = net.minecraft.world.level.block.Block.UPDATE_CLIENTS;
    }
}
