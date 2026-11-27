package io.github.hammerknife404.dreadpatch.entity;

import com.iafenvoy.iceandfire.entity.DreadLichEntity;
import com.iafenvoy.iceandfire.entity.DreadMobEntity;
import io.github.hammerknife404.dreadpatch.DreadPatchConfig;
import net.minecraft.world.entity.ai.goal.BreakDoorGoal;
import net.minecraft.world.entity.ai.navigation.GroundPathNavigation;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.tick.EntityTickEvent;

/**
 * The Dread break wooden doors as zombies do (0.96.0, the owner: "Dread mobs can break wooden doors like vanilla zombies on
 * hard difficulty (by default and a toggle for it at all and for other difficulties)"; the server config's
 * {@code dread_break_doors} and {@code dread_break_doors_from}). Ice and Fire's walking Dread (not liches, which keep
 * their distance) get vanilla's {@link BreakDoorGoal}, at a zombie's priority, its difficulty test the config's (read
 * each time); vanilla's goal also asks for mob griefing. As a zombie's, their pathfinding goes through doors only while
 * they may break them (checked once a second, so a change applies at once): otherwise they'd stand at a door they can't
 * open.
 */
public final class DreadDoors {
    private DreadDoors() {
    }

    public static void register(IEventBus gameBus) {
        gameBus.addListener(DreadDoors::onJoin);
        gameBus.addListener(DreadDoors::onTick);
    }

    static boolean breaker(Object entity) {
        return entity instanceof DreadMobEntity mob && !(mob instanceof DreadLichEntity) && mob.getNavigation() instanceof GroundPathNavigation;
    }

    /** Goals aren't saved: given on every join. */
    private static void onJoin(EntityJoinLevelEvent event) {
        if (!event.getLevel().isClientSide() && breaker(event.getEntity())) {
            DreadMobEntity mob = (DreadMobEntity) event.getEntity();
            mob.goalSelector.addGoal(1, new BreakDoorGoal(mob, DreadPatchConfig::dreadBreakDoors));
            update(mob);
        }
    }

    private static void onTick(EntityTickEvent.Post event) {
        if (event.getEntity().tickCount % 20 == 0 && !event.getEntity().level().isClientSide() && breaker(event.getEntity())) {
            update((DreadMobEntity) event.getEntity());
        }
    }

    private static void update(DreadMobEntity mob) {
        ((GroundPathNavigation) mob.getNavigation()).setCanOpenDoors(DreadPatchConfig.dreadBreakDoors(mob.level().getDifficulty()));
    }
}
