package io.github.hammerknife404.dreadpatch.entity;

import com.iafenvoy.iceandfire.entity.DreadScuttlerEntity;
import io.github.hammerknife404.dreadpatch.DreadPatch;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.ai.navigation.WallClimberNavigation;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.util.ObfuscationReflectionHelper;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.tick.EntityTickEvent;

import java.lang.reflect.Field;

/**
 * Dread scuttlers climb walls like spiders (0.41.0, the owner's ask; every one, Ice and Fire's own and
 * a court's). Ice and Fire's {@code DreadScuttlerEntity} is its class, so it's done from outside, as
 * vanilla's spider does it: a {@link WallClimberNavigation} (it heads straight for a target it has no
 * ground path to, up whatever's in the way) in place of its ground navigation, set once as it joins a
 * level, and each tick it's pressed against a wall it rises at a spider's 0.2 blocks a tick (vanilla's
 * {@code LivingEntity.travel} does that for a climbing mob), taking no fall damage meanwhile.
 */
public final class ScuttlerClimbing {
    /** How fast it climbs: a spider's (vanilla sets a climbing mob's rise to 0.2 a tick). */
    static final double CLIMB_SPEED = 0.2;
    private static final Field NAVIGATION = ObfuscationReflectionHelper.findField(Mob.class, "navigation");

    private ScuttlerClimbing() {
    }

    public static void register(IEventBus gameBus) {
        gameBus.addListener(ScuttlerClimbing::onJoin);
        gameBus.addListener(ScuttlerClimbing::onTick);
    }

    private static void onJoin(EntityJoinLevelEvent event) {
        if (event.getEntity() instanceof DreadScuttlerEntity scuttler && !(scuttler.getNavigation() instanceof WallClimberNavigation)) {
            PathNavigation climber = new WallClimberNavigation(scuttler, event.getLevel());
            try {
                NAVIGATION.set(scuttler, climber);
            } catch (IllegalAccessException e) {
                DreadPatch.LOGGER.warn("Couldn't give a dread scuttler a spider's navigation", e);
            }
        }
    }

    private static void onTick(EntityTickEvent.Post event) {
        if (event.getEntity() instanceof DreadScuttlerEntity scuttler && !scuttler.level().isClientSide() && scuttler.horizontalCollision
                && !scuttler.isPassenger() && !scuttler.isNoAi()) {
            Vec3 motion = scuttler.getDeltaMovement();
            scuttler.setDeltaMovement(motion.x, Math.max(motion.y, CLIMB_SPEED), motion.z);
            scuttler.resetFallDistance();
        }
    }
}
