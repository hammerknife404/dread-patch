package io.github.hammerknife404.dreadpatch.entity;

import com.iafenvoy.iceandfire.entity.DragonBaseEntity;
import io.github.hammerknife404.dreadpatch.magic.LeapRules;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.navigation.GroundPathNavigation;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.tick.EntityTickEvent;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.WeakHashMap;

/**
 * Ghouls, beasts, knights and (0.97.4, the owner: "add jumping to the scuttlers too") scuttlers leap narrow gaps
 * ({@link LeapRules}; 0.97.3), and Dread Fish on land (0.97.4, the owner: "and to dread fish, but only on land, if their
 * dolphin-like abilities already do this skip it": their own land lunge goes only at prey in reach, so not wandering or
 * making for water): heading somewhere (its path's end, else its
 * target; so chasing, wandering and on patrol alike), one on the ground at a gap 1-2 blocks wide right ahead toward it,
 * deeper than a step, with ground past it at its own height or one lower and room to stand, leaps it, fast enough to land
 * just past the far edge. Landed, it plans its path afresh (the old one likely ran round the gap). Not one riding, in water,
 * or within a second of its last leap. Thralls didn't (the owner: "to slow to jump") until 0.98.0, when every walking one
 * of the Dread does (the owner's handoff: "Give all Dread mobs the ability to jump over terrain and low obstacles", then,
 * asked about thralls: "All Dread leap"): thralls, liches, Dread horses, the queen (but her only toward her path's end,
 * never across a gap toward the target she keeps her distance from), the risen that walk; not dragons, nor what flies.
 * Her hops in a fight ({@link #hop}) are {@code KeepDistanceGoal}'s.
 */
public final class DreadLeaps {
    /** Game time each may next leap, and whether it's in the air from one. Server thread only. */
    private static final Map<Mob, Long> NEXT = new WeakHashMap<>();
    private static final Map<Mob, Boolean> FLYING = new WeakHashMap<>();

    private DreadLeaps() {
    }

    public static void register(IEventBus gameBus) {
        gameBus.addListener(DreadLeaps::onTick);
    }

    static boolean leaper(Object entity) {
        return entity instanceof DreadFish || entity instanceof Mob mob && RisenDread.dread(mob) && !(mob instanceof DragonBaseEntity)
                && mob.getNavigation() instanceof GroundPathNavigation;
    }

    private static void onTick(EntityTickEvent.Post event) {
        if (!leaper(event.getEntity()) || event.getEntity().level().isClientSide) {
            return;
        }
        Mob mob = (Mob) event.getEntity();
        if (!mob.isAlive() || mob.isNoAi() || mob.isPassenger() || mob.isInWater() || !mob.onGround()) {
            return;
        }
        if (FLYING.remove(mob) != null && !mob.getNavigation().isDone()) {
            mob.getNavigation().recomputePath();
        }
        long now = mob.level().getGameTime();
        if (now < NEXT.getOrDefault(mob, 0L)) {
            return;
        }
        Vec3 toward = heading(mob);
        if (toward == null) {
            return;
        }
        Vec3 leap = leap(mob, toward);
        if (leap != null) {
            launch(mob, leap, toward);
        }
    }

    /** Which way it's going across the ground: to its path's end, else its target; null if nowhere, or nearly there. */
    @Nullable
    static Vec3 heading(Mob mob) {
        Vec3 to = null;
        if (!mob.getNavigation().isDone() && mob.getNavigation().getTargetPos() != null) {
            to = Vec3.atBottomCenterOf(mob.getNavigation().getTargetPos());
        } else if (!keepsAway(mob)) {
            LivingEntity target = mob.getTarget();
            if (target != null && target.isAlive()) {
                to = target.position();
            }
        }
        if (to == null) {
            return null;
        }
        Vec3 flat = new Vec3(to.x - mob.getX(), 0.0, to.z - mob.getZ());
        return flat.lengthSqr() < 9.0 ? null : flat.normalize();
    }

    /**
     * Whether it keeps its distance from its target, so never leaps toward it: the queen, a lich, a thrall with a bow or
     * crossbow (0.98.13, a review: liches and bowmen leapt a ravine at the player they were standing off from).
     */
    static boolean keepsAway(Mob mob) {
        return mob instanceof DreadQueen || mob instanceof com.iafenvoy.iceandfire.entity.DreadLichEntity
                || mob instanceof com.iafenvoy.iceandfire.entity.DreadThrallEntity && ThrallWeapons.ranged(mob.getMainHandItem());
    }

