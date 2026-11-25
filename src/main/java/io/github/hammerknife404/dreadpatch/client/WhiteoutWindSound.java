package io.github.hammerknife404.dreadpatch.client;

import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;

/**
 * The wind inside Whiteout Dominion: vanilla's soul sand valley loop, low, as loud as the storm is round the camera;
 * and over the Sastrugi Wastes, softer (0.69.0).
 */
final class WhiteoutWindSound extends AbstractTickableSoundInstance {
    WhiteoutWindSound() {
        super(SoundEvents.AMBIENT_SOUL_SAND_VALLEY_LOOP.value(), SoundSource.WEATHER, RandomSource.create());
        this.looping = true;
        this.delay = 0;
        this.relative = true;
        this.attenuation = SoundInstance.Attenuation.NONE;
        this.pitch = 0.8F;
        this.volume = 0.01F;
    }

    @Override
    public void tick() {
        float strength = WhiteoutClient.windVolume();
        if (strength <= 0.0F) {
            this.stop();
            return;
        }
        this.volume = Math.max(0.01F, strength * 0.9F);
    }

    @Override
    public boolean canStartSilent() {
        return true;
    }
}
