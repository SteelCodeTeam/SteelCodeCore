// SPDX-License-Identifier: LGPL-3.0-or-later
package dev.steelcode.core.api.world;

import dev.architectury.registry.registries.DeferredSupplier;
import dev.architectury.registry.registries.RegistrySupplier;
import dev.steelcode.core.api.block.Drops;
import dev.steelcode.core.api.block.RenderLayer;
import dev.steelcode.core.api.block.Variant;
import dev.steelcode.core.api.item.ItemBuilder;
import dev.steelcode.core.content.block.CoreCropBlock;
import dev.steelcode.core.impl.registry.RegistryContext;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemNameBlockItem;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import org.jetbrains.annotations.ApiStatus;

import java.util.Objects;
import java.util.function.UnaryOperator;

/**
 * A crop with eight growth ages (like wheat). Obtain one with {@code REG.crop(name)}.
 * <pre>{@code
 * // Separate seeds (like wheat): tomato_seeds plant tomato_crop, which yields tomato
 * CropSet TOMATO = REG.crop("tomato").food(3, 0.6f).build();
 *
 * // The produce itself is planted (like carrots)
 * CropSet ONION = REG.crop("onion").plantedByProduce().food(2, 0.4f).build();
 * }</pre>
 * Textures: {@code block/<name>_stage0..3} (or {@code ..7} with {@code .stages(8)}), {@code item/<name>} and
 * {@code item/<name>_seeds}. Mature crops drop produce and seeds (with Fortune bonus), young ones drop seeds;
 * seeds are compostable and villagers can farm them.
 */
public final class CropBuilder {

    private final RegistryContext registry;
    private final String name;
    private boolean separateSeeds = true;
    private int stages = 4;
    private int nutrition = -1;
    private float saturation;
    private UnaryOperator<ItemBuilder> produce = UnaryOperator.identity();
    private DeferredSupplier<CreativeModeTab> tab;

    /** Internal: use {@code REG.crop(name)}. */
    @ApiStatus.Internal
    public CropBuilder(RegistryContext registry, String name) {
        this.registry = registry;
        this.name = Objects.requireNonNull(name);
    }

    /**
     * The produce is planted directly; no seeds item is created.
     * @return this builder
     */
    public CropBuilder plantedByProduce() {
        this.separateSeeds = false;
        return this;
    }

    /**
     * @param textures number of growth textures: 4 (like carrots, the default) or 8 (like wheat)
     * @return this builder
     */
    public CropBuilder stages(int textures) {
        if (textures != 4 && textures != 8) throw new IllegalArgumentException("stages(...) must be 4 or 8");
        this.stages = textures;
        return this;
    }

    /**
     * Makes the produce edible.
     * @param nutrition  hunger points restored
     * @param saturation saturation modifier
     * @return this builder
     */
    public CropBuilder food(int nutrition, float saturation) {
        this.nutrition = nutrition;
        this.saturation = saturation;
        return this;
    }

    /**
     * Extra settings for the produce item.
     * <pre>{@code .produce(p -> p.foodEffect(MobEffects.REGENERATION, 5, 0, 1f))}</pre>
     * @param produce configures the produce item builder
     * @return this builder
     */
    public CropBuilder produce(UnaryOperator<ItemBuilder> produce) {
        this.produce = Objects.requireNonNull(produce);
        return this;
    }

    /**
     * Creative tab of the seeds and produce.
     * @param tab the tab
     * @return this builder
     */
    public CropBuilder tab(DeferredSupplier<CreativeModeTab> tab) {
        this.tab = tab;
        return this;
    }

    /**
     * Registers the crop block, produce and seeds and queues their datagen.
     * @return the created crop
     */
    public CropSet build() {
        String ns = registry.modId();
        ResourceLocation seedsId = ResourceLocation.fromNamespaceAndPath(ns, separateSeeds ? name + "_seeds" : name);
        @SuppressWarnings("unchecked")
        RegistrySupplier<Item>[] seedRef = new RegistrySupplier[1];

        RegistrySupplier<Block> crop = registry.block(name + "_crop").copy(Blocks.WHEAT)
                .factory(p -> new CoreCropBlock(p, () -> seedRef[0].get()))
                .model(Variant.CROP).texture("block/" + name + "_stage").param(stages, 7)
                .renderLayer(RenderLayer.CUTOUT).naming("self", name).noItem()
                .drops(Drops.self()).tag("minecraft:crops").tag("minecraft:bee_growables").tag("minecraft:maintains_farmland")
                .build();

        ItemBuilder pb = registry.item(name).naming("self", name).compostable(0.65F)
                .tag("c:crops/" + name);
        if (nutrition >= 0) pb.food(nutrition, saturation);
        if (!separateSeeds) {
            pb.factory(p -> new ItemNameBlockItem(crop.get(), p)).tag("minecraft:villager_plantable_seeds");
        }
        if (tab != null) pb.tab(tab);
        RegistrySupplier<Item> produceItem = produce.apply(pb).build();
        registry.addItemTag(ResourceLocation.fromNamespaceAndPath("c", "crops"), "#c:crops/" + name);
        if (nutrition >= 0) registry.addItemTag(ResourceLocation.fromNamespaceAndPath("c", "foods"), produceItem.getId().toString());

        RegistrySupplier<Item> seeds = produceItem;
        if (separateSeeds) {
            ItemBuilder sb = registry.item(name + "_seeds").naming("seeds", name).compostable(0.3F)
                    .factory(p -> new ItemNameBlockItem(crop.get(), p))
                    .tag("c:seeds/" + name).tag("minecraft:villager_plantable_seeds");
            if (tab != null) sb.tab(tab);
            seeds = sb.build();
            registry.addItemTag(ResourceLocation.fromNamespaceAndPath("c", "seeds"), "#c:seeds/" + name);
        }
        seedRef[0] = seeds;
        registry.cropLoot(crop, produceItem, separateSeeds ? seeds : null);
        return new CropSet(crop, produceItem, seeds);
    }

    /**
     * What {@link CropBuilder} creates.
     * @param crop    the crop block
     * @param produce the harvested item
     * @param seeds   the seeds; the same as {@code produce} with {@code plantedByProduce()}
     */
    public record CropSet(RegistrySupplier<Block> crop, RegistrySupplier<Item> produce, RegistrySupplier<Item> seeds) {
    }
}
