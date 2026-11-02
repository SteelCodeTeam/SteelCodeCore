// SPDX-License-Identifier: LGPL-3.0-or-later
package dev.steelcode.core.content.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.AmethystBlock;
import net.minecraft.world.level.block.AmethystClusterBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.BuddingAmethystBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluids;

import java.util.function.Supplier;

/**
 * A budding block: grows its crystals on random ticks exactly like budding amethyst
 * (small → medium → large → cluster), on any free face.
 */
public class CoreBuddingBlock extends AmethystBlock {

    private static final Direction[] DIRECTIONS = Direction.values();
    private final Supplier<Block>[] stages;

    /**
     * @param properties block properties (copy {@code Blocks.BUDDING_AMETHYST})
     * @param stages     the bud blocks in growth order, ending with the cluster
     */
    @SafeVarargs
    public CoreBuddingBlock(Properties properties, Supplier<Block>... stages) {
        super(properties);
        this.stages = stages;
    }

    @Override
    public MapCodec<? extends AmethystBlock> codec() {
        return simpleCodec(p -> new CoreBuddingBlock(p, stages));
    }

    @Override
    protected void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (random.nextInt(5) != 0) return;
        Direction dir = DIRECTIONS[random.nextInt(DIRECTIONS.length)];
        BlockPos target = pos.relative(dir);
        BlockState current = level.getBlockState(target);
        Block next = null;
        if (BuddingAmethystBlock.canClusterGrowAtState(current)) {
            next = stages[0].get();
        } else {
            for (int i = 0; i < stages.length - 1; i++) {
                if (current.is(stages[i].get()) && current.getValue(AmethystClusterBlock.FACING) == dir) {
                    next = stages[i + 1].get();
                    break;
                }
            }
        }
        if (next != null) {
            level.setBlockAndUpdate(target, next.defaultBlockState().setValue(AmethystClusterBlock.FACING, dir)
                    .setValue(AmethystClusterBlock.WATERLOGGED, current.getFluidState().getType() == Fluids.WATER));
        }
    }
}
