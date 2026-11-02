// SPDX-License-Identifier: LGPL-3.0-or-later
package dev.steelcode.core.fabric.impl;

import dev.steelcode.core.impl.platform.CorePlatform;
import net.fabricmc.fabric.api.object.builder.v1.block.entity.FabricBlockEntityType;
import net.fabricmc.fabric.api.registry.FabricBrewingRecipeRegistryBuilder;
import net.minecraft.core.Holder;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.alchemy.Potion;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import org.jetbrains.annotations.ApiStatus;

import java.util.function.Supplier;

/** Fabric implementation of {@link CorePlatform.Hooks}. */
@ApiStatus.Internal
public final class FabricHooks implements CorePlatform.Hooks {

    @Override
    public void pottedPlant(Block plant, Block pot) {
        // The vanilla FlowerPotBlock constructor already registers the plant.
    }

    @Override
    public void supportBlockEntity(BlockEntityType<?> type, Supplier<? extends Block> block) {
        ((FabricBlockEntityType) (Object) type).addSupportedBlock(block.get());
    }

    @Override
    public void brewing(Holder<Potion> input, Supplier<? extends Item> ingredient, Holder<Potion> output) {
        FabricBrewingRecipeRegistryBuilder.BUILD.register(builder -> builder.addMix(input, ingredient.get(), output));
    }

    @Override
    public void registerWoodType(net.minecraft.world.level.block.state.properties.WoodType type) {
        // WoodType is a record: the copy Fabric registers equals (equals/hashCode) the one our blocks use.
        net.fabricmc.fabric.api.object.builder.v1.block.type.WoodTypeBuilder.copyOf(type)
                .register(net.minecraft.resources.ResourceLocation.parse(type.name()), type.setType());
    }

    @Override
    @SuppressWarnings("UnstableApiUsage")
    public <T> CorePlatform.AttachmentHandle<T> attachment(net.minecraft.resources.ResourceLocation id,
                                                           com.mojang.serialization.Codec<T> codec, Supplier<T> def,
                                                           boolean copyOnDeath) {
        var builder = net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry.<T>builder().persistent(codec).initializer(def);
        if (copyOnDeath) builder = builder.copyOnDeath();
        net.fabricmc.fabric.api.attachment.v1.AttachmentType<T> type = builder.buildAndRegister(id);
        return new CorePlatform.AttachmentHandle<>() {
            @Override
            public T get(net.minecraft.world.entity.Entity entity) {
                return ((net.fabricmc.fabric.api.attachment.v1.AttachmentTarget) (Object) entity).getAttachedOrCreate(type);
            }

            @Override
            public void set(net.minecraft.world.entity.Entity entity, T value) {
                ((net.fabricmc.fabric.api.attachment.v1.AttachmentTarget) (Object) entity).setAttached(type, value);
            }
        };
    }

    @Override
    public void oxidizable(Block block, Block next) {
        net.fabricmc.fabric.api.registry.OxidizableBlocksRegistry.registerOxidizableBlockPair(block, next);
    }

    @Override
    public void waxable(Block block, Block waxed) {
        net.fabricmc.fabric.api.registry.OxidizableBlocksRegistry.registerWaxableBlockPair(block, waxed);
    }

    @Override
    public void itemRenderer(Supplier<? extends Item> item, CorePlatform.ItemRenderCallback renderer) {
        FabricClientHooks.itemRenderer(item, renderer);
    }
}
