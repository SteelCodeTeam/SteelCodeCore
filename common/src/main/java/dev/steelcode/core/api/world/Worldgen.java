// SPDX-License-Identifier: LGPL-3.0-or-later
package dev.steelcode.core.api.world;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import dev.architectury.registry.level.biome.BiomeModifications;
import dev.steelcode.core.impl.registry.FeatureEntry;
import dev.steelcode.core.impl.registry.RegistryContext;
import dev.steelcode.core.impl.util.Refs;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.levelgen.GenerationStep;
import org.jetbrains.annotations.ApiStatus;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * World generation: ores, stone blobs, boulders, disks and geodes. Datagen writes the configured and placed
 * feature JSON; at runtime the features are injected into biomes on both Fabric and NeoForge.
 * Obtain it with {@code REG.worldgen()}.
 * <pre>{@code
 * // An ore like iron: 10 veins of 9 blocks per chunk, most common around y=16
 * REG.worldgen().ore("ore_tin")
 *         .replace(OreTarget.STONE, TIN_ORE).replace(OreTarget.DEEPSLATE, DEEPSLATE_TIN_ORE)
 *         .size(9).count(10).trapezoid(-24, 56)
 *         .build();
 *
 * // A blob like granite: pockets of 64 blocks, one every 6 chunks, between y=0 and y=60
 * REG.worldgen().patch("marble_patch", MARBLE).size(64).rarity(6).uniform(0, 60).build();
 *
 * // Surface boulders, river disks and a geode
 * REG.worldgen().rock("mossy_boulder", Blocks.MOSSY_COBBLESTONE).build();
 * REG.worldgen().disk("jade_disk", JADE).build();
 * REG.worldgen().geode("jade_geode").inner(JADE).rarity(10).build();
 * }</pre>
 * Every feature can be switched off from your config with {@code .enabledIf(CONFIG.value::get)}.
 */
public final class Worldgen {

    /** Which blocks an ore replaces. */
    public enum OreTarget {
        /** Stone ({@code minecraft:stone_ore_replaceables}). */
        STONE("minecraft:stone_ore_replaceables"),
        /** Deepslate ({@code minecraft:deepslate_ore_replaceables}). */
        DEEPSLATE("minecraft:deepslate_ore_replaceables"),
        /** Stone, granite, diorite, andesite, tuff and deepslate: what granite-like blobs use. */
        ANY_OVERWORLD_STONE("minecraft:base_stone_overworld"),
        /** Netherrack, basalt and blackstone ({@code minecraft:base_stone_nether}). */
        NETHER("minecraft:base_stone_nether"),
        /** End stone. */
        END("minecraft:end_stone");

        private final String tag;

        OreTarget(String tag) {
            this.tag = tag;
        }

        JsonObject json() {
            JsonObject o = new JsonObject();
            if (this == END) {
                o.addProperty("predicate_type", "minecraft:block_match");
                o.addProperty("block", tag);
            } else {
                o.addProperty("predicate_type", "minecraft:tag_match");
                o.addProperty("tag", tag);
            }
            return o;
        }
    }

    private final RegistryContext registry;

    /** Internal: use {@code REG.worldgen()}. */
    @ApiStatus.Internal
    public Worldgen(RegistryContext registry) {
        this.registry = registry;
    }

    /**
     * Starts an ore. Choose what it replaces with {@link OreBuilder#replace}.
     * @param name feature name, e.g. {@code "ore_tin"}
     * @return a new ore builder
     */
    public OreBuilder ore(String name) {
        return new OreBuilder(name);
    }

    /**
     * Starts a large blob of a block in overworld stone (like granite, diorite or tuff).
     * @param name  feature name
     * @param block the block: item-like, supplier or id
     * @return a new builder, preconfigured for blobs
     */
    public OreBuilder patch(String name, Object block) {
        return new OreBuilder(name).replace(OreTarget.ANY_OVERWORLD_STONE, block).size(64).rarity(6).uniform(0, 60);
    }

