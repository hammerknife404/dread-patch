package io.github.hammerknife404.dreadpatch.entity;

import com.iafenvoy.iceandfire.entity.DreadBeastEntity;
import com.iafenvoy.iceandfire.entity.DreadGhoulEntity;
import com.iafenvoy.iceandfire.entity.DreadKnightEntity;
import io.github.hammerknife404.dreadpatch.magic.LeapRules;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.tick.EntityTickEvent;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.WeakHashMap;

/**
 * Ghouls, beasts and knights leap narrow gaps ({@link LeapRules}; 0.97.3): heading somewhere (its path's end, else its
 * target; so chasing, wandering and on patrol alike), one on the ground at a gap 1-2 blocks wide right ahead toward it,
 * deeper than a step, with ground past it at its own height or one lower and room to stand, leaps it, fast enough to land
 * just past the far edge. Landed, it plans its path afresh (the old one likely ran round the gap). Thralls (the owner: "to
 * slow to jump") and scuttlers (which climb) don't; nor one riding, in water, or within a second of its last leap.
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
        return entity instanceof DreadGhoulEntity || entity instanceof DreadBeastEntity || entity instanceof DreadKnightEntity;
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
            mob.setDeltaMovement(leap);
            mob.setYRot((float) (Mth.atan2(toward.z, toward.x) * Mth.RAD_TO_DEG) - 90.0F);
            mob.yBodyRot = mob.getYRot();
            mob.hasImpulse = true;
            NEXT.put(mob, now + LeapRules.COOLDOWN);
            FLYING.put(mob, true);
        }
    }

    /** Which way it's going across the ground: to its path's end, else its target; null if nowhere, or nearly there. */
    @Nullable
    static Vec3 heading(Mob mob) {
        Vec3 to = null;
        if (!mob.getNavigation().isDone() && mob.getNavigation().getTargetPos() != null) {
            to = Vec3.atBottomCenterOf(mob.getNavigation().getTargetPos());
        } else {
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
                double drop = ground ? 0.0 : 1.0;
                double speed = LeapRules.speedFor(start + gap + LeapRules.MARGIN, drop);
                return new Vec3(toward.x * speed, LeapRules.RISE, toward.z * speed);
            }
            gap++;
        }
        return null;
    }

    /** Room for it to pass at {@code cell}: its height free of anything solid. */
    private static boolean clear(Level level, BlockPos cell, Mob mob) {
        int height = Mth.ceil(mob.getBbHeight());
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
