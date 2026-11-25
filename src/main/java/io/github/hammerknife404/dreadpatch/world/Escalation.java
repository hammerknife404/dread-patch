package io.github.hammerknife404.dreadpatch.world;

import com.iafenvoy.iceandfire.entity.DreadMobEntity;
import io.github.hammerknife404.dreadpatch.DreadPatch;
import io.github.hammerknife404.dreadpatch.DreadPatchConfig;
import io.github.hammerknife404.dreadpatch.client.DefeatStormClient;
import io.github.hammerknife404.dreadpatch.entity.DreadPatrols;
import io.github.hammerknife404.dreadpatch.entity.DreadQueen;
import io.github.hammerknife404.dreadpatch.item.DreadRegalia;
import io.github.hammerknife404.dreadpatch.magic.DreadlandsSpawning;
import io.github.hammerknife404.dreadpatch.magic.EscalationRules;
import io.github.hammerknife404.dreadpatch.magic.RegaliaBonuses;
import io.github.hammerknife404.dreadpatch.registry.ModRegistries;
import io.netty.buffer.ByteBuf;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.StructureTags;
import net.minecraft.tags.TagKey;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Difficulty;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.Tags;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import org.jetbrains.annotations.Nullable;

import java.util.Random;
import java.util.function.Predicate;

/**
 * What the Dread Queen's defeat stirs in the overworld (0.62.0, the owner's feature dump,
 * "Post-Defeat Overworld Escalation"; {@link EscalationRules}). A defeat is her death by anything
 * but {@code /kill}, wherever she falls.
 * <ul>
 * <li><b>Round the Mausoleums:</b> after the world's first defeat ({@link DreadlandsData#queenFallen},
 * for good), the Dread rise round Ice and Fire's Mausoleums as they do in the Dreadlands on a clear
 * day, and patrols cross there: within 128 of one, in a cold or snowy biome, on snow, ice or
 * dreadstone, never in a village.</li>
 * <li><b>The defeat storm:</b> each defeat (one per {@code defeat_storm_hours}, real time) brings 30-60 s
 * over the cold lands. Dry: no snowfall, no damage, no raising of the dead, nothing frozen; to
 * everyone under the sky in a cold biome (or anywhere, by the server's setting), a mild Rimefrost
 * (I), and on their client ({@link DefeatStormClient}) an ash-grey fog, rime blown along the ground,
 * the Dreadlands' wind and its ice cracking. Its end is game time in the world's saved data, so a
 * night slept through doesn't end it early, nor does vanilla's weather.</li>
 * </ul>
 */
public final class Escalation {
    public static final TagKey<Structure> MAUSOLEUMS = TagKey.create(Registries.STRUCTURE, DreadPatch.id("mausoleums"));
    private static final TagKey<Block> DREADSTONE = TagKey.create(Registries.BLOCK, DreadPatch.id("dread_gate_frame"));
    /** How far (in chunks) to look for a Mausoleum a player is near. */
    static final int MAUSOLEUM_CHUNKS = 12;
    static final int RIMEFROST_TICKS = 60;

    /** To a client: the defeat storm lasts until this game time (0, or past: none). */
    public record DefeatStorm(long until) implements CustomPacketPayload {
        public static final Type<DefeatStorm> TYPE = new Type<>(DreadPatch.id("defeat_storm"));
        public static final StreamCodec<ByteBuf, DefeatStorm> CODEC = ByteBufCodecs.VAR_LONG.map(DefeatStorm::new, DefeatStorm::until);

        @Override
        public Type<DefeatStorm> type() {
            return TYPE;
        }
    }

    private Escalation() {
    }

    public static void register(IEventBus modBus, IEventBus gameBus) {
        modBus.addListener(Escalation::onRegisterPayloads);
        gameBus.addListener(Escalation::onDeath);
        gameBus.addListener(Escalation::onLevelTick);
        gameBus.addListener((PlayerEvent.PlayerLoggedInEvent event) -> tell(event.getEntity()));
        gameBus.addListener((PlayerEvent.PlayerChangedDimensionEvent event) -> tell(event.getEntity()));
    }

