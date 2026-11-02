// SPDX-License-Identifier: LGPL-3.0-or-later
package dev.steelcode.core.impl.compat;

import dev.steelcode.core.SteelCodeCore;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.registration.IRecipeRegistration;
import mezz.jei.api.runtime.IJeiRuntime;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.ApiStatus;

import java.util.List;

/** JEI plugin: info pages and hidden items. Found via {@code @JeiPlugin} on NeoForge and the "jei_mod_plugin" entrypoint on Fabric. */
@JeiPlugin
@ApiStatus.Internal
public class JeiCompat implements IModPlugin {
    @Override
    public ResourceLocation getPluginUid() {
        return ResourceLocation.fromNamespaceAndPath(SteelCodeCore.MOD_ID, "jei");
    }

    @Override
    public void registerRecipes(IRecipeRegistration registration) {
        ViewerData.info().forEach((stack, lines) ->
                registration.addItemStackInfo(stack, lines.toArray(new Component[0])));
    }

    @Override
    public void onRuntimeAvailable(IJeiRuntime runtime) {
        List<ItemStack> hidden = ViewerData.hidden();
        if (!hidden.isEmpty())
            runtime.getIngredientManager().removeIngredientsAtRuntime(VanillaTypes.ITEM_STACK, hidden);
    }
}
