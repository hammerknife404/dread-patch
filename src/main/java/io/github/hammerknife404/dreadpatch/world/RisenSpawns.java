package io.github.hammerknife404.dreadpatch.world;

import io.github.hammerknife404.dreadpatch.DreadPatchConfig;
import io.github.hammerknife404.dreadpatch.entity.MiniatureDreadPhantom;
import io.github.hammerknife404.dreadpatch.entity.RisenDread;
import io.github.hammerknife404.dreadpatch.magic.DreadlandsSpawning;
import io.github.hammerknife404.dreadpatch.magic.RisenRules;
import io.github.hammerknife404.dreadpatch.registry.ModRegistries;
import io.github.hammerknife404.dreadpatch.magic.TerrainRules;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Difficulty;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.levelgen.Heightmap;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.EventHooks;
import net.neoforged.neoforge.event.tick.LevelTickEvent;
import org.jetbrains.annotations.Nullable;

import java.util.Random;

/**
 * Risen Dread stirring on their own (0.63.0, the owner: "allow the smaller dread phantoms rarely spawn in caves,
 * same as the aquatic beast variant in the rare chance of underground water"): in the overworld and the
 * Dreadlands, every 10 s each player (not spectating; not on Peaceful; with {@code doMobSpawning}) has a
 * one-in-{@code cave_dread_phantoms} chance of a miniature dread phantom in dark cave air near them, and a
 * one-in-{@code cave_dread_water} chance of a Dread water creature in underground water near them
 * ({@link RisenRules}: 12-32 blocks off, within 12 up or down, under the ground's top, no sky; the air no
 * brighter than 7 by block light), never past {@link RisenRules#CAVE_CAP} of them near. No raiser, so they
 * hunt players. In the Rime Chasma (0.68.0) a further one-in-{@code chasma_dread_water} of one in a pocket of water
 * sealed in its ice ({@link DreadTerrain.ChasmaPocket}), searched for column by column under the floor.
 */
public final class RisenSpawns {
    private RisenSpawns() {
    }

    public static void register(IEventBus gameBus) {
        gameBus.addListener(RisenSpawns::onLevelTick);
    }

    private static void onLevelTick(LevelTickEvent.Post event) {
        if (!(event.getLevel() instanceof ServerLevel level) || level.dimension() != Level.OVERWORLD && !Dreadlands.is(level)
                || level.getGameTime() % DreadlandsSpawning.SPAWN_INTERVAL != 0 || level.getDifficulty() == Difficulty.PEACEFUL
                || !level.getGameRules().getBoolean(GameRules.RULE_DOMOBSPAWNING)) {
            return;
        }
        RandomSource random = level.getRandom();
        for (ServerPlayer player : level.players()) {
            if (player.isSpectator()) {
                continue;
            }
            Random roll = new Random(random.nextLong());
            boolean phantom = DreadlandsSpawning.comesUp(DreadPatchConfig.cavePhantomOneIn(), roll);
            boolean water = DreadlandsSpawning.comesUp(DreadPatchConfig.caveWaterOneIn(), roll);
            boolean pocket = Dreadlands.is(level) && level.getBiome(player.blockPosition()).is(Dreadlands.RIME_CHASMA)
                    && DreadlandsSpawning.comesUp(DreadPatchConfig.chasmaWaterOneIn(), roll);
            if (!phantom && !water && !pocket || level.getEntitiesOfClass(Mob.class, player.getBoundingBox().inflate(DreadlandsSpawning.CAP_RANGE),
                    mob -> mob instanceof MiniatureDreadPhantom || RisenDread.is(mob)).size() >= RisenRules.CAVE_CAP) {
                continue;
            }
            if (phantom) {
                BlockPos spot = cave(level, player, random, false);
                MiniatureDreadPhantom mini = spot == null ? null : ModRegistries.MINIATURE_DREAD_PHANTOM.get().create(level);
                if (mini != null) {
                    add(level, mini, spot, random);
                }
            }
            if (water) {
                spawnWater(level, cave(level, player, random, true), roll, random);
            }
            if (pocket) {
                spawnWater(level, pocket(level, player, random), roll, random);
            }
        }
    }

