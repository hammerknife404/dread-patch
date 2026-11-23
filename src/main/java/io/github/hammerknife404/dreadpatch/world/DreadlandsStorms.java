package io.github.hammerknife404.dreadpatch.world;

import com.iafenvoy.iceandfire.entity.util.IDreadMob;
import io.github.hammerknife404.dreadpatch.magic.StormRules;
import io.github.hammerknife404.dreadpatch.registry.ModRegistries;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.tick.LevelTickEvent;

import java.util.Random;

/**
 * The Dreadlands' storms (0.48.0), on the server: while it snows there (the overworld's rain, read
 * through), everyone under the open sky but the Dread is slowed by Rimefrost; no damage. In thunder,
 * lightning strikes round players ({@link StormRules#STRIKE_ONE_IN}) and leaves a patch of rimefire
 * ({@link io.github.hammerknife404.dreadpatch.block.RimefireBlock}). The fog is the client's
 * ({@code DreadlandsClient}).
 */
public final class DreadlandsStorms {
    private DreadlandsStorms() {
    }

    public static void register(IEventBus gameBus) {
        gameBus.addListener(DreadlandsStorms::onLevelTick);
    }

    private static void onLevelTick(LevelTickEvent.Post event) {
        if (!(event.getLevel() instanceof ServerLevel level) || !Dreadlands.is(level) || !level.isRaining()) {
            return;
        }
        if (level.getGameTime() % 20 == 0) {
            chill(level);
        }
        if (level.isThundering()) {
            RandomSource random = level.getRandom();
            for (ServerPlayer player : level.players()) {
                if (!player.isSpectator() && random.nextInt(StormRules.STRIKE_ONE_IN) == 0) {
                    int x = player.getBlockX() + random.nextInt(2 * StormRules.STRIKE_RANGE + 1) - StormRules.STRIKE_RANGE;
                    int z = player.getBlockZ() + random.nextInt(2 * StormRules.STRIKE_RANGE + 1) - StormRules.STRIKE_RANGE;
                    if (level.hasChunk(x >> 4, z >> 4)) {
                        strike(level, level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING, new BlockPos(x, 0, z)));
                    }
                }
            }
        }
    }

    /** Rimefrost on every living thing under the open sky, but the Dread. */
    private static void chill(ServerLevel level) {
        for (Entity entity : level.getAllEntities()) {
            if (entity instanceof LivingEntity living && living.isAlive() && !(living instanceof IDreadMob) && !living.isSpectator() && exposed(level, living)) {
                living.addEffect(new MobEffectInstance(ModRegistries.RIMEFROST, StormRules.RIMEFROST_TICKS, level.isThundering() ? io.github.hammerknife404.dreadpatch.effect.RimefrostEffect.HEAVY : 0, true, true));
            }
        }
    }

    /** Whether the open sky is over its head. */
    public static boolean exposed(ServerLevel level, LivingEntity living) {
        return level.canSeeSky(BlockPos.containing(living.getX(), living.getEyeY(), living.getZ()));
    }

    /**
     * Lightning at {@code at}: a bolt (seen and heard; it strikes those round it as lightning does, but
     * sets no fire of its own) and a rimefire patch where it struck.
     */
    public static void strike(ServerLevel level, BlockPos at) {
        LightningBolt bolt = EntityType.LIGHTNING_BOLT.create(level);
        if (bolt == null) {
            return;
        }
        bolt.moveTo(Vec3.atBottomCenterOf(at));
        bolt.setVisualOnly(true);
        level.addFreshEntity(bolt);
        AABB reach = new AABB(at.getX() - 3.0, at.getY() - 3.0, at.getZ() - 3.0, at.getX() + 4.0, at.getY() + 9.0, at.getZ() + 4.0);
        for (Entity struck : level.getEntities(bolt, reach, Entity::isAlive)) {
            if (!(struck instanceof IDreadMob)) {
                struck.thunderHit(level, bolt);
            }
        }
        ignite(level, at);
    }

    /** Lays a rimefire patch round {@code at} on whatever ground will hold it. How many blocks took. */
    public static int ignite(ServerLevel level, BlockPos at) {
        BlockState rimefire = ModRegistries.RIMEFIRE.get().defaultBlockState();
        int lit = 0;
        for (int[] cell : StormRules.patch(new Random(level.getRandom().nextLong()))) {
            for (int dy = 2; dy >= -3; dy--) {
                BlockPos pos = at.offset(cell[0], dy, cell[1]);
                BlockState here = level.getBlockState(pos);
                if ((here.isAir() || here.canBeReplaced()) && here.getFluidState().isEmpty() && rimefire.canSurvive(level, pos)) {
                    level.setBlock(pos, rimefire, Block.UPDATE_ALL);
                    lit++;
                    break;
                }
            }
        }
        return lit;
    }
}
