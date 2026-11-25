package io.github.hammerknife404.dreadpatch.world;

import io.github.hammerknife404.dreadpatch.DreadPatch;
import io.github.hammerknife404.dreadpatch.client.DreadlandsMusicClient;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.entity.player.AdvancementEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;

/**
 * The Dreadlands' music (0.60.0), its server side. The track is the biome's music, and the client keeps
 * any other out ({@link DreadlandsMusicClient}); this only starts it at once the first time a player
 * arrives (the advancement {@code dreadpatch:dreadlands/root}, earned once), instead of after the biome
 * music's usual wait. The mod's first network payload.
 */
public final class DreadlandsMusic {
    public static final ResourceLocation ARRIVAL = DreadPatch.id("dreadlands/root");

    /** To a player's client: start the Dreadlands' music now. */
    public record FirstArrival() implements CustomPacketPayload {
        public static final FirstArrival INSTANCE = new FirstArrival();
        public static final Type<FirstArrival> TYPE = new Type<>(DreadPatch.id("first_arrival"));
        public static final StreamCodec<ByteBuf, FirstArrival> CODEC = StreamCodec.unit(INSTANCE);

        @Override
        public Type<FirstArrival> type() {
            return TYPE;
        }
    }

    private DreadlandsMusic() {
    }

    public static void register(IEventBus modBus, IEventBus gameBus) {
        modBus.addListener(DreadlandsMusic::onRegisterPayloads);
        gameBus.addListener(DreadlandsMusic::onAdvancement);
    }

    private static void onRegisterPayloads(RegisterPayloadHandlersEvent event) {
        // The handler only ever runs on a client (play to client), so a dedicated server never loads the client class.
        event.registrar("1").playToClient(FirstArrival.TYPE, FirstArrival.CODEC, (payload, context) -> DreadlandsMusicClient.startNow());
    }

    private static void onAdvancement(AdvancementEvent.AdvancementEarnEvent event) {
        if (event.getAdvancement().id().equals(ARRIVAL) && event.getEntity() instanceof ServerPlayer player) {
            PacketDistributor.sendToPlayer(player, FirstArrival.INSTANCE);
        }
    }
}
