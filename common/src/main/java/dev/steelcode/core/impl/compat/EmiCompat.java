// SPDX-License-Identifier: LGPL-3.0-or-later
package dev.steelcode.core.impl.compat;

import dev.emi.emi.api.EmiEntrypoint;
import dev.emi.emi.api.EmiPlugin;
import dev.emi.emi.api.EmiRegistry;
import dev.emi.emi.api.recipe.EmiInfoRecipe;
import dev.emi.emi.api.stack.EmiIngredient;
import dev.emi.emi.api.stack.EmiStack;
import dev.steelcode.core.SteelCodeCore;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.ApiStatus;

import java.util.List;

/** EMI plugin: info pages and hidden stacks. Found via {@code @EmiEntrypoint} on NeoForge and the "emi" entrypoint on Fabric. */
@EmiEntrypoint
@ApiStatus.Internal
public class EmiCompat implements EmiPlugin {
    @Override
    public void register(EmiRegistry registry) {
        ViewerData.info().forEach((stack, lines) -> {
            ResourceLocation item = BuiltInRegistries.ITEM.getKey(stack.getItem());
            ResourceLocation id = ResourceLocation.fromNamespaceAndPath(SteelCodeCore.MOD_ID,
                    "/info/" + item.getNamespace() + "/" + item.getPath());
            registry.addRecipe(new EmiInfoRecipe(List.<EmiIngredient>of(EmiStack.of(stack)), lines, id));
        });
        List<ItemStack> hidden = ViewerData.hidden();
        if (!hidden.isEmpty())
            registry.removeEmiStacks(s -> {
                ItemStack is = s.getItemStack();
                return !is.isEmpty() && hidden.stream().anyMatch(h -> h.is(is.getItem()));
            });
    }
}
