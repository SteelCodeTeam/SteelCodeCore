// SPDX-License-Identifier: LGPL-3.0-or-later
package dev.steelcode.core.api.block;

import dev.architectury.registry.registries.DeferredSupplier;
import dev.architectury.registry.registries.RegistrySupplier;
import dev.steelcode.core.impl.registry.BlockEntry;
import dev.steelcode.core.impl.registry.RegistryContext;
import dev.steelcode.core.impl.registry.ShapeSupport;
import dev.steelcode.core.impl.util.Refs;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.properties.BlockSetType;
import net.minecraft.world.level.block.state.properties.WoodType;
import org.jetbrains.annotations.ApiStatus;

import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.function.UnaryOperator;

/**
 * Builds a block family: a base block plus shaped and decorative variants that share its texture.
 * Obtain one with {@code REG.family()}.
 * <pre>{@code
 * // Every shape of a vanilla block, without a single new texture
 * BlockFamily GOLD = REG.family().base(Blocks.GOLD_BLOCK).preset(FamilyPreset.METAL).createAll().build();
 *
 * // A new block plus a few variants
 * BlockFamily RUBY_BRICKS = REG.family().newBase("ruby_bricks", Blocks.STONE_BRICKS)
 *         .preset(FamilyPreset.STONE).stairs().slab().wall().light(4).build();
 *
 * // A full wooden family (fuel, wooden tags, doors)
 * BlockFamily MAPLE = REG.family().newBase("maple_planks", Blocks.OAK_PLANKS).preset(FamilyPreset.WOOD)
 *         .createAll().doorTextures("block/maple_door_top", "block/maple_door_bottom", "item/maple_door")
 *         .build();
 * }</pre>
 * Ids are {@code <prefix>_<variant>} ({@code gold_stairs}, {@code ruby_brick_wall}...); see {@link #name(String)}.
 * Recipes (crafting and stonecutter), tags, loot tables, models and names are generated for every variant.
 */
public final class BlockFamilyBuilder {

    private final RegistryContext registry;

    // ---- base
    private ResourceLocation baseId;
    private Supplier<? extends Block> baseSupplier;
    private String newBaseName;
    private Supplier<BlockBehaviour.Properties> newBaseProperties;
    private Function<BlockBehaviour.Properties, ? extends Block> newBaseFactory;

    // ---- appearance
    private String prefix;
    private Textures textures;
    private ResourceLocation baseModel;
    private final Map<Variant, Textures> variantTextures = new EnumMap<>(Variant.class);
    private ResourceLocation doorItemTexture;
    private final Map<Variant, String> displayNames = new EnumMap<>(Variant.class);
    private RenderLayer renderLayer;

    // ---- variants and behaviour
    private final Set<Variant> variants = EnumSet.noneOf(Variant.class);
    private FamilyPreset preset;
    private UnaryOperator<BlockBehaviour.Properties> propertiesModifier = UnaryOperator.identity();
    private final Map<Variant, UnaryOperator<BlockBehaviour.Properties>> variantModifiers = new EnumMap<>(Variant.class);
    private int light = -1;
    private Boolean wooden;
    private WoodType woodType;
    private BlockSetType blockSetType;
    private int buttonTicks = -1;
    private int weightedPlate = -1;
    private final Map<Variant, VariantFactory> factories = new EnumMap<>(Variant.class);
    private int burnOdds = -1;
    private int spreadOdds;
    private int baseSpreadOdds = -1;
    private String basePattern;
    private String baseSubject;
    private String nameSubject;
    private String nameColor;

    // ---- items
    private int fuel = -1;
    private DeferredSupplier<CreativeModeTab> tab;
    private boolean noTab;

    // ---- datagen
    private boolean recipes = true;
    private Boolean stonecutting;
    private Object ingredient;
    private Object stick = "minecraft:stick";
    private MiningTool tool;
    private MiningTool.Tier tier = MiningTool.Tier.ANY;
    private final List<String> extraBlockTags = new ArrayList<>();

    /** Internal: use {@code REG.family()}. */
    @ApiStatus.Internal
    public BlockFamilyBuilder(RegistryContext registry) {
        this.registry = registry;
    }

    // ================================================================ base

