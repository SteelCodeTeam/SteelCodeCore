// SPDX-License-Identifier: LGPL-3.0-or-later
package dev.steelcode.core.impl.registry;

import dev.architectury.registry.CreativeTabRegistry;
import dev.architectury.registry.fuel.FuelRegistry;
import dev.architectury.registry.registries.DeferredRegister;
import dev.architectury.registry.registries.DeferredSupplier;
import dev.architectury.registry.registries.RegistrySupplier;
import dev.steelcode.core.api.CoreRegistry;
import dev.steelcode.core.api.block.BlockBuilder;
import dev.steelcode.core.api.block.BlockFamily;
import dev.steelcode.core.api.block.BlockFamilyBuilder;
import dev.steelcode.core.api.block.ColorSet;
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
import dev.steelcode.core.api.item.EquipmentSet;
import dev.steelcode.core.api.item.ItemBuilder;
import dev.steelcode.core.api.item.MaterialBuilder;
import dev.steelcode.core.api.item.MaterialSet;
import dev.steelcode.core.api.item.SmithingTemplateBuilder;
import dev.steelcode.core.api.item.TabBuilder;
import dev.steelcode.core.api.world.CropBuilder;
import dev.steelcode.core.api.world.PlantBuilder;
import dev.steelcode.core.api.world.VariantsBuilder;
import dev.steelcode.core.api.world.WoodBuilder;
import dev.steelcode.core.api.world.WoodSet;
import dev.steelcode.core.api.world.Worldgen;
import dev.steelcode.core.impl.util.Refs;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.alchemy.Potion;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ComposterBlock;
import net.minecraft.world.level.block.FireBlock;
import net.minecraft.world.level.block.entity.BlockEntityType;
import org.jetbrains.annotations.ApiStatus;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Supplier;
import java.util.function.UnaryOperator;

/**
 * Internal state behind a {@link CoreRegistry}: deferred registers, everything queued for datagen and the
 * hooks that run on {@link #register()}.
 *
 * <p><b>Not API.</b> Builders and the datagen/compat/client layers use it; mods should only talk to
 * {@link CoreRegistry}. Anything here may change without notice.
 */
@ApiStatus.Internal
public final class RegistryContext {

    private static final java.util.Map<CoreRegistry, RegistryContext> BY_FACADE = new java.util.IdentityHashMap<>();
    private CoreRegistry facade;

    /** Creates the context for a new {@link CoreRegistry} facade. */
    public static RegistryContext create(String modId, CoreRegistry facade) {
        RegistryContext ctx = new RegistryContext(modId);
        ctx.facade = facade;
        synchronized (BY_FACADE) { BY_FACADE.put(facade, ctx); }
        return ctx;
    }

    /** The context behind a public registry. */
    public static RegistryContext of(CoreRegistry registry) {
        synchronized (BY_FACADE) {
            RegistryContext ctx = BY_FACADE.get(registry);
            if (ctx == null) throw new IllegalArgumentException("Unknown CoreRegistry");
            return ctx;
        }
    }

    /** The public facade of this context. */
    public CoreRegistry facade() { return facade; }

    private static final List<RegistryContext> ALL = new CopyOnWriteArrayList<>();

