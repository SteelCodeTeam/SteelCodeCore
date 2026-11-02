// SPDX-License-Identifier: LGPL-3.0-or-later
package dev.steelcode.core.neoforge.impl;

import dev.steelcode.core.impl.platform.CorePlatform;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.alchemy.Potion;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.FlowerPotBlock;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.BlockEntityTypeAddBlocksEvent;
import net.neoforged.neoforge.event.brewing.RegisterBrewingRecipesEvent;
import org.jetbrains.annotations.ApiStatus;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

/** NeoForge implementation of {@link CorePlatform.Hooks}: queues requests and applies them in its events. */
@ApiStatus.Internal
public final class NeoForgeHooks implements CorePlatform.Hooks {

    private record BlockEntityAddition(BlockEntityType<?> type, Supplier<? extends Block> block) {
    }

    private record Brewing(Holder<Potion> input, Supplier<? extends Item> ingredient, Holder<Potion> output) {
    }

    private final List<BlockEntityAddition> blockEntities = new ArrayList<>();
    private final List<Brewing> brewing = new ArrayList<>();
    final List<Object[]> itemRenderers = new ArrayList<>();
    private final net.neoforged.neoforge.registries.DeferredRegister<net.neoforged.neoforge.attachment.AttachmentType<?>> attachments =
            net.neoforged.neoforge.registries.DeferredRegister.create(
                    net.neoforged.neoforge.registries.NeoForgeRegistries.Keys.ATTACHMENT_TYPES, dev.steelcode.core.SteelCodeCore.MOD_ID);

    public NeoForgeHooks(IEventBus modBus) {
        attachments.register(modBus);
        modBus.addListener((BlockEntityTypeAddBlocksEvent event) -> {
            for (BlockEntityAddition a : blockEntities) event.modify(a.type(), a.block().get());
        });
        NeoForge.EVENT_BUS.addListener((RegisterBrewingRecipesEvent event) -> {
            for (Brewing b : brewing) event.getBuilder().addMix(b.input(), b.ingredient().get(), b.output());
        });
    }

    @Override
    public void pottedPlant(Block plant, Block pot) {
        ((FlowerPotBlock) Blocks.FLOWER_POT).addPlant(BuiltInRegistries.BLOCK.getKey(plant), () -> pot);
    }

    @Override
    public void supportBlockEntity(BlockEntityType<?> type, Supplier<? extends Block> block) {
        blockEntities.add(new BlockEntityAddition(type, block));
    }

    @Override
    public void brewing(Holder<Potion> input, Supplier<? extends Item> ingredient, Holder<Potion> output) {
        brewing.add(new Brewing(input, ingredient, output));
    }

    @Override
    public void registerWoodType(net.minecraft.world.level.block.state.properties.WoodType type) {
        net.minecraft.world.level.block.state.properties.WoodType.register(type);
    }

    @Override
    public <T> CorePlatform.AttachmentHandle<T> attachment(net.minecraft.resources.ResourceLocation id,
                                                           com.mojang.serialization.Codec<T> codec, Supplier<T> def,
                                                           boolean copyOnDeath) {
        var holder = attachments.register(id.getNamespace() + "__" + id.getPath(), () -> {
            var b = net.neoforged.neoforge.attachment.AttachmentType.builder(def).serialize(codec);
            if (copyOnDeath) b = b.copyOnDeath();
            return b.build();
        });
        return new CorePlatform.AttachmentHandle<>() {
            @Override
            public T get(net.minecraft.world.entity.Entity entity) {
                return entity.getData(holder.get());
            }

            @Override
            public void set(net.minecraft.world.entity.Entity entity, T value) {
                entity.setData(holder.get(), value);
            }
        };
    }

    @Override
    public void oxidizable(Block block, Block next) {
        // NeoForge reads it from the neoforge:oxidizables data map, written by datagen.
    }

    @Override
    public void waxable(Block block, Block waxed) {
        // neoforge:waxables data map (datagen).
    }

    @Override
    public void itemRenderer(Supplier<? extends Item> item, CorePlatform.ItemRenderCallback renderer) {
        itemRenderers.add(new Object[]{item, renderer});
    }
}
