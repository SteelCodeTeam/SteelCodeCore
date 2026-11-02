# SteelCodeCore (referencia rápida en español)

> La documentación principal, en inglés, está en `docs/` (GitHub Pages) y en el Javadoc. Esta página es un resumen
> en español con los ejemplos más usados. Paquetes: `dev.steelcode.core.api` (+ `.block`, `.item`, `.world`,
> `.effect`, `.data`, `.gameplay`) y `dev.steelcode.core.content`.

Librería común de SteelCode Team para **Minecraft 1.21.1**, multiloader (Fabric + NeoForge, Architectury).
Todo se declara una vez en `common`, se registra en los dos loaders y se genera por datagen:
blockstates, modelos, loot tables, tags, recetas (con desbloqueo en el libro) y `en_us.json`.

## Empezar

```java
public final class MyContent {
    public static final CoreRegistry REG = SteelCodeCore.registry(MyMod.MOD_ID);

    // Pestaña creativa propia: la primera que creas es la de por defecto para todo lo demás
    public static final RegistrySupplier<CreativeModeTab> TAB = REG.tab("main")
            .icon(() -> MyContent.RUBY.main().get())
            .title("Mi Mod")
            .build();

    // Todas las formas posibles del oro vanilla, sin añadir ni un png
    public static final BlockFamily GOLD = REG.family().base(Blocks.GOLD_BLOCK).createAll().build();

    public static void init() {
        REG.register();   // desde el init común
    }
}
```

## Familias

```java
REG.family()
   .base(Blocks.GOLD_BLOCK)              // o .base(RegistrySupplier) o .newBase("ruby_bricks", Blocks.STONE_BRICKS)
   .preset(FamilyPreset.METAL)           // STONE / WOOD / METAL
   .createAll()                          // todas las formas
   .without(Variant.CARPET)              // quitar alguna
   .light(10)                            // todas emiten luz
   .build();
```

| Grupo | Variantes | Textura |
|---|---|---|
| `createAll()` | stairs, slab, vertical_slab, wall, fence, fence_gate, button, pressure_plate, door, trapdoor, pane, carpet | la del base (no hace falta png) |
| `createDecorative()` | polished_, chiseled_, cracked_, cut_, smooth_, _bricks, _tiles, _pillar | propia: `block/<id>` (pillar: `<id>` y `<id>_top`) |
| `createEverything()` | las dos anteriores | |

Puertas, trampillas y paneles usan la textura del base salvo que pongas `doorTextures(top, bottom, item)` o
`texture(Variant.TRAPDOOR, "block/...")`. También se registra sola su capa de render (cutout) en el cliente.

**Presets**

| Preset | Qué ajusta |
|---|---|
| `STONE` | pico, cortapiedras, botones/placas de piedra |
| `WOOD` | hacha, tags `wooden_*` y `planks`, combustible con los valores vanilla, sonidos de roble |
| `METAL` | pico, puertas/trampillas de hierro (solo con redstone), placa con peso, cortapiedras |

**Otras opciones:** `name()`, `texture()`, `textures(side, top, bottom)`, `properties(...)`,
`properties(Variant, ...)`, `fuel(ticks)`, `renderLayer(...)`, `tab(...)`/`noTab()`, `tool(...)`, `tier(...)`,
`tag(...)`, `ingredient(...)`, `stick(...)`, `stonecutting(bool)`, `noRecipes()`, `woodType`, `blockSetType`,
`weightedPressurePlate(peso)`, `displayName(Variant, "...")`.

**Recetas automáticas:** si dos variantes acaban con la misma receta (p. ej. muro de madera y trampilla, los dos 3x2),
gana la primera y la otra pasa a ser de cortapiedras. El log del datagen te dice cuáles.

## Bloques sueltos

```java
// Mena: XP, rubíes con fortuna, y con silk touch se suelta a sí misma
REG.block("ruby_ore").copy(Blocks.DIAMOND_ORE).xp(3, 7).drops(Drops.ore(RUBY)).tool(MiningTool.PICKAXE, Tier.IRON).build();

// Cristal: solo se recoge con silk touch, translúcido
REG.block("amber_glass").copy(Blocks.GLASS).drops(Drops.silkTouchOnly()).renderLayer(RenderLayer.TRANSLUCENT).build();

// Sin silk touch suelta 2-4 de polvo de hueso; con silk touch, el bloque
REG.block("fossil_stone").copy(Blocks.STONE).xp(1, 3)
   .drops(Drops.item(Items.BONE_MEAL).count(2, 4).withSilkTouch(Drops.SELF)).build();

// Tronco (pilar), combustible y luz
REG.block("glow_log").copy(Blocks.OAK_LOG).pillar().fuel(300).light(8).build();
```

