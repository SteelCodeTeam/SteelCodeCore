// SPDX-License-Identifier: LGPL-3.0-or-later
package dev.steelcode.core.impl.registry;

import com.google.gson.JsonObject;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.ApiStatus;

import java.util.function.Supplier;

/** An enchantment waiting to be written by datagen. */
@ApiStatus.Internal
public record EnchantmentEntry(ResourceLocation id, Supplier<JsonObject> json) {
}