    /**
     * Uses an already registered block as the base, e.g. {@code Blocks.GOLD_BLOCK} or a block from another mod.
     * @param block the base block
     * @return this builder
     */
    public BlockFamilyBuilder base(Block block) {
        ResourceLocation id = BuiltInRegistries.BLOCK.getKey(block);
        if (block != BuiltInRegistries.BLOCK.get(id)) {
            throw new IllegalArgumentException("The base block is not registered yet; use base(RegistrySupplier)");
        }
        return base(id, () -> block);
    }

    /**
     * Uses a block registered through Architectury (one of yours, from {@code REG.block(...)} or a material) as the base.
     * @param block the base block supplier
     * @return this builder
     */
    public BlockFamilyBuilder base(RegistrySupplier<? extends Block> block) {
        return base(block.getId(), block);
    }

    /**
     * Uses any block as the base, given its id and a supplier.
     * @param id    the base block id (used for textures and names)
     * @param block supplier of the block
     * @return this builder
     */
    public BlockFamilyBuilder base(ResourceLocation id, Supplier<? extends Block> block) {
        ensureNoBase();
        this.baseId = id;
        this.baseSupplier = block;
        return this;
    }

    /**
     * Creates a new cube {@code <modid>:<name>} as the base, copying the properties of {@code copyFrom}.
     * @param name     base block name
     * @param copyFrom block whose properties are copied
     * @return this builder
     */
    public BlockFamilyBuilder newBase(String name, Block copyFrom) {
        return newBase(name, () -> BlockBehaviour.Properties.ofFullCopy(copyFrom));
    }

    /**
     * Creates a new cube {@code <modid>:<name>} as the base with explicit properties.
     * @param name       base block name
     * @param properties supplier of the properties
     * @return this builder
     */
    public BlockFamilyBuilder newBase(String name, Supplier<BlockBehaviour.Properties> properties) {
        return newBase(name, properties, null);
    }

    /**
     * Creates a new base block with your own class.
     * @param name       base block name
     * @param properties supplier of the properties
     * @param factory    creates the block from its properties
     * @return this builder
     */
    public BlockFamilyBuilder newBase(String name, Supplier<BlockBehaviour.Properties> properties,
                                      Function<BlockBehaviour.Properties, ? extends Block> factory) {
        ensureNoBase();
        this.newBaseName = Objects.requireNonNull(name);
        this.newBaseProperties = Objects.requireNonNull(properties);
        this.newBaseFactory = factory;
        this.baseId = ResourceLocation.fromNamespaceAndPath(registry.modId(), name);
        return this;
    }

    private void ensureNoBase() {
        if (baseId != null) throw new IllegalStateException("The family already has a base block: " + baseId);
    }

    // ================================================================ variants

    /**
     * Adds every shape that works with the base texture alone ({@link Variant.Kind#SHAPE}): no new PNGs needed.
     * @return this builder
     */
    public BlockFamilyBuilder createAll() {
        variants.addAll(Variant.shapes());
        return this;
    }

    /**
     * Adds the decorative blocks ({@link Variant.Kind#DECORATIVE}: polished, chiseled, bricks...). Each needs its own PNG.
     * @return this builder
     */
    public BlockFamilyBuilder createDecorative() {
        variants.addAll(Variant.decorative());
        return this;
    }

    /**
     * {@link #createAll()} plus {@link #createDecorative()}.
     * @return this builder
     */
    public BlockFamilyBuilder createEverything() {
        return createAll().createDecorative();
    }

    /**
     * Adds specific variants.
     * @param variants shape or decorative variants
     * @return this builder
     */
    public BlockFamilyBuilder with(Variant... variants) {
        return with(List.of(variants));
    }

    /**
     * Adds specific variants.
     * @param variants shape or decorative variants
     * @return this builder
     */
    public BlockFamilyBuilder with(Collection<Variant> variants) {
        for (Variant v : variants) {
            if (v == Variant.BASE) throw new IllegalArgumentException("BASE is not a variant; use newBase(...)");
            if (v.kind() == Variant.Kind.SPECIAL) throw new IllegalArgumentException(v + " is not a family variant");
            this.variants.add(v);
        }
        return this;
    }

    /**
     * Removes variants, typically after {@link #createAll()}.
     * @param variants variants to drop
     * @return this builder
     */
    public BlockFamilyBuilder without(Variant... variants) {
        for (Variant v : variants) this.variants.remove(v);
        return this;
    }

