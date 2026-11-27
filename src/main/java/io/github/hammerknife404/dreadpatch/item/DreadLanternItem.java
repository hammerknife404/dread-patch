package io.github.hammerknife404.dreadpatch.item;

import net.minecraft.core.Direction;
import net.minecraft.world.item.StandingAndWallBlockItem;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

/**
 * The dread lantern's item (0.92.0): against a wall's side it hangs the lantern there on its bracket
 * ({@code block/DreadWallLanternBlock}); on a floor or ceiling, or a wall that can't hold it, it stands or
 * hangs as a lantern does. A vanilla torch's item picks by where the player looks instead, which would put
 * a lantern meant for a ceiling on the nearest wall.
 */
public class DreadLanternItem extends StandingAndWallBlockItem {
    public DreadLanternItem(Block lantern, Block wallLantern, Properties properties) {
        super(lantern, wallLantern, properties, Direction.DOWN);
    }

    @Override
    @Nullable
    protected BlockState getPlacementState(BlockPlaceContext context) {
        if (context.getClickedFace().getAxis().isHorizontal()) {
            BlockState wall = this.wallBlock.getStateForPlacement(context);
            if (wall != null && this.canPlace(context.getLevel(), wall, context.getClickedPos())) {
                return wall;
            }
        }
        BlockState lantern = this.getBlock().getStateForPlacement(context);
        return lantern != null && this.canPlace(context.getLevel(), lantern, context.getClickedPos()) ? lantern : null;
    }
}
