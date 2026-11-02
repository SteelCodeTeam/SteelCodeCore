// SPDX-License-Identifier: LGPL-3.0-or-later
package dev.steelcode.core.api.item;

import dev.architectury.registry.registries.DeferredSupplier;
import dev.architectury.registry.registries.RegistrySupplier;
import dev.steelcode.core.api.block.MiningTool;
import dev.steelcode.core.api.data.Recipes;
import dev.steelcode.core.impl.registry.RegistryContext;
import dev.steelcode.core.impl.util.Refs;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.AxeItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.DiggerItem;
import net.minecraft.world.item.HoeItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.PickaxeItem;
import net.minecraft.world.item.ShovelItem;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.Tier;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.block.Block;
import org.jetbrains.annotations.ApiStatus;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;

/**
 * Tools (sword, pickaxe, axe, shovel, hoe), armor and extra gear of a material, with recipes, tags and recipe
 * unlocks. Obtain one with {@code REG.equipment(name, repairItem)}.
 * <pre>{@code
 * REG.equipment("ruby", RUBY).tools(ToolStats.DIAMOND.withUses(1200)).armor(ArmorStats.DIAMOND).build();
 *
 * // Netherite-style upgrade on the smithing table, from diamond gear
 * REG.equipment("mythril", MYTHRIL_INGOT).tools(ToolStats.NETHERITE)
 *         .upgradeFrom("minecraft:diamond", Items.NETHERITE_UPGRADE_SMITHING_TEMPLATE).build();
 *
 * // Bow, shield and horse armor
 * REG.equipment("ruby_extra", RUBY).bow(500).shield(400).armor(ArmorStats.IRON).horseArmor().build();
 * }</pre>
 * Textures: {@code item/<name>_sword}, {@code item/<name>_helmet}... and the worn armor in
 * {@code textures/models/armor/<name>_layer_1.png} and {@code <name>_layer_2.png}.
 */
public final class EquipmentBuilder {

    private final RegistryContext registry;
    private final String name;
    private final Object repair;
    private ToolStats tools;
    private ArmorStats armor;
    private Holder<SoundEvent> equipSound = SoundEvents.ARMOR_EQUIP_IRON;
    private boolean fireResistant;
    private boolean recipes = true;
    private String upgradeBase;
    private Object upgradeTemplate;
    private DeferredSupplier<CreativeModeTab> tab;
    private int bow = -1, crossbow = -1, fishingRod = -1, shield = -1;
    private boolean horseArmor;

    /** Internal: use {@code REG.equipment(name, repairItem)}. */
    @ApiStatus.Internal
    public EquipmentBuilder(RegistryContext registry, String name, Object repair) {
        this.registry = registry;
        this.name = Objects.requireNonNull(name);
        this.repair = Objects.requireNonNull(repair);
    }

    /**
     * Creates the five tools.
     * @param stats tool stats, e.g. {@link ToolStats#DIAMOND}
     * @return this builder
     */
    public EquipmentBuilder tools(ToolStats stats) {
        this.tools = Objects.requireNonNull(stats);
        return this;
    }

    /**
     * Creates the four armor pieces and their armor material.
     * @param stats armor stats, e.g. {@link ArmorStats#IRON}
     * @return this builder
     */
    public EquipmentBuilder armor(ArmorStats stats) {
        this.armor = Objects.requireNonNull(stats);
        return this;
    }

    /**
     * Creates a bow {@code <name>_bow}. Textures {@code _bow} and {@code _bow_pulling_0/1/2}.
     * @param durability uses (vanilla: 384)
     * @return this builder
     */
    public EquipmentBuilder bow(int durability) {
        this.bow = durability;
        return this;
    }

    /**
     * Creates a crossbow {@code <name>_crossbow}. Textures {@code _crossbow_standby}, {@code _crossbow_pulling_0/1/2},
     * {@code _crossbow_arrow} and {@code _crossbow_firework}.
     * @param durability uses (vanilla: 465)
     * @return this builder
     */
    public EquipmentBuilder crossbow(int durability) {
        this.crossbow = durability;
        return this;
    }

    /**
     * Creates a fishing rod {@code <name>_fishing_rod}. Textures {@code _fishing_rod} and {@code _fishing_rod_cast}.
     * @param durability uses (vanilla: 64)
     * @return this builder
     */
    public EquipmentBuilder fishingRod(int durability) {
        this.fishingRod = durability;
        return this;
    }

