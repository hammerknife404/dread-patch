package io.github.hammerknife404.dreadpatch.world;

import com.iafenvoy.iceandfire.entity.DreadMobEntity;
import com.iafenvoy.iceandfire.registry.IafEntities;
import io.github.hammerknife404.dreadpatch.DreadPatch;
import io.github.hammerknife404.dreadpatch.DreadPatchConfig;
import io.github.hammerknife404.dreadpatch.client.DefeatStormClient;
import io.github.hammerknife404.dreadpatch.client.DreadWeatherAlert;
import io.github.hammerknife404.dreadpatch.entity.DreadPatrols;
import io.github.hammerknife404.dreadpatch.item.DreadRegalia;
import io.github.hammerknife404.dreadpatch.magic.DreadWeatherRules;
import io.github.hammerknife404.dreadpatch.magic.DreadlandsSpawning;
import io.github.hammerknife404.dreadpatch.magic.RegaliaBonuses;
import io.github.hammerknife404.dreadpatch.magic.StormRules;
import io.github.hammerknife404.dreadpatch.registry.ModRegistries;
import io.netty.buffer.ByteBuf;
import it.unimi.dsi.fastutil.longs.LongArrayList;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Difficulty;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.Tags;
import net.neoforged.neoforge.event.EventHooks;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;

import java.util.List;
import java.util.Random;

/**
 * The Dread's weather in the overworld (0.89.0, {@link DreadWeatherRules}; the config's {@code [dread_weather]}, both
 * switches off by default). With either on, a rain starting in the overworld is a dread storm one time in
 * {@code dread_weather_one_in}, for as long as it rains (saved, {@link DreadlandsData}); the Nether and the End never.
 * <ul>
 * <li>Where it reaches ({@link #reaches}): Rimefrost on those under the sky as in the Dreadlands' snow (I, II in thunder;
 * her full regalia eases it a step), no damage; snow layers laid round them, melted once it's over; and in thunder,
 * Rime Lightning, the Dreadlands' bolt and rimefire ({@link DreadlandsStorms#strike}).</li>
 * <li>The Dread rise: in the cold as in the Dreadlands, and elsewhere one or two of the lesser Dread (thralls, ghouls,
 * scuttlers, beasts).</li>
 * <li>Its clients are told it's on ({@link State}), and draw it as the Dreadlands' storms are drawn (fog, blown snow,
 * the ambience: {@code client/DefeatStormClient}); and as it starts each player it reaches sees one of its warnings
 * ({@link Alert}, {@code client/DreadWeatherAlert}).</li>
 * </ul>
 */
public final class DreadWeather {
    /** Whether the overworld rained the tick before (per server; set on its first tick). */
    private static Boolean wasRaining;

    /** The overworld's dread storm, on or off, and whether it reaches the hot lands. */
    public record State(boolean on, boolean hot) implements CustomPacketPayload {
        public static final Type<State> TYPE = new Type<>(DreadPatch.id("dread_weather"));
        public static final StreamCodec<ByteBuf, State> CODEC = StreamCodec.composite(ByteBufCodecs.BOOL, State::on, ByteBufCodecs.BOOL, State::hot, State::new);

        @Override
        public Type<State> type() {
            return TYPE;
        }
    }

    /** A warning as it starts: which line. */
    public record Alert(int line) implements CustomPacketPayload {
        public static final Type<Alert> TYPE = new Type<>(DreadPatch.id("dread_weather_alert"));
        public static final StreamCodec<ByteBuf, Alert> CODEC = ByteBufCodecs.VAR_INT.map(Alert::new, Alert::line);

        @Override
        public Type<Alert> type() {
            return TYPE;
        }
    }

    private DreadWeather() {
    }

    public static void register(IEventBus modBus, IEventBus gameBus) {
        modBus.addListener(DreadWeather::onRegisterPayloads);
        gameBus.addListener(DreadWeather::onLevelTick);
        gameBus.addListener((PlayerEvent.PlayerLoggedInEvent event) -> tell(event.getEntity()));
        gameBus.addListener((PlayerEvent.PlayerChangedDimensionEvent event) -> tell(event.getEntity()));
        gameBus.addListener((net.neoforged.neoforge.event.server.ServerStoppedEvent event) -> wasRaining = null);
    }

    private static void onRegisterPayloads(RegisterPayloadHandlersEvent event) {
        // Each handler calls into a client class, which only a client ever runs.
        var registrar = event.registrar("1");
        registrar.playToClient(State.TYPE, State.CODEC, (payload, context) -> DefeatStormClient.setDreadWeather(payload.on(), payload.hot()));
        registrar.playToClient(Alert.TYPE, Alert.CODEC, (payload, context) -> DreadWeatherAlert.show(payload.line()));
    }

