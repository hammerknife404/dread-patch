package io.github.hammerknife404.dreadpatch.world;

import io.github.hammerknife404.dreadpatch.DreadPatch;
import io.github.hammerknife404.dreadpatch.DreadPatchConfig;
import io.github.hammerknife404.dreadpatch.magic.ArrivalRules;
import io.github.hammerknife404.dreadpatch.magic.DreadlandsSpawning;
import io.github.hammerknife404.dreadpatch.entity.DreadPhantom;
import io.github.hammerknife404.dreadpatch.registry.ModRegistries;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.TagKey;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Difficulty;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.NaturalSpawner;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.EventHooks;
import net.neoforged.neoforge.event.tick.LevelTickEvent;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * The Dreadlands (0.46.0): a dimension of bleak frozen waste ({@code data/dreadpatch/dimension}), with
 * rare tall spires ({@link SpireStructure}). Its time and weather are the overworld's (every other
 * level's data is the overworld's, read through). Reached through a Dread Gate, arriving somewhere
 * random clear of the spires; left through a Dread Gate keyed there, back to where you came in. Dread phantoms
 * come down out of its sky now and then, never for want of sleep.
 */
public final class Dreadlands {
    public static final ResourceKey<Level> LEVEL = ResourceKey.create(Registries.DIMENSION, DreadPatch.id("dreadlands"));
    public static final TagKey<Structure> SPIRES = TagKey.create(Registries.STRUCTURE, DreadPatch.id("spires"));
    /**
     * Its dread phantoms (the owner: 1-2 at a time, at a low rate, not tied to sleep): every 30 s, a
     * player under open sky has a one-in-8 chance of 1-2 (about one flight every 4 minutes), one in 3
     * within 96 blocks of a spire (0.52.0, the owner's second drop: "more around spires"; both the server
     * config's); never while 4 wild ones are near already.
     */
    static final int PHANTOM_INTERVAL = 600;
    static final int PHANTOM_CAP = 4;
    /** How far (in chunks) to look for a spire a player is near. */
    static final int SPIRE_NEAR_CHUNKS = 7;

    /** Where someone arrives, facing the spire. */
    public record Arrival(Vec3 pos, float yaw) {
    }

    private Dreadlands() {
    }

    public static void register(IEventBus gameBus) {
        gameBus.addListener(Dreadlands::onLevelTick);
    }

    public static boolean is(Level level) {
        return level.dimension() == LEVEL;
    }

