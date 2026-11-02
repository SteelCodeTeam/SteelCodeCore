// SPDX-License-Identifier: LGPL-3.0-or-later
package dev.steelcode.core.api.item;

import dev.architectury.registry.registries.DeferredSupplier;
import dev.architectury.registry.registries.RegistrySupplier;
import dev.steelcode.core.api.block.BlockBuilder;
import dev.steelcode.core.api.block.BlockFamily;
import dev.steelcode.core.api.block.BlockFamilyBuilder;
import dev.steelcode.core.api.block.Drops;
import dev.steelcode.core.api.block.FamilyPreset;
import dev.steelcode.core.api.block.MiningTool;
import dev.steelcode.core.api.data.Recipes;
import dev.steelcode.core.api.world.Worldgen;
import dev.steelcode.core.impl.registry.RegistryContext;
import dev.steelcode.core.impl.util.Refs;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import org.jetbrains.annotations.ApiStatus;

import java.util.Objects;
import java.util.function.UnaryOperator;

/**
 * A complete material in one line: items, blocks, ores, recipes and the common {@code c:} tags that other mods
 * use on both Fabric and NeoForge. Obtain one with {@code REG.material(name)}.
 * <pre>{@code
 * // Metal: tin_ingot, tin_nugget, raw_tin, raw_tin_block, tin_block, tin_ore, deepslate_tin_ore
 * MaterialSet TIN = REG.material("tin").metal().tier(MiningTool.Tier.STONE).build();
 *
 * // Gem with XP and a block family derived from the ruby block
 * MaterialSet RUBY = REG.material("ruby").gem().tier(MiningTool.Tier.IRON)
 *         .family(f -> f.createAll())
 *         .build();
 *
 * // Twenty metals from an enum, with tools, armor and world generation
 * for (Metal m : Metal.values()) {
 *     METALS.put(m, REG.material(m.id()).metal().tier(m.tier())
 *             .tools(ToolStats.IRON).armor(ArmorStats.IRON)
 *             .generate(9, 10, -24, 56)
 *             .build());
 * }
 * }</pre>
 * Expected textures: {@code item/<id>} for items and {@code block/<id>} for blocks. Recipes include ingot ↔ nugget ↔
 * block crafting, ore and raw smelting and blasting, and the tool and armor recipes.
 */
public final class MaterialBuilder {

    /** Kind of material. */
    public enum Type {
        /** Ingot, nugget, raw material and their blocks. */
        METAL,
        /** Gem and its block. */
        GEM
    }

    private final RegistryContext registry;
    private final String name;
    private Type type = Type.METAL;
    private MiningTool.Tier tier;
    private boolean stoneOre = true;
    private boolean deepslateOre = true;
    private boolean netherOre;
    private boolean nugget = true;
    private boolean raw = true;
    private int oreXpMin = -1;
    private int oreXpMax = -1;
    private float dropMin = 1;
    private float dropMax = 1;
    private float smeltXp = -1;
    private int light = -1;
    private int fuel;
    private boolean beacon = true;
    private boolean fireResistant;
    private String displayName;
    private UnaryOperator<BlockFamilyBuilder> family;
    private DeferredSupplier<CreativeModeTab> tab;
    private ToolStats tools;
    private ArmorStats armor;
    private UnaryOperator<Worldgen.OreBuilder> oreGen;

    /** Internal: use {@code REG.material(name)}. */
    @ApiStatus.Internal
    public MaterialBuilder(RegistryContext registry, String name) {
        this.registry = registry;
        this.name = Objects.requireNonNull(name);
    }

    /**
     * Makes it a metal ({@link Type#METAL}, the default).
     * @return this builder
     */
    public MaterialBuilder metal() {
        this.type = Type.METAL;
        return this;
    }

    /**
     * Makes it a gem ({@link Type#GEM}): no nugget or raw material, ores drop the gem with XP.
     * @return this builder
     */
    public MaterialBuilder gem() {
        this.type = Type.GEM;
        return this;
    }

    /**
     * Pickaxe tier for ores and blocks. Defaults to stone for metals and iron for gems.
     * @param tier the tier
     * @return this builder
     */
    public MaterialBuilder tier(MiningTool.Tier tier) {
        this.tier = Objects.requireNonNull(tier);
        return this;
    }

