// SPDX-License-Identifier: LGPL-3.0-or-later
package dev.steelcode.core.impl.datagen;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import dev.architectury.registry.registries.RegistrySupplier;
import dev.steelcode.core.SteelCodeCore;
import dev.steelcode.core.api.block.Textures;
import dev.steelcode.core.api.block.Variant;
import dev.steelcode.core.api.data.Lang;
import dev.steelcode.core.api.data.Recipes;
import dev.steelcode.core.impl.registry.AdvancementEntry;
import dev.steelcode.core.impl.registry.BlockEntry;
import dev.steelcode.core.impl.registry.EnchantmentEntry;
import dev.steelcode.core.impl.registry.FeatureEntry;
import dev.steelcode.core.impl.registry.ItemEntry;
import dev.steelcode.core.impl.registry.RecipeEntry;
import dev.steelcode.core.impl.registry.RegistryContext;
import dev.steelcode.core.impl.util.Refs;
import net.minecraft.data.CachedOutput;
import net.minecraft.data.DataProvider;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.ApiStatus;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

/**
 * Vanilla data provider generating everything of a registry: blockstates, models, loot tables, tags, recipes
 * (with recipe-book unlocks), lang files, worldgen, data maps and more. Identical on Fabric and NeoForge.
 * Mods add it through {@code SteelCodeCoreFabricDatagen} or {@code SteelCodeCoreNeoForgeDatagen}.
 */
@ApiStatus.Internal
public final class CoreDataProvider implements DataProvider {

    /** Y rotation order: east = 0, south = 90, west = 180, north = 270 (like vanilla stairs). */
    private static final String[] FACINGS = {"east", "south", "west", "north"};
    private static final ResourceLocation VERTICAL_SLAB_TEMPLATE =
            ResourceLocation.fromNamespaceAndPath(SteelCodeCore.MOD_ID, "block/vertical_slab");
    private static final ResourceLocation TINTED_CUBE_TEMPLATE =
            ResourceLocation.fromNamespaceAndPath(SteelCodeCore.MOD_ID, "block/tinted_cube_all");

    private final RegistryContext registry;
    private final PackOutput.PathProvider blockstates;
    private final PackOutput.PathProvider models;
    private final PackOutput.PathProvider lang;
    private final PackOutput.PathProvider lootTables;
    private final PackOutput.PathProvider recipes;
    private final PackOutput.PathProvider advancements;
    private final PackOutput.PathProvider blockTags;
    private final PackOutput.PathProvider itemTags;
    private final PackOutput.PathProvider configuredFeatures;
    private final PackOutput.PathProvider placedFeatures;
    private final PackOutput output;
    private final List<CompletableFuture<?>> pending = new ArrayList<>();
    private final Map<ResourceLocation, dev.steelcode.core.impl.registry.ItemModelSpec> itemOverrides = new java.util.HashMap<>();
    private final Set<String> textures = new java.util.TreeSet<>();
    private CachedOutput cache;

    public CoreDataProvider(PackOutput output, dev.steelcode.core.api.CoreRegistry registry) {
        this(output, RegistryContext.of(registry));
    }

    public CoreDataProvider(PackOutput output, RegistryContext registry) {
        this.registry = registry;
        this.blockstates = output.createPathProvider(PackOutput.Target.RESOURCE_PACK, "blockstates");
        this.models = output.createPathProvider(PackOutput.Target.RESOURCE_PACK, "models");
        this.lang = output.createPathProvider(PackOutput.Target.RESOURCE_PACK, "lang");
        this.lootTables = output.createPathProvider(PackOutput.Target.DATA_PACK, "loot_table");
        this.recipes = output.createPathProvider(PackOutput.Target.DATA_PACK, "recipe");
        this.advancements = output.createPathProvider(PackOutput.Target.DATA_PACK, "advancement");
        this.blockTags = output.createPathProvider(PackOutput.Target.DATA_PACK, "tags/block");
        this.itemTags = output.createPathProvider(PackOutput.Target.DATA_PACK, "tags/item");
        this.configuredFeatures = output.createPathProvider(PackOutput.Target.DATA_PACK, "worldgen/configured_feature");
        this.placedFeatures = output.createPathProvider(PackOutput.Target.DATA_PACK, "worldgen/placed_feature");
        this.output = output;
        this.enchantments = output.createPathProvider(PackOutput.Target.DATA_PACK, "enchantment");
        this.enchantmentTags = output.createPathProvider(PackOutput.Target.DATA_PACK, "tags/enchantment");
    }

    private final PackOutput.PathProvider enchantments;
    private final PackOutput.PathProvider enchantmentTags;

    @Override
    public CompletableFuture<?> run(CachedOutput cache) {
        this.cache = cache;
        pending.clear();
        textures.clear();
        itemOverrides.clear();
        for (BlockEntry entry : registry.blockEntries()) {
            if (entry.itemModel() != null) itemOverrides.put(entry.id(), entry.itemModel());
        }

        for (BlockEntry entry : registry.blockEntries()) {
            if (entry.variant() != null) generateBlock(entry);
            generateLoot(entry);
        }
        for (ItemEntry item : registry.itemEntries()) generateItem(item);
        for (RegistrySupplier<?>[] crop : registry.cropLoots()) {
            save(Json.cropLoot(crop[0].getId(), crop[1].getId(), crop[2] == null ? null : crop[2].getId(), 7),
                    lootTables.json(crop[0].getId().withPrefix("blocks/")));
        }
        for (EnchantmentEntry e : registry.enchantmentEntries()) save(e.json().get(), enchantments.json(e.id()));
        writeTags(enchantmentTags, registry.enchantmentTags());
        for (AdvancementEntry a : registry.advancementEntries()) save(a.json().get(), advancements.json(a.id()));
        Path dataRoot = output.getOutputFolder(PackOutput.Target.DATA_PACK);
        Path root = output.getOutputFolder();
        for (RegistryContext.DataEntry d : registry.dataEntries()) {
            save(d.json().get(), dataRoot.resolve(d.id().getNamespace()).resolve(d.folder()).resolve(d.id().getPath() + ".json"));
        }
        registry.otherTags().forEach((folder, tags) -> tags.forEach((tag, entries) -> {
            JsonObject json = new JsonObject();
            JsonArray values = new JsonArray();
            entries.forEach(values::add);
            json.add("values", values);
            save(json, dataRoot.resolve(tag.getNamespace()).resolve("tags").resolve(folder).resolve(tag.getPath() + ".json"));
        }));
        for (RegistryContext.JsonEntry a : registry.assetEntries()) save(a.json(), root.resolve(a.path()));
        if (!registry.soundDefinitions().isEmpty()) {
            JsonObject sounds = new JsonObject();
            registry.soundDefinitions().forEach(sounds::add);
            save(sounds, output.getOutputFolder(PackOutput.Target.RESOURCE_PACK).resolve(registry.modId()).resolve("sounds.json"));
        }
        writeDataMap("oxidizables", "next_oxidation_stage", registry.oxidizables());
        writeDataMap("waxables", "waxed", registry.waxables());
        writeRecipes();
        writeTags(blockTags, registry.blockTags());
        writeTags(itemTags, registry.itemTags());
        writeWorldgen();
        writeCompostables();
        if (registry.generateLang()) writeLang();
        checkTextures();
        return CompletableFuture.allOf(pending.toArray(CompletableFuture[]::new));
    }

