// SPDX-License-Identifier: LGPL-3.0-or-later
package dev.steelcode.core.content.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.block.CropBlock;

import java.util.function.Supplier;

/** A crop with its own seeds (vanilla {@code CropBlock} always returns wheat seeds from {@code getBaseSeedId}). */
public class CoreCropBlock extends CropBlock {

    private final Supplier<? extends ItemLike> seed;

    /**
     * @param properties block properties
     * @param seed       the item that plants this crop and is picked with middle click
     */
    public CoreCropBlock(Properties properties, Supplier<? extends ItemLike> seed) {
        super(properties);
        this.seed = seed;
    }

    @Override
    public MapCodec<? extends CropBlock> codec() {
        return simpleCodec(p -> new CoreCropBlock(p, seed));
    }

    @Override
    protected ItemLike getBaseSeedId() {
        return seed.get();
    }
}
