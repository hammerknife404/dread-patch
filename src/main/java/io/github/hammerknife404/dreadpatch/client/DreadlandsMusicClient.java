package io.github.hammerknife404.dreadpatch.client;

import io.github.hammerknife404.dreadpatch.registry.ModRegistries;
import io.github.hammerknife404.dreadpatch.world.Dreadlands;
import net.minecraft.client.Minecraft;
import net.minecraft.client.sounds.MusicManager;
import net.minecraft.sounds.Music;
import net.minecraft.sounds.Musics;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.ClientTickEvent;

/**
 * The Dreadlands' music (0.60.0), the owner's "Entombed In Rimefrost": the only music played there
 * ("no other normal music can"), and only there.
 * <ul>
 * <li>It's the biome's music (dreadlands.json: 10 to 20 minutes between plays, as vanilla's, taking over
 * from whatever was playing on arrival).</li>
 * <li>Vanilla picks its creative tracks over a biome's in every dimension but the Nether, and its game
 * tracks where a biome has none: either is swapped for ours as it starts, so the gaps stay the same.</li>
 * <li>Leaving, it stops.</li>
 * <li>The first arrival starts it at once ({@code world/DreadlandsMusic}).</li>
 * </ul>
 */
public final class DreadlandsMusicClient {
    /** As the biome's music. */
    static final Music MUSIC = new Music(ModRegistries.DREADLANDS_MUSIC, 12000, 24000, true);

    private DreadlandsMusicClient() {
    }

    public static void register(IEventBus gameBus) {
        gameBus.addListener(DreadlandsMusicClient::onClientTick);
    }

    /** Whatever is playing stops, and the Dreadlands' music starts. */
    public static void startNow() {
        MusicManager manager = Minecraft.getInstance().getMusicManager();
        manager.stopPlaying();
        manager.startPlaying(MUSIC);
    }

    private static void onClientTick(ClientTickEvent.Post event) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null || minecraft.player == null) {
            return;
        }
        MusicManager manager = minecraft.getMusicManager();
        if (Dreadlands.is(minecraft.level)) {
            if (manager.isPlayingMusic(Musics.CREATIVE) || manager.isPlayingMusic(Musics.GAME)) {
                startNow();
            }
        } else if (manager.isPlayingMusic(MUSIC)) {
            manager.stopPlaying(MUSIC);
        }
    }
}