    @Override
    public String getName() {
        return "SteelCodeCore resources for " + registry.modId();
    }

    // ================================================================ blocks

    private void generateBlock(BlockEntry e) {
        ResourceLocation id = e.id();
        Textures t = e.textures();
        ResourceLocation model = blockModel(id, "");
        switch (e.variant()) {
            case BASE, POLISHED, CHISELED, CRACKED, CUT, SMOOTH, BRICKS, TILES -> {
                if (t.isUniform()) saveModel(model, Json.model("minecraft:block/cube_all", "all", t.side()));
                else saveModel(model, Json.model("minecraft:block/cube_bottom_top",
                        "top", t.top(), "bottom", t.bottom(), "side", t.side()));
                int textures = e.variant() == Variant.BASE ? Math.max(1, e.param()) : 1;
                boolean rotate = e.variant() == Variant.BASE && e.param2() == 1;
                if (textures > 1 || rotate) {
                    JsonArray options = new JsonArray();
                    for (int i = 0; i < textures; i++) {
                        ResourceLocation m = i == 0 ? model : blockModel(id, "_" + i);
                        if (i > 0) saveModel(m, Json.model("minecraft:block/cube_all", "all", t.side().withSuffix("_" + i)));
                        for (int r = 0; r < (rotate ? 4 : 1); r++) options.add(Json.ref(m, 0, r * 90, false));
                    }
                    JsonObject variants = new JsonObject();
                    variants.add("", options);
                    JsonObject state = new JsonObject();
                    state.add("variants", variants);
                    saveBlockstate(id, state);
                } else {
                    saveBlockstate(id, new Json.Variants().add("", Json.ref(model)).build());
                }
                saveItemModel(id, Json.parentOnly(model));
            }
            case CONNECTED -> connected(id, t.side());
            case PILLAR -> pillar(id, t);
            case STAIRS -> stairs(id, t);
            case SLAB -> slab(id, t, e.fullModel());
            case VERTICAL_SLAB -> verticalSlab(id, t, e.fullModel());
            case WALL -> wall(id, t.side());
            case FENCE -> fence(id, t.side());
            case FENCE_GATE -> fenceGate(id, t.side());
            case BUTTON -> button(id, t.side());
            case PRESSURE_PLATE -> pressurePlate(id, t.side(), e.weighted());
            case DOOR -> door(id, t, e.itemTexture());
            case TRAPDOOR -> trapdoor(id, t.side());
            case PANE -> pane(id, t.side(), e.itemTexture());
            case LEAVES, TINTED_LEAVES -> {
                saveModel(model, e.variant() == Variant.TINTED_LEAVES
                        ? Json.model("minecraft:block/leaves", "all", t.side())
                        : Json.model("minecraft:block/cube_all", "all", t.side()));
                saveBlockstate(id, new Json.Variants().add("", Json.ref(model)).build());
                saveItemModel(id, Json.parentOnly(model));
            }
            case TINTED_CUBE -> {
                saveModel(model, Json.model(TINTED_CUBE_TEMPLATE.toString(), "all", t.side()));
                saveBlockstate(id, new Json.Variants().add("", Json.ref(model)).build());
                saveItemModel(id, Json.parentOnly(model));
            }
            case CROSS -> {
                saveModel(model, Json.model("minecraft:block/cross", "cross", t.side()));
                if (e.param() == 1) {
                    // Orientable crystal (like amethyst)
                    saveBlockstate(id, new Json.Variants()
                            .add("facing=down", Json.ref(model, 180, 0, false))
                            .add("facing=east", Json.ref(model, 90, 90, false))
                            .add("facing=north", Json.ref(model, 90, 0, false))
                            .add("facing=south", Json.ref(model, 90, 180, false))
                            .add("facing=up", Json.ref(model))
                            .add("facing=west", Json.ref(model, 90, 270, false)).build());
                } else {
                    saveBlockstate(id, new Json.Variants().add("", Json.ref(model)).build());
                }
                saveItemModel(id, Json.model("minecraft:item/generated", "layer0", t.side()));
            }
            case POTTED -> {
                saveModel(model, Json.model("minecraft:block/flower_pot_cross", "plant", t.side()));
                saveBlockstate(id, new Json.Variants().add("", Json.ref(model)).build());
            }
            case SIGN, WALL_SIGN, HANGING_SIGN, WALL_HANGING_SIGN -> {
                JsonObject m = new JsonObject();
                JsonObject tex = new JsonObject();
                tex.addProperty("particle", t.side().toString());
                m.add("textures", tex);
                saveModel(model, m);
                saveBlockstate(id, new Json.Variants().add("", Json.ref(model)).build());
            }
            case CROP -> {
                int stages = Math.max(1, e.param());
                int maxAge = e.param2() > 0 ? e.param2() : 7;
                for (int i = 0; i < stages; i++) {
                    saveModel(blockModel(id, "_stage" + i), Json.model("minecraft:block/crop", "crop", t.side().withSuffix(String.valueOf(i))));
                }
                int[] carrots = {0, 0, 1, 1, 2, 2, 2, 3};
                Json.Variants states = new Json.Variants();
                for (int age = 0; age <= maxAge; age++) {
                    int stage = stages == maxAge + 1 ? age : stages == 4 && maxAge == 7 ? carrots[age] : Math.min(stages - 1, age * stages / (maxAge + 1));
                    states.add("age=" + age, Json.ref(blockModel(id, "_stage" + stage)));
                }
                saveBlockstate(id, states.build());
            }
            case DOUBLE_PLANT -> {
                saveModel(blockModel(id, "_bottom"), Json.model("minecraft:block/cross", "cross", t.side().withSuffix("_bottom")));
                saveModel(blockModel(id, "_top"), Json.model("minecraft:block/cross", "cross", t.side().withSuffix("_top")));
                saveBlockstate(id, new Json.Variants().add("half=lower", Json.ref(blockModel(id, "_bottom")))
                        .add("half=upper", Json.ref(blockModel(id, "_top"))).build());
                saveItemModel(id, Json.model("minecraft:item/generated", "layer0", t.side().withSuffix("_top")));
            }
            case LAMP -> {
                saveModel(model, Json.model("minecraft:block/cube_all", "all", t.side()));
                saveModel(blockModel(id, "_on"), Json.model("minecraft:block/cube_all", "all", t.side().withSuffix("_on")));
                saveBlockstate(id, new Json.Variants().add("lit=false", Json.ref(model))
                        .add("lit=true", Json.ref(blockModel(id, "_on"))).build());
                saveItemModel(id, Json.parentOnly(model));
            }
            case LADDER -> {
                saveModel(model, Json.model(SteelCodeCore.MOD_ID + ":block/ladder", "texture", t.side()));
                Json.Variants states = new Json.Variants();
                for (int f = 0; f < 4; f++) states.add("facing=" + FACINGS[f], Json.ref(model, 0, Math.floorMod(f * 90 + 90, 360), false));
                saveBlockstate(id, states.build());
                saveItemModel(id, Json.model("minecraft:item/generated", "layer0", t.side()));
            }
            case ORIENTABLE -> {
                saveModel(model, Json.model("minecraft:block/orientable", "top", t.top(), "front", t.bottom(), "side", t.side()));
                Json.Variants states = new Json.Variants();
                for (int f = 0; f < 4; f++) states.add("facing=" + FACINGS[f], Json.ref(model, 0, Math.floorMod(f * 90 + 90, 360), false));
                saveBlockstate(id, states.build());
                saveItemModel(id, Json.parentOnly(model));
            }
            case CARPET -> {
                saveModel(model, Json.model("minecraft:block/carpet", "wool", t.side()));
                saveBlockstate(id, new Json.Variants().add("", Json.ref(model)).build());
                saveItemModel(id, Json.parentOnly(model));
            }
        }
    }