    /**
     * Creates a shield {@code <name>_shield}. Texture {@code textures/entity/shield/<name>_shield.png}, 64x64 like
     * vanilla {@code shield_base_nopattern}.
     * @param durability uses (vanilla: 336)
     * @return this builder
     */
    public EquipmentBuilder shield(int durability) {
        this.shield = durability;
        return this;
    }

    /**
     * Creates horse armor (requires {@link #armor}). Texture {@code textures/entity/horse/armor/horse_armor_<name>.png}.
     * @return this builder
     */
    public EquipmentBuilder horseArmor() {
        this.horseArmor = true;
        return this;
    }

    /**
     * @param sound sound played when equipping the armor (default: iron)
     * @return this builder
     */
    public EquipmentBuilder equipSound(Holder<SoundEvent> sound) {
        this.equipSound = Objects.requireNonNull(sound);
        return this;
    }

    /**
     * Every piece survives fire and lava.
     * @return this builder
     */
    public EquipmentBuilder fireResistant() {
        this.fireResistant = true;
        return this;
    }

    /**
     * Generates no recipes.
     * @return this builder
     */
    public EquipmentBuilder noRecipes() {
        this.recipes = false;
        return this;
    }

    /**
     * Instead of crafting, the gear is upgraded on the smithing table from other gear; the addition is the repair item.
     * @param basePrefix       id prefix of the base gear: {@code "minecraft:diamond"} means {@code diamond_sword}...
     * @param smithingTemplate the template item, supplier or id
     * @return this builder
     */
    public EquipmentBuilder upgradeFrom(String basePrefix, Object smithingTemplate) {
        this.upgradeBase = basePrefix;
        this.upgradeTemplate = smithingTemplate;
        return this;
    }

    /**
     * Creative tab of every piece.
     * @param tab the tab
     * @return this builder
     */
    public EquipmentBuilder tab(DeferredSupplier<CreativeModeTab> tab) {
        this.tab = tab;
        return this;
    }

    // ================================================================ build

