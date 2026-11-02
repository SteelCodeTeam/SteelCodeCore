// SPDX-License-Identifier: LGPL-3.0-or-later
package dev.steelcode.core.fabric;

import dev.steelcode.core.SteelCodeCore;
import dev.steelcode.core.fabric.impl.FabricHooks;
import dev.steelcode.core.impl.platform.CorePlatform;
import net.fabricmc.api.ModInitializer;

/**
 * Fabric entrypoint of the library ({@code main}). Mods do not call it; it installs the Fabric platform hooks and
 * initializes SteelCodeCore before dependent mods initialize.
 */
public final class SteelCodeCoreFabric implements ModInitializer {
    @Override
    public void onInitialize() {
        CorePlatform.install(new FabricHooks());
        SteelCodeCore.init();
    }
}
