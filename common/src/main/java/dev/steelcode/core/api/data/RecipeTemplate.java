// SPDX-License-Identifier: LGPL-3.0-or-later
package dev.steelcode.core.api.data;

import dev.steelcode.core.api.block.Variant;

import java.util.function.BiConsumer;

/**
 * A "default" recipe attached to a block or item while you build it; the result is that block or item.
 * <pre>{@code
 * REG.block("gold_stairs").shape(Variant.STAIRS, Blocks.GOLD_BLOCK)
 *         .factory(p -> new MyStairs(Blocks.GOLD_BLOCK.defaultBlockState(), p))
 *         .recipe(RecipeTemplate.stairs(Blocks.GOLD_BLOCK))
 *         .recipe(RecipeTemplate.stonecutting(Blocks.GOLD_BLOCK, 1))
 *         .build();
 *
 * REG.item("ruby_sword").handheld().recipe(RecipeTemplate.sword(RUBY)).build();
 * }</pre>
 * Each factory mirrors the {@link Recipes} method of the same name. For anything else:
 * {@code RecipeTemplate.of((recipes, self) -> recipes.shaped(self)...save())}.
 */
@FunctionalInterface
public interface RecipeTemplate {

    /**
     * Adds the recipe.
     * @param recipes the mod recipes
     * @param result  the block or item the template is attached to
     */
    void apply(Recipes recipes, Object result);

    /**
     * @param recipe any recipe code, receiving the recipes helper and the result
     * @return the template
     */
    static RecipeTemplate of(BiConsumer<Recipes, Object> recipe) {
        return recipe::accept;
    }

    // ---------------------------------------------------------------- blocks

    /**
     * @param ingredient the ingredient
     * @return {@link Recipes#stairs} as a template
     */
    static RecipeTemplate stairs(Object ingredient) { return (r, self) -> r.stairs(self, ingredient); }
    /**
     * @param ingredient the ingredient
     * @return {@link Recipes#slab} as a template
     */
    static RecipeTemplate slab(Object ingredient) { return (r, self) -> r.slab(self, ingredient); }
    /**
     * @param ingredient the ingredient
     * @return {@link Recipes#verticalSlab} as a template
     */
    static RecipeTemplate verticalSlab(Object ingredient) { return (r, self) -> r.verticalSlab(self, ingredient); }
    /**
     * @param slab the matching slab
     * @return {@link Recipes#verticalSlabFromSlab} as a template
     */
    static RecipeTemplate verticalSlabFromSlab(Object slab) { return (r, self) -> r.verticalSlabFromSlab(self, slab); }
    /**
     * @param ingredient the ingredient
     * @return {@link Recipes#wall} as a template
     */
    static RecipeTemplate wall(Object ingredient) { return (r, self) -> r.wall(self, ingredient); }
    /**
     * @param ingredient the ingredient
     * @return {@link Recipes#fence(Object, Object)} as a template
     */
    static RecipeTemplate fence(Object ingredient) { return (r, self) -> r.fence(self, ingredient); }
    /**
     * @param ingredient the ingredient
     * @param stick      the stick
     * @return {@link Recipes#fence(Object, Object, Object)} as a template
     */
    static RecipeTemplate fence(Object ingredient, Object stick) { return (r, self) -> r.fence(self, ingredient, stick); }
    /**
     * @param ingredient the ingredient
     * @return {@link Recipes#fenceGate(Object, Object)} as a template
     */
    static RecipeTemplate fenceGate(Object ingredient) { return (r, self) -> r.fenceGate(self, ingredient); }
    /**
     * @param ingredient the ingredient
     * @param stick      the stick
     * @return {@link Recipes#fenceGate(Object, Object, Object)} as a template
     */
    static RecipeTemplate fenceGate(Object ingredient, Object stick) { return (r, self) -> r.fenceGate(self, ingredient, stick); }
    /**
     * @param ingredient the ingredient
     * @return {@link Recipes#button} as a template
     */
    static RecipeTemplate button(Object ingredient) { return (r, self) -> r.button(self, ingredient); }
    /**
     * @param ingredient the ingredient
     * @return {@link Recipes#pressurePlate} as a template
     */
    static RecipeTemplate pressurePlate(Object ingredient) { return (r, self) -> r.pressurePlate(self, ingredient); }
    /**
     * @param ingredient the ingredient
     * @return {@link Recipes#door} as a template
     */
    static RecipeTemplate door(Object ingredient) { return (r, self) -> r.door(self, ingredient); }
    /**
     * @param ingredient the ingredient
     * @return {@link Recipes#trapdoor} as a template
     */
    static RecipeTemplate trapdoor(Object ingredient) { return (r, self) -> r.trapdoor(self, ingredient); }
    /**
     * @param ingredient the ingredient
     * @return {@link Recipes#woodenTrapdoor} as a template
     */
    static RecipeTemplate woodenTrapdoor(Object ingredient) { return (r, self) -> r.woodenTrapdoor(self, ingredient); }
    /**
     * @param ingredient the ingredient
     * @return {@link Recipes#pane} as a template
     */
    static RecipeTemplate pane(Object ingredient) { return (r, self) -> r.pane(self, ingredient); }
    /**
     * @param ingredient the ingredient
     * @return {@link Recipes#carpet} as a template
     */
    static RecipeTemplate carpet(Object ingredient) { return (r, self) -> r.carpet(self, ingredient); }
    /**
     * @param ingredient the ingredient
     * @return {@link Recipes#polished} as a template
     */
    static RecipeTemplate polished(Object ingredient) { return (r, self) -> r.polished(self, ingredient); }
    /**
     * @param ingredient the ingredient
     * @return {@link Recipes#pillar} as a template
     */
    static RecipeTemplate pillar(Object ingredient) { return (r, self) -> r.pillar(self, ingredient); }
    /**
     * @param slab the slab
     * @return {@link Recipes#chiseled} as a template
     */
    static RecipeTemplate chiseled(Object slab) { return (r, self) -> r.chiseled(self, slab); }
    /**
     * @param ingredient the ingredient
     * @return {@link Recipes#cracked} as a template
     */
    static RecipeTemplate cracked(Object ingredient) { return (r, self) -> r.cracked(self, ingredient); }
    /**
     * @param count  result count
     * @param center center ingredient
     * @param ring   surrounding ingredient
     * @return {@link Recipes#surround} as a template
     */
    static RecipeTemplate surround(int count, Object center, Object ring) { return (r, self) -> r.surround(self, count, center, ring); }

