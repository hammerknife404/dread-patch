package io.github.hammerknife404.dreadpatch.entity;

import com.iafenvoy.iceandfire.entity.DreadScuttlerEntity;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.tick.EntityTickEvent;

/**
 * Ice and Fire's dread scuttlers, the Dread's spiders (the owner's ask), spit a weakened Withering
 * Rime-Bolt at what they hunt: {@link #DAMAGE} + {@link #MAX_HEALTH_SHARE} of max health (at most
 * 20) and {@link #RIME_TICKS} of Withering Rime, from {@link #MIN_RANGE} to {@link #MAX_RANGE} blocks
 * (closer, they bite; since 0.75.1 they notice from 24 and open with webs, ScuttlerWebs), every 3-4 s. Every scuttler, hers or Ice and Fire's own; not
 * while rising out of the ground. The class is Ice and Fire's, so it's done from its tick.
 */
public final class ScuttlerBolts {
    public static final float DAMAGE = 3.0F;
    public static final float MAX_HEALTH_SHARE = 0.04F;
    public static final int RIME_TICKS = 40;
    public static final double MIN_RANGE = 3.0;
    public static final double MAX_RANGE = 12.0;
    /** Ticks between bolts: 60 to 80. */
    static final int COOLDOWN = 60;
    static final int COOLDOWN_SPREAD = 21;
    /** When its next bolt is ready (game time), in its persistent data. */
    static final String NEXT_BOLT = "dreadpatch:next_bolt";

    private ScuttlerBolts() {
    }

    public static void register(IEventBus gameBus) {
        gameBus.addListener(ScuttlerBolts::onTick);
    }

    private static void onTick(EntityTickEvent.Post event) {
        if (!(event.getEntity() instanceof DreadScuttlerEntity scuttler) || scuttler.level().isClientSide || !scuttler.isAlive()
                || scuttler.getAnimation() == DreadScuttlerEntity.ANIMATION_SPAWN) {
            return;
        }
        LivingEntity target = scuttler.getTarget();
        if (target == null || !target.isAlive()) {
            return;
        }
        long now = scuttler.level().getGameTime();
        CompoundTag data = scuttler.getPersistentData();
        if (now < data.getLong(NEXT_BOLT)) {
            return;
        }
        double distance = scuttler.distanceTo(target);
        if (distance < MIN_RANGE || distance > MAX_RANGE || !scuttler.hasLineOfSight(target)) {
            return;
        }
        WitheringRimeBolt bolt = new WitheringRimeBolt(scuttler.level(), scuttler, DAMAGE, MAX_HEALTH_SHARE, RIME_TICKS);
        bolt.setPos(scuttler.getX(), scuttler.getEyeY() - 0.2, scuttler.getZ());
        Vec3 aim = target.position().add(0, target.getBbHeight() * 0.6, 0).subtract(bolt.position());
        bolt.shoot(aim.normalize());
        scuttler.level().addFreshEntity(bolt);
        scuttler.swing(InteractionHand.MAIN_HAND);
        scuttler.playSound(SoundEvents.ILLUSIONER_CAST_SPELL, 0.7F, 1.5F);
        data.putLong(NEXT_BOLT, now + COOLDOWN + scuttler.getRandom().nextInt(COOLDOWN_SPREAD));
    }
}
