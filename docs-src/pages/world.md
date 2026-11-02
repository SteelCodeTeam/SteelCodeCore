---
title: World
order: 5
---
# World

## Wood sets

```java
WoodSet MAPLE = REG.wood("maple")
        .tree(TreeShape.FANCY_OAK)
        .generate("minecraft:is_forest", 2)     // 2 attempts per chunk in forests
        .build();
```

One call creates logs, wood, stripped variants, planks with their family (stairs, slab, fence, fence gate, button,
pressure plate, door, trapdoor), leaves, sapling, potted sapling, the tree feature, signs, hanging signs, boats
and chest boats, with loot, tags, recipes, axe stripping, flammability, composting and fuel.

| Option | Effect |
|---|---|
| `tree(TreeShape.X)` | `OAK`, `FANCY_OAK`, `BIRCH`, `SPRUCE`, `ACACIA`, `CHERRY`, `JUNGLE` (copied from vanilla) |
| `tintedLeaves()` | biome-colored leaves (grayscale texture) |
| `leavesLight(n)` | glowing leaves |
| `planks(f -> f.createAll())` | more planks shapes |
| `generate(tag, perChunk)`, `generateRare(tag, chunks)`, `generateIf(condition)` | world generation |
| `noSigns()`, `noBoats()` | skip parts |

Signs and boats are wired on both loaders: the wood type is registered, sign blocks are added to the vanilla sign
block entities and boats have their own entity types and renderers.

## Crops

```java
CropBuilder.CropSet TOMATO = REG.crop("tomato").food(3, 0.6f).build();             // tomato_seeds plant tomato_crop
CropBuilder.CropSet ONION  = REG.crop("onion").plantedByProduce().stages(8).build(); // planted like carrots
```

Mature crops drop produce plus seeds (Fortune applies), young crops drop seeds. Seeds and produce are
compostable, and whatever is planted is tagged `minecraft:villager_plantable_seeds` so villagers farm it. Use `produce(p -> p.foodEffect(...))` to customize
the produce item.

## Plants

```java
REG.plant("bluebell").flower(MobEffects.NIGHT_VISION, 5).dye(DyeColor.BLUE)
        .generate("minecraft:is_forest", 16).build();                  // with flower pot
REG.plant("giant_bluebell").flower(MobEffects.NIGHT_VISION, 5).tall().build();   // two blocks tall
REG.plant("dry_shrub").bush().generate("minecraft:is_badlands", 4).build();     // shears only, replaceable
```

## World generation

All features are written as JSON and injected into biomes on both loaders (NeoForge through a biome modifier
shipped with the library).

```java
// An ore like iron: veins of 9, 10 per chunk, most common around y=16
REG.worldgen().ore("ore_tin")
        .replace(Worldgen.OreTarget.STONE, TIN_ORE).replace(Worldgen.OreTarget.DEEPSLATE, DEEPSLATE_TIN_ORE)
        .size(9).count(10).trapezoid(-24, 56)
        .build();

// A blob like granite
REG.worldgen().patch("marble_patch", MARBLE).size(64).rarity(6).uniform(0, 60).build();

// Surface boulders, underwater disks, geodes
REG.worldgen().rock("mossy_boulder", Blocks.MOSSY_COBBLESTONE).build();
REG.worldgen().disk("jade_disk", JADE).radius(2, 4).build();
REG.worldgen().geode("jade_geode").inner(JADE).budding(BUDDING_JADE)
        .buds(SMALL_BUD, MEDIUM_BUD, LARGE_BUD, CLUSTER).rarity(24).build();
```

| Ore option | Meaning |
|---|---|
| `replace(OreTarget, block)` / `replaceTag(tag, block)` | what is replaced (`STONE`, `DEEPSLATE`, `ANY_OVERWORLD_STONE`, `NETHER`, `END`) |
| `size(n)`, `count(n)`, `rarity(chunks)` | vein size and frequency |
| `uniform(minY, maxY)`, `trapezoid(minY, maxY)` | height distribution |
| `airExposure(chance)` | discard blocks touching air (diamond: 0.5) |
| `biomes("#tag")` | where (default: overworld) |
| `enabledIf(CONFIG::get)` | switch off from your config |

## Data-driven variants

```java
REG.variants().wolf("ember", "#minecraft:is_nether");
REG.variants().painting("sunset", 2, 1, "Sunset", "RuDaHee");
REG.variants().trimMaterial("ruby", RUBY.main(), 0xD01030);
RegistrySupplier<Item> ANVIL = REG.variants().bannerPattern("anvil", "Anvil");
RegistrySupplier<Item> DISC  = REG.variants().musicDisc("forge", 142, 8, "RuDaHee - Forge");
```

| Variant | You provide |
|---|---|
| wolf | `textures/entity/wolf/<name>.png`, `<name>_tame.png`, `<name>_angry.png` |
| painting | `textures/painting/<name>.png` (16 px per block) |
| trim material | `textures/trims/color_palettes/<name>.png` (8×1 palette) |
| banner pattern | `textures/entity/banner/<name>.png`, `textures/entity/shield/<name>.png`, item texture |
| music disc | `sounds/records/<name>.ogg`, item texture `item/music_disc_<name>` |

<div class="note">
Minecraft 1.21.1 does not show painting titles and authors yet (they are written to the lang files for later
versions). Custom trim materials show on worn armor, but vanilla armor item icons only know vanilla materials.
</div>
