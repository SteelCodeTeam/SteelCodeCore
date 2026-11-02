// SPDX-License-Identifier: LGPL-3.0-or-later
package dev.steelcode.core.neoforge.impl;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.steelcode.core.impl.platform.CorePlatform;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.extensions.common.IClientItemExtensions;
import net.neoforged.neoforge.client.extensions.common.RegisterClientExtensionsEvent;
import org.jetbrains.annotations.ApiStatus;

import java.util.function.Supplier;

/** NeoForge client hooks; only loaded on the client. */
@ApiStatus.Internal
public final class NeoForgeClientHooks {

    private NeoForgeClientHooks() {
    }

    @SuppressWarnings("unchecked")
    public static void register(IEventBus modBus, NeoForgeHooks hooks) {
        modBus.addListener((RegisterClientExtensionsEvent event) -> {
            for (Object[] r : hooks.itemRenderers) {
                Supplier<? extends Item> item = (Supplier<? extends Item>) r[0];
                CorePlatform.ItemRenderCallback callback = (CorePlatform.ItemRenderCallback) r[1];
                event.registerItem(new IClientItemExtensions() {
                    private BlockEntityWithoutLevelRenderer renderer;

                    @Override
                    public BlockEntityWithoutLevelRenderer getCustomRenderer() {
                        if (renderer == null) {
                            Minecraft mc = Minecraft.getInstance();
                            renderer = new BlockEntityWithoutLevelRenderer(mc.getBlockEntityRenderDispatcher(), mc.getEntityModels()) {
                                @Override
                                public void renderByItem(ItemStack stack, ItemDisplayContext context, PoseStack pose,
                                                         MultiBufferSource buffers, int light, int overlay) {
                                    callback.render(stack, context, pose, buffers, light, overlay);
                                }
                            };
                        }
                        return renderer;
                    }
                }, item.get());
            }
        });
    }
}
