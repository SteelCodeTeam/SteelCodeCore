// SPDX-License-Identifier: LGPL-3.0-or-later
package dev.steelcode.core.api.item;

/**
 * Armor statistics (a vanilla {@code ArmorMaterial}). Start from a vanilla preset and tweak it:
 * <pre>{@code ArmorStats.DIAMOND.withDurability(40).withToughness(2.5f)}</pre>
 * @param durability          durability multiplier (iron 15, diamond 33)
 * @param helmet              helmet defense (iron 2)
 * @param chestplate          chestplate defense (iron 6)
 * @param leggings            leggings defense (iron 5)
 * @param boots               boots defense (iron 2)
 * @param enchantability      enchantability (iron 9, gold 25)
 * @param toughness           toughness (diamond 2, netherite 3)
 * @param knockbackResistance knockback resistance (netherite 0.1)
 */
public record ArmorStats(int durability, int helmet, int chestplate, int leggings, int boots, int enchantability,
                         float toughness, float knockbackResistance) {

    /** Vanilla leather armor. */
    public static final ArmorStats LEATHER = new ArmorStats(5, 1, 3, 2, 1, 15, 0, 0);
    /** Vanilla chainmail armor. */
    public static final ArmorStats CHAIN = new ArmorStats(15, 2, 5, 4, 1, 12, 0, 0);
    /** Vanilla iron armor. */
    public static final ArmorStats IRON = new ArmorStats(15, 2, 6, 5, 2, 9, 0, 0);
    /** Vanilla golden armor. */
    public static final ArmorStats GOLD = new ArmorStats(7, 2, 5, 3, 1, 25, 0, 0);
    /** Vanilla diamond armor. */
    public static final ArmorStats DIAMOND = new ArmorStats(33, 3, 8, 6, 3, 10, 2, 0);
    /** Vanilla netherite armor. */
    public static final ArmorStats NETHERITE = new ArmorStats(37, 3, 8, 6, 3, 15, 3, 0.1F);

    /**
     * @param multiplier new durability multiplier
     * @return a copy with that value
     */
    public ArmorStats withDurability(int multiplier) {
        return new ArmorStats(multiplier, helmet, chestplate, leggings, boots, enchantability, toughness, knockbackResistance);
    }

    /**
     * @param toughness new toughness
     * @return a copy with that value
     */
    public ArmorStats withToughness(float toughness) {
        return new ArmorStats(durability, helmet, chestplate, leggings, boots, enchantability, toughness, knockbackResistance);
    }

    /**
     * @param value new enchantability
     * @return a copy with that value
     */
    public ArmorStats withEnchantability(int value) {
        return new ArmorStats(durability, helmet, chestplate, leggings, boots, value, toughness, knockbackResistance);
    }
}
