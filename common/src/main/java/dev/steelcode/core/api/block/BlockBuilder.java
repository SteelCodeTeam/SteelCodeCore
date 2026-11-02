// SPDX-License-Identifier: LGPL-3.0-or-later
package dev.steelcode.core.api.block;

import dev.architectury.registry.registries.DeferredSupplier;
import dev.architectury.registry.registries.RegistrySupplier;
import dev.steelcode.core.api.data.RecipeTemplate;
import dev.steelcode.core.impl.registry.BlockEntry;
import dev.steelcode.core.impl.registry.ItemModelSpec;
import dev.steelcode.core.impl.registry.RegistryContext;
import dev.steelcode.core.impl.registry.ShapeSupport;
import dev.steelcode.core.impl.util.Refs;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.valueproviders.ConstantInt;
import net.minecraft.util.valueproviders.IntProvider;
import net.minecraft.util.valueproviders.UniformInt;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.DropExperienceBlock;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.properties.BlockSetType;
import net.minecraft.world.level.block.state.properties.WoodType;
import org.jetbrains.annotations.ApiStatus;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.function.UnaryOperator;

/**
 * Builds a single block (and, by default, its {@code BlockItem}). Obtain one with {@code REG.block(name)}.
 * <p>
 * Everything is optional: with no calls at all you get a full cube that drops itself, textured
 * {@code <modid>:block/<name>}, placed in the mod's default creative tab and named after its id.
 * <pre>{@code
 * // Ore: gives XP, drops rubies affected by Fortune, drops itself with Silk Touch
 * RegistrySupplier<Block> RUBY_ORE = REG.block("ruby_ore")
 *         .copy(Blocks.DIAMOND_ORE)
 *         .xp(3, 7)
 *         .drops(Drops.ore(RUBY))
 *         .tool(MiningTool.PICKAXE, MiningTool.Tier.IRON)
 *         .build();
 *
 * // Glowing planks that burn in a furnace
 * REG.block("glow_planks").copy(Blocks.OAK_PLANKS).light(12).fuel(300).build();
 *
 * // Pillar like a log: textures glow_log (side) and glow_log_top (ends)
 * REG.block("glow_log").copy(Blocks.OAK_LOG).pillar().build();
 *
 * // Stairs with your own class, gold textures and the vanilla stairs recipe
 * REG.block("bouncy_gold_stairs")
 *         .shape(Variant.STAIRS, Blocks.GOLD_BLOCK)
 *         .factory(p -> new BouncyStairs(Blocks.GOLD_BLOCK.defaultBlockState(), p))
 *         .defaultRecipe()
 *         .build();
 * }</pre>
 * <h2>Flexible references</h2>
 * Parameters typed {@code Object} that name an item or block accept an {@code ItemLike}, a {@code Supplier} /
 * {@code RegistrySupplier}, an id string ({@code "mymod:ruby"}; without a namespace it means {@code minecraft:}) and,
 * where a tag makes sense, {@code "#c:ingots/tin"}. They are resolved lazily, so you may reference content that is
 * declared later. Tag and texture names passed as {@code String} parameters default to your namespace instead.
 * <p>
 * A builder is single-use: call {@link #build()} once.
 */
public final class BlockBuilder {

    private final RegistryContext registry;
    private final String name;
    private Supplier<BlockBehaviour.Properties> properties = BlockBehaviour.Properties::of;
    private UnaryOperator<BlockBehaviour.Properties> modifier = UnaryOperator.identity();
    private Function<BlockBehaviour.Properties, ? extends Block> factory;
    private Variant model = Variant.BASE;
    private Textures textures;
    private IntProvider xp;
    private int light = -1;
    private Drops drops = Drops.self();
    private MiningTool tool;
    private MiningTool.Tier tier = MiningTool.Tier.ANY;
    private final List<String> blockTags = new ArrayList<>();
    private final List<String> itemTags = new ArrayList<>();
    private RenderLayer renderLayer;
    private String displayName;
    private DeferredSupplier<CreativeModeTab> tab;
    private boolean noTab;
    private int fuel;
    private boolean withItem = true;
    private UnaryOperator<Item.Properties> itemProperties = UnaryOperator.identity();
    private boolean propertiesSet;

