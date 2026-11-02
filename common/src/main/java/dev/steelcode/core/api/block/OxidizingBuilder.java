// SPDX-License-Identifier: LGPL-3.0-or-later
package dev.steelcode.core.api.block;

import dev.architectury.registry.registries.RegistrySupplier;
import dev.steelcode.core.api.data.Recipes;
import dev.steelcode.core.impl.platform.CorePlatform;
import dev.steelcode.core.impl.registry.RegistryContext;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.WeatheringCopper;
import net.minecraft.world.level.block.WeatheringCopperFullBlock;
import net.minecraft.world.level.block.WeatheringCopperSlabBlock;
import net.minecraft.world.level.block.WeatheringCopperStairBlock;
import org.jetbrains.annotations.ApiStatus;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Blocks that weather like copper: four oxidation stages, waxed versions (honeycomb), axe scraping and
 * de-oxidation by lightning. Obtain one with {@code REG.oxidizing(name)}.
 * <pre>{@code
 * OxidizingBuilder.OxidizingSet BRONZE = REG.oxidizing("bronze_plating").copy(Blocks.COPPER_BLOCK)
 *         .stairsAndSlab().build();
 * }</pre>
 * Creates {@code bronze_plating}, {@code exposed_bronze_plating}, {@code weathered_bronze_plating},
 * {@code oxidized_bronze_plating} and their {@code waxed_} versions (plus stairs and slabs of each).
 * Textures: one per stage ({@code block/exposed_bronze_plating}...); waxed blocks reuse their stage's texture.
 * <p>
 * On Fabric the chains are registered with {@code OxidizableBlocksRegistry}; on NeoForge datagen writes the
 * {@code neoforge:oxidizables} and {@code neoforge:waxables} data maps.
 */
public final class OxidizingBuilder {

    private static final String[] PREFIX = {"", "exposed_", "weathered_", "oxidized_"};
    private static final String[] PATTERN = {null, "exposed", "weathered", "oxidized"};

    private final RegistryContext registry;
    private final String name;
    private Block copy = Blocks.COPPER_BLOCK;
    private boolean stairsAndSlab;
    private MiningTool.Tier tier = MiningTool.Tier.STONE;

    /** Internal: use {@code REG.oxidizing(name)}. */
    @ApiStatus.Internal
    public OxidizingBuilder(RegistryContext registry, String name) {
        this.registry = registry;
        this.name = Objects.requireNonNull(name);
    }

    /**
     * Properties copied by every stage (default {@code Blocks.COPPER_BLOCK}).
     * @param block the block to copy
     * @return this builder
     */
    public OxidizingBuilder copy(Block block) {
        this.copy = block;
        return this;
    }

    /**
     * Adds stairs and a slab to every stage; they weather and wax too.
     * @return this builder
     */
    public OxidizingBuilder stairsAndSlab() {
        this.stairsAndSlab = true;
        return this;
    }

    /**
     * Minimum pickaxe tier (default stone).
     * @param tier the tier
     * @return this builder
     */
    public OxidizingBuilder tier(MiningTool.Tier tier) {
        this.tier = tier;
        return this;
    }