    private static void onRegisterPayloads(RegisterPayloadHandlersEvent event) {
        // Only ever handled on a client, so a dedicated server never loads the client class.
        event.registrar("1").playToClient(DefeatStorm.TYPE, DefeatStorm.CODEC, (payload, context) -> DefeatStormClient.set(payload.until()));
    }

    private static void tell(net.minecraft.world.entity.player.Player player) {
        if (player instanceof ServerPlayer server) {
            PacketDistributor.sendToPlayer(server, new DefeatStorm(DreadlandsData.get(server.server).defeatStormUntil()));
        }
    }

    private static void onDeath(LivingDeathEvent event) {
        if (event.getEntity() instanceof DreadQueen queen && queen.level() instanceof ServerLevel level
                && EscalationRules.defeat(event.getSource().is(DamageTypes.GENERIC_KILL), event.getSource().is(DamageTypes.FELL_OUT_OF_WORLD))) {
            defeated(level.getServer());
        }
    }

    /** A Dread Queen defeated: the Mausoleums stir, if they hadn't, and a storm comes, if it's due. */
    public static void defeated(MinecraftServer server) {
        DreadlandsData data = DreadlandsData.get(server);
        if (!data.queenFallen()) {
            data.setQueenFallen(true);
            server.getPlayerList().broadcastSystemMessage(Component.translatable("message.dreadpatch.queen_fallen").withStyle(ChatFormatting.DARK_AQUA), false);
        }
        long now = System.currentTimeMillis();
        if (EscalationRules.stormDue(now, data.lastDefeatStorm(), DreadPatchConfig.defeatStormCooldownMillis())) {
            startStorm(server, EscalationRules.stormTicks(server.overworld().getRandom()::nextInt), now);
        }
    }

    /** A defeat storm for {@code ticks}, from now; every client is told. */
    public static void startStorm(MinecraftServer server, int ticks, long now) {
        DreadlandsData data = DreadlandsData.get(server);
        data.setDefeatStorm(server.overworld().getGameTime() + ticks, now);
        PacketDistributor.sendToAllPlayers(new DefeatStorm(data.defeatStormUntil()));
    }

    /** Forgets it all: no defeat yet, no storm, none due to wait for. */
    public static void reset(MinecraftServer server) {
        DreadlandsData data = DreadlandsData.get(server);
        data.setQueenFallen(false);
        data.setDefeatStorm(0L, 0L);
        PacketDistributor.sendToAllPlayers(new DefeatStorm(0L));
    }

    /** Ticks of the defeat storm left (0: none). */
    public static long stormLeft(MinecraftServer server) {
        return Math.max(0L, DreadlandsData.get(server).defeatStormUntil() - server.overworld().getGameTime());
    }

    public static boolean cold(Level level, BlockPos pos) {
        Holder<Biome> biome = level.getBiome(pos);
        return biome.is(Tags.Biomes.IS_COLD) || biome.is(Tags.Biomes.IS_SNOWY);
    }

    private static void onLevelTick(LevelTickEvent.Post event) {
        if (!(event.getLevel() instanceof ServerLevel level) || level.dimension() != Level.OVERWORLD) {
            return;
        }
        long time = level.getGameTime();
        DreadlandsData data = DreadlandsData.get(level.getServer());
        if (time % 20 == 0 && time < data.defeatStormUntil()) {
            chill(level);
        }
        if (data.queenFallen()) {
            stir(level, time);
        }
    }

    /** The storm's mild Rimefrost on everyone it reaches; her full regalia eases it away, as the Dreadlands' snow. */
    private static void chill(ServerLevel level) {
        boolean coldOnly = DreadPatchConfig.defeatStormColdOnly();
        for (ServerPlayer player : level.players()) {
            if (player.isSpectator() || !level.canSeeSky(BlockPos.containing(player.getEyePosition()))
                    || coldOnly && !cold(level, player.blockPosition())
                    || DreadPatchConfig.regaliaEasesWeather() && RegaliaBonuses.fullSet(DreadRegalia.pieces(player))) {
                continue;
            }
            player.addEffect(new MobEffectInstance(ModRegistries.RIMEFROST, RIMEFROST_TICKS, 0, true, true));
        }
    }

