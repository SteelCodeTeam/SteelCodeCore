// SPDX-License-Identifier: LGPL-3.0-or-later
package dev.steelcode.core.api;

import com.mojang.serialization.Codec;
import dev.architectury.registry.registries.DeferredSupplier;
import dev.architectury.registry.registries.RegistrySupplier;
import dev.steelcode.core.SteelCodeCore;
import dev.steelcode.core.api.block.BlockBuilder;
import dev.steelcode.core.api.block.BlockFamilyBuilder;
import dev.steelcode.core.api.block.ColorSetBuilder;
import dev.steelcode.core.api.block.CrystalBuilder;
import dev.steelcode.core.api.block.OxidizingBuilder;
import dev.steelcode.core.api.data.Advancements;
import dev.steelcode.core.api.data.CoreConfig;
import dev.steelcode.core.api.data.Lang;
import dev.steelcode.core.api.data.LootInjector;
import dev.steelcode.core.api.data.Recipes;
import dev.steelcode.core.api.data.Trades;
import dev.steelcode.core.api.effect.EffectBuilder;
import dev.steelcode.core.api.effect.EnchantmentBuilder;
import dev.steelcode.core.api.effect.PotionBuilder;
import dev.steelcode.core.api.gameplay.CommandBuilder;
import dev.steelcode.core.api.gameplay.CoreNetwork;
import dev.steelcode.core.api.gameplay.CoreStat;
import dev.steelcode.core.api.gameplay.CoreTrigger;
import dev.steelcode.core.api.gameplay.HudBuilder;
import dev.steelcode.core.api.gameplay.KeyBindBuilder;
import dev.steelcode.core.api.gameplay.PlayerData;
import dev.steelcode.core.api.gameplay.WorldData;
import dev.steelcode.core.api.item.EquipmentBuilder;
import dev.steelcode.core.api.item.ItemBuilder;
import dev.steelcode.core.api.item.MaterialBuilder;
import dev.steelcode.core.api.item.SmithingTemplateBuilder;
import dev.steelcode.core.api.item.TabBuilder;
import dev.steelcode.core.api.world.CropBuilder;
import dev.steelcode.core.api.world.PlantBuilder;
import dev.steelcode.core.api.world.VariantsBuilder;
import dev.steelcode.core.api.world.WoodBuilder;
import dev.steelcode.core.api.world.Worldgen;
import dev.steelcode.core.impl.registry.RegistryContext;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;

import java.util.function.Supplier;

/**
 * The entry point of SteelCodeCore: one registry per mod, declared in your {@code common} module.
 *
 * <p>Everything you create through a registry is registered on <b>both Fabric and NeoForge</b>, added to a
 * creative tab, translated and turned into JSON (blockstates, models, loot tables, recipes, tags...) by datagen.
 *
 * <pre>{@code
 * public final class MyMod {
 *     public static final CoreRegistry REG = SteelCodeCore.registry("mymod");
 *
 *     public static final RegistrySupplier<CreativeModeTab> TAB =
 *             REG.tab("main").icon(() -> RUBY.main().get()).title("My Mod").build();
 *     public static final MaterialSet RUBY = REG.material("ruby").gem().tools(ToolStats.DIAMOND).build();
 *     public static final BlockFamily MARBLE = REG.family().name("marble").createAll().build();
 *
 *     public static void init() {   // call from your common init
 *         REG.register();
 *     }
 * }
 * }</pre>
 *
 * <h2>Lifecycle</h2>
 * <ol>
 *     <li>Declare content in static fields (or a static block) using the builder methods below.</li>
 *     <li>Call {@link #register()} exactly once from your common initializer.</li>
 *     <li>After {@code register()} the registry is closed: creating more content throws
 *     {@link IllegalStateException}.</li>
 * </ol>
 *
 * <h2>Names</h2>
 * Every {@code name} argument is a registry path in your mod's namespace ({@code "ruby_block"} becomes
 * {@code mymod:ruby_block}). English names are derived from it automatically; other languages use
 * {@link #lang(String)}.
 *
 * <p>Instances are obtained with {@link SteelCodeCore#registry(String)}; this class cannot be extended.
 */
public final class CoreRegistry {

    private final RegistryContext ctx;

    private CoreRegistry(String modId) {
        this.ctx = RegistryContext.create(modId, this);
    }

