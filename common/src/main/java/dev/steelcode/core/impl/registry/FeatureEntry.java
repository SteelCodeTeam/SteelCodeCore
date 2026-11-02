// SPDX-License-Identifier: LGPL-3.0-or-later
package dev.steelcode.core.impl.registry;

import com.google.gson.JsonObject;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.levelgen.GenerationStep;
import org.jetbrains.annotations.ApiStatus;

import java.util.function.BooleanSupplier;
import java.util.function.Supplier;

/**
 * A worldgen feature: its configured feature, its placed feature (same id) and where it is injected.
 *
 * @param placed  {@code null} if only the configured feature exists (e.g. a sapling tree that does not generate)
 * @param biomes  biome tag, or {@code null} to not inject it
 * @param enabled checked at startup; {@code false} = not injected (handy with configs)
 */
@ApiStatus.Internal
public record FeatureEntry(ResourceLocation id, Supplier<JsonObject> configured, Supplier<JsonObject> placed,
                           ResourceLocation biomes, GenerationStep.Decoration step, BooleanSupplier enabled) {

    public FeatureEntry(ResourceLocation id, Supplier<JsonObject> configured, Supplier<JsonObject> placed,
                        ResourceLocation biomes, GenerationStep.Decoration step) {
        this(id, configured, placed, biomes, step, () -> true);
    }
}
