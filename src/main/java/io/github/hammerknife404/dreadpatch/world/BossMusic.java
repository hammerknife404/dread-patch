package io.github.hammerknife404.dreadpatch.world;

import io.github.hammerknife404.dreadpatch.DreadPatch;
import io.github.hammerknife404.dreadpatch.client.BossMusicClient;
import io.github.hammerknife404.dreadpatch.entity.DreadQueen;
import io.github.hammerknife404.dreadpatch.entity.Syndrigossa;
import io.netty.buffer.ByteBuf;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;

/**
 * Her boss theme (0.72.0), its server side: the owner's "Beneath the Permafrost", played for the players
 * near the Dread Queen or Syndrigossa as she spawns, and only then ("strictly on her spawn with no
 * overlapping or restarting").
 * <ul>
 * <li>Each of them is marked once seen ({@link #PLAYED}, persistent data, so it follows her through a
 * dimension change), and only a fresh spawn (not loaded from disk) starts it: a chunk reloading never
 * does, nor does a queen saved before this.</li>
 * <li>The client starts it only if it isn't playing already (a queen spawns with her dragon), and keeps
 * any other music from cutting it off ({@link BossMusicClient}).</li>
 * </ul>
 */
public final class BossMusic {
    /** Persistent data: her theme has been played for this one. */
    public static final String PLAYED = "dreadpatch:boss_music_played";
    /** Players within this many blocks of her hear it. */
    public static final double RANGE = 96.0;

    /** To a player's client: start her theme now, unless it's playing. */
    public record Start() implements CustomPacketPayload {
        public static final Start INSTANCE = new Start();
        public static final Type<Start> TYPE = new Type<>(DreadPatch.id("boss_music"));
        public static final StreamCodec<ByteBuf, Start> CODEC = StreamCodec.unit(INSTANCE);

        @Override
        public Type<Start> type() {
            return TYPE;
        }
    }

    private BossMusic() {
    }

    public static void register(IEventBus modBus, IEventBus gameBus) {
        modBus.addListener(BossMusic::onRegisterPayloads);
        gameBus.addListener(BossMusic::onJoin);
    }

    private static void onRegisterPayloads(RegisterPayloadHandlersEvent event) {
        // Played only to a client, so a dedicated server never loads the client class.
        event.registrar("1").playToClient(Start.TYPE, Start.CODEC, (payload, context) -> BossMusicClient.start());
    }

    /** Whether this entity's spawn plays her theme. */
    public static boolean isBoss(Entity entity) {
        return entity instanceof DreadQueen || entity instanceof Syndrigossa;
    }

    private static void onJoin(EntityJoinLevelEvent event) {
        Entity entity = event.getEntity();
        if (!(event.getLevel() instanceof ServerLevel level) || !isBoss(entity)) {
            return;
        }
        CompoundTag data = entity.getPersistentData();
        if (data.getBoolean(PLAYED)) {
            return;
        }
        data.putBoolean(PLAYED, true);
        if (event.loadedFromDisk()) {
            return;
        }
        for (ServerPlayer player : level.players()) {
            if (player.distanceToSqr(entity) <= RANGE * RANGE) {
                PacketDistributor.sendToPlayer(player, Start.INSTANCE);
            }
        }
    }
}