    /** The Dread rising, and patrols crossing, round the Mausoleums near players. */
    private static void stir(ServerLevel level, long time) {
        int oneIn = DreadPatchConfig.mausoleumDreadOneIn();
        boolean dread = time % DreadlandsSpawning.SPAWN_INTERVAL == 0;
        boolean patrols = time % DreadlandsSpawning.PATROL_INTERVAL == 0;
        if (oneIn <= 0 || !dread && !patrols || level.getDifficulty() == Difficulty.PEACEFUL
                || !level.getGameRules().getBoolean(GameRules.RULE_DOMOBSPAWNING)) {
            return;
        }
        RandomSource random = level.getRandom();
        for (ServerPlayer player : level.players()) {
            if (player.isSpectator()) {
                continue;
            }
            Random roll = new Random(random.nextLong());
            boolean rise = dread && DreadlandsSpawning.comesUp(oneIn, roll);
            boolean patrol = patrols && DreadlandsSpawning.comesUp(DreadPatchConfig.patrolOneIn(), roll);
            BlockPos mausoleum = rise || patrol ? mausoleumNear(level, player.blockPosition()) : null;
            if (mausoleum == null) {
                continue;
            }
            Predicate<BlockPos> ground = pos -> pos.distToCenterSqr(mausoleum.getX(), pos.getY(), mausoleum.getZ())
                    <= EscalationRules.MAUSOLEUM_REACH * EscalationRules.MAUSOLEUM_REACH
                    && cold(level, pos) && frozen(level, pos) && !level.structureManager().getStructureWithPieceAt(pos, StructureTags.VILLAGE).isValid();
            if (rise) {
                int standing = level.getEntitiesOfClass(DreadMobEntity.class, player.getBoundingBox().inflate(DreadlandsSpawning.CAP_RANGE)).size();
                int count = DreadlandsSpawning.groupSize(standing, DreadPatchConfig.dreadCap(), roll);
                Vec3 at = count > 0 ? DreadlandsSpawns.ground(level, player, DreadlandsSpawning.MIN_DISTANCE, DreadlandsSpawning.MAX_DISTANCE, random, ground) : null;
                if (at != null) {
                    DreadPatrols.raiseLoose(level, at, count, random);
                }
            }
            if (patrol && !DreadPatrols.patrolNear(level, player.position(), DreadlandsSpawns.PATROL_SPACING)) {
                Vec3 at = DreadlandsSpawns.ground(level, player, DreadlandsSpawning.PATROL_MIN_DISTANCE, DreadlandsSpawning.PATROL_MAX_DISTANCE, random, ground);
                if (at != null) {
                    BlockPos toward = level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                            player.blockPosition().offset(random.nextInt(33) - 16, 0, random.nextInt(33) - 16));
                    DreadPatrols.raise(level, at, toward, random);
                }
            }
        }
    }

    /** The middle (near enough) of a Mausoleum the player is near enough to that the Dread could rise within its reach; else null. */
    @Nullable
    static BlockPos mausoleumNear(ServerLevel level, BlockPos player) {
        BlockPos found = level.findNearestMapStructure(MAUSOLEUMS, player, MAUSOLEUM_CHUNKS, false);
        if (found == null) {
            return null;
        }
        BlockPos middle = new BlockPos((found.getX() & ~15) + 8, player.getY(), (found.getZ() & ~15) + 8);
        double near = EscalationRules.MAUSOLEUM_REACH + DreadlandsSpawning.PATROL_MAX_DISTANCE;
        return middle.distSqr(player) <= near * near ? middle : null;
    }

    /** Standing at {@code feet}: in snow, or on snow, ice or dreadstone. */
    static boolean frozen(ServerLevel level, BlockPos feet) {
        if (level.getBlockState(feet).is(Blocks.SNOW)) {
            return true;
        }
        var floor = level.getBlockState(feet.below());
        return floor.is(BlockTags.SNOW) || floor.is(BlockTags.ICE) || floor.is(DREADSTONE);
    }
}