    private void generateLoot(BlockEntry e) {
        ResourceLocation id = e.id();
        if (e.noLoot() || e.variant() == Variant.CROP) return;
        JsonObject table;
        if (e.variant() == Variant.SLAB) table = Json.slabLoot(id, "type", "double");
        else if (e.variant() == Variant.VERTICAL_SLAB) table = Json.slabLoot(id, "double", "true");
        else if (e.variant() == Variant.DOOR) table = Json.doorLoot(id);
        else if (e.drops().vanillaTable() != null) table = vanillaLoot(id, e.drops());
        else if (e.drops().isShearsOnly()) table = Json.shearsLoot(id);
        else table = Json.loot(id, e.drops());
        save(table, lootTables.json(id.withPrefix("blocks/")));
    }

    private static JsonObject vanillaLoot(ResourceLocation id, dev.steelcode.core.api.block.Drops drops) {
        Object[] pairs = drops.replacements();
        String[] replacements = new String[pairs.length];
        for (int i = 0; i < pairs.length; i += 2) {
            replacements[i] = pairs[i].toString();
            replacements[i + 1] = pairs[i + 1] == dev.steelcode.core.api.block.Drops.SELF ? id.toString() : Refs.id(pairs[i + 1]).toString();
        }
        JsonObject table = VanillaData.json("loot_table/blocks/" + drops.vanillaTable() + ".json", replacements);
        table.addProperty("random_sequence", id.getNamespace() + ":blocks/" + id.getPath());
        return table;
    }

    // ================================================================ items

