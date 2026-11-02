// SPDX-License-Identifier: LGPL-3.0-or-later
package dev.steelcode.core.impl.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.model.BoatModel;
import net.minecraft.client.model.ChestBoatModel;
import net.minecraft.client.model.ListModel;
import net.minecraft.client.model.WaterPatchModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.vehicle.Boat;
import org.jetbrains.annotations.ApiStatus;
import org.joml.Quaternionf;

/** Renderer of custom boats: the vanilla one, with a single texture and model. */
@ApiStatus.Internal
public class CoreBoatRenderer extends EntityRenderer<Boat> {

    private final ResourceLocation texture;
    private final ListModel<Boat> model;

    public CoreBoatRenderer(EntityRendererProvider.Context context, ModelLayerLocation layer, ResourceLocation texture,
                            boolean chest) {
        super(context);
        this.shadowRadius = 0.8F;
        this.texture = texture;
        this.model = chest ? new ChestBoatModel(context.bakeLayer(layer)) : new BoatModel(context.bakeLayer(layer));
    }

    @Override
    public void render(Boat boat, float yaw, float partialTick, PoseStack pose, MultiBufferSource buffers, int light) {
        pose.pushPose();
        pose.translate(0.0F, 0.375F, 0.0F);
        pose.mulPose(Axis.YP.rotationDegrees(180.0F - yaw));
        float hurt = boat.getHurtTime() - partialTick;
        float damage = Math.max(0.0F, boat.getDamage() - partialTick);
        if (hurt > 0.0F) {
            pose.mulPose(Axis.XP.rotationDegrees(Mth.sin(hurt) * hurt * damage / 10.0F * boat.getHurtDir()));
        }
        float bubble = boat.getBubbleAngle(partialTick);
        if (!Mth.equal(bubble, 0.0F)) {
            pose.mulPose(new Quaternionf().setAngleAxis(bubble * (float) (Math.PI / 180.0), 1.0F, 0.0F, 1.0F));
        }
        pose.scale(-1.0F, -1.0F, 1.0F);
        pose.mulPose(Axis.YP.rotationDegrees(90.0F));
        model.setupAnim(boat, partialTick, 0.0F, -0.1F, 0.0F, 0.0F);
        VertexConsumer consumer = buffers.getBuffer(model.renderType(texture));
        model.renderToBuffer(pose, consumer, light, OverlayTexture.NO_OVERLAY);
        if (!boat.isUnderWater() && model instanceof WaterPatchModel patch) {
            patch.waterPatch().render(pose, buffers.getBuffer(RenderType.waterMask()), light, OverlayTexture.NO_OVERLAY);
        }
        pose.popPose();
        super.render(boat, yaw, partialTick, pose, buffers, light);
    }

    @Override
    public ResourceLocation getTextureLocation(Boat boat) {
        return texture;
    }
}
