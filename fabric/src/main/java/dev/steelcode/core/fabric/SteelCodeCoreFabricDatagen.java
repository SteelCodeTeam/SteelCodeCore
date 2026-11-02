// SPDX-License-Identifier: LGPL-3.0-or-later
package dev.steelcode.core.fabric;

import dev.steelcode.core.api.CoreRegistry;
import dev.steelcode.core.impl.datagen.CoreDataProvider;
import net.fabricmc.fabric.api.datagen.v1.FabricDataGenerator;

/**
 * Adds SteelCodeCore's data provider to Fabric datagen. Call it from your {@code DataGeneratorEntrypoint}
 * (declared as {@code "fabric-datagen"} in {@code fabric.mod.json}):
 * <pre>{@code
 * public final class MyDatagen implements DataGeneratorEntrypoint {
 *     @Override
 *     public void onInitializeDataGenerator(FabricDataGenerator generator) {
 *         SteelCodeCoreFabricDatagen.addProviders(generator.createPack(), MyMod.REG);
 *     }
 * }
 * }</pre>
 * The generated files are identical for both loaders, so one Fabric datagen run can feed the common resources.
 */
public final class SteelCodeCoreFabricDatagen {

    private SteelCodeCoreFabricDatagen() {
    }

    /**
     * Adds one data provider per registry to a pack.
     * @param pack       the datagen pack, usually {@code generator.createPack()}
     * @param registries the registries to generate
     */
    public static void addProviders(FabricDataGenerator.Pack pack, CoreRegistry... registries) {
        for (CoreRegistry registry : registries) {
            pack.addProvider((FabricDataGenerator.Pack.Factory<CoreDataProvider>)
                    output -> new CoreDataProvider(output, registry));
        }
    }
}