    /**
     * Which ores to create (default: stone and deepslate).
     * @param stone     {@code <name>_ore}
     * @param deepslate {@code deepslate_<name>_ore}
     * @param nether    {@code nether_<name>_ore}
     * @return this builder
     */
    public MaterialBuilder ores(boolean stone, boolean deepslate, boolean nether) {
        this.stoneOre = stone;
        this.deepslateOre = deepslate;
        this.netherOre = nether;
        return this;
    }

    /**
     * Creates no ores (alloys like bronze or steel).
     * @return this builder
     */
    public MaterialBuilder noOres() {
        return ores(false, false, false);
    }

    /**
     * Creates no raw material or raw block. Ores then drop the ingot directly.
     * @return this builder
     */
    public MaterialBuilder noRaw() {
        this.raw = false;
        return this;
    }

    /**
     * Creates no nugget.
     * @return this builder
     */
    public MaterialBuilder noNugget() {
        this.nugget = false;
        return this;
    }

    /**
     * An alloy: no ores and no raw material.
     * @return this builder
     */
    public MaterialBuilder alloy() {
        return noOres().noRaw();
    }

    /**
     * Experience dropped by ores. Defaults to 0 for metals and 3-7 for gems.
     * @param min minimum XP
     * @param max maximum XP
     * @return this builder
     */
    public MaterialBuilder oreXp(int min, int max) {
        this.oreXpMin = min;
        this.oreXpMax = max;
        return this;
    }

    /**
     * How many items an ore drops without Silk Touch, before Fortune.
     * @param min minimum count
     * @param max maximum count
     * @return this builder
     */
    public MaterialBuilder oreDrops(float min, float max) {
        this.dropMin = min;
        this.dropMax = max;
        return this;
    }

    /**
     * Experience from smelting. Defaults to 0.7 for metals and 1.0 for gems.
     * @param xp experience per item
     * @return this builder
     */
    public MaterialBuilder smeltingXp(float xp) {
        this.smeltXp = xp;
        return this;
    }

    /**
     * Light level of every block of the material.
     * @param level 0-15
     * @return this builder
     */
    public MaterialBuilder light(int level) {
        this.light = level;
        return this;
    }

    /**
     * Makes the ingot or gem a fuel; the storage block burns ten times as long.
     * @param burnTicks burn time of one item
     * @return this builder
     */
    public MaterialBuilder fuel(int burnTicks) {
        this.fuel = burnTicks;
        return this;
    }

    /**
     * Every item of the material survives fire and lava.
     * @return this builder
     */
    public MaterialBuilder fireResistant() {
        this.fireResistant = true;
        return this;
    }

    /**
     * Keeps the material out of the beacon base and payment tags.
     * @return this builder
     */
    public MaterialBuilder noBeacon() {
        this.beacon = false;
        return this;
    }

    /**
     * Creates the five tools (sword, pickaxe, axe, shovel, hoe), repaired with the ingot or gem.
     * @param stats tool stats, e.g. {@link ToolStats#IRON}
     * @return this builder
     */
    public MaterialBuilder tools(ToolStats stats) {
        this.tools = stats;
        return this;
    }

    /**
     * Creates the four armor pieces. Worn texture: {@code textures/models/armor/<name>_layer_1/2.png}.
     * @param stats armor stats, e.g. {@link ArmorStats#IRON}
     * @return this builder
     */
    public MaterialBuilder armor(ArmorStats stats) {
        this.armor = stats;
        return this;
    }

    /**
     * Generates the ores in the world with a triangular height distribution (iron: {@code 9, 10, -24, 56}).
     * @param size     blocks per vein
     * @param perChunk veins per chunk
     * @param minY     lowest height
     * @param maxY     highest height
     * @return this builder
     */
    public MaterialBuilder generate(int size, int perChunk, int minY, int maxY) {
        return generate(o -> o.size(size).count(perChunk).trapezoid(minY, maxY));
    }

    /**
     * Generates the ores with full control.
     * <pre>{@code .generate(o -> o.size(4).rarity(2).uniform(-64, 16).airExposure(0.5f))}</pre>
     * @param ore configures the ore placement
     * @return this builder
     */
    public MaterialBuilder generate(UnaryOperator<Worldgen.OreBuilder> ore) {
        this.oreGen = Objects.requireNonNull(ore);
        return this;
    }

