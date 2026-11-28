package io.github.hammerknife404.dreadpatch.world;

import com.iafenvoy.iceandfire.entity.DreadMobEntity;
import com.iafenvoy.iceandfire.entity.util.IDreadMob;
import io.github.hammerknife404.dreadpatch.DreadPatchConfig;
import io.github.hammerknife404.dreadpatch.entity.DreadPatrols;
import io.github.hammerknife404.dreadpatch.magic.DreadlandsSpawning;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.tags.EntityTypeTags;
import net.minecraft.world.Difficulty;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.EventHooks;
import net.neoforged.neoforge.event.tick.LevelTickEvent;
import org.jetbrains.annotations.Nullable;

import java.util.Random;
import java.util.function.Predicate;

/**
 * What rises on its own in the Dreadlands (0.52.0, the owner's second drop; {@link DreadlandsSpawning}):
 * the Dread in small groups, day and night, more in snow and a little more in a heavy snowstorm; and
 * small patrols; and now and then vanilla's undead, strayed in (0.60.0). Round each player (not spectating), not on Peaceful, with {@code doMobSpawning}; the
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
            if (dread && DreadlandsSpawning.comesUp(DreadPatchConfig.undeadOneIn(), roll)) {
                raiseUndead(level, player, roll, random);
            }
            if (dread && DreadlandsSpawning.comesUp(DreadPatchConfig.iceSpiderOneIn(), roll) && Dreadlands.nearVillage(level, player.blockPosition())) {
                raiseIceSpiders(level, player, roll, random);
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

    /**
     * 1-2 of vanilla's undead, strayed in (0.60.0), on the ground near the player, rising as vanilla's
     * own natural spawns do (their own gear, despawning as monsters do); never past the cap of them.
     */
    private static void raiseUndead(ServerLevel level, ServerPlayer player, Random roll, RandomSource random) {
        int standing = level.getEntitiesOfClass(Monster.class, player.getBoundingBox().inflate(DreadlandsSpawning.CAP_RANGE),
                mob -> mob.getType().is(EntityTypeTags.UNDEAD) && !(mob instanceof IDreadMob)).size();
        int count = Math.min(DreadPatchConfig.undeadCap() - standing, 1 + roll.nextInt(2));
        Vec3 at = count > 0 ? ground(level, player, DreadlandsSpawning.MIN_DISTANCE, DreadlandsSpawning.MAX_DISTANCE, random) : null;
        if (at == null) {
            return;
        }
        for (int i = 0; i < count; i++) {
            EntityType<? extends Monster> type = switch (DreadlandsSpawning.undead(roll.nextInt(DreadlandsSpawning.UNDEAD_WEIGHT))) {
                case STRAY -> EntityType.STRAY;
                case ZOMBIE -> EntityType.ZOMBIE;
                case SKELETON -> EntityType.SKELETON;
                case ZOMBIE_VILLAGER -> EntityType.ZOMBIE_VILLAGER;
            };
            Monster mob = type.create(level);
            if (mob == null) {
                continue;
            }
            mob.moveTo(at.x + random.nextInt(5) - 2, at.y, at.z + random.nextInt(5) - 2, random.nextFloat() * 360.0F, 0.0F);
            if (!level.noCollision(mob)) {
                mob.moveTo(at.x, at.y, at.z, mob.getYRot(), 0.0F);
            }
            EventHooks.finalizeMobSpawn(mob, level, level.getCurrentDifficultyAt(mob.blockPosition()), MobSpawnType.NATURAL, null);
            level.addFreshEntityWithPassengers(mob);
        }
    }

    /** Iron's ice spider, if Iron's has it. */
    static final net.minecraft.resources.ResourceLocation ICE_SPIDER = net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("irons_spellbooks", "ice_spider");

    /** 1-2 of Iron's ice spiders near a village (0.98.6), on the ground near the player; never past the cap of them. */
    private static void raiseIceSpiders(ServerLevel level, ServerPlayer player, Random roll, RandomSource random) {
        EntityType<?> type = net.minecraft.core.registries.BuiltInRegistries.ENTITY_TYPE.get(ICE_SPIDER);
        if (!net.minecraft.core.registries.BuiltInRegistries.ENTITY_TYPE.containsKey(ICE_SPIDER)) {
            return;
        }
        int standing = level.getEntities(type, player.getBoundingBox().inflate(DreadlandsSpawning.CAP_RANGE), entity -> true).size();
        int count = Math.min(DreadPatchConfig.iceSpiderCap() - standing, 1 + roll.nextInt(2));
        Vec3 at = count > 0 ? ground(level, player, DreadlandsSpawning.MIN_DISTANCE, DreadlandsSpawning.MAX_DISTANCE, random) : null;
        if (at == null) {
            return;
        }
        for (int i = 0; i < count; i++) {
            if (!(type.create(level) instanceof net.minecraft.world.entity.Mob spider)) {
                return;
            }
            spider.moveTo(at.x + random.nextInt(5) - 2, at.y, at.z + random.nextInt(5) - 2, random.nextFloat() * 360.0F, 0.0F);
            if (!level.noCollision(spider)) {
                spider.moveTo(at.x, at.y, at.z, spider.getYRot(), 0.0F);
            }
            EventHooks.finalizeMobSpawn(spider, level, level.getCurrentDifficultyAt(spider.blockPosition()), MobSpawnType.NATURAL, null);
            level.addFreshEntityWithPassengers(spider);
        }
    }

    /** Open ground {@code min}-{@code max} blocks from the player, in a loaded chunk, not under water; null if the tries find none. */
    @Nullable
    static Vec3 ground(ServerLevel level, ServerPlayer player, double min, double max, RandomSource random) {
        return ground(level, player, min, max, random, pos -> true);
    }

    /** The same, where {@code ok} takes the spot (the feet's block). */
    @Nullable
    static Vec3 ground(ServerLevel level, ServerPlayer player, double min, double max, RandomSource random, Predicate<BlockPos> ok) {
        for (int attempt = 0; attempt < 8; attempt++) {
            double angle = random.nextDouble() * Math.PI * 2.0;
            double distance = min + random.nextDouble() * (max - min);
            int x = Mth.floor(player.getX() + Math.cos(angle) * distance);
            int z = Mth.floor(player.getZ() + Math.sin(angle) * distance);
            if (!level.hasChunk(x >> 4, z >> 4)) {
                continue;
            }
            int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
            if (!level.getFluidState(new BlockPos(x, y - 1, z)).isEmpty() || !level.noCollision(new AABB(x, y, z, x + 1, y + 2, z + 1))
                    || !ok.test(new BlockPos(x, y, z))) {
                continue;
            }
            return new Vec3(x + 0.5, y, z + 0.5);
        }
        return null;
    }
}