    /**
     * Adds {@link Variant#STAIRS}.
     * @return this builder
     */
    public BlockFamilyBuilder stairs() { return with(Variant.STAIRS); }
    /**
     * Adds {@link Variant#SLAB}.
     * @return this builder
     */
    public BlockFamilyBuilder slab() { return with(Variant.SLAB); }
    /**
     * Adds {@link Variant#VERTICAL_SLAB}.
     * @return this builder
     */
    public BlockFamilyBuilder verticalSlab() { return with(Variant.VERTICAL_SLAB); }
    /**
     * Adds {@link Variant#WALL}.
     * @return this builder
     */
    public BlockFamilyBuilder wall() { return with(Variant.WALL); }
    /**
     * Adds {@link Variant#FENCE}.
     * @return this builder
     */
    public BlockFamilyBuilder fence() { return with(Variant.FENCE); }
    /**
     * Adds {@link Variant#FENCE_GATE}.
     * @return this builder
     */
    public BlockFamilyBuilder fenceGate() { return with(Variant.FENCE_GATE); }
    /**
     * Adds {@link Variant#BUTTON}.
     * @return this builder
     */
    public BlockFamilyBuilder button() { return with(Variant.BUTTON); }
    /**
     * Adds {@link Variant#PRESSURE_PLATE}.
     * @return this builder
     */
    public BlockFamilyBuilder pressurePlate() { return with(Variant.PRESSURE_PLATE); }
    /**
     * Adds {@link Variant#DOOR}.
     * @return this builder
     */
    public BlockFamilyBuilder door() { return with(Variant.DOOR); }
    /**
     * Adds {@link Variant#TRAPDOOR}.
     * @return this builder
     */
    public BlockFamilyBuilder trapdoor() { return with(Variant.TRAPDOOR); }
    /**
     * Adds {@link Variant#PANE}.
     * @return this builder
     */
    public BlockFamilyBuilder pane() { return with(Variant.PANE); }
    /**
     * Adds {@link Variant#CARPET}.
     * @return this builder
     */
    public BlockFamilyBuilder carpet() { return with(Variant.CARPET); }
    /**
     * Adds {@link Variant#POLISHED}. Needs its own texture {@code <prefix>_polished}.
     * @return this builder
     */
    public BlockFamilyBuilder polished() { return with(Variant.POLISHED); }
    /**
     * Adds {@link Variant#CHISELED}. Needs its own texture {@code <prefix>_chiseled}.
     * @return this builder
     */
    public BlockFamilyBuilder chiseled() { return with(Variant.CHISELED); }
    /**
     * Adds {@link Variant#CRACKED}. Needs its own texture {@code <prefix>_cracked}.
     * @return this builder
     */
    public BlockFamilyBuilder cracked() { return with(Variant.CRACKED); }
    /**
     * Adds {@link Variant#CUT}. Needs its own texture {@code <prefix>_cut}.
     * @return this builder
     */
    public BlockFamilyBuilder cut() { return with(Variant.CUT); }
    /**
     * Adds {@link Variant#SMOOTH}. Needs its own texture {@code <prefix>_smooth}.
     * @return this builder
     */
    public BlockFamilyBuilder smooth() { return with(Variant.SMOOTH); }
    /**
     * Adds {@link Variant#BRICKS}. Needs its own texture {@code <prefix>_bricks}.
     * @return this builder
     */
    public BlockFamilyBuilder bricks() { return with(Variant.BRICKS); }
    /**
     * Adds {@link Variant#TILES}. Needs its own texture {@code <prefix>_tiles}.
     * @return this builder
     */
    public BlockFamilyBuilder tiles() { return with(Variant.TILES); }
    /**
     * Adds {@link Variant#PILLAR}. Needs its own texture {@code <prefix>_pillar}.
     * @return this builder
     */
    public BlockFamilyBuilder pillar() { return with(Variant.PILLAR); }

    // ================================================================ behaviour

    /**
     * Uses your own class for one variant.
     * <pre>{@code .factory(Variant.STAIRS, (base, p) -> new MyStairs(base.defaultBlockState(), p))}</pre>
     * @param variant the variant
     * @param factory creates the block from the base block and the prepared properties
     * @return this builder
     */
    public BlockFamilyBuilder factory(Variant variant, VariantFactory factory) {
        if (variant == Variant.BASE) throw new IllegalArgumentException("For the base block use newBase(name, props, factory)");
        factories.put(variant, Objects.requireNonNull(factory));
        with(variant);
        return this;
    }

