// SPDX-License-Identifier: LGPL-3.0-or-later
package dev.steelcode.core.api.item;

import dev.architectury.registry.registries.DeferredSupplier;
import dev.architectury.registry.registries.RegistrySupplier;
import dev.steelcode.core.api.data.RecipeTemplate;
import dev.steelcode.core.impl.registry.ItemEntry;
import dev.steelcode.core.impl.registry.ItemModelSpec;
import dev.steelcode.core.impl.registry.RegistryContext;
import dev.steelcode.core.impl.util.Refs;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import org.jetbrains.annotations.ApiStatus;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;
import java.util.function.UnaryOperator;

/**
 * Builds a single item. Obtain one with {@code REG.item(name)}.
 * <pre>{@code
 * RegistrySupplier<Item> RUBY      = REG.item("ruby").tag("c:gems/ruby").build();
 * RegistrySupplier<Item> COAL_BIT  = REG.item("coal_bit").fuel(400).build();         // smelts 2 items
 * RegistrySupplier<Item> BERRY     = REG.item("berry").food(2, 0.3f).fastFood().build();
 * RegistrySupplier<Item> WAND      = REG.item("wand").stacksTo(1).glint()
 *         .onUse((level, player, hand, stack) -> { player.heal(2); return null; })
 *         .build();
 * RegistrySupplier<Item> RUBY_SWORD = REG.item("ruby_sword")
 *         .factory(p -> new SwordItem(RubyTier.INSTANCE, p)).handheld()
 *         .recipe(RecipeTemplate.sword(RUBY))
 *         .build();
 * }</pre>
 * By default the item is a flat {@link Model#GENERATED} item textured {@code <modid>:item/<name>}, placed in the
 * mod's default creative tab and named after its id. Behaviour hooks ({@link #onUse}, {@link #onHit}...) and
 * tooltips work without writing an item class; they use {@link dev.steelcode.core.content.item.CoreItem}.
 */
public final class ItemBuilder {

    /** How the item model is generated. */
    public enum Model {
        /** {@code minecraft:item/generated}: flat items. */
        GENERATED,
        /** {@code minecraft:item/handheld}: tools and weapons. */
        HANDHELD,
        /** No model is generated; provide your own. */
        NONE,
        /**
         * Large tool ({@code steelcodecore:item/handheld_large}): twice the size in hand, for greatswords and hammers.
         */
        HANDHELD_LARGE,
        /** Bow: textures {@code <id>} and {@code <id>_pulling_0/1/2}. */
        BOW,
        /** Crossbow: {@code <id>_standby}, {@code <id>_pulling_0/1/2}, {@code <id>_arrow}, {@code <id>_firework}. */
        CROSSBOW,
        /** Fishing rod: {@code <id>} and {@code <id>_cast}. */
        FISHING_ROD,
        /** Shield: drawn by the vanilla model with {@code textures/entity/shield/<id>.png}. */
        SHIELD,
        /** A custom parent model, see {@link #modelParent}. */
        CUSTOM
    }

    private final RegistryContext registry;
    private final String name;
    private static final Function<Item.Properties, Item> DEFAULT_FACTORY = Item::new;
    private Function<Item.Properties, ? extends Item> factory = DEFAULT_FACTORY;
    private UnaryOperator<Item.Properties> properties = UnaryOperator.identity();
    private Model model = Model.GENERATED;
    private ResourceLocation texture;
    private String displayName;
    private DeferredSupplier<CreativeModeTab> tab;
    private boolean noTab;
    private int fuel;
    private final List<String> tags = new ArrayList<>();
    private final List<RecipeTemplate> recipeTemplates = new ArrayList<>();
    private String namePattern = "self";
    private String nameSubject;
    private float compostChance;
    private final List<String> infos = new ArrayList<>();
    private boolean hidden;
    private ItemModelSpec customModel;
    private final List<String> tooltips = new ArrayList<>();
    private dev.steelcode.core.content.item.CoreItem.Use use;
    private dev.steelcode.core.content.item.CoreItem.UseOn useOn;
    private dev.steelcode.core.content.item.CoreItem.Hit hit;
    private int nutrition = -1;
    private float saturation;
    private boolean alwaysEdible;
    private boolean fastFood;
    private final List<Object[]> foodEffects = new ArrayList<>();