    private static void tell(Player player) {
        if (player instanceof ServerPlayer server) {
            DreadlandsData data = DreadlandsData.get(server.server);
            PacketDistributor.sendToPlayer(server, new State(data.dreadWeather(), data.dreadWeatherHot()));
        }
    }

    /** Whether it reaches {@code pos} (the switches, its biome, and whether this storm reaches the hot lands). */
    public static boolean reaches(Level level, BlockPos pos, boolean hotReach) {
        Holder<Biome> biome = level.getBiome(pos);
        return DreadWeatherRules.reaches(Escalation.cold(level, pos), biome.is(Tags.Biomes.IS_HOT_OVERWORLD),
                DreadPatchConfig.dreadWeatherCold(), DreadPatchConfig.dreadWeatherAnywhere(), hotReach);
    }

    private static void onLevelTick(LevelTickEvent.Post event) {
        if (!(event.getLevel() instanceof ServerLevel level) || level.dimension() != Level.OVERWORLD) {
            return;
        }
        DreadlandsData data = DreadlandsData.get(level.getServer());
        boolean raining = level.isRaining();
        boolean enabled = DreadWeatherRules.enabled(DreadPatchConfig.dreadWeatherCold(), DreadPatchConfig.dreadWeatherAnywhere());
        if (wasRaining == null) {
            wasRaining = raining;
        }
        if (raining && !wasRaining && enabled && !data.dreadWeather() && level.getRandom().nextInt(DreadPatchConfig.dreadWeatherOneIn()) == 0) {
            start(level, data);
        } else if (data.dreadWeather() && !raining) {
            data.setDreadWeather(false, false);
            PacketDistributor.sendToAllPlayers(new State(false, false));
        }
        wasRaining = raining;
        if (data.dreadWeather()) {
            storm(level, data);
        } else if (!data.dreadSnow().isEmpty()) {
            melt(level, data);
        }
    }

    /** It begins: the clients told, and each player it reaches warned. */
    public static void start(ServerLevel level, DreadlandsData data) {
        boolean hot = level.getRandom().nextInt(DreadWeatherRules.HOT_REACH_ONE_IN) == 0;
        data.setDreadWeather(true, hot);
        PacketDistributor.sendToAllPlayers(new State(true, hot));
        for (ServerPlayer player : level.players()) {
            if (!player.isSpectator() && reaches(level, player.blockPosition(), hot)) {
                PacketDistributor.sendToPlayer(player, new Alert(level.getRandom().nextInt(DreadWeatherRules.ALERT_LINES)));
            }
        }
    }

    /** It ends (a command): the clients told; its snow melts from now. */
    public static void stop(ServerLevel level) {
        DreadlandsData.get(level.getServer()).setDreadWeather(false, false);
        PacketDistributor.sendToAllPlayers(new State(false, false));
    }

