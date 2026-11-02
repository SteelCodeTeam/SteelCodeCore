---
title: Core concepts
order: 2
---
# Core concepts

## The registry

`CoreRegistry` is the only object you need. Every builder hangs off it:

```java
public static final CoreRegistry REG = SteelCodeCore.registry("mymod");
```

Its lifecycle has two phases:

1. **Declare.** Builders called from static fields or a `static {}` block queue registrations and datagen work.
   Nothing touches Minecraft registries yet, so declaration order inside your class does not matter for
   references (see below).
2. **Register.** `REG.register()` registers everything through Architectury's deferred registers, hooks creative
   tabs, fuels, compostables, worldgen, loot, trades, networking and commands, and closes the registry.
   Calling a builder afterwards throws `IllegalStateException`.

Builders are single-use: configure, then call `build()` exactly once. Helpers that are not builders
(`REG.recipes()`, `REG.loot()`, `REG.worldgen()`, `REG.variants()`...) always return the same instance.

## Names and ids

Every `name` is a registry path in your namespace: `REG.block("ruby_lamp")` is `mymod:ruby_lamp`.

English names are derived automatically from **patterns** and **words**: `ruby_stairs` uses the pattern
`stairs` ("%s Stairs") and the word `ruby` ("Ruby"), giving "Ruby Stairs". Other languages translate the words
and reuse the same structure:

```java
REG.lang("es_es").words("ruby", "rubí", "maple", "arce");
// ruby_stairs -> "Escaleras de rubí", maple_door -> "Puerta de arce"
```

Use `.displayName("...")` on a builder for a fixed English name, and `REG.lang(code).set(id, text)` for a fixed
translation. Datagen logs every missing word per language. See [Translations](data.html#translations).

## Flexible references

Parameters typed `Object` that name an item or block accept whatever you have at hand:

| You pass | Meaning |
|---|---|
| `RUBY` (a `RegistrySupplier`) | your item or block |
| `Items.STICK`, `Blocks.STONE` | any `ItemLike` |
| `"minecraft:stick"`, `"othermod:gear"` | an id; **without a namespace it means `minecraft:`** |
| `"#c:ingots/tin"`, a `TagKey` | a tag (where a tag makes sense: recipes, repair items) |

They are resolved lazily, during datagen or at registration, so you can reference something declared later in
the same class. Tag and texture names given as `String` parameters (`.tag("gems")`, `.texture("block/x")`)
default to **your** namespace instead.

## Defaults you get for free

- **Creative tab.** The first `REG.tab(...)` becomes the default tab of everything else; override per builder
  with `.tab(...)` or `.noTab()`, or globally with `REG.defaultTab(...)`.
- **Textures.** Blocks use `block/<name>`, items use `item/<name>`. Multi-texture features document their names;
  see [Textures](textures.html).
- **Drops.** Blocks drop themselves. Slabs drop two when double, doors drop one, containers drop their contents.
- **Recipe unlocks.** Every recipe gets a recipe-book advancement unlocked by its first ingredient.
- **Tags.** Shapes get their vanilla tags (`minecraft:stairs`, `wooden_doors`...), materials get the common
  `c:` tags, tools get `minecraft:swords`, `pickaxes`...

## What datagen writes

For each registry, the data provider writes (for both loaders at once):

- blockstates, block models and item models
- loot tables, recipes and recipe-unlock advancements
- block, item, enchantment and other registry tags
- configured and placed features, enchantments, advancements, data-driven variants
- `assets/<modid>/lang/<code>.json` for `en_us` and every language you declared
- `sounds.json`, atlas sources, NeoForge data maps (compostables, oxidizables, waxables)

Runtime-only parts (biome injection, loot injection, trades, brewing, fuel, flammability) are registered in code.

## Packages and API stability

| Package | Contents | Use it? |
|---|---|---|
| `dev.steelcode.core` | `SteelCodeCore`, the entry point | yes |
| `dev.steelcode.core.api` | `CoreRegistry` | yes |
| `dev.steelcode.core.api.block` · `item` · `world` · `effect` · `data` · `gameplay` | builders and their result types, grouped by topic | yes |
| `dev.steelcode.core.content.*` | block, item, effect and entity classes the builders create; open for extension | yes |
| `dev.steelcode.core.impl.*` | registry internals, datagen, client, compat, platform bridge | **no** |
| `dev.steelcode.core.fabric` / `neoforge` | entrypoints and the datagen helpers | only the `*Datagen` classes |

Everything marked `@ApiStatus.Internal` (all of `impl`, plus builder constructors that take a
`RegistryContext`) is public only because Java has no friend packages. IntelliJ warns when you use it; it may
change in any release.