    /** Internal: use {@code REG.item(name)}. */
    @ApiStatus.Internal
    public ItemBuilder(RegistryContext registry, String name) {
        this.registry = registry;
        this.name = Objects.requireNonNull(name);
    }

    /**
     * Uses your own item class.
     * <pre>{@code .factory(p -> new SwordItem(tier, p))}</pre>
     * @param factory creates the item from its final properties
     * @return this builder
     */
    public ItemBuilder factory(Function<Item.Properties, ? extends Item> factory) {
        this.factory = Objects.requireNonNull(factory);
        return this;
    }

    /**
     * Adjusts the {@code Item.Properties}. Calls chain in order.
     * @param modifier function applied to the properties
     * @return this builder
     */
    public ItemBuilder properties(UnaryOperator<Item.Properties> modifier) {
        UnaryOperator<Item.Properties> previous = this.properties;
        this.properties = p -> modifier.apply(previous.apply(p));
        return this;
    }

    /**
     * @param size maximum stack size, 1-99
     * @return this builder
     */
    public ItemBuilder stacksTo(int size) {
        return properties(p -> p.stacksTo(size));
    }

    /**
     * Makes the item damageable (implies a stack size of 1).
     * @param durability uses before breaking
     * @return this builder
     */
    public ItemBuilder durability(int durability) {
        return properties(p -> p.durability(durability));
    }

    /**
     * @param rarity name color and rarity
     * @return this builder
     */
    public ItemBuilder rarity(Rarity rarity) {
        return properties(p -> p.rarity(rarity));
    }

    /**
     * The item survives fire and lava, like netherite.
     * @return this builder
     */
    public ItemBuilder fireResistant() {
        return properties(Item.Properties::fireResistant);
    }

    /**
     * Makes the item edible. Combine with {@link #foodEffect}, {@link #alwaysEdible} and {@link #fastFood}.
     * @param nutrition  hunger points restored (half shanks)
     * @param saturation saturation modifier (bread: 0.6)
     * @return this builder
     */
    public ItemBuilder food(int nutrition, float saturation) {
        this.nutrition = nutrition;
        this.saturation = saturation;
        return this;
    }

    /**
     * Applies an effect when eaten.
     * <pre>{@code .foodEffect(MobEffects.REGENERATION, 5, 1, 1.0f)   // Regeneration II for 5 s, always}</pre>
     * @param effect      the effect
     * @param seconds     duration
     * @param amplifier   0 = level I
     * @param probability 0-1
     * @return this builder
     */
    public ItemBuilder foodEffect(net.minecraft.core.Holder<net.minecraft.world.effect.MobEffect> effect, int seconds,
                                  int amplifier, float probability) {
        foodEffects.add(new Object[]{effect, seconds, amplifier, probability});
        return this;
    }

    /**
     * Can be eaten when not hungry, like golden apples.
     * @return this builder
     */
    public ItemBuilder alwaysEdible() {
        this.alwaysEdible = true;
        return this;
    }

    /**
     * Is eaten quickly, like berries.
     * @return this builder
     */
    public ItemBuilder fastFood() {
        this.fastFood = true;
        return this;
    }

    /**
     * Adds an information page in JEI, REI and EMI. Call several times for several paragraphs.
     * Translate it with {@code REG.lang(code).set("info.<modid>.<name>.<n>", text)}.
     * @param english the text in English
     * @return this builder
     */
    public ItemBuilder info(String english) {
        infos.add(english);
        return this;
    }

    /**
     * Hides the item from JEI, REI and EMI.
     * @return this builder
     */
    public ItemBuilder hideInViewers() {
        this.hidden = true;
        return this;
    }

    /**
     * Always shows the enchantment glint.
     * @return this builder
     */
    public ItemBuilder glint() {
        return properties(p -> p.component(net.minecraft.core.component.DataComponents.ENCHANTMENT_GLINT_OVERRIDE, true));
    }

