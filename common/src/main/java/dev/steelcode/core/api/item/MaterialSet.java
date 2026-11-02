// SPDX-License-Identifier: LGPL-3.0-or-later
package dev.steelcode.core.api.item;

import dev.architectury.registry.registries.RegistrySupplier;
import dev.steelcode.core.api.block.BlockFamily;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;

import java.util.Optional;

/**
 * The result of {@link MaterialBuilder#build()}. Immutable.
 * <p>
 * The plain getters throw if the material lacks that piece (e.g. {@code nugget()} on a gem); use the
 * {@code ...Opt()} variants when you are not sure.
 */
public final class MaterialSet {

    private final String name;
    private final MaterialBuilder.Type type;
    private final RegistrySupplier<Item> main;
    private final RegistrySupplier<Item> nugget;
    private final RegistrySupplier<Item> raw;
    private final RegistrySupplier<Block> block;
    private final RegistrySupplier<Block> rawBlock;
    private final RegistrySupplier<Block> ore;
    private final RegistrySupplier<Block> deepslateOre;
    private final RegistrySupplier<Block> netherOre;
    private final BlockFamily family;
    private final EquipmentSet equipment;

    MaterialSet(String name, MaterialBuilder.Type type, RegistrySupplier<Item> main, RegistrySupplier<Item> nugget,
                RegistrySupplier<Item> raw, RegistrySupplier<Block> block, RegistrySupplier<Block> rawBlock,
                RegistrySupplier<Block> ore, RegistrySupplier<Block> deepslateOre, RegistrySupplier<Block> netherOre,
                BlockFamily family, EquipmentSet equipment) {
        this.name = name;
        this.type = type;
        this.main = main;
        this.nugget = nugget;
        this.raw = raw;
        this.block = block;
        this.rawBlock = rawBlock;
        this.ore = ore;
        this.deepslateOre = deepslateOre;
        this.netherOre = netherOre;
        this.family = family;
        this.equipment = equipment;
    }

    /**
     * @return the material name, e.g. {@code "tin"}
     */
    public String name() { return name; }
    /**
     * @return whether it is a metal or a gem
     */
    public MaterialBuilder.Type type() { return type; }

    /**
     * @return the ingot (metal) or gem
     */
    public RegistrySupplier<Item> main() { return main; }
    /**
     * @return the ingot
     * @throws IllegalStateException if this is a gem
     */
    public RegistrySupplier<Item> ingot() { return need(type == MaterialBuilder.Type.METAL ? main : null, "ingot"); }
    /**
     * @return the gem
     * @throws IllegalStateException if this is a metal
     */
    public RegistrySupplier<Item> gem() { return need(type == MaterialBuilder.Type.GEM ? main : null, "gem"); }
    /**
     * @return the nugget
     * @throws IllegalStateException if the material has none
     */
    public RegistrySupplier<Item> nugget() { return need(nugget, "nugget"); }
    /**
     * @return the raw material ({@code raw_<name>})
     * @throws IllegalStateException if the material has none
     */
    public RegistrySupplier<Item> raw() { return need(raw, "raw"); }
    /**
     * @return the storage block ({@code <name>_block})
     */
    public RegistrySupplier<Block> block() { return block; }
    /**
     * @return the raw block
     * @throws IllegalStateException if the material has none
     */
    public RegistrySupplier<Block> rawBlock() { return need(rawBlock, "rawBlock"); }
    /**
     * @return the stone ore
     * @throws IllegalStateException if the material has none
     */
    public RegistrySupplier<Block> ore() { return need(ore, "ore"); }
    /**
     * @return the deepslate ore
     * @throws IllegalStateException if the material has none
     */
    public RegistrySupplier<Block> deepslateOre() { return need(deepslateOre, "deepslateOre"); }
    /**
     * @return the nether ore
     * @throws IllegalStateException if the material has none
     */
    public RegistrySupplier<Block> netherOre() { return need(netherOre, "netherOre"); }
    /**
     * @return the block family
     * @throws IllegalStateException if none was created
     */
    public BlockFamily family() { return need(family, "family"); }
    /**
     * @return the tools and armor created with {@code .tools()} / {@code .armor()}
     * @throws IllegalStateException if none were created
     */
    public EquipmentSet equipment() { return need(equipment, "tools/armor"); }

    /**
     * @return the nugget, if the material has one
     */
    public Optional<RegistrySupplier<Item>> nuggetOpt() { return Optional.ofNullable(nugget); }
    /**
     * @return the raw, if the material has one
     */
    public Optional<RegistrySupplier<Item>> rawOpt() { return Optional.ofNullable(raw); }
    /**
     * @return the ore, if the material has one
     */
    public Optional<RegistrySupplier<Block>> oreOpt() { return Optional.ofNullable(ore); }
    /**
     * @return the deepslateOre, if the material has one
     */
    public Optional<RegistrySupplier<Block>> deepslateOreOpt() { return Optional.ofNullable(deepslateOre); }
    /**
     * @return the netherOre, if the material has one
     */
    public Optional<RegistrySupplier<Block>> netherOreOpt() { return Optional.ofNullable(netherOre); }
    /**
     * @return the block family, if one was created
     */
    public Optional<BlockFamily> familyOpt() { return Optional.ofNullable(family); }

    private <T> T need(T value, String what) {
        if (value == null) throw new IllegalStateException("Material '" + name + "' has no " + what);
        return value;
    }
}