    /**
     * Where someone coming in near {@code near} arrives (0.60.0, the owner: "somewhere random, but never on
     * a tower or within 48 blocks from one. And prefer the surface, but caves or similar are ok";
     * {@link ArrivalRules}): a random spot within 256 blocks of {@code near}'s x and z, clear of every
     * spire, on dry ground at the surface (generating it); failing every try there, the first dry floor
     * lower down at a spot clear of spires (a cave); failing that, the first clear spot's surface, wet or
     * not. Facing anywhere. Null if there's no Dreadlands.
     */
    @Nullable
    public static Arrival arrival(MinecraftServer server, BlockPos near, RandomSource random) {
        ServerLevel lands = server.getLevel(LEVEL);
        if (lands == null) {
            return null;
        }
        float yaw = random.nextFloat() * 360.0F;
        List<BlockPos> clear = new ArrayList<>();
        for (int i = 0; i < ArrivalRules.TRIES; i++) {
            double[] offset = ArrivalRules.offset(random.nextDouble(), random.nextDouble());
            int x = Mth.floor(near.getX() + 0.5 + offset[0]);
            int z = Mth.floor(near.getZ() + 0.5 + offset[1]);
            if (!clearOfSpires(lands, x, z)) {
                continue;
            }
            lands.getChunk(x >> 4, z >> 4);
            BlockPos surface = feet(lands, x, z);
            if (standable(lands, surface)) {
                return new Arrival(Vec3.atBottomCenterOf(surface), yaw);
            }
            clear.add(surface);
        }
        for (BlockPos surface : clear) {
            for (BlockPos below = surface.below(2); below.getY() > lands.getMinBuildHeight() + 1; below = below.below()) {
                if (standable(lands, below)) {
                    return new Arrival(Vec3.atBottomCenterOf(below), yaw);
                }
            }
        }
        BlockPos fallback = !clear.isEmpty() ? clear.get(0) : lands.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, near);
        return new Arrival(Vec3.atBottomCenterOf(fallback), yaw);
    }

    /** Whether no spire stands within {@link ArrivalRules#CLEARANCE} of x, z (its structure placement; no chunk generated). */
    static boolean clearOfSpires(ServerLevel lands, int x, int z) {
        int chunks = (int) Math.ceil(ArrivalRules.FROM_MIDDLE / 16.0) + 1;
        BlockPos spire = lands.findNearestMapStructure(SPIRES, new BlockPos(x, 64, z), chunks, false);
        return spire == null || ArrivalRules.clearOfSpire((spire.getX() & ~15) + 8.5 - (x + 0.5), (spire.getZ() & ~15) + 8.5 - (z + 0.5));
    }

    /** The open spot on top of x, z: on the ground, in any snow layer (the biome lays one on everything) or plant on it. */
    static BlockPos feet(ServerLevel lands, int x, int z) {
        BlockPos pos = new BlockPos(x, lands.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z), z);
        while (pos.getY() > lands.getMinBuildHeight() + 1 && lands.getBlockState(pos.below()).canBeReplaced()
                && lands.getFluidState(pos.below()).isEmpty()) {
            pos = pos.below();
        }
        return pos;
    }

    /** Room to stand at {@code feet}: two clear blocks, no fluid there or below, and a sturdy floor. */
    static boolean standable(ServerLevel lands, BlockPos feet) {
        BlockPos floor = feet.below();
        return lands.getFluidState(floor).isEmpty() && lands.getFluidState(feet).isEmpty()
                && lands.getBlockState(floor).isFaceSturdy(lands, floor, Direction.UP)
                && lands.noCollision(new AABB(feet.getX() + 0.2, feet.getY(), feet.getZ() + 0.2, feet.getX() + 0.8, feet.getY() + 1.8, feet.getZ() + 0.8));
    }

    /** Takes {@code player} into the Dreadlands, remembering where they came from. */
    public static void enter(ServerPlayer player, Arrival arrival, DreadlandsData.Origin from) {
        ServerLevel lands = player.server.getLevel(LEVEL);
        if (lands == null) {
            return;
        }
        DreadlandsData.get(player.server).setOrigin(player.getUUID(), from);
        player.stopRiding();
        player.teleportTo(lands, arrival.pos().x, arrival.pos().y, arrival.pos().z, arrival.yaw(), 0.0F);
        lands.playSound(null, arrival.pos().x, arrival.pos().y, arrival.pos().z, SoundEvents.PORTAL_TRAVEL, SoundSource.PLAYERS, 0.4F, 0.6F);
        player.displayClientMessage(Component.translatable("message.dreadpatch.dreadlands.arrive"), true);
    }

    /** Takes {@code player} home: where they came in from, else the world's spawn. */
    public static void goHome(ServerPlayer player) {
        MinecraftServer server = player.server;
        DreadlandsData.Origin origin = DreadlandsData.get(server).origin(player.getUUID());
        ServerLevel level = origin != null ? server.getLevel(origin.level()) : null;
        player.stopRiding();
        if (level != null && !is(level)) {
            player.teleportTo(level, origin.pos().x, origin.pos().y, origin.pos().z, origin.yaw(), 0.0F);
        } else {
            ServerLevel overworld = server.overworld();
            BlockPos spawn = overworld.getSharedSpawnPos();
            overworld.getChunk(spawn.getX() >> 4, spawn.getZ() >> 4);
            int y = overworld.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, spawn.getX(), spawn.getZ());
            player.teleportTo(overworld, spawn.getX() + 0.5, y, spawn.getZ() + 0.5, overworld.getSharedSpawnAngle(), 0.0F);
        }
        player.level().playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.PORTAL_TRAVEL, SoundSource.PLAYERS, 0.4F, 0.8F);
    }

    /** Whether {@code pos} is within {@link DreadlandsSpawning#SPIRE_REACH} of a spire's middle. */
    public static boolean nearSpire(ServerLevel level, BlockPos pos) {
        BlockPos spire = level.findNearestMapStructure(SPIRES, pos, SPIRE_NEAR_CHUNKS, false);
        if (spire == null) {
            return false;
        }
        double dx = (spire.getX() & ~15) + 8.5 - pos.getX();
        double dz = (spire.getZ() & ~15) + 8.5 - pos.getZ();
        return dx * dx + dz * dz <= DreadlandsSpawning.SPIRE_REACH * DreadlandsSpawning.SPIRE_REACH;
    }

    /** Now and then, 1-2 dread phantoms over a player under the open sky; more often near a spire. */
    private static void onLevelTick(LevelTickEvent.Post event) {
        if (!(event.getLevel() instanceof ServerLevel level) || !is(level) || level.getGameTime() % PHANTOM_INTERVAL != 0
                || level.getDifficulty() == Difficulty.PEACEFUL || !level.getGameRules().getBoolean(GameRules.RULE_DOMOBSPAWNING)) {
            return;
        }
        RandomSource random = level.getRandom();
        for (ServerPlayer player : level.players()) {
            if (player.isSpectator() || !level.canSeeSky(player.blockPosition())
                    || !DreadlandsSpawning.comesUp(DreadPatchConfig.phantomOneIn(nearSpire(level, player.blockPosition())), new java.util.Random(random.nextLong()))) {
                continue;
            }
            int wild = level.getEntitiesOfClass(DreadPhantom.class, player.getBoundingBox().inflate(64.0), p -> p.getSummonerId() == null).size();
            if (wild >= PHANTOM_CAP) {
                continue;
            }
            BlockPos spot = player.blockPosition().above(20 + random.nextInt(15)).east(-10 + random.nextInt(21)).south(-10 + random.nextInt(21));
            if (!NaturalSpawner.isValidEmptySpawnBlock(level, spot, level.getBlockState(spot), level.getFluidState(spot), ModRegistries.DREAD_PHANTOM.get())) {
                continue;
            }
            int count = Math.min(PHANTOM_CAP - wild, 1 + random.nextInt(2));
            for (int i = 0; i < count; i++) {
                DreadPhantom phantom = ModRegistries.DREAD_PHANTOM.get().create(level);
                if (phantom == null) {
                    break;
                }
                phantom.moveTo(spot, 0.0F, 0.0F);
                EventHooks.finalizeMobSpawn(phantom, level, level.getCurrentDifficultyAt(spot), MobSpawnType.NATURAL, null);
                level.addFreshEntityWithPassengers(phantom);
            }
        }
    }
}
