package io.github.hammerknife404.dreadpatch.compat.jei;

import com.iafenvoy.iceandfire.recipe.DragonForgeRecipe;
import io.github.hammerknife404.dreadpatch.DreadPatch;
import io.github.hammerknife404.dreadpatch.client.DreadRecolors;
import io.github.hammerknife404.dreadpatch.registry.DreadForge;
import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.category.AbstractRecipeCategory;
import net.minecraft.Util;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

/**
 * The Rimefrost dragonforge's JEI page (0.59.0), laid out as Ice and Fire's forge pages are (its
 * {@code DragonForgeRecipeCategory}: the screen's top, 170×79, its breath filling over 50 ticks), in
 * the forge's recoloured screen.
 */
final class RimefrostForgeCategory extends AbstractRecipeCategory<DragonForgeRecipe> {
    static final RecipeType<DragonForgeRecipe> TYPE = RecipeType.create(DreadPatch.MODID, "rimefrost_dragonforge", DragonForgeRecipe.class);
    private static final int WIDTH = 170;
    private static final int HEIGHT = 79;
    /** Ice and Fire's: the breath fills in 50 ticks. */
    private static final int FILL_MILLIS = 50 * 50;

    RimefrostForgeCategory(IGuiHelper gui) {
        super(TYPE, Component.translatable("jei.dreadpatch.rimefrost_forge"),
                gui.createDrawableIngredient(VanillaTypes.ITEM_STACK, new ItemStack(DreadForge.CORE_ITEM.get())), WIDTH, HEIGHT);
    }

    @Override
    public void setRecipe(IRecipeLayoutBuilder builder, DragonForgeRecipe recipe, IFocusGroup focuses) {
        builder.addSlot(RecipeIngredientRole.INPUT, 65, 30).addIngredients(recipe.getInput()).setStandardSlotBackground();
        builder.addSlot(RecipeIngredientRole.INPUT, 83, 30).addIngredients(recipe.getBlood()).setStandardSlotBackground();
        builder.addSlot(RecipeIngredientRole.OUTPUT, 145, 31).addItemStack(recipe.getResultItem());
    }

    @Override
    public void draw(DragonForgeRecipe recipe, IRecipeSlotsView slots, GuiGraphics graphics, double mouseX, double mouseY) {
        ResourceLocation texture = DreadRecolors.forgeScreen();
        graphics.blit(texture, 0, 0, 3, 4, WIDTH, HEIGHT);
        int filled = (int) (126 * (Util.getMillis() % FILL_MILLIS) / FILL_MILLIS);
        graphics.blit(texture, 9, 19, 0, 166, filled, 38);
    }
}
