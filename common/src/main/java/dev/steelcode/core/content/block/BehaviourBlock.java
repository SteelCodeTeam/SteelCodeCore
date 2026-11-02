// SPDX-License-Identifier: LGPL-3.0-or-later
package dev.steelcode.core.content.block;

import dev.steelcode.core.api.block.BlockBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

/**
 * A block that runs an action when an entity walks on it (effects, damage...). Created by
 * {@code BlockBuilder.onStep}, {@code stepEffect} and {@code stepDamage}; extend it for more behaviour.
 */
public class BehaviourBlock extends Block {

    /** Action run when an entity steps on the block. */
    @FunctionalInterface
    public interface StepAction {
        /**
         * @param level  the level (client or server)
         * @param pos    the block position
         * @param state  the block state
         * @param entity the entity walking on it
         */
        void onStep(Level level, BlockPos pos, BlockState state, Entity entity);
    }

    private final StepAction step;

    /**
     * @param properties block properties
     * @param step       action run on step
     */
    public BehaviourBlock(Properties properties, StepAction step) {
        super(properties);
        this.step = step;
    }

    @Override
    public void stepOn(Level level, BlockPos pos, BlockState state, Entity entity) {
        if (step != null) step.onStep(level, pos, state, entity);
        super.stepOn(level, pos, state, entity);
    }
}
