package io.github.hammerknife404.dreadpatch.block;

import com.iafenvoy.iceandfire.recipe.DragonForgeRecipe;
import com.iafenvoy.iceandfire.registry.IafBlocks;
import com.iafenvoy.iceandfire.registry.IafRecipes;
import io.github.hammerknife404.dreadpatch.magic.ForgeRules;
import io.github.hammerknife404.dreadpatch.registry.DreadForge;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BaseContainerBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

/**
 * The Rimefrost dragonforge (0.59.0), Ice and Fire's dragonforge redone for the Rimefrost: Ice and
 * Fire's own can't be reused (its core picks its recipes by checking for its own three core blocks,
 * any other reading as "fire"; its block entities are bound to its own types; its screen needs a
 * registered dragon type).
 * <ul>
 * <li>Three slots, as Ice and Fire's: what goes in (0), its blood (1), what comes out (2). Hoppers put
 * in from above, blood from the sides, and take out from below.</li>
 * <li>It makes Ice and Fire's {@code iceandfire:dragonforge} recipes of {@code "dragonType": "rimefrost"}
 * (ours: Rimefrost dragon blood, dread dragonsteel; packs can add their own), and nothing else: Ice and
 * Fire's makes dragon ice or ash of anything it has no recipe for.</li>
 * <li>Only while it stands whole ({@link ForgeRules#assembled}: its bricks, Ice and Fire's dragon bone
 * blocks on the corners above and below), and only lit by a Rimefrost dragon's breath on its aperture
 * ({@link #breathe}); its rules are {@link ForgeRules}'s.</li>
 * </ul>
 */
public class RimefrostForgeBlockEntity extends BaseContainerBlockEntity implements WorldlyContainer {
    /** The forge recipes it makes: {@code "dragonType": "rimefrost"}. */
    public static final String DRAGON_TYPE = "rimefrost";
    public static final int INPUT = 0;
    public static final int BLOOD = 1;
    public static final int OUTPUT = 2;
    private static final int[] TOP = {INPUT};
    private static final int[] SIDES = {BLOOD};
    private static final int[] BOTTOM = {OUTPUT};

    private NonNullList<ItemStack> items = NonNullList.withSize(3, ItemStack.EMPTY);
    private double cookTime;
    private int litTicks;
    /** For the screen: the cook so far, and how long this one takes (0 with nothing to make). */
    private final ContainerData data = new ContainerData() {
        @Override
        public int get(int index) {
            return switch (index) {
                case 0 -> (int) RimefrostForgeBlockEntity.this.cookTime;
                case 1 -> RimefrostForgeBlockEntity.this.cookFor();
                default -> 0;
            };
        }

        @Override
        public void set(int index, int value) {
            if (index == 0) {
                RimefrostForgeBlockEntity.this.cookTime = value;
            }
        }

        @Override
        public int getCount() {
            return 2;
        }
    };

    public RimefrostForgeBlockEntity(BlockPos pos, BlockState state) {
        super(DreadForge.CORE_ENTITY.get(), pos, state);
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, RimefrostForgeBlockEntity forge) {
        if (forge.litTicks > 0) {
            forge.litTicks--;
        }
        boolean assembled = forge.assembled();
        if (state.getValue(RimefrostForgeCoreBlock.LIT) != assembled) {
            level.setBlock(pos, state.setValue(RimefrostForgeCoreBlock.LIT, assembled), Block.UPDATE_ALL);
        }
        forge.showGrills(assembled);
        if (!assembled) {
            // As Ice and Fire's: a broken forge holds its cook till it's whole again.
            return;
        }
        DragonForgeRecipe recipe = forge.recipe();
        boolean canMake = recipe != null && forge.fits(recipe);
        double before = forge.cookTime;
        forge.cookTime = ForgeRules.advance(forge.cookTime, forge.litTicks > 0, canMake);
        if (canMake && ForgeRules.done(forge.cookTime, recipe.getCookTime())) {
            forge.make(recipe);
            forge.cookTime = 0;
            forge.setChanged();
        } else if (before > 0 && forge.cookTime == 0) {
            forge.setChanged();
        }
    }

    /** Whether the cube round it is whole. */
    public boolean assembled() {
        if (this.level == null) {
            return false;
        }
        return ForgeRules.assembled((dx, dy, dz) -> part(this.level.getBlockState(this.worldPosition.offset(dx, dy, dz))));
    }

    private static ForgeRules.Part part(BlockState state) {
        if (state.is(DreadForge.BRICK.get())) {
            return ForgeRules.Part.BRICK;
        }
        return state.is(IafBlocks.DRAGON_BONE_BLOCK.get()) ? ForgeRules.Part.BONE : ForgeRules.Part.OTHER;
    }

    /** The grill on the bricks beside it while it's whole, as Ice and Fire's shows. */
    private void showGrills(boolean whole) {
        for (Direction side : Direction.Plane.HORIZONTAL) {
            BlockPos at = this.worldPosition.relative(side);
            BlockState state = this.level.getBlockState(at);
            if (state.is(DreadForge.BRICK.get()) && state.getValue(RimefrostForgeBrickBlock.GRILL) != whole) {
                this.level.setBlock(at, state.setValue(RimefrostForgeBrickBlock.GRILL, whole), Block.UPDATE_ALL);
            }
        }
    }

