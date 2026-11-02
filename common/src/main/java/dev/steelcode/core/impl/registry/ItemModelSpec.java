// SPDX-License-Identifier: LGPL-3.0-or-later
package dev.steelcode.core.impl.registry;

import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.ApiStatus;

/**
 * A custom item model.
 *
 * @param parent  parent model ({@code minecraft:item/generated}, {@code steelcodecore:item/handheld_large}, a block model...)
 * @param texture {@code layer0} texture, or {@code null} to inherit the parent only (e.g. a 3D block model)
 */
@ApiStatus.Internal
public record ItemModelSpec(ResourceLocation parent, ResourceLocation texture) {

    /** A flat item with this texture. */
    public static ItemModelSpec flat(ResourceLocation texture) {
        return new ItemModelSpec(ResourceLocation.withDefaultNamespace("item/generated"), texture);
    }

    /** Inherits a block or item model unchanged, so a door or pane looks 3D in hand. */
    public static ItemModelSpec model(ResourceLocation parent) {
        return new ItemModelSpec(parent, null);
    }
}