    private final String modId;
    private final DeferredRegister<Block> blocks;
    private final DeferredRegister<Item> items;
    private final DeferredRegister<CreativeModeTab> tabs;
    private final DeferredRegister<ArmorMaterial> armorMaterials;
    private final DeferredRegister<EntityType<?>> entityTypes;
    private final DeferredRegister<BlockEntityType<?>> blockEntityTypes;
    private final DeferredRegister<MobEffect> mobEffects;
    private final DeferredRegister<Potion> potions;
    private final LootInjector loot = new LootInjector(this);
    private final CoreNetwork network = new CoreNetwork(this);
    private final VariantsBuilder variants = new VariantsBuilder(this);
    private final DeferredRegister<net.minecraft.sounds.SoundEvent> soundEvents;
    private final DeferredRegister<ResourceLocation> customStats;
    private final DeferredRegister<net.minecraft.advancements.CriterionTrigger<?>> triggers;
    private final List<java.util.function.BiConsumer<com.mojang.brigadier.CommandDispatcher<net.minecraft.commands.CommandSourceStack>,
            net.minecraft.commands.CommandBuildContext>> commands = new ArrayList<>();
    private final List<DataEntry> dataEntries = new ArrayList<>();
    private final Map<String, JsonEntry> assetEntries = new java.util.LinkedHashMap<>();
    private final Map<String, com.google.gson.JsonObject> soundDefinitions = new TreeMap<>();
    private final Set<String> literalOnce = new java.util.HashSet<>();
    private final Map<RegistrySupplier<?>, List<String>> viewerInfo = new java.util.LinkedHashMap<>();
    private final Set<RegistrySupplier<?>> hiddenInViewers = new java.util.LinkedHashSet<>();
    private final Map<RegistrySupplier<Block>, java.util.function.Function<net.minecraft.world.level.block.state.BlockState, net.minecraft.network.chat.Component>> blockTooltips = new java.util.LinkedHashMap<>();
    private final Trades trades = new Trades(this);
    private final Advancements advancements = new Advancements(this);
    private CoreConfig config;
    private final List<Runnable> clientEarly = new ArrayList<>();
    private final List<Runnable> clientLate = new ArrayList<>();
    private final List<String> literalKeys = new ArrayList<>();
    private final List<EnchantmentEntry> enchantments = new ArrayList<>();
    private final List<AdvancementEntry> advancementEntries = new ArrayList<>();
    private final Recipes recipes = new Recipes(this);
    private final Worldgen worldgen = new Worldgen(this);
    private final Recipes autoRecipes = recipes.automatic();

    private final List<BlockFamily> families = new ArrayList<>();
    private final List<MaterialSet> materials = new ArrayList<>();
    private final List<BlockEntry> blockEntries = new ArrayList<>();
    private final List<ItemEntry> itemEntries = new ArrayList<>();
    private final List<RecipeEntry> recipeEntries = new ArrayList<>();
    private final Map<ResourceLocation, Set<String>> blockTags = new TreeMap<>();
    private final Map<ResourceLocation, Set<String>> itemTags = new TreeMap<>();
    private final List<NameEntry> names = new ArrayList<>();
    private final Map<String, Lang> langs = new LinkedHashMap<>();
    private final List<FeatureEntry> features = new ArrayList<>();
    private final List<WoodSet> woods = new ArrayList<>();
    private final List<ColorSet> colorSets = new ArrayList<>();
    private final List<EquipmentSet> equipment = new ArrayList<>();
    private final List<Runnable> afterRegister = new ArrayList<>();
    private final Map<RegistrySupplier<Item>, Float> compostables = new LinkedHashMap<>();

    private final List<TabAssignment> tabAssignments = new ArrayList<>();
    private final List<Fuel> fuels = new ArrayList<>();
    private DeferredSupplier<CreativeModeTab> defaultTab;
    private boolean generateLang = true;
    private boolean registered;

    private RegistryContext(String modId) {
        this.modId = Objects.requireNonNull(modId);
        this.blocks = DeferredRegister.create(modId, Registries.BLOCK);
        this.items = DeferredRegister.create(modId, Registries.ITEM);
        this.tabs = DeferredRegister.create(modId, Registries.CREATIVE_MODE_TAB);
        this.armorMaterials = DeferredRegister.create(modId, Registries.ARMOR_MATERIAL);
        this.entityTypes = DeferredRegister.create(modId, Registries.ENTITY_TYPE);
        this.blockEntityTypes = DeferredRegister.create(modId, Registries.BLOCK_ENTITY_TYPE);
        this.mobEffects = DeferredRegister.create(modId, Registries.MOB_EFFECT);
        this.potions = DeferredRegister.create(modId, Registries.POTION);
        this.soundEvents = DeferredRegister.create(modId, Registries.SOUND_EVENT);
        this.customStats = DeferredRegister.create(modId, Registries.CUSTOM_STAT);
        this.triggers = DeferredRegister.create(modId, Registries.TRIGGER_TYPE);
        langs.put("en_us", new Lang("en_us"));
    }


    /** @return every registry created so far (used by the client, datagen and compat layers) */
    public static List<RegistryContext> all() {
        return Collections.unmodifiableList(ALL);
    }

    // ================================================================ builders

    /** Backs {@link CoreRegistry#family}. */
    public BlockFamilyBuilder family() {
        ensureOpen();
        return new BlockFamilyBuilder(this);
    }

