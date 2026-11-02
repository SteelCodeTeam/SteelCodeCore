// SPDX-License-Identifier: LGPL-3.0-or-later
package dev.steelcode.core.neoforge;

import dev.steelcode.core.SteelCodeCore;
import dev.steelcode.core.impl.client.SteelCodeCoreClient;
import dev.steelcode.core.impl.platform.CorePlatform;
import dev.steelcode.core.neoforge.impl.NeoForgeClientHooks;
import dev.steelcode.core.neoforge.impl.NeoForgeHooks;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.fml.loading.FMLEnvironment;

/**
 * NeoForge entrypoint of the library ({@code @Mod}). Mods do not call it; it installs the NeoForge platform hooks
 * and initializes SteelCodeCore.
 */
@Mod(SteelCodeCore.MOD_ID)
public final class SteelCodeCoreNeoForge {

    /**
     * Constructed by NeoForge.
     * @param modBus the library's mod event bus
     */
    public SteelCodeCoreNeoForge(IEventBus modBus) {
        NeoForgeHooks hooks = new NeoForgeHooks(modBus);
        CorePlatform.install(hooks);
        SteelCodeCore.init();
        if (FMLEnvironment.dist == Dist.CLIENT) {
            NeoForgeClientHooks.register(modBus, hooks);
            modBus.addListener((FMLClientSetupEvent event) -> event.enqueueWork(SteelCodeCoreClient::init));
        }
    }
}
