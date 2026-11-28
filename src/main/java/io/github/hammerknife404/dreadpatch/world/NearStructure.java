package io.github.hammerknife404.dreadpatch.world;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.hammerknife404.dreadpatch.registry.ModRegistries;
import net.minecraft.core.Holder;
import net.minecraft.core.QuartPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureSet;
import net.minecraft.world.level.levelgen.structure.StructureType;
import net.minecraft.world.level.levelgen.structure.placement.RandomSpreadStructurePlacement;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

/**
 * Another structure, placed only near one of a structure set's (0.98.12, the owner: "allow ice and fires graveyard
 * structures to sometimes spawn near dread villages"): Ice and Fire's graveyard near the Dreadlands' villages. Its own set
 * tries it on a fine grid; it's made only where the {@code near} set's placement puts one of its structures
 * {@code min_chunks}-{@code max_chunks} chunks off (by the same seed, as the world places them), in a biome that set's
 * structure takes, and then {@code chance} of the time. The structure it builds ({@code structure}) builds itself, as
 * its own type does; this only chooses where. (Ice and Fire's graveyard rolls its own {@code generateGraveYardChance}
 * too, 0.5 by default, so the graveyards' {@code chance} is 1: theirs alone.)
 */
public class NearStructure extends Structure {
    public static final MapCodec<NearStructure> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            settingsCodec(i),
            Structure.CODEC.fieldOf("structure").forGetter(s -> s.structure),
            ResourceKey.codec(Registries.STRUCTURE_SET).fieldOf("near").forGetter(s -> s.near),
            TagKey.codec(Registries.BIOME).fieldOf("near_biomes").forGetter(s -> s.nearBiomes),
            Codec.intRange(0, 32).fieldOf("min_chunks").forGetter(s -> s.minChunks),
            Codec.intRange(1, 32).fieldOf("max_chunks").forGetter(s -> s.maxChunks),
            Codec.floatRange(0.0F, 1.0F).fieldOf("chance").forGetter(s -> s.chance)
    ).apply(i, NearStructure::new));

    private final Holder<Structure> structure;
    private final ResourceKey<StructureSet> near;
    private final TagKey<Biome> nearBiomes;
    private final int minChunks;
    private final int maxChunks;
    private final float chance;

    public NearStructure(StructureSettings settings, Holder<Structure> structure, ResourceKey<StructureSet> near, TagKey<Biome> nearBiomes,
                         int minChunks, int maxChunks, float chance) {
        super(settings);
        this.structure = structure;
        this.near = near;
        this.nearBiomes = nearBiomes;
        this.minChunks = minChunks;
        this.maxChunks = maxChunks;
        this.chance = chance;
    }

    @Override
    protected Optional<GenerationStub> findGenerationPoint(GenerationContext context) {
        if (context.random().nextFloat() >= this.chance || !this.nearOne(context)) {
            return Optional.empty();
        }
        return this.structure.value().findValidGenerationPoint(context);
    }

    /** Whether the {@code near} set's placement puts one of its structures in range of this chunk, in a biome it takes. */
    private boolean nearOne(GenerationContext context) {
        Optional<Holder.Reference<StructureSet>> set = context.registryAccess().registryOrThrow(Registries.STRUCTURE_SET).getHolder(this.near);
        if (set.isEmpty() || !(set.get().value().placement() instanceof RandomSpreadStructurePlacement placement)) {
            return false;
        }
        ChunkPos here = context.chunkPos();
        Set<ChunkPos> candidates = new HashSet<>();
        // Every chunk in range asked for its region's spot: every region the range reaches covered, whatever its spacing.
        for (int dx = -this.maxChunks; dx <= this.maxChunks; dx++) {
            for (int dz = -this.maxChunks; dz <= this.maxChunks; dz++) {
                candidates.add(placement.getPotentialStructureChunk(context.seed(), here.x + dx, here.z + dz));
            }
        }
        for (ChunkPos candidate : candidates) {
            int dx = candidate.x - here.x;
            int dz = candidate.z - here.z;
            int distanceSq = dx * dx + dz * dz;
            if (distanceSq < this.minChunks * this.minChunks || distanceSq > this.maxChunks * this.maxChunks) {
                continue;
            }
            Holder<Biome> biome = context.biomeSource().getNoiseBiome(QuartPos.fromBlock(candidate.getMiddleBlockX()), QuartPos.fromBlock(80),
                    QuartPos.fromBlock(candidate.getMiddleBlockZ()), context.randomState().sampler());
            if (biome.is(this.nearBiomes)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public StructureType<?> type() {
        return ModRegistries.NEAR_STRUCTURE.get();
    }
}