    private void generateItem(ItemEntry item) {
        ResourceLocation id = item.id();
        ResourceLocation tex = item.texture();
        switch (item.model()) {
            case GENERATED -> saveItemModel(id, Json.model("minecraft:item/generated", "layer0", tex));
            case HANDHELD -> saveItemModel(id, Json.model("minecraft:item/handheld", "layer0", tex));
            case HANDHELD_LARGE -> saveItemModel(id, Json.model(SteelCodeCore.MOD_ID + ":item/handheld_large", "layer0", tex));
            case CUSTOM -> {
                var spec = item.custom();
                saveItemModel(id, spec.texture() == null ? Json.parentOnly(spec.parent())
                        : Json.model(spec.parent().toString(), "layer0", spec.texture()));
            }
            case BOW -> {
                JsonObject base = Json.model("minecraft:item/bow", "layer0", tex);
                JsonArray overrides = new JsonArray();
                float[] pulls = {0, 0.65F, 0.9F};
                for (int i = 0; i < 3; i++) {
                    ResourceLocation m = id.withPrefix("item/").withSuffix("_pulling_" + i);
                    saveModel(m, Json.model("minecraft:item/bow", "layer0", tex.withSuffix("_pulling_" + i)));
                    JsonObject pred = new JsonObject();
                    pred.addProperty("pulling", 1);
                    if (pulls[i] > 0) pred.addProperty("pull", pulls[i]);
                    overrides.add(override(pred, m));
                }
                base.add("overrides", overrides);
                saveItemModel(id, base);
            }
            case CROSSBOW -> {
                JsonObject base = Json.model("minecraft:item/crossbow", "layer0", tex.withSuffix("_standby"));
                JsonArray overrides = new JsonArray();
                float[] pulls = {0, 0.58F, 1.0F};
                for (int i = 0; i < 3; i++) {
                    ResourceLocation m = id.withPrefix("item/").withSuffix("_pulling_" + i);
                    saveModel(m, Json.model("minecraft:item/crossbow", "layer0", tex.withSuffix("_pulling_" + i)));
                    JsonObject pred = new JsonObject();
                    pred.addProperty("pulling", 1);
                    if (pulls[i] > 0) pred.addProperty("pull", pulls[i]);
                    overrides.add(override(pred, m));
                }
                for (String charged : new String[]{"arrow", "firework"}) {
                    ResourceLocation m = id.withPrefix("item/").withSuffix("_" + charged);
                    saveModel(m, Json.model("minecraft:item/crossbow", "layer0", tex.withSuffix("_" + charged)));
                    JsonObject pred = new JsonObject();
                    pred.addProperty("charged", 1);
                    if (charged.equals("firework")) pred.addProperty("firework", 1);
                    overrides.add(override(pred, m));
                }
                base.add("overrides", overrides);
                saveItemModel(id, base);
            }
            case FISHING_ROD -> {
                JsonObject base = Json.model("minecraft:item/handheld_rod", "layer0", tex);
                ResourceLocation cast = id.withPrefix("item/").withSuffix("_cast");
                saveModel(cast, Json.model("minecraft:item/handheld_rod", "layer0", tex.withSuffix("_cast")));
                JsonObject pred = new JsonObject();
                pred.addProperty("cast", 1);
                base.add("overrides", Json.array(override(pred, cast)));
                saveItemModel(id, base);
            }
            case SHIELD -> {
                JsonObject base = Json.parentOnly(ResourceLocation.fromNamespaceAndPath(SteelCodeCore.MOD_ID, "item/template_shield"));
                ResourceLocation blocking = id.withPrefix("item/").withSuffix("_blocking");
                saveModel(blocking, Json.parentOnly(ResourceLocation.fromNamespaceAndPath(SteelCodeCore.MOD_ID, "item/template_shield_blocking")));
                JsonObject pred = new JsonObject();
                pred.addProperty("blocking", 1);
                base.add("overrides", Json.array(override(pred, blocking)));
                saveItemModel(id, base);
            }
            case NONE -> {
            }
        }
    }

    private static JsonObject override(JsonObject predicate, ResourceLocation model) {
        JsonObject o = new JsonObject();
        o.add("predicate", predicate);
        o.addProperty("model", model.toString());
        return o;
    }

    // ================================================================ missing textures

    /** Records the textures of this mod that a model uses. */
    private void collectTextures(JsonElement json) {
        if (!json.isJsonObject()) return;
        JsonObject o = json.getAsJsonObject();
        if (o.has("textures") && o.get("textures").isJsonObject()) {
            for (var t : o.getAsJsonObject("textures").entrySet()) {
                String v = t.getValue().getAsString();
                if (v.startsWith("#")) continue;
                ResourceLocation rl = ResourceLocation.parse(v);
                if (rl.getNamespace().equals(registry.modId())) {
                    textures.add("assets/" + rl.getNamespace() + "/textures/" + rl.getPath() + ".png");
                }
            }
        }
    }

    private void checkTextures() {
        Set<String> all = new java.util.TreeSet<>(textures);
        all.addAll(registry.expectedTextures());
        java.util.Optional<dev.architectury.platform.Mod> mod;
        try {
            mod = dev.architectury.platform.Platform.getOptionalMod(registry.modId());
        } catch (Throwable t) {
            return;
        }
        if (mod.isEmpty()) return;
        List<String> missing = new ArrayList<>();
        for (String path : all) {
            if (mod.get().findResource(path.split("/")).isEmpty()) missing.add(path);
        }
        if (!missing.isEmpty()) {
            SteelCodeCore.LOGGER.warn("[{}] {} textures are missing (they will render magenta and black):\n  {}", registry.modId(),
                    missing.size(), String.join("\n  ", missing));
        } else {
            SteelCodeCore.LOGGER.info("[{}] All {} textures are present", registry.modId(), all.size());
        }
    }

    // ================================================================ worldgen, composting, languages

    private void writeWorldgen() {
        for (FeatureEntry f : registry.features()) {
            save(f.configured().get(), configuredFeatures.json(f.id()));
            if (f.placed() != null) save(f.placed().get(), placedFeatures.json(f.id()));
        }
    }

    private void writeDataMap(String name, String field, Map<RegistrySupplier<net.minecraft.world.level.block.Block>,
            RegistrySupplier<net.minecraft.world.level.block.Block>> pairs) {
        if (pairs.isEmpty()) return;
        JsonObject values = new JsonObject();
        pairs.forEach((a, b) -> {
            JsonObject v = new JsonObject();
            v.addProperty(field, b.getId().toString());
            values.add(a.getId().toString(), v);
        });
        JsonObject json = new JsonObject();
        json.add("values", values);
        save(json, output.getOutputFolder(PackOutput.Target.DATA_PACK).resolve("neoforge/data_maps/block/" + name + ".json"));
    }

    /** NeoForge reads composting from a data map; Fabric registers it in code and ignores this file. */
    private void writeCompostables() {
        if (registry.compostables().isEmpty()) return;
        JsonObject values = new JsonObject();
        registry.compostables().forEach((item, chance) -> {
            JsonObject v = new JsonObject();
            v.addProperty("chance", chance);
            values.add(item.getId().toString(), v);
        });
        JsonObject json = new JsonObject();
        json.add("values", values);
        save(json, output.getOutputFolder(PackOutput.Target.DATA_PACK)
                .resolve("neoforge/data_maps/item/compostables.json"));
    }

    private void writeLang() {
        for (Map.Entry<String, Lang> entry : registry.languages().entrySet()) {
            Lang language = entry.getValue();
            Set<String> missing = new java.util.TreeSet<>();
            JsonObject json = new JsonObject();
            java.util.Map<String, String> sorted = new java.util.TreeMap<>();
            for (RegistryContext.NameEntry name : registry.names()) {
                sorted.put(name.key(), language.resolve(name, missing));
            }
            if (sorted.isEmpty()) continue;
            sorted.forEach(json::addProperty);
            save(json, lang.json(ResourceLocation.fromNamespaceAndPath(registry.modId(), entry.getKey())));
            if (!missing.isEmpty()) {
                SteelCodeCore.LOGGER.warn("[{}] {}: no translation for {} -> REG.lang(\"{}\").words(...)",
                        registry.modId(), entry.getKey(), missing, entry.getKey());
            }
        }
    }

