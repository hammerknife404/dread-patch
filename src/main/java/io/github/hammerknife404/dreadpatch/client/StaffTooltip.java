package io.github.hammerknife404.dreadpatch.client;

import io.github.hammerknife404.dreadpatch.item.DreadQueenStaff;
import io.redspace.ironsspellbooks.api.spells.CastSource;
import io.redspace.ironsspellbooks.api.spells.ISpellContainer;
import io.redspace.ironsspellbooks.util.TooltipsUtils;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;

import java.util.ArrayList;
import java.util.List;

/**
 * Her staff's spells in its tooltip, kept short (0.50.0; the owner: long spell lists "stay readable and
 * do not clip or overflow off-screen"). With up to three spells Iron's writes each one out in full (a
 * blank line, its name, its details, its cast time, mana and cooldown), which ran her three past the
 * bottom of the screen. Here: one line a spell, its name and level; holding Shift, each spell's
 * details under its name, with no blank lines between. Only her staff; Iron's own items as Iron's has them.
 */
public final class StaffTooltip {
    private static final String HEADER_PLURAL = "tooltip.irons_spellbooks.imbued_tooltip_plural";
    private static final String HEADER = "tooltip.irons_spellbooks.imbued_tooltip";

    private StaffTooltip() {
    }

    public static void register(IEventBus gameBus) {
        // After Iron's has written its own.
        gameBus.addListener(EventPriority.LOWEST, ItemTooltipEvent.class, StaffTooltip::onTooltip);
    }

    private static void onTooltip(ItemTooltipEvent event) {
        ItemStack stack = event.getItemStack();
        if (!DreadQueenStaff.is(stack) || !ISpellContainer.isSpellContainer(stack) || Minecraft.getInstance().player == null) {
            return;
        }
        ISpellContainer container = ISpellContainer.get(stack);
        if (container.isEmpty() || container.getActiveSpellCount() > 3) {
            // Past three, Iron's already lists them a line each.
            return;
        }
        List<Component> lines = event.getToolTip();
        int header = indexOfHeader(lines);
        if (header < 1) {
            return;
        }
        // Iron's block: the first spell's blank line, the header, then every spell's lines.
        List<List<MutableComponent>> spells = new ArrayList<>();
        int length = 1;
        for (var slot : container.getActiveSpells()) {
            List<MutableComponent> spell = TooltipsUtils.formatActiveSpellTooltip(stack, slot.spellData(), CastSource.SWORD, Minecraft.getInstance().player);
            spells.add(spell);
            length += spell.size();
        }
        int start = header - 1;
        if (start + length > lines.size() || !lines.get(start).getString().isEmpty()) {
            return;
        }
        List<Component> compact = new ArrayList<>();
        compact.add(Component.empty());
        compact.add(lines.get(header));
        boolean details = Screen.hasShiftDown();
        for (List<MutableComponent> spell : spells) {
            // Its blank line goes; its name stays, its details only with Shift.
            compact.add(Component.literal(" ").append(spell.get(1)));
            if (details) {
                for (int i = 2; i < spell.size(); i++) {
                    compact.add(Component.literal("  ").append(spell.get(i)));
                }
            }
        }
        if (!details) {
            compact.add(Component.translatable("tooltip.dreadpatch.spell_details").withStyle(ChatFormatting.DARK_GRAY));
        }
        lines.subList(start, start + length).clear();
        lines.addAll(start, compact);
    }

    private static int indexOfHeader(List<Component> lines) {
        for (int i = 0; i < lines.size(); i++) {
            if (lines.get(i).getContents() instanceof TranslatableContents contents
                    && (HEADER_PLURAL.equals(contents.getKey()) || HEADER.equals(contents.getKey()))) {
                return i;
            }
        }
        return -1;
    }
}
