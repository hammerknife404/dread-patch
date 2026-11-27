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
 * <p>
 * Its melting is hers (0.97.2, the owner: "dread queens frost walk ice blocks no longer remove themselves"): vanilla's
 * frosted ice melts only where the light is over 11 less its age, so never at night or under the Dreadlands' snow, and
 * hers stood. Each block she freezes, or keeps frozen, is noted with the game time; {@link #MELT_AFTER} after she last
 * kept it, it ages a stage every second or so (by chance, so the edge breaks up as vanilla's does) and then thaws to
 * water, whatever the light. Not saved: after a restart what's left melts as vanilla's would, by day.
 */
public final class Rimewalk {
    /** Frost Walker II's. */
    static final int RADIUS = 4;
    /** Ticks after she last kept a block before it starts to melt (vanilla's 60-120 before its first check). */
    static final int MELT_AFTER = 60;
    /** Each block of hers, by level: the game time she last froze or kept it. Server thread only. */
    private static final java.util.Map<Level, it.unimi.dsi.fastutil.longs.Long2LongOpenHashMap> ICE = new java.util.WeakHashMap<>();

    public static void register(net.neoforged.bus.api.IEventBus gameBus) {
        gameBus.addListener(Rimewalk::melt);
    }

    private static void keep(Level level, BlockPos at) {
        ICE.computeIfAbsent(level, l -> new it.unimi.dsi.fastutil.longs.Long2LongOpenHashMap()).put(at.asLong(), level.getGameTime());
    }

    /** Every 20 ticks, each block she's left a while ages a stage half the time, and thaws past its last. */
    private static void melt(net.neoforged.neoforge.event.tick.LevelTickEvent.Post event) {
        Level level = event.getLevel();
        if (level.isClientSide || level.getGameTime() % 20 != 0) {
            return;
        }
        it.unimi.dsi.fastutil.longs.Long2LongOpenHashMap ice = ICE.get(level);
        if (ice == null || ice.isEmpty()) {
            return;
        }
        long now = level.getGameTime();
        BlockPos.MutableBlockPos at = new BlockPos.MutableBlockPos();
        for (var it = ice.long2LongEntrySet().fastIterator(); it.hasNext(); ) {
            var entry = it.next();
            at.set(entry.getLongKey());
            if (!level.isLoaded(at)) {
                continue;
            }
            BlockState state = level.getBlockState(at);
            if (!state.is(Blocks.FROSTED_ICE)) {
                it.remove();
            } else if (now - entry.getLongValue() >= MELT_AFTER && level.random.nextBoolean()) {
                int age = state.getValue(FrostedIceBlock.AGE);
                if (age < FrostedIceBlock.MAX_AGE) {
                    level.setBlock(at, state.setValue(FrostedIceBlock.AGE, age + 1), 2);
                } else {
                    level.setBlockAndUpdate(at, Blocks.WATER.defaultBlockState());
                    level.neighborChanged(at, Blocks.WATER, at);
                    it.remove();
                }
            }
        }
    }

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
                    // Only her own (her storm's ice is its own to thaw, a player's Frost Walker's vanilla's).
                    if (ICE.containsKey(level) && ICE.get(level).containsKey(at.asLong())) {
                        keep(level, at);
                    }
                } else if (canFreeze(level, at, state) && level.setBlockAndUpdate(at, ice)) {
                    level.gameEvent(walker, GameEvent.BLOCK_PLACE, at);
                    keep(level, at);
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
