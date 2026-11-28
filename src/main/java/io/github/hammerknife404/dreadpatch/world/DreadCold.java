package io.github.hammerknife404.dreadpatch.world;

import com.iafenvoy.iceandfire.registry.IafBlocks;
import io.github.hammerknife404.dreadpatch.DreadPatchConfig;
import io.github.hammerknife404.dreadpatch.magic.ColdRules;
import io.github.hammerknife404.dreadpatch.registry.ModRegistries;
import it.unimi.dsi.fastutil.longs.Long2LongOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.LongArrayTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.saveddata.SavedData;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.event.level.ChunkEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent;

import java.util.ArrayList;
import java.util.List;

/**
 * Dread torches and lanterns as sources of cold (0.65.0, {@link ColdRules}): each level keeps where its dread lights
 * are (found as chunks load, and whenever one's set: placed, turned from another flame, or set any way that tells
 * its neighbours), and every {@link ColdRules#CHECK_EVERY} ticks the
 * water round each loaded one gathers cold in game time: ice, packed ice, then blue ice, a third of
 * {@code dread_light_freeze_minutes} each. Water is any still source of a fluid in {@code #minecraft:water} as its own
 * block (not a waterlogged block). A block that stops being water or ice loses its count.
 */
public final class DreadCold extends SavedData {
    static final String NAME = "dreadpatch_cold";

    private final LongOpenHashSet lights = new LongOpenHashSet();
    /** Per block: its cold so far, and the game time it was last counted. */
    private final Long2LongOpenHashMap cold = new Long2LongOpenHashMap();
    private final Long2LongOpenHashMap counted = new Long2LongOpenHashMap();

    public static void register(IEventBus gameBus) {
        gameBus.addListener(DreadCold::onChunkLoad);
        gameBus.addListener(DreadCold::onPlace);
        gameBus.addListener(DreadCold::onNeighborNotify);
        gameBus.addListener(DreadCold::onLevelTick);
    }

    public static DreadCold get(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(new SavedData.Factory<>(DreadCold::new, DreadCold::load), NAME);
    }

    public static boolean isDreadLight(BlockState state) {
        return state.is(IafBlocks.DREAD_TORCH.get()) || state.is(IafBlocks.DREAD_TORCH_WALL.get()) || state.is(ModRegistries.DREAD_LANTERN.get())
                || state.is(ModRegistries.DREAD_WALL_LANTERN.get()) || io.github.hammerknife404.dreadpatch.block.DreadCampfireBlock.litDread(state);
    }

    /** A dread light at {@code pos} now (placed, or a flame turned to one). */
    public static void track(LevelAccessor level, BlockPos pos) {
        if (level instanceof ServerLevel server && get(server).lights.add(pos.asLong())) {
            get(server).setDirty();
        }
    }

    private static void onChunkLoad(ChunkEvent.Load event) {
        if (!(event.getLevel() instanceof ServerLevel level) || !(event.getChunk() instanceof LevelChunk chunk)) {
            return;
        }
        LevelChunkSection[] sections = chunk.getSections();
        for (int i = 0; i < sections.length; i++) {
            LevelChunkSection section = sections[i];
            if (section.hasOnlyAir() || !section.maybeHas(DreadCold::isDreadLight)) {
                continue;
            }
            int baseY = level.getSectionYFromSectionIndex(i) << 4;
            for (int x = 0; x < 16; x++) {
                for (int y = 0; y < 16; y++) {
                    for (int z = 0; z < 16; z++) {
                        if (isDreadLight(section.getBlockState(x, y, z))) {
                            track(level, new BlockPos(chunk.getPos().getMinBlockX() + x, baseY + y, chunk.getPos().getMinBlockZ() + z));
                        }
                    }
                }
            }
        }
    }

    private static void onPlace(BlockEvent.EntityPlaceEvent event) {
        if (isDreadLight(event.getPlacedBlock())) {
            track(event.getLevel(), event.getPos());
        }
    }

    /** Any block change that tells its neighbours (a command's, another mod's, a piston's): a dread light set there is tracked too. */
    private static void onNeighborNotify(BlockEvent.NeighborNotifyEvent event) {
        if (isDreadLight(event.getState())) {
            track(event.getLevel(), event.getPos());
        }
    }