    /** Internal: injects the features into biomes (called by {@code register()}). */
    @ApiStatus.Internal
    public void inject() {
        for (FeatureEntry f : registry.features()) {
            if (f.placed() == null || f.biomes() == null || !f.enabled().getAsBoolean()) continue;
            TagKey<net.minecraft.world.level.biome.Biome> biomes = TagKey.create(Registries.BIOME, f.biomes());
            var key = ResourceKey.create(Registries.PLACED_FEATURE, f.id());
            BiomeModifications.addProperties(ctx -> ctx.hasTag(biomes),
                    (ctx, props) -> props.getGenerationProperties().addFeature(f.step(), key));
        }
    }

    // ================================================================ ore / blob builder

    /**
     * Configures an ore or blob. Defaults are iron-like: veins of 9, 10 per chunk, trapezoid between y=-24 and y=56,
     * overworld biomes. {@link Worldgen#patch} presets size 64, one every 6 chunks, uniform between y=0 and y=60.
     */
    public final class OreBuilder {
        private final String name;
        private final List<Object[]> targets = new ArrayList<>();
        private int size = 9;
        private int count = 10;
        private int rarity;
        private String heightType = "minecraft:trapezoid";
        private int minY = -24;
        private int maxY = 56;
        private float airExposure;
        private ResourceLocation biomes = ResourceLocation.withDefaultNamespace("is_overworld");
        private java.util.function.BooleanSupplier enabled = () -> true;

        private OreBuilder(String name) {
            this.name = Objects.requireNonNull(name);
        }

        /**
         * Replaces {@code target} with {@code block}. Call it several times (stone + deepslate).
         * @param target what to replace
         * @param block  the ore block
         * @return this builder
         */
        public OreBuilder replace(OreTarget target, Object block) {
            targets.add(new Object[]{target.json(), block});
            return this;
        }

        /**
         * Replaces the blocks of any tag.
         * @param blockTag tag id, e.g. {@code "minecraft:sand"}
         * @param block    the ore block
         * @return this builder
         */
        public OreBuilder replaceTag(String blockTag, Object block) {
            JsonObject o = new JsonObject();
            o.addProperty("predicate_type", "minecraft:tag_match");
            o.addProperty("tag", Refs.parse("minecraft", blockTag).toString());
            targets.add(new Object[]{o, block});
            return this;
        }

        /**
         * @param size blocks per vein (iron 9, diamond 4-8, granite 64)
         * @return this builder
         */
        public OreBuilder size(int size) {
            this.size = size;
            return this;
        }

        /**
         * @param count veins per chunk
         * @return this builder
         */
        public OreBuilder count(int count) {
            this.count = count;
            this.rarity = 0;
            return this;
        }

        /**
         * One vein every {@code chunks} chunks, for rare ores or large blobs (replaces {@link #count}).
         * @param chunks average chunks between veins
         * @return this builder
         */
        public OreBuilder rarity(int chunks) {
            this.rarity = chunks;
            return this;
        }

        /**
         * Spreads the veins evenly between two heights.
         * @param minY lowest height
         * @param maxY highest height
         * @return this builder
         */
        public OreBuilder uniform(int minY, int maxY) {
            return height("minecraft:uniform", minY, maxY);
        }

        /**
         * Triangular distribution: most common at the midpoint (like iron or copper).
         * @param minY lowest height
         * @param maxY highest height
         * @return this builder
         */
        public OreBuilder trapezoid(int minY, int maxY) {
            return height("minecraft:trapezoid", minY, maxY);
        }

        private OreBuilder height(String type, int min, int max) {
            this.heightType = type;
            this.minY = min;
            this.maxY = max;
            return this;
        }

        /**
         * @param chance probability 0-1 of discarding blocks exposed to air (diamond 0.5, ancient debris 1)
         * @return this builder
         */
        public OreBuilder airExposure(float chance) {
            this.airExposure = chance;
            return this;
        }

