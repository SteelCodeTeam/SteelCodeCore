// SPDX-License-Identifier: LGPL-3.0-or-later
package dev.steelcode.core.impl.registry;

import com.google.gson.JsonObject;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.ApiStatus;

import java.util.function.Supplier;

/** An advancement waiting to be written by datagen. */
@ApiStatus.Internal
public record AdvancementEntry(ResourceLocation id, Supplier<JsonObject> json) {
}
