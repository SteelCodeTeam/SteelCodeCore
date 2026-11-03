# Changelog

All notable changes to SteelCodeCore are documented here.
The format follows [Keep a Changelog](https://keepachangelog.com/en/1.1.0/) and the project uses
[Semantic Versioning](https://semver.org/): while the version is `0.x`, minor releases may break the API.

The release workflow publishes the section whose heading matches the pushed tag (`v0.2.0` → `## [0.2.0]`),
so add the section before tagging.

## [Unreleased]

## [0.1.1] - 2026-11-02

### Fixed
- Release pipeline: publication to the Codeberg Maven registry.

## [0.1.0] - 2026-11-02

First public release, for Minecraft 1.21.1 (Fabric and NeoForge).

### Added
- `CoreRegistry` with builders for blocks, block families, colors, weathering blocks, crystals, items,
  creative tabs, materials, equipment, smithing templates, wood sets, crops, plants, world generation and
  data-driven variants (wolves, paintings, trims, banner patterns, music discs).
- Status effects, potions with brewing recipes and data-driven enchantments.
- Recipes and recipe templates, loot injection, villager trades, advancements, multi-language names and a
  config file.
- Networking, persistent player and world data, key bindings, HUD elements, commands, statistics and
  advancement triggers.
- Optional JEI, REI, EMI, Jade and WTHIT integration.
- Datagen for every resource on both loaders, documentation site and Javadoc.