    /**
     * Adds a gray tooltip line. Call several times for several lines.
     * Translate it with {@code REG.lang(code).set("item.<modid>.<name>.tooltip.<n>", text)}.
     * @param english the text in English
     * @return this builder
     */
    public ItemBuilder tooltip(String english) {
        tooltips.add(english);
        return this;
    }

    /**
     * What happens when the item is used in the air (right click).
     * @param use the action; return {@code null} to keep the default behaviour (eating, etc.)
     * @return this builder
     */
    public ItemBuilder onUse(dev.steelcode.core.content.item.CoreItem.Use use) {
        this.use = use;
        return this;
    }

    /**
     * What happens when the item is used on a block.
     * @param useOn the action; return {@code null} to keep the default behaviour
     * @return this builder
     */
    public ItemBuilder onUseOnBlock(dev.steelcode.core.content.item.CoreItem.UseOn useOn) {
        this.useOn = useOn;
        return this;
    }

    /**
     * What happens when the item hits an entity.
     * @param hit the action
     * @return this builder
     */
    public ItemBuilder onHit(dev.steelcode.core.content.item.CoreItem.Hit hit) {
        this.hit = hit;
        return this;
    }

    /**
     * Uses fully custom food properties.
     * @param food the food properties
     * @return this builder
     */
    public ItemBuilder food(FoodProperties food) {
        return properties(p -> p.food(food));
    }

    /**
     * Makes the item a furnace fuel.
     * @param burnTicks burn time in ticks (200 smelts one item; coal is 1600)
     * @return this builder
     */
    public ItemBuilder fuel(int burnTicks) {
        this.fuel = burnTicks;
        return this;
    }

    /**
     * Uses the {@link Model#HANDHELD} model (held like a tool).
     * @return this builder
     */
    public ItemBuilder handheld() {
        this.model = Model.HANDHELD;
        return this;
    }

    /**
     * Generates no item model ({@link Model#NONE}).
     * @return this builder
     */
    public ItemBuilder noModel() {
        this.model = Model.NONE;
        return this;
    }

    /**
     * Uses the {@link Model#HANDHELD_LARGE} model (greatswords, hammers).
     * @return this builder
     */
    public ItemBuilder largeHandheld() {
        this.model = Model.HANDHELD_LARGE;
        return this;
    }

    /**
     * Selects how the model is generated (bow, crossbow, shield...).
     * @param model the model type
     * @return this builder
     */
    public ItemBuilder model(Model model) {
        this.model = model;
        return this;
    }

    /**
     * Uses a custom parent model ({@link Model#CUSTOM}).
     * @param parent  parent model id
     * @param texture {@code layer0} texture id, or {@code null} to inherit the parent unchanged
     * @return this builder
     */
    public ItemBuilder modelParent(String parent, String texture) {
        this.model = Model.CUSTOM;
        this.customModel = new ItemModelSpec(Refs.parse("minecraft", parent),
                texture == null ? null : Refs.parse(registry.modId(), texture));
        return this;
    }

    /**
     * @param texture texture id; defaults to {@code <modid>:item/<name>}
     * @return this builder
     */
    public ItemBuilder texture(String texture) {
        this.texture = Refs.parse(registry.modId(), texture);
        return this;
    }

    /**
     * Lets the item be composted.
     * @param chance probability 0-1 of raising the composter level (seeds 0.3, apples 0.65)
     * @return this builder
     */
    public ItemBuilder compostable(float chance) {
        this.compostChance = chance;
        return this;
    }

    /**
     * Internal: derives the name from a lang pattern, e.g. {@code naming("sword", "ruby")}.
     * @param pattern pattern id
     * @param subject word id
     * @return this builder
     */
    @ApiStatus.Internal
    public ItemBuilder naming(String pattern, String subject) {
        this.namePattern = pattern;
        this.nameSubject = subject;
        return this;
    }

    /**
     * Sets the English name instead of deriving it from the id. Other languages: {@code REG.lang(code).set(...)}.
     * @param displayName display name
     * @return this builder
     */
    public ItemBuilder displayName(String displayName) {
        this.displayName = displayName;
        return this;
    }

