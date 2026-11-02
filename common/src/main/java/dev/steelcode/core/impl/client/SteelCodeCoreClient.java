// SPDX-License-Identifier: LGPL-3.0-or-later
package dev.steelcode.core.impl.client;

import dev.architectury.registry.client.level.entity.EntityModelLayerRegistry;
import dev.architectury.registry.client.level.entity.EntityRendererRegistry;
import dev.architectury.registry.client.rendering.ColorHandlerRegistry;
import dev.architectury.registry.client.rendering.RenderTypeRegistry;
import dev.architectury.registry.item.ItemPropertiesRegistry;
import dev.architectury.registry.registries.RegistrySupplier;
import dev.steelcode.core.impl.registry.BlockEntry;
import dev.steelcode.core.impl.registry.RegistryContext;
import net.minecraft.client.model.BoatModel;
import net.minecraft.client.model.ChestBoatModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.renderer.BiomeColors;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.vehicle.Boat;
import net.minecraft.world.item.CrossbowItem;
import net.minecraft.world.item.FishingRodItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.ChargedProjectiles;
import net.minecraft.world.level.FoliageColor;
import org.jetbrains.annotations.ApiStatus;

/**
 * Client side of the library.
 * <ul>
 *     <li>{@link #early(RegistryContext)}: at registration (colors, entity renderers and layers).</li>
 *     <li>{@link #init()}: once blocks exist (render layers, item properties).</li>
 * </ul>
 */
@ApiStatus.Internal
public final class SteelCodeCoreClient {

    private SteelCodeCoreClient() {
    }

    /** Called by {@code register()} on the client. */
    public static void early(RegistryContext registry) {
        for (BlockEntry entry : registry.blockEntries()) registerTint(entry);
    }

    /** Called by the loaders during client setup. */
    public static void init() {
        for (RegistryContext registry : RegistryContext.all()) {
            for (BlockEntry entry : registry.blockEntries()) registerRenderLayer(entry);
            registry.clientLate().forEach(Runnable::run);
        }
    }

    // ================================================================ boats

    @SuppressWarnings({"unchecked", "rawtypes"})
    public static void boat(RegistrySupplier<? extends EntityType<? extends Boat>> type, String ns, String name,
                            ResourceLocation texture, boolean chest) {
        ModelLayerLocation layer = new ModelLayerLocation(
                ResourceLocation.fromNamespaceAndPath(ns, (chest ? "chest_boat/" : "boat/") + name), "main");
        EntityModelLayerRegistry.register(layer, chest ? ChestBoatModel::createBodyModel : BoatModel::createBodyModel);
        EntityRendererRegistry.register((RegistrySupplier) type, ctx -> new CoreBoatRenderer(ctx, layer, texture, chest));
    }

    // ================================================================ weapon item properties (like vanilla)

    public static void bowProperties(Item item) {
        ItemPropertiesRegistry.register(item, ResourceLocation.withDefaultNamespace("pull"), (stack, level, entity, seed) ->
                entity == null || entity.getUseItem() != stack ? 0.0F
                        : (stack.getUseDuration(entity) - entity.getUseItemRemainingTicks()) / 20.0F);
        ItemPropertiesRegistry.register(item, ResourceLocation.withDefaultNamespace("pulling"), (stack, level, entity, seed) ->
                entity != null && entity.isUsingItem() && entity.getUseItem() == stack ? 1.0F : 0.0F);
    }

    public static void crossbowProperties(Item item) {
        ItemPropertiesRegistry.register(item, ResourceLocation.withDefaultNamespace("pull"), (stack, level, entity, seed) ->
                entity == null || CrossbowItem.isCharged(stack) ? 0.0F
                        : (float) (stack.getUseDuration(entity) - entity.getUseItemRemainingTicks())
                        / CrossbowItem.getChargeDuration(stack, entity));
        ItemPropertiesRegistry.register(item, ResourceLocation.withDefaultNamespace("pulling"), (stack, level, entity, seed) ->
                entity != null && entity.isUsingItem() && entity.getUseItem() == stack && !CrossbowItem.isCharged(stack) ? 1.0F : 0.0F);
        ItemPropertiesRegistry.register(item, ResourceLocation.withDefaultNamespace("charged"), (stack, level, entity, seed) ->
                CrossbowItem.isCharged(stack) ? 1.0F : 0.0F);
        ItemPropertiesRegistry.register(item, ResourceLocation.withDefaultNamespace("firework"), (stack, level, entity, seed) -> {
            ChargedProjectiles charged = stack.get(DataComponents.CHARGED_PROJECTILES);
            return charged != null && charged.contains(Items.FIREWORK_ROCKET) ? 1.0F : 0.0F;
        });
    }

    public static void fishingRodProperties(Item item) {
        ItemPropertiesRegistry.register(item, ResourceLocation.withDefaultNamespace("cast"), (stack, level, entity, seed) -> {
            if (entity == null) return 0.0F;
            boolean main = entity.getMainHandItem() == stack;
            boolean off = entity.getOffhandItem() == stack && !(entity.getMainHandItem().getItem() instanceof FishingRodItem);
            return (main || off) && entity instanceof Player player && player.fishing != null ? 1.0F : 0.0F;
        });
    }

    public static void shieldProperties(Item item) {
        ItemPropertiesRegistry.register(item, ResourceLocation.withDefaultNamespace("blocking"), (stack, level, entity, seed) ->
                entity != null && entity.isUsingItem() && entity.getUseItem() == stack ? 1.0F : 0.0F);
    }

    // ================================================================ blocks

    private static void registerRenderLayer(BlockEntry entry) {
        if (entry.renderLayer() == null) return;
        RenderType type = switch (entry.renderLayer()) {
            case SOLID -> null;
            case CUTOUT -> RenderType.cutout();
            case CUTOUT_MIPPED -> RenderType.cutoutMipped();
            case TRANSLUCENT -> RenderType.translucent();
        };
        if (type != null) RenderTypeRegistry.register(type, entry.block().get());
    }

    private static void registerTint(BlockEntry entry) {
        int tint = entry.tint();
        if (tint == BlockEntry.NO_TINT) return;
        if (tint == BlockEntry.FOLIAGE_TINT) {
            ColorHandlerRegistry.registerBlockColors((state, level, pos, index) ->
                    level != null && pos != null ? BiomeColors.getAverageFoliageColor(level, pos) : FoliageColor.getDefaultColor(),
                    entry.block());
            if (entry.hasItem()) {
                ColorHandlerRegistry.registerItemColors((stack, index) -> 0xFF000000 | FoliageColor.getDefaultColor(), entry.block());
            }
        } else {
            int argb = 0xFF000000 | tint;
            ColorHandlerRegistry.registerBlockColors((state, level, pos, index) -> argb, entry.block());
            if (entry.hasItem()) ColorHandlerRegistry.registerItemColors((stack, index) -> argb, entry.block());
        }
    }
}