    /**
     * The block stores {@code item}: 9 items give the block and the block gives 9.
     * @param item the stored item
     * @return the template
     */
    static RecipeTemplate storageOf(Object item) { return (r, self) -> r.storage(self, item); }

    /**
     * The block is made from 4 {@code item} (like quartz).
     * @param item       the item
     * @param reversible whether the block can be crafted back
     * @return the template
     */
    static RecipeTemplate storage2x2Of(Object item, boolean reversible) {
        return (r, self) -> r.storage2x2(self, item, reversible);
    }

    // ---------------------------------------------------------------- items

    /**
     * The item is a nugget of {@code ingot}: 1 ingot gives 9 and 9 give 1 ingot.
     * @param ingot the ingot
     * @return the template
     */
    static RecipeTemplate nuggetOf(Object ingot) { return (r, self) -> r.nuggets(self, ingot); }

    /**
     * @param material the material
     * @return the vanilla sword recipe as a template
     */
    static RecipeTemplate sword(Object material) { return (r, self) -> r.sword(self, material); }
    /**
     * @param material the material
     * @return the vanilla pickaxe recipe as a template
     */
    static RecipeTemplate pickaxe(Object material) { return (r, self) -> r.pickaxe(self, material); }
    /**
     * @param material the material
     * @return the vanilla axe recipe as a template
     */
    static RecipeTemplate axe(Object material) { return (r, self) -> r.axe(self, material); }
    /**
     * @param material the material
     * @return the vanilla shovel recipe as a template
     */
    static RecipeTemplate shovel(Object material) { return (r, self) -> r.shovel(self, material); }
    /**
     * @param material the material
     * @return the vanilla hoe recipe as a template
     */
    static RecipeTemplate hoe(Object material) { return (r, self) -> r.hoe(self, material); }
    /**
     * @param material the material
     * @return the vanilla helmet recipe as a template
     */
    static RecipeTemplate helmet(Object material) { return (r, self) -> r.helmet(self, material); }
    /**
     * @param material the material
     * @return the vanilla chestplate recipe as a template
     */
    static RecipeTemplate chestplate(Object material) { return (r, self) -> r.chestplate(self, material); }
    /**
     * @param material the material
     * @return the vanilla leggings recipe as a template
     */
    static RecipeTemplate leggings(Object material) { return (r, self) -> r.leggings(self, material); }
    /**
     * @param material the material
     * @return the vanilla boots recipe as a template
     */
    static RecipeTemplate boots(Object material) { return (r, self) -> r.boots(self, material); }

    /**
     * Smithing-table upgrade: {@code template + base + addition} give this item.
     * @param template the smithing template
     * @param base     the upgraded item
     * @param addition the added material
     * @return the template
     */
    static RecipeTemplate smithing(Object template, Object base, Object addition) {
        return (r, self) -> r.smithing(template, base, addition, self);
    }

    // ---------------------------------------------------------------- furnaces and stonecutter

    /**
     * @param ingredient the input
     * @param xp         experience
     * @return {@link Recipes#smelting} as a template
     */
    static RecipeTemplate smelting(Object ingredient, float xp) { return (r, self) -> r.smelting(ingredient, self, xp); }
    /**
     * @param ingredient the input
     * @param xp         experience
     * @return {@link Recipes#oreSmelting} as a template
     */
    static RecipeTemplate oreSmelting(Object ingredient, float xp) { return (r, self) -> r.oreSmelting(ingredient, self, xp); }
    /**
     * @param raw the raw food
     * @param xp  experience
     * @return {@link Recipes#foodCooking} as a template
     */
    static RecipeTemplate foodCooking(Object raw, float xp) { return (r, self) -> r.foodCooking(raw, self, xp); }
    /**
     * @param ingredient the input
     * @param count      output count
     * @return {@link Recipes#stonecutting} as a template
     */
    static RecipeTemplate stonecutting(Object ingredient, int count) { return (r, self) -> r.stonecutting(ingredient, self, count); }
}
