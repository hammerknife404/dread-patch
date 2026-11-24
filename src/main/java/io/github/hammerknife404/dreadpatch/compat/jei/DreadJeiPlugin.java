package io.github.hammerknife404.dreadpatch.compat.jei;

import com.iafenvoy.iceandfire.recipe.DragonForgeRecipe;
import com.iafenvoy.iceandfire.registry.IafRecipes;
import io.github.hammerknife404.dreadpatch.DreadPatch;
import io.github.hammerknife404.dreadpatch.block.RimefrostForgeBlockEntity;
import io.github.hammerknife404.dreadpatch.registry.DreadForge;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.registration.IRecipeCatalystRegistration;
import mezz.jei.api.registration.IRecipeCategoryRegistration;
import mezz.jei.api.registration.IRecipeRegistration;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.crafting.RecipeHolder;

import java.util.List;

/**
 * JEI (0.59.0, in the owner's pack; never required): the Rimefrost dragonforge's recipes on a page of
 * their own, its core their catalyst. Ice and Fire's JEI pages sort its forge recipes by their
 * {@code dragonType}, fire, ice or lightning, so a {@code rimefrost} one shows on none of them.
 */
@JeiPlugin
public final class DreadJeiPlugin implements IModPlugin {
    @Override
    public ResourceLocation getPluginUid() {
        return DreadPatch.id("jei");
    }

    @Override
    public void registerCategories(IRecipeCategoryRegistration registration) {
        registration.addRecipeCategories(new RimefrostForgeCategory(registration.getJeiHelpers().getGuiHelper()));
    }

    @Override
    public void registerRecipes(IRecipeRegistration registration) {
        if (Minecraft.getInstance().level == null) {
            return;
        }
        List<DragonForgeRecipe> recipes = Minecraft.getInstance().level.getRecipeManager().getAllRecipesFor(IafRecipes.DRAGON_FORGE_TYPE.get()).stream()
                .map(RecipeHolder::value)
                .filter(recipe -> RimefrostForgeBlockEntity.DRAGON_TYPE.equals(recipe.getDragonType()))
                .toList();
        registration.addRecipes(RimefrostForgeCategory.TYPE, recipes);
    }

    @Override
    public void registerRecipeCatalysts(IRecipeCatalystRegistration registration) {
        registration.addRecipeCatalyst(DreadForge.CORE_ITEM.get(), RimefrostForgeCategory.TYPE);
    }
}