    /**
     * Adds the item to an item tag.
     * @param tag tag id, e.g. {@code "c:gems/ruby"} or {@code "minecraft:beacon_payment_items"}
     * @return this builder
     */
    public ItemBuilder tag(String tag) {
        tags.add(tag);
        return this;
    }

    /**
     * Adds a recipe whose result is this item.
     * <pre>{@code .recipe(RecipeTemplate.sword(RUBY))}</pre>
     * @param template the recipe template
     * @return this builder
     */
    public ItemBuilder recipe(RecipeTemplate template) {
        recipeTemplates.add(Objects.requireNonNull(template));
        return this;
    }

    /**
     * Puts the item in a specific creative tab instead of the mod's default one.
     * @param tab the tab
     * @return this builder
     */
    public ItemBuilder tab(DeferredSupplier<CreativeModeTab> tab) {
        this.tab = tab;
        this.noTab = false;
        return this;
    }

    /**
     * Keeps the item out of every creative tab.
     * @return this builder
     */
    public ItemBuilder noTab() {
        this.noTab = true;
        return this;
    }

    /**
     * Registers the item and queues its model, name, tags and recipes.
     * @return the item supplier
     */
    public RegistrySupplier<Item> build() {
        if (nutrition >= 0) {
            int n = nutrition;
            float sat = saturation;
            boolean always = alwaysEdible, fast = fastFood;
            List<Object[]> effects = List.copyOf(foodEffects);
            properties(p -> {
                FoodProperties.Builder fb = new FoodProperties.Builder().nutrition(n).saturationModifier(sat);
                if (always) fb.alwaysEdible();
                if (fast) fb.fast();
                for (Object[] e : effects) {
                    @SuppressWarnings("unchecked")
                    var holder = (net.minecraft.core.Holder<net.minecraft.world.effect.MobEffect>) e[0];
                    fb.effect(new net.minecraft.world.effect.MobEffectInstance(holder, (int) e[1] * 20, (int) e[2]), (float) e[3]);
                }
                return p.food(fb.build());
            });
        }
        List<String> tooltipKeys = new ArrayList<>();
        for (int i = 0; i < tooltips.size(); i++) {
            String key = "item." + registry.modId() + "." + name + ".tooltip." + i;
            tooltipKeys.add(key);
            registry.literal(key, tooltips.get(i));
        }
        boolean behaviour = !tooltipKeys.isEmpty() || use != null || useOn != null || hit != null;
        if (behaviour && factory != (Object) DEFAULT_FACTORY) {
            throw new IllegalStateException(name + ": tooltip/onUse/onHit only work without factory(); implement them in your class");
        }
        if (behaviour) {
            var b = new dev.steelcode.core.content.item.CoreItem.Behaviour(List.copyOf(tooltipKeys), use, useOn, hit);
            factory = p -> new dev.steelcode.core.content.item.CoreItem(p, b);
        }
        Function<Item.Properties, ? extends Item> f = factory;
        UnaryOperator<Item.Properties> props = properties;
        RegistrySupplier<Item> item = registry.registerItem(name, () -> f.apply(props.apply(new Item.Properties())),
                new RegistryContext.ItemOptions(tab, noTab, fuel, UnaryOperator.identity()));
        ResourceLocation tex = texture != null ? texture : ResourceLocation.fromNamespaceAndPath(registry.modId(), "item/" + name);
        registry.addItemEntry(new ItemEntry(item, model, tex, customModel));
        registry.nameItem(item.getId(), namePattern, nameSubject != null ? nameSubject : name, displayName);
        if (compostChance > 0) registry.compostable(item, compostChance);
        for (String i : infos) registry.viewerInfo(item, i);
        if (hidden) registry.hideInViewers(item);
        for (String tag : tags) {
            registry.addItemTag(Refs.parse(registry.modId(), tag), item.getId().toString());
        }
        for (RecipeTemplate t : recipeTemplates) t.apply(registry.recipes(), item);
        return item;
    }
}