        /**
         * @param biomeTag biome tag, e.g. {@code "minecraft:is_overworld"}, {@code "minecraft:is_nether"}, {@code "c:is_mountain"}
         * @return this builder
         */
        public OreBuilder biomes(String biomeTag) {
            this.biomes = Refs.parse("minecraft", biomeTag.startsWith("#") ? biomeTag.substring(1) : biomeTag);
            return this;
        }

        /**
         * Only generates when the condition is true at server start.
         * <pre>{@code .enabledIf(CONFIG_TIN_ORE::get)}</pre>
         * @param condition checked when biomes are modified
         * @return this builder
         */
        public OreBuilder enabledIf(java.util.function.BooleanSupplier condition) {
            this.enabled = Objects.requireNonNull(condition);
            return this;
        }

        /**
         * Queues the configured and placed features and their biome injection.
         * @return the placed feature id
         */
        public ResourceLocation build() {
            if (targets.isEmpty()) throw new IllegalStateException("Ore '" + name + "' replaces nothing: call replace(...)");
            ResourceLocation id = ResourceLocation.fromNamespaceAndPath(registry.modId(), name);
            List<Object[]> t = List.copyOf(targets);
            int s = size, c = count, r = rarity, min = minY, max = maxY;
            float air = airExposure;
            String ht = heightType;

            registry.addFeature(new FeatureEntry(id, () -> {
                JsonArray arr = new JsonArray();
                for (Object[] target : t) {
                    JsonObject state = new JsonObject();
                    state.addProperty("Name", Refs.id(target[1]).toString());
                    JsonObject entry = new JsonObject();
                    entry.add("state", state);
                    entry.add("target", (JsonObject) target[0]);
                    arr.add(entry);
                }
                JsonObject config = new JsonObject();
                config.addProperty("discard_chance_on_air_exposure", air);
                config.addProperty("size", s);
                config.add("targets", arr);
                JsonObject feature = new JsonObject();
                feature.addProperty("type", "minecraft:ore");
                feature.add("config", config);
                return feature;
            }, () -> {
                JsonArray placement = new JsonArray();
                placement.add(r > 0 ? modifier("minecraft:rarity_filter", "chance", r) : modifier("minecraft:count", "count", c));
                placement.add(modifier("minecraft:in_square", null, 0));
                JsonObject height = new JsonObject();
                height.addProperty("type", ht);
                height.add("min_inclusive", absolute(min));
                height.add("max_inclusive", absolute(max));
                JsonObject range = new JsonObject();
                range.addProperty("type", "minecraft:height_range");
                range.add("height", height);
                placement.add(range);
                placement.add(modifier("minecraft:biome", null, 0));
                return placed(id, placement);
            }, biomes, GenerationStep.Decoration.UNDERGROUND_ORES, enabled));
            return id;
        }
    }

    // ================================================================ rocks, disks and geodes

    /**
     * Surface boulders, like the mossy ones in giant taigas (2 per chunk in forest biomes by default).
     * @param name  feature name
     * @param block the boulder block
     * @return a new feature builder
     */
    public SimpleFeature rock(String name, Object block) {
        return new SimpleFeature(name, "rock", block).count(2).biomes("minecraft:is_forest");
    }

    /**
     * An underwater disk, like clay or gravel in rivers. Replaces dirt, clay, sand and gravel (river biomes by default).
     * @param name  feature name
     * @param block the disk block
     * @return a new feature builder
     */
    public SimpleFeature disk(String name, Object block) {
        return new SimpleFeature(name, "disk", block).count(1).biomes("minecraft:is_river");
    }

    /**
     * A geode like the amethyst one.
     * @param name feature name
     * @return a new geode builder
     */
    public GeodeBuilder geode(String name) {
        return new GeodeBuilder(name);
    }

    /** Configures a boulder or disk feature. */
    public final class SimpleFeature {
        private final String name;
        private final String kind;
        private final Object block;
        private int count = 1;
        private int rarity;
        private int radiusMin = 2;
        private int radiusMax = 3;
        private ResourceLocation biomes;
        private java.util.function.BooleanSupplier enabled = () -> true;