    // shape
    private ResourceLocation shapeBaseId;
    private Supplier<? extends Block> shapeBase;
    private boolean wooden;
    private BlockSetType blockSetType;
    private WoodType woodType;
    private int weightedPlate = -1;

    // recipes
    private final List<RecipeTemplate> recipeTemplates = new ArrayList<>();
    private String namePattern;
    private String nameSubject;
    private String nameColor;
    private final List<String> infos = new ArrayList<>();
    private boolean hidden;
    private java.util.function.Function<net.minecraft.world.level.block.state.BlockState, net.minecraft.network.chat.Component> tooltipLine;
    private Boolean connectedSeeThrough;
    private boolean randomRotation;
    private int randomTextures;
    private boolean toggleable;
    private int tint = BlockEntry.NO_TINT;
    private float compostChance;
    private int burnOdds = -1;
    private int spreadOdds;
    private boolean noLoot;
    private ItemModelSpec itemModel;
    private int param;
    private int param2;
    private int containerRows;
    private boolean containerOrientable;
    private Integer fallingDust;
    private dev.steelcode.core.content.block.BehaviourBlock.StepAction step;
    private boolean defaultRecipe;
    private Object defaultIngredient;
    private Object stick = "minecraft:stick";

    /** Internal: use {@code REG.block(name)}. */
    @ApiStatus.Internal
    public BlockBuilder(RegistryContext registry, String name) {
        this.registry = registry;
        this.name = Objects.requireNonNull(name);
    }

    // ================================================================ properties and class

    /**
     * Copies every property (hardness, resistance, sound, map color, required tool...) from another block.
     * @param block the block to copy
     * @return this builder
     */
    public BlockBuilder copy(Block block) {
        this.properties = () -> BlockBehaviour.Properties.ofFullCopy(block);
        this.propertiesSet = true;
        return this;
    }

    /**
     * Uses explicit block properties instead of copying a block.
     * @param properties supplier of fresh properties (called at registration time)
     * @return this builder
     */
    public BlockBuilder properties(Supplier<BlockBehaviour.Properties> properties) {
        this.properties = Objects.requireNonNull(properties);
        this.propertiesSet = true;
        return this;
    }

    /**
     * Tweaks the properties after {@link #copy} or {@link #properties}. Calls chain in order.
     * <pre>{@code REG.block("soft_stone").copy(Blocks.STONE).modify(p -> p.strength(0.5f)).build();}</pre>
     * @param modifier function applied to the properties
     * @return this builder
     */
    public BlockBuilder modify(UnaryOperator<BlockBehaviour.Properties> modifier) {
        UnaryOperator<BlockBehaviour.Properties> previous = this.modifier;
        this.modifier = p -> modifier.apply(previous.apply(p));
        return this;
    }

    /**
     * Sets hardness and blast resistance (stone is {@code 1.5f, 6f}).
     * @param hardness   time to mine
     * @param resistance blast resistance
     * @return this builder
     */
    public BlockBuilder strength(float hardness, float resistance) {
        return modify(p -> p.strength(hardness, resistance));
    }

    /**
     * Light level the block emits.
     * @param level 0 (none) to 15 (like glowstone)
     * @return this builder
     */
    public BlockBuilder light(int level) {
        if (level < 0 || level > 15) throw new IllegalArgumentException("Light level must be between 0 and 15");
        this.light = level;
        return this;
    }

    /**
     * Uses your own block class. The function receives the final properties.
     * <pre>{@code .factory(MyMachineBlock::new)}</pre>
     * @param factory creates the block from its properties
     * @return this builder
     */
    public BlockBuilder factory(Function<BlockBehaviour.Properties, ? extends Block> factory) {
        this.factory = Objects.requireNonNull(factory);
        return this;
    }

    // ================================================================ shape

