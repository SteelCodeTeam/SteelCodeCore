---
title: Recipe viewers & tooltips
nav: Compat
order: 9
---
# Recipe viewers and tooltips

SteelCodeCore ships plugins for **JEI**, **REI**, **EMI**, **Jade** and **WTHIT**. They are compiled against
those mods' APIs but never required: each plugin only loads if its mod is installed, on either loader.

```java
REG.block("framed_glass").copy(Blocks.GLASS).connected(true)
        .info("Connects to neighbouring framed glass")                 // info page in JEI / REI / EMI
        .tooltipLine(state -> Component.literal("Connected"))          // extra line in Jade / WTHIT
        .build();

REG.block("debug_block").hideInViewers().build();                      // hidden in JEI / REI / EMI
REG.item("secret_key").info("Opens the vault").hideInViewers().build();
```

| Feature | JEI | REI | EMI | Jade | WTHIT |
|---|:-:|:-:|:-:|:-:|:-:|
| `info(...)` pages | ✓ | ✓ | ✓ | | |
| `hideInViewers()` | ✓ | ✓ | ✓ | | |
| `tooltipLine(...)` | | | | ✓ | ✓ |

`info` texts are English; translate them with `REG.lang(code).set("info.<modid>.<name>.<n>", text)`. Call
`info` several times for several paragraphs.

How each plugin is discovered, for reference:

| Mod | Fabric | NeoForge |
|---|---|---|
| JEI | `jei_mod_plugin` entrypoint | `@JeiPlugin` |
| REI | `rei_client` entrypoint | `@REIPluginClient` |
| EMI | `emi` entrypoint | `@EmiEntrypoint` |
| Jade | `jade` entrypoint | `@WailaPlugin` |
| WTHIT | `waila_plugins.json` | `waila_plugins.json` |
