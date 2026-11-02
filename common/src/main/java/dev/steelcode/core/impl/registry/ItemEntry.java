// SPDX-License-Identifier: LGPL-3.0-or-later
package dev.steelcode.core.impl.registry;

import dev.architectury.registry.registries.RegistrySupplier;
import dev.steelcode.core.api.item.ItemBuilder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import org.jetbrains.annotations.ApiStatus;

/**
 * Datagen data of a single item.
 *
 * @param custom custom model ({@link ItemBuilder.Model#CUSTOM}), or {@code null}
 */
@ApiStatus.Internal
public record ItemEntry(RegistrySupplier<Item> item, ItemBuilder.Model model, ResourceLocation texture, ItemModelSpec custom) {
    public ResourceLocation id() {
        return item.getId();
    }
}