### Bloques con forma y clase propia

```java
// Escalera custom: modelos con la textura del oro, tags de escaleras y receta vanilla de escalera
REG.block("bouncy_gold_stairs")
   .shape(Variant.STAIRS, Blocks.GOLD_BLOCK)
   .factory(p -> new BouncyStairs(Blocks.GOLD_BLOCK.defaultBlockState(), p))
   .defaultRecipe()                       // o .defaultRecipe(Items.GOLD_INGOT)
   .build();

// En una familia, igual pero por variante
REG.family().base(Blocks.GOLD_BLOCK).createAll()
   .factory(Variant.STAIRS, (base, p) -> new BouncyStairs(base.defaultBlockState(), p))
   .build();
```

`shape(...)` vale para cualquier variante: escalera, losa, losa vertical, muro, valla, puerta, trampilla, panel... También
acepta `.wooden()`, `.blockSetType(...)`, `.woodType(...)` y `.weightedPressurePlate(peso)`.

`Drops`: `self()`, `none()`, `silkTouchOnly()`, `item(x)`, `ore(x)`, más `.withSilkTouch(...)`,
`.withoutSilkTouch(...)`, `.count(min, max)` y `.fortune(bool)`.

## Items

```java
REG.item("ruby").tag("c:gems/ruby").build();
REG.item("coal_bit").fuel(400).build();                    // 2 items en el horno
REG.item("berry").food(2, 0.3f).build();
REG.item("ruby_sword").factory(p -> new SwordItem(RubyTier.INSTANCE, p)).handheld().build();
```

También: `stacksTo`, `durability`, `rarity`, `fireResistant`, `properties(...)`, `texture`, `tab`/`noTab`, `noModel`.

## Materiales: 20 metales con un `for`

```java
public enum Metal {
    TIN(Tier.STONE), ZINC(Tier.STONE), SILVER(Tier.IRON), PLATINUM(Tier.DIAMOND) /* ... */;
    final MiningTool.Tier tier;
    Metal(MiningTool.Tier tier) { this.tier = tier; }
    public String id() { return name().toLowerCase(Locale.ROOT); }
}

public static final Map<Metal, MaterialSet> METALS = new EnumMap<>(Metal.class);
static {
    for (Metal m : Metal.values()) {
        METALS.put(m, REG.material(m.id()).metal().tier(m.tier).family(f -> f.createAll()).build());
    }
}

Item tinIngot = METALS.get(Metal.TIN).ingot().get();
```

Cada metal crea `x_ingot`, `x_nugget`, `raw_x`, `raw_x_block`, `x_block`, `x_ore`, `deepslate_x_ore`. Además:

- las recetas: 9↔1 de pepitas, lingotes y bruto, y horno y alto horno desde el bruto y las menas;
- el loot de las menas, igual que el del hierro (con silk touch, fortuna y XP configurable);
- las tags `c:` (`c:ingots/x`, `c:ores/x`, `c:storage_blocks/x`, `c:raw_materials/x`...) y las del faro;
- con `.family(...)`, una familia de bloques a partir del bloque de almacenamiento.

Variantes: `.gem()` (gema + bloque, mena con XP 3-7), `.alloy()` (sin menas ni bruto, p. ej. bronce),
`.ores(stone, deepslate, nether)`, `.oreXp`, `.oreDrops`, `.smeltingXp`, `.light`, `.fuel`, `.fireResistant`,
`.noBeacon`, `.displayName`, `.tab`.

## Herramientas y armadura

```java
// Desde un material (repara con el lingote/gema)
REG.material("silver").metal().tier(Tier.IRON).tools(ToolStats.IRON).armor(ArmorStats.IRON).build();

// Sueltas, con estadísticas propias
REG.equipment("ruby", RUBY).tools(ToolStats.DIAMOND.withUses(1200)).armor(ArmorStats.DIAMOND.withToughness(1)).build();

// Mejora tipo netherita (mesa de herrería desde el equipo de diamante)
REG.equipment("mythril", MYTHRIL_INGOT).tools(ToolStats.NETHERITE)
   .upgradeFrom("minecraft:diamond", Items.NETHERITE_UPGRADE_SMITHING_TEMPLATE).build();
```

