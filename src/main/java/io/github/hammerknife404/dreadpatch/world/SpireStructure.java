package io.github.hammerknife404.dreadpatch.world;

import com.mojang.serialization.MapCodec;
import io.github.hammerknife404.dreadpatch.magic.SpireShape;
import io.github.hammerknife404.dreadpatch.registry.ModRegistries;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureType;

import java.util.Optional;

/**
 * A spire of the Dreadlands (0.46.0; {@link SpireShape}): one piece, standing on the lowest ground
 * round its foot (its foundation sinks into the rest), at the middle of its chunk.
 */
public class SpireStructure extends Structure {
    public static final MapCodec<SpireStructure> CODEC = simpleCodec(SpireStructure::new);

    public SpireStructure(StructureSettings settings) {
        super(settings);
    }

    @Override
    protected Optional<GenerationStub> findGenerationPoint(GenerationContext context) {
        ChunkPos chunk = context.chunkPos();
        int x = chunk.getMiddleBlockX();
        int z = chunk.getMiddleBlockZ();
        long seed = context.random().nextLong();
        SpireShape shape = SpireShape.roll(seed);
        int ground = this.ground(context, x, z);
        // Its whole foot (0.66.0: the Sastrugi Wastes' dunes and the basins' bluffs): 8 round its edge, 8 half way in.
        for (double reach : new double[]{shape.baseRadius, shape.baseRadius / 2.0}) {
            for (int i = 0; i < 8; i++) {
                double angle = i * Math.PI / 4.0 + (reach < shape.baseRadius ? Math.PI / 8.0 : 0.0);
                ground = Math.min(ground, this.ground(context, x + (int) Math.round(Math.cos(angle) * reach),
                        z + (int) Math.round(Math.sin(angle) * reach)));
            }
        }
        BlockPos base = new BlockPos(x, ground, z);
        if (base.getY() + shape.height + SpireShape.MAX_HEIGHT / 10 >= context.heightAccessor().getMaxBuildHeight()) {
            return Optional.empty();
        }
        return Optional.of(new GenerationStub(base, builder -> builder.addPiece(new SpirePiece(base, seed))));
    }

    /** The first open block over the ground (or frozen water) here. */
    private int ground(GenerationContext context, int x, int z) {
        return context.chunkGenerator().getBaseHeight(x, z, Heightmap.Types.WORLD_SURFACE_WG, context.heightAccessor(), context.randomState());
    }

    @Override
    public StructureType<?> type() {
        return ModRegistries.SPIRE.get();
    }
}