    /**
     * Applies a preset: required tool, door and button sounds, tags and typical recipes.
     * @param preset {@link FamilyPreset#STONE}, {@link FamilyPreset#WOOD} or {@link FamilyPreset#METAL}
     * @return this builder
     */
    public BlockFamilyBuilder preset(FamilyPreset preset) {
        this.preset = Objects.requireNonNull(preset);
        return this;
    }

    /**
     * Light level emitted by every block of the family.
     * @param level 0-15
     * @return this builder
     */
    public BlockFamilyBuilder light(int level) {
        if (level < 0 || level > 15) throw new IllegalArgumentException("Light level must be between 0 and 15");
        this.light = level;
        return this;
    }

    /**
     * Adjusts the properties (copied from the base) of every block of the family.
     * @param modifier function applied to the properties
     * @return this builder
     */
    public BlockFamilyBuilder properties(UnaryOperator<BlockBehaviour.Properties> modifier) {
        this.propertiesModifier = Objects.requireNonNull(modifier);
        return this;
    }

    /**
     * Adjusts the properties of one variant, after the family-wide {@link #properties(UnaryOperator)}.
     * @param variant  the variant
     * @param modifier function applied to the properties
     * @return this builder
     */
    public BlockFamilyBuilder properties(Variant variant, UnaryOperator<BlockBehaviour.Properties> modifier) {
        this.variantModifiers.put(variant, Objects.requireNonNull(modifier));
        return this;
    }

    /**
     * Wooden behaviour: {@code wooden_*} tags, 30-tick buttons, oak sounds and fuel. Also enabled by {@code preset(WOOD)}.
     * @return this builder
     */
    public BlockFamilyBuilder wooden() {
        this.wooden = true;
        return this;
    }

    /**
     * Wood type for fence gates (sounds).
     * @param type e.g. {@code WoodType.OAK}
     * @return this builder
     */
    public BlockFamilyBuilder woodType(WoodType type) {
        this.woodType = type;
        return this;
    }

    /**
     * Sounds and behaviour of doors, trapdoors, buttons and pressure plates.
     * @param type e.g. {@code BlockSetType.IRON} (iron doors cannot be opened by hand)
     * @return this builder
     */
    public BlockFamilyBuilder blockSetType(BlockSetType type) {
        this.blockSetType = type;
        return this;
    }

    /**
     * How long buttons stay pressed.
     * @param ticks stone: 20, wood: 30
     * @return this builder
     */
    public BlockFamilyBuilder buttonTicks(int ticks) {
        this.buttonTicks = ticks;
        return this;
    }

    /**
     * Makes the pressure plate weighted.
     * @param maxWeight entities for the maximum signal (gold: 15, iron: 150)
     * @return this builder
     */
    public BlockFamilyBuilder weightedPressurePlate(int maxWeight) {
        this.weightedPlate = maxWeight;
        return this;
    }

    /**
     * Makes every block of the family burn.
     * @param burnOdds   vanilla wood: 5
     * @param spreadOdds vanilla wood: 20
     * @return this builder
     */
    public BlockFamilyBuilder flammable(int burnOdds, int spreadOdds) {
        this.burnOdds = burnOdds;
        this.spreadOdds = spreadOdds;
        return this;
    }

    /**
     * Internal: names the base block from a lang pattern ({@code "planks"} + {@code "maple"}).
     * @param pattern pattern id
     * @param subject word id
     * @return this builder
     */
    @ApiStatus.Internal
    public BlockFamilyBuilder baseNaming(String pattern, String subject) {
        this.basePattern = pattern;
        this.baseSubject = subject;
        return this;
    }

    /** Variant names with a color ("Red Marble Stairs"). */
    BlockFamilyBuilder colorNaming(String subject, String color) {
        this.nameSubject = subject;
        this.nameColor = color;
        return this;
    }

    /**
     * Every item of the family burns for this long (slabs burn half as long).
     * @param burnTicks burn time in ticks
     * @return this builder
     */
    public BlockFamilyBuilder fuel(int burnTicks) {
        this.fuel = burnTicks;
        return this;
    }