    private static void onLevelTick(LevelTickEvent.Post event) {
        if (!(event.getLevel() instanceof ServerLevel level) || level.getGameTime() % ColdRules.CHECK_EVERY != 0) {
            return;
        }
        long stage = ColdRules.stageTicks(DreadPatchConfig.dreadLightFreezeMinutes());
        if (stage <= 0L) {
            return;
        }
        DreadCold data = get(level);
        if (data.lights.isEmpty()) {
            return;
        }
        // Every block in reach of any loaded light, once each, however many lights reach it.
        LongOpenHashSet near = new LongOpenHashSet();
        List<Long> gone = new ArrayList<>();
        for (long light : data.lights) {
            BlockPos pos = BlockPos.of(light);
            if (!level.isLoaded(pos)) {
                continue;
            }
            if (!isDreadLight(level.getBlockState(pos))) {
                gone.add(light);
                continue;
            }
            for (BlockPos at : BlockPos.betweenClosed(pos.offset(-ColdRules.RADIUS, -ColdRules.DOWN, -ColdRules.RADIUS),
                    pos.offset(ColdRules.RADIUS, ColdRules.UP, ColdRules.RADIUS))) {
                near.add(at.asLong());
            }
        }
        gone.forEach(data.lights::remove);
        long now = level.getGameTime();
        for (long at : near) {
            BlockPos pos = BlockPos.of(at);
            ColdRules.Stage was = stage(level.getBlockState(pos), level.getFluidState(pos));
            if (was == null || was == ColdRules.Stage.BLUE_ICE) {
                data.cold.remove(at);
                data.counted.remove(at);
                continue;
            }
            long had = data.cold.get(at);
            long elapsed = data.counted.containsKey(at) ? now - data.counted.get(at) : 0L;
            data.counted.put(at, now);
            if (ColdRules.colder(had, elapsed, stage)) {
                level.setBlockAndUpdate(pos, switch (was.next()) {
                    case ICE -> Blocks.ICE.defaultBlockState();
                    case PACKED_ICE -> Blocks.PACKED_ICE.defaultBlockState();
                    default -> Blocks.BLUE_ICE.defaultBlockState();
                });
                data.cold.put(at, 0L);
            } else {
                data.cold.put(at, had + elapsed);
            }
        }
        // Counts for blocks no light reaches now: dropped.
        data.cold.keySet().removeIf(at -> !near.contains(at) && level.isLoaded(BlockPos.of(at)));
        data.counted.keySet().removeIf(at -> !data.cold.containsKey(at));
        data.setDirty();
    }

    /** Where the cold has a block in its chain: still water (as its own block), ice, packed ice, blue ice; else null. */
    static ColdRules.Stage stage(BlockState state, FluidState fluid) {
        if (state.getBlock() instanceof LiquidBlock && fluid.is(FluidTags.WATER) && fluid.isSource()) {
            return ColdRules.Stage.WATER;
        }
        if (state.is(Blocks.ICE) || state.is(Blocks.FROSTED_ICE)) {
            return ColdRules.Stage.ICE;
        }
        if (state.is(Blocks.PACKED_ICE)) {
            return ColdRules.Stage.PACKED_ICE;
        }
        return state.is(Blocks.BLUE_ICE) ? ColdRules.Stage.BLUE_ICE : null;
    }

    private static DreadCold load(CompoundTag tag, HolderLookup.Provider registries) {
        DreadCold data = new DreadCold();
        for (long light : tag.getLongArray("Lights")) {
            data.lights.add(light);
        }
        long[] at = tag.getLongArray("ColdAt");
        long[] had = tag.getLongArray("Cold");
        long[] when = tag.getLongArray("Counted");
        for (int i = 0; i < Math.min(at.length, Math.min(had.length, when.length)); i++) {
            data.cold.put(at[i], had[i]);
            data.counted.put(at[i], when[i]);
        }
        return data;
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        tag.put("Lights", new LongArrayTag(this.lights.toLongArray()));
        long[] at = this.cold.keySet().toLongArray();
        long[] had = new long[at.length];
        long[] when = new long[at.length];
        for (int i = 0; i < at.length; i++) {
            had[i] = this.cold.get(at[i]);
            when[i] = this.counted.get(at[i]);
        }
        tag.put("ColdAt", new LongArrayTag(at));
        tag.put("Cold", new LongArrayTag(had));
        tag.put("Counted", new LongArrayTag(when));
        return tag;
    }
}