    /**
     * Creates a registry. Prefer {@link SteelCodeCore#registry(String)}.
     *
     * @param modId your mod id; every name you register lives in this namespace
     * @return a new, open registry
     */
    public static CoreRegistry create(String modId) {
        return new CoreRegistry(modId);
    }

    /** @return the mod id (namespace) of this registry */
    public String modId() {
        return ctx.modId();
    }

    /**
     * Registers everything declared so far on the current loader and closes the registry.
     * Call it exactly once, from your common initializer, after all content is declared.
     *
     * @throws IllegalStateException if called twice
     */
    public void register() {
        ctx.register();
    }

    // ------------------------------------------------------------------ blocks

    /**
     * Starts a block family: a base block plus any of its shapes (stairs, slabs, walls, doors, buttons...).
     *
     * <pre>{@code
     * REG.family().base(Blocks.GOLD_BLOCK).createAll().build();          // shapes for a vanilla block
     * REG.family().name("marble").copy(Blocks.STONE).stairs().slab().wall().build();
     * }</pre>
     *
     * @return a new family builder
     */
    public BlockFamilyBuilder family() {
        return ctx.family();
    }

    /**
     * Starts a single block with full control: light, XP, drops, shape, fuel, render layer, behaviour...
     *
     * @param name registry path, e.g. {@code "ruby_lamp"}
     * @return a new block builder
     */
    public BlockBuilder block(String name) {
        return ctx.block(name);
    }

    /**
     * Shortcut for a plain cube with texture {@code <modid>:block/<name>} that copies another block's properties.
     *
     * @param name               registry path
     * @param copyPropertiesFrom block whose hardness, sound, map color... are copied
     * @return the registered block (and its item)
     */
    public RegistrySupplier<Block> simpleBlock(String name, Block copyPropertiesFrom) {
        return ctx.simpleBlock(name, copyPropertiesFrom);
    }

    /**
     * Sixteen dye-colored variants of one block ({@code red_marble}, {@code blue_marble}...), optionally as families.
     *
     * @param name base name; colors are prefixed
     * @return a new color set builder
     */
    public ColorSetBuilder colors(String name) {
        return ctx.colors(name);
    }

    /**
     * Copper-like weathering blocks: four oxidation stages plus waxed versions, honeycomb waxing and axe scraping.
     *
     * @param name name of the unaffected stage, e.g. {@code "tin_plate"}
     * @return a new oxidizing builder
     */
    public OxidizingBuilder oxidizing(String name) {
        return ctx.oxidizing(name);
    }

    /**
     * Amethyst-like crystals: block, budding block, three bud sizes, cluster and shard.
     *
     * @param name crystal name, e.g. {@code "rose_quartz"}
     * @return a new crystal builder
     */
    public CrystalBuilder crystal(String name) {
        return ctx.crystal(name);
    }

    // ------------------------------------------------------------------ items

    /**
     * Starts a single item: food, durability, tooltip, use behaviour, model type, fuel...
     *
     * @param name registry path
     * @return a new item builder
     */
    public ItemBuilder item(String name) {
        return ctx.item(name);
    }

    /**
     * Starts a complete metal or gem: ingot/gem, nugget, raw ore, storage blocks, ores, recipes and {@code c:} tags.
     * Designed to be used in a loop:
     *
     * <pre>{@code
     * for (Metal m : Metal.values()) MATERIALS.put(m, REG.material(m.id()).metal().tools(m.tools()).build());
     * }</pre>
     *
     * @param name material name, e.g. {@code "tin"}
     * @return a new material builder
     */
    public MaterialBuilder material(String name) {
        return ctx.material(name);
    }

    /**
     * A creative tab. The first tab you create becomes this mod's default tab.
     *
     * @param name registry path of the tab
     * @return a new tab builder
     */
    public TabBuilder tab(String name) {
        return ctx.tab(name);
    }

    /**
     * Stand-alone tools, armor, bow, crossbow, fishing rod, shield or horse armor.
     * Materials create their own equipment with {@code .tools()} / {@code .armor()}.
     *
     * @param name       prefix for every piece, e.g. {@code "obsidian"}
     * @param repairItem repair ingredient: an {@code ItemLike}, a supplier, an id string or {@code "#tag"}
     * @return a new equipment builder
     */
    public EquipmentBuilder equipment(String name, Object repairItem) {
        return ctx.equipment(name, repairItem);
    }