    /**
     * English name of the material ({@code "Tin"} by default), used in "Tin Ingot", "Block of Tin"...
     * @param displayName the name
     * @return this builder
     */
    public MaterialBuilder displayName(String displayName) {
        this.displayName = displayName;
        return this;
    }

    /**
     * Also creates a block family from the storage block.
     * <pre>{@code .family(f -> f.createAll())}</pre>
     * @param family configures the family
     * @return this builder
     */
    public MaterialBuilder family(UnaryOperator<BlockFamilyBuilder> family) {
        this.family = Objects.requireNonNull(family);
        return this;
    }

    /**
     * Creative tab of every item of the material.
     * @param tab the tab
     * @return this builder
     */
    public MaterialBuilder tab(DeferredSupplier<CreativeModeTab> tab) {
        this.tab = tab;
        return this;
    }

    // ================================================================ build

    /**
     * Registers everything and queues recipes, tags, loot tables and world generation.
     * @return the material, to access its items and blocks
     */
    public MaterialSet build() {
        boolean metal = type == Type.METAL;
        MiningTool.Tier t = tier != null ? tier : (metal ? MiningTool.Tier.STONE : MiningTool.Tier.IRON);
        if (displayName != null) registry.lang("en_us").word(name, displayName);
        int xpMin = oreXpMin >= 0 ? oreXpMin : (metal ? 0 : 3);
        int xpMax = oreXpMax >= 0 ? oreXpMax : (metal ? 0 : 7);
        float sxp = smeltXp >= 0 ? smeltXp : (metal ? 0.7F : 1.0F);
        Recipes r = registry.autoRecipes();

        // ---- items
        String mainId = metal ? name + "_ingot" : name;
        RegistrySupplier<Item> main = item(mainId, metal ? "ingot" : "self", fuel)
                .tag((metal ? "c:ingots/" : "c:gems/") + name).build();
        tagParent(metal ? "c:ingots" : "c:gems", (metal ? "c:ingots/" : "c:gems/") + name);
        if (beacon) registry.addItemTag(Refs.parse("minecraft", "beacon_payment_items"), main.getId().toString());

        RegistrySupplier<Item> nug = null;
        if (metal && nugget) {
            nug = item(name + "_nugget", "nugget", fuel > 0 ? fuel / 9 : 0).tag("c:nuggets/" + name).build();
            tagParent("c:nuggets", "c:nuggets/" + name);
        }
        RegistrySupplier<Item> rawItem = null;
        if (metal && raw) {
            rawItem = item("raw_" + name, "raw", 0).tag("c:raw_materials/" + name).build();
            tagParent("c:raw_materials", "c:raw_materials/" + name);
        }

        // ---- blocks
        RegistrySupplier<Block> storage = block(name + "_block", "block",
                metal ? Blocks.IRON_BLOCK : Blocks.DIAMOND_BLOCK, t)
                .tag("c:storage_blocks/" + name).itemTag("c:storage_blocks/" + name)
                .fuel(fuel * 10)
                .build();
        storageTags(name, storage);
        if (beacon) registry.addBlockTag(Refs.parse("minecraft", "beacon_base_blocks"), storage.getId().toString());

        RegistrySupplier<Block> rawBlock = null;
        if (rawItem != null) {
            rawBlock = block("raw_" + name + "_block", "raw_block", Blocks.RAW_IRON_BLOCK, t)
                    .tag("c:storage_blocks/raw_" + name).itemTag("c:storage_blocks/raw_" + name).build();
            storageTags("raw_" + name, rawBlock);
        }

        Object oreDrop = rawItem != null ? rawItem : main;
        RegistrySupplier<Block> ore = stoneOre
                ? ore(name + "_ore", "ore", metal ? Blocks.IRON_ORE : Blocks.DIAMOND_ORE, "stone", t, xpMin, xpMax, oreDrop)
                : null;
        RegistrySupplier<Block> dOre = deepslateOre
                ? ore("deepslate_" + name + "_ore", "deepslate_ore",
                metal ? Blocks.DEEPSLATE_IRON_ORE : Blocks.DEEPSLATE_DIAMOND_ORE, "deepslate", t, xpMin, xpMax, oreDrop)
                : null;
        RegistrySupplier<Block> nOre = netherOre
                ? ore("nether_" + name + "_ore", "nether_ore", Blocks.NETHER_GOLD_ORE, "netherrack", t, xpMin, xpMax, oreDrop)
                : null;

        // ---- recipes
        r.storage(storage, main);
        if (nug != null) r.nuggets(nug, main);
        if (rawItem != null) {
            r.storage(rawBlock, rawItem);
            r.oreSmelting(rawItem, main, sxp);
        }
        for (RegistrySupplier<Block> o : new RegistrySupplier[]{ore, dOre, nOre}) {
            if (o != null) r.oreSmelting(o, main, sxp);
        }

        // ---- family
        BlockFamily fam = null;
        if (family != null) {
            BlockFamilyBuilder fb = registry.family().base(storage).name(name).preset(FamilyPreset.METAL).tier(t);
            if (light >= 0) fb.light(light);
            if (tab != null) fb.tab(tab);
            fam = family.apply(fb).build();
        }

        // ---- equipment
        EquipmentSet equipment = null;
        if (tools != null || armor != null) {
            EquipmentBuilder eb = registry.equipment(name, main);
            if (tools != null) eb.tools(tools);
            if (armor != null) eb.armor(armor);
            if (fireResistant) eb.fireResistant();
            if (tab != null) eb.tab(tab);
            equipment = eb.build();
        }

        // ---- world generation
        if (oreGen != null && (ore != null || dOre != null || nOre != null)) {
            if (ore != null || dOre != null) {
                Worldgen.OreBuilder ob = registry.worldgen().ore("ore_" + name);
                if (ore != null) ob.replace(Worldgen.OreTarget.STONE, ore);
                if (dOre != null) ob.replace(Worldgen.OreTarget.DEEPSLATE, dOre);
                oreGen.apply(ob).build();
            }
            if (nOre != null) {
                Worldgen.OreBuilder ob = registry.worldgen().ore("ore_" + name + "_nether")
                        .replace(Worldgen.OreTarget.NETHER, nOre).biomes("minecraft:is_nether");
                oreGen.apply(ob).biomes("minecraft:is_nether").build();
            }
        }

        MaterialSet set = new MaterialSet(name, type, main, nug, rawItem, storage, rawBlock, ore, dOre, nOre, fam, equipment);
        registry.addMaterial(set);
        return set;
    }

