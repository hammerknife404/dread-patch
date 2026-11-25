package io.github.hammerknife404.dreadpatch.client;

import com.mojang.blaze3d.audio.Channel;
import io.github.hammerknife404.dreadpatch.DreadPatch;
import io.github.hammerknife404.dreadpatch.magic.SoundRules;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.client.sounds.ChannelAccess;
import net.minecraft.client.sounds.SoundEngine;
import net.minecraft.client.sounds.SoundManager;
import net.minecraft.sounds.SoundSource;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.util.ObfuscationReflectionHelper;
import net.neoforged.neoforge.client.event.sound.PlaySoundSourceEvent;
import net.neoforged.neoforge.client.event.sound.PlayStreamingSourceEvent;
import net.neoforged.neoforge.client.event.sound.SoundEvent;
import org.lwjgl.openal.AL10;
import org.lwjgl.openal.ALC10;
import org.lwjgl.openal.EXTEfx;
import org.slf4j.Logger;
import com.mojang.logging.LogUtils;

import java.lang.reflect.Field;
import java.util.Map;

/**
 * OpenAL low-pass filters on sounds as they start (0.64.0, the owner):
 * <ul>
 * <li>"clip the top end a tiny bit ... on all sounds": every sound of ours (the music, the disc, the ambience,
 * the dread phantoms' layer) loses a little of its highs ({@link SoundRules#OUR_HIGHS});</li>
 * <li>"Dread weather severity should muffle sounds around the player faster the further the source is away":
 * in the Dreadlands' snow and storms, a whiteout, a Whiteout Dominion or the defeat storm, every sound placed in
 * the world is quieter and duller the further off it is, the more so the worse the weather ({@link SoundRules#muffle});
 * and (0.69.0) music, jukeboxes and note blocks too, as though at least {@link SoundRules#MUSIC_AT} off.</li>
 * </ul>
 * Through OpenAL's EFX (OpenAL Soft, which Minecraft ships, has it); with no EFX, nothing. A source is reused,
 * so each start sets its filter, or clears it. Every {@link #RECHECK} ticks (0.69.0; until then only at its start)
 * each sound still playing is set again, so a song or a long sound follows the weather as it comes and goes.
 */
public final class SoundFilters {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static Field sourceField;
    private static long context;
    private static int filter;
    private static boolean efx;
    /** How often playing sounds are set again: ticks. */
    private static final int RECHECK = 10;
    private static Field channelsField;
    private static int ticks;
    /** Off if reaching the sound engine's playing sounds failed: then each is set only at its start, as before. */
    private static boolean following = true;

    private SoundFilters() {
    }

    public static void register(IEventBus gameBus) {
        gameBus.addListener(PlaySoundSourceEvent.class, SoundFilters::onStart);
        gameBus.addListener(PlayStreamingSourceEvent.class, SoundFilters::onStart);
        gameBus.addListener(ClientTickEvent.Post.class, SoundFilters::onTick);
    }

    /** How bad the Dread's weather is round the camera now: 0 none to 1 a whiteout. */
    static float severity() {
        return Math.max(Math.max(DreadlandsClient.severity(), WhiteoutClient.windStrength()), DefeatStormClient.severity());
    }

    private static void onStart(SoundEvent.SoundSourceEvent event) {
        apply(event.getSound(), event.getChannel(), severity(), Minecraft.getInstance().gameRenderer.getMainCamera().getPosition());
    }

    /** Every playing sound set again for the weather now: on the sound thread, as each start is. */
    private static void onTick(ClientTickEvent.Post event) {
        if (++ticks % RECHECK != 0 || context == -1L || !following) {
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        try {
            if (channelsField == null) {
                channelsField = ObfuscationReflectionHelper.findField(SoundEngine.class, "instanceToChannel");
            }
            SoundEngine engine = ObfuscationReflectionHelper.getPrivateValue(SoundManager.class, minecraft.getSoundManager(), "soundEngine");
            @SuppressWarnings("unchecked")
            Map<SoundInstance, ChannelAccess.ChannelHandle> channels = (Map<SoundInstance, ChannelAccess.ChannelHandle>) channelsField.get(engine);
            float severity = severity();
            Vec3 ear = minecraft.gameRenderer.getMainCamera().getPosition();
            for (Map.Entry<SoundInstance, ChannelAccess.ChannelHandle> entry : channels.entrySet()) {
                SoundInstance sound = entry.getKey();
                if (!entry.getValue().isStopped()) {
                    entry.getValue().execute(channel -> apply(sound, channel, severity, ear));
                }
            }
        } catch (Exception | LinkageError e) {
            LOGGER.warn("Dread Patch's sound filters won't follow the weather: {}", e.toString());
            following = false;
        }
    }

    /** Music, a jukebox's record or a note block (vanilla's records source has both). */
    private static boolean music(SoundInstance sound) {
        return sound.getSource() == SoundSource.MUSIC || sound.getSource() == SoundSource.RECORDS;
    }

    private static void apply(SoundInstance sound, Channel channel, float severity, Vec3 ear) {
        try {
            if (!ready()) {
                return;
            }
            float gain = 1.0F;
            float highs = DreadPatch.MODID.equals(sound.getLocation().getNamespace()) ? SoundRules.OUR_HIGHS : 1.0F;
            boolean placed = !sound.isRelative() && sound.getAttenuation() == SoundInstance.Attenuation.LINEAR;
            if (severity > 0.0F && (placed || music(sound))) {
                double distance = placed ? ear.distanceTo(new Vec3(sound.getX(), sound.getY(), sound.getZ())) : 0.0;
                float muffle = SoundRules.muffle(severity, SoundRules.heardAt(music(sound), distance));
                gain *= muffle;
                highs *= muffle;
            }
            int source = sourceField.getInt(channel);
            if (gain >= 0.999F && highs >= 0.999F) {
                AL10.alSourcei(source, EXTEfx.AL_DIRECT_FILTER, EXTEfx.AL_FILTER_NULL);
                return;
            }
            EXTEfx.alFilterf(filter, EXTEfx.AL_LOWPASS_GAIN, gain);
            EXTEfx.alFilterf(filter, EXTEfx.AL_LOWPASS_GAINHF, highs);
            AL10.alSourcei(source, EXTEfx.AL_DIRECT_FILTER, filter);
        } catch (Exception | LinkageError e) {
            LOGGER.warn("Dread Patch's sound filters are off: {}", e.toString());
            efx = false;
            sourceField = null;
            context = -1L;
        }
    }

    /** EFX and our filter, made afresh for each OpenAL context (a sound reload makes a new one). */
    private static boolean ready() throws ReflectiveOperationException {
        if (context == -1L) {
            return false;
        }
        long current = ALC10.alcGetCurrentContext();
        if (current == 0L) {
            return false;
        }
        if (current != context) {
            context = current;
            efx = ALC10.alcIsExtensionPresent(ALC10.alcGetContextsDevice(current), "ALC_EXT_EFX");
            if (efx) {
                filter = EXTEfx.alGenFilters();
                EXTEfx.alFilteri(filter, EXTEfx.AL_FILTER_TYPE, EXTEfx.AL_FILTER_LOWPASS);
            }
            if (sourceField == null) {
                sourceField = ObfuscationReflectionHelper.findField(Channel.class, "source");
            }
        }
        return efx;
    }
}
