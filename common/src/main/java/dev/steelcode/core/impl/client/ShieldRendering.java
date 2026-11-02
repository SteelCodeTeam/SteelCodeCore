// SPDX-License-Identifier: LGPL-3.0-or-later
package dev.steelcode.core.impl.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import dev.steelcode.core.impl.platform.CorePlatform;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.ShieldModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.ApiStatus;

/** Draws a custom shield with the vanilla model and a full 64x64 texture (like {@code shield_base_nopattern}). */
@ApiStatus.Internal
public final class ShieldRendering {

    private ShieldRendering() {
    }

    public static CorePlatform.ItemRenderCallback forTexture(ResourceLocation texture) {
        ShieldModel[] model = new ShieldModel[1];
        return (stack, context, pose, buffers, light, overlay) -> {
            if (model[0] == null) {
                model[0] = new ShieldModel(Minecraft.getInstance().getEntityModels().bakeLayer(ModelLayers.SHIELD));
            }
            PoseStack p = pose;
            p.pushPose();
            p.scale(1.0F, -1.0F, -1.0F);
            VertexConsumer consumer = ItemRenderer.getFoilBufferDirect(buffers, model[0].renderType(texture), true, stack.hasFoil());
            model[0].handle().render(p, consumer, light, overlay);
            model[0].plate().render(p, consumer, light, overlay);
            p.popPose();
        };
    }
}
