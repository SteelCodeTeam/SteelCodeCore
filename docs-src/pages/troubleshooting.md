---
title: Troubleshooting
order: 11
---
# Troubleshooting

### `Cannot add content to 'mymod' after register()`

A builder ran after `REG.register()`. Usually a class with content was loaded late (first touched after init).
Make sure every content class is initialized before calling `register()`, for example by referencing it from
your `init()`.

### `CoreRegistry 'mymod' is already registered`

`register()` was called twice, often from both loader entrypoints *and* the common init. Call it once, from
common code.

### Magenta and black textures

The PNG is missing or misnamed. Run datagen and check the "textures are missing" list, or see
[Textures](textures.html).

### A name shows as `block.mymod.something`

Datagen has not been run since you added it, or the generated folder is not a resource source set. Check
`sourceSets.main.resources.srcDir 'src/main/generated'` in `common/build.gradle`.

### Spanish (or another language) names are half English

A word has no translation. Datagen logs `no translation for [...]`; add them with
`REG.lang("es_es").words(...)`.

### A recipe is missing

Two recipes with the same pattern and ingredients collide. Your own recipes win; automatic ones are dropped or
turned into stonecutter recipes. Datagen logs each collision.

### Ores or trees do not generate on NeoForge

The library ships the biome modifier that injects features; make sure you depend on the `steelcodecore-neoforge`
artifact (not only `common`) and that the feature is not disabled by `enabledIf(...)`.

### `tooltip/onUse/onHit only work without factory()`

Behaviour lambdas need the library's item class. With your own class, override `appendHoverText`, `use` or
`hurtEnemy` there instead.

### `xp() only works on full blocks` / `xp() creates a DropExperienceBlock`

XP drops use `DropExperienceBlock`. For shaped blocks or your own class, drop experience in your block class.

### `PlayerData.set must be called on the server`

Player data is authoritative on the server. On the client read it with `getClient()`; to change it, send a
packet (or use a key binding's `onPress`).

### A key binding does nothing on the server

`onPress` runs on the server only if the server also has your mod (and the library). On a vanilla server only
`onPressClient` runs.
