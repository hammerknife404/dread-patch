package io.github.hammerknife404.dreadpatch.world;

import com.iafenvoy.iceandfire.registry.IafBlocks;
import io.github.hammerknife404.dreadpatch.registry.ModRegistries;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent;
import org.jetbrains.annotations.Nullable;

import java.util.Iterator;
import java.util.Map;

/**
 * Flames in the Dreadlands (0.57.0, the owner: "Any lanterns placed fade from lanter to soul lantern to dread
 * lanterns. Same with Torches."): a lantern or torch placed there turns, a step every 30-45 s
 * (0.58.0, the owner; server config {@code flame_fade_min_seconds}/{@code _max_seconds}), to its soul kind and then to the Dread's: the dread lantern
 * (ours) or Ice and Fire's dread torch, hanging, standing or on its wall as it was, with a puff of smoke and
 * a fizz. Pending steps are kept in {@link DreadlandsData}, so a restart doesn't stop them. Abandoned
 * villages' lanterns and torches are the Dread's from the start ({@link #dread}).
 */
public final class DreadFlames {
    private DreadFlames() {
    }

    public static void register(IEventBus gameBus) {
        gameBus.addListener(DreadFlames::onPlace);
        gameBus.addListener(DreadFlames::onLevelTick);
    }

    /** The next step for a flame, or null if it's the Dread's already (or no flame). */
    @Nullable
    public static BlockState next(BlockState state) {
        if (state.is(Blocks.LANTERN)) {
            return shaped(state, Blocks.SOUL_LANTERN.defaultBlockState());
        }
        if (state.is(Blocks.TORCH)) {
            return Blocks.SOUL_TORCH.defaultBlockState();
        }
        if (state.is(Blocks.WALL_TORCH)) {
            return shaped(state, Blocks.SOUL_WALL_TORCH.defaultBlockState());
        }
        return dread(state);
    }

    /** The Dread's kind of a lantern or torch (any), its shape kept; null if it isn't one, or already is. */
    @Nullable
    public static BlockState dread(BlockState state) {
        if (state.is(Blocks.LANTERN) || state.is(Blocks.SOUL_LANTERN)) {
            return shaped(state, ModRegistries.DREAD_LANTERN.get().defaultBlockState());
        }
        if (state.is(Blocks.TORCH) || state.is(Blocks.SOUL_TORCH)) {
            return IafBlocks.DREAD_TORCH.get().defaultBlockState();
        }
        if (state.is(Blocks.WALL_TORCH) || state.is(Blocks.SOUL_WALL_TORCH)) {
            return shaped(state, IafBlocks.DREAD_TORCH_WALL.get().defaultBlockState());
        }
        return null;
    }

    /** {@code to}, with every property it shares with {@code from} copied. */
    public static BlockState shaped(BlockState from, BlockState to) {
        BlockState result = to;
        for (Property<?> property : from.getProperties()) {
            if (result.hasProperty(property)) {
                result = copy(from, result, property);
            }
        }
        return result;
    }

    private static <T extends Comparable<T>> BlockState copy(BlockState from, BlockState to, Property<T> property) {
        return to.setValue(property, from.getValue(property));
    }

    private static void onPlace(BlockEvent.EntityPlaceEvent event) {
        if (event.getLevel() instanceof ServerLevel level && Dreadlands.is(level) && next(event.getPlacedBlock()) != null) {
            schedule(level, event.getPos());
        }
    }

    static void schedule(ServerLevel level, BlockPos pos) {
        long due = level.getGameTime() + step(level);
        DreadlandsData data = DreadlandsData.get(level.getServer());
        data.fading().put(pos.asLong(), due);
        data.setDirty();
    }

    /** Ticks to a flame's next step: the config's seconds. */
    private static int step(ServerLevel level) {
        int min = io.github.hammerknife404.dreadpatch.DreadPatchConfig.flameFadeMinTicks();
        int max = io.github.hammerknife404.dreadpatch.DreadPatchConfig.flameFadeMaxTicks();
        return min + level.getRandom().nextInt(max - min + 1);
    }

    private static void onLevelTick(LevelTickEvent.Post event) {
        if (!(event.getLevel() instanceof ServerLevel level) || !Dreadlands.is(level) || level.getGameTime() % 20 != 0) {
            return;
        }
        DreadlandsData data = DreadlandsData.get(level.getServer());
        Map<Long, Long> fading = data.fading();
        if (fading.isEmpty()) {
            return;
        }
        long now = level.getGameTime();
        boolean changed = false;
        for (Iterator<Map.Entry<Long, Long>> it = fading.entrySet().iterator(); it.hasNext(); ) {
            Map.Entry<Long, Long> entry = it.next();
            BlockPos pos = BlockPos.of(entry.getKey());
            if (entry.getValue() > now || !level.isLoaded(pos)) {
                continue;
            }
            BlockState next = next(level.getBlockState(pos));
            if (next == null) {
                it.remove();
            } else {
                level.setBlock(pos, next, Block.UPDATE_ALL);
                level.sendParticles(ParticleTypes.SMOKE, pos.getX() + 0.5, pos.getY() + 0.6, pos.getZ() + 0.5, 6, 0.15, 0.1, 0.15, 0.01);
                level.playSound(null, pos, SoundEvents.FIRE_EXTINGUISH, SoundSource.BLOCKS, 0.3F, 1.6F);
                if (next(next) == null) {
                    it.remove();
                } else {
                    entry.setValue(now + step(level));
                }
            }
            changed = true;
        }
        if (changed) {
            data.setDirty();
        }
    }
}
