---
title: Blocks
order: 3
---
# Blocks

## Single blocks

`REG.block(name)` builds one block and its item. With no options you get a full cube that drops itself:

```java
RegistrySupplier<Block> MARBLE = REG.block("marble").copy(Blocks.CALCITE).build();
RegistrySupplier<Block> STONE_LIKE = REG.simpleBlock("basalt_tiles", Blocks.POLISHED_BASALT);   // shortcut
```

### Properties

| Method | Effect |
|---|---|
| `copy(Blocks.X)` | copy hardness, sound, map color, required tool... |
| `properties(() -> ...)` | explicit `BlockBehaviour.Properties` |
| `modify(p -> p.strength(0.5f))` | tweak after copying (chains) |
| `strength(hardness, resistance)` | shortcut |
| `light(0-15)` | emitted light |
| `factory(MyBlock::new)` | your own block class |

### Mining, drops and XP

```java
RegistrySupplier<Block> RUBY_ORE = REG.block("ruby_ore")
        .copy(Blocks.DIAMOND_ORE)
        .tool(MiningTool.PICKAXE, MiningTool.Tier.IRON)  // mineable/pickaxe + needs_iron_tool
        .xp(3, 7)                                         // like ores; nothing with Silk Touch
        .drops(Drops.ore(RUBY))                           // Silk Touch -> the ore, else rubies with Fortune
        .build();
```

`Drops` covers the usual patterns and is immutable:

```java
Drops.self()                 // default
Drops.none()
Drops.silkTouchOnly()        // like glass
Drops.ore(RAW_TIN).count(1, 3)
Drops.item(Items.COBBLESTONE).withSilkTouch(Drops.SELF)
Drops.shearsOnly()           // like grass
Drops.likeVanilla("oak_leaves", "minecraft:oak_leaves", Drops.SELF, "minecraft:oak_sapling", MAPLE_SAPLING)
```

`likeVanilla` copies a vanilla loot table from the Minecraft jar during datagen and replaces ids, which is the
easiest way to get complex tables (leaves, pots) exactly right.

### Item, fuel, tags

```java
REG.block("charcoal_block").copy(Blocks.COAL_BLOCK)
        .fuel(16000)                      // item burns 16000 ticks
        .flammable(5, 5)                  // catches and spreads fire
        .compostable(0.3f)
        .tag("minecraft:infiniburn_overworld")
        .itemTag("c:storage_blocks/charcoal")
        .displayName("Block of Charcoal")
        .build();
```

Use `noItem()` for technical blocks, `noTab()` / `tab(...)` for creative tabs and `itemProperties(...)` for
rarity or stack size.

### Functional blocks

No block class needed for these:

```java
REG.block("crate").copy(Blocks.BARREL).container(3, true).tool(MiningTool.AXE).build(); // 27 slots, faces player
REG.block("ruby_lamp").copy(Blocks.REDSTONE_LAMP).redstoneLamp().build();               // textures: ruby_lamp(_on)
REG.block("switch_lamp").copy(Blocks.GLOWSTONE).toggleable().build();                   // right click toggles
REG.block("ash").copy(Blocks.SAND).falling(0x555555).build();
REG.block("rope_ladder").copy(Blocks.LADDER).ladder().build();
REG.block("speed_pad").copy(Blocks.STONE).stepEffect(MobEffects.MOVEMENT_SPEED, 3, 1).build();
REG.block("hot_stone").copy(Blocks.STONE).stepDamage(1).build();
REG.block("bouncer").copy(Blocks.SLIME_BLOCK)
        .onStep((level, pos, state, entity) -> entity.setDeltaMovement(0, 1, 0)).build();
```

Containers open the generic chest menu, work with hoppers and comparators, keep custom names and drop their
contents. Orientable containers use `<name>_front`, `<name>_side` and `<name>_top`.

### Textures and models

| Method | Result |
|---|---|
| `texture("block/x")` / `textures(side, top, bottom)` | override textures |
| `pillar()` | log-like pillar: `<name>` and `<name>_top` |
| `cross()` | cross plant model with cutout layer |
| `connected(seeThrough)` | connected textures: `<name>` (bordered) and `<name>_center` |
| `randomRotation()` | random Y rotation per position, like dirt |
| `randomTextures(n)` | random pick of `<name>`, `<name>_1` ... `<name>_<n-1>` |
| `renderLayer(RenderLayer.CUTOUT)` | render layer (glass, leaves and plants pick one automatically) |
| `flatItem("mymod:item/rope")` / `itemModel("mymod:block/x")` | item model |
| `customModel()` | write blockstate and model yourself |

Connected textures are pure blockstate multipart: each missing neighbour adds border strips on that edge. They
need no client mod and work with Sodium.