    private static void spawnWater(ServerLevel level, @Nullable BlockPos spot, Random roll, RandomSource random) {
        if (spot == null) {
            return;
        }
        EntityType<? extends Mob> kind = switch (RisenRules.waterKind(roll.nextInt(RisenRules.WATER_WEIGHT))) {
            case GLOW_SQUID -> EntityType.GLOW_SQUID;
            case SQUID -> EntityType.SQUID;
            case COD -> EntityType.COD;
            case SALMON -> EntityType.SALMON;
        };
        Mob beast = kind.create(level);
        if (beast != null) {
            beast.moveTo(spot.getX() + 0.5, spot.getY() + 0.1, spot.getZ() + 0.5, random.nextFloat() * 360.0F, 0.0F);
            EventHooks.finalizeMobSpawn(beast, level, level.getCurrentDifficultyAt(spot), MobSpawnType.NATURAL, null);
            RisenDread.makeDread(beast, null);
            level.addFreshEntity(beast);
        }
    }

    /** Water in a pocket sealed under the Rime Chasma's floor near the player (a source with water over it); null if none's found. */
    @Nullable
    static BlockPos pocket(ServerLevel level, ServerPlayer player, RandomSource random) {
        for (int attempt = 0; attempt < 32; attempt++) {
            double angle = random.nextDouble() * Math.PI * 2.0;
            double distance = 6 + random.nextDouble() * (RisenRules.CAVE_MAX - 6);
            int x = Mth.floor(player.getX() + Math.cos(angle) * distance);
            int z = Mth.floor(player.getZ() + Math.sin(angle) * distance);
            if (!level.hasChunk(x >> 4, z >> 4)) {
                continue;
            }
            int top = level.getHeight(Heightmap.Types.WORLD_SURFACE, x, z) - 1;
            for (int y = top - TerrainRules.POCKET_DEPTH + 2; y >= top - TerrainRules.POCKET_DEPTH - TerrainRules.POCKET_DEPTH_SPREAD - 3; y--) {
                BlockPos pos = new BlockPos(x, y, z);
                if (level.getFluidState(pos).isSourceOfType(net.minecraft.world.level.material.Fluids.WATER)
                        && level.getFluidState(pos.above()).is(FluidTags.WATER)) {
                    return pos;
                }
            }
        }
        return null;
    }

    private static void add(ServerLevel level, Mob mob, BlockPos spot, RandomSource random) {
        mob.moveTo(spot.getX() + 0.5, spot.getY() + 0.2, spot.getZ() + 0.5, random.nextFloat() * 360.0F, 0.0F);
        EventHooks.finalizeMobSpawn(mob, level, level.getCurrentDifficultyAt(spot), MobSpawnType.NATURAL, null);
        level.addFreshEntity(mob);
    }

    /** Dark cave air (two clear blocks), or underground water (a water block with water or air over it), near the player; null if the tries find none. */
    @Nullable
    static BlockPos cave(ServerLevel level, ServerPlayer player, RandomSource random, boolean water) {
        for (int attempt = 0; attempt < 12; attempt++) {
            double angle = random.nextDouble() * Math.PI * 2.0;
            double distance = RisenRules.CAVE_MIN + random.nextDouble() * (RisenRules.CAVE_MAX - RisenRules.CAVE_MIN);
            int x = Mth.floor(player.getX() + Math.cos(angle) * distance);
            int z = Mth.floor(player.getZ() + Math.sin(angle) * distance);
            int y = player.getBlockY() + random.nextInt(2 * RisenRules.CAVE_DEPTH + 1) - RisenRules.CAVE_DEPTH;
            BlockPos pos = new BlockPos(x, y, z);
            if (!level.hasChunk(x >> 4, z >> 4) || y <= level.getMinBuildHeight() || level.canSeeSky(pos)
                    || y >= level.getHeight(Heightmap.Types.WORLD_SURFACE, x, z) - 4) {
                continue;
            }
            if (water ? level.getFluidState(pos).is(FluidTags.WATER) && level.getFluidState(pos).isSource()
                    && (level.getFluidState(pos.above()).is(FluidTags.WATER) || level.getBlockState(pos.above()).isAir())
                    : level.getBlockState(pos).isAir() && level.getBlockState(pos.above()).isAir() && level.getFluidState(pos).isEmpty()
                    && level.getBrightness(LightLayer.BLOCK, pos) <= 7) {
                return pos;
            }
        }
        return null;
    }
}
