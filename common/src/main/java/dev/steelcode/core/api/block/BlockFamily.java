// SPDX-License-Identifier: LGPL-3.0-or-later
package dev.steelcode.core.api.block;

import dev.architectury.registry.registries.RegistrySupplier;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;

import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;

/**
 * The result of {@link BlockFamilyBuilder#build()}: the base block and its variants. Immutable.
 * <pre>{@code
 * Block stairs = GOLD.stairs().get();
 * GOLD.get(Variant.DOOR).ifPresent(door -> ...);
 * }</pre>
 * The typed getters ({@link #stairs()}, {@link #slab()}...) throw if the family lacks that variant;
 * use {@link #get(Variant)} when it is optional.
 */
public final class BlockFamily {

    private final String prefix;
    private final ResourceLocation baseId;
    private final Supplier<? extends Block> base;
    private final Map<Variant, RegistrySupplier<Block>> blocks;

    BlockFamily(String prefix, ResourceLocation baseId, Supplier<? extends Block> base,
                Map<Variant, RegistrySupplier<Block>> blocks) {
        this.prefix = prefix;
        this.baseId = baseId;
        this.base = base;
        this.blocks = Collections.unmodifiableMap(new EnumMap<>(blocks));
    }

    /**
     * @return the id prefix of the variants, e.g. {@code "gold"}
     */
    public String prefix() { return prefix; }
    /**
     * @return the id of the base block
     */
    public ResourceLocation baseId() { return baseId; }

    /**
     * @return the base block (vanilla, from another mod, or created by the family)
     */
    public Block base() { return base.get(); }

    /**
     * @param variant a variant
     * @return whether the family registered it
     */
    public boolean has(Variant variant) { return blocks.containsKey(variant); }

    /**
     * @param variant a variant
     * @return the block, or empty if the family does not have it
     */
    public Optional<RegistrySupplier<Block>> get(Variant variant) {
        return Optional.ofNullable(blocks.get(variant));
    }

    /**
     * @param variant a variant
     * @return the block
     * @throws IllegalStateException if the family does not have that variant
     */
    public RegistrySupplier<Block> require(Variant variant) {
        RegistrySupplier<Block> block = blocks.get(variant);
        if (block == null) {
            throw new IllegalStateException("Family '" + prefix + "' has no variant " + variant);
        }
        return block;
    }

    /**
     * @return every variant registered by the family (includes {@link Variant#BASE} if the family created the base), unmodifiable
     */
    public Map<Variant, RegistrySupplier<Block>> blocks() { return blocks; }

    /**
     * @return the {@link Variant#STAIRS} block
     * @throws IllegalStateException if the family does not have it
     */
    public RegistrySupplier<Block> stairs() { return require(Variant.STAIRS); }
    /**
     * @return the {@link Variant#SLAB} block
     * @throws IllegalStateException if the family does not have it
     */
    public RegistrySupplier<Block> slab() { return require(Variant.SLAB); }
    /**
     * @return the {@link Variant#VERTICAL_SLAB} block
     * @throws IllegalStateException if the family does not have it
     */
    public RegistrySupplier<Block> verticalSlab() { return require(Variant.VERTICAL_SLAB); }
    /**
     * @return the {@link Variant#WALL} block
     * @throws IllegalStateException if the family does not have it
     */
    public RegistrySupplier<Block> wall() { return require(Variant.WALL); }
    /**
     * @return the {@link Variant#FENCE} block
     * @throws IllegalStateException if the family does not have it
     */
    public RegistrySupplier<Block> fence() { return require(Variant.FENCE); }
    /**
     * @return the {@link Variant#FENCE_GATE} block
     * @throws IllegalStateException if the family does not have it
     */
    public RegistrySupplier<Block> fenceGate() { return require(Variant.FENCE_GATE); }
    /**
     * @return the {@link Variant#BUTTON} block
     * @throws IllegalStateException if the family does not have it
     */
    public RegistrySupplier<Block> button() { return require(Variant.BUTTON); }
    /**
     * @return the {@link Variant#PRESSURE_PLATE} block
     * @throws IllegalStateException if the family does not have it
     */
    public RegistrySupplier<Block> pressurePlate() { return require(Variant.PRESSURE_PLATE); }
    /**
     * @return the {@link Variant#DOOR} block
     * @throws IllegalStateException if the family does not have it
     */
    public RegistrySupplier<Block> door() { return require(Variant.DOOR); }
    /**
     * @return the {@link Variant#TRAPDOOR} block
     * @throws IllegalStateException if the family does not have it
     */
    public RegistrySupplier<Block> trapdoor() { return require(Variant.TRAPDOOR); }
    /**
     * @return the {@link Variant#PANE} block
     * @throws IllegalStateException if the family does not have it
     */
    public RegistrySupplier<Block> pane() { return require(Variant.PANE); }
    /**
     * @return the {@link Variant#CARPET} block
     * @throws IllegalStateException if the family does not have it
     */
    public RegistrySupplier<Block> carpet() { return require(Variant.CARPET); }
}