Crea `x_sword`, `x_pickaxe`, `x_axe`, `x_shovel`, `x_hoe`, `x_helmet`, `x_chestplate`, `x_leggings` y `x_boots`.
También genera las recetas vanilla, las tags (`minecraft:swords`, `head_armor`…, `trimmable_armor`, que además
las hacen encantables) y el material de armadura registrado. Hay presets copiados de vanilla: `WOOD`, `STONE`,
`IRON`, `GOLD`, `DIAMOND` y `NETHERITE`.
Texturas: `item/x_sword.png`... y la armadura puesta en `textures/models/armor/x_layer_1.png` / `x_layer_2.png`.

## Generación en el mundo

```java
// Desde un material: 9 bloques por veta, 10 vetas por chunk, en triángulo entre y=-24 y y=56 (como el hierro)
REG.material("tin").metal().generate(9, 10, -24, 56).build();
REG.material("ruby").gem().generate(o -> o.size(4).rarity(2).uniform(-64, 16).airExposure(0.5f)).build();

// Mena suelta
REG.worldgen().ore("ore_jade").replace(OreTarget.STONE, JADE_ORE).size(6).count(4).uniform(0, 64)
   .biomes("minecraft:is_jungle").build();

// Mancha como el granito o la diorita (bolsas de 64, 1 cada 6 chunks, y 0-60)
REG.worldgen().patch("marble_patch", MARBLE).build();
```

`OreTarget`: `STONE`, `DEEPSLATE`, `ANY_OVERWORLD_STONE`, `NETHER`, `END`, o `replaceTag("...")`.
El datagen escribe la `configured_feature` y la `placed_feature`, y se inyectan en los biomas en los dos loaders.

> En NeoForge, la inyección de Architectury necesita un `biome_modifier` que SteelCodeCore ya incluye. **No añadas
> otro en tu mod**: si no, las features se generarían el doble.

## Madera y árboles

```java
WoodSet MAPLE = REG.wood("maple")
        .tree(TreeShape.FANCY_OAK)              // OAK, FANCY_OAK, BIRCH, SPRUCE, ACACIA, CHERRY, JUNGLE
        .generate("minecraft:is_forest", 2)     // o .generateRare(biomas, 1 cada N chunks)
        .build();

REG.wood("pine").tree(TreeShape.SPRUCE).tintedLeaves().planks(f -> f.createAll()).build();
```

Crea, sin escribir nada más:

- **Bloques:** tronco, madera, tronco y madera descortezados, tablones con su familia (escaleras, losa, valla, puerta de valla, botón, placa, puerta y trampilla), hojas, brote y brote en maceta.
- **Árbol:** el brote crece a partir de la forma vanilla elegida, con tus bloques.
- **Loot:** las hojas sueltan brotes, palos y manzanas igual que las de roble.
- **Comportamiento:** el hacha descorteza, todo arde como en vanilla, y hojas y brote se pueden compostar.
- **Tags:** `<mod>:maple_logs` y su uso en `logs_that_burn` (lo que la hace combustible y le da recetas vanilla), además de `leaves`, `saplings`, `planks`…
- **Recetas:** tablones, madera y madera descortezada.

`tintedLeaves()` pinta las hojas con el color de follaje del bioma; dibuja su textura en gris.

Texturas (en `block/`): `maple_log`, `maple_log_top`, `stripped_maple_log`, `stripped_maple_log_top`, `maple_planks`,
`maple_leaves`, `maple_sapling`, `maple_door_top`, `maple_door_bottom`, `maple_trapdoor`. En `item/`: `maple_door`.

### Carteles y barcas

`REG.wood(...)` crea también `maple_sign`, `maple_hanging_sign` (con sus versiones de pared), `maple_boat` y
`maple_chest_boat`, con recetas y tags. Los carteles usan el block entity, el renderizado y la pantalla de edición de
vanilla; las barcas, una entidad propia con el modelo vanilla. `.noSigns()` / `.noBoats()` para quitarlos.