    /**
     * Render layer for every block of the family.
     * @param layer the layer
     * @return this builder
     */
    public BlockFamilyBuilder renderLayer(RenderLayer layer) {
        this.renderLayer = layer;
        return this;
    }

    /**
     * Creative tab for every item of the family.
     * @param tab the tab
     * @return this builder
     */
    public BlockFamilyBuilder tab(DeferredSupplier<CreativeModeTab> tab) {
        this.tab = tab;
        this.noTab = false;
        return this;
    }

    /**
     * Keeps the family out of every creative tab.
     * @return this builder
     */
    public BlockFamilyBuilder noTab() {
        this.noTab = true;
        return this;
    }

    // ================================================================ names and textures

    /**
     * Prefix for the variant ids. By default it is derived from the base:
     * {@code gold_block → gold}, {@code stone_bricks → stone_brick}, {@code oak_planks → oak}.
     * @param prefix id prefix
     * @return this builder
     */
    public BlockFamilyBuilder name(String prefix) {
        this.prefix = prefix;
        return this;
    }

    /**
     * Texture of the base and every shape. Defaults to {@code <namespace>:block/<base>}.
     * @param texture texture id
     * @return this builder
     */
    public BlockFamilyBuilder texture(String texture) {
        this.textures = Textures.all(Refs.parse(registry.modId(), texture));
        return this;
    }

    /**
     * Different side, top and bottom textures for the base and every shape.
     * @param side   side texture id
     * @param top    top texture id
     * @param bottom bottom texture id
     * @return this builder
     */
    public BlockFamilyBuilder textures(String side, String top, String bottom) {
        this.textures = Textures.of(registry.modId(), side, top, bottom);
        return this;
    }

    /**
     * Texture of one specific variant (trapdoor, decorative blocks...).
     * @param variant the variant
     * @param texture texture id
     * @return this builder
     */
    public BlockFamilyBuilder texture(Variant variant, String texture) {
        variantTextures.put(variant, Textures.all(Refs.parse(registry.modId(), texture)));
        return this;
    }

    /**
     * Door textures. Without them the door uses the base texture (and so does its item).
     * @param top    upper half texture id
     * @param bottom lower half texture id
     * @param item   flat item texture id
     * @return this builder
     */
    public BlockFamilyBuilder doorTextures(String top, String bottom, String item) {
        String ns = registry.modId();
        variantTextures.put(Variant.DOOR, new Textures(Refs.parse(ns, bottom), Refs.parse(ns, top), Refs.parse(ns, bottom)));
        this.doorItemTexture = Refs.parse(ns, item);
        return this;
    }

    /**
     * Model used for double slabs. Defaults to {@code <namespace>:block/<base>}.
     * @param model model id
     * @return this builder
     */
    public BlockFamilyBuilder baseModel(String model) {
        this.baseModel = Refs.parse(registry.modId(), model);
        return this;
    }

    /**
     * Sets the English name of one variant instead of deriving it.
     * @param variant the variant
     * @param name    display name
     * @return this builder
     */
    public BlockFamilyBuilder displayName(Variant variant, String name) {
        displayNames.put(variant, name);
        return this;
    }

    // ================================================================ datagen

    /**
     * Tool that mines the family fast ({@code mineable/<tool>} tag).
     * @param tool the tool
     * @return this builder
     */
    public BlockFamilyBuilder tool(MiningTool tool) {
        return tool(tool, MiningTool.Tier.ANY);
    }

    /**
     * Tool and minimum tier for every block of the family.
     * @param tool the tool
     * @param tier minimum tier
     * @return this builder
     */
    public BlockFamilyBuilder tool(MiningTool tool, MiningTool.Tier tier) {
        this.tool = tool;
        this.tier = Objects.requireNonNull(tier);
        return this;
    }

    /**
     * Minimum tool tier for every block of the family.
     * @param tier the tier
     * @return this builder
     */
    public BlockFamilyBuilder tier(MiningTool.Tier tier) {
        this.tier = Objects.requireNonNull(tier);
        return this;
    }

    /**
     * Adds every block of the family to a block tag.
     * @param blockTag tag id
     * @return this builder
     */
    public BlockFamilyBuilder tag(String blockTag) {
        extraBlockTags.add(blockTag);
        return this;
    }

