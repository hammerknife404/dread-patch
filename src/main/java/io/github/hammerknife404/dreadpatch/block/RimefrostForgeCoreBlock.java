package io.github.hammerknife404.dreadpatch.block;

import com.iafenvoy.iceandfire.item.block.util.DragonProof;
import com.mojang.serialization.MapCodec;
import io.github.hammerknife404.dreadpatch.registry.DreadForge;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.Nullable;

/**
 * The Rimefrost dragonforge's core (0.59.0): Ice and Fire's ice dragonforge core made Dread. Its
 * forge ({@link RimefrostForgeBlockEntity}) cooks while a Rimefrost dragon's breath keeps it lit. One
 * block, lit while its forge stands whole (Ice and Fire swaps two blocks, and lights its core on any
 * change, whole or not); its light is 10, under what melts snow and ice, as the Dread's lanterns are.
 * Dragon-proof, as Ice and Fire's forge blocks are.
 */
public class RimefrostForgeCoreBlock extends BaseEntityBlock implements DragonProof {
    public static final MapCodec<RimefrostForgeCoreBlock> CODEC = simpleCodec(RimefrostForgeCoreBlock::new);
    public static final BooleanProperty LIT = BlockStateProperties.LIT;
    /** Its light while lit. */
    public static final int LIGHT = 10;

    public RimefrostForgeCoreBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any().setValue(LIT, false));
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(LIT);
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new RimefrostForgeBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide ? null : createTickerHelper(type, DreadForge.CORE_ENTITY.get(), RimefrostForgeBlockEntity::serverTick);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (player.isShiftKeyDown()) {
            return InteractionResult.PASS;
        }
        if (player instanceof ServerPlayer serverPlayer && level.getBlockEntity(pos) instanceof RimefrostForgeBlockEntity forge) {
            serverPlayer.openMenu(forge);
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState old, boolean moving) {
        super.onPlace(state, level, pos, old, moving);
        if (!old.is(this)) {
            invalidateAround(level, pos);
        }
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState replacement, boolean moving) {
        if (!state.is(replacement.getBlock())) {
            if (level.getBlockEntity(pos) instanceof RimefrostForgeBlockEntity forge) {
                Containers.dropContents(level, pos, forge);
                level.updateNeighbourForOutputSignal(pos, this);
            }
            invalidateAround(level, pos);
        }
        super.onRemove(state, level, pos, replacement, moving);
    }

    /** Its bricks hand hoppers and pipes its slots (their capability): tell whoever cached one that it's come or gone. */
    private static void invalidateAround(Level level, BlockPos pos) {
        for (Direction side : Direction.values()) {
            level.invalidateCapabilities(pos.relative(side));
        }
    }

    @Override
    protected boolean hasAnalogOutputSignal(BlockState state) {
        return true;
    }

    @Override
    protected int getAnalogOutputSignal(BlockState state, Level level, BlockPos pos) {
        return AbstractContainerMenu.getRedstoneSignalFromBlockEntity(level.getBlockEntity(pos));
    }
}
