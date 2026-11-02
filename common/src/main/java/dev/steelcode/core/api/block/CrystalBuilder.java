// SPDX-License-Identifier: LGPL-3.0-or-later
package dev.steelcode.core.api.block;

import dev.architectury.registry.registries.RegistrySupplier;
import dev.steelcode.core.content.block.CoreBuddingBlock;
import dev.steelcode.core.impl.registry.RegistryContext;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.AmethystBlock;
import net.minecraft.world.level.block.AmethystClusterBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import org.jetbrains.annotations.ApiStatus;

import java.util.Objects;

/**
 * Crystals that grow like amethyst. Obtain one with {@code REG.crystal(name)}.
 * <pre>{@code
 * CrystalBuilder.CrystalSet JADE = REG.crystal("jade").build();
 * REG.worldgen().geode("jade_geode").inner(JADE.block()).budding(JADE.budding())
 *         .buds(JADE.small(), JADE.medium(), JADE.large(), JADE.cluster()).build();
 * }</pre>
 * Creates {@code jade_block}, {@code budding_jade}, {@code small_jade_bud}, {@code medium_jade_bud},
 * {@code large_jade_bud}, {@code jade_cluster} and {@code jade_shard} (unless you pass your own with {@link #shard}).
 * Clusters drop shards like amethyst; the budding block cannot be picked up and grows buds on random ticks.
 */
public final class CrystalBuilder {

    private final RegistryContext registry;
    private final String name;
    private Object shard;

    /** Internal: use {@code REG.crystal(name)}. */
    @ApiStatus.Internal
    public CrystalBuilder(RegistryContext registry, String name) {
        this.registry = registry;
        this.name = Objects.requireNonNull(name);
    }

    /**
     * Uses an existing item as the shard instead of creating {@code <name>_shard}.
     * @param item item, supplier or id
     * @return this builder
     */
    public CrystalBuilder shard(Object item) {
        this.shard = item;
        return this;
    }

    /**
     * Registers every block (and the shard) and queues their datagen.
     * @return the created blocks
     */
    @SuppressWarnings("unchecked")
    public CrystalSet build() {
        Object shardItem = shard;
        RegistrySupplier<Item> created = null;
        if (shardItem == null) {
            created = registry.item(name + "_shard").naming("shard", name).build();
            shardItem = created;
        }
        RegistrySupplier<Block> block = registry.block(name + "_block").copy(Blocks.AMETHYST_BLOCK).factory(AmethystBlock::new)
                .naming("block", name).tool(MiningTool.PICKAXE).build();
        registry.autoRecipes().storage2x2(block, shardItem, false);
        RegistrySupplier<Block>[] stages = new RegistrySupplier[4];
        RegistrySupplier<Block> budding = registry.block("budding_" + name).copy(Blocks.BUDDING_AMETHYST)
                .factory(p -> new CoreBuddingBlock(p, () -> stages[0].get(), () -> stages[1].get(), () -> stages[2].get(), () -> stages[3].get()))
                .naming("budding", name).drops(Drops.none()).tool(MiningTool.PICKAXE).build();
        stages[0] = bud("small_" + name + "_bud", "small_bud", Blocks.SMALL_AMETHYST_BUD, 3, 4, "small_amethyst_bud");
        stages[1] = bud("medium_" + name + "_bud", "medium_bud", Blocks.MEDIUM_AMETHYST_BUD, 4, 3, "medium_amethyst_bud");
        stages[2] = bud("large_" + name + "_bud", "large_bud", Blocks.LARGE_AMETHYST_BUD, 5, 3, "large_amethyst_bud");
        Object s = shardItem;
        stages[3] = registry.block(name + "_cluster").copy(Blocks.AMETHYST_CLUSTER)
                .factory(p -> new AmethystClusterBlock(7, 3, p)).model(Variant.CROSS).param(1, 0)
                .renderLayer(RenderLayer.CUTOUT).naming("cluster", name).tool(MiningTool.PICKAXE)
                .drops(Drops.likeVanilla("amethyst_cluster", "minecraft:amethyst_cluster", Drops.SELF, "minecraft:amethyst_shard", s))
                .build();
        return new CrystalSet(block, budding, stages[0], stages[1], stages[2], stages[3], created);
    }

    private RegistrySupplier<Block> bud(String id, String pattern, Block copy, float height, float offset, String vanilla) {
        return registry.block(id).copy(copy).factory(p -> new AmethystClusterBlock(height, offset, p)).model(Variant.CROSS)
                .param(1, 0).renderLayer(RenderLayer.CUTOUT).naming(pattern, name).tool(MiningTool.PICKAXE)
                .drops(Drops.likeVanilla(vanilla, "minecraft:" + vanilla, Drops.SELF)).build();
    }

    /**
     * Blocks created by {@link CrystalBuilder}.
     * @param block   the crystal block
     * @param budding the budding block that grows buds
     * @param small   the small bud
     * @param medium  the medium bud
     * @param large   the large bud
     * @param cluster the fully grown cluster
     * @param shard   the created shard, or {@code null} if you supplied your own with {@link #shard(Object)}
     */
    public record CrystalSet(RegistrySupplier<Block> block, RegistrySupplier<Block> budding, RegistrySupplier<Block> small,
                             RegistrySupplier<Block> medium, RegistrySupplier<Block> large, RegistrySupplier<Block> cluster,
                             RegistrySupplier<Item> shard) {
    }
}
