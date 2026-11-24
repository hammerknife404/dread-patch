package io.github.hammerknife404.dreadpatch.block;

import io.github.hammerknife404.dreadpatch.registry.DreadForge;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.FurnaceResultSlot;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/**
 * The Rimefrost dragonforge's menu (0.59.0): Ice and Fire's {@code DragonForgeMenu}'s slots where its
 * are (what goes in, its blood, what comes out, then the player's inventory), so its screen's art fits.
 * Shift-clicked in, blood goes to the blood slot (Ice and Fire's puts everything in the first).
 */
public class RimefrostForgeMenu extends AbstractContainerMenu {
    private static final int FORGE_SLOTS = 3;
    private static final int INVENTORY_END = FORGE_SLOTS + 27;
    private static final int HOTBAR_END = INVENTORY_END + 9;
    private final Container forge;
    private final ContainerData data;

    /** On the client: filled from the server's. */
    public RimefrostForgeMenu(int id, Inventory inventory) {
        this(id, inventory, new SimpleContainer(FORGE_SLOTS), new SimpleContainerData(2));
    }

    public RimefrostForgeMenu(int id, Inventory inventory, Container forge, ContainerData data) {
        super(DreadForge.MENU.get(), id);
        checkContainerSize(forge, FORGE_SLOTS);
        checkContainerDataCount(data, 2);
        this.forge = forge;
        this.data = data;
        this.addSlot(new Slot(forge, RimefrostForgeBlockEntity.INPUT, 68, 34));
        this.addSlot(new Slot(forge, RimefrostForgeBlockEntity.BLOOD, 86, 34));
        this.addSlot(new FurnaceResultSlot(inventory.player, forge, RimefrostForgeBlockEntity.OUTPUT, 148, 35));
        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < 9; column++) {
                this.addSlot(new Slot(inventory, column + row * 9 + 9, 8 + column * 18, 84 + row * 18));
            }
        }
        for (int column = 0; column < 9; column++) {
            this.addSlot(new Slot(inventory, column, 8 + column * 18, 142));
        }
        this.addDataSlots(data);
    }

    @Override
    public boolean stillValid(Player player) {
        return this.forge.stillValid(player);
    }

    /** The cook so far. */
    public int cookTime() {
        return this.data.get(0);
    }

    /** How long this cook takes: 0 with nothing to make. */
    public int cookFor() {
        return this.data.get(1);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        Slot slot = this.slots.get(index);
        if (!slot.hasItem()) {
            return ItemStack.EMPTY;
        }
        ItemStack stack = slot.getItem();
        ItemStack was = stack.copy();
        if (index == RimefrostForgeBlockEntity.OUTPUT) {
            if (!this.moveItemStackTo(stack, FORGE_SLOTS, HOTBAR_END, true)) {
                return ItemStack.EMPTY;
            }
            slot.onQuickCraft(stack, was);
        } else if (index < FORGE_SLOTS) {
            if (!this.moveItemStackTo(stack, FORGE_SLOTS, HOTBAR_END, false)) {
                return ItemStack.EMPTY;
            }
        } else {
            boolean blood = this.forge instanceof RimefrostForgeBlockEntity core ? core.isBlood(stack) : false;
            boolean moved = blood
                    ? this.moveItemStackTo(stack, RimefrostForgeBlockEntity.BLOOD, RimefrostForgeBlockEntity.BLOOD + 1, false)
                    : this.moveItemStackTo(stack, RimefrostForgeBlockEntity.INPUT, RimefrostForgeBlockEntity.INPUT + 1, false);
            if (!moved) {
                boolean fromInventory = index < INVENTORY_END;
                if (!this.moveItemStackTo(stack, fromInventory ? INVENTORY_END : FORGE_SLOTS, fromInventory ? HOTBAR_END : INVENTORY_END, false)) {
                    return ItemStack.EMPTY;
                }
            }
        }
        if (stack.isEmpty()) {
            slot.setByPlayer(ItemStack.EMPTY);
        } else {
            slot.setChanged();
        }
        if (stack.getCount() == was.getCount()) {
            return ItemStack.EMPTY;
        }
        slot.onTake(player, stack);
        return was;
    }
}