Texturas: `entity/signs/maple.png`, `entity/signs/hanging/maple.png`, `gui/hanging_signs/maple.png`,
`entity/boat/maple.png` y `entity/chest_boat/maple.png` (todas en `assets/<mod>/textures/`); en `item/`:
`maple_sign`, `maple_hanging_sign`, `maple_boat`, `maple_chest_boat`.

## Cultivos y plantas

```java
// Como el trigo: tomato_seeds plantan tomato_crop, que suelta tomates (y semillas con fortuna)
CropBuilder.CropSet TOMATO = REG.crop("tomato").food(3, 0.6f).build();

// Como la zanahoria: el fruto se planta. 8 texturas de etapas en vez de 4
REG.crop("onion").plantedByProduce().stages(8).food(2, 0.3f).build();

// Flor con maceta, tinte y efecto de estofado sospechoso, generada en bosques
REG.plant("bluebell").flower(MobEffects.NIGHT_VISION, 5).dye(DyeColor.BLUE).generate("minecraft:is_forest", 8).build();
REG.plant("giant_bluebell").flower(MobEffects.NIGHT_VISION, 5).tall().build();     // dos bloques de alto
REG.plant("dry_shrub").bush().generate("minecraft:is_badlands", 2).build();        // solo con tijeras
```

Los cultivos funcionan con el polvo de hueso, la tierra de cultivo, las abejas, los aldeanos granjeros y el compostador.
Texturas: `block/tomato_stage0..3`, `item/tomato`, `item/tomato_seeds`; plantas: `block/bluebell` o
`block/giant_bluebell_bottom` / `_top`.

## Colores

```java
// Una textura en gris (block/marble.png) tintada para los 16 colores, y receta 8 + tinte → 8
ColorSet MARBLE_COLORS = REG.colors("marble").copy(Blocks.CALCITE).tinted().dyeFrom(MARBLE).build();

// Una textura por color (block/red_tiles.png...) con escaleras y losas de cada uno
ColorSet TILES = REG.colors("tiles").copy(Blocks.TERRACOTTA).family(f -> f.stairs().slab()).build();

Block red = MARBLE_COLORS.get(DyeColor.RED).get();
```

Opciones: `renderLayer`, `drops`, `light`, `tool`, `flammable`, `only(DyeColor...)`, `tab`. Todos van a la tag `<mod>:<name>`.

## Más generación

```java
REG.worldgen().rock("mossy_boulder", Blocks.MOSSY_COBBLESTONE).count(2).biomes("minecraft:is_taiga").build();
REG.worldgen().disk("jade_disk", JADE).radius(2, 4).biomes("minecraft:is_river").build();
REG.worldgen().geode("jade_geode").inner(JADE).budding(BUDDING_JADE).buds(SMALL, MEDIUM, LARGE, CLUSTER)
   .outer(Blocks.SMOOTH_BASALT).rarity(24).build();
```

Todas las features admiten `.biomes(...)` y `.enabledIf(...)` (ver configuración).

## Traducciones

Los nombres se generan con patrones ("Escaleras de %s") y palabras ("gold" → "oro"). `en_us` sale siempre;
`es_es` ya trae los patrones y muchas palabras (colores, materiales y maderas vanilla, metales habituales).
Normalmente solo tienes que traducir tus palabras:

```java
REG.lang("es_es").words("mythril", "mithril", "jade", "jade");
REG.lang("es_es").set("ruby_sword", "Espada carmesí");             // excepción puntual: id o clave completa
REG.lang("fr_fr").pattern("stairs", "Escalier en %s").words("gold", "or");   // idioma nuevo
```

Resultado: `gold_stairs` → "Gold Stairs" / "Escaleras de oro"; `red_marble` → "Red Marble" / "Mármol rojo";
`deepslate_tin_ore` → "Mena de estaño de pizarra profunda". El datagen avisa en el log de qué palabras o patrones
faltan en cada idioma.

`.displayName("...")` en cualquier builder fija el nombre en inglés. Los colores están en masculino en español;
si tu bloque es femenino, cámbialo con `.word("red", "roja")`.

## Más equipo

```java
REG.equipment("ruby", RUBY).bow(500).crossbow(600).fishingRod(100).shield(400).armor(ArmorStats.DIAMOND).horseArmor().build();
```

Arco, ballesta y caña: modelos con sus etapas (tensar, cargado, lanzada) como los vanilla. Escudo: modelo vanilla con
tu textura (`entity/shield/ruby_shield.png`, 64x64 como `shield_base_nopattern`; no admite estandartes). Armadura de
caballo: `entity/horse/armor/horse_armor_ruby.png`.

