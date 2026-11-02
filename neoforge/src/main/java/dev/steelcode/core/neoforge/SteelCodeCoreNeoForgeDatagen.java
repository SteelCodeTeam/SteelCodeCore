// SPDX-License-Identifier: LGPL-3.0-or-later
package dev.steelcode.core.neoforge;

import dev.steelcode.core.api.CoreRegistry;
import dev.steelcode.core.impl.datagen.CoreDataProvider;
import net.minecraft.data.DataProvider;
import net.neoforged.neoforge.data.event.GatherDataEvent;

/**
 * Adds SteelCodeCore's data provider to NeoForge datagen. Register it on your mod bus:
 * <pre>{@code
 * modBus.addListener((GatherDataEvent e) -> SteelCodeCoreNeoForgeDatagen.addProviders(e, MyMod.REG));
 * }</pre>
 */
public final class SteelCodeCoreNeoForgeDatagen {

    private SteelCodeCoreNeoForgeDatagen() {
    }

    /**
     * Adds one data provider per registry to the data generator.
     * @param event      the gather-data event
     * @param registries the registries to generate
     */
    public static void addProviders(GatherDataEvent event, CoreRegistry... registries) {
        boolean run = event.includeClient() || event.includeServer();
        for (CoreRegistry registry : registries) {
            event.getGenerator().addProvider(run,
                    (DataProvider.Factory<CoreDataProvider>) output -> new CoreDataProvider(output, registry));
        }
    }
}