    /** Backs {@link CoreRegistry#block}. */
    public BlockBuilder block(String name) {
        ensureOpen();
        return new BlockBuilder(this, name);
    }

    /** Backs {@link CoreRegistry#simpleBlock}. */
    public RegistrySupplier<Block> simpleBlock(String name, Block copyPropertiesFrom) {
        return block(name).copy(copyPropertiesFrom).build();
    }

    public ItemBuilder item(String name) {
        ensureOpen();
        return new ItemBuilder(this, name);
    }

    /** Backs {@link CoreRegistry#material}. */
    public MaterialBuilder material(String name) {
        ensureOpen();
        return new MaterialBuilder(this, name);
    }

    /** Backs {@link CoreRegistry#tab}. */
    public TabBuilder tab(String name) {
        ensureOpen();
        return new TabBuilder(this, name);
    }

    /** Backs {@link CoreRegistry#wood}. */
    public WoodBuilder wood(String name) {
        ensureOpen();
        return new WoodBuilder(this, name);
    }

    /** Backs {@link CoreRegistry#colors}. */
    public ColorSetBuilder colors(String name) {
        ensureOpen();
        return new ColorSetBuilder(this, name);
    }

    /** Backs {@link CoreRegistry#equipment}. */
    public EquipmentBuilder equipment(String name, Object repairItem) {
        ensureOpen();
        return new EquipmentBuilder(this, name, repairItem);
    }

    public Recipes recipes() {
        return recipes;
    }

    /** Backs {@link CoreRegistry#crop}. */
    public CropBuilder crop(String name) {
        ensureOpen();
        return new CropBuilder(this, name);
    }

    /** Backs {@link CoreRegistry#plant}. */
    public PlantBuilder plant(String name) {
        ensureOpen();
        return new PlantBuilder(this, name);
    }

    /** Backs {@link CoreRegistry#effect}. */
    public EffectBuilder effect(String name) {
        ensureOpen();
        return new EffectBuilder(this, name);
    }

    /** Backs {@link CoreRegistry#potion}. */
    public PotionBuilder potion(String name) {
        ensureOpen();
        return new PotionBuilder(this, name);
    }

    /** Backs {@link CoreRegistry#enchantment}. */
    public EnchantmentBuilder enchantment(String name) {
        ensureOpen();
        return new EnchantmentBuilder(this, name);
    }

    /** Backs {@link CoreRegistry#spawnEgg}. */
    public RegistrySupplier<Item> spawnEgg(RegistrySupplier<? extends EntityType<? extends net.minecraft.world.entity.Mob>> entity,
                                           int background, int highlight) {
        ensureOpen();
        String id = entity.getId().getPath() + "_spawn_egg";
        return item(id).factory(p -> new dev.architectury.core.item.ArchitecturySpawnEggItem(entity, background, highlight, p))
                .modelParent("minecraft:item/template_spawn_egg", null)
                .naming("spawn_egg", entity.getId().getPath()).build();
    }

    /** Backs {@link CoreRegistry#smithingTemplate}. */
    public SmithingTemplateBuilder smithingTemplate(String name) {
        ensureOpen();
        return new SmithingTemplateBuilder(this, name);
    }

    /** Backs {@link CoreRegistry#loot}. */
    public LootInjector loot() {
        return loot;
    }

    /** Backs {@link CoreRegistry#trades}. */
    public Trades trades() {
        return trades;
    }

    /** Backs {@link CoreRegistry#advancements}. */
    public Advancements advancements() {
        return advancements;
    }

    /** Backs {@link CoreRegistry#config}. */
    public CoreConfig config() {
        if (config == null) config = new CoreConfig(modId);
        return config;
    }

    /** Backs {@link CoreRegistry#network}. */
    public CoreNetwork network() {
        return network;
    }

    /** Backs {@link CoreRegistry#playerData}. */
    public <T> PlayerData.Builder<T> playerData(String name, com.mojang.serialization.Codec<T> codec, Supplier<T> defaultValue) {
        ensureOpen();
        return new PlayerData.Builder<>(this, name, codec, defaultValue);
    }

    /** Backs {@link CoreRegistry#worldData}. */
    public <T> WorldData<T> worldData(String name, com.mojang.serialization.Codec<T> codec, Supplier<T> defaultValue) {
        return new WorldData<>(modId, name, codec, defaultValue);
    }