    /**
     * A spawn egg for an entity type (yours or vanilla).
     *
     * @param entity     the entity type
     * @param background base color, {@code 0xRRGGBB}
     * @param highlight  spots color, {@code 0xRRGGBB}
     * @return the registered egg, named {@code <entity>_spawn_egg}
     */
    public RegistrySupplier<Item> spawnEgg(RegistrySupplier<? extends EntityType<? extends Mob>> entity,
                                           int background, int highlight) {
        return ctx.spawnEgg(entity, background, highlight);
    }

    /**
     * A custom smithing template (netherite-upgrade style).
     *
     * @param name registry path, conventionally ending in {@code _smithing_template}
     * @return a new template builder
     */
    public SmithingTemplateBuilder smithingTemplate(String name) {
        return ctx.smithingTemplate(name);
    }

    // ------------------------------------------------------------------ world

    /**
     * A full wood set: logs, wood, stripped variants, planks family, leaves, sapling, tree, signs and boats.
     *
     * @param name wood name, e.g. {@code "maple"}
     * @return a new wood builder
     */
    public WoodBuilder wood(String name) {
        return ctx.wood(name);
    }

    /**
     * A crop: growing block with stages, seeds and produce (or produce that is planted directly, like carrots).
     *
     * @param name crop name, e.g. {@code "tomato"}
     * @return a new crop builder
     */
    public CropBuilder crop(String name) {
        return ctx.crop(name);
    }

    /**
     * Flowers, tall plants and bushes, with flower pot and optional world generation.
     *
     * @param name plant name
     * @return a new plant builder
     */
    public PlantBuilder plant(String name) {
        return ctx.plant(name);
    }

    /**
     * World generation: ores, blobs (like granite), boulders, disks and geodes, injected into biomes on both loaders.
     *
     * @return this mod's worldgen helper (always the same instance)
     */
    public Worldgen worldgen() {
        return ctx.worldgen();
    }

    /**
     * Data-driven variants: wolf variants, paintings, armor trim materials, banner patterns and music discs.
     *
     * @return this mod's variants helper (always the same instance)
     */
    public VariantsBuilder variants() {
        return ctx.variants();
    }

    // ------------------------------------------------------------------ effects

    /**
     * A status effect (like Poison or Speed), with optional tick behaviour and attribute modifiers.
     *
     * @param name registry path
     * @return a new effect builder
     */
    public EffectBuilder effect(String name) {
        return ctx.effect(name);
    }

    /**
     * A potion, with its brewing-stand recipe and optional long/strong versions.
     *
     * @param name registry path
     * @return a new potion builder
     */
    public PotionBuilder potion(String name) {
        return ctx.potion(name);
    }

    /**
     * An enchantment. In 1.21 enchantments are data, so this generates the JSON definition and tags.
     *
     * @param name registry path
     * @return a new enchantment builder
     */
    public EnchantmentBuilder enchantment(String name) {
        return ctx.enchantment(name);
    }

    // ------------------------------------------------------------------ data

    /**
     * Manual recipes and recipe templates ({@code stairs}, {@code slab}, {@code smelting}, {@code shaped}...).
     *
     * @return this mod's recipe helper (always the same instance)
     */
    public Recipes recipes() {
        return ctx.recipes();
    }

    /**
     * Injects items into vanilla loot tables (dungeons, temples, mobs...).
     *
     * @return this mod's loot helper (always the same instance)
     */
    public LootInjector loot() {
        return ctx.loot();
    }

    /**
     * Villager and wandering-trader trades.
     *
     * @return this mod's trades helper (always the same instance)
     */
    public Trades trades() {
        return ctx.trades();
    }

    /**
     * Advancements (usually one tab per mod).
     *
     * @return this mod's advancements helper (always the same instance)
     */
    public Advancements advancements() {
        return ctx.advancements();
    }

    /**
     * The config file {@code config/<modid>.toml}. Declare values as static fields; they are read on first access.
     *
     * @return this mod's config (always the same instance)
     */
    public CoreConfig config() {
        return ctx.config();
    }

    /**
     * Extra translations for a language. {@code en_us} is always generated from names.
     *
     * <pre>{@code REG.lang("es_es").words("tin", "estaño").name("ruby_lamp", "Lámpara de rubí");}</pre>
     *
     * @param code Minecraft language code, e.g. {@code "es_es"}
     * @return the translations for that language (same instance per code)
     */
    public Lang lang(String code) {
        return ctx.lang(code);
    }

