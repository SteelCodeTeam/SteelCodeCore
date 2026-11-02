// SPDX-License-Identifier: LGPL-3.0-or-later
package dev.steelcode.core.api.block;

import dev.architectury.registry.registries.RegistrySupplier;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.block.Block;

import java.util.Map;
import java.util.Optional;

/**
 * The colored blocks created by {@link ColorSetBuilder}.
 * @param name     base name, e.g. {@code "marble"}
 * @param blocks   one block per color created
 * @param families one family per color, if {@link ColorSetBuilder#family} was used
 * @param tag      block and item tag {@code <modid>:<name>} containing every color
 */
public record ColorSet(String name, Map<DyeColor, RegistrySupplier<Block>> blocks, Map<DyeColor, BlockFamily> families,
                       ResourceLocation tag) {

    /**
     * @param color a dye color
     * @return the block of that color
     * @throws IllegalStateException if the set does not contain that color
     */
    public RegistrySupplier<Block> get(DyeColor color) {
        RegistrySupplier<Block> b = blocks.get(color);
        if (b == null) throw new IllegalStateException("Color set '" + name + "' has no " + color + " block");
        return b;
    }

    /**
     * @param color a dye color
     * @return the family of that color, if families were created
     */
    public Optional<BlockFamily> family(DyeColor color) {
        return Optional.ofNullable(families.get(color));
    }
}
