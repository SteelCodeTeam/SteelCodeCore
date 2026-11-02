// SPDX-License-Identifier: LGPL-3.0-or-later
package dev.steelcode.core.impl.platform;

import net.minecraft.core.Holder;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.alchemy.Potion;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import org.jetbrains.annotations.ApiStatus;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * The little that differs between loaders. Each loader installs its implementation at startup; requests made
 * before that are queued and applied as soon as it arrives (mod load order is not guaranteed).
 */
@ApiStatus.Internal
public final class CorePlatform {

    /** Implemented by each loader. */
    public interface Hooks {
        /** Lets the empty flower pot accept the plant (NeoForge needs this registered; Fabric does not). */
        void pottedPlant(Block plant, Block pot);

        /** Adds a block to a vanilla block entity type (signs), whether or not it is registered yet. */
        void supportBlockEntity(BlockEntityType<?> type, Supplier<? extends Block> block);

        /** Brewing recipe: {@code input + ingredient -> output}. */
        void brewing(Holder<Potion> input, Supplier<? extends Item> ingredient, Holder<Potion> output);

        /** Registers a wood type (signs). Private in vanilla; the loaders expose it. */
        void registerWoodType(net.minecraft.world.level.block.state.properties.WoodType type);

        /** Data stored on entities (Fabric/NeoForge attachments). */
        <T> AttachmentHandle<T> attachment(net.minecraft.resources.ResourceLocation id, com.mojang.serialization.Codec<T> codec,
                                           Supplier<T> defaultValue, boolean copyOnDeath);

        /** A block that oxidizes into {@code next} (NeoForge uses a data map, so this may do nothing). */
        void oxidizable(Block block, Block next);

        /** A block and its waxed version. */
        void waxable(Block block, Block waxed);

        /** Custom item rendering (shields). Client only. */
        void itemRenderer(Supplier<? extends Item> item, ItemRenderCallback renderer);
    }

    /** Access to an entity attachment. */
    public interface AttachmentHandle<T> {
        T get(net.minecraft.world.entity.Entity entity);

        void set(net.minecraft.world.entity.Entity entity, T value);
    }

    /** A handle that waits for the loader to install its hooks. */
    private static final class LazyHandle<T> implements AttachmentHandle<T> {
        private volatile AttachmentHandle<T> delegate;

        private AttachmentHandle<T> d() {
            if (delegate == null) throw new IllegalStateException("SteelCodeCore is not loaded yet");
            return delegate;
        }

        @Override public T get(net.minecraft.world.entity.Entity e) { return d().get(e); }
        @Override public void set(net.minecraft.world.entity.Entity e, T v) { d().set(e, v); }
    }

    public static <T> AttachmentHandle<T> attachment(net.minecraft.resources.ResourceLocation id, com.mojang.serialization.Codec<T> codec,
                                                     Supplier<T> def, boolean copyOnDeath) {
        LazyHandle<T> handle = new LazyHandle<>();
        run(h -> handle.delegate = h.attachment(id, codec, def, copyOnDeath));
        return handle;
    }

    public static void oxidizable(Block block, Block next) {
        run(h -> h.oxidizable(block, next));
    }

    public static void waxable(Block block, Block waxed) {
        run(h -> h.waxable(block, waxed));
    }

    /** Draws an item with custom rendering (like vanilla does with shields). */
    @FunctionalInterface
    public interface ItemRenderCallback {
        void render(net.minecraft.world.item.ItemStack stack, net.minecraft.world.item.ItemDisplayContext context,
                    com.mojang.blaze3d.vertex.PoseStack pose, net.minecraft.client.renderer.MultiBufferSource buffers,
                    int light, int overlay);
    }

    private static Hooks hooks;
    private static final List<Consumer<Hooks>> pending = new ArrayList<>();

    private CorePlatform() {
    }

    public static synchronized void install(Hooks impl) {
        hooks = impl;
        pending.forEach(p -> p.accept(impl));
        pending.clear();
    }

    private static synchronized void run(Consumer<Hooks> action) {
        if (hooks != null) action.accept(hooks);
        else pending.add(action);
    }

    public static void pottedPlant(Block plant, Block pot) {
        run(h -> h.pottedPlant(plant, pot));
    }

    public static void supportBlockEntity(BlockEntityType<?> type, Supplier<? extends Block> block) {
        run(h -> h.supportBlockEntity(type, block));
    }

    public static void brewing(Holder<Potion> input, Supplier<? extends Item> ingredient, Holder<Potion> output) {
        run(h -> h.brewing(input, ingredient, output));
    }

    public static void registerWoodType(net.minecraft.world.level.block.state.properties.WoodType type) {
        run(h -> h.registerWoodType(type));
    }

    public static void itemRenderer(Supplier<? extends Item> item, ItemRenderCallback renderer) {
        run(h -> h.itemRenderer(item, renderer));
    }
}
