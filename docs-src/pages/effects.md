---
title: Effects & enchantments
nav: Effects
order: 6
---
# Effects, potions and enchantments

## Status effects

```java
RegistrySupplier<MobEffect> BLEEDING = REG.effect("bleeding").harmful().color(0x8A0303)
        .onTick((entity, amp) -> { entity.hurt(entity.damageSources().magic(), 1 + amp); return true; }, 20)
        .build();

REG.effect("lightness").beneficial().color(0xBFEFFF)
        .attribute(Attributes.MOVEMENT_SPEED, 0.2, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL)
        .build();
```

The tick lambda runs every `interval` ticks; return `false` to remove the effect. The icon is
`textures/mob_effect/<name>.png` (18×18). The returned supplier is also a `Holder<MobEffect>`, so it can be passed
straight to potions, food effects and `addEffect`.

## Potions

```java
REG.potion("bleeding").effect(BLEEDING, 45, 0)
        .brewedFrom(Potions.AWKWARD, Items.REDSTONE_BLOCK)
        .longVersion(90)          // + redstone   -> long_bleeding
        .strongVersion(22, 1)     // + glowstone  -> strong_bleeding
        .build();
```

Names are generated for the potion, splash and lingering potions and tipped arrows.

## Enchantments

Enchantments are data in 1.21, so the builder writes the enchantment JSON and its tags:

```java
ResourceKey<Enchantment> VENOM = REG.enchantment("venom")
        .supported("#minecraft:enchantable/sword").maxLevel(3).weight(5)
        .damageAgainst("#minecraft:sensitive_to_bane_of_arthropods", 2.0, 2.0)
        .build();

REG.enchantment("swiftness").supported("#minecraft:enchantable/foot_armor").slots("feet").maxLevel(2)
        .attribute("minecraft:generic.movement_speed", 0.1, 0.1, "add_multiplied_base")
        .build();
```

| Method | Meaning |
|---|---|
| `supported(...)`, `primary(...)` | items it applies to / appears on in the table |
| `weight(n)`, `maxLevel(n)`, `cost(...)`, `anvilCost(n)` | rarity and costs |
| `slots(...)` | `mainhand`, `offhand`, `hand`, `head`, `chest`, `legs`, `feet`, `armor`, `any` |
| `exclusiveWith("#tag")`, `treasure()`, `curse()` | availability |
| `damage(base, perLevel)`, `damageAgainst(tag, base, perLevel)`, `attribute(...)` | common effects |
| `effect(component, json)` | any vanilla effect component as raw JSON |

Non-treasure enchantments are added to the enchanting-table, trade and loot tags; treasure and curse
enchantments only to the trade and loot tags.