    /**
     * Connected textures: a cube with the borderless texture plus, for each direction without an identical neighbour,
     * border strips on the 4 faces touching that edge (automatic UV picks exactly that strip of the bordered texture).
     */
    private void connected(ResourceLocation id, ResourceLocation border) {
        ResourceLocation base = blockModel(id, "");
        JsonObject cube = new JsonObject();
        cube.addProperty("parent", "minecraft:block/block");
        JsonObject tex = new JsonObject();
        tex.addProperty("center", border.withSuffix("_center").toString());
        tex.addProperty("particle", border.toString());
        cube.add("textures", tex);
        cube.add("elements", Json.array(element(new float[]{0, 0, 0}, new float[]{16, 16, 16},
                new String[]{"north", "south", "east", "west", "up", "down"}, "#center")));
        saveModel(base, cube);
        saveModel(blockModel(id, "_item"), Json.model("minecraft:block/cube_all", "all", border));

        float o = 0.005F, lo = -o, hi = 16 + o;
        java.util.Map<String, JsonArray> strips = new java.util.LinkedHashMap<>();
        // neighbour direction -> strips (from, to, face)
        strips.put("up", Json.array(element(new float[]{0, 15, lo}, new float[]{16, 16, lo}, new String[]{"north"}, "#border"),
                element(new float[]{0, 15, hi}, new float[]{16, 16, hi}, new String[]{"south"}, "#border"),
                element(new float[]{lo, 15, 0}, new float[]{lo, 16, 16}, new String[]{"west"}, "#border"),
                element(new float[]{hi, 15, 0}, new float[]{hi, 16, 16}, new String[]{"east"}, "#border")));
        strips.put("down", Json.array(element(new float[]{0, 0, lo}, new float[]{16, 1, lo}, new String[]{"north"}, "#border"),
                element(new float[]{0, 0, hi}, new float[]{16, 1, hi}, new String[]{"south"}, "#border"),
                element(new float[]{lo, 0, 0}, new float[]{lo, 1, 16}, new String[]{"west"}, "#border"),
                element(new float[]{hi, 0, 0}, new float[]{hi, 1, 16}, new String[]{"east"}, "#border")));
        strips.put("north", Json.array(element(new float[]{0, hi, 0}, new float[]{16, hi, 1}, new String[]{"up"}, "#border"),
                element(new float[]{0, lo, 0}, new float[]{16, lo, 1}, new String[]{"down"}, "#border"),
                element(new float[]{lo, 0, 0}, new float[]{lo, 16, 1}, new String[]{"west"}, "#border"),
                element(new float[]{hi, 0, 0}, new float[]{hi, 16, 1}, new String[]{"east"}, "#border")));
        strips.put("south", Json.array(element(new float[]{0, hi, 15}, new float[]{16, hi, 16}, new String[]{"up"}, "#border"),
                element(new float[]{0, lo, 15}, new float[]{16, lo, 16}, new String[]{"down"}, "#border"),
                element(new float[]{lo, 0, 15}, new float[]{lo, 16, 16}, new String[]{"west"}, "#border"),
                element(new float[]{hi, 0, 15}, new float[]{hi, 16, 16}, new String[]{"east"}, "#border")));
        strips.put("west", Json.array(element(new float[]{0, hi, 0}, new float[]{1, hi, 16}, new String[]{"up"}, "#border"),
                element(new float[]{0, lo, 0}, new float[]{1, lo, 16}, new String[]{"down"}, "#border"),
                element(new float[]{0, 0, lo}, new float[]{1, 16, lo}, new String[]{"north"}, "#border"),
                element(new float[]{0, 0, hi}, new float[]{1, 16, hi}, new String[]{"south"}, "#border")));
        strips.put("east", Json.array(element(new float[]{15, hi, 0}, new float[]{16, hi, 16}, new String[]{"up"}, "#border"),
                element(new float[]{15, lo, 0}, new float[]{16, lo, 16}, new String[]{"down"}, "#border"),
                element(new float[]{15, 0, lo}, new float[]{16, 16, lo}, new String[]{"north"}, "#border"),
                element(new float[]{15, 0, hi}, new float[]{16, 16, hi}, new String[]{"south"}, "#border")));
        Json.Multipart mp = new Json.Multipart().always(Json.ref(base));
        strips.forEach((dir, elements) -> {
            JsonObject m = new JsonObject();
            JsonObject t = new JsonObject();
            t.addProperty("border", border.toString());
            t.addProperty("particle", border.toString());
            m.add("textures", t);
            m.add("elements", elements);
            ResourceLocation stripModel = blockModel(id, "_edge_" + dir);
            saveModel(stripModel, m);
            mp.when(dir, "false", Json.ref(stripModel));
        });
        saveBlockstate(id, mp.build());
        saveItemModel(id, Json.parentOnly(blockModel(id, "_item")));
    }

    /** An element with faces in the given directions (automatic UV, cullface on its own face). */
    private static JsonObject element(float[] from, float[] to, String[] faces, String texture) {
        JsonObject el = new JsonObject();
        JsonArray f = new JsonArray();
        for (float v : from) f.add(v);
        JsonArray t = new JsonArray();
        for (float v : to) t.add(v);
        el.add("from", f);
        el.add("to", t);
        JsonObject fs = new JsonObject();
        for (String face : faces) {
            JsonObject fc = new JsonObject();
            fc.addProperty("texture", texture);
            fc.addProperty("cullface", face);
            fs.add(face, fc);
        }
        el.add("faces", fs);
        return el;
    }

    private void pillar(ResourceLocation id, Textures t) {
        ResourceLocation vertical = blockModel(id, "");
        ResourceLocation horizontal = blockModel(id, "_horizontal");
        saveModel(vertical, Json.model("minecraft:block/cube_column", "end", t.top(), "side", t.side()));
        saveModel(horizontal, Json.model("minecraft:block/cube_column_horizontal", "end", t.top(), "side", t.side()));
        saveBlockstate(id, new Json.Variants()
                .add("axis=x", Json.ref(horizontal, 90, 90, false))
                .add("axis=y", Json.ref(vertical))
                .add("axis=z", Json.ref(horizontal, 90, 0, false))
                .build());
        saveItemModel(id, Json.parentOnly(vertical));
    }

