// SPDX-License-Identifier: LGPL-3.0-or-later
package dev.steelcode.core.impl.registry;

import dev.architectury.registry.registries.RegistrySupplier;
import dev.steelcode.core.api.block.Drops;
import dev.steelcode.core.api.block.RenderLayer;
import dev.steelcode.core.api.block.Textures;
import dev.steelcode.core.api.block.Variant;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import org.jetbrains.annotations.ApiStatus;

/**
 * What datagen needs to know about a registered block: shape, textures, drops and render layer.
 * Created by the builders.
 *
 * @param variant      model shape; {@code null} = hand-made model (not generated)
 * @param fullModel    full-block model (double slab / double vertical slab)
 * @param itemTexture  flat item texture (doors and panes); {@code null} if the item uses the block model
 * @param weighted     weighted pressure plate ({@code power} state instead of {@code powered})
 * @param tint         client color: {@link #NO_TINT}, {@link #FOLIAGE_TINT} or an RGB value
 * @param itemModel    custom item model, or {@code null} for the shape default
 * @param param        extra model data (crops: number of stage textures; max age in {@code param2})
 * @param noLoot       skip the loot table (wall signs drop the standing sign)
 */
@ApiStatus.Internal
public record BlockEntry(RegistrySupplier<Block> block, Variant variant, Textures textures, ResourceLocation fullModel,
                         ResourceLocation itemTexture, boolean weighted, Drops drops, RenderLayer renderLayer,
                         boolean hasItem, int tint, ItemModelSpec itemModel, int param, int param2, boolean noLoot) {

    /** Short constructor for the common case. */
    public BlockEntry(RegistrySupplier<Block> block, Variant variant, Textures textures, ResourceLocation fullModel,
                      ResourceLocation itemTexture, boolean weighted, Drops drops, RenderLayer renderLayer,
                      boolean hasItem, int tint) {
        this(block, variant, textures, fullModel, itemTexture, weighted, drops, renderLayer, hasItem, tint, null, 0, 0, false);
    }

    public static final int NO_TINT = 0;
    /** Biome foliage color (like oak leaves). */
    public static final int FOLIAGE_TINT = -1;

    public ResourceLocation id() {
        return block.getId();
    }
}
