package io.github.hammerknife404.dreadpatch.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.FrostedIceBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.level.gameevent.GameEvent;
import org.jetbrains.annotations.Nullable;

/**
 * Rimewalk, the Dread Queen's Frost Walker: water freezes under her as it does under Frost Walker II
 * boots (vanilla's {@code replace_disk}: still water with air over it, in a disk of
 * {@link #RADIUS} round her feet, to frosted ice, which melts back on its own), so water can't keep
 * her off anyone. Unlike the enchantment, the ice in her disk stays whole while she stands on it
 * (its melting set back), and in water she freezes the surface round her, so she climbs out onto it.
 */
final class Rimewalk {
    /** Frost Walker II's. */
    static final int RADIUS = 4;

    private Rimewalk() {
    }

    /** Each tick, on the ground or in water, not riding. */
    static void tick(LivingEntity walker) {
        if (!walker.isAlive() || walker.isPassenger() || walker.isSpectator()) {
            return;
        }
        Level level = walker.level();
        BlockPos layer;
        if (walker.onGround()) {
            layer = BlockPos.containing(walker.position()).below();
        } else if (walker.isInWater()) {
            layer = surfaceAround(walker);
        } else {
            return;
        }
        if (layer == null) {
            return;
        }
        BlockState ice = Blocks.FROSTED_ICE.defaultBlockState();
        BlockPos.MutableBlockPos at = new BlockPos.MutableBlockPos();
        double x = walker.getX();
        double z = walker.getZ();
        for (int dz = -RADIUS; dz <= RADIUS; dz++) {
            for (int dx = -RADIUS; dx <= RADIUS; dx++) {
                at.set(layer.getX() + dx, layer.getY(), layer.getZ() + dz);
                if (Mth.square(at.getX() + 0.5 - x) + Mth.square(at.getZ() + 0.5 - z) >= RADIUS * RADIUS) {
                    continue;
                }
                BlockState state = level.getBlockState(at);
                if (state.is(Blocks.FROSTED_ICE)) {
                    // Hers stays frozen while she's on it: the melting Frost Walker's ice does in daylight, set back.
                    if (state.getValue(FrostedIceBlock.AGE) > 0) {
                        level.setBlock(at, ice, 2);
                    }
                } else if (canFreeze(level, at, state) && level.setBlockAndUpdate(at, ice)) {
                    level.gameEvent(walker, GameEvent.BLOCK_PLACE, at);
                }
            }
        }
    }

    /**
     * Where Frost Walker freezes: still water (a source) open to the air, with nothing living (or a
     * boat) in the way. Her storm freezes by the same rule ({@code WhiteoutDominion}).
     */
    static boolean canFreeze(Level level, BlockPos at, BlockState state) {
        return state.is(Blocks.WATER) && state.getFluidState().isSource()
                && level.getBlockState(at.above()).isAir()
                && level.isUnobstructed(Blocks.FROSTED_ICE.defaultBlockState(), at, CollisionContext.empty());
    }

    /** In water: the top water block in her column, from her feet to her eyes, if it's open to the air. */
    @Nullable
    private static BlockPos surfaceAround(LivingEntity walker) {
        Level level = walker.level();
        BlockPos.MutableBlockPos at = BlockPos.containing(walker.position()).mutable();
        int eyes = Mth.floor(walker.getEyeY());
        for (; at.getY() <= eyes; at.move(0, 1, 0)) {
            if (level.getBlockState(at).is(Blocks.WATER) && level.getBlockState(at.above()).isAir()) {
                return at.immutable();
            }
        }
        return null;
    }
}
