// SPDX-License-Identifier: LGPL-3.0-or-later
package dev.steelcode.core.api.world;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import dev.architectury.registry.registries.RegistrySupplier;
import dev.steelcode.core.impl.registry.RegistryContext;
import dev.steelcode.core.impl.util.Refs;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.BannerPatternItem;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import org.jetbrains.annotations.ApiStatus;

import java.util.List;

/**
 * Variants that are data in 1.21: wolves, paintings, armor trim materials, banner patterns and music discs.
 * Obtain it with {@code REG.variants()}.
 * <pre>{@code
 * REG.variants().wolf("maple", "minecraft:is_forest");              // textures/entity/wolf/maple(_tame/_angry).png
 * REG.variants().painting("sunset", 2, 1, "Sunset", "RuDaHee");      // textures/painting/sunset.png (32x16)
 * REG.variants().trimMaterial("ruby", RUBY, 0xE0115F);               // textures/trims/color_palettes/ruby.png (8x1)
 * RegistrySupplier<Item> GEAR = REG.variants().bannerPattern("gear", "Gear");  // textures/entity/banner|shield/gear.png
 * RegistrySupplier<Item> DISC = REG.variants().musicDisc("forge", 142, 8, "RuDaHee - Forge"); // sounds/records/forge.ogg
 * }</pre>
 */
public final class VariantsBuilder {

    private static final String[] TRIM_PATTERNS = {"coast", "sentry", "dune", "wild", "ward", "eye", "vex", "tide", "snout",
            "rib", "spire", "wayfinder", "shaper", "silence", "raiser", "host", "flow", "bolt"};

    private final RegistryContext registry;
    private float nextTrimIndex = 0.05F;

    /** Internal: use {@code REG.variants()}. */
    @ApiStatus.Internal
    public VariantsBuilder(RegistryContext registry) {
        this.registry = registry;
    }

    /**
     * A wolf variant that spawns in some biomes.
     * Textures {@code entity/wolf/<name>.png}, {@code <name>_tame.png} and {@code <name>_angry.png}.
     * @param name   variant name
     * @param biomes a biome id, or a biome tag starting with {@code #}
     * @return the variant id
     */
    public ResourceLocation wolf(String name, String biomes) {
        String ns = registry.modId();
        ResourceLocation id = id(name);
        for (String s : new String[]{"", "_tame", "_angry"}) registry.expectTexture("assets/" + ns + "/textures/entity/wolf/" + name + s + ".png");
        registry.addData("wolf_variant", id, () -> {
            JsonObject j = new JsonObject();
            j.addProperty("wild_texture", ns + ":entity/wolf/" + name);
            j.addProperty("tame_texture", ns + ":entity/wolf/" + name + "_tame");
            j.addProperty("angry_texture", ns + ":entity/wolf/" + name + "_angry");
            j.addProperty("biomes", biomes.startsWith("#") || !biomes.contains("is_") ? biomes : "#" + biomes);
            return j;
        });
        return id;
    }

    /**
     * A painting that can appear when placing paintings. Texture {@code painting/<name>.png}, 16 px per block.
     * <p>
     * Title and author are written to the lang files; Minecraft 1.21.1 does not display them yet (1.21.2 does).
     * @param name   painting name
     * @param width  width in blocks
     * @param height height in blocks
     * @param title  English title
     * @param author author
     * @return the variant id
     */
    public ResourceLocation painting(String name, int width, int height, String title, String author) {
        ResourceLocation id = id(name);
        registry.expectTexture("assets/" + id.getNamespace() + "/textures/painting/" + name + ".png");
        registry.addData("painting_variant", id, () -> {
            JsonObject j = new JsonObject();
            j.addProperty("asset_id", id.toString());
            j.addProperty("width", width);
            j.addProperty("height", height);
            return j;
        });
        registry.addTag("painting_variant", ResourceLocation.withDefaultNamespace("placeable"), id.toString());
        registry.literal("painting." + id.getNamespace() + "." + name + ".title", title);
        registry.literal("painting." + id.getNamespace() + "." + name + ".author", author);
        return id;
    }

