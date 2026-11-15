package io.github.hammerknife404.dreadpatch.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.client.extensions.common.IClientItemExtensions;
import org.jetbrains.annotations.Nullable;

/**
 * Her sword's Fire-Drinking Guard, drawn: a sword's use has no pose of its own, so while it's raised
 * the arm takes a shield's blocking pose (third person) and, in first person, the blade turns across
 * the body as swords blocked in old versions of the game.
 */
final class SwordGuardPose implements IClientItemExtensions {
    @Override
    @Nullable
    public HumanoidModel.ArmPose getArmPose(LivingEntity entity, InteractionHand hand, ItemStack stack) {
        return entity.isUsingItem() && entity.getUsedItemHand() == hand ? HumanoidModel.ArmPose.BLOCK : null;
    }

    @Override
    public boolean applyForgeHandTransform(PoseStack pose, LocalPlayer player, HumanoidArm arm, ItemStack stack,
                                           float partialTick, float equipProcess, float swingProcess) {
        if (!player.isUsingItem() || usedArm(player) != arm) {
            return false;
        }
        int side = arm == HumanoidArm.RIGHT ? 1 : -1;
        // Vanilla's item-in-hand placement, then the blade turned across the body, edge out.
        pose.translate(side * 0.56F, -0.52F + equipProcess * -0.6F, -0.72F);
        pose.translate(side * -0.14142136F, 0.08F, 0.14142136F);
        pose.mulPose(Axis.XP.rotationDegrees(-102.25F));
        pose.mulPose(Axis.YP.rotationDegrees(side * 13.365F));
        pose.mulPose(Axis.ZP.rotationDegrees(side * 78.05F));
        return true;
    }

    private static HumanoidArm usedArm(LocalPlayer player) {
        return player.getUsedItemHand() == InteractionHand.MAIN_HAND ? player.getMainArm() : player.getMainArm().getOpposite();
    }
}