    /** Backs {@link CoreRegistry#keybind}. */
    public KeyBindBuilder keybind(String name, int defaultKey) {
        ensureOpen();
        return new KeyBindBuilder(this, name, defaultKey);
    }

    /** Backs {@link CoreRegistry#hud}. */
    public HudBuilder hud(String name) {
        ensureOpen();
        return new HudBuilder(this, name);
    }

    /** Backs {@link CoreRegistry#command}. */
    public CommandBuilder command(String name) {
        ensureOpen();
        return new CommandBuilder(this, name);
    }

    /** Backs {@link CoreRegistry#stat}. */
    public CoreStat stat(String name) {
        ensureOpen();
        ResourceLocation id = ResourceLocation.fromNamespaceAndPath(modId, name);
        RegistrySupplier<ResourceLocation> reg = customStats.register(name, () -> id);
        afterRegister(() -> reg.listen(rl -> net.minecraft.stats.Stats.CUSTOM.get(rl, net.minecraft.stats.StatFormatter.DEFAULT)));
        String key = "stat." + modId + "." + name;
        name(key, name, "self", name);
        return new CoreStat(id);
    }

    /** Backs {@link CoreRegistry#trigger}. */
    public RegistrySupplier<CoreTrigger> trigger(String name) {
        ensureOpen();
        return triggers.register(name, CoreTrigger::new);
    }

    /** Backs {@link CoreRegistry#variants}. */
    public VariantsBuilder variants() {
        return variants;
    }

    /** Backs {@link CoreRegistry#oxidizing}. */
    public OxidizingBuilder oxidizing(String name) {
        ensureOpen();
        return new OxidizingBuilder(this, name);
    }

    /** Backs {@link CoreRegistry#crystal}. */
    public CrystalBuilder crystal(String name) {
        ensureOpen();
        return new CrystalBuilder(this, name);
    }

    /** Backs {@link CoreRegistry#worldgen}. */
    public Worldgen worldgen() {
        return worldgen;
    }

    /** Backs {@link CoreRegistry#lang}. */
    public Lang lang(String code) {
        return langs.computeIfAbsent(code, Lang::new);
    }

    // ================================================================ settings

    /** Backs {@link CoreRegistry#defaultTab}. */
    public RegistryContext defaultTab(ResourceKey<CreativeModeTab> tab) {
        this.defaultTab = CreativeTabRegistry.defer(tab);
        return this;
    }

    /** Backs {@link CoreRegistry#defaultTab}. */
    public RegistryContext defaultTab(DeferredSupplier<CreativeModeTab> tab) {
        this.defaultTab = tab;
        return this;
    }

    /** Backs {@link CoreRegistry#noLang}. */
    public RegistryContext noLang() {
        this.generateLang = false;
        return this;
    }

    /** Backs {@link CoreRegistry#blockTag}. */
    public RegistryContext blockTag(String tag, Object... entries) {
        for (Object e : entries) addBlockTag(Refs.parse(modId, tag), Refs.entry(e));
        return this;
    }

    public RegistryContext itemTag(String tag, Object... entries) {
        for (Object e : entries) addItemTag(Refs.parse(modId, tag), Refs.entry(e));
        return this;
    }

    // ================================================================ registration

    /** Backs {@link CoreRegistry#register}. */
    public void register() {
        if (registered) {
            throw new IllegalStateException("CoreRegistry '" + modId + "' is already registered");
        }
        registered = true;
        tabs.register();
        armorMaterials.register();
        mobEffects.register();
        potions.register();
        soundEvents.register();
        customStats.register();
        triggers.register();
        blocks.register();
        items.register();
        entityTypes.register();
        blockEntityTypes.register();

        for (TabAssignment a : tabAssignments) {
            DeferredSupplier<CreativeModeTab> tab = a.tab() != null ? a.tab() : defaultTab;
            if (tab != null) {
                CreativeTabRegistry.append(tab, a.item());
            }
        }
        for (Fuel fuel : fuels) {
            fuel.item().listen(item -> FuelRegistry.register(fuel.ticks(), item));
        }
        compostables.forEach((item, chance) -> item.listen(i -> ComposterBlock.COMPOSTABLES.put(i, (float) chance)));
        worldgen.inject();
        loot.inject();
        trades.inject();
        network.register();
        if (!commands.isEmpty()) {
            dev.architectury.event.events.common.CommandRegistrationEvent.EVENT.register((dispatcher, ctx, selection) ->
                    commands.forEach(c -> c.accept(dispatcher, ctx)));
        }
        afterRegister.forEach(Runnable::run);
        ALL.add(this);
        dev.architectury.utils.EnvExecutor.runInEnv(dev.architectury.utils.Env.CLIENT, () -> () -> {
            dev.steelcode.core.impl.client.SteelCodeCoreClient.early(this);
            clientEarly.forEach(Runnable::run);
        });
    }