> En Fabric, la durabilidad de los escudos no-vanilla no baja al bloquear (vanilla solo comprueba su escudo);
> en NeoForge sí.

## Items con comportamiento (sin clases)

```java
REG.item("magic_stick").glint().tooltip("Cura 1 corazón")
   .onUse((level, player, hand, stack) -> { player.heal(2); return null; })     // null = sigue lo normal
   .onUseOnBlock(ctx -> ...).onHit((stack, target, attacker) -> target.igniteForSeconds(3))
   .build();

REG.item("spicy_pepper").food(2, 0.2f).foodEffect(MobEffects.FIRE_RESISTANCE, 10, 0, 1.0f).alwaysEdible().build();
REG.item("greatsword").largeHandheld().build();                          // se ve el doble en la mano
REG.item("rune").modelParent("minecraft:item/generated", "item/rune_glow").build();   // modelo a medida
REG.block("glass_door").shape(Variant.DOOR, Blocks.GLASS).itemModel("block/glass_door_bottom_left").build();  // item en 3D
```

Huevos y plantillas:

```java
REG.spawnEgg(MY_MOB, 0x00AFAF, 0x799C65);
RegistrySupplier<Item> UPGRADE = REG.smithingTemplate("ruby_upgrade_smithing_template")
        .texts("Diamond Equipment", "Ruby", "Ruby Upgrade", "Add diamond gear", "Add ruby")
        .duplicateWith(Items.DIAMOND, Blocks.DEEPSLATE).build();
REG.equipment("ruby_plus", RUBY).tools(ToolStats.NETHERITE).upgradeFrom("minecraft:diamond", UPGRADE).build();
```

## Efectos, pociones y encantamientos

```java
RegistrySupplier<MobEffect> BLEEDING = REG.effect("bleeding").harmful().color(0x8A0303)
        .onTick((e, amp) -> { e.hurt(e.damageSources().magic(), 1 + amp); return true; }, 20).build();
REG.effect("lightness").beneficial().attribute(Attributes.MOVEMENT_SPEED, 0.2, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL).build();

REG.potion("bleeding").effect(BLEEDING, 45, 0).brewedFrom(Potions.AWKWARD, Items.REDSTONE_BLOCK)
   .longVersion(90).strongVersion(22, 1).build();         // + arrojadiza, persistente y flecha

ResourceKey<Enchantment> VENOM = REG.enchantment("venom").supported("#minecraft:enchantable/sword").maxLevel(3)
        .damageAgainst("#minecraft:sensitive_to_bane_of_arthropods", 2.0, 2.0).build();
REG.enchantment("swiftness").supported("#minecraft:enchantable/foot_armor").slots("feet")
   .attribute("minecraft:generic.movement_speed", 0.1, 0.1, "add_multiplied_base").treasure().build();
```

Icono del efecto: `textures/mob_effect/bleeding.png` (18x18).

## Bloques con inventario y funcionales

```java
REG.block("crate").copy(Blocks.BARREL).container(3, true).tool(MiningTool.AXE).build();   // 3 filas, orientable
REG.block("small_box").copy(Blocks.OAK_PLANKS).container(1, false).build();
REG.block("jade_lamp").copy(Blocks.REDSTONE_LAMP).redstoneLamp().build();     // jade_lamp + jade_lamp_on
REG.block("ash").copy(Blocks.SAND).falling(0x555555).build();
REG.block("speed_pad").copy(Blocks.STONE).stepEffect(MobEffects.MOVEMENT_SPEED, 3, 1).build();
REG.block("hot_stone").copy(Blocks.STONE).stepDamage(1).build();
REG.block("rope_ladder").copy(Blocks.LADDER).ladder().build();
```

Los contenedores usan el menú genérico de cofre (no hay que registrar menús ni pantallas). Funcionan con tolvas y
comparadores, guardan el nombre si los renombras y sueltan lo que tienen al romperse.

## Loot, comercios y avances