    /**
     * Generates no recipes for the family.
     * @return this builder
     */
    public BlockFamilyBuilder noRecipes() {
        this.recipes = false;
        return this;
    }

    /**
     * Enables or disables stonecutter recipes (enabled by the stone and metal presets).
     * @param enabled whether to generate them
     * @return this builder
     */
    public BlockFamilyBuilder stonecutting(boolean enabled) {
        this.stonecutting = enabled;
        return this;
    }

    /**
     * Ingredient used by the recipes instead of the base block, e.g. ingots for metal doors.
     * @param item item, supplier, id or {@code "#tag"}
     * @return this builder
     */
    public BlockFamilyBuilder ingredient(Object item) {
        this.ingredient = Objects.requireNonNull(item);
        return this;
    }

    /**
     * Stick used by fence and fence-gate recipes (default {@code minecraft:stick}).
     * @param item item, supplier or id
     * @return this builder
     */
    public BlockFamilyBuilder stick(Object item) {
        this.stick = Objects.requireNonNull(item);
        return this;
    }

    // ================================================================ build

    /**
     * Registers every block of the family and queues its datagen.
     * @return the family, to look up its blocks
     */
    public BlockFamily build() {
        if (baseId == null) throw new IllegalStateException("Missing base block: call base(...) or newBase(...)");
        applyPreset();
        boolean wood = Boolean.TRUE.equals(wooden);
        String familyPrefix = prefix != null ? prefix : derivePrefix(baseId.getPath());
        Map<Variant, RegistrySupplier<Block>> blocks = new EnumMap<>(Variant.class);

        // ---- base
        Supplier<? extends Block> base;
        if (newBaseName != null) {
            BlockBuilder b = registry.block(newBaseName).properties(newBaseProperties).modify(propertiesModifier);
            if (newBaseFactory != null) b.factory(newBaseFactory);
            if (light >= 0) b.light(light);
            if (textures != null) b.textures(textures.side().toString(), textures.top().toString(), textures.bottom().toString());
            if (tool != null) b.tool(tool, tier);
            if (renderLayer != null) b.renderLayer(renderLayer);
            if (displayNames.containsKey(Variant.BASE)) b.displayName(displayNames.get(Variant.BASE));
            if (basePattern != null) b.naming(basePattern, baseSubject);
            if (burnOdds >= 0) b.flammable(burnOdds, baseSpreadOdds >= 0 ? baseSpreadOdds : spreadOdds);
            if (noTab) b.noTab(); else if (tab != null) b.tab(tab);
            int f = ShapeSupport.fuel(Variant.BASE, wood, fuel);
            if (f > 0) b.fuel(f);
            extraBlockTags.forEach(b::tag);
            if (wood) b.tag("minecraft:planks").itemTag("minecraft:planks");
            RegistrySupplier<Block> registered = b.build();
            blocks.put(Variant.BASE, registered);
            base = registered;
        } else {
            base = baseSupplier;
        }

        Textures tex = textures != null ? textures : Textures.all(Textures.blockTexture(baseId));
        ResourceLocation fullModel = baseModel != null ? baseModel : Textures.blockTexture(baseId);
        ShapeSupport.Behaviour behaviour = ShapeSupport.Behaviour.of(wood, blockSetType, woodType, buttonTicks, weightedPlate);
        int plateWeight = weightedPlate;

        // ---- variants
        for (Variant variant : variants) {
            String name = variant.idFor(familyPrefix);
            Supplier<BlockBehaviour.Properties> props = propertiesFor(variant, base);
            VariantFactory custom = factories.get(variant);
            Supplier<Block> factory = custom != null
                    ? () -> custom.create(base.get(), ShapeSupport.adjust(variant, props.get()))
                    : () -> ShapeSupport.create(variant, base.get(), ShapeSupport.adjust(variant, props.get()), behaviour);

            int f = ShapeSupport.fuel(variant, wood, fuel);
            RegistryContext.Registered reg = registry.registerBlock(name, factory,
                    new RegistryContext.ItemOptions(tab, noTab, f, UnaryOperator.identity()), true);
            ResourceLocation id = reg.block().getId();
            blocks.put(variant, reg.block());

            registry.addBlockEntry(new BlockEntry(reg.block(), variant, texturesFor(variant, id, tex), fullModel,
                    itemTextureFor(variant, tex), variant == Variant.PRESSURE_PLATE && plateWeight > 0,
                    Drops.self(), renderLayerFor(variant), true, BlockEntry.NO_TINT));
            registry.nameBlock(id, variant.name().toLowerCase(java.util.Locale.ROOT),
                    nameSubject != null ? nameSubject : familyPrefix, nameColor, displayNames.get(variant));
            if (burnOdds >= 0) registry.flammable(reg.block(), burnOdds, spreadOdds);
            addTags(variant, id.toString(), wood);
        }

        if (recipes) {
            FamilyRecipes.generate(registry.autoRecipes(), blocks, ingredient != null ? ingredient : baseId, baseId, stick,
                    stonecutting != null ? stonecutting : !wood, wood);
        }

        BlockFamily family = new BlockFamily(familyPrefix, baseId, base, blocks);
        registry.addFamily(family);
        return family;
    }