    /**
     * An armor trim material for the smithing table.
     * <p>
     * You provide the 8x1 palette {@code textures/trims/color_palettes/<name>.png}; the paletted-permutation atlas sources
     * for worn armor and trim item icons are generated. Vanilla armor item models only list vanilla materials in their
     * overrides, so the trim is visible on worn armor but not on the item icon.
     * @param name       material name
     * @param ingredient item used in the smithing table (added to {@code minecraft:trim_materials})
     * @param rgb        color of the material name in tooltips, {@code 0xRRGGBB}
     * @return the trim material id
     */
    public ResourceLocation trimMaterial(String name, Object ingredient, int rgb) {
        ResourceLocation id = id(name);
        String ns = id.getNamespace();
        float index = nextTrimIndex;
        nextTrimIndex += 0.01F;
        registry.expectTexture("assets/" + ns + "/textures/trims/color_palettes/" + name + ".png");
        String key = "trim_material." + ns + "." + name;
        registry.name(key, name, "self", name);
        registry.addData("trim_material", id, () -> {
            JsonObject desc = new JsonObject();
            desc.addProperty("color", String.format("#%06X", rgb & 0xFFFFFF));
            desc.addProperty("translate", key);
            JsonObject j = new JsonObject();
            j.addProperty("asset_name", ns + "_" + name);
            j.add("description", desc);
            j.addProperty("ingredient", Refs.id(ingredient).toString());
            j.addProperty("item_model_index", index);
            return j;
        });
        registry.addItemTag(ResourceLocation.withDefaultNamespace("trim_materials"), Refs.entry(ingredient));

        // Atlas: the palette applies to every pattern (worn armor) and to the item icons.
        JsonObject perm = new JsonObject();
        perm.addProperty(ns + "_" + name, ns + ":trims/color_palettes/" + name);
        JsonArray armor = new JsonArray();
        for (String p : TRIM_PATTERNS) {
            armor.add("trims/models/armor/" + p);
            armor.add("trims/models/armor/" + p + "_leggings");
        }
        registry.addAtlasSource("armor_trims", paletted(armor, perm));
        JsonArray items = new JsonArray();
        for (String piece : new String[]{"leggings", "chestplate", "helmet", "boots"}) items.add("trims/items/" + piece + "_trim");
        registry.addAtlasSource("blocks", paletted(items, perm.deepCopy()));
        return id;
    }

    /**
     * A banner pattern with its pattern item, used in the loom.
     * Textures {@code entity/banner/<name>.png} and {@code entity/shield/<name>.png}; item texture {@code item/<name>_banner_pattern}.
     * @param name  pattern name
     * @param title English pattern name
     * @return the pattern item {@code <name>_banner_pattern}
     */
    public RegistrySupplier<Item> bannerPattern(String name, String title) {
        ResourceLocation id = id(name);
        String ns = id.getNamespace();
        String key = "block." + ns + ".banner." + name;
        registry.expectTexture("assets/" + ns + "/textures/entity/banner/" + name + ".png");
        registry.expectTexture("assets/" + ns + "/textures/entity/shield/" + name + ".png");
        registry.addData("banner_pattern", id, () -> {
            JsonObject j = new JsonObject();
            j.addProperty("asset_id", id.toString());
            j.addProperty("translation_key", key);
            return j;
        });
        for (DyeColor c : DyeColor.values()) registry.literal(key + "." + c.getName(), Refs.titleCase(c.getName()) + " " + title);
        ResourceLocation tagId = ResourceLocation.fromNamespaceAndPath(ns, "pattern_item/" + name);
        registry.addTag("banner_pattern", tagId, id.toString());
        TagKey<net.minecraft.world.level.block.entity.BannerPattern> tag = TagKey.create(Registries.BANNER_PATTERN, tagId);
        String itemId = name + "_banner_pattern";
        registry.literal("item." + ns + "." + itemId + ".desc", title);
        return registry.item(itemId).naming("banner_pattern", name).stacksTo(1).tag("minecraft:banner_patterns")
                .factory(p -> new BannerPatternItem(tag, p)).build();
    }

    /**
     * A music disc and its jukebox song. Sound file {@code sounds/records/<name>.ogg} (streamed); item texture
     * {@code item/music_disc_<name>}.
     * @param name             song name
     * @param seconds          song length
     * @param comparatorOutput comparator signal while playing, 1-15
     * @param description      tooltip text, e.g. {@code "Artist - Title"}
     * @return the disc item {@code music_disc_<name>}
     */
    public RegistrySupplier<Item> musicDisc(String name, float seconds, int comparatorOutput, String description) {
        ResourceLocation id = id(name);
        String ns = id.getNamespace();
        registry.registerSound("music_disc." + name, "records/" + name, true);
        String key = "jukebox_song." + ns + "." + name;
        registry.literal(key, description);
        registry.addData("jukebox_song", id, () -> {
            JsonObject d = new JsonObject();
            d.addProperty("translate", key);
            JsonObject j = new JsonObject();
            j.addProperty("comparator_output", comparatorOutput);
            j.add("description", d);
            j.addProperty("length_in_seconds", seconds);
            j.addProperty("sound_event", ns + ":music_disc." + name);
            return j;
        });
        ResourceKey<net.minecraft.world.item.JukeboxSong> song = ResourceKey.create(Registries.JUKEBOX_SONG, id);
        return registry.item("music_disc_" + name).naming("music_disc", name).stacksTo(1)
                .properties(p -> p.rarity(Rarity.RARE).jukeboxPlayable(song)).tag("c:music_discs").build();
    }

    private ResourceLocation id(String name) {
        return ResourceLocation.fromNamespaceAndPath(registry.modId(), name);
    }

    private static JsonObject paletted(JsonArray textures, JsonObject permutations) {
        JsonObject s = new JsonObject();
        s.addProperty("type", "paletted_permutations");
        s.add("textures", textures);
        s.addProperty("palette_key", "trims/color_palettes/trim_palette");
        s.add("permutations", permutations);
        return s;
    }

}