```java
REG.loot().add(LootInjector.DUNGEON, RUBY, 0.25f, 1, 3);                 // 25 %: 1-3 rubíes
REG.loot().add(LootInjector.ALL_VILLAGE_CHESTS, TIN_INGOT, 0.4f, 2, 5);

REG.trades().villager(VillagerProfession.TOOLSMITH, 3).sells(RUBY_PICKAXE, 1, 18).buys(RUBY, 4, 1);
REG.trades().wanderer(false).sells(MAPLE_SAPLING, 1, 5);

ResourceLocation ROOT = REG.advancements().root("root", RUBY).title("SteelCode").description("Empieza")
        .background("minecraft:textures/block/calcite.png").obtain(RUBY).build();
REG.advancements().child("farmer", ROOT, TOMATO).title("Granjero").obtain(TOMATO).frame(Advancements.Frame.GOAL).xp(20).build();
```

## Configuración

```java
public static final CoreConfig CFG = REG.config();                         // config/<modid>.toml
public static final CoreConfig.Value<Boolean> GEODES = CFG.bool("jade_geodes", true, "Generar geodas de jade");
public static final CoreConfig.Value<Integer> HEAT = CFG.integer("max_heat", 100, 1, 1000, "Calor máximo");

REG.worldgen().geode("jade_geode").inner(JADE).enabledIf(GEODES::get).build();
if (HEAT.get() > 50) { ... }
```

El archivo se crea con comentarios, valor por defecto y rango. Si un valor no es válido, se usa el de por defecto y
se avisa en el log.

## Aviso de texturas que faltan

Al ejecutar el datagen, SteelCodeCore lista en el log todos los png que tus bloques, items, entidades, armaduras y
efectos necesitan y no están en `src/main/resources`:

```
[mimod] Faltan 3 texturas (se verán moradas y negras):
  assets/mimod/textures/block/tomato_stage0.png
  ...
```

## Otras opciones de bloques e items

`.flammable(arde, propaga)`, `.compostable(probabilidad)` y `.cross()` (plantas en cruz) en `REG.block(...)`.
`.compostable(...)` también en `REG.item(...)`.

## Variantes por datos (lobo, cuadro, trim, estandarte, disco)

```java
REG.variants().wolf("ember", "#minecraft:is_nether");            // textures/entity/wolf/ember(_tame,_angry).png
REG.variants().painting("sunset", 2, 1, "Sunset", "RuDaHee");    // textures/painting/sunset.png
REG.variants().trimMaterial("ruby", RUBY.main(), 0xD01030);      // paleta auto (atlas armor_trims + blocks)
RegistrySupplier<Item> ANVIL = REG.variants().bannerPattern("anvil", "Anvil");   // item + tag pattern_item/anvil
RegistrySupplier<Item> DISC  = REG.variants().musicDisc("forge", 142, 8, "RuDaHee - Forge"); // sounds/records/forge.ogg
```

## Texturas conectadas y aleatorias

```java
REG.block("framed_glass").copy(Blocks.GLASS).connected(true).build();   // vidrio: caras internas ocultas
REG.block("framed_stone").copy(Blocks.STONE).connected(false).build();
REG.block("rough_dirt").copy(Blocks.DIRT).randomRotation().build();     // como la hierba/arena
REG.block("pebbles").copy(Blocks.STONE).randomTextures(3).build();      // pebbles.png, pebbles_1.png, pebbles_2.png
```

`connected` usa dos texturas: `<id>.png` (con borde) y `<id>_center.png` (sin borde). El borde se dibuja solo en los lados sin vecino del mismo bloque. Va por blockstate + multipart, sin mixins ni modelos custom, así que funciona en los dos loaders y con Sodium.

## Estados extra: interruptor, oxidación y cristales

```java
REG.block("switch_lamp").copy(Blocks.GLOWSTONE).toggleable().build();   // clic derecho = encender/apagar (prop "lit")

OxidizingSet TIN = REG.oxidizing("tin_plate").stairsAndSlab().build();
// 4 etapas + 4 enceradas (+ escaleras/losas). Se oxida sola, panal encera, el hacha raspa.
// Fabric: OxidizableBlocksRegistry. NeoForge: data maps generados por datagen.

CrystalSet QUARTZ = REG.crystal("rose_quartz").build();
// bloque, budding (hace crecer brotes), 3 tamaños de brote, cluster y fragmento, con loot como la amatista.
REG.crystal("jade").shard(JADE_GEM).build();   // usa un item tuyo como fragmento
```

## Teclas, HUD y red

