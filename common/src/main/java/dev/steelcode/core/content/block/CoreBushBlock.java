// SPDX-License-Identifier: LGPL-3.0-or-later
package dev.steelcode.core.content.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.BushBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/** A simple bush or grass: no collision, placed on dirt-like blocks, grass-like shape. */
public class CoreBushBlock extends BushBlock {

    /** Block codec. */
    public static final MapCodec<CoreBushBlock> CODEC = simpleCodec(CoreBushBlock::new);
    private static final VoxelShape SHAPE = box(2.0, 0.0, 2.0, 14.0, 13.0, 14.0);

    /**
     * @param properties block properties
     */
    public CoreBushBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected MapCodec<? extends BushBlock> codec() {
        return CODEC;
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }
}