    /**
     * Gives the block the shape of a variant (stairs, slab, door...) derived from {@code base}.
     * <p>
     * Models use the base block's texture, and the right tags and render layer are added. Without {@link #factory}
     * the vanilla class for that shape is used. Properties are copied from the base unless you call
     * {@link #copy} or {@link #properties}.
     * @param variant the shape; must not be a {@link Variant.Kind#SPECIAL} variant
     * @param base    the base block: a {@code Block}, a supplier or an id string
     * @return this builder
     */
    public BlockBuilder shape(Variant variant, Object base) {
        if (variant == Variant.BASE) throw new IllegalArgumentException("BASE is the default shape; pass a shape variant");
        this.model = variant;
        if (base instanceof RegistrySupplier<?> rs) {
            this.shapeBaseId = rs.getId();
            @SuppressWarnings("unchecked")
            Supplier<? extends Block> sup = (Supplier<? extends Block>) rs;
            this.shapeBase = sup;
        } else if (base instanceof Block b) {
            this.shapeBaseId = BuiltInRegistries.BLOCK.getKey(b);
            this.shapeBase = () -> b;
        } else {
            throw new IllegalArgumentException("The base must be a Block, a supplier or an id: " + base);
        }
        return this;
    }

    /**
     * Marks a shaped block as wooden: {@code wooden_*} tags, vanilla fuel times and wooden door/button behaviour.
     * @return this builder
     */
    public BlockBuilder wooden() {
        this.wooden = true;
        return this;
    }

    /**
     * Block set type for doors, trapdoors, buttons and pressure plates (sounds, who can open it).
     * @param type e.g. {@code BlockSetType.IRON}
     * @return this builder
     */
    public BlockBuilder blockSetType(BlockSetType type) {
        this.blockSetType = type;
        return this;
    }

    /**
     * Wood type for fence gates (sounds).
     * @param type e.g. {@code WoodType.OAK}
     * @return this builder
     */
    public BlockBuilder woodType(WoodType type) {
        this.woodType = type;
        return this;
    }

    /**
     * Turns a pressure-plate shape into a weighted plate (like gold or iron plates).
     * @param maxWeight entities needed for the maximum signal (gold: 15, iron: 150)
     * @return this builder
     */
    public BlockBuilder weightedPressurePlate(int maxWeight) {
        this.weightedPlate = maxWeight;
        return this;
    }

    // ================================================================ recipes

    /**
     * Adds a recipe whose result is this block.
     * <pre>{@code .recipe(RecipeTemplate.stairs(Blocks.GOLD_BLOCK))}</pre>
     * @param template the recipe template; see {@link RecipeTemplate}
     * @return this builder
     */
    public BlockBuilder recipe(RecipeTemplate template) {
        recipeTemplates.add(Objects.requireNonNull(template));
        return this;
    }

    /**
     * Adds the vanilla recipe for this block's {@link #shape shape}, using the shape's base block as ingredient.
     * @return this builder
     */
    public BlockBuilder defaultRecipe() {
        this.defaultRecipe = true;
        return this;
    }

    /**
     * Adds the vanilla recipe for this block's shape with a different ingredient (e.g. ingots for a metal door).
     * @param ingredient item, supplier, id or {@code "#tag"}
     * @return this builder
     */
    public BlockBuilder defaultRecipe(Object ingredient) {
        this.defaultRecipe = true;
        this.defaultIngredient = Objects.requireNonNull(ingredient);
        return this;
    }

    /**
     * Stick item used by {@link #defaultRecipe} for fences and fence gates (default {@code minecraft:stick}).
     * @param item item, supplier or id
     * @return this builder
     */
    public BlockBuilder stick(Object item) {
        this.stick = Objects.requireNonNull(item);
        return this;
    }

    // ================================================================ model and textures

    /**
     * Makes an orientable pillar ({@code RotatedPillarBlock}) with textures {@code <name>} and {@code <name>_top}.
     * @return this builder
     */
    public BlockBuilder pillar() {
        this.model = Variant.PILLAR;
        return this;
    }

    /**
     * Makes a cross-shaped plant (flowers, saplings, grass) with the {@code cross} model and cutout layer.
     * Add {@code noCollission()} to the properties.
     * @return this builder
     */
    public BlockBuilder cross() {
        this.model = Variant.CROSS;
        if (renderLayer == null) renderLayer = RenderLayer.CUTOUT;
        return this;
    }

    /**
     * Internal: selects a special model generator (leaves, pots, tinted cubes...).
     * @param variant the model variant
     * @return this builder
     */
    @ApiStatus.Internal
    public BlockBuilder model(Variant variant) {
        this.model = variant;
        return this;
    }

