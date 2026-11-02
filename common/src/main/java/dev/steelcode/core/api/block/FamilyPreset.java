// SPDX-License-Identifier: LGPL-3.0-or-later
package dev.steelcode.core.api.block;

/**
 * Typical settings for a family. They can always be overridden afterwards
 * ({@code .preset(METAL).tool(PICKAXE, Tier.IRON)}).
 */
public enum FamilyPreset {
    /** Pickaxe, stone buttons and plates, doors that open by hand, stonecutter recipes. */
    STONE,
    /** Axe, {@code wooden_*} tags (fuel and vanilla recipes), oak sounds and timings, flammable. */
    WOOD,
    /** Pickaxe, iron-style doors and trapdoors (redstone only), weighted pressure plate, stonecutter recipes. */
    METAL
}
