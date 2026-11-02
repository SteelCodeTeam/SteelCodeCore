// SPDX-License-Identifier: LGPL-3.0-or-later
package dev.steelcode.core.impl.i18n;

import org.jetbrains.annotations.ApiStatus;

import java.util.HashMap;
import java.util.Map;

/**
 * Patterns and words built into SteelCodeCore ({@code en_us} and {@code es_es}). A pattern is a {@code String.format}:
 * {@code %1$s} is the main word (material/wood) and {@code %2$s} the color.
 * Any of them can be added or replaced with {@code REG.lang("xx_xx")}.
 */
@ApiStatus.Internal
public final class BuiltinLang {

    private BuiltinLang() {
    }

    private static final Map<String, Map<String, String>> PATTERNS = new HashMap<>();
    private static final Map<String, Map<String, String>> WORDS = new HashMap<>();

    public static String pattern(String lang, String pattern) {
        return PATTERNS.getOrDefault(lang, Map.of()).get(pattern);
    }

    public static String word(String lang, String word) {
        return WORDS.getOrDefault(lang, Map.of()).get(word);
    }

    private static void patterns(String lang, String... pairs) {
        Map<String, String> m = PATTERNS.computeIfAbsent(lang, k -> new HashMap<>());
        for (int i = 0; i < pairs.length; i += 2) m.put(pairs[i], pairs[i + 1]);
    }

    private static void words(String lang, String... pairs) {
        Map<String, String> m = WORDS.computeIfAbsent(lang, k -> new HashMap<>());
        for (int i = 0; i < pairs.length; i += 2) m.put(pairs[i], pairs[i + 1]);
    }