    /**
     * Internal: client tint, a fixed RGB or {@link BlockEntry#FOLIAGE_TINT}. The model must declare a tint index.
     * @param tint RGB color or a tint constant
     * @return this builder
     */
    @ApiStatus.Internal
    public BlockBuilder tint(int tint) {
        this.tint = tint;
        return this;
    }

    /**
     * Internal: derives the display name from a lang pattern and a subject word.
     * @param pattern pattern id, e.g. {@code "stairs"}
     * @param subject word id, e.g. {@code "tin"}
     * @return this builder
     */
    @ApiStatus.Internal
    public BlockBuilder naming(String pattern, String subject) {
        this.namePattern = pattern;
        this.nameSubject = subject;
        return this;
    }

    /** Pattern + word + modifier ({@code "@exposed"}): "Exposed Tin Stairs". */
    BlockBuilder namingModified(String pattern, String subject, String modifier) {
        this.namePattern = pattern;
        this.nameSubject = subject;
        this.nameColor = modifier;
        return this;
    }

    /**
     * Adds an information page in JEI, REI and EMI. Call several times for several paragraphs.
     * Translate it with {@code REG.lang(code).set("info.<modid>.<name>.<n>", text)}.
     * @param english the text in English
     * @return this builder
     */
    public BlockBuilder info(String english) {
        infos.add(english);
        return this;
    }

    /**
     * Hides the block's item from JEI, REI and EMI.
     * @return this builder
     */
    public BlockBuilder hideInViewers() {
        this.hidden = true;
        return this;
    }

    /**
     * Adds a line to the Jade / WTHIT tooltip while the player looks at the block.
     * @param line computes the line from the block state; return {@code null} for no line
     * @return this builder
     */
    public BlockBuilder tooltipLine(java.util.function.Function<net.minecraft.world.level.block.state.BlockState, net.minecraft.network.chat.Component> line) {
        this.tooltipLine = line;
        return this;
    }

    /**
     * Connected textures: borders are drawn only on sides with no identical neighbour.
     * <p>
     * Textures: {@code <name>} (with border, also used by the item) and {@code <name>_center} (borderless).
     * Implemented with blockstates and multipart models only, so it works on both loaders and with Sodium.
     * @param seeThrough behave like glass (translucent, no inner faces, no suffocation)
     * @return this builder
     */
    public BlockBuilder connected(boolean seeThrough) {
        this.connectedSeeThrough = seeThrough;
        this.model = Variant.CONNECTED;
        if (seeThrough && renderLayer == null) renderLayer = RenderLayer.CUTOUT;
        return this;
    }

    /**
     * Rotates the texture randomly per position (like dirt or sand) to hide repetition.
     * @return this builder
     */
    public BlockBuilder randomRotation() {
        this.randomRotation = true;
        return this;
    }

    /**
     * Picks one of several textures per position: {@code <name>}, {@code <name>_1} ... {@code <name>_<count-1>}.
     * @param count number of textures, at least 2
     * @return this builder
     */
    public BlockBuilder randomTextures(int count) {
        this.randomTextures = count;
        return this;
    }

    /**
     * Right-click toggles the block on and off (light 15 when on). Textures {@code <name>} and {@code <name>_on}.
     * Uses {@link dev.steelcode.core.content.block.ToggleBlock}.
     * @return this builder
     */
    public BlockBuilder toggleable() {
        this.toggleable = true;
        this.model = Variant.LAMP;
        return this;
    }

    BlockBuilder namingColored(String subject, String color) {
        this.namePattern = "self";
        this.nameSubject = subject;
        this.nameColor = color;
        return this;
    }

    /**
     * Lets the block's item be composted.
     * @param chance probability 0-1 of raising the composter level (leaves: 0.3, melon: 0.65)
     * @return this builder
     */
    public BlockBuilder compostable(float chance) {
        this.compostChance = chance;
        return this;
    }

    /**
     * Makes the block burn and spread fire.
     * @param burnOdds   how easily it catches fire (logs 5, planks 5, leaves 30, wool 30)
     * @param spreadOdds how easily it is destroyed by fire (logs 5, planks 20, leaves 60, wool 60)
     * @return this builder
     */
    public BlockBuilder flammable(int burnOdds, int spreadOdds) {
        this.burnOdds = burnOdds;
        this.spreadOdds = spreadOdds;
        return this;
    }

    // ================================================================ functional blocks

