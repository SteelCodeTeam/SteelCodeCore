---
title: Textures
order: 10
---
# Textures

Datagen writes models; you provide the PNGs under `assets/<modid>/textures/`. After each datagen run the log
lists every texture still missing:

```
[mymod] 12 textures are missing (they will render magenta and black):
  assets/mymod/textures/block/marble.png
  ...
```

Paths below are relative to `assets/<modid>/textures/`. `<name>` is the name you passed to the builder.

## Blocks

| Feature | Textures |
|---|---|
| any block | `block/<name>` |
| `textures(side, top, bottom)` | the three you name |
| `pillar()` | `block/<name>`, `block/<name>_top` |
| `redstoneLamp()`, `toggleable()` | `block/<name>`, `block/<name>_on` |
| `connected(...)` | `block/<name>` (with border, also the item), `block/<name>_center` (borderless) |
| `randomTextures(n)` | `block/<name>`, `block/<name>_1` ... `block/<name>_<n-1>` |
| `container(rows, true)` | `block/<name>_front`, `block/<name>_side`, `block/<name>_top` |
| `cross()`, `ladder()` | `block/<name>` (also the flat item) |
| family shapes | the base texture; decorative variants use `block/<their id>` |
| family `doorTextures(top, bottom, item)` | the three you name (otherwise the base texture) |
| colors, `tinted()` | one grayscale `block/<name>` |
| colors, per color | `block/<color>_<name>`, e.g. `block/red_tiles` |
| oxidizing | `block/<name>`, `block/exposed_<name>`, `block/weathered_<name>`, `block/oxidized_<name>` |
| crystal | `block/<name>_block`, `block/budding_<name>`, `block/small_<name>_bud`, `block/medium_<name>_bud`, `block/large_<name>_bud`, `block/<name>_cluster`, `item/<name>_shard` |

## Items

| Feature | Textures |
|---|---|
| any item | `item/<name>` |
| tools and armor | `item/<prefix>_sword`, `_pickaxe`, `_axe`, `_shovel`, `_hoe`, `_helmet`, `_chestplate`, `_leggings`, `_boots` |
| worn armor | `models/armor/<prefix>_layer_1.png`, `models/armor/<prefix>_layer_2.png` |
| bow | `item/<prefix>_bow`, `item/<prefix>_bow_pulling_0/1/2` |
| crossbow | `item/<prefix>_crossbow_standby`, `_crossbow_pulling_0/1/2`, `_crossbow_arrow`, `_crossbow_firework` |
| fishing rod | `item/<prefix>_fishing_rod`, `item/<prefix>_fishing_rod_cast` |
| shield | `entity/shield/<prefix>_shield.png` (64×64, like vanilla `shield_base_nopattern`) |
| horse armor | `item/<prefix>_horse_armor`, `entity/horse/armor/horse_armor_<prefix>.png` |
| smithing template | `item/<name>` |
| spawn egg | none (vanilla tinted template) |

## World

| Feature | Textures |
|---|---|
| wood (`block/`) | `<w>_log`, `<w>_log_top`, `stripped_<w>_log`, `stripped_<w>_log_top`, `<w>_planks`, `<w>_leaves`, `<w>_sapling`, `<w>_door_top`, `<w>_door_bottom`, `<w>_trapdoor` |
| wood (`item/`) | `<w>_door`, `<w>_sign`, `<w>_hanging_sign`, `<w>_boat`, `<w>_chest_boat` |
| wood (entities) | `entity/signs/<w>.png`, `entity/signs/hanging/<w>.png`, `gui/hanging_signs/<w>.png`, `entity/boat/<w>.png`, `entity/chest_boat/<w>.png` |
| crop | `block/<name>_stage0` ... `_stage3` (or `_stage7` with `stages(8)`), `item/<name>`, `item/<name>_seeds` |
| plant | `block/<name>`; tall plants `block/<name>_bottom`, `block/<name>_top` |
| wolf variant | `entity/wolf/<name>.png`, `<name>_tame.png`, `<name>_angry.png` |
| painting | `painting/<name>.png` |
| trim material | `trims/color_palettes/<name>.png` (8×1) |
| banner pattern | `entity/banner/<name>.png`, `entity/shield/<name>.png`, `item/<name>_banner_pattern` |
| music disc | `item/music_disc_<name>`, sound `sounds/records/<name>.ogg` |

## Effects

| Feature | Textures |
|---|---|
| status effect | `mob_effect/<name>.png` (18×18) |
