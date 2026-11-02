// SPDX-License-Identifier: LGPL-3.0-or-later
package dev.steelcode.core.api.item;

import dev.steelcode.core.api.block.MiningTool;


/**
 * Tool statistics (a vanilla {@code Tier}). Start from a vanilla preset and tweak it:
 * <pre>{@code ToolStats.DIAMOND.withUses(1200).withSpeed(9)}</pre>
 * @param uses           durability (iron 250, diamond 1561)
 * @param speed          mining speed (iron 6, diamond 8, gold 12)
 * @param attackBonus    extra damage over each tool's base damage (iron 2, diamond 3)
 * @param enchantability enchantability (iron 14, gold 22)
 * @param level          what it can mine: {@code STONE} mines iron ore, {@code IRON} mines diamond ore,
 *                       {@code DIAMOND} mines obsidian
 */
public record ToolStats(int uses, float speed, float attackBonus, int enchantability, MiningTool.Tier level) {

    /** Vanilla wooden tools. */
    public static final ToolStats WOOD = new ToolStats(59, 2.0F, 0.0F, 15, MiningTool.Tier.ANY);
    /** Vanilla stone tools. */
    public static final ToolStats STONE = new ToolStats(131, 4.0F, 1.0F, 5, MiningTool.Tier.STONE);
    /** Vanilla iron tools. */
    public static final ToolStats IRON = new ToolStats(250, 6.0F, 2.0F, 14, MiningTool.Tier.IRON);
    /** Vanilla golden tools. */
    public static final ToolStats GOLD = new ToolStats(32, 12.0F, 0.0F, 22, MiningTool.Tier.ANY);
    /** Vanilla diamond tools. */
    public static final ToolStats DIAMOND = new ToolStats(1561, 8.0F, 3.0F, 10, MiningTool.Tier.DIAMOND);
    /** Vanilla netherite tools. */
    public static final ToolStats NETHERITE = new ToolStats(2031, 9.0F, 4.0F, 15, MiningTool.Tier.DIAMOND);

    /**
     * @param uses new durability
     * @return a copy with that value
     */
    public ToolStats withUses(int uses) { return new ToolStats(uses, speed, attackBonus, enchantability, level); }
    /**
     * @param speed new mining speed
     * @return a copy with that value
     */
    public ToolStats withSpeed(float speed) { return new ToolStats(uses, speed, attackBonus, enchantability, level); }
    /**
     * @param bonus new extra attack damage
     * @return a copy with that value
     */
    public ToolStats withAttackBonus(float bonus) { return new ToolStats(uses, speed, bonus, enchantability, level); }
    /**
     * @param value new enchantability
     * @return a copy with that value
     */
    public ToolStats withEnchantability(int value) { return new ToolStats(uses, speed, attackBonus, value, level); }
    /**
     * @param level new mining level
     * @return a copy with that value
     */
    public ToolStats withLevel(MiningTool.Tier level) { return new ToolStats(uses, speed, attackBonus, enchantability, level); }
}