    /** Its leap over the gap right ahead along {@code toward}, or null if there's none it leaps. */
    @Nullable
    static Vec3 leap(Mob mob, Vec3 toward) {
        Level level = mob.level();
        int feet = Mth.floor(mob.getY() + 0.01);
        // From half a block past its middle: overhanging an edge, the gap's first block is under it, and counts.
        double start = 0.5;
        int gap = 0;
        for (int k = 0; k <= LeapRules.MAX_GAP; k++) {
            BlockPos cell = BlockPos.containing(mob.getX() + toward.x * (start + k), feet, mob.getZ() + toward.z * (start + k));
            if (!clear(level, cell, mob)) {
                return null;
            }
            boolean ground = solid(level, cell.below());
            boolean step = !ground && solid(level, cell.below(2));
            if (ground || step) {
                // Ground to land on (its height, or one lower): a gap before it is one to leap; none at all, it just walks.
                if (!LeapRules.leaps(gap)) {
                    return null;
                }
                // Never into lava or fire (0.98.13, a review: lava has no collision shape, so a lava pool read as landing).
                BlockPos landing = ground ? cell : cell.below();
                if (hazard(level, landing)) {
                    return null;
                }
                double drop = ground ? 0.0 : 1.0;
                double speed = LeapRules.speedFor(start + gap + LeapRules.MARGIN, drop);
                return new Vec3(toward.x * speed, LeapRules.RISE, toward.z * speed);
            }
            gap++;
        }
        return null;
    }

    /**
     * The queen's hop {@code distance} along {@code toward} (flat, length 1; {@link LeapRules#hop}), if there's room the
     * whole way (her height free at each block) and ground to land on at her height or one lower: its velocity, else null.
     */
    @Nullable
    static Vec3 hop(Mob mob, Vec3 toward, double distance) {
        Level level = mob.level();
        int feet = Mth.floor(mob.getY() + 0.01);
        for (double d = 1.0; d <= distance + 0.01; d += 1.0) {
            BlockPos at = BlockPos.containing(mob.getX() + toward.x * d, feet, mob.getZ() + toward.z * d);
            // Room for its rise too, and no drop of more than a block on the way (0.98.13, a review: under a low ceiling
            // she bumped it and fell short, maybe into a gap the hop never looked at).
            if (!clear(level, at, mob) || !clear(level, at.above(Mth.ceil(mob.getBbHeight())), mob, 1)
                    || !solid(level, at.below()) && !solid(level, at.below(2))) {
                return null;
            }
        }
        BlockPos land = BlockPos.containing(mob.getX() + toward.x * distance, feet, mob.getZ() + toward.z * distance);
        double drop;
        if (solid(level, land.below())) {
            drop = 0.0;
        } else if (solid(level, land.below(2))) {
            drop = 1.0;
        } else {
            return null;
        }
        if (!level.getFluidState(land).isEmpty() || !level.getFluidState(land.below((int) drop)).isEmpty()) {
            return null;
        }
        double speed = LeapRules.speedFor(distance, drop);
        return new Vec3(toward.x * speed, LeapRules.RISE, toward.z * speed);
    }

    /** Leaps {@code mob} with {@code velocity}, facing {@code facing}: as a gap leap, its path planned afresh on landing. */
    static void launch(Mob mob, Vec3 velocity, Vec3 facing) {
        mob.setDeltaMovement(velocity);
        // A Dread Fish isn't turned at once (0.98.11: its whole chained body swung round); it turns as it flies.
        if (!(mob instanceof DreadFish)) {
            mob.setYRot((float) (Mth.atan2(facing.z, facing.x) * Mth.RAD_TO_DEG) - 90.0F);
            mob.yBodyRot = mob.getYRot();
        }
        mob.hasImpulse = true;
        NEXT.put(mob, mob.level().getGameTime() + LeapRules.COOLDOWN);
        FLYING.put(mob, true);
    }

    /** Lava or fire at {@code pos}: no place to land. */
    private static boolean hazard(Level level, BlockPos pos) {
        return level.getFluidState(pos).is(net.minecraft.tags.FluidTags.LAVA) || level.getBlockState(pos).is(net.minecraft.tags.BlockTags.FIRE)
                || level.getFluidState(pos.below()).is(net.minecraft.tags.FluidTags.LAVA);
    }

    /** Room for it to pass at {@code cell}: its height free of anything solid. */
    private static boolean clear(Level level, BlockPos cell, Mob mob) {
        return clear(level, cell, mob, Mth.ceil(mob.getBbHeight()));
    }

    /** {@code height} blocks from {@code cell} up free of anything solid. */
    private static boolean clear(Level level, BlockPos cell, Mob mob, int height) {
        for (int y = 0; y < height; y++) {
            if (!level.getBlockState(cell.above(y)).getCollisionShape(level, cell.above(y)).isEmpty()) {
                return false;
            }
        }
        return true;
    }

    private static boolean solid(Level level, BlockPos pos) {
        return level.getBlockState(pos).isFaceSturdy(level, pos, Direction.UP);
    }
}
