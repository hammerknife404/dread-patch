package io.github.hammerknife404.dreadpatch.entity;

import com.iafenvoy.iceandfire.entity.DreadBeastEntity;
import io.github.hammerknife404.dreadpatch.magic.BeastVoice;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.PlayLevelSoundEvent;

import java.util.List;
import java.util.Set;

/**
 * A dread beast's voice by its size (0.85.0, {@link BeastVoice}). Ice and Fire's beast sounds a wolf's growl, hurt and
 * death through {@code Mob.playSound}, which plays at its position with no entity named, so a wolf's sound played
 * exactly where a beast stands is the beast's: pitched and made louder by its size.
 */
public final class BeastVoices {
    private static final Set<SoundEvent> VOICE = Set.of(SoundEvents.WOLF_GROWL, SoundEvents.WOLF_HURT, SoundEvents.WOLF_DEATH,
            SoundEvents.WOLF_AMBIENT, SoundEvents.WOLF_STEP);

    private BeastVoices() {
    }

    public static void register(IEventBus gameBus) {
        gameBus.addListener(BeastVoices::onSound);
    }

    private static void onSound(PlayLevelSoundEvent.AtPosition event) {
        if (event.getLevel().isClientSide() || event.getSound() == null || !VOICE.contains(event.getSound().value())) {
            return;
        }
        Vec3 at = event.getPosition();
        List<DreadBeastEntity> beasts = event.getLevel().getEntitiesOfClass(DreadBeastEntity.class, new AABB(at, at).inflate(0.01),
                beast -> beast.position().distanceToSqr(at) < 1e-6);
        if (beasts.isEmpty()) {
            return;
        }
        float size = beasts.getFirst().getSize();
        event.setNewPitch(event.getNewPitch() * BeastVoice.pitch(size));
        event.setNewVolume(event.getNewVolume() * BeastVoice.volume(size));
    }
}