    /**
     * Adds entries to a block tag.
     *
     * @param tag     tag id, e.g. {@code "minecraft:beacon_base_blocks"}; no namespace means your mod
     * @param entries blocks, suppliers, ids or {@code "#tags"}
     * @return this registry
     */
    public CoreRegistry blockTag(String tag, Object... entries) {
        ctx.blockTag(tag, entries);
        return this;
    }

    /**
     * Adds entries to an item tag.
     *
     * @param tag     tag id, e.g. {@code "c:gems"}; no namespace means your mod
     * @param entries items, suppliers, ids or {@code "#tags"}
     * @return this registry
     */
    public CoreRegistry itemTag(String tag, Object... entries) {
        ctx.itemTag(tag, entries);
        return this;
    }

    // ------------------------------------------------------------------ gameplay

    /**
     * Typed client ↔ server packets.
     *
     * @return this mod's network helper (always the same instance)
     */
    public CoreNetwork network() {
        return ctx.network();
    }

    /**
     * Persistent per-player data, optionally copied on death and synced to the owning client.
     *
     * @param name         unique name within your mod
     * @param codec        codec used to save the value
     * @param defaultValue value for players that have none yet
     * @param <T>          value type; treat values as immutable and use {@code set}/{@code update}
     * @return a builder; call {@code build()}
     */
    public <T> PlayerData.Builder<T> playerData(String name, Codec<T> codec, Supplier<T> defaultValue) {
        return ctx.playerData(name, codec, defaultValue);
    }

    /**
     * Persistent per-dimension data, saved with the world.
     *
     * @param name         unique name within your mod
     * @param codec        codec used to save the value
     * @param defaultValue initial value
     * @param <T>          value type
     * @return the world data handle
     */
    public <T> WorldData<T> worldData(String name, Codec<T> codec, Supplier<T> defaultValue) {
        return ctx.worldData(name, codec, defaultValue);
    }

    /**
     * A rebindable key mapping; its action can run on the client or (automatically via a packet) on the server.
     *
     * @param name       unique name; translation key {@code key.<modid>.<name>}
     * @param defaultKey GLFW key code, e.g. {@code GLFW.GLFW_KEY_R}
     * @return a new key bind builder
     */
    public KeyBindBuilder keybind(String name, int defaultKey) {
        return ctx.keybind(name, defaultKey);
    }

    /**
     * A HUD element made of bars and text lines.
     *
     * @param name unique name within your mod
     * @return a new HUD builder
     */
    public HudBuilder hud(String name) {
        return ctx.hud(name);
    }

    /**
     * A command, registered as {@code /<modid> <name> ...}.
     *
     * @param name sub-command literal
     * @return a new command builder
     */
    public CommandBuilder command(String name) {
        return ctx.command(name);
    }

    /**
     * A custom statistic shown in the statistics screen.
     *
     * @param name registry path
     * @return the statistic handle
     */
    public CoreStat stat(String name) {
        return ctx.stat(name);
    }

    /**
     * A custom advancement criterion trigger, fired from code.
     *
     * @param name registry path, used as {@code "<modid>:<name>"} in advancement JSON
     * @return the registered trigger
     */
    public RegistrySupplier<CoreTrigger> trigger(String name) {
        return ctx.trigger(name);
    }

    // ------------------------------------------------------------------ settings

    /**
     * Sets the default creative tab to a vanilla one (instead of the first tab you create).
     *
     * @param tab e.g. {@code CreativeModeTabs.BUILDING_BLOCKS}
     * @return this registry
     */
    public CoreRegistry defaultTab(ResourceKey<CreativeModeTab> tab) {
        ctx.defaultTab(tab);
        return this;
    }

    /**
     * Sets the default creative tab to one of yours or another mod's.
     *
     * @param tab the tab supplier
     * @return this registry
     */
    public CoreRegistry defaultTab(DeferredSupplier<CreativeModeTab> tab) {
        ctx.defaultTab(tab);
        return this;
    }

    /**
     * Disables {@code en_us.json} generation, for mods that write their own translations.
     *
     * @return this registry
     */
    public CoreRegistry noLang() {
        ctx.noLang();
        return this;
    }

    @Override
    public String toString() {
        return "CoreRegistry[" + modId() + "]";
    }
}
