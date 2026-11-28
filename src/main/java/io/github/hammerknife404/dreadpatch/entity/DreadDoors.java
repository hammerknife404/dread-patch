package io.github.hammerknife404.dreadpatch.entity;

import com.iafenvoy.iceandfire.entity.DragonBaseEntity;
import com.iafenvoy.iceandfire.entity.DreadKnightEntity;
import io.github.hammerknife404.dreadpatch.DreadPatchConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.animal.horse.AbstractHorse;
import net.minecraft.world.entity.ai.goal.BreakDoorGoal;
import net.minecraft.world.entity.ai.navigation.GroundPathNavigation;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.pathfinder.Node;
import net.minecraft.world.level.pathfinder.Path;
import net.minecraft.world.level.pathfinder.PathType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.EventHooks;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.tick.EntityTickEvent;

/**
 * The Dread break doors as zombies do (0.96.0, the owner: "Dread mobs can break wooden doors like vanilla zombies on
 * hard difficulty (by default and a toggle for it at all and for other difficulties)"; the server config's
 * {@code dread_break_doors} and {@code dread_break_doors_from}). Since 0.98.0 (the owner's handoff: "Give all Dread
 * mobs wooden door breaking as a server config option, plus a separate Hard-difficulty config toggle for breaking iron
 * doors") every walking one of the Dread ({@link RisenDread#dread}: liches and the queen too; not dragons, horses or
 * anything that flies or swims), and iron doors on Hard ({@code dread_break_iron_doors}), twice as slowly: Dread Knights
 * alone (0.98.1, the owner: "Only Dread Knights can break iron level doors").
 * <p>Each gets vanilla's {@link BreakDoorGoal}, at a zombie's priority, its difficulty test the config's (read each time;
 * vanilla's goal also asks for mob griefing), and {@link IronDoorGoal} beside it. As a zombie's, their pathfinding goes
 * through wooden doors only while they may break them, and through iron ones likewise (vanilla never paths through a
 * closed iron door: its malus is set to a wooden door's then), checked once a second, so a change applies at once:
 * otherwise they'd stand at a door they can't open.
 */
public final class DreadDoors {
    /** An iron door takes this many times a wooden door's 240 ticks (Hard). */
    static final int IRON_TIMES = 2;

    private DreadDoors() {
    }

    public static void register(IEventBus gameBus) {
        gameBus.addListener(DreadDoors::onJoin);
        gameBus.addListener(DreadDoors::onTick);
    }

    static boolean breaker(Object entity) {
        return entity instanceof Mob mob && RisenDread.dread(mob) && !(mob instanceof DragonBaseEntity)
                && !(mob instanceof AbstractHorse) && mob.getNavigation() instanceof GroundPathNavigation;
    }

    /** Goals aren't saved: given on every join. */
    private static void onJoin(EntityJoinLevelEvent event) {
        if (!event.getLevel().isClientSide() && breaker(event.getEntity())) {
            Mob mob = (Mob) event.getEntity();
            mob.goalSelector.addGoal(1, new BreakDoorGoal(mob, DreadPatchConfig::dreadBreakDoors));
            if (mob instanceof DreadKnightEntity) {
                mob.goalSelector.addGoal(1, new IronDoorGoal(mob));
            }
            update(mob);
        }
    }

    private static void onTick(EntityTickEvent.Post event) {
        if (event.getEntity().tickCount % 20 == 0 && !event.getEntity().level().isClientSide() && breaker(event.getEntity())) {
            update((Mob) event.getEntity());
        }
    }

    private static void update(Mob mob) {
        ((GroundPathNavigation) mob.getNavigation()).setCanOpenDoors(DreadPatchConfig.dreadBreakDoors(mob.level().getDifficulty()));
        boolean iron = mob instanceof DreadKnightEntity && DreadPatchConfig.dreadBreakIronDoors(mob.level().getDifficulty());
        mob.setPathfindingMalus(PathType.DOOR_IRON_CLOSED, iron ? 0.0F : -1.0F);
    }

    static boolean iron(BlockState state) {
        return state.getBlock() instanceof DoorBlock && !DoorBlock.isWoodenDoor(state);
    }

    /**
     * Vanilla's {@link BreakDoorGoal}, at an iron door: it looks along its path as vanilla's does, for an iron door where
     * vanilla's looks for a wooden one, and takes {@link #IRON_TIMES} as long.
     */
    static final class IronDoorGoal extends BreakDoorGoal {
        IronDoorGoal(Mob mob) {
            super(mob, IRON_TIMES * 240, DreadPatchConfig::dreadBreakIronDoors);
        }

        @Override
        public boolean canUse() {
            if (!this.mob.horizontalCollision || !(this.mob.getNavigation() instanceof GroundPathNavigation navigation)
                    || !DreadPatchConfig.dreadBreakIronDoors(this.mob.level().getDifficulty())
                    || !EventHooks.canEntityGrief(this.mob.level(), this.mob)) {
                return false;
            }
            Path path = navigation.getPath();
            if (path == null || path.isDone()) {
                return false;
            }
            this.hasDoor = false;
            for (int i = 0; i < Math.min(path.getNextNodeIndex() + 2, path.getNodeCount()) && !this.hasDoor; i++) {
                Node node = path.getNode(i);
                BlockPos pos = new BlockPos(node.x, node.y + 1, node.z);
                if (this.mob.distanceToSqr(pos.getX(), this.mob.getY(), pos.getZ()) <= 2.25 && iron(this.mob.level().getBlockState(pos))) {
                    this.doorPos = pos;
                    this.hasDoor = true;
                }
            }
            if (!this.hasDoor) {
                this.doorPos = this.mob.blockPosition().above();
                this.hasDoor = iron(this.mob.level().getBlockState(this.doorPos));
            }
            return this.hasDoor && !this.isOpen();
        }

        @Override
        public void tick() {
            // Vanilla's knock is a wooden door's; at an iron one, a zombie's on iron.
            if (this.mob.getRandom().nextInt(20) == 0) {
                this.mob.level().levelEvent(1020, this.doorPos, 0);
            }
            super.tick();
        }
    }
}
