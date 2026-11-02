---
title: Items
order: 4
---
# Items

## Single items

```java
RegistrySupplier<Item> RUBY     = REG.item("ruby").tag("c:gems/ruby").build();
RegistrySupplier<Item> COAL_BIT = REG.item("coal_bit").fuel(400).build();          // smelts 2 items
RegistrySupplier<Item> BERRY    = REG.item("berry").food(2, 0.3f).fastFood().compostable(0.3f).build();
RegistrySupplier<Item> PEPPER   = REG.item("spicy_pepper").food(2, 0.2f)
        .foodEffect(MobEffects.FIRE_RESISTANCE, 10, 0, 1f).alwaysEdible().build();
```

| Method | Effect |
|---|---|
| `stacksTo(n)`, `durability(n)`, `rarity(...)`, `fireResistant()` | common properties |
| `properties(p -> ...)` | any other `Item.Properties` tweak |
| `food(nutrition, saturation)`, `foodEffect(...)`, `alwaysEdible()`, `fastFood()`, `food(FoodProperties)` | food |
| `fuel(ticks)`, `compostable(chance)` | furnace fuel, composter |
| `tag("c:gems/ruby")`, `recipe(RecipeTemplate.sword(RUBY))` | tags and recipes |
| `glint()`, `tooltip("...")`, `info("...")`, `hideInViewers()` | presentation |
| `factory(p -> new MyItem(p))` | your own class |

### Behaviour without a class

```java
REG.item("wand").stacksTo(1).glint()
        .tooltip("Heals you a little")
        .onUse((level, player, hand, stack) -> { player.heal(2); return null; })      // null = default behaviour
        .onUseOnBlock(ctx -> { /* ... */ return null; })
        .onHit((stack, target, attacker) -> target.igniteForSeconds(3))
        .build();
```

These lambdas use `CoreItem`, so they cannot be combined with `factory(...)`; override the methods in your own
class instead.

### Models

| Method | Model |
|---|---|
| default | `item/generated` (flat) |
| `handheld()` | held like a tool |
| `largeHandheld()` | twice the size in hand (greatswords, hammers) |
| `model(ItemBuilder.Model.BOW)` and others | bow, crossbow, fishing rod, shield models with their predicates |
| `modelParent(parent, texture)` | any parent model |
| `texture("item/x")`, `noModel()` | texture override / no model |

## Creative tabs

```java
public static final RegistrySupplier<CreativeModeTab> TAB = REG.tab("main")
        .icon(() -> RUBY.get())        // lazy: may be declared later
        .title("Ruby Mod")
        .build();
```

The first tab is the default for everything registered by this registry. Use `makeDefault()` on another tab,
`REG.defaultTab(CreativeModeTabs.INGREDIENTS)` for a vanilla one, or `.tab(...)` / `.noTab()` per builder.

## Materials

A material creates every piece a metal or gem needs, with recipes, loot, tags and optional world generation:

```java
MaterialSet TIN  = REG.material("tin").metal().tier(MiningTool.Tier.STONE).generate(9, 10, -24, 56).build();
MaterialSet RUBY = REG.material("ruby").gem().tier(MiningTool.Tier.IRON).family(f -> f.createAll()).build();
MaterialSet BRONZE = REG.material("bronze").alloy().tools(ToolStats.IRON).build();
```

| Type | Creates |
|---|---|
| metal | `tin_ingot`, `tin_nugget`, `raw_tin`, `tin_block`, `raw_tin_block`, `tin_ore`, `deepslate_tin_ore` |
| gem | `ruby`, `ruby_block`, `ruby_ore`, `deepslate_ruby_ore` |

Options: `ores(stone, deepslate, nether)`, `noOres()`, `noRaw()`, `noNugget()`, `alloy()`, `oreXp(min, max)`,
`oreDrops(min, max)`, `smeltingXp(xp)`, `light(n)`, `fuel(ticks)`, `fireResistant()`, `noBeacon()`,
`displayName("...")`, `family(...)`, `tools(...)`, `armor(...)`, `generate(...)`.

Tags follow the common convention used by both loaders: `c:ingots/tin`, `c:nuggets/tin`, `c:raw_materials/tin`,
`c:storage_blocks/tin`, `c:ores/tin`, `c:ores_in_ground/stone`... plus the beacon tags.

### Twenty metals from an enum

Builders are plain Java, so loops just work:

```java
public enum Metal {
    TIN(MiningTool.Tier.STONE, ToolStats.STONE), SILVER(MiningTool.Tier.IRON, ToolStats.IRON) /* , ... */;

    final MiningTool.Tier tier; final ToolStats tools;
    Metal(MiningTool.Tier tier, ToolStats tools) { this.tier = tier; this.tools = tools; }
    String id() { return name().toLowerCase(Locale.ROOT); }
}

public static final Map<Metal, MaterialSet> METALS = new EnumMap<>(Metal.class);
static {
    for (Metal m : Metal.values()) {
        METALS.put(m, REG.material(m.id()).metal().tier(m.tier)
                .tools(m.tools).armor(ArmorStats.IRON).generate(9, 10, -24, 56).build());
    }
}

Item ingot = METALS.get(Metal.TIN).ingot().get();
```

`MaterialSet` getters throw if a piece does not exist (`nugget()` on a gem); the `...Opt()` variants return an
`Optional`.

## Equipment

Materials create tools and armor with `.tools(...)` and `.armor(...)`. For stand-alone gear:

```java
EquipmentSet RUBY_GEAR = REG.equipment("ruby", RUBY)          // name prefix, repair item
        .tools(ToolStats.DIAMOND.withUses(1200))
        .armor(ArmorStats.DIAMOND)
        .bow(500).crossbow(600).fishingRod(100).shield(400)
        .horseArmor()
        .build();

// Netherite-style upgrade on the smithing table instead of crafting
REG.equipment("mythril", MYTHRIL_INGOT).tools(ToolStats.NETHERITE)
        .upgradeFrom("minecraft:diamond", Items.NETHERITE_UPGRADE_SMITHING_TEMPLATE).build();
```

`ToolStats` and `ArmorStats` ship vanilla presets (`WOOD` ... `NETHERITE`, `LEATHER` ... `NETHERITE`) with
`with...` copies. Bows, crossbows, rods and shields get the vanilla model predicates (pulling, cast, blocking)
registered on the client.

## Smithing templates

```java
RegistrySupplier<Item> MYTHRIL_UPGRADE = REG.smithingTemplate("mythril_upgrade_smithing_template")
        .texts("Diamond Equipment", "Mythril Ingot", "Mythril Upgrade",
               "Add diamond armor, weapon, or tool", "Add Mythril Ingot")
        .duplicateWith(Items.DIAMOND, Blocks.DEEPSLATE)
        .build();
```

## Spawn eggs

```java
RegistrySupplier<Item> EGG = REG.spawnEgg(MY_ENTITY, 0x00AFAF, 0x799C65);   // <entity>_spawn_egg
```
