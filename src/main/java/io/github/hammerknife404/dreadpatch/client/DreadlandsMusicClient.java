package io.github.hammerknife404.dreadpatch.client;

import io.github.hammerknife404.dreadpatch.registry.ModRegistries;
import io.github.hammerknife404.dreadpatch.world.Dreadlands;
import net.minecraft.client.Minecraft;
import net.minecraft.client.sounds.MusicManager;
import net.minecraft.sounds.Music;
import net.minecraft.sounds.Musics;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.SelectMusicEvent;

/**
 * The Dreadlands' music (0.60.0), the owner's "Entombed In Rimefrost": the only music played there
 * ("no other normal music can"), and only there.
 * <ul>
 * <li>It's the biome's music (dreadlands.json: 10 to 20 minutes between plays, as vanilla's, taking over
 * from whatever was playing on arrival).</li>
 * <li>Vanilla picks its creative tracks over a biome's in every dimension but the Nether, and its game
 * tracks where a biome has none: since 0.84.4 (the owner: "Stop the creative mode musive from playing in the dread
 * lands") {@link SelectMusicEvent} picks ours in their place, so neither is ever chosen there; before, either was
 * swapped for ours once it had started, which the tick still does should anything else start one.</li>
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
        gameBus.addListener(DreadlandsMusicClient::onSelectMusic);
    }

    /** In the Dreadlands, vanilla's creative or game music is never the pick: ours is. */
    private static void onSelectMusic(SelectMusicEvent event) {
        Minecraft minecraft = Minecraft.getInstance();
        Music music = event.getMusic();
        if (music != null && minecraft.level != null && Dreadlands.is(minecraft.level) && (isVanilla(music, Musics.CREATIVE) || isVanilla(music, Musics.GAME))) {
            event.setMusic(MUSIC);
        }
    }

    private static boolean isVanilla(Music music, Music vanilla) {
        return music.getEvent().value().getLocation().equals(vanilla.getEvent().value().getLocation());
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