        private SimpleFeature(String name, String kind, Object block) {
            this.name = name;
            this.kind = kind;
            this.block = block;
        }

        /**
         * @param perChunk attempts per chunk
         * @return this builder
         */
        public SimpleFeature count(int perChunk) {
            this.count = perChunk;
            this.rarity = 0;
            return this;
        }

        /**
         * @param chunks one attempt every {@code chunks} chunks (replaces {@link #count})
         * @return this builder
         */
        public SimpleFeature rarity(int chunks) {
            this.rarity = chunks;
            return this;
        }

        /**
         * Radius of disks.
         * @param min minimum radius
         * @param max maximum radius
         * @return this builder
         */
        public SimpleFeature radius(int min, int max) {
            this.radiusMin = min;
            this.radiusMax = max;
            return this;
        }

        /**
         * @param biomeTag biome tag where it generates
         * @return this builder
         */
        public SimpleFeature biomes(String biomeTag) {
            this.biomes = Refs.parse("minecraft", biomeTag.replace("#", ""));
            return this;
        }

        /**
         * @param condition only generates when true at server start
         * @return this builder
         */
        public SimpleFeature enabledIf(java.util.function.BooleanSupplier condition) {
            this.enabled = Objects.requireNonNull(condition);
            return this;
        }

        /**
         * Queues the features and their biome injection.
         * @return the placed feature id
         */
        public ResourceLocation build() {
            ResourceLocation id = ResourceLocation.fromNamespaceAndPath(registry.modId(), name);
            Object b = block;
            int c = count, r = rarity, rmin = radiusMin, rmax = radiusMax;
            boolean rock = kind.equals("rock");
            registry.addFeature(new FeatureEntry(id, () -> {
                JsonObject feature = new JsonObject();
                JsonObject config = new JsonObject();
                if (rock) {
                    feature.addProperty("type", "minecraft:forest_rock");
                    config.add("state", state(b));
                } else {
                    feature.addProperty("type", "minecraft:disk");
                    config.addProperty("half_height", 1);
                    JsonObject radius = new JsonObject();
                    radius.addProperty("type", "minecraft:uniform");
                    radius.addProperty("min_inclusive", rmin);
                    radius.addProperty("max_inclusive", rmax);
                    config.add("radius", radius);
                    JsonObject fallback = new JsonObject();
                    fallback.addProperty("type", "minecraft:simple_state_provider");
                    fallback.add("state", state(b));
                    JsonObject provider = new JsonObject();
                    provider.add("fallback", fallback);
                    provider.add("rules", new JsonArray());
                    config.add("state_provider", provider);
                    JsonObject target = new JsonObject();
                    target.addProperty("type", "minecraft:matching_blocks");
                    JsonArray blocks = new JsonArray();
                    for (String t : new String[]{"minecraft:dirt", "minecraft:clay", "minecraft:sand", "minecraft:gravel"}) blocks.add(t);
                    target.add("blocks", blocks);
                    config.add("target", target);
                }
                feature.add("config", config);
                return feature;
            }, () -> {
                JsonArray p = new JsonArray();
                if (r > 0) p.add(modifier("minecraft:rarity_filter", "chance", r));
                else if (c > 1 || rock) p.add(modifier("minecraft:count", "count", c));
                p.add(modifier("minecraft:in_square", null, 0));
                JsonObject hm = modifier("minecraft:heightmap", null, 0);
                hm.addProperty("heightmap", rock ? "MOTION_BLOCKING" : "OCEAN_FLOOR_WG");
                p.add(hm);
                if (!rock) {
                    JsonObject predicate = new JsonObject();
                    predicate.addProperty("type", "minecraft:matching_fluids");
                    predicate.addProperty("fluids", "minecraft:water");
                    JsonObject filter = modifier("minecraft:block_predicate_filter", null, 0);
                    filter.add("predicate", predicate);
                    p.add(filter);
                }
                p.add(modifier("minecraft:biome", null, 0));
                return placed(id, p);
            }, biomes, rock ? GenerationStep.Decoration.LOCAL_MODIFICATIONS : GenerationStep.Decoration.UNDERGROUND_ORES, enabled));
            return id;
        }
    }