    // ================================================================ accessors (datagen / client)

    public String modId() { return modId; }
    public List<BlockFamily> families() { return Collections.unmodifiableList(families); }
    public List<MaterialSet> materials() { return Collections.unmodifiableList(materials); }
    public List<BlockEntry> blockEntries() { return Collections.unmodifiableList(blockEntries); }
    public List<ItemEntry> itemEntries() { return Collections.unmodifiableList(itemEntries); }
    public List<RecipeEntry> recipeEntries() { return Collections.unmodifiableList(recipeEntries); }
    public Map<ResourceLocation, Set<String>> blockTags() { return Collections.unmodifiableMap(blockTags); }
    public Map<ResourceLocation, Set<String>> itemTags() { return Collections.unmodifiableMap(itemTags); }
    public List<NameEntry> names() { return Collections.unmodifiableList(names); }
    public Map<String, Lang> languages() { return Collections.unmodifiableMap(langs); }
    public List<FeatureEntry> features() { return Collections.unmodifiableList(features); }
    public List<WoodSet> woods() { return Collections.unmodifiableList(woods); }
    public List<ColorSet> colorSets() { return Collections.unmodifiableList(colorSets); }
    public List<EquipmentSet> equipmentSets() { return Collections.unmodifiableList(equipment); }
    public Map<RegistrySupplier<Item>, Float> compostables() { return Collections.unmodifiableMap(compostables); }
    public List<DataEntry> dataEntries() { return Collections.unmodifiableList(dataEntries); }
    public java.util.Collection<JsonEntry> assetEntries() { return Collections.unmodifiableCollection(assetEntries.values()); }
    public Map<String, com.google.gson.JsonObject> soundDefinitions() { return Collections.unmodifiableMap(soundDefinitions); }
    /** @return JEI/REI/EMI descriptions (translation keys per item) */
    public Map<RegistrySupplier<?>, List<String>> viewerInfo() { return Collections.unmodifiableMap(viewerInfo); }
    public Set<RegistrySupplier<?>> hiddenInViewers() { return Collections.unmodifiableSet(hiddenInViewers); }
    /** @return Jade/WTHIT tooltip lines per block */
    public Map<RegistrySupplier<Block>, java.util.function.Function<net.minecraft.world.level.block.state.BlockState, net.minecraft.network.chat.Component>> blockTooltips() {
        return Collections.unmodifiableMap(blockTooltips);
    }
    public List<EnchantmentEntry> enchantmentEntries() { return Collections.unmodifiableList(enchantments); }
    public List<AdvancementEntry> advancementEntries() { return Collections.unmodifiableList(advancementEntries); }
    /** @return client registrations that must run late (render layers, item properties) */
    public List<Runnable> clientLate() { return Collections.unmodifiableList(clientLate); }
    public DeferredRegister<EntityType<?>> entityRegister() { return entityTypes; }
    public DeferredRegister<BlockEntityType<?>> blockEntityRegister() { return blockEntityTypes; }
    public boolean generateLang() { return generateLang; }
    public DeferredRegister<Block> blockRegister() { return blocks; }
    public DeferredRegister<Item> itemRegister() { return items; }
    public DeferredRegister<CreativeModeTab> tabRegister() { return tabs; }

    // ================================================================ internal (used by the builders)

    /** Common item options: tab, fuel and properties. */
    public record ItemOptions(DeferredSupplier<CreativeModeTab> tab, boolean noTab, int fuel,
                       UnaryOperator<Item.Properties> properties) {
        static final ItemOptions DEFAULT = new ItemOptions(null, false, 0, UnaryOperator.identity());
    }