    /** A Rimefrost dragon's breath on its aperture: lit for a while, and its cook started ({@code power}, the dragon's forge speed). */
    public void breathe(double power) {
        DragonForgeRecipe recipe = this.recipe();
        boolean canMake = recipe != null && this.fits(recipe);
        this.cookTime = ForgeRules.heat(this.cookTime, power, canMake ? recipe.getCookTime() : 0, canMake);
        this.litTicks = ForgeRules.LIT_TICKS;
    }

    /** Whether it stands whole with something it can make: what draws a tame Rimefrost dragon to its aperture. */
    public boolean ready() {
        DragonForgeRecipe recipe = this.recipe();
        return recipe != null && this.fits(recipe) && this.assembled();
    }

    /** The Rimefrost forge recipe for what's in it, if any. */
    @Nullable
    public DragonForgeRecipe recipe() {
        ItemStack input = this.items.get(INPUT);
        if (this.level == null || input.isEmpty()) {
            return null;
        }
        ItemStack blood = this.items.get(BLOOD);
        for (RecipeHolder<DragonForgeRecipe> holder : this.level.getRecipeManager().getAllRecipesFor(IafRecipes.DRAGON_FORGE_TYPE.get())) {
            DragonForgeRecipe recipe = holder.value();
            if (DRAGON_TYPE.equals(recipe.getDragonType()) && recipe.isValidInput(input) && recipe.isValidBlood(blood)) {
                return recipe;
            }
        }
        return null;
    }

    /** Whether {@code stack} is the blood of any Rimefrost forge recipe (what its sides take). */
    public boolean isBlood(ItemStack stack) {
        if (this.level == null) {
            return false;
        }
        for (RecipeHolder<DragonForgeRecipe> holder : this.level.getRecipeManager().getAllRecipesFor(IafRecipes.DRAGON_FORGE_TYPE.get())) {
            if (DRAGON_TYPE.equals(holder.value().getDragonType()) && holder.value().isValidBlood(stack)) {
                return true;
            }
        }
        return false;
    }

    private int cookFor() {
        DragonForgeRecipe recipe = this.recipe();
        return recipe == null ? 0 : recipe.getCookTime();
    }

    private boolean fits(DragonForgeRecipe recipe) {
        ItemStack result = recipe.getResultItem();
        if (result.isEmpty()) {
            return false;
        }
        ItemStack out = this.items.get(OUTPUT);
        if (out.isEmpty()) {
            return true;
        }
        if (!ItemStack.isSameItemSameComponents(out, result)) {
            return false;
        }
        int count = out.getCount() + result.getCount();
        return count <= this.getMaxStackSize() && count <= out.getMaxStackSize();
    }

    private void make(DragonForgeRecipe recipe) {
        ItemStack result = recipe.getResultItem();
        ItemStack out = this.items.get(OUTPUT);
        ItemStack input = this.items.get(INPUT);
        if (out.isEmpty() && input.has(io.redspace.ironsspellbooks.registries.ComponentRegistry.SPELL_CONTAINER.get())) {
            // A spellbook forged (0.98.3, the Dread Sovereign's Tome) keeps what it held: its spells and anything else of it.
            this.items.set(OUTPUT, input.transmuteCopy(result.getItem(), result.getCount()));
        } else if (out.isEmpty()) {
            this.items.set(OUTPUT, result.copy());
        } else {
            out.grow(result.getCount());
        }
        this.items.get(INPUT).shrink(1);
        this.items.get(BLOOD).shrink(1);
    }

    @Override
    public void setItem(int slot, ItemStack stack) {
        ItemStack was = this.items.get(slot);
        boolean same = !stack.isEmpty() && ItemStack.isSameItemSameComponents(stack, was);
        super.setItem(slot, stack);
        if (slot == INPUT && !same) {
            this.cookTime = 0;
        }
    }

    @Override
    protected Component getDefaultName() {
        return Component.translatable("container.dreadpatch.rimefrost_forge");
    }

    @Override
    protected NonNullList<ItemStack> getItems() {
        return this.items;
    }

    @Override
    protected void setItems(NonNullList<ItemStack> items) {
        this.items = items;
    }

    @Override
    protected AbstractContainerMenu createMenu(int id, Inventory inventory) {
        return new RimefrostForgeMenu(id, inventory, this, this.data);
    }

    @Override
    public int getContainerSize() {
        return this.items.size();
    }

    @Override
    public boolean canPlaceItem(int slot, ItemStack stack) {
        return switch (slot) {
            case INPUT -> true;
            case BLOOD -> this.isBlood(stack);
            default -> false;
        };
    }

    @Override
    public int[] getSlotsForFace(Direction side) {
        return side == Direction.DOWN ? BOTTOM : side == Direction.UP ? TOP : SIDES;
    }

    @Override
    public boolean canPlaceItemThroughFace(int slot, ItemStack stack, @Nullable Direction side) {
        return this.canPlaceItem(slot, stack);
    }

    @Override
    public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction side) {
        return slot == OUTPUT;
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        this.items = NonNullList.withSize(this.getContainerSize(), ItemStack.EMPTY);
        ContainerHelper.loadAllItems(tag, this.items, registries);
        this.cookTime = tag.getDouble("CookTime");
        this.litTicks = tag.getInt("Lit");
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        ContainerHelper.saveAllItems(tag, this.items, registries);
        tag.putDouble("CookTime", this.cookTime);
        tag.putInt("Lit", this.litTicks);
    }
}