    /**
     * A geode: copies the amethyst geode and swaps its layers.
     * <pre>{@code
     * REG.worldgen().geode("jade_geode").inner(JADE_BLOCK).budding(BUDDING_JADE)
     *         .buds(SMALL_JADE_BUD, MEDIUM_JADE_BUD, LARGE_JADE_BUD, JADE_CLUSTER).rarity(24).build();
     * }</pre>
     * Defaults: calcite middle layer, smooth basalt outer layer, one geode every 24 chunks below y=30.
     */
    public final class GeodeBuilder {
        private final String name;
        private Object inner = "minecraft:amethyst_block";
        private Object budding;
        private Object middle = "minecraft:calcite";
        private Object outer = "minecraft:smooth_basalt";
        private Object[] buds;
        private int rarity = 24;
        private int maxY = 30;
        private ResourceLocation biomes = ResourceLocation.withDefaultNamespace("is_overworld");
        private java.util.function.BooleanSupplier enabled = () -> true;

        private GeodeBuilder(String name) {
            this.name = name;
        }

        /**
         * @param block the inner layer block
         * @return this builder
         */
        public GeodeBuilder inner(Object block) { this.inner = block; return this; }
        /**
         * @param block block that grows crystals (like budding amethyst); defaults to the inner block
         * @return this builder
         */
        public GeodeBuilder budding(Object block) { this.budding = block; return this; }
        /**
         * @param block the middle layer (default calcite)
         * @return this builder
         */
        public GeodeBuilder middle(Object block) { this.middle = block; return this; }
        /**
         * @param block the outer layer (default smooth basalt)
         * @return this builder
         */
        public GeodeBuilder outer(Object block) { this.outer = block; return this; }

        /**
         * The four crystals that grow inside. Without them the geode has no crystals.
         * @param small   small bud
         * @param medium  medium bud
         * @param large   large bud
         * @param cluster cluster
         * @return this builder
         */
        public GeodeBuilder buds(Object small, Object medium, Object large, Object cluster) {
            this.buds = new Object[]{small, medium, large, cluster};
            return this;
        }

        /**
         * @param chunks one geode every {@code chunks} chunks
         * @return this builder
         */
        public GeodeBuilder rarity(int chunks) { this.rarity = chunks; return this; }
        /**
         * @param y highest height
         * @return this builder
         */
        public GeodeBuilder maxY(int y) { this.maxY = y; return this; }

        /**
         * @param biomeTag biome tag where it generates
         * @return this builder
         */
        public GeodeBuilder biomes(String biomeTag) {
            this.biomes = Refs.parse("minecraft", biomeTag.replace("#", ""));
            return this;
        }

        /**
         * @param condition only generates when true at server start
         * @return this builder
         */
        public GeodeBuilder enabledIf(java.util.function.BooleanSupplier condition) {
            this.enabled = Objects.requireNonNull(condition);
            return this;
        }