    private void stairs(ResourceLocation id, Textures t) {
        ResourceLocation straight = blockModel(id, "");
        ResourceLocation inner = blockModel(id, "_inner");
        ResourceLocation outer = blockModel(id, "_outer");
        Object[] tex = {"bottom", t.bottom(), "top", t.top(), "side", t.side()};
        saveModel(straight, Json.model("minecraft:block/stairs", tex));
        saveModel(inner, Json.model("minecraft:block/inner_stairs", tex));
        saveModel(outer, Json.model("minecraft:block/outer_stairs", tex));

        Json.Variants states = new Json.Variants();
        for (int f = 0; f < 4; f++) {
            for (String half : new String[]{"bottom", "top"}) {
                boolean top = half.equals("top");
                for (String shape : new String[]{"straight", "inner_left", "inner_right", "outer_left", "outer_right"}) {
                    ResourceLocation m = shape.equals("straight") ? straight : shape.startsWith("inner") ? inner : outer;
                    int y = f * 90;
                    if (!top && shape.endsWith("_left")) y -= 90;
                    if (top && shape.endsWith("_right")) y += 90;
                    states.add("facing=" + FACINGS[f] + ",half=" + half + ",shape=" + shape,
                            Json.ref(m, top ? 180 : 0, Math.floorMod(y, 360), true));
                }
            }
        }
        saveBlockstate(id, states.build());
        saveItemModel(id, Json.parentOnly(straight));
    }

    private void slab(ResourceLocation id, Textures t, ResourceLocation full) {
        ResourceLocation bottom = blockModel(id, "");
        ResourceLocation top = blockModel(id, "_top");
        Object[] tex = {"bottom", t.bottom(), "top", t.top(), "side", t.side()};
        saveModel(bottom, Json.model("minecraft:block/slab", tex));
        saveModel(top, Json.model("minecraft:block/slab_top", tex));
        saveBlockstate(id, new Json.Variants()
                .add("type=bottom", Json.ref(bottom))
                .add("type=double", Json.ref(full))
                .add("type=top", Json.ref(top))
                .build());
        saveItemModel(id, Json.parentOnly(bottom));
    }

    private void verticalSlab(ResourceLocation id, Textures t, ResourceLocation full) {
        ResourceLocation model = blockModel(id, "");
        saveModel(model, Json.model(VERTICAL_SLAB_TEMPLATE.toString(),
                "bottom", t.bottom(), "top", t.top(), "side", t.side()));
        Json.Variants states = new Json.Variants();
        for (int f = 0; f < 4; f++) {
            int y = Math.floorMod(f * 90 + 90, 360); // north = 0 (the base model occupies the north side)
            states.add("double=false,facing=" + FACINGS[f], Json.ref(model, 0, y, true));
        }
        states.add("double=true", Json.ref(full));
        saveBlockstate(id, states.build());
        saveItemModel(id, Json.parentOnly(model));
    }

    private void wall(ResourceLocation id, ResourceLocation texture) {
        ResourceLocation post = blockModel(id, "_post");
        ResourceLocation side = blockModel(id, "_side");
        ResourceLocation tall = blockModel(id, "_side_tall");
        ResourceLocation inventory = blockModel(id, "_inventory");
        saveModel(post, Json.model("minecraft:block/template_wall_post", "wall", texture));
        saveModel(side, Json.model("minecraft:block/template_wall_side", "wall", texture));
        saveModel(tall, Json.model("minecraft:block/template_wall_side_tall", "wall", texture));
        saveModel(inventory, Json.model("minecraft:block/wall_inventory", "wall", texture));
        Json.Multipart mp = new Json.Multipart().when("up", "true", Json.ref(post));
        for (int f = 0; f < 4; f++) {
            int y = Math.floorMod(f * 90 + 90, 360); // north = 0, east = 90...
            mp.when(FACINGS[f], "low", Json.ref(side, 0, y, true));
            mp.when(FACINGS[f], "tall", Json.ref(tall, 0, y, true));
        }
        saveBlockstate(id, mp.build());
        saveItemModel(id, Json.parentOnly(inventory));
    }

    private void fence(ResourceLocation id, ResourceLocation texture) {
        ResourceLocation post = blockModel(id, "_post");
        ResourceLocation side = blockModel(id, "_side");
        ResourceLocation inventory = blockModel(id, "_inventory");
        saveModel(post, Json.model("minecraft:block/fence_post", "texture", texture));
        saveModel(side, Json.model("minecraft:block/fence_side", "texture", texture));
        saveModel(inventory, Json.model("minecraft:block/fence_inventory", "texture", texture));
        Json.Multipart mp = new Json.Multipart().always(Json.ref(post));
        for (int f = 0; f < 4; f++) {
            mp.when(FACINGS[f], "true", Json.ref(side, 0, Math.floorMod(f * 90 + 90, 360), true));
        }
        saveBlockstate(id, mp.build());
        saveItemModel(id, Json.parentOnly(inventory));
    }

    private void fenceGate(ResourceLocation id, ResourceLocation texture) {
        ResourceLocation closed = blockModel(id, "");
        ResourceLocation open = blockModel(id, "_open");
        ResourceLocation wall = blockModel(id, "_wall");
        ResourceLocation wallOpen = blockModel(id, "_wall_open");
        saveModel(closed, Json.model("minecraft:block/template_fence_gate", "texture", texture));
        saveModel(open, Json.model("minecraft:block/template_fence_gate_open", "texture", texture));
        saveModel(wall, Json.model("minecraft:block/template_fence_gate_wall", "texture", texture));
        saveModel(wallOpen, Json.model("minecraft:block/template_fence_gate_wall_open", "texture", texture));
        Json.Variants states = new Json.Variants();
        for (int f = 0; f < 4; f++) {
            int y = Math.floorMod(f * 90 - 90, 360); // south = 0, west = 90, north = 180, east = 270
            for (boolean inWall : new boolean[]{false, true}) {
                for (boolean isOpen : new boolean[]{false, true}) {
                    ResourceLocation m = inWall ? (isOpen ? wallOpen : wall) : (isOpen ? open : closed);
                    states.add("facing=" + FACINGS[f] + ",in_wall=" + inWall + ",open=" + isOpen, Json.ref(m, 0, y, true));
                }
            }
        }
        saveBlockstate(id, states.build());
        saveItemModel(id, Json.parentOnly(closed));
    }