    /**
     * Gives the block an inventory that opens a chest-style menu and drops its contents when broken.
     * <p>
     * Orientable containers face the player and use textures {@code <name>_front}, {@code <name>_side} and
     * {@code <name>_top}; others use a single texture.
     * @param rows       rows of 9 slots, 1-6
     * @param orientable whether the block faces the player when placed
     * @return this builder
     */
    public BlockBuilder container(int rows, boolean orientable) {
        if (rows < 1 || rows > 6) throw new IllegalArgumentException("Containers have 1 to 6 rows");
        this.containerRows = rows;
        this.containerOrientable = orientable;
        if (orientable) this.model = Variant.ORIENTABLE;
        return this;
    }

    /**
     * Redstone lamp: light 15 while powered. Textures {@code <name>} and {@code <name>_on}.
     * @return this builder
     */
    public BlockBuilder redstoneLamp() {
        this.model = Variant.LAMP;
        return this;
    }

    /**
     * Falls like sand or gravel.
     * @param dustColor RGB color of the falling particles
     * @return this builder
     */
    public BlockBuilder falling(int dustColor) {
        this.fallingDust = dustColor;
        return this;
    }

    /**
     * Climbable ladder that attaches to walls. Uses the cutout layer and a flat item.
     * @return this builder
     */
    public BlockBuilder ladder() {
        this.model = Variant.LADDER;
        if (renderLayer == null) renderLayer = RenderLayer.CUTOUT;
        tag("minecraft:climbable");
        return this;
    }

    /**
     * Runs an action whenever an entity walks on the block.
     * @param action called on both sides; check {@code level.isClientSide()} yourself
     * @return this builder
     */
    public BlockBuilder onStep(dev.steelcode.core.content.block.BehaviourBlock.StepAction action) {
        this.step = action;
        return this;
    }

    /**
     * Applies a status effect to living entities walking on the block.
     * <pre>{@code .stepEffect(MobEffects.MOVEMENT_SPEED, 3, 1)}</pre>
     * @param effect    the effect
     * @param seconds   duration
     * @param amplifier 0 = level I
     * @return this builder
     */
    public BlockBuilder stepEffect(net.minecraft.core.Holder<net.minecraft.world.effect.MobEffect> effect, int seconds, int amplifier) {
        return onStep((level, pos, state, entity) -> {
            if (!level.isClientSide && entity instanceof net.minecraft.world.entity.LivingEntity living) {
                living.addEffect(new net.minecraft.world.effect.MobEffectInstance(effect, seconds * 20, amplifier));
            }
        });
    }

    /**
     * Hurts entities walking on it, like magma (sneaking players are safe).
     * @param amount damage per step, in half hearts
     * @return this builder
     */
    public BlockBuilder stepDamage(float amount) {
        return onStep((level, pos, state, entity) -> {
            if (!entity.isSteppingCarefully() && entity instanceof net.minecraft.world.entity.LivingEntity) {
                entity.hurt(level.damageSources().hotFloor(), amount);
            }
        });
    }

    // ================================================================ item models

    /**
     * Uses a flat item model with this texture instead of the 3D block.
     * @param texture texture id, e.g. {@code "mymod:item/rope"}
     * @return this builder
     */
    public BlockBuilder flatItem(String texture) {
        this.itemModel = ItemModelSpec.flat(Refs.parse(registry.modId(), texture));
        return this;
    }

    /**
     * Uses an existing model as the item model, unchanged.
     * @param parent model id, e.g. {@code "mymod:block/crate_closed"}
     * @return this builder
     */
    public BlockBuilder itemModel(String parent) {
        this.itemModel = ItemModelSpec.model(Refs.parse(registry.modId(), parent));
        return this;
    }

    /**
     * Skips the loot table (you provide your own JSON, or the block drops nothing).
     * @return this builder
     */
    public BlockBuilder noLoot() {
        this.noLoot = true;
        return this;
    }

    /**
     * Internal: extra parameters for special model variants.
     * @param param  first parameter
     * @param param2 second parameter
     * @return this builder
     */
    @ApiStatus.Internal
    public BlockBuilder param(int param, int param2) {
        this.param = param;
        this.param2 = param2;
        return this;
    }

    /**
     * Skips blockstate and model generation: you provide them as resources.
     * @return this builder
     */
    public BlockBuilder customModel() {
        this.model = null;
        return this;
    }

