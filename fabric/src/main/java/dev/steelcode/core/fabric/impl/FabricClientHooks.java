// SPDX-License-Identifier: LGPL-3.0-or-later
package dev.steelcode.core.fabric.impl;

import dev.steelcode.core.impl.platform.CorePlatform;
import net.fabricmc.fabric.api.client.rendering.v1.BuiltinItemRendererRegistry;
import net.minecraft.world.item.Item;
import org.jetbrains.annotations.ApiStatus;

import java.util.function.Supplier;

/** Fabric client hooks; only loaded on the client. */
@ApiStatus.Internal
public final class FabricClientHooks {

    private FabricClientHooks() {
    }

    static void itemRenderer(Supplier<? extends Item> item, CorePlatform.ItemRenderCallback renderer) {
        BuiltinItemRendererRegistry.INSTANCE.register(item.get(), renderer::render);
    }
}