    public record Registered(RegistrySupplier<Block> block, RegistrySupplier<Item> item) {
    }

    private record TabAssignment(RegistrySupplier<Item> item, DeferredSupplier<CreativeModeTab> tab) {
    }

    private record Fuel(RegistrySupplier<Item> item, int ticks) {
    }

    public Registered registerBlock(String name, Supplier<? extends Block> factory, ItemOptions options, boolean withItem) {
        ensureOpen();
        RegistrySupplier<Block> block = blocks.register(name, factory::get);
        RegistrySupplier<Item> item = null;
        if (withItem) {
            item = items.register(name, () -> new BlockItem(block.get(), options.properties().apply(new Item.Properties())));
            itemSideEffects(item, options);
        }
        return new Registered(block, item);
    }

    public RegistrySupplier<Item> registerItem(String name, Supplier<? extends Item> factory, ItemOptions options) {
        ensureOpen();
        RegistrySupplier<Item> item = items.register(name, factory::get);
        itemSideEffects(item, options);
        return item;
    }

    private void itemSideEffects(RegistrySupplier<Item> item, ItemOptions options) {
        if (!options.noTab()) tabAssignments.add(new TabAssignment(item, options.tab()));
        if (options.fuel() > 0) fuels.add(new Fuel(item, options.fuel()));
    }

    public RegistrySupplier<CreativeModeTab> registerTab(String name, Supplier<CreativeModeTab> factory, String title) {
        ensureOpen();
        RegistrySupplier<CreativeModeTab> tab = tabs.register(name, factory::get);
        String key = "itemGroup." + modId + "." + name;
        // The title acts as a word: other languages translate it with .word("My Mod", "...") or .set(key, "...").
        name(key, name, "self", title != null ? title : name);
        if (defaultTab == null) defaultTab = tab;
        return tab;
    }

    public Recipes autoRecipes() { return autoRecipes; }
    public void addFamily(BlockFamily family) { families.add(family); }
    public void addMaterial(MaterialSet material) { materials.add(material); }
    public void addBlockEntry(BlockEntry entry) { blockEntries.add(entry); }
    public void addItemEntry(ItemEntry entry) { itemEntries.add(entry); }
    public void addRecipe(RecipeEntry entry) { recipeEntries.add(entry); }

    public void addBlockTag(ResourceLocation tag, String entry) {
        blockTags.computeIfAbsent(tag, k -> new TreeSet<>()).add(entry);
    }

    private final Set<String> expectedTextures = new TreeSet<>();

    /** A texture no model references but the game needs (entities, armor, icons); full asset path. */
    public void expectTexture(String assetPath) {
        expectedTextures.add(assetPath);
    }

    public Set<String> expectedTextures() { return Collections.unmodifiableSet(expectedTextures); }

    private final Map<String, Map<ResourceLocation, Set<String>>> otherTags = new TreeMap<>();

    /** A tag of another registry ({@code painting_variant}, {@code banner_pattern}...). */
    public void addTag(String registryFolder, ResourceLocation tag, String entry) {
        otherTags.computeIfAbsent(registryFolder, k -> new TreeMap<>()).computeIfAbsent(tag, k -> new TreeSet<>()).add(entry);
    }

    public Map<String, Map<ResourceLocation, Set<String>>> otherTags() { return Collections.unmodifiableMap(otherTags); }

    private final Map<ResourceLocation, Set<String>> enchantmentTags = new TreeMap<>();

    public void addEnchantmentTag(ResourceLocation tag, String entry) {
        enchantmentTags.computeIfAbsent(tag, k -> new TreeSet<>()).add(entry);
    }

    public Map<ResourceLocation, Set<String>> enchantmentTags() { return Collections.unmodifiableMap(enchantmentTags); }

    public void addItemTag(ResourceLocation tag, String entry) {
        itemTags.computeIfAbsent(tag, k -> new TreeSet<>()).add(entry);
    }

    /**
     * Translatable name: {@code key} = translation key, {@code pattern} = pattern ("stairs", "ingot", "self"...),
     * {@code subject} = main word, {@code color} = color or {@code null}.
     */
    public record NameEntry(String key, String id, String pattern, String subject, String color) {
    }

    public void name(String key, String id, String pattern, String subject) {
        names.add(new NameEntry(key, id, pattern, subject, null));
    }

