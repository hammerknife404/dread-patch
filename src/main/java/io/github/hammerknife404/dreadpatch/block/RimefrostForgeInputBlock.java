package io.github.hammerknife404.dreadpatch.block;

import com.iafenvoy.iceandfire.item.block.util.DragonProof;
import com.mojang.serialization.MapCodec;
import io.github.hammerknife404.dreadpatch.registry.DreadForge;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.Nullable;

/**
 * The Rimefrost dragonforge's aperture (0.59.0), Ice and Fire's ice forge aperture made Dread: set in
 * a side of the forge, beside its core. A Rimefrost dragon's breath landing on it lights the forge, and a
 * whole forge with something to make draws a tame one to breathe on it ({@link RimefrostForgeInputBlockEntity}).
 * It glows while breath is on it ({@link #ACTIVE}; Ice and Fire's has the texture but never sets it).
 * Used, it opens its forge. Dragon-proof.
 */
public class RimefrostForgeInputBlock extends BaseEntityBlock implements DragonProof {
    public static final MapCodec<RimefrostForgeInputBlock> CODEC = simpleCodec(RimefrostForgeInputBlock::new);
    public static final BooleanProperty ACTIVE = BooleanProperty.create("active");

    public RimefrostForgeInputBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any().setValue(ACTIVE, false));
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(ACTIVE);
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new RimefrostForgeInputBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide ? null : createTickerHelper(type, DreadForge.INPUT_ENTITY.get(), RimefrostForgeInputBlockEntity::serverTick);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        RimefrostForgeBlockEntity forge = RimefrostForgeInputBlockEntity.core(level, pos);
        if (forge == null) {
            return InteractionResult.PASS;
        }
        if (player instanceof ServerPlayer serverPlayer) {
            serverPlayer.openMenu(forge);
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }
}
