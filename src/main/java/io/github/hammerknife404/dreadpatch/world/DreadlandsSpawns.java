package io.github.hammerknife404.dreadpatch.world;

import com.iafenvoy.iceandfire.entity.DreadMobEntity;
import io.github.hammerknife404.dreadpatch.DreadPatchConfig;
import io.github.hammerknife404.dreadpatch.entity.DreadPatrols;
import io.github.hammerknife404.dreadpatch.magic.DreadlandsSpawning;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Difficulty;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.tick.LevelTickEvent;
import org.jetbrains.annotations.Nullable;

import java.util.Random;

/**
 * What rises on its own in the Dreadlands (0.52.0, the owner's second drop; {@link DreadlandsSpawning}):
 * the Dread in small groups, day and night, more in snow and a little more in a heavy snowstorm; and
 * small patrols. Round each player (not spectating), not on Peaceful, with {@code doMobSpawning}; the
 * rates are the server config's. (Its dread phantoms are {@link Dreadlands}'.) Out of sight of nothing
 * in particular: 24-48 blocks off, on the open ground.
 */
public final class DreadlandsSpawns {
    /** No patrol rises while another's leader is this near the player. */
    static final double PATROL_SPACING = 128.0;

    private DreadlandsSpawns() {
    }

    public static void register(IEventBus gameBus) {
        gameBus.addListener(DreadlandsSpawns::onLevelTick);
    }

    private static void onLevelTick(LevelTickEvent.Post event) {
        if (!(event.getLevel() instanceof ServerLevel level) || !Dreadlands.is(level) || level.getDifficulty() == Difficulty.PEACEFUL
                || !level.getGameRules().getBoolean(GameRules.RULE_DOMOBSPAWNING)) {
            return;
        }
        long time = level.getGameTime();
        boolean dread = time % DreadlandsSpawning.SPAWN_INTERVAL == 0;
        boolean patrols = time % DreadlandsSpawning.PATROL_INTERVAL == 0;
        if (!dread && !patrols) {
            return;
        }
        RandomSource random = level.getRandom();
        DreadlandsSpawning.Weather weather = level.isThundering() ? DreadlandsSpawning.Weather.STORM
                : level.isRaining() ? DreadlandsSpawning.Weather.SNOW : DreadlandsSpawning.Weather.CLEAR;
        for (ServerPlayer player : level.players()) {
            if (player.isSpectator()) {
                continue;
            }
            Random roll = new Random(random.nextLong());
            if (dread && DreadlandsSpawning.comesUp(DreadPatchConfig.dreadSpawnOneIn(weather), roll)) {
                int standing = level.getEntitiesOfClass(DreadMobEntity.class, player.getBoundingBox().inflate(DreadlandsSpawning.CAP_RANGE)).size();
                int count = DreadlandsSpawning.groupSize(standing, DreadPatchConfig.dreadCap(), roll);
                Vec3 at = count > 0 ? ground(level, player, DreadlandsSpawning.MIN_DISTANCE, DreadlandsSpawning.MAX_DISTANCE, random) : null;
                if (at != null) {
                    DreadPatrols.raiseLoose(level, at, count, random);
                }
            }
            if (patrols && DreadlandsSpawning.comesUp(DreadPatchConfig.patrolOneIn(), roll)
                    && !DreadPatrols.patrolNear(level, player.position(), PATROL_SPACING)) {
                Vec3 at = ground(level, player, DreadlandsSpawning.PATROL_MIN_DISTANCE, DreadlandsSpawning.PATROL_MAX_DISTANCE, random);
                if (at != null) {
                    // Its first leg passes by the player's ground.
                    BlockPos toward = level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                            player.blockPosition().offset(random.nextInt(33) - 16, 0, random.nextInt(33) - 16));
                    DreadPatrols.raise(level, at, toward, random);
                }
            }
        }
    }

    /** Open ground {@code min}-{@code max} blocks from the player, in a loaded chunk, not under water; null if the tries find none. */
    @Nullable
    static Vec3 ground(ServerLevel level, ServerPlayer player, double min, double max, RandomSource random) {
        for (int attempt = 0; attempt < 8; attempt++) {
            double angle = random.nextDouble() * Math.PI * 2.0;
            double distance = min + random.nextDouble() * (max - min);
            int x = Mth.floor(player.getX() + Math.cos(angle) * distance);
            int z = Mth.floor(player.getZ() + Math.sin(angle) * distance);
            if (!level.hasChunk(x >> 4, z >> 4)) {
                continue;
            }
            int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
            if (!level.getFluidState(new BlockPos(x, y - 1, z)).isEmpty() || !level.noCollision(new AABB(x, y, z, x + 1, y + 2, z + 1))) {
                continue;
            }
            return new Vec3(x + 0.5, y, z + 0.5);
        }
        return null;
    }
}