```java
CoreNetwork.Channel<String> PING = REG.network().toServer("ping", ByteBufCodecs.STRING_UTF8,
        (player, msg) -> player.sendSystemMessage(Component.literal(msg)));
PING.sendToServer("hola");                       // desde el cliente
// toClient(...) igual, con sendTo(player, v) / sendToAll(server, v). También hay versiones con CompoundTag.

REG.keybind("cast", GLFW.GLFW_KEY_R).displayName("Cast spell")
        .onPress(player -> MANA.update(player, m -> m - 10))   // se ejecuta en el SERVIDOR (el paquete va solo)
        .onPressClient(() -> {})                               // o en el cliente
        .build();

REG.hud("mana").anchor(HudBuilder.Anchor.BOTTOM_LEFT, 10, 30)   // esquina + margen
        .bar(() -> MANA.getClient() / 100f, 80, 5, 0xFF3070FF)
        .text(() -> Component.literal("Mana " + MANA.getClient()), 0xFFFFFFFF)
        .visibleWhen(() -> MANA.getClient() < 100)
        .build();
```

## Datos persistentes del jugador y del mundo

```java
PlayerData<Integer> MANA = REG.playerData("mana", Codec.INT, () -> 50)
        .copyOnDeath()                         // sobrevive a la muerte
        .synced(ByteBufCodecs.VAR_INT)         // el cliente lo recibe (getClient()) al entrar, reaparecer y cambiar de dimensión
        .build();
MANA.get(player); MANA.set(player, 80); MANA.update(player, m -> m + 1);

WorldData<Integer> COUNTER = REG.worldData("counter", Codec.INT, () -> 0);
COUNTER.update(level, c -> c + 1);             // por dimensión; getGlobal/setGlobal usa el overworld
```

Fabric usa Data Attachments y NeoForge sus AttachmentTypes; se guardan en el NBT del jugador. El mundo usa `SavedData`.

## Comandos

```java
REG.command("mana").permission(2)
        .sub("get", n -> n.executes(ctx -> ctx.reply("mana=" + MANA.get(ctx.self()))))
        .sub("set", n -> n.arg("player", CommandBuilder.player()).arg("value", CommandBuilder.integer(0, 1000))
                .executes(ctx -> { MANA.set(ctx.player("player"), ctx.integer("value")); ctx.reply("ok"); }))
        .build();
// => /steelcodecore mana get | /steelcodecore mana set <player> <value>
```

Argumentos: `player, players, integer, decimal, bool, word, text, item, blockPos`.

## Estadísticas y triggers de avances

```java
CoreStat JUMPS = REG.stat("ruby_jumps");                // aparece en la pantalla de estadísticas
JUMPS.award(player, 1);

RegistrySupplier<CoreTrigger> PRESSED = REG.trigger("pressed_key");
PRESSED.get().fire(serverPlayer);
REG.advancements().child("caster", root, RUBY.main()).title("Caster").description("Press R").trigger(PRESSED).build();
```

## JEI / REI / EMI y Jade / WTHIT

```java
REG.block("framed_glass")...
        .info("Se conecta con los cristales vecinos")       // página de info en JEI, REI y EMI (traducible)
        .tooltipLine(state -> Component.literal("Conectado"))  // línea extra al mirar el bloque con Jade o WTHIT
        .build();
REG.block("debug_block")....hideInViewers().build();          // no aparece en JEI/REI/EMI
REG.item("secret").info("...").hideInViewers().build();
```

Los plugins vienen ya registrados en la librería y solo se cargan si el mod correspondiente está instalado; no son dependencias.

## Recetas por defecto (plantillas)

Se enganchan al bloque o item mientras lo creas; el resultado es el propio bloque/item:

```java
REG.block("gold_stairs").shape(Variant.STAIRS, Blocks.GOLD_BLOCK)
   .recipe(RecipeTemplate.stairs(Blocks.GOLD_BLOCK))
   .recipe(RecipeTemplate.stonecutting(Blocks.GOLD_BLOCK, 1))
   .build();

REG.item("ruby_sword").handheld().recipe(RecipeTemplate.sword(RUBY)).build();
REG.item("ruby_helmet").recipe(RecipeTemplate.helmet("#c:gems/ruby")).build();
REG.item("mythic_sword").recipe(RecipeTemplate.smithing(Items.NETHERITE_UPGRADE_SMITHING_TEMPLATE, RUBY_SWORD, Items.NETHERITE_INGOT)).build();
```

