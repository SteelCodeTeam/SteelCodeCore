---
title: Data & resources
nav: Data
order: 7
---
# Data and resources

## Recipes

Recipes are written by datagen with your namespace and a recipe-book unlock. Items are
[flexible references](concepts.html#flexible-references).

```java
Recipes r = REG.recipes();
r.shaped(RUBY_SWORD).pattern(" R ", " R ", " S ").define('R', RUBY).define('S', Items.STICK).save();
r.shapeless(RUBY, 9).requires(RUBY_BLOCK).save("ruby_from_block");
r.oreSmelting(RAW_TIN, TIN_INGOT, 0.7f);        // furnace + blast furnace
r.foodCooking(RAW_EEL, COOKED_EEL, 0.35f);      // furnace + smoker + campfire
r.stonecutting(Blocks.STONE, RUBY_BRICKS, 1);
r.smithing(TEMPLATE, Items.DIAMOND_SWORD, MYTHRIL, MYTHRIL_SWORD);
```

### Templates

The vanilla shapes are one-liners — `stairs`, `slab`, `verticalSlab`, `wall`, `fence`, `fenceGate`, `button`,
`pressurePlate`, `door`, `trapdoor`, `woodenTrapdoor`, `pane`, `carpet`, `polished`, `pillar`, `chiseled`,
`cracked`, `surround`, `storage`, `storage2x2`, `nuggets`, the five tools and four armor pieces:

```java
r.stairs(MARBLE_STAIRS, MARBLE);
r.storage(TIN_BLOCK, TIN_INGOT);          // 9 <-> 1
```

Every template also exists as a `RecipeTemplate` to attach while building:

```java
REG.block("marble_stairs").shape(Variant.STAIRS, MARBLE)
        .recipe(RecipeTemplate.stairs(MARBLE))
        .recipe(RecipeTemplate.stonecutting(MARBLE, 1))
        .build();
REG.item("ruby_sword").handheld().recipe(RecipeTemplate.sword(RUBY)).build();
REG.item("odd").recipe(RecipeTemplate.of((recipes, self) -> recipes.shapeless(self).requires(Items.DIRT).save())).build();
```

Your own recipes always win over automatic ones (families, materials). Identical automatic recipes are dropped
or turned into stonecutter recipes, and datagen logs what happened.

## Loot injection

Adds pools to existing loot tables at load time, without replacing them:

```java
REG.loot().add(LootInjector.DUNGEON, RUBY, 0.25f, 1, 3);                 // 25% chance of 1-3 rubies
REG.loot().add(LootInjector.ALL_VILLAGE_CHESTS, TIN_INGOT, 0.4f, 2, 5);
REG.loot().add("minecraft:entities/zombie", TIN_NUGGET, 0.05f, 1, 1);
REG.loot().add("minecraft:chests/village/*", ROPE, 0.3f, 1, 4);          // * = prefix match
```

## Trades

```java
REG.trades().villager(VillagerProfession.TOOLSMITH, 3)
        .sells(RUBY_PICKAXE, 1, 18)        // 18 emeralds -> 1 pickaxe
        .buys(RUBY, 4, 1);                 // 4 rubies -> 1 emerald
REG.trades().wanderer(false).maxUses(4).sells(MAPLE_SAPLING, 1, 5);
```

## Advancements

```java
ResourceLocation ROOT = REG.advancements().root("root", RUBY).title("My Mod").description("Find a ruby")
        .background("minecraft:textures/block/calcite.png").obtain(RUBY).build();

REG.advancements().child("all_metals", ROOT, TIN_INGOT).title("Metallurgist").description("Get every metal")
        .obtainAll(TIN_INGOT, SILVER_INGOT).frame(Advancements.Frame.GOAL).xp(50).build();

REG.advancements().child("caster", ROOT, WAND).title("Caster").trigger(USED_POWER).build();   // custom trigger
```

Criteria: `obtain(...)`, `obtainAll(...)`, `placed(block)`, `kill(entity)`, `trigger(...)` and any vanilla
`criterion(id, trigger, conditionsJson)`. An advancement without criteria is granted immediately.

## Translations

English is generated from ids. For other languages translate **words** once and let patterns do the rest:

```java
REG.lang("es_es")
        .words("tin", "estaño", "ruby", "rubí", "maple", "arce")   // every tin_*, *_ruby, maple_* name
        .set("ruby_sword", "Espada carmesí")                       // a one-off override
        .set("itemGroup.mymod.main", "Mi mod");                    // any key

REG.lang("fr_fr").pattern("stairs", "Escalier en %s").words("gold", "or");
```

`es_es` ships with the patterns and many common words (vanilla materials, woods, colors and usual mod metals).
Datagen prints, per language, the words and texts still missing. Free texts — tooltips, `info(...)` pages,
advancement titles — are listed with their keys so you can `set(key, text)` them.

## Config

```java
public static final CoreConfig CFG = REG.config();
public static final CoreConfig.Value<Boolean> TIN_ORE  = CFG.bool("tin_ore", true, "Generate tin ore");
public static final CoreConfig.Value<Integer> MAX_HEAT = CFG.integer("max_heat", 100, 1, 1000, "Maximum heat");
public static final CoreConfig.Value<Double>  SPEED    = CFG.decimal("speed", 1.5, 0, 10, "Speed multiplier");
public static final CoreConfig.Value<String>  GREETING = CFG.text("greeting", "Hello", "Join message");

if (MAX_HEAT.get() > 50) { /* ... */ }
REG.worldgen().ore("ore_tin")./* ... */enabledIf(TIN_ORE::get).build();
```

The file is `config/<modid>.toml`, written with defaults and comments on first read. Invalid or out-of-range
values fall back to the default with a warning. Declare all values before reading any; values are read once per
game start (no live reload, no client/server sync).