## Shapes

`shape(variant, base)` gives a block the shape of a family variant using the base block's texture:

```java
REG.block("bouncy_gold_stairs")
        .shape(Variant.STAIRS, Blocks.GOLD_BLOCK)
        .factory(p -> new BouncyStairs(Blocks.GOLD_BLOCK.defaultBlockState(), p))   // optional
        .defaultRecipe()                         // 6 gold blocks -> 4 stairs
        .build();
```

Useful with shapes: `wooden()`, `blockSetType(BlockSetType.IRON)`, `woodType(...)`, `weightedPressurePlate(15)`,
`defaultRecipe(ingredient)` and `stick(item)`.

## Families

A family is a base block plus any of its variants, sharing its texture:

```java
// Every shape of a vanilla block: no new textures at all
BlockFamily GOLD = REG.family().base(Blocks.GOLD_BLOCK).preset(FamilyPreset.METAL).createAll().build();

// A new base block plus some shapes
BlockFamily RUBY_BRICKS = REG.family().newBase("ruby_bricks", Blocks.STONE_BRICKS)
        .preset(FamilyPreset.STONE).stairs().slab().wall().light(4).build();

Block stairs = GOLD.stairs().get();
GOLD.get(Variant.DOOR).ifPresent(door -> { /* ... */ });
```

| Selection | Variants |
|---|---|
| `createAll()` | the 12 shapes: stairs, slab, vertical slab, wall, fence, fence gate, button, pressure plate, door, trapdoor, pane, carpet |
| `createDecorative()` | polished, chiseled, cracked, cut, smooth, bricks, tiles, pillar (each needs its own texture) |
| `createEverything()` | both |
| `stairs()`, `slab()`... / `with(...)` / `without(...)` | pick individually |

Presets set sensible behaviour: `STONE` (pickaxe, stone buttons, stonecutter recipes), `WOOD` (axe, `wooden_*`
tags, fuel, oak sounds, flammable) and `METAL` (pickaxe, iron doors, weighted plate). Everything can be adjusted:
`tool(...)`, `blockSetType(...)`, `buttonTicks(...)`, `fuel(...)`, `properties(Variant.DOOR, p -> ...)`,
`texture(Variant.TRAPDOOR, "block/x")`, `doorTextures(top, bottom, item)`, `name(prefix)`...

Ids are `<prefix>_<variant>`, the prefix being derived from the base: `gold_block → gold`,
`stone_bricks → stone_brick`, `oak_planks → oak`.

Recipes: crafting recipes for every variant plus stonecutter recipes for stone and metal. If two variants would
produce the same crafting pattern (they collide), the first wins and the other becomes a stonecutter recipe.
Use `noRecipes()` to skip them, or `ingredient(TIN_INGOT)` to craft from another item.

## Colors

Sixteen dye colors in one call:

```java
// One grayscale texture tinted in code for each color
ColorSet MARBLE = REG.colors("marble").copy(Blocks.CALCITE).tinted().dyeFrom(MARBLE_BLOCK).build();

// One texture per color (block/red_tiles.png ...) plus stairs and slabs for each
ColorSet TILES = REG.colors("tiles").copy(Blocks.TERRACOTTA).family(f -> f.stairs().slab()).build();

Block red = MARBLE.get(DyeColor.RED).get();
```

`dyeFrom(base)` adds the 8-around-a-dye recipe; `only(DyeColor.RED, DyeColor.BLUE)` limits the colors. Every
block also goes into the tag `<modid>:<name>`.

## Weathering (copper-like)

```java
OxidizingBuilder.OxidizingSet BRONZE = REG.oxidizing("bronze_plating").copy(Blocks.COPPER_BLOCK)
        .stairsAndSlab().build();
```

Creates the four stages (`bronze_plating`, `exposed_`, `weathered_`, `oxidized_`) and their `waxed_` versions,
plus stairs and slabs of each. Blocks weather over time, honeycomb waxes them, axes scrape them and lightning
cleans them. Textures: one per stage; waxed blocks reuse their stage's texture.

## Crystals (amethyst-like)

```java
CrystalBuilder.CrystalSet QUARTZ = REG.crystal("rose_quartz").build();
REG.worldgen().geode("rose_quartz_geode").inner(QUARTZ.block()).budding(QUARTZ.budding())
        .buds(QUARTZ.small(), QUARTZ.medium(), QUARTZ.large(), QUARTZ.cluster()).build();
```

Creates the block, the budding block (grows buds on random ticks and drops nothing, even with Silk Touch), three bud sizes, the
cluster (drops shards) and the shard item. Pass `shard(MY_GEM)` to reuse an existing item.
