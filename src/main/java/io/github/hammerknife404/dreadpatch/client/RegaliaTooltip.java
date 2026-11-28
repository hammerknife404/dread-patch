package io.github.hammerknife404.dreadpatch.client;

import io.github.hammerknife404.dreadpatch.item.DreadRegalia;
import io.github.hammerknife404.dreadpatch.magic.RegaliaBonuses;
import io.github.hammerknife404.dreadpatch.registry.DreadGear;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;

import java.util.List;

/**
 * Her regalia's innate powers in its tooltip (0.98.5, the owner: "Make sure the Regalia pieces also show their innate
 * passive abilities too, Regalia gives innate freeze immunity, and keep text scaling on tooltips in mind"). Its
 * attributes (mana, the mage buffs) Iron's and vanilla already list; these are the rest: each piece's mana and Rimefrost
 * damage, the full set's (freeze immunity, the Dread's truce, the weather), the crown's sight. Kept short for big GUI
 * scales, as her staff's tooltip is ({@link StaffTooltip}): one line ("Hold Shift") until Shift is held, then a
 * short line each, the set's with how many the player wears.
 */
public final class RegaliaTooltip {
    private RegaliaTooltip() {
    }

    public static void register(IEventBus gameBus) {
        gameBus.addListener(RegaliaTooltip::onTooltip);
    }

    private static void onTooltip(ItemTooltipEvent event) {
        ItemStack stack = event.getItemStack();
        if (!DreadRegalia.isRegalia(stack)) {
            return;
        }
        List<Component> lines = event.getToolTip();
        if (!Screen.hasShiftDown()) {
            lines.add(Component.translatable("tooltip.dreadpatch.regalia.shift").withStyle(ChatFormatting.DARK_GRAY));
            return;
        }
        int percent = Math.round(RegaliaBonuses.MANA_COST_PER_PIECE * 100);
        int damage = Math.round(RegaliaBonuses.RIMEFROST_DAMAGE_PER_PIECE * 100);
        lines.add(Component.translatable("tooltip.dreadpatch.regalia.piece").withStyle(ChatFormatting.GRAY));
        lines.add(power("tooltip.dreadpatch.regalia.mana", percent));
        lines.add(power("tooltip.dreadpatch.regalia.dealt", damage));
        lines.add(power("tooltip.dreadpatch.regalia.taken", damage));
        int worn = Minecraft.getInstance().player == null ? 0 : DreadRegalia.pieces(Minecraft.getInstance().player);
        lines.add(Component.translatable("tooltip.dreadpatch.regalia.set", worn, RegaliaBonuses.PIECES)
                .withStyle(RegaliaBonuses.fullSet(worn) ? ChatFormatting.GOLD : ChatFormatting.GRAY));
        lines.add(power("tooltip.dreadpatch.regalia.freeze"));
        lines.add(power("tooltip.dreadpatch.regalia.truce"));
        lines.add(power("tooltip.dreadpatch.regalia.weather"));
        if (stack.is(DreadGear.DREAD_QUEEN_CROWN.get())) {
            lines.add(Component.translatable("tooltip.dreadpatch.regalia.crown").withStyle(ChatFormatting.GRAY));
            lines.add(power("tooltip.dreadpatch.regalia.sight"));
        }
    }

    private static Component power(String key, Object... args) {
        return Component.literal(" ").append(Component.translatable(key, args)).withStyle(ChatFormatting.DARK_AQUA);
    }
}