    private void button(ResourceLocation id, ResourceLocation texture) {
        ResourceLocation up = blockModel(id, "");
        ResourceLocation pressed = blockModel(id, "_pressed");
        ResourceLocation inventory = blockModel(id, "_inventory");
        saveModel(up, Json.model("minecraft:block/button", "texture", texture));
        saveModel(pressed, Json.model("minecraft:block/button_pressed", "texture", texture));
        saveModel(inventory, Json.model("minecraft:block/button_inventory", "texture", texture));
        Json.Variants states = new Json.Variants();
        for (String face : new String[]{"ceiling", "floor", "wall"}) {
            for (int f = 0; f < 4; f++) {
                int y = Math.floorMod(f * 90 + 90, 360);
                int x = face.equals("wall") ? 90 : 0;
                if (face.equals("ceiling")) {
                    x = 180;
                    y = Math.floorMod(y + 180, 360);
                }
                for (boolean powered : new boolean[]{false, true}) {
                    states.add("face=" + face + ",facing=" + FACINGS[f] + ",powered=" + powered,
                            Json.ref(powered ? pressed : up, x, y, face.equals("wall")));
                }
            }
        }
        saveBlockstate(id, states.build());
        saveItemModel(id, Json.parentOnly(inventory));
    }

    private void pressurePlate(ResourceLocation id, ResourceLocation texture, boolean weighted) {
        ResourceLocation up = blockModel(id, "");
        ResourceLocation down = blockModel(id, "_down");
        saveModel(up, Json.model("minecraft:block/pressure_plate_up", "texture", texture));
        saveModel(down, Json.model("minecraft:block/pressure_plate_down", "texture", texture));
        Json.Variants states = new Json.Variants();
        if (weighted) {
            for (int power = 0; power <= 15; power++) states.add("power=" + power, Json.ref(power == 0 ? up : down));
        } else {
            states.add("powered=false", Json.ref(up)).add("powered=true", Json.ref(down));
        }
        saveBlockstate(id, states.build());
        saveItemModel(id, Json.parentOnly(up));
    }

    private void door(ResourceLocation id, Textures t, ResourceLocation itemTexture) {
        Json.Variants states = new Json.Variants();
        for (String half : new String[]{"lower", "upper"}) {
            for (String hinge : new String[]{"left", "right"}) {
                for (boolean open : new boolean[]{false, true}) {
                    String name = (half.equals("lower") ? "bottom" : "top") + "_" + hinge + (open ? "_open" : "");
                    ResourceLocation model = blockModel(id, "_" + name);
                    saveModel(model, Json.model("minecraft:block/door_" + name, "bottom", t.bottom(), "top", t.top()));
                    for (int f = 0; f < 4; f++) {
                        int y = f * 90;
                        if (open) y += hinge.equals("left") ? 90 : 270;
                        states.add("facing=" + FACINGS[f] + ",half=" + half + ",hinge=" + hinge + ",open=" + open,
                                Json.ref(model, 0, Math.floorMod(y, 360), false));
                    }
                }
            }
        }
        saveBlockstate(id, states.build());
        saveItemModel(id, Json.model("minecraft:item/generated", "layer0", itemTexture));
    }

    private void trapdoor(ResourceLocation id, ResourceLocation texture) {
        ResourceLocation bottom = blockModel(id, "_bottom");
        ResourceLocation top = blockModel(id, "_top");
        ResourceLocation open = blockModel(id, "_open");
        saveModel(bottom, Json.model("minecraft:block/template_trapdoor_bottom", "texture", texture));
        saveModel(top, Json.model("minecraft:block/template_trapdoor_top", "texture", texture));
        saveModel(open, Json.model("minecraft:block/template_trapdoor_open", "texture", texture));
        Json.Variants states = new Json.Variants();
        for (int f = 0; f < 4; f++) {
            for (String half : new String[]{"bottom", "top"}) {
                for (boolean isOpen : new boolean[]{false, true}) {
                    JsonObject ref = isOpen ? Json.ref(open, 0, Math.floorMod(f * 90 + 90, 360), false)
                            : Json.ref(half.equals("top") ? top : bottom);
                    states.add("facing=" + FACINGS[f] + ",half=" + half + ",open=" + isOpen, ref);
                }
            }
        }
        saveBlockstate(id, states.build());
        saveItemModel(id, Json.parentOnly(bottom));
    }

    private void pane(ResourceLocation id, ResourceLocation texture, ResourceLocation itemTexture) {
        String[] parts = {"post", "side", "side_alt", "noside", "noside_alt"};
        for (String p : parts) {
            saveModel(blockModel(id, "_" + p), Json.model("minecraft:block/template_glass_pane_" + p,
                    "pane", texture, "edge", texture));
        }
        saveBlockstate(id, new Json.Multipart()
                .always(Json.ref(blockModel(id, "_post")))
                .when("north", "true", Json.ref(blockModel(id, "_side")))
                .when("east", "true", Json.ref(blockModel(id, "_side"), 0, 90, false))
                .when("south", "true", Json.ref(blockModel(id, "_side_alt")))
                .when("west", "true", Json.ref(blockModel(id, "_side_alt"), 0, 90, false))
                .when("north", "false", Json.ref(blockModel(id, "_noside")))
                .when("east", "false", Json.ref(blockModel(id, "_noside_alt")))
                .when("south", "false", Json.ref(blockModel(id, "_noside_alt"), 0, 90, false))
                .when("west", "false", Json.ref(blockModel(id, "_noside"), 0, 270, false))
                .build());
        saveItemModel(id, Json.model("minecraft:item/generated", "layer0", itemTexture));
    }

    // ================================================================ recipes

