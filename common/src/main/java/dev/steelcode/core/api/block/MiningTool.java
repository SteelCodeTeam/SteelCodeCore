// SPDX-License-Identifier: LGPL-3.0-or-later
package dev.steelcode.core.api.block;

import net.minecraft.resources.ResourceLocation;

/**
 * The tool that mines a block fast, i.e. the vanilla {@code minecraft:mineable/*} tag it belongs to.
 * Combine with {@link Tier} for the minimum tool level.
 */
public enum MiningTool {
    /** {@code minecraft:mineable/pickaxe} */
    PICKAXE("mineable/pickaxe"),
    /** {@code minecraft:mineable/axe} */
    AXE("mineable/axe"),
    /** {@code minecraft:mineable/shovel} */
    SHOVEL("mineable/shovel"),
    /** {@code minecraft:mineable/hoe} */
    HOE("mineable/hoe");

    private final String tag;

    MiningTool(String tag) {
        this.tag = tag;
    }

    /** @return the vanilla block tag of this tool */
    public ResourceLocation tag() {
        return ResourceLocation.withDefaultNamespace(tag);
    }

    /**
     * Minimum tool level ({@code minecraft:needs_*_tool} tags). Only enforced if the block has
     * {@code requiresCorrectToolForDrops()}.
     */
    public enum Tier {
        /** Any tool (or none). */
        ANY(null),
        /** Stone or better. */
        STONE("needs_stone_tool"),
        /** Iron or better. */
        IRON("needs_iron_tool"),
        /** Diamond or better. */
        DIAMOND("needs_diamond_tool");

        private final String tag;

        Tier(String tag) {
            this.tag = tag;
        }

        /** @return the vanilla block tag of this tier, or {@code null} for {@link #ANY} */
        public ResourceLocation tag() {
            return tag == null ? null : ResourceLocation.withDefaultNamespace(tag);
        }
    }
}
