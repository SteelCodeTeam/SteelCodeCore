// SPDX-License-Identifier: LGPL-3.0-or-later
package dev.steelcode.core.fabric;

import dev.steelcode.core.impl.client.SteelCodeCoreClient;
import net.fabricmc.api.ClientModInitializer;

/**
 * Fabric client entrypoint of the library ({@code client}): late client registrations (render layers, item properties).
 */
public final class SteelCodeCoreFabricClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        SteelCodeCoreClient.init();
    }
}