    private void writeRecipes() {
        // Yours first, then automatic ones: on collision, yours win.
        List<RecipeEntry> ordered = new ArrayList<>(registry.recipeEntries());
        ordered.sort(Comparator.comparing(RecipeEntry::automatic));
        Set<String> signatures = new HashSet<>();
        Set<ResourceLocation> ids = new HashSet<>();

        for (RecipeEntry recipe : ordered) {
            JsonObject json = recipe.json().get();
            String signature = signature(json);
            if (!ids.add(recipe.id())) {
                // Automatic ones may repeat (fallback + stonecutter); yours trigger a warning.
                if (!recipe.automatic()) {
                    SteelCodeCore.LOGGER.warn("[{}] Duplicate recipe {}; ignored", registry.modId(), recipe.id());
                }
                continue;
            }
            if (signature != null && !signatures.add(signature)) {
                if (recipe.automatic()) {
                    if (recipe.fallbackFrom() != null) {
                        ResourceLocation cutId = recipe.id().withSuffix("_from_"
                                + Refs.id(recipe.fallbackFrom()).getPath() + "_stonecutting");
                        SteelCodeCore.LOGGER.info("[{}] Recipe {} collides with an identical one; replaced by a stonecutter recipe",
                                registry.modId(), recipe.id());
                        if (ids.add(cutId)) {
                            save(Recipes.stonecuttingJson(recipe.fallbackFrom(), recipe.result(), recipe.fallbackCount()),
                                    recipes.json(cutId));
                            save(advancement(cutId, recipe.fallbackFrom()),
                                    advancements.json(cutId.withPrefix("recipes/building_blocks/")));
                        }
                    } else {
                        SteelCodeCore.LOGGER.info("[{}] Recipe {} collides with an identical one; skipped", registry.modId(), recipe.id());
                    }
                    continue;
                }
                SteelCodeCore.LOGGER.warn("[{}] Recipe {} has the same pattern as another one", registry.modId(), recipe.id());
            }
            save(json, recipes.json(recipe.id()));
            if (recipe.unlockedBy() != null) {
                save(advancement(recipe.id(), recipe.unlockedBy()),
                        advancements.json(recipe.id().withPrefix("recipes/" + recipe.bookCategory() + "/")));
            }
        }
    }

    /** What makes two recipes "the same" for the game; {@code null} if it cannot collide. */
    private static String signature(JsonObject r) {
        String type = r.get("type").getAsString();
        switch (type) {
            case "minecraft:crafting_shaped" -> {
                JsonObject key = r.getAsJsonObject("key");
                StringBuilder sb = new StringBuilder("shaped");
                for (JsonElement row : r.getAsJsonArray("pattern")) {
                    sb.append('|');
                    for (char c : row.getAsString().toCharArray()) {
                        sb.append(c == ' ' ? "_" : key.get(String.valueOf(c)).toString()).append(',');
                    }
                }
                return sb.toString();
            }
            case "minecraft:crafting_shapeless" -> {
                List<String> ings = new ArrayList<>();
                r.getAsJsonArray("ingredients").forEach(i -> ings.add(i.toString()));
                ings.sort(null);
                return "shapeless|" + ings;
            }
            case "minecraft:smelting", "minecraft:blasting", "minecraft:smoking", "minecraft:campfire_cooking" -> {
                return type + "|" + r.get("ingredient");
            }
            default -> {
                return null;
            }
        }
    }

    private static JsonObject advancement(ResourceLocation recipe, Object unlockedBy) {
        JsonObject itemPredicate = new JsonObject();
        itemPredicate.addProperty("items", Refs.entry(unlockedBy));
        JsonObject hasItemConditions = new JsonObject();
        hasItemConditions.add("items", Json.array(itemPredicate));
        JsonObject hasItem = new JsonObject();
        hasItem.addProperty("trigger", "minecraft:inventory_changed");
        hasItem.add("conditions", hasItemConditions);

        JsonObject recipeConditions = new JsonObject();
        recipeConditions.addProperty("recipe", recipe.toString());
        JsonObject hasRecipe = new JsonObject();
        hasRecipe.addProperty("trigger", "minecraft:recipe_unlocked");
        hasRecipe.add("conditions", recipeConditions);

        JsonObject criteria = new JsonObject();
        criteria.add("has_item", hasItem);
        criteria.add("has_the_recipe", hasRecipe);

        JsonArray requirement = new JsonArray();
        requirement.add("has_the_recipe");
        requirement.add("has_item");
        JsonArray requirements = new JsonArray();
        requirements.add(requirement);

        JsonArray rewardRecipes = new JsonArray();
        rewardRecipes.add(recipe.toString());
        JsonObject rewards = new JsonObject();
        rewards.add("recipes", rewardRecipes);

        JsonObject adv = new JsonObject();
        adv.addProperty("parent", "minecraft:recipes/root");
        adv.add("criteria", criteria);
        adv.add("requirements", requirements);
        adv.add("rewards", rewards);
        return adv;
    }

    // ================================================================ utilities

    private void writeTags(PackOutput.PathProvider provider, Map<ResourceLocation, Set<String>> tags) {
        tags.forEach((tag, entries) -> {
            JsonObject json = new JsonObject();
            json.addProperty("replace", false);
            JsonArray values = new JsonArray();
            entries.forEach(values::add);
            json.add("values", values);
            save(json, provider.json(tag));
        });
    }

    private static ResourceLocation blockModel(ResourceLocation id, String suffix) {
        return ResourceLocation.fromNamespaceAndPath(id.getNamespace(), "block/" + id.getPath() + suffix);
    }

    private void saveModel(ResourceLocation model, JsonElement json) {
        collectTextures(json);
        save(json, models.json(model));
    }

    private void saveItemModel(ResourceLocation id, JsonElement json) {
        var override = itemOverrides.get(id);
        if (override != null) {
            json = override.texture() == null ? Json.parentOnly(override.parent())
                    : Json.model(override.parent().toString(), "layer0", override.texture());
        }
        collectTextures(json);
        save(json, models.json(id.withPrefix("item/")));
    }

    private void saveBlockstate(ResourceLocation id, JsonElement json) {
        save(json, blockstates.json(id));
    }

    private void save(JsonElement json, Path path) {
        pending.add(DataProvider.saveStable(cache, json, path));
    }
}
