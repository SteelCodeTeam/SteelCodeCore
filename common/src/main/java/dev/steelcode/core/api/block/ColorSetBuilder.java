// SPDX-License-Identifier: LGPL-3.0-or-later
package dev.steelcode.core.api.block;

import dev.architectury.registry.registries.DeferredSupplier;
import dev.architectury.registry.registries.RegistrySupplier;
import dev.steelcode.core.impl.registry.RegistryContext;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import org.jetbrains.annotations.ApiStatus;

import java.util.Arrays;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.UnaryOperator;

/**
 * Sixteen dye-colored blocks ({@code white_marble} ... {@code black_marble}). Obtain one with {@code REG.colors(name)}.
 * <pre>{@code
 * // One grayscale texture (block/marble.png) tinted in code for every color
 * ColorSet MARBLE = REG.colors("marble").copy(Blocks.CALCITE).tinted().dyeFrom(MARBLE_BLOCK).build();
 *
 * // One texture per color (block/red_tiles.png...) plus stairs and slabs for each
 * ColorSet TILES = REG.colors("tiles").copy(Blocks.TERRACOTTA).family(f -> f.stairs().slab()).build();
 *
 * // Stained glass
 * ColorSet GLASS = REG.colors("frosted_glass").copy(Blocks.GLASS).renderLayer(RenderLayer.TRANSLUCENT)
 *         .drops(Drops.silkTouchOnly()).build();
 * }</pre>
 */
public final class ColorSetBuilder {

    private final RegistryContext registry;
    private final String name;
    private Block copy = Blocks.STONE;
    private boolean tinted;
    private RenderLayer renderLayer;
    private Drops drops = Drops.self();
    private int light = -1;
    private MiningTool tool = MiningTool.PICKAXE;
    private MiningTool.Tier tier = MiningTool.Tier.ANY;
    private int burnOdds = -1;
    private int spreadOdds;
    private Object dyeFrom;
    private UnaryOperator<BlockFamilyBuilder> family;
    private Set<DyeColor> colors = EnumSet.allOf(DyeColor.class);
    private DeferredSupplier<CreativeModeTab> tab;

    /** Internal: use {@code REG.colors(name)}. */
    @ApiStatus.Internal
    public ColorSetBuilder(RegistryContext registry, String name) {
        this.registry = registry;
        this.name = Objects.requireNonNull(name);
    }

    /**
     * Copies properties (hardness, sound...) from a block.
     * @param block the block to copy
     * @return this builder
     */
    public ColorSetBuilder copy(Block block) {
        this.copy = Objects.requireNonNull(block);
        return this;
    }

    /**
     * Uses one grayscale texture {@code block/<name>} tinted with each dye color, instead of one texture per color.
     * @return this builder
     */
    public ColorSetBuilder tinted() {
        this.tinted = true;
        return this;
    }

    /**
     * Render layer of every block.
     * @param layer the layer
     * @return this builder
     */
    public ColorSetBuilder renderLayer(RenderLayer layer) {
        this.renderLayer = layer;
        return this;
    }

    /**
     * Drop rule of every block.
     * @param drops the drop rule
     * @return this builder
     */
    public ColorSetBuilder drops(Drops drops) {
        this.drops = Objects.requireNonNull(drops);
        return this;
    }

    /**
     * Light level of every block.
     * @param level 0-15
     * @return this builder
     */
    public ColorSetBuilder light(int level) {
        this.light = level;
        return this;
    }

    /**
     * Tool and minimum tier of every block.
     * @param tool the tool
     * @param tier minimum tier
     * @return this builder
     */
    public ColorSetBuilder tool(MiningTool tool, MiningTool.Tier tier) {
        this.tool = tool;
        this.tier = Objects.requireNonNull(tier);
        return this;
    }

    /**
     * Makes every block burn.
     * @param burnOdds   how easily it catches fire
     * @param spreadOdds how easily it is destroyed by fire
     * @return this builder
     */
    public ColorSetBuilder flammable(int burnOdds, int spreadOdds) {
        this.burnOdds = burnOdds;
        this.spreadOdds = spreadOdds;
        return this;
    }

    /**
     * Adds dyeing recipes: 8 {@code base} around a dye give 8 colored blocks (like stained glass).
     * @param base the undyed block: item, supplier, id or {@code "#tag"}
     * @return this builder
     */
    public ColorSetBuilder dyeFrom(Object base) {
        this.dyeFrom = Objects.requireNonNull(base);
        return this;
    }

    /**
     * Creates a block family for each color. Only works with one texture per color, not with {@link #tinted()}.
     * @param family configures each color's family, e.g. {@code f -> f.stairs().slab()}
     * @return this builder
     */
    public ColorSetBuilder family(UnaryOperator<BlockFamilyBuilder> family) {
        this.family = Objects.requireNonNull(family);
        return this;
    }

    /**
     * Creates only some colors instead of all sixteen.
     * @param colors the colors to create
     * @return this builder
     */
    public ColorSetBuilder only(DyeColor... colors) {
        this.colors = EnumSet.copyOf(Arrays.asList(colors));
        return this;
    }

    /**
     * Creative tab of every block.
     * @param tab the tab
     * @return this builder
     */
    public ColorSetBuilder tab(DeferredSupplier<CreativeModeTab> tab) {
        this.tab = tab;
        return this;
    }

    // ================================================================ build

    /**
     * Registers the blocks (and families) and queues their datagen.
     * @return the color set
     */
    public ColorSet build() {
        if (tinted && family != null) {
            throw new IllegalStateException(name + ": family() needs one texture per color (remove tinted())");
        }
        ResourceLocation tag = ResourceLocation.fromNamespaceAndPath(registry.modId(), name);
        Map<DyeColor, RegistrySupplier<Block>> blocks = new EnumMap<>(DyeColor.class);
        Map<DyeColor, BlockFamily> families = new EnumMap<>(DyeColor.class);

        for (DyeColor color : colors) {
            String id = color.getName() + "_" + name;
            BlockBuilder b = registry.block(id).copy(copy).namingColored(name, color.getName())
                    .drops(drops).tool(tool, tier).tag(tag.toString()).itemTag(tag.toString());
            if (tinted) {
                b.model(Variant.TINTED_CUBE).texture("block/" + name).tint(color.getTextureDiffuseColor() & 0xFFFFFF);
            }
            if (renderLayer != null) b.renderLayer(renderLayer);
            if (light >= 0) b.light(light);
            if (burnOdds >= 0) b.flammable(burnOdds, spreadOdds);
            if (tab != null) b.tab(tab);
            RegistrySupplier<Block> block = b.build();
            blocks.put(color, block);

            if (dyeFrom != null) {
                registry.autoRecipes().surround(block, 8, "minecraft:" + color.getName() + "_dye", dyeFrom);
            }
            if (family != null) {
                BlockFamilyBuilder fb = registry.family().base(block).name(id).colorNaming(name, color.getName())
                        .tool(tool, tier);
                if (renderLayer != null) fb.renderLayer(renderLayer);
                if (light >= 0) fb.light(light);
                if (tab != null) fb.tab(tab);
                families.put(color, family.apply(fb).build());
            }
        }
        ColorSet set = new ColorSet(name, blocks, families, tag);
        registry.addColorSet(set);
        return set;
    }

}