    public void name(String key, String id, String pattern, String subject, String color) {
        names.add(new NameEntry(key, id, pattern, subject, color));
    }

    /** Block/item name: if {@code english} is given it is used verbatim in en_us. */
    public void nameBlock(ResourceLocation id, String pattern, String subject, String english) {
        nameBlock(id, pattern, subject, null, english);
    }

    public void nameBlock(ResourceLocation id, String pattern, String subject, String color, String english) {
        String key = "block." + id.getNamespace() + "." + id.getPath();
        name(key, id.getPath(), pattern, subject, color);
        if (english != null) lang("en_us").set(key, english);
    }

    public void nameItem(ResourceLocation id, String pattern, String subject, String english) {
        String key = "item." + id.getNamespace() + "." + id.getPath();
        name(key, id.getPath(), pattern, subject);
        if (english != null) lang("en_us").set(key, english);
    }

    public RegistrySupplier<ArmorMaterial> registerArmorMaterial(String name, Supplier<ArmorMaterial> factory) {
        ensureOpen();
        return armorMaterials.register(name, factory::get);
    }

    public void addFeature(FeatureEntry feature) { features.add(feature); }

    /** A generic data file ({@code data/<ns>/<folder>/<path>.json}). */
    public record DataEntry(String folder, ResourceLocation id, java.util.function.Supplier<com.google.gson.JsonObject> json) {
    }

    /** An asset file with its full path (atlases). */
    public record JsonEntry(String path, com.google.gson.JsonObject json) {
    }

    public void addData(String folder, ResourceLocation id, java.util.function.Supplier<com.google.gson.JsonObject> json) {
        dataEntries.add(new DataEntry(folder, id, json));
    }

    /** Merges sources into an atlas ({@code assets/minecraft/atlases/<atlas>.json}). */
    public void addAtlasSource(String atlas, com.google.gson.JsonObject source) {
        String path = "assets/minecraft/atlases/" + atlas + ".json";
        JsonEntry e = assetEntries.computeIfAbsent(path, p -> {
            com.google.gson.JsonObject root = new com.google.gson.JsonObject();
            root.add("sources", new com.google.gson.JsonArray());
            return new JsonEntry(p, root);
        });
        e.json().getAsJsonArray("sources").add(source);
    }

    public RegistrySupplier<net.minecraft.sounds.SoundEvent> registerSound(String name, String file, boolean stream) {
        ensureOpen();
        ResourceLocation id = ResourceLocation.fromNamespaceAndPath(modId, name);
        com.google.gson.JsonObject sound = new com.google.gson.JsonObject();
        sound.addProperty("name", modId + ":" + file);
        if (stream) sound.addProperty("stream", true);
        com.google.gson.JsonArray sounds = new com.google.gson.JsonArray();
        sounds.add(sound);
        com.google.gson.JsonObject def = new com.google.gson.JsonObject();
        def.add("sounds", sounds);
        soundDefinitions.put(name, def);
        expectTexture("assets/" + modId + "/sounds/" + file + ".ogg");
        return soundEvents.register(name, () -> net.minecraft.sounds.SoundEvent.createVariableRangeEvent(id));
    }

    public void addCommand(java.util.function.BiConsumer<com.mojang.brigadier.CommandDispatcher<net.minecraft.commands.CommandSourceStack>,
            net.minecraft.commands.CommandBuildContext> command) {
        commands.add(command);
    }

    /** Like {@link #literal}, but skipped if the key already exists (key categories). */
    public void literalOnce(String key, String english) {
        if (literalOnce.add(key)) literal(key, english);
    }

    public void viewerInfo(RegistrySupplier<?> item, String english) {
        String key = "info." + modId + "." + item.getId().getPath() + "." + viewerInfo.computeIfAbsent(item, k -> new ArrayList<>()).size();
        viewerInfo.get(item).add(key);
        literal(key, english);
    }

    public void hideInViewers(RegistrySupplier<?> item) { hiddenInViewers.add(item); }

    private final Map<RegistrySupplier<Block>, RegistrySupplier<Block>> oxidizables = new java.util.LinkedHashMap<>();
    private final Map<RegistrySupplier<Block>, RegistrySupplier<Block>> waxables = new java.util.LinkedHashMap<>();

