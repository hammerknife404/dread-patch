package io.github.hammerknife404.dreadpatch.client;

import io.github.hammerknife404.dreadpatch.registry.ModRegistries;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.client.sounds.MusicManager;
import net.minecraft.sounds.Music;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.SelectMusicEvent;

/**
 * Her boss theme (0.72.0), its client side ({@code world/BossMusic} sends the start).
 * <ul>
 * <li>Starting, it stops whatever music is playing, unless it's her theme already: it never restarts or
 * plays twice at once.</li>
 * <li>While it plays, {@link SelectMusicEvent} keeps it the situational music, so the Dreadlands' (which
 * replaces what's playing) or any other doesn't cut it off. Once it's over, the usual music comes back,
 * after the usual wait.</li>
 * </ul>
 */
public final class BossMusicClient {
    /** Its delays matter only on the tick it ends: then the usual music's wait is no longer than the Dreadlands'. */
    static final Music MUSIC = new Music(ModRegistries.DREAD_QUEEN_MUSIC, 12000, 24000, true);

    private BossMusicClient() {
    }

    public static void register(IEventBus gameBus) {
        gameBus.addListener(BossMusicClient::onSelectMusic);
    }

    /** Her theme starts, unless it's playing. */
    public static void start() {
        MusicManager manager = Minecraft.getInstance().getMusicManager();
        if (manager.isPlayingMusic(MUSIC)) {
            return;
        }
        manager.stopPlaying();
        manager.startPlaying(MUSIC);
    }

    private static void onSelectMusic(SelectMusicEvent event) {
        SoundInstance playing = event.getPlayingMusic();
        if (playing != null && playing.getLocation().equals(MUSIC.getEvent().value().getLocation())
                && Minecraft.getInstance().getSoundManager().isActive(playing)) {
            event.overrideMusic(MUSIC);
        }
    }
}
