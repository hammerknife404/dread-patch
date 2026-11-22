package io.github.hammerknife404.dreadpatch.world;

import io.github.hammerknife404.dreadpatch.DreadPatch;
import io.github.hammerknife404.dreadpatch.entity.DreadPhantom;
import io.github.hammerknife404.dreadpatch.registry.ModRegistries;
import net.minecraft.core.BlockPos;
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
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.EventHooks;
import net.neoforged.neoforge.event.tick.LevelTickEvent;
import org.jetbrains.annotations.Nullable;

/**
 * The Dreadlands (0.46.0): a dimension of bleak frozen waste ({@code data/dreadpatch/dimension}), with
 * rare tall spires ({@link SpireStructure}). Its time and weather are the overworld's (every other
 * level's data is the overworld's, read through). Reached through a Dread Gate, arriving near a
 * spire's foot; left through a Dread Gate keyed there, back to where you came in. Dread phantoms
 * come down out of its sky now and then, never for want of sleep.
 */
public final class Dreadlands {
    public static final ResourceKey<Level> LEVEL = ResourceKey.create(Registries.DIMENSION, DreadPatch.id("dreadlands"));
    public static final TagKey<Structure> SPIRES = TagKey.create(Registries.STRUCTURE, DreadPatch.id("spires"));
    /** How far (in chunks) to look for a spire to arrive by. */
    static final int SPIRE_SEARCH = 100;
    /** Arrival: this far from a spire's middle and a little more, near its foot, never on it. */
    static final double ARRIVAL_DISTANCE = 28.0;
    static final double ARRIVAL_SPREAD = 8.0;
    /**
     * Its dread phantoms (the owner: 1-2 at a time, at a low rate, not tied to sleep): every 30 s, one
     * player in 8 under open sky draws 1-2, about one flight every 4 minutes each; never while 4 wild
     * ones are near already.
     */
    static final int PHANTOM_INTERVAL = 600;
    static final int PHANTOM_ONE_IN = 8;
    static final int PHANTOM_CAP = 4;

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
     * Where someone coming in near {@code near} arrives: by the nearest spire's foot (generating the
     * ground there), on dry ground or ice, facing it; with no spire about, at {@code near} itself.
     * Null if there's no Dreadlands.
     */
    @Nullable
    public static Arrival arrival(MinecraftServer server, BlockPos near, RandomSource random) {
        ServerLevel lands = server.getLevel(LEVEL);
        if (lands == null) {
            return null;
        }
        BlockPos spire = lands.findNearestMapStructure(SPIRES, near, SPIRE_SEARCH, false);
        double cx = spire != null ? (spire.getX() & ~15) + 8.5 : near.getX() + 0.5;
        double cz = spire != null ? (spire.getZ() & ~15) + 8.5 : near.getZ() + 0.5;
        double distance = spire != null ? ARRIVAL_DISTANCE + random.nextDouble() * ARRIVAL_SPREAD : 0.0;
        double start = random.nextDouble() * Math.PI * 2.0;
        Arrival wet = null;
        for (int i = 0; i < 8; i++) {
            double angle = start + i * Math.PI / 4.0;
            int x = Mth.floor(cx + Math.cos(angle) * distance);
            int z = Mth.floor(cz + Math.sin(angle) * distance);
            lands.getChunk(x >> 4, z >> 4);
            int y = lands.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
            float yaw = distance > 0.0 ? (float) (Mth.atan2(-(cx - (x + 0.5)), cz - (z + 0.5)) * Mth.RAD_TO_DEG) : random.nextFloat() * 360.0F;
            Arrival arrival = new Arrival(new Vec3(x + 0.5, y, z + 0.5), yaw);
            if (lands.getFluidState(new BlockPos(x, y - 1, z)).isEmpty()) {
                return arrival;
            }
            if (wet == null) {
                wet = arrival;
            }
        }
        return wet;
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

    /** Now and then, 1-2 dread phantoms over a player under the open sky. */
    private static void onLevelTick(LevelTickEvent.Post event) {
        if (!(event.getLevel() instanceof ServerLevel level) || !is(level) || level.getGameTime() % PHANTOM_INTERVAL != 0
                || level.getDifficulty() == Difficulty.PEACEFUL || !level.getGameRules().getBoolean(GameRules.RULE_DOMOBSPAWNING)) {
            return;
        }
        RandomSource random = level.getRandom();
        for (ServerPlayer player : level.players()) {
            if (player.isSpectator() || random.nextInt(PHANTOM_ONE_IN) != 0 || !level.canSeeSky(player.blockPosition())) {
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