    /**
     * Registers every piece and queues models, names, tags and recipes.
     * @return the created equipment
     */
    public EquipmentSet build() {
        RegistrySupplier<Item> sword = null, pickaxe = null, axe = null, shovel = null, hoe = null;
        RegistrySupplier<Item> helmet = null, chest = null, legs = null, boots = null;
        RegistrySupplier<ArmorMaterial> material = null;

        if (tools != null) {
            Tier tier = new SimpleTier(tools, incorrectFor(tools.level()), repair);
            sword = tool("sword", "minecraft:swords", p -> new SwordItem(tier, p.attributes(SwordItem.createAttributes(tier, 3, -2.4F))));
            pickaxe = tool("pickaxe", "minecraft:pickaxes", p -> new PickaxeItem(tier, p.attributes(DiggerItem.createAttributes(tier, 1.0F, -2.8F))));
            axe = tool("axe", "minecraft:axes", p -> new AxeItem(tier, p.attributes(DiggerItem.createAttributes(tier, 6.0F, -3.1F))));
            shovel = tool("shovel", "minecraft:shovels", p -> new ShovelItem(tier, p.attributes(DiggerItem.createAttributes(tier, 1.5F, -3.0F))));
            hoe = tool("hoe", "minecraft:hoes", p -> new HoeItem(tier, p.attributes(DiggerItem.createAttributes(tier, -2.0F, -1.0F))));
        }
        if (armor != null) {
            ArmorStats a = armor;
            Object rep = repair;
            Holder<SoundEvent> sound = equipSound;
            ResourceLocation layer = ResourceLocation.fromNamespaceAndPath(registry.modId(), name);
            registry.expectTexture("assets/" + registry.modId() + "/textures/models/armor/" + name + "_layer_1.png");
            registry.expectTexture("assets/" + registry.modId() + "/textures/models/armor/" + name + "_layer_2.png");
            material = registry.registerArmorMaterial(name, () -> {
                Map<ArmorItem.Type, Integer> defense = new EnumMap<>(ArmorItem.Type.class);
                defense.put(ArmorItem.Type.HELMET, a.helmet());
                defense.put(ArmorItem.Type.CHESTPLATE, a.chestplate());
                defense.put(ArmorItem.Type.LEGGINGS, a.leggings());
                defense.put(ArmorItem.Type.BOOTS, a.boots());
                defense.put(ArmorItem.Type.BODY, a.chestplate());
                return new ArmorMaterial(defense, a.enchantability(), sound, () -> Refs.runtimeIngredient(rep),
                        List.of(new ArmorMaterial.Layer(layer)), a.toughness(), a.knockbackResistance());
            });
            helmet = armorPiece("helmet", ArmorItem.Type.HELMET, "minecraft:head_armor", material);
            chest = armorPiece("chestplate", ArmorItem.Type.CHESTPLATE, "minecraft:chest_armor", material);
            legs = armorPiece("leggings", ArmorItem.Type.LEGGINGS, "minecraft:leg_armor", material);
            boots = armorPiece("boots", ArmorItem.Type.BOOTS, "minecraft:foot_armor", material);
        }

        // ---- weapons and extras
        RegistrySupplier<Item> bowItem = null, crossbowItem = null, rodItem = null, shieldItem = null, horse = null;
        if (bow > 0) {
            int d = bow;
            bowItem = item("bow", p -> new net.minecraft.world.item.BowItem(p.durability(d))).model(ItemBuilder.Model.BOW)
                    .tag("minecraft:enchantable/bow").tag("minecraft:enchantable/durability").tag("minecraft:enchantable/vanishing").build();
            RegistrySupplier<Item> ref = bowItem;
            registry.clientLate(() -> dev.steelcode.core.impl.client.SteelCodeCoreClient.bowProperties(ref.get()));
        }
        if (crossbow > 0) {
            int d = crossbow;
            crossbowItem = item("crossbow", p -> new net.minecraft.world.item.CrossbowItem(p.durability(d)))
                    .model(ItemBuilder.Model.CROSSBOW).tag("minecraft:enchantable/crossbow").tag("minecraft:enchantable/durability")
                    .tag("minecraft:enchantable/vanishing").build();
            RegistrySupplier<Item> ref = crossbowItem;
            registry.clientLate(() -> dev.steelcode.core.impl.client.SteelCodeCoreClient.crossbowProperties(ref.get()));
        }
        if (fishingRod > 0) {
            int d = fishingRod;
            rodItem = item("fishing_rod", p -> new net.minecraft.world.item.FishingRodItem(p.durability(d)))
                    .model(ItemBuilder.Model.FISHING_ROD).tag("minecraft:enchantable/fishing").tag("minecraft:enchantable/durability")
                    .tag("minecraft:enchantable/vanishing").build();
            RegistrySupplier<Item> ref = rodItem;
            registry.clientLate(() -> dev.steelcode.core.impl.client.SteelCodeCoreClient.fishingRodProperties(ref.get()));
        }
        if (shield > 0) {
            int d = shield;
            Object rep = repair;
            shieldItem = item("shield", p -> new net.minecraft.world.item.ShieldItem(p.durability(d)) {
                @Override
                public boolean isValidRepairItem(net.minecraft.world.item.ItemStack stack, net.minecraft.world.item.ItemStack repairWith) {
                    return Refs.runtimeIngredient(rep).test(repairWith);
                }
            }).model(ItemBuilder.Model.SHIELD).tag("minecraft:enchantable/durability").tag("minecraft:enchantable/vanishing").build();
            RegistrySupplier<Item> ref = shieldItem;
            ResourceLocation tex = ResourceLocation.fromNamespaceAndPath(registry.modId(), "textures/entity/shield/" + name + "_shield.png");
            registry.expectTexture("assets/" + tex.getNamespace() + "/" + tex.getPath());
            registry.clientEarly(() -> dev.steelcode.core.impl.platform.CorePlatform.itemRenderer(ref,
                    dev.steelcode.core.impl.client.ShieldRendering.forTexture(tex)));
            registry.clientLate(() -> dev.steelcode.core.impl.client.SteelCodeCoreClient.shieldProperties(ref.get()));
        }
        if (horseArmor) {
            if (material == null) throw new IllegalStateException(name + ": horseArmor() needs armor(...)");
            RegistrySupplier<ArmorMaterial> mat = material;
            registry.expectTexture("assets/" + registry.modId() + "/textures/entity/horse/armor/horse_armor_" + name + ".png");
            horse = item("horse_armor", p -> new net.minecraft.world.item.AnimalArmorItem(mat,
                    net.minecraft.world.item.AnimalArmorItem.BodyType.EQUESTRIAN, false, p.stacksTo(1))).build();
        }

        EquipmentSet set = new EquipmentSet(name, Optional.ofNullable(sword), Optional.ofNullable(pickaxe),
                Optional.ofNullable(axe), Optional.ofNullable(shovel), Optional.ofNullable(hoe), Optional.ofNullable(helmet),
                Optional.ofNullable(chest), Optional.ofNullable(legs), Optional.ofNullable(boots), Optional.ofNullable(material),
                Optional.ofNullable(bowItem), Optional.ofNullable(crossbowItem), Optional.ofNullable(rodItem),
                Optional.ofNullable(shieldItem), Optional.ofNullable(horse));

        if (recipes) {
            Recipes r = registry.autoRecipes();
            if (upgradeBase != null) {
                for (String piece : new String[]{"sword", "pickaxe", "axe", "shovel", "hoe", "helmet", "chestplate", "leggings", "boots"}) {
                    RegistrySupplier<Item> item = byName(set, piece);
                    if (item != null) r.smithing(upgradeTemplate, upgradeBase + "_" + piece, repair, item);
                }
            } else {
                r.toolSet(repair, sword, pickaxe, axe, shovel, hoe);
                r.armorSet(repair, helmet, chest, legs, boots);
            }
            if (set.bow().isPresent()) r.shaped(set.bow().get()).category("equipment").book("combat").pattern(" MS", "M S", " MS")
                    .define('M', repair).define('S', "minecraft:string").save();
            if (set.crossbow().isPresent()) r.shaped(set.crossbow().get()).category("equipment").book("combat").pattern("#M#", "SHS", " # ")
                    .define('#', "minecraft:stick").define('M', repair).define('S', "minecraft:string")
                    .define('H', "minecraft:tripwire_hook").save();
            if (set.fishingRod().isPresent()) r.shaped(set.fishingRod().get()).category("equipment").book("tools").pattern("  M", " MS", "M S")
                    .define('M', repair).define('S', "minecraft:string").save();
            if (set.shield().isPresent()) r.shaped(set.shield().get()).category("equipment").book("combat").pattern("W#W", "WWW", " W ")
                    .define('W', "#minecraft:planks").define('#', repair).save();
            if (set.horseArmor().isPresent()) r.shaped(set.horseArmor().get()).category("misc").book("combat").pattern("X X", "XXX", "X X")
                    .define('X', repair).save();
        }
        registry.addEquipment(set);
        return set;
    }