    // ================================================================ helpers

    private ItemBuilder item(String id, String pattern, int fuelTicks) {
        ItemBuilder b = registry.item(id).naming(pattern, name);
        if (fuelTicks > 0) b.fuel(fuelTicks);
        if (fireResistant) b.fireResistant();
        if (tab != null) b.tab(tab);
        return b;
    }

    private BlockBuilder block(String id, String pattern, Block copy, MiningTool.Tier t) {
        BlockBuilder b = registry.block(id).copy(copy).naming(pattern, name).tool(MiningTool.PICKAXE, t);
        if (light >= 0) b.light(light);
        if (fireResistant) b.itemProperties(Item.Properties::fireResistant);
        if (tab != null) b.tab(tab);
        return b;
    }

    private RegistrySupplier<Block> ore(String id, String pattern, Block copy, String ground, MiningTool.Tier t,
                                        int xpMin, int xpMax, Object drop) {
        RegistrySupplier<Block> ore = block(id, pattern, copy, t)
                .xp(xpMin, xpMax)
                .drops(Drops.ore(drop).count(dropMin, dropMax))
                .tag("c:ores/" + name).itemTag("c:ores/" + name)
                .tag("c:ores_in_ground/" + ground).itemTag("c:ores_in_ground/" + ground)
                .build();
        tagParent("c:ores", "c:ores/" + name);
        registry.addBlockTag(Refs.parse("c", "ores"), "#c:ores/" + name);
        return ore;
    }

    private void storageTags(String sub, RegistrySupplier<Block> block) {
        registry.addBlockTag(Refs.parse("c", "storage_blocks"), "#c:storage_blocks/" + sub);
        registry.addItemTag(Refs.parse("c", "storage_blocks"), "#c:storage_blocks/" + sub);
    }

    /** Adds {@code #child} to the parent item tag ({@code c:ingots} <- {@code #c:ingots/tin}). */
    private void tagParent(String parent, String child) {
        registry.addItemTag(Refs.parse("c", parent), "#" + child);
    }
}
