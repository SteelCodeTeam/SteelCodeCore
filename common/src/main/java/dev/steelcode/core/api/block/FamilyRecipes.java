// SPDX-License-Identifier: LGPL-3.0-or-later
package dev.steelcode.core.api.block;

import dev.architectury.registry.registries.RegistrySupplier;
import dev.steelcode.core.api.data.Recipes;
import dev.steelcode.core.impl.registry.ShapeSupport;
import net.minecraft.world.level.block.Block;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

/**
 * Automatic recipes of a family, built from the {@link Recipes} templates. When two recipes collide (same pattern
 * and ingredients), the first one wins and the other becomes a stonecutter recipe from the base block.
 * {@code in} is the crafting-table ingredient (it may be an ingot); stonecutter and furnace recipes use the base.
 */
final class FamilyRecipes {

    private FamilyRecipes() {
    }

    static void generate(Recipes auto, Map<Variant, RegistrySupplier<Block>> blocks, Object in, Object baseBlock,
                         Object stick, boolean stonecutting, boolean wood) {
        Recipes r = auto.automaticWithFallback(baseBlock);
        RegistrySupplier<Block> slab = blocks.get(Variant.SLAB);

        // Doors and trapdoors first: if they collide with another shape, the vanilla recipe wins.
        List<Variant> order = new ArrayList<>(blocks.keySet());
        order.sort(Comparator.comparing(v -> v == Variant.DOOR || v == Variant.TRAPDOOR ? 0 : 1));
        for (Variant v : order) {
            RegistrySupplier<Block> out = blocks.get(v);
            if (v == Variant.BASE) continue;
            ShapeSupport.recipe(r, v, out, in, baseBlock, slab, stick, wood);
            int cut = ShapeSupport.stonecutterYield(v, stonecutting);
            if (cut > 0) r.stonecutting(baseBlock, out, cut);
        }
    }
}
