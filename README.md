# SteelCodeCore

A multiloader library for **Minecraft 1.21.1** (Fabric + NeoForge, Architectury) to declare blocks, items and
gameplay features once in `common`, register them on both loaders and generate every JSON with datagen.

```java
public static final CoreRegistry REG = SteelCodeCore.registry("mymod");

public static final BlockFamily GOLD = REG.family().base(Blocks.GOLD_BLOCK).createAll().build();
public static final MaterialSet RUBY = REG.material("ruby").gem()
        .tools(ToolStats.DIAMOND).armor(ArmorStats.DIAMOND).generate(4, 3, -64, 16).build();

public static void init() {
    REG.register();
}
```

Blocks and families, colors, weathering, crystals, connected textures · items, materials, tools and armor ·
wood sets, crops, plants, ores and geodes · effects, potions, enchantments · recipes, loot, trades,
advancements, translations, config · networking, player/world data, key bindings, HUD, commands, statistics ·
JEI, REI, EMI, Jade and WTHIT support.

## Using it

```groovy
repositories {
    maven { url = 'https://codeberg.org/api/packages/steelcode/maven'; content { includeGroup 'dev.steelcode' } }
}
dependencies {
    modImplementation "dev.steelcode:steelcodecore-fabric:0.1.0"     // or -neoforge / -common
}
```

The Codeberg registry keeps the last 10 versions, with sources and Javadoc. Every version's jars are attached to
the GitHub releases.

## Documentation

- **Guides and API reference:** https://steelcode-team.github.io/steelcodecore/
- Spanish quick reference: [README.es.md](README.es.md)

## Building

```bash
./gradlew build                 # library jars for Fabric and NeoForge
./gradlew publishToMavenLocal   # test unreleased versions from other projects
./gradlew buildDocs             # docs site into docs/ (python3 + docs-src/requirements.txt)
```

Guide pages are Markdown in `docs-src/pages/`; `docs/` is generated and not committed.

## Automation

| Workflow | When | What |
|---|---|---|
| `ci.yml` | push / pull request | builds both loaders and the Javadoc |
| `docs.yml` | push to `main` touching docs or API | builds and deploys GitHub Pages |
| `release.yml` | tag `vX.Y.Z` | publishes to Codeberg Maven and creates the GitHub release |

To release: add a `## [X.Y.Z]` section to [CHANGELOG.md](CHANGELOG.md), then
`git tag vX.Y.Z && git push origin vX.Y.Z`. See [CONTRIBUTING.md](CONTRIBUTING.md).

### One-time setup

1. **Codeberg:** create the organization `steelcode` (public) and an access token with scope
   `write:package`. In the organization's *Settings → Packages → Cleanup rules*, add a rule for type
   **Maven** that keeps the **10** most recent versions.
2. **GitHub:** add the token as the repository secret `CODEBERG_TOKEN`
   (*Settings → Secrets and variables → Actions*).
3. **GitHub Pages:** *Settings → Pages → Build and deployment → Source: GitHub Actions*.
4. If the GitHub repository or the Codeberg owner have other names, update `project_url`, `docs_url` and
   `codeberg_owner` in `gradle.properties` and the URLs in the docs.

## Project layout

| Path | Contents |
|---|---|
| `common/src/main/java/dev/steelcode/core/api` | public API: `CoreRegistry` and builders by topic |
| `common/src/main/java/dev/steelcode/core/content` | reusable block, item, effect and entity classes |
| `common/src/main/java/dev/steelcode/core/impl` | internals (`@ApiStatus.Internal`) |
| `fabric/`, `neoforge/` | loader entrypoints, platform hooks and datagen helpers |
| `docs-src/` | documentation sources |

## License

SteelCodeCore is free software: you can redistribute it and/or modify it under the terms of the
[GNU Lesser General Public License](COPYING.LESSER) as published by the Free Software Foundation, either
version 3 of the License, or (at your option) any later version.

In practice: mods under any license may depend on SteelCodeCore; modified versions of the library itself must
remain under the LGPL.