    // ================================================================ details

    private void applyPreset() {
        if (preset == null) return;
        switch (preset) {
            case STONE -> {
                if (tool == null) tool = MiningTool.PICKAXE;
                if (stonecutting == null) stonecutting = true;
            }
            case WOOD -> {
                if (tool == null) tool = MiningTool.AXE;
                if (wooden == null) wooden = true;
            }
            case METAL -> {
                if (tool == null) tool = MiningTool.PICKAXE;
                if (blockSetType == null) blockSetType = BlockSetType.IRON;
                if (weightedPlate < 0) weightedPlate = 150;
                if (stonecutting == null) stonecutting = true;
            }
        }
    }

    private Supplier<BlockBehaviour.Properties> propertiesFor(Variant variant, Supplier<? extends Block> base) {
        UnaryOperator<BlockBehaviour.Properties> family = propertiesModifier;
        UnaryOperator<BlockBehaviour.Properties> specific = variantModifiers.getOrDefault(variant, UnaryOperator.identity());
        int lightLevel = light;
        return () -> {
            BlockBehaviour.Properties p = family.apply(BlockBehaviour.Properties.ofFullCopy(base.get()));
            if (lightLevel >= 0) p = p.lightLevel(s -> lightLevel);
            return specific.apply(p);
        };
    }

    private Textures texturesFor(Variant variant, ResourceLocation id, Textures familyTextures) {
        Textures custom = variantTextures.get(variant);
        if (custom != null) return custom;
        return switch (variant.kind()) {
            case DECORATIVE -> variant == Variant.PILLAR
                    ? new Textures(Textures.blockTexture(id), Textures.blockTexture(id).withSuffix("_top"),
                    Textures.blockTexture(id).withSuffix("_top"))
                    : Textures.all(Textures.blockTexture(id));
            default -> familyTextures;
        };
    }

    private ResourceLocation itemTextureFor(Variant variant, Textures familyTextures) {
        return switch (variant) {
            case DOOR -> doorItemTexture != null ? doorItemTexture : familyTextures.side();
            case PANE -> variantTextures.containsKey(Variant.PANE) ? variantTextures.get(Variant.PANE).side() : familyTextures.side();
            default -> null;
        };
    }

    private RenderLayer renderLayerFor(Variant variant) {
        return renderLayer != null ? renderLayer : ShapeSupport.defaultRenderLayer(variant);
    }

    private void addTags(Variant variant, String entry, boolean wood) {
        ShapeSupport.tags(registry, variant, entry, wood);
        if (tool != null) registry.addBlockTag(tool.tag(), entry);
        if (tier.tag() != null) registry.addBlockTag(tier.tag(), entry);
        for (String t : extraBlockTags) registry.addBlockTag(Refs.parse(registry.modId(), t), entry);
    }

    /** gold_block → gold, stone_bricks → stone_brick, oak_planks → oak, bricks → brick. */
    static String derivePrefix(String path) {
        if (path.equals("bricks")) return "brick";
        if (path.endsWith("_block")) return path.substring(0, path.length() - "_block".length());
        if (path.endsWith("_planks")) return path.substring(0, path.length() - "_planks".length());
        if (path.endsWith("_bricks") || path.endsWith("_tiles")) return path.substring(0, path.length() - 1);
        return path;
    }
}