    static {
        patterns("en_us",
                "self", "%s",
                // shapes
                "stairs", "%s Stairs", "slab", "%s Slab", "vertical_slab", "%s Vertical Slab", "wall", "%s Wall",
                "fence", "%s Fence", "fence_gate", "%s Fence Gate", "button", "%s Button",
                "pressure_plate", "%s Pressure Plate", "door", "%s Door", "trapdoor", "%s Trapdoor", "pane", "%s Pane",
                "carpet", "%s Carpet",
                // decorative
                "polished", "Polished %s", "chiseled", "Chiseled %s", "cracked", "Cracked %s", "cut", "Cut %s",
                "smooth", "Smooth %s", "bricks", "%s Bricks", "tiles", "%s Tiles", "pillar", "%s Pillar",
                // materials
                "ingot", "%s Ingot", "nugget", "%s Nugget", "raw", "Raw %s", "block", "Block of %s",
                "raw_block", "Block of Raw %s", "ore", "%s Ore", "deepslate_ore", "Deepslate %s Ore",
                "nether_ore", "Nether %s Ore",
                // equipment
                "sword", "%s Sword", "pickaxe", "%s Pickaxe", "axe", "%s Axe", "shovel", "%s Shovel", "hoe", "%s Hoe",
                "helmet", "%s Helmet", "chestplate", "%s Chestplate", "leggings", "%s Leggings", "boots", "%s Boots",
                // wood
                "log", "%s Log", "wood", "%s Wood", "stripped_log", "Stripped %s Log", "stripped_wood", "Stripped %s Wood",
                "planks", "%s Planks", "leaves", "%s Leaves", "sapling", "%s Sapling", "potted", "Potted %s Sapling",
                // colors
                "colored", "%2$s %1$s",
                // more
                "sign", "%s Sign", "hanging_sign", "%s Hanging Sign", "boat", "%s Boat", "chest_boat", "%s Boat with Chest",
                "seeds", "%s Seeds", "spawn_egg", "%s Spawn Egg", "bow", "%s Bow", "crossbow", "%s Crossbow",
                "shield", "%s Shield", "fishing_rod", "%s Fishing Rod", "horse_armor", "%s Horse Armor",
                "smithing_template", "Smithing Template", "potted_plant", "Potted %s",
                "potion", "Potion of %s", "splash_potion", "Splash Potion of %s", "lingering_potion", "Lingering Potion of %s",
                "tipped_arrow", "Arrow of %s",
                "banner_pattern", "Banner Pattern", "music_disc", "Music Disc",
                "exposed", "Exposed %s", "weathered", "Weathered %s", "oxidized", "Oxidized %s", "waxed", "Waxed %s",
                "waxed_exposed", "Waxed Exposed %s", "waxed_weathered", "Waxed Weathered %s", "waxed_oxidized", "Waxed Oxidized %s",
                "budding", "Budding %s", "small_bud", "Small %s Bud", "medium_bud", "Medium %s Bud", "large_bud", "Large %s Bud",
                "cluster", "%s Cluster", "shard", "%s Shard");

        patterns("es_es",
                "self", "%s",
                "stairs", "Escaleras de %s", "slab", "Losa de %s", "vertical_slab", "Losa vertical de %s",
                "wall", "Muro de %s", "fence", "Valla de %s", "fence_gate", "Puerta de valla de %s",
                "button", "Botón de %s", "pressure_plate", "Placa de presión de %s", "door", "Puerta de %s",
                "trapdoor", "Trampilla de %s", "pane", "Panel de %s", "carpet", "Alfombra de %s",
                "polished", "%s pulido", "chiseled", "%s cincelado", "cracked", "%s agrietado", "cut", "%s cortado",
                "smooth", "%s liso", "bricks", "Ladrillos de %s", "tiles", "Baldosas de %s", "pillar", "Pilar de %s",
                "ingot", "Lingote de %s", "nugget", "Pepita de %s", "raw", "%s en bruto", "block", "Bloque de %s",
                "raw_block", "Bloque de %s en bruto", "ore", "Mena de %s",
                "deepslate_ore", "Mena de %s de pizarra profunda", "nether_ore", "Mena de %s del Nether",
                "sword", "Espada de %s", "pickaxe", "Pico de %s", "axe", "Hacha de %s", "shovel", "Pala de %s",
                "hoe", "Azada de %s", "helmet", "Casco de %s", "chestplate", "Peto de %s", "leggings", "Grebas de %s",
                "boots", "Botas de %s",
                "log", "Tronco de %s", "wood", "Madera de %s", "stripped_log", "Tronco de %s sin corteza",
                "stripped_wood", "Madera de %s sin corteza", "planks", "Tablones de %s", "leaves", "Hojas de %s",
                "sapling", "Brote de %s", "potted", "Maceta con brote de %s",
                "colored", "%1$s %2$s",
                "sign", "Cartel de %s", "hanging_sign", "Cartel colgante de %s", "boat", "Bote de %s",
                "chest_boat", "Bote de %s con cofre", "seeds", "Semillas de %s", "spawn_egg", "Huevo generador de %s",
                "bow", "Arco de %s", "crossbow", "Ballesta de %s", "shield", "Escudo de %s",
                "fishing_rod", "Caña de pescar de %s", "horse_armor", "Armadura de %s para caballo",
                "smithing_template", "Plantilla de herrería", "potted_plant", "Maceta con %s",
                "potion", "Poción de %s", "splash_potion", "Poción arrojadiza de %s", "lingering_potion", "Poción persistente de %s",
                "tipped_arrow", "Flecha de %s",
                "banner_pattern", "Diseño de estandarte", "music_disc", "Disco de música",
                "exposed", "%s expuesto", "weathered", "%s erosionado", "oxidized", "%s oxidado", "waxed", "%s encerado",
                "waxed_exposed", "%s expuesto encerado", "waxed_weathered", "%s erosionado encerado",
                "waxed_oxidized", "%s oxidado encerado",
                "budding", "%s en ciernes", "small_bud", "Brote pequeño de %s", "medium_bud", "Brote mediano de %s",
                "large_bud", "Brote grande de %s", "cluster", "Racimo de %s", "shard", "Fragmento de %s");

        words("es_es",
                // colors (masculine; use .word("red", "roja") if your block noun is feminine)
                "white", "blanco", "orange", "naranja", "magenta", "magenta", "light_blue", "azul claro",
                "yellow", "amarillo", "lime", "verde lima", "pink", "rosa", "gray", "gris", "light_gray", "gris claro",
                "cyan", "cian", "purple", "morado", "blue", "azul", "brown", "marrón", "green", "verde", "red", "rojo",
                "black", "negro",
                // vanilla materials
                "gold", "oro", "iron", "hierro", "copper", "cobre", "diamond", "diamante", "emerald", "esmeralda",
                "netherite", "netherita", "coal", "carbón", "lapis", "lapislázuli", "quartz", "cuarzo",
                "amethyst", "amatista", "stone", "piedra", "stone_brick", "ladrillos de piedra", "cobblestone", "roca",
                "granite", "granito", "diorite", "diorita", "andesite", "andesita", "deepslate", "pizarra profunda",
                "sandstone", "arenisca", "brick", "ladrillo", "obsidian", "obsidiana", "glass", "cristal",
                "wool", "lana", "concrete", "hormigón", "terracotta", "terracota", "marble", "mármol",
                // woods
                "oak", "roble", "spruce", "abeto", "birch", "abedul", "jungle", "jungla", "acacia", "acacia",
                "dark_oak", "roble oscuro", "cherry", "cerezo", "mangrove", "mangle", "bamboo", "bambú",
                "maple", "arce", "pine", "pino", "willow", "sauce", "palm", "palmera",
                // metals and gems common in mods
                "tin", "estaño", "zinc", "zinc", "silver", "plata", "platinum", "platino", "bronze", "bronce",
                "steel", "acero", "lead", "plomo", "nickel", "níquel", "aluminum", "aluminio", "brass", "latón",
                "titanium", "titanio", "cobalt", "cobalto", "ruby", "rubí", "sapphire", "zafiro", "topaz", "topacio");
    }
}
