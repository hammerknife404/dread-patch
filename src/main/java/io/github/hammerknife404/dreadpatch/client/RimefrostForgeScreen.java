package io.github.hammerknife404.dreadpatch.client;

import io.github.hammerknife404.dreadpatch.block.RimefrostForgeMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

/**
 * The Rimefrost dragonforge's screen (0.59.0): Ice and Fire's ice dragonforge screen, recoloured
 * ({@link DreadRecolors#forgeScreen}), its breath filling as the cook goes, as Ice and Fire's does.
 * Its labels are soot, the recoloured panel being darker than vanilla's grey.
 */
public class RimefrostForgeScreen extends AbstractContainerScreen<RimefrostForgeMenu> {
    /** The labels: her soot. */
    private static final int LABEL = 0x14161D;

    public RimefrostForgeScreen(RimefrostForgeMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
    }

    @Override
    protected void init() {
        super.init();
        this.titleLabelX = (this.imageWidth - this.font.width(this.title)) / 2;
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        this.renderTooltip(graphics, mouseX, mouseY);
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        var texture = DreadRecolors.forgeScreen();
        int x = (this.width - this.imageWidth) / 2;
        int y = (this.height - this.imageHeight) / 2;
        graphics.blit(texture, x, y, 0, 0, this.imageWidth, this.imageHeight);
        int cookFor = this.menu.cookFor();
        if (cookFor > 0) {
            int width = Math.min(125, 125 * this.menu.cookTime() / cookFor);
            graphics.blit(texture, x + 12, y + 23, 0, 166, width, 38);
        }
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        graphics.drawString(this.font, this.title, this.titleLabelX, this.titleLabelY, LABEL, false);
        graphics.drawString(this.font, this.playerInventoryTitle, this.inventoryLabelX, this.inventoryLabelY, LABEL, false);
    }
}