    // ================================================================ helpers

    private RegistrySupplier<Item> tool(String kind, String tag, Function<Item.Properties, Item> factory) {
        return item(kind, factory).handheld().tag(tag).build();
    }

    private RegistrySupplier<Item> armorPiece(String kind, ArmorItem.Type type, String tag,
                                              RegistrySupplier<ArmorMaterial> material) {
        int mult = armor.durability();
        return item(kind, p -> new ArmorItem(material, type, p.durability(type.getDurability(mult))))
                .tag(tag).tag("minecraft:trimmable_armor").build();
    }

    private ItemBuilder item(String kind, Function<Item.Properties, Item> factory) {
        ItemBuilder b = registry.item(name + "_" + kind).factory(factory).naming(kind, name);
        if (fireResistant) b.fireResistant();
        if (tab != null) b.tab(tab);
        return b;
    }

    private static RegistrySupplier<Item> byName(EquipmentSet s, String piece) {
        return (switch (piece) {
            case "sword" -> s.sword();
            case "pickaxe" -> s.pickaxe();
            case "axe" -> s.axe();
            case "shovel" -> s.shovel();
            case "hoe" -> s.hoe();
            case "helmet" -> s.helmet();
            case "chestplate" -> s.chestplate();
            case "leggings" -> s.leggings();
            default -> s.boots();
        }).orElse(null);
    }

    private static TagKey<Block> incorrectFor(MiningTool.Tier level) {
        return switch (level) {
            case ANY -> BlockTags.INCORRECT_FOR_WOODEN_TOOL;
            case STONE -> BlockTags.INCORRECT_FOR_STONE_TOOL;
            case IRON -> BlockTags.INCORRECT_FOR_IRON_TOOL;
            case DIAMOND -> BlockTags.INCORRECT_FOR_DIAMOND_TOOL;
        };
    }

    /** Vanilla {@code Tier} built from {@link ToolStats}. */
    private record SimpleTier(ToolStats stats, TagKey<Block> incorrect, Object repair) implements Tier {
        @Override public int getUses() { return stats.uses(); }
        @Override public float getSpeed() { return stats.speed(); }
        @Override public float getAttackDamageBonus() { return stats.attackBonus(); }
        @Override public TagKey<Block> getIncorrectBlocksForDrops() { return incorrect; }
        @Override public int getEnchantmentValue() { return stats.enchantability(); }
        @Override public Ingredient getRepairIngredient() { return Refs.runtimeIngredient(repair); }
    }
}