    /**
     * Registers every stage and queues the waxing recipes and datagen.
     * @return the created blocks
     */
    public OxidizingSet build() {
        WeatheringCopper.WeatherState[] states = WeatheringCopper.WeatherState.values();
        Map<WeatheringCopper.WeatherState, RegistrySupplier<Block>> blocks = new EnumMap<>(WeatheringCopper.WeatherState.class);
        Map<WeatheringCopper.WeatherState, RegistrySupplier<Block>> waxed = new EnumMap<>(WeatheringCopper.WeatherState.class);
        List<RegistrySupplier<Block>[]> pairs = new ArrayList<>();
        Map<WeatheringCopper.WeatherState, RegistrySupplier<Block>> stairsByStage = new EnumMap<>(WeatheringCopper.WeatherState.class);
        Map<WeatheringCopper.WeatherState, RegistrySupplier<Block>> slabByStage = new EnumMap<>(WeatheringCopper.WeatherState.class);
        Recipes r = registry.autoRecipes();
        for (int i = 0; i < states.length; i++) {
            WeatheringCopper.WeatherState st = states[i];
            String id = PREFIX[i] + name;
            String waxPattern = i == 0 ? "waxed" : "waxed_" + PATTERN[i];
            RegistrySupplier<Block> b = block(id, PATTERN[i], p -> new WeatheringCopperFullBlock(st, p), "block/" + id);
            RegistrySupplier<Block> w = block("waxed_" + id, waxPattern, Block::new, "block/" + id);
            blocks.put(st, b);
            waxed.put(st, w);
            pairs.add(pair(b, w));
            r.shapeless(w).category("building").requires(b).requires("minecraft:honeycomb").save("waxed_" + id + "_from_honeycomb");
            if (stairsAndSlab) {
                RegistrySupplier<Block> stairs = shape(id + "_stairs", "stairs", PATTERN[i], Variant.STAIRS, b,
                        p -> new WeatheringCopperStairBlock(st, b.get().defaultBlockState(), p));
                RegistrySupplier<Block> slab = shape(id + "_slab", "slab", PATTERN[i], Variant.SLAB, b,
                        p -> new WeatheringCopperSlabBlock(st, p));
                RegistrySupplier<Block> wStairs = shape("waxed_" + id + "_stairs", "stairs", waxPattern, Variant.STAIRS, b,
                        p -> new StairBlock(b.get().defaultBlockState(), p));
                RegistrySupplier<Block> wSlab = shape("waxed_" + id + "_slab", "slab", waxPattern, Variant.SLAB, b, SlabBlock::new);
                r.stairs(stairs, b);
                r.slab(slab, b);
                r.shapeless(wStairs).category("building").requires(stairs).requires("minecraft:honeycomb")
                        .save("waxed_" + id + "_stairs_from_honeycomb");
                r.shapeless(wSlab).category("building").requires(slab).requires("minecraft:honeycomb")
                        .save("waxed_" + id + "_slab_from_honeycomb");
                pairs.add(pair(stairs, wStairs));
                pairs.add(pair(slab, wSlab));
                stairsByStage.put(st, stairs);
                slabByStage.put(st, slab);
            }
        }
        // Oxidation chains: stage i -> stage i+1 (blocks, stairs and slabs)
        for (int i = 0; i < states.length - 1; i++) {
            link(blocks.get(states[i]), blocks.get(states[i + 1]));
            if (stairsAndSlab) {
                link(stairsByStage.get(states[i]), stairsByStage.get(states[i + 1]));
                link(slabByStage.get(states[i]), slabByStage.get(states[i + 1]));
            }
        }
        for (RegistrySupplier<Block>[] p : pairs) {
            RegistrySupplier<Block> plain = p[0], wax = p[1];
            registry.waxable(plain, wax);
            registry.afterRegister(() -> plain.listen(a -> wax.listen(c -> CorePlatform.waxable(a, c))));
        }
        return new OxidizingSet(blocks, waxed);
    }

    @SuppressWarnings("unchecked")
    private static RegistrySupplier<Block>[] pair(RegistrySupplier<Block> a, RegistrySupplier<Block> b) {
        return new RegistrySupplier[]{a, b};
    }

    private void link(RegistrySupplier<Block> from, RegistrySupplier<Block> to) {
        registry.oxidizable(from, to);
        registry.afterRegister(() -> from.listen(a -> to.listen(b -> CorePlatform.oxidizable(a, b))));
    }

    private RegistrySupplier<Block> block(String id, String pattern, java.util.function.Function<net.minecraft.world.level.block.state.BlockBehaviour.Properties, ? extends Block> f, String texture) {
        BlockBuilder b = registry.block(id).copy(copy).factory(f).texture(texture).tool(MiningTool.PICKAXE, tier);
        if (pattern == null) b.naming("self", name);
        else b.naming(pattern, name);
        return b.build();
    }

    private RegistrySupplier<Block> shape(String id, String pattern, String stage, Variant variant, RegistrySupplier<Block> base,
                                          java.util.function.Function<net.minecraft.world.level.block.state.BlockBehaviour.Properties, ? extends Block> f) {
        BlockBuilder b = registry.block(id).shape(variant, base).factory(f).tool(MiningTool.PICKAXE, tier);
        b.namingModified(pattern, name, stage == null ? null : "@" + stage);
        return b.build();
    }

    /**
     * Blocks created by {@link OxidizingBuilder}, keyed by weathering stage.
     * @param blocks the four unwaxed full blocks, from {@code UNAFFECTED} to {@code OXIDIZED}
     * @param waxed  the four waxed full blocks
     */
    public record OxidizingSet(Map<WeatheringCopper.WeatherState, RegistrySupplier<Block>> blocks,
                               Map<WeatheringCopper.WeatherState, RegistrySupplier<Block>> waxed) {
    }
}
