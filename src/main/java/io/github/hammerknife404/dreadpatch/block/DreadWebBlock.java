package io.github.hammerknife404.dreadpatch.block;

import com.mojang.serialization.MapCodec;
import io.github.hammerknife404.dreadpatch.entity.RisenDread;
import io.github.hammerknife404.dreadpatch.magic.WebRules;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

/**
 * A dread scuttler's web (0.75.0, {@link WebRules}): where its shot lands, a web that holds less than a
 * cobweb and melts away after {@link WebRules#LIFETIME_TICKS} (a scheduled tick, saved with the world, so it
 * goes even after a reload). It drops nothing, and the Dread pass through it.
 */
public class DreadWebBlock extends Block {
    public static final MapCodec<DreadWebBlock> CODEC = simpleCodec(DreadWebBlock::new);

    public DreadWebBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected MapCodec<? extends Block> codec() {
        return CODEC;
    }

    @Override
    protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState old, boolean moved) {
        if (!level.isClientSide && !old.is(this)) {
            level.scheduleTick(pos, this, WebRules.LIFETIME_TICKS);
        }
    }

    @Override
    protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        level.removeBlock(pos, false);
    }

    @Override
    protected void entityInside(BlockState state, Level level, BlockPos pos, Entity entity) {
        if (!RisenDread.dread(entity)) {
            entity.makeStuckInBlock(state, new Vec3(WebRules.DREAD_WEB_ACROSS, WebRules.DREAD_WEB_UP, WebRules.DREAD_WEB_ACROSS));
        }
    }
}
