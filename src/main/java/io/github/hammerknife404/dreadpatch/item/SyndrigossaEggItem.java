package io.github.hammerknife404.dreadpatch.item;

import io.github.hammerknife404.dreadpatch.block.DreadGateBlock;
import io.github.hammerknife404.dreadpatch.entity.SyndrigossaEggEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * Syndrigossa's egg (only the boss, an ancient Syndrigossa, leaves it): laid on top of dreadstone (the
 * blocks a Dread Gate's frame takes), it waits for Whiteout Dominion ({@link SyndrigossaEggEntity}).
 * Anywhere else, a hint and nothing laid.
 */
public class SyndrigossaEggItem extends Item {
    public SyndrigossaEggItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        Player player = context.getPlayer();
        boolean dreadstone = context.getClickedFace() == Direction.UP && level.getBlockState(pos).is(DreadGateBlock.FRAME)
                && level.getBlockState(pos.above()).getCollisionShape(level, pos.above()).isEmpty();
        if (!dreadstone) {
            if (!level.isClientSide && player != null) {
                player.displayClientMessage(Component.translatable("message.dreadpatch.syndrigossa_egg.dreadstone"), true);
            }
            return InteractionResult.FAIL;
        }
        if (!level.isClientSide) {
            ItemStack stack = context.getItemInHand();
            SyndrigossaEggEntity egg = SyndrigossaEggEntity.lay(level, Vec3.atBottomCenterOf(pos.above()),
                    player != null ? player.getYRot() : 0.0F, player != null ? player.getUUID() : null);
            if (stack.has(DataComponents.CUSTOM_NAME)) {
                egg.setCustomName(stack.get(DataComponents.CUSTOM_NAME));
            }
            level.addFreshEntity(egg);
            level.playSound(null, pos.above(), SoundEvents.STONE_PLACE, SoundSource.BLOCKS, 1.0F, 0.8F);
            stack.consume(1, player);
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }
}