        /**
         * Queues the features and their biome injection.
         * @return the placed feature id
         */
        public ResourceLocation build() {
            ResourceLocation id = ResourceLocation.fromNamespaceAndPath(registry.modId(), name);
            Object in = inner, bud = budding != null ? budding : inner, mid = middle, out = outer;
            Object[] crystals = buds;
            int r = rarity, max = maxY;
            registry.addFeature(new FeatureEntry(id, () -> {
                JsonObject f = dev.steelcode.core.impl.datagen.VanillaData.json("worldgen/configured_feature/amethyst_geode.json",
                        "minecraft:amethyst_block", Refs.id(in).toString(),
                        "minecraft:budding_amethyst", Refs.id(bud).toString(),
                        "minecraft:calcite", Refs.id(mid).toString(),
                        "minecraft:smooth_basalt", Refs.id(out).toString());
                JsonObject blocks = f.getAsJsonObject("config").getAsJsonObject("blocks");
                JsonArray placements = new JsonArray();
                if (crystals != null) {
                    for (Object c : crystals) {
                        JsonObject st = state(c);
                        JsonObject props = new JsonObject();
                        props.addProperty("facing", "up");
                        props.addProperty("waterlogged", "false");
                        st.add("Properties", props);
                        placements.add(st);
                    }
                } else {
                    JsonObject air = new JsonObject();
                    air.addProperty("Name", "minecraft:air");
                    placements.add(air);
                }
                blocks.add("inner_placements", placements);
                return f;
            }, () -> {
                JsonObject p = dev.steelcode.core.impl.datagen.VanillaData.json("worldgen/placed_feature/amethyst_geode.json",
                        "minecraft:amethyst_geode", id.toString());
                JsonArray mods = p.getAsJsonArray("placement");
                mods.get(0).getAsJsonObject().addProperty("chance", r);
                mods.get(2).getAsJsonObject().getAsJsonObject("height").getAsJsonObject("max_inclusive").addProperty("absolute", max);
                return p;
            }, biomes, GenerationStep.Decoration.LOCAL_MODIFICATIONS, enabled));
            return id;
        }
    }

    // ================================================================ plant patches (used by PlantBuilder)

    static JsonObject patchFeature(String type, ResourceLocation block, boolean tall, int tries) {
        JsonObject st = new JsonObject();
        st.addProperty("Name", block.toString());
        if (tall) {
            JsonObject props = new JsonObject();
            props.addProperty("half", "lower");
            st.add("Properties", props);
        }
        JsonObject provider = new JsonObject();
        provider.addProperty("type", "minecraft:simple_state_provider");
        provider.add("state", st);
        JsonObject simpleConfig = new JsonObject();
        simpleConfig.add("to_place", provider);
        JsonObject simple = new JsonObject();
        simple.addProperty("type", "minecraft:simple_block");
        simple.add("config", simpleConfig);
        JsonObject predicate = new JsonObject();
        predicate.addProperty("type", "minecraft:matching_blocks");
        predicate.addProperty("blocks", "minecraft:air");
        JsonObject filter = modifier("minecraft:block_predicate_filter", null, 0);
        filter.add("predicate", predicate);
        JsonArray placement = new JsonArray();
        placement.add(filter);
        JsonObject inner = new JsonObject();
        inner.add("feature", simple);
        inner.add("placement", placement);
        JsonObject config = new JsonObject();
        config.add("feature", inner);
        config.addProperty("tries", tries);
        config.addProperty("xz_spread", 7);
        config.addProperty("y_spread", 3);
        JsonObject feature = new JsonObject();
        feature.addProperty("type", type);
        feature.add("config", config);
        return feature;
    }

    static JsonObject surfacePlacement(ResourceLocation feature, int rarity) {
        JsonArray p = new JsonArray();
        p.add(modifier("minecraft:rarity_filter", "chance", Math.max(1, rarity)));
        p.add(modifier("minecraft:in_square", null, 0));
        JsonObject hm = modifier("minecraft:heightmap", null, 0);
        hm.addProperty("heightmap", "MOTION_BLOCKING");
        p.add(hm);
        p.add(modifier("minecraft:biome", null, 0));
        return placed(feature, p);
    }

    private static JsonObject state(Object block) {
        JsonObject st = new JsonObject();
        st.addProperty("Name", Refs.id(block).toString());
        return st;
    }

    // ================================================================ JSON helpers

    static JsonObject modifier(String type, String key, int value) {
        JsonObject o = new JsonObject();
        o.addProperty("type", type);
        if (key != null) o.addProperty(key, value);
        return o;
    }

    static JsonObject absolute(int y) {
        JsonObject o = new JsonObject();
        o.addProperty("absolute", y);
        return o;
    }

    static JsonObject placed(ResourceLocation feature, JsonArray placement) {
        JsonObject o = new JsonObject();
        o.addProperty("feature", feature.toString());
        o.add("placement", placement);
        return o;
    }
}
