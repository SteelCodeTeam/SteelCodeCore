// SPDX-License-Identifier: LGPL-3.0-or-later
package dev.steelcode.core.api.block;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;

/**
 * Creates a family variant with your own block class.
 * <pre>{@code
 * REG.family().base(Blocks.GOLD_BLOCK).stairs()
 *         .factory(Variant.STAIRS, (base, props) -> new MyStairs(base.defaultBlockState(), props))
 *         .build();
 * }</pre>
 * @see BlockFamilyBuilder#factory(Variant, VariantFactory)
 */
@FunctionalInterface
public interface VariantFactory {
    /**
     * Creates the block.
     * @param base       the family's base block
     * @param properties properties already prepared (copied from the base, with light and modifiers applied)
     * @return the new block
     */
    Block create(Block base, BlockBehaviour.Properties properties);
}
