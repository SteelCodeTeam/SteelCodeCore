// SPDX-License-Identifier: LGPL-3.0-or-later
package dev.steelcode.core.api.block;

import java.util.Objects;

/**
 * What a block drops when broken. Immutable: every modifier returns a new instance.
 * <p>
 * Items are flexible references: an {@code ItemLike}, a supplier, or an id string.
 *
 * <pre>{@code
 * Drops.self()                                    // drops itself (the default)
 * Drops.none()                                    // drops nothing
 * Drops.silkTouchOnly()                           // like glass
 * Drops.ore(RUBY)                                 // Silk Touch -> the block; otherwise RUBY, with Fortune
 * Drops.ore(RAW_TIN).count(1, 3)                  // like copper ore
 * Drops.item(Items.COBBLESTONE).withSilkTouch(Drops.SELF)
 * Drops.shearsOnly()                              // like grass
 * }</pre>
 *
 * @see BlockBuilder#drops(Drops)
 */
public final class Drops {

    /** Marker meaning "the block itself", usable wherever an item is expected. */
    public static final Object SELF = new Object() {
        @Override
        public String toString() {
            return "SELF";
        }
    };

    private final boolean shearsOnly;
    private final String vanillaTable;
    private final Object[] replacements;
    private final Object withoutSilk;
    private final Object withSilk;
    private final float min;
    private final float max;
    private final boolean fortune;

    private Drops(boolean shearsOnly, String vanillaTable, Object[] replacements, Object withoutSilk, Object withSilk,
                  float min, float max, boolean fortune) {
        this.shearsOnly = shearsOnly;
        this.vanillaTable = vanillaTable;
        this.replacements = replacements;
        this.withoutSilk = withoutSilk;
        this.withSilk = withSilk;
        this.min = min;
        this.max = max;
        this.fortune = fortune;
    }

    private static Drops simple(Object withoutSilk, Object withSilk, boolean fortune) {
        return new Drops(false, null, null, withoutSilk, withSilk, 1, 1, fortune);
    }

    private Drops copy(Object withoutSilk, Object withSilk, float min, float max, boolean fortune) {
        return new Drops(shearsOnly, vanillaTable, replacements, withoutSilk, withSilk, min, max, fortune);
    }

    /** @return a rule dropping the block itself, with or without Silk Touch */
    public static Drops self() {
        return simple(SELF, SELF, false);
    }

    /** @return a rule dropping nothing */
    public static Drops none() {
        return simple(null, null, false);
    }

    /** @return a rule dropping the block only with Silk Touch, like glass */
    public static Drops silkTouchOnly() {
        return simple(null, SELF, false);
    }

    /**
     * Always drops {@code item}, with or without Silk Touch. Chain {@link #withSilkTouch} to change the Silk Touch drop.
     *
     * @param item item, supplier or id
     * @return the rule
     */
    public static Drops item(Object item) {
        return simple(Objects.requireNonNull(item), item, false);
    }

    /**
     * The ore pattern: the block with Silk Touch, otherwise {@code item} multiplied by Fortune.
     *
     * @param item item, supplier or id
     * @return the rule
     */
    public static Drops ore(Object item) {
        return simple(Objects.requireNonNull(item), SELF, true);
    }

    /**
     * Copies a vanilla loot table, replacing ids. Useful for complex tables (leaves with saplings and sticks,
     * flower pots...). Datagen reads the table from the Minecraft jar itself.
     * <pre>{@code
     * Drops.likeVanilla("oak_leaves", "minecraft:oak_leaves", Drops.SELF, "minecraft:oak_sapling", MAPLE_SAPLING)
     * }</pre>
     *
     * @param table        vanilla block loot table path, e.g. {@code "oak_leaves"}
     * @param replacements pairs of {@code "minecraft:id"} and the replacement ({@link #SELF}, an item or id)
     * @return the rule
     * @throws IllegalArgumentException if replacements are not in pairs
     */
    public static Drops likeVanilla(String table, Object... replacements) {
        if (replacements.length % 2 != 0) {
            throw new IllegalArgumentException("Replacements must be pairs of vanilla id -> replacement");
        }
        return new Drops(false, Objects.requireNonNull(table), replacements.clone(), SELF, SELF, 1, 1, false);
    }

    /** @return a rule dropping the block only when broken with shears (grass, bushes) */
    public static Drops shearsOnly() {
        return new Drops(true, null, null, SELF, SELF, 1, 1, false);
    }

    /**
     * @param item the Silk Touch drop: {@link #SELF}, an item, or {@code null} for nothing
     * @return a copy with that Silk Touch drop
     */
    public Drops withSilkTouch(Object item) {
        return copy(withoutSilk, item, min, max, fortune);
    }

    /**
     * @param item the drop without Silk Touch: {@link #SELF}, an item, or {@code null} for nothing
     * @return a copy with that drop
     */
    public Drops withoutSilkTouch(Object item) {
        return copy(item, withSilk, min, max, fortune);
    }

    /**
     * Random count for the drop without Silk Touch.
     *
     * @param min minimum count
     * @param max maximum count
     * @return a copy with that count
     */
    public Drops count(float min, float max) {
        return copy(withoutSilk, withSilk, min, max, fortune);
    }

    /**
     * Fixed count for the drop without Silk Touch.
     *
     * @param count the count
     * @return a copy with that count
     */
    public Drops count(int count) {
        return count(count, count);
    }

    /**
     * @param fortune whether Fortune multiplies the drop without Silk Touch
     * @return a copy with that setting
     */
    public Drops fortune(boolean fortune) {
        return copy(withoutSilk, withSilk, min, max, fortune);
    }

    /** @return whether only shears collect the block */
    public boolean isShearsOnly() {
        return shearsOnly;
    }

    /** @return the vanilla table to copy, or {@code null} */
    public String vanillaTable() {
        return vanillaTable;
    }

    /** @return a copy of the {@code "minecraft:id", replacement} pairs (empty if none) */
    public Object[] replacements() {
        return replacements == null ? new Object[0] : replacements.clone();
    }

    /** @return the drop without Silk Touch ({@link #SELF}, an item reference or {@code null}) */
    public Object withoutSilk() {
        return withoutSilk;
    }

    /** @return the drop with Silk Touch ({@link #SELF}, an item reference or {@code null}) */
    public Object withSilk() {
        return withSilk;
    }

    /** @return minimum count of the drop without Silk Touch */
    public float min() {
        return min;
    }

    /** @return maximum count of the drop without Silk Touch */
    public float max() {
        return max;
    }

    /** @return whether Fortune applies */
    public boolean fortune() {
        return fortune;
    }

    /** @return whether this is exactly {@link #self()} */
    public boolean isSimpleSelf() {
        return !shearsOnly && vanillaTable == null && withoutSilk == SELF && withSilk == SELF
                && min == 1 && max == 1 && !fortune;
    }
}