    public void oxidizable(RegistrySupplier<Block> from, RegistrySupplier<Block> to) { oxidizables.put(from, to); }
    public void waxable(RegistrySupplier<Block> plain, RegistrySupplier<Block> waxed) { waxables.put(plain, waxed); }
    /** For the NeoForge data map (Fabric registers it in code). */
    public Map<RegistrySupplier<Block>, RegistrySupplier<Block>> oxidizables() { return Collections.unmodifiableMap(oxidizables); }
    public Map<RegistrySupplier<Block>, RegistrySupplier<Block>> waxables() { return Collections.unmodifiableMap(waxables); }

    public void blockTooltip(RegistrySupplier<Block> block,
                      java.util.function.Function<net.minecraft.world.level.block.state.BlockState, net.minecraft.network.chat.Component> line) {
        blockTooltips.put(block, line);
    }

    /** Crop loot: mature -> produce (+ seeds with bonus), otherwise -> seeds. */
    public record CropLoot(RegistrySupplier<Block> crop, RegistrySupplier<Item> produce, RegistrySupplier<Item> seeds) {
    }

    private final List<CropLoot> cropLoots = new ArrayList<>();

    public void cropLoot(RegistrySupplier<Block> crop, RegistrySupplier<Item> produce, RegistrySupplier<Item> seeds) {
        cropLoots.add(new CropLoot(crop, produce, seeds));
    }

    /** @return {block, produce, seeds or null} of every crop (for datagen) */
    public List<RegistrySupplier<?>[]> cropLoots() {
        List<RegistrySupplier<?>[]> out = new ArrayList<>();
        for (CropLoot c : cropLoots) out.add(new RegistrySupplier<?>[]{c.crop(), c.produce(), c.seeds()});
        return out;
    }
    public void addEnchantment(EnchantmentEntry e) { enchantments.add(e); }
    public void addAdvancement(AdvancementEntry e) { advancementEntries.add(e); }

    /** A client registration that must run early (colors, entity renderers, model layers). */
    public void clientEarly(Runnable action) { clientEarly.add(action); }

    /** A client registration that needs the blocks/items to exist (render layers, properties). */
    public void clientLate(Runnable action) { clientLate.add(action); }

    public <T extends net.minecraft.world.entity.Entity> RegistrySupplier<EntityType<T>> registerEntity(String name,
            Supplier<EntityType<T>> factory) {
        ensureOpen();
        return entityTypes.register(name, factory::get);
    }

    public <T extends net.minecraft.world.level.block.entity.BlockEntity> RegistrySupplier<BlockEntityType<T>> registerBlockEntity(
            String name, Supplier<BlockEntityType<T>> factory) {
        ensureOpen();
        return blockEntityTypes.register(name, factory::get);
    }

    public RegistrySupplier<MobEffect> registerEffect(String name, Supplier<MobEffect> factory) {
        ensureOpen();
        return mobEffects.register(name, factory::get);
    }

    public RegistrySupplier<Potion> registerPotion(String name, Supplier<Potion> factory) {
        ensureOpen();
        return potions.register(name, factory::get);
    }

    /**
     * Free translatable text (tooltips, advancement descriptions...). In en_us it is {@code english};
     * other languages use {@code REG.lang(code).set(key, ...)}, falling back to English with a warning.
     */
    public void literal(String key, String english) {
        names.add(new NameEntry(key, key, "literal", english, null));
    }
    public void addWood(WoodSet wood) { woods.add(wood); }
    public void addColorSet(ColorSet set) { colorSets.add(set); }
    public void addEquipment(EquipmentSet set) { equipment.add(set); }
    public void afterRegister(Runnable action) { afterRegister.add(action); }
    public void compostable(RegistrySupplier<Item> item, float chance) { compostables.put(item, chance); }

    /** Flammability (vanilla: logs 5/5, planks 5/20, leaves 30/60). */
    public void flammable(RegistrySupplier<Block> block, int burnOdds, int spreadOdds) {
        afterRegister(() -> block.listen(b -> ((FireBlock) Blocks.FIRE).setFlammable(b, burnOdds, spreadOdds)));
    }

    private void ensureOpen() {
        if (registered) {
            throw new IllegalStateException("Cannot add content to '" + modId + "' after register()");
        }
    }
}