O sueltas, con `REG.recipes().stairs(resultado, ingrediente)`, etc.:

| Grupo | Plantillas |
|---|---|
| Formas | `stairs`, `slab`, `verticalSlab`, `verticalSlabFromSlab`, `wall`, `fence`, `fenceGate`, `button`, `pressurePlate`, `door`, `trapdoor`, `woodenTrapdoor`, `pane`, `carpet` |
| Decorativos | `polished` (2x2), `pillar`, `chiseled` (2 losas), `cracked` (horno), `surround` (8 alrededor de 1) |
| Almacenamiento | `storage` (9 ↔ 1), `storage2x2` (4 → 1), `nuggets` (9 ↔ lingote) |
| Equipo | `sword`, `pickaxe`, `axe`, `shovel`, `hoe`, `toolSet`, `helmet`, `chestplate`, `leggings`, `boots`, `armorSet`, `smithing` |
| Hornos | `smelting`, `blasting`, `smoking`, `campfire`, `oreSmelting`, `foodCooking`, `stonecutting` |

Las recetas usan los patrones y categorías de vanilla, y cada una genera su desbloqueo en el libro de recetas.
`RecipeTemplate.of((recipes, self) -> ...)` para cualquier otra.

## Recetas y tags a mano

```java
REG.recipes().shaped(BRONZE.ingot(), 4).pattern("TT", "TC").define('T', "#c:ingots/tin").define('C', Items.COPPER_INGOT).save();
REG.recipes().shapeless(RUBY, 9).requires(RUBY_BLOCK).save("ruby_from_block");
REG.recipes().oreSmelting(RAW_TIN, TIN_INGOT, 0.7f);        // horno + alto horno
REG.recipes().foodCooking(RAW_MEAT, COOKED_MEAT, 0.35f);   // horno + ahumador + hoguera
REG.recipes().stonecutting(Blocks.STONE, RUBY_BRICKS, 1);

REG.blockTag("minecraft:dragon_immune", OBSIDIAN_BRICKS);
REG.itemTag("minecraft:piglin_loved", GOLD.stairs());
```

Los items se pueden pasar como `RegistrySupplier`, `Items.X`, `"minecraft:stick"` o `"#c:ingots/tin"`
(si empieza por `#`, es una tag).

## Datagen

Genera desde Fabric hacia `common/src/main/generated`, que comparten los dos loaders.

```java
// fabric: entrypoint "fabric-datagen" en fabric.mod.json
public final class MyDatagen implements DataGeneratorEntrypoint {
    @Override
    public void onInitializeDataGenerator(FabricDataGenerator generator) {
        SteelCodeCoreFabricDatagen.addProviders(generator.createPack(), MyContent.REG);
    }
}
```

```groovy
// fabric/build.gradle
loom {
    runs {
        datagen {
            inherit server
            name 'Data Generation'
            vmArg '-Dfabric-api.datagen'
            vmArg "-Dfabric-api.datagen.output-dir=${project(':common').file('src/main/generated')}"
            vmArg '-Dfabric-api.datagen.modid=tumod'
            runDir 'build/datagen'
        }
    }
}

// common/build.gradle
sourceSets.main.resources.srcDir 'src/main/generated'
```

`./gradlew :fabric:runDatagen`. En NeoForge existe también `SteelCodeCoreNeoForgeDatagen.addProviders(event, REG)`.

Los `.png` los pones tú en `assets/<modid>/textures/block/` e `/item/`.

## Usarla desde otro mod

```groovy
repositories {
    maven { url = 'https://codeberg.org/api/packages/steelcode/maven'; content { includeGroup 'dev.steelcode' } }
}
dependencies {
    modCompileOnly "dev.steelcode:steelcodecore-common:0.1.0"       // common
    modImplementation "dev.steelcode:steelcodecore-fabric:0.1.0"    // fabric
    modImplementation "dev.steelcode:steelcodecore-neoforge:0.1.0"  // neoforge
}
```

En Codeberg se conservan las 10 últimas versiones (con sources y javadoc). Añade `steelcodecore` como
dependencia en `fabric.mod.json` / `neoforge.mods.toml`.

## Licencia

LGPL-3.0 o posterior: cualquier mod, con cualquier licencia, puede depender de ella; las modificaciones de la
propia librería deben seguir siendo LGPL.
