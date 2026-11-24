package io.github.hammerknife404.dreadpatch.block;

import com.iafenvoy.iceandfire.item.block.util.DragonProof;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.Nullable;

/**
 * The Rimefrost dragonforge's brick (0.59.0), Ice and Fire's ice forge brick made Dread: its grill
 * shows on the four beside a whole forge's core ({@link #GRILL}, set by the core). Used, it opens the
 * whole forge it touches. Hoppers and pipes on it reach the core's slots (a capability, {@code DreadForge}),
 * where Ice and Fire gives its bricks a block entity for that. Dragon-proof.
 */
public class RimefrostForgeBrickBlock extends Block implements DragonProof {
    public static final BooleanProperty GRILL = BooleanProperty.create("grill");

    public RimefrostForgeBrickBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any().setValue(GRILL, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(GRILL);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        RimefrostForgeBlockEntity forge = core(level, pos);
        if (forge == null || !forge.assembled()) {
            return InteractionResult.PASS;
        }
        if (player instanceof ServerPlayer serverPlayer) {
            serverPlayer.openMenu(forge);
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    /** The forge core touching it, if any. */
    @Nullable
    public static RimefrostForgeBlockEntity core(Level level, BlockPos pos) {
        for (Direction side : Direction.values()) {
            if (level.getBlockEntity(pos.relative(side)) instanceof RimefrostForgeBlockEntity forge) {
                return forge;
            }
        }
        return null;
    }
}