    private static void storm(ServerLevel level, DreadlandsData data) {
        long time = level.getGameTime();
        RandomSource random = level.getRandom();
        boolean hot = data.dreadWeatherHot();
        for (ServerPlayer player : level.players()) {
            if (player.isSpectator() || !reaches(level, player.blockPosition(), hot)) {
                continue;
            }
            if (time % 20 == 0) {
                chill(level, player);
                snow(level, player, data, random);
            }
            if (level.isThundering() && random.nextInt(StormRules.STRIKE_ONE_IN) == 0) {
                int x = player.getBlockX() + random.nextInt(2 * StormRules.STRIKE_RANGE + 1) - StormRules.STRIKE_RANGE;
                int z = player.getBlockZ() + random.nextInt(2 * StormRules.STRIKE_RANGE + 1) - StormRules.STRIKE_RANGE;
                if (level.hasChunk(x >> 4, z >> 4)) {
                    DreadlandsStorms.strike(level, level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING, new BlockPos(x, 0, z)));
                }
            }
            if (time % DreadlandsSpawning.SPAWN_INTERVAL == 0) {
                rise(level, player, random);
            }
        }
    }

    /** The Dreadlands' snow's Rimefrost: I, or II in thunder; her full regalia eases it a step. No damage. */
    private static void chill(ServerLevel level, ServerPlayer player) {
        if (!level.canSeeSky(BlockPos.containing(player.getEyePosition()))) {
            return;
        }
        int amplifier = level.isThundering() ? 1 : 0;
        if (DreadPatchConfig.regaliaEasesWeather() && RegaliaBonuses.fullSet(DreadRegalia.pieces(player))) {
            amplifier--;
        }
        if (amplifier >= 0) {
            player.addEffect(new MobEffectInstance(ModRegistries.RIMEFROST, Escalation.RIMEFROST_TICKS, amplifier, true, true));
        }
    }

    /** A few snow layers round them, on open ground that would hold one, remembered to melt. */
    private static void snow(ServerLevel level, ServerPlayer player, DreadlandsData data, RandomSource random) {
        LongArrayList laid = data.dreadSnow();
        int radius = DreadWeatherRules.SNOW_RADIUS;
        for (int i = 0; i < DreadWeatherRules.SNOW_PER_SECOND && laid.size() < DreadWeatherRules.SNOW_MAX; i++) {
            int x = player.getBlockX() + random.nextInt(2 * radius + 1) - radius;
            int z = player.getBlockZ() + random.nextInt(2 * radius + 1) - radius;
            if (!level.hasChunk(x >> 4, z >> 4)) {
                continue;
            }
            BlockPos at = level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING, new BlockPos(x, 0, z));
            BlockState snow = Blocks.SNOW.defaultBlockState();
            if (level.getBlockState(at).isAir() && level.getFluidState(at.below()).isEmpty() && snow.canSurvive(level, at)) {
                level.setBlock(at, snow, 3);
                laid.add(at.asLong());
                data.setDirty();
            }
        }
    }

    /** Once it's over, a few of its snow layers a tick melt (only where they're still its snow, and loaded). */
    private static void melt(ServerLevel level, DreadlandsData data) {
        LongArrayList laid = data.dreadSnow();
        for (int i = 0; i < DreadWeatherRules.MELT_PER_TICK && !laid.isEmpty(); i++) {
            long packed = laid.getLong(laid.size() - 1);
            BlockPos at = BlockPos.of(packed);
            if (!level.hasChunk(at.getX() >> 4, at.getZ() >> 4)) {
                // Not loaded: kept for later, behind the rest.
                laid.removeLong(laid.size() - 1);
                laid.add(0, packed);
                continue;
            }
            laid.removeLong(laid.size() - 1);
            if (level.getBlockState(at).is(Blocks.SNOW)) {
                level.removeBlock(at, false);
            }
        }
        data.setDirty();
    }

    /** The Dread rising: in the cold as in the Dreadlands; elsewhere one or two of the lesser Dread. */
    private static void rise(ServerLevel level, ServerPlayer player, RandomSource random) {
        if (level.getDifficulty() == Difficulty.PEACEFUL || !level.getGameRules().getBoolean(GameRules.RULE_DOMOBSPAWNING)) {
            return;
        }
        Random roll = new Random(random.nextLong());
        DreadlandsSpawning.Weather weather = level.isThundering() ? DreadlandsSpawning.Weather.STORM : DreadlandsSpawning.Weather.SNOW;
        if (!DreadlandsSpawning.comesUp(DreadPatchConfig.dreadSpawnOneIn(weather), roll)) {
            return;
        }
        int standing = level.getEntitiesOfClass(DreadMobEntity.class, player.getBoundingBox().inflate(DreadlandsSpawning.CAP_RANGE)).size();
        boolean cold = Escalation.cold(level, player.blockPosition());
        int count = cold ? DreadlandsSpawning.groupSize(standing, DreadPatchConfig.dreadCap(), roll)
                : Math.min(DreadPatchConfig.dreadCap() - standing,
                DreadWeatherRules.LESSER_MIN + roll.nextInt(DreadWeatherRules.LESSER_MAX - DreadWeatherRules.LESSER_MIN + 1));
        Vec3 at = count > 0 ? DreadlandsSpawns.ground(level, player, DreadlandsSpawning.MIN_DISTANCE, DreadlandsSpawning.MAX_DISTANCE, random) : null;
        if (at == null) {
            return;
        }
        if (cold) {
            DreadPatrols.raiseLoose(level, at, count, random);
            return;
        }
        List<EntityType<? extends Mob>> lesser = List.of(IafEntities.DREAD_THRALL.get(), IafEntities.DREAD_GHOUL.get(),
                IafEntities.DREAD_SCUTTLER.get(), IafEntities.DREAD_BEAST.get());
        for (int i = 0; i < count; i++) {
            Mob mob = lesser.get(roll.nextInt(lesser.size())).create(level);
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
}
