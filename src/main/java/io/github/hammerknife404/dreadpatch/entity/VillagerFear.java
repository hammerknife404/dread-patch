package io.github.hammerknife404.dreadpatch.entity;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.AvoidEntityGoal;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.npc.WanderingTrader;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.tick.EntityTickEvent;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.WeakHashMap;

/**
 * Villagers fear the Dread and take shelter (0.97.2, the owner: "dread mobs should cause villagers to fear and seek
 * shelter"). Vanilla's villager knows its hostiles by a fixed list ({@code VillagerHostilesSensor}, zombies, raiders and
 * the like), which nothing can add to; so the Dread are put in its memory ourselves. Every half second a villager looks
 * for one of the Dread ({@link RisenDread#dread}, the risen and anyone's summons too) it can see within {@link #SEE},
 * or any within {@link #NEAR}; while it has one, before each of its brain's ticks:
 * <ul>
 * <li>its nearest hostile (as a zombie would be): it panics and runs from it;</li>
 * <li>the bell heard (as in a raid): it goes to its home and hides there, and comes out a while after the last.</li>
 * </ul>
 * A wandering trader (goals, not a brain) runs from them as from a zombie.
 */
public final class VillagerFear {
    /** How far a villager sees the Dread coming, and how near it feels them unseen. */
    static final double SEE = 12.0;
    static final double NEAR = 6.0;
    /** How often it looks (ticks). */
    static final int LOOK_EVERY = 10;

    /** What each villager fears now, until its next look. Server thread only. */
    private static final Map<Villager, LivingEntity> FEARED = new WeakHashMap<>();

    private VillagerFear() {
    }

    public static void register(IEventBus gameBus) {
        gameBus.addListener(VillagerFear::onTick);
        gameBus.addListener(VillagerFear::onJoin);
    }

    private static void onTick(EntityTickEvent.Pre event) {
        if (!(event.getEntity() instanceof Villager villager) || villager.level().isClientSide || !villager.isAlive()) {
            return;
        }
        if (villager.tickCount % LOOK_EVERY == 0) {
            LivingEntity dread = nearest(villager);
            if (dread == null) {
                FEARED.remove(villager);
            } else {
                FEARED.put(villager, dread);
            }
        }
        LivingEntity dread = FEARED.get(villager);
        if (dread != null && dread.isAlive() && dread.level() == villager.level()) {
            villager.getBrain().setMemory(MemoryModuleType.NEAREST_HOSTILE, dread);
            villager.getBrain().setMemory(MemoryModuleType.HEARD_BELL_TIME, villager.level().getGameTime());
        }
    }

    /** The nearest of the Dread it sees within {@link #SEE}, or feels within {@link #NEAR}. */
    @Nullable
    static LivingEntity nearest(Villager villager) {
        LivingEntity best = null;
        double bestSq = Double.MAX_VALUE;
        for (LivingEntity entity : villager.level().getEntitiesOfClass(LivingEntity.class, villager.getBoundingBox().inflate(SEE), VillagerFear::feared)) {
            double sq = entity.distanceToSqr(villager);
            if (sq < bestSq && sq <= SEE * SEE && (sq <= NEAR * NEAR || villager.hasLineOfSight(entity))) {
                best = entity;
                bestSq = sq;
            }
        }
        return best;
    }

    static boolean feared(LivingEntity entity) {
        return entity.isAlive() && !(entity instanceof Player) && RisenDread.dread(entity);
    }

    /** Goals aren't saved: a wandering trader's flight from the Dread is given on every join. */
    private static void onJoin(EntityJoinLevelEvent event) {
        if (!event.getLevel().isClientSide() && event.getEntity() instanceof WanderingTrader trader) {
            trader.goalSelector.addGoal(1, new AvoidEntityGoal<>(trader, LivingEntity.class, 8.0F, 0.5, 0.5, VillagerFear::feared));
        }
    }
}