    /**
     * Uses one texture for every face.
     * @param texture texture id; no namespace means your mod, e.g. {@code "block/marble"}
     * @return this builder
     */
    public BlockBuilder texture(String texture) {
        this.textures = Textures.all(Refs.parse(registry.modId(), texture));
        return this;
    }

    /**
     * Uses different side, top and bottom textures (pillars only use side and top).
     * @param side   side texture id
     * @param top    top texture id
     * @param bottom bottom texture id
     * @return this builder
     */
    public BlockBuilder textures(String side, String top, String bottom) {
        this.textures = Textures.of(registry.modId(), side, top, bottom);
        return this;
    }

    /**
     * Overrides the render layer (glass, leaves and plants pick one automatically).
     * @param layer the layer
     * @return this builder
     */
    public BlockBuilder renderLayer(RenderLayer layer) {
        this.renderLayer = layer;
        return this;
    }

    // ================================================================ drops and XP

    /**
     * Drops experience when mined, like ores. No XP is dropped with Silk Touch.
     * @param min minimum XP
     * @param max maximum XP
     * @return this builder
     */
    public BlockBuilder xp(int min, int max) {
        this.xp = UniformInt.of(min, max);
        return this;
    }

    /**
     * Drops a fixed amount of experience when mined (none with Silk Touch).
     * @param amount XP points
     * @return this builder
     */
    public BlockBuilder xp(int amount) {
        this.xp = ConstantInt.of(amount);
        return this;
    }

    /**
     * What the block drops. See {@link Drops}: {@code Drops.ore(RUBY)}, {@code Drops.silkTouchOnly()}...
     * @param drops the drop rule
     * @return this builder
     */
    public BlockBuilder drops(Drops drops) {
        this.drops = Objects.requireNonNull(drops);
        return this;
    }

    /**
     * Shortcut: drops {@code item} without Silk Touch and the block itself with Silk Touch.
     * @param item    the item, supplier or id
     * @param min     minimum count
     * @param max     maximum count
     * @param fortune whether Fortune increases the count
     * @return this builder
     */
    public BlockBuilder dropsWithoutSilkTouch(Object item, int min, int max, boolean fortune) {
        return drops(Drops.ore(item).count(min, max).fortune(fortune));
    }

    // ================================================================ tags

    /**
     * Adds the {@code mineable/<tool>} tag (the tool that mines it fast).
     * @param tool the tool
     * @return this builder
     */
    public BlockBuilder tool(MiningTool tool) {
        return tool(tool, MiningTool.Tier.ANY);
    }

    /**
     * Adds the {@code mineable/<tool>} tag and the minimum tool tier. Requires {@code requiresCorrectToolForDrops()}
     * in the properties (copied from vanilla ores automatically).
     * @param tool the tool
     * @param tier minimum tier, e.g. {@link MiningTool.Tier#IRON}
     * @return this builder
     */
    public BlockBuilder tool(MiningTool tool, MiningTool.Tier tier) {
        this.tool = tool;
        this.tier = Objects.requireNonNull(tier);
        return this;
    }

    /**
     * Adds the block to a block tag.
     * @param blockTag tag id, e.g. {@code "minecraft:beacon_base_blocks"}
     * @return this builder
     */
    public BlockBuilder tag(String blockTag) {
        blockTags.add(blockTag);
        return this;
    }

    /**
     * Adds the block's item to an item tag.
     * @param itemTag tag id, e.g. {@code "c:storage_blocks/ruby"}
     * @return this builder
     */
    public BlockBuilder itemTag(String itemTag) {
        itemTags.add(itemTag);
        return this;
    }

    // ================================================================ item

    /**
     * Sets the English name instead of deriving it from the id.
     * @param name display name, e.g. {@code "Block of Ruby"}
     * @return this builder
     */
    public BlockBuilder displayName(String name) {
        this.displayName = name;
        return this;
    }

    /**
     * Makes the block's item a furnace fuel.
     * @param burnTicks burn time in ticks (vanilla planks: 300, coal: 1600)
     * @return this builder
     */
    public BlockBuilder fuel(int burnTicks) {
        this.fuel = burnTicks;
        return this;
    }

    /**
     * Puts the item in a specific creative tab instead of the mod's default one.
     * @param tab the tab
     * @return this builder
     */
    public BlockBuilder tab(DeferredSupplier<CreativeModeTab> tab) {
        this.tab = tab;
        this.noTab = false;
        return this;
    }

    /**
     * Keeps the item out of every creative tab.
     * @return this builder
     */
    public BlockBuilder noTab() {
        this.noTab = true;
        return this;
    }

    /**
     * Adjusts the {@code BlockItem}'s properties (rarity, stack size, fire resistance...).
     * @param properties function applied to the item properties
     * @return this builder
     */
    public BlockBuilder itemProperties(UnaryOperator<Item.Properties> properties) {
        this.itemProperties = Objects.requireNonNull(properties);
        return this;
    }

    /**
     * Registers no {@code BlockItem} (technical blocks, wall variants...).
     * @return this builder
     */
    public BlockBuilder noItem() {
        this.withItem = false;
        return this;
    }

    // ================================================================ build

    /**
     * Registers the block (and its item) and queues its datagen.
     * @return the block supplier; the item is available as {@code block.get().asItem()} after registration
     */
    public RegistrySupplier<Block> build() {
        if (xp != null && factory != null) {
            throw new IllegalStateException(name + ": xp() creates a DropExperienceBlock; with factory() handle XP in your own class");
        }
        boolean shaped = shapeBase != null;
        if (shaped && xp != null) throw new IllegalStateException(name + ": xp() only works on full blocks");
        if (defaultRecipe && !shaped) throw new IllegalStateException(name + ": defaultRecipe() needs shape(...)");
        Variant shape = model;
        Supplier<? extends Block> base = shapeBase;
        Supplier<BlockBehaviour.Properties> props = shaped && !propertiesSet
                ? () -> BlockBehaviour.Properties.ofFullCopy(base.get()) : properties;
        UnaryOperator<BlockBehaviour.Properties> mod = modifier;
        int lightLevel = light;
        Supplier<BlockBehaviour.Properties> finalProps0 = () -> {
            BlockBehaviour.Properties p = mod.apply(props.get());
            if (lightLevel >= 0) p = p.lightLevel(s -> lightLevel);
            return shaped ? ShapeSupport.adjust(shape, p) : p;
        };
        Supplier<BlockBehaviour.Properties> finalProps = finalProps0;

        if (model == Variant.LAMP) {
            Supplier<BlockBehaviour.Properties> base0 = finalProps;
            finalProps = () -> base0.get().lightLevel(st -> st.getValue(net.minecraft.world.level.block.state.properties.BlockStateProperties.LIT) ? 15 : 0);
        }
        if (model == Variant.LADDER) {
            Supplier<BlockBehaviour.Properties> base0 = finalProps;
            finalProps = () -> base0.get().noOcclusion();
        }
        @SuppressWarnings("unchecked")
        RegistrySupplier<net.minecraft.world.level.block.entity.BlockEntityType<dev.steelcode.core.content.block.CoreContainerBlockEntity>>[] beType =
                new RegistrySupplier[1];
        Function<BlockBehaviour.Properties, ? extends Block> f;
        if (factory != null) f = factory;
        else if (connectedSeeThrough != null) {
            boolean see = connectedSeeThrough;
            f = p -> new dev.steelcode.core.content.block.ConnectedBlock(see ? p.noOcclusion() : p, see);
        } else if (toggleable) f = dev.steelcode.core.content.block.ToggleBlock::new;
        else if (containerRows > 0) {
            int rows = containerRows;
            boolean orient = containerOrientable;
            f = p -> new dev.steelcode.core.content.block.CoreContainerBlock(p, rows, orient, () -> beType[0].get());
        } else if (model == Variant.LAMP) f = net.minecraft.world.level.block.RedstoneLampBlock::new;
        else if (model == Variant.LADDER) f = net.minecraft.world.level.block.LadderBlock::new;
        else if (fallingDust != null) {
            int dust = fallingDust;
            f = p -> new net.minecraft.world.level.block.ColoredFallingBlock(new net.minecraft.util.ColorRGBA(0xFF000000 | dust), p);
        } else if (step != null && xp == null) {
            var action = step;
            f = p -> new dev.steelcode.core.content.block.BehaviourBlock(p, action);
        } else if (xp != null) {
            IntProvider x = xp;
            f = p -> new DropExperienceBlock(x, p);
        } else if (shaped) {
            ShapeSupport.Behaviour behaviour = ShapeSupport.Behaviour.of(wooden, blockSetType, woodType, -1, weightedPlate);
            f = p -> ShapeSupport.create(shape, base.get(), p, behaviour);
        } else if (model == Variant.PILLAR) f = RotatedPillarBlock::new;
        else f = Block::new;
        Function<BlockBehaviour.Properties, ? extends Block> ff = f;

        int fuelTicks = fuel > 0 ? fuel : (shaped ? ShapeSupport.fuel(shape, wooden, -1) : 0);
        Supplier<BlockBehaviour.Properties> pp = finalProps;
        RegistryContext.Registered reg = registry.registerBlock(name, () -> ff.apply(pp.get()),
                new RegistryContext.ItemOptions(tab, noTab, fuelTicks, itemProperties), withItem);
        ResourceLocation id = reg.block().getId();
        if (containerRows > 0 && factory == null) {
            RegistrySupplier<Block> blockRef = reg.block();
            beType[0] = registry.registerBlockEntity(name, () -> net.minecraft.world.level.block.entity.BlockEntityType.Builder
                    .<dev.steelcode.core.content.block.CoreContainerBlockEntity>of((pos, state) ->
                            new dev.steelcode.core.content.block.CoreContainerBlockEntity(beType[0].get(), pos, state, containerRows), blockRef.get())
                    .build(null));
        }

        Textures tex = textures;
        if (tex == null) {
            ResourceLocation own = Textures.blockTexture(shaped ? shapeBaseId : id);
            tex = model == Variant.PILLAR ? new Textures(own, own.withSuffix("_top"), own.withSuffix("_top"))
                    : model == Variant.ORIENTABLE ? new Textures(own.withSuffix("_side"), own.withSuffix("_top"), own.withSuffix("_front"))
                    : Textures.all(own);
        }
        ResourceLocation fullModel = Textures.blockTexture(shaped ? shapeBaseId : id);
        ResourceLocation itemTexture = model == Variant.DOOR || model == Variant.PANE ? tex.side() : null;
        RenderLayer layer = renderLayer != null ? renderLayer : (shaped ? ShapeSupport.defaultRenderLayer(shape) : null);
        if (model == Variant.LADDER && itemTexture == null) itemTexture = tex.side();
        registry.addBlockEntry(new BlockEntry(reg.block(), model, tex, fullModel, itemTexture,
                model == Variant.PRESSURE_PLATE && weightedPlate > 0, drops, layer, withItem, tint, itemModel,
                model == Variant.BASE ? randomTextures : param, model == Variant.BASE ? (randomRotation ? 1 : 0) : param2, noLoot));
        registry.nameBlock(id, namePattern != null ? namePattern : "self", nameSubject != null ? nameSubject : id.getPath(),
                nameColor, displayName);
        if (compostChance > 0 && reg.item() != null) registry.compostable(reg.item(), compostChance);
        if (burnOdds >= 0) registry.flammable(reg.block(), burnOdds, spreadOdds);
        if (reg.item() != null) {
            for (String i : infos) registry.viewerInfo(reg.item(), i);
            if (hidden) registry.hideInViewers(reg.item());
        }
        if (tooltipLine != null) registry.blockTooltip(reg.block(), tooltipLine);

        String entry = id.toString();
        if (shaped) ShapeSupport.tags(registry, shape, entry, wooden);
        if (tool != null) registry.addBlockTag(tool.tag(), entry);
        if (tier.tag() != null) registry.addBlockTag(tier.tag(), entry);
        for (String t : blockTags) registry.addBlockTag(Refs.parse(registry.modId(), t), entry);
        if (withItem) for (String t : itemTags) registry.addItemTag(Refs.parse(registry.modId(), t), entry);

        if (defaultRecipe) {
            ShapeSupport.recipe(registry.recipes(), shape, reg.block(),
                    defaultIngredient != null ? defaultIngredient : shapeBaseId, shapeBaseId, null, stick, wooden);
        }
        for (RecipeTemplate t : recipeTemplates) t.apply(registry.recipes(), reg.block());
        return reg.block();
    }
}
