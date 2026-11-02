// SPDX-License-Identifier: LGPL-3.0-or-later
package dev.steelcode.core.impl.datagen;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import dev.steelcode.core.api.block.Drops;
import dev.steelcode.core.impl.util.Refs;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.ApiStatus;

import java.util.LinkedHashMap;
import java.util.Map;

/** Helpers to build model, blockstate and loot table JSON. */
@ApiStatus.Internal
final class Json {

    private Json() {
    }

    // ---------------------------------------------------------------- models

    static JsonObject model(String parent, Object... textureKeyValues) {
        JsonObject model = new JsonObject();
        model.addProperty("parent", parent);
        if (textureKeyValues.length > 0) {
            JsonObject textures = new JsonObject();
            for (int i = 0; i < textureKeyValues.length; i += 2) {
                textures.addProperty((String) textureKeyValues[i], textureKeyValues[i + 1].toString());
            }
            model.add("textures", textures);
        }
        return model;
    }

    static JsonObject parentOnly(ResourceLocation parent) {
        return model(parent.toString());
    }

    // ---------------------------------------------------------------- blockstates

    static JsonObject ref(ResourceLocation model, int x, int y, boolean uvlock) {
        JsonObject o = new JsonObject();
        o.addProperty("model", model.toString());
        if (x != 0) o.addProperty("x", x);
        if (y != 0) o.addProperty("y", y);
        if (uvlock && (x != 0 || y != 0)) o.addProperty("uvlock", true);
        return o;
    }

    static JsonObject ref(ResourceLocation model) {
        return ref(model, 0, 0, false);
    }

    static final class Variants {
        private final Map<String, JsonObject> variants = new LinkedHashMap<>();

        Variants add(String key, JsonObject model) {
            variants.put(key, model);
            return this;
        }

        JsonObject build() {
            JsonObject v = new JsonObject();
            variants.forEach(v::add);
            JsonObject root = new JsonObject();
            root.add("variants", v);
            return root;
        }
    }

    static final class Multipart {
        private final JsonArray parts = new JsonArray();

        Multipart always(JsonObject apply) {
            JsonObject part = new JsonObject();
            part.add("apply", apply);
            parts.add(part);
            return this;
        }

        Multipart when(String property, String value, JsonObject apply) {
            JsonObject when = new JsonObject();
            when.addProperty(property, value);
            JsonObject part = new JsonObject();
            part.add("when", when);
            part.add("apply", apply);
            parts.add(part);
            return this;
        }

        JsonObject build() {
            JsonObject root = new JsonObject();
            root.add("multipart", parts);
            return root;
        }
    }

    // ---------------------------------------------------------------- loot tables

    /** Generic loot table from {@link Drops}. */
    static JsonObject loot(ResourceLocation block, Drops drops) {
        Object normal = resolve(drops.withoutSilk(), block);
        Object silk = resolve(drops.withSilk(), block);
        boolean countChanges = drops.min() != 1 || drops.max() != 1 || drops.fortune();

        if (normal == null && silk == null) {
            JsonObject table = new JsonObject();
            table.addProperty("type", "minecraft:block");
            table.add("pools", new JsonArray());
            table.addProperty("random_sequence", sequence(block));
            return table;
        }
        // Same drop with and without Silk Touch and no counts: a simple drop.
        if (normal != null && normal.equals(silk) && !countChanges) {
            JsonObject pool = pool(itemEntry(normal));
            pool.add("conditions", array(condition("minecraft:survives_explosion")));
            return table(block, pool);
        }
        // Silk Touch only (glass).
        if (normal == null) {
            JsonObject pool = pool(itemEntry(silk));
            pool.add("conditions", array(silkTouch()));
            return table(block, pool);
        }
        JsonObject normalEntry = itemEntry(normal);
        JsonArray functions = new JsonArray();
        if (drops.min() != 1 || drops.max() != 1) {
            JsonObject setCount = new JsonObject();
            setCount.addProperty("function", "minecraft:set_count");
            if (drops.min() == drops.max()) {
                setCount.addProperty("count", drops.min());
            } else {
                JsonObject uniform = new JsonObject();
                uniform.addProperty("type", "minecraft:uniform");
                uniform.addProperty("min", drops.min());
                uniform.addProperty("max", drops.max());
                setCount.add("count", uniform);
            }
            setCount.addProperty("add", false);
            functions.add(setCount);
        }
        if (drops.fortune()) {
            JsonObject bonus = new JsonObject();
            bonus.addProperty("function", "minecraft:apply_bonus");
            bonus.addProperty("enchantment", "minecraft:fortune");
            bonus.addProperty("formula", "minecraft:ore_drops");
            functions.add(bonus);
        }
        functions.add(function("minecraft:explosion_decay"));
        normalEntry.add("functions", functions);

        if (silk == null) {
            // Without Silk Touch -> item; with Silk Touch -> nothing.
            JsonObject inverted = condition("minecraft:inverted");
            inverted.add("term", silkTouch());
            normalEntry.add("conditions", array(inverted));
            return table(block, pool(normalEntry));
        }
        JsonObject silkEntry = itemEntry(silk);
        silkEntry.add("conditions", array(silkTouch()));
        JsonObject alternatives = new JsonObject();
        alternatives.addProperty("type", "minecraft:alternatives");
        alternatives.add("children", array(silkEntry, normalEntry));
        return table(block, pool(alternatives));
    }

    /** Shears only (like grass, without seeds). */
    static JsonObject shearsLoot(ResourceLocation block) {
        JsonObject predicate = new JsonObject();
        predicate.addProperty("items", "minecraft:shears");
        JsonObject cond = condition("minecraft:match_tool");
        cond.add("predicate", predicate);
        JsonObject pool = pool(itemEntry(block.toString()));
        pool.add("conditions", array(cond));
        return table(block, pool);
    }

    /** A crop like wheat (separate seeds) or like carrots (seeds == null). */
    static JsonObject cropLoot(ResourceLocation crop, ResourceLocation produce, ResourceLocation seeds, int maxAge) {
        JsonObject mature = stateCondition(crop, "age", String.valueOf(maxAge));
        JsonObject bonus = function("minecraft:apply_bonus");
        bonus.addProperty("enchantment", "minecraft:fortune");
        bonus.addProperty("formula", "minecraft:binomial_with_bonus_count");
        JsonObject params = new JsonObject();
        params.addProperty("extra", 3);
        params.addProperty("probability", 0.5714286);
        bonus.add("parameters", params);
        JsonArray pools = new JsonArray();
        if (seeds != null) {
            JsonObject produceEntry = itemEntry(produce.toString());
            produceEntry.add("conditions", array(mature.deepCopy()));
            JsonObject alternatives = new JsonObject();
            alternatives.addProperty("type", "minecraft:alternatives");
            alternatives.add("children", array(produceEntry, itemEntry(seeds.toString())));
            pools.add(pool(alternatives));
            JsonObject seedEntry = itemEntry(seeds.toString());
            seedEntry.add("functions", array(bonus));
            JsonObject p2 = pool(seedEntry);
            p2.add("conditions", array(mature.deepCopy()));
            pools.add(p2);
        } else {
            pools.add(pool(itemEntry(produce.toString())));
            JsonObject extra = itemEntry(produce.toString());
            extra.add("functions", array(bonus));
            JsonObject p2 = pool(extra);
            p2.add("conditions", array(mature.deepCopy()));
            pools.add(p2);
        }
        JsonObject table = new JsonObject();
        table.addProperty("type", "minecraft:block");
        table.add("functions", array(function("minecraft:explosion_decay")));
        table.add("pools", pools);
        table.addProperty("random_sequence", sequence(crop));
        return table;
    }

    /** Slabs: a double slab drops 2. {@code property}/{@code value} identify the double state. */
    static JsonObject slabLoot(ResourceLocation block, String property, String value) {
        JsonObject setCount = function("minecraft:set_count");
        setCount.addProperty("count", 2.0);
        setCount.addProperty("add", false);
        setCount.add("conditions", array(stateCondition(block, property, value)));
        JsonObject entry = itemEntry(block.toString());
        entry.add("functions", array(setCount, function("minecraft:explosion_decay")));
        return table(block, pool(entry));
    }

    /** Doors: only the lower half drops. */
    static JsonObject doorLoot(ResourceLocation block) {
        JsonObject entry = itemEntry(block.toString());
        entry.add("conditions", array(stateCondition(block, "half", "lower")));
        JsonObject pool = pool(entry);
        pool.add("conditions", array(condition("minecraft:survives_explosion")));
        return table(block, pool);
    }

    private static Object resolve(Object drop, ResourceLocation self) {
        if (drop == null) return null;
        if (drop == Drops.SELF) return self.toString();
        return Refs.id(drop).toString();
    }

    private static JsonObject silkTouch() {
        JsonObject levels = new JsonObject();
        levels.addProperty("min", 1);
        JsonObject ench = new JsonObject();
        ench.addProperty("enchantments", "minecraft:silk_touch");
        ench.add("levels", levels);
        JsonObject predicates = new JsonObject();
        predicates.add("minecraft:enchantments", array(ench));
        JsonObject predicate = new JsonObject();
        predicate.add("predicates", predicates);
        JsonObject c = condition("minecraft:match_tool");
        c.add("predicate", predicate);
        return c;
    }

    private static JsonObject stateCondition(ResourceLocation block, String property, String value) {
        JsonObject props = new JsonObject();
        props.addProperty(property, value);
        JsonObject c = condition("minecraft:block_state_property");
        c.addProperty("block", block.toString());
        c.add("properties", props);
        return c;
    }

    private static JsonObject condition(String type) {
        JsonObject c = new JsonObject();
        c.addProperty("condition", type);
        return c;
    }

    private static JsonObject function(String type) {
        JsonObject f = new JsonObject();
        f.addProperty("function", type);
        return f;
    }

    private static JsonObject itemEntry(Object item) {
        JsonObject entry = new JsonObject();
        entry.addProperty("type", "minecraft:item");
        entry.addProperty("name", item.toString());
        return entry;
    }

    private static JsonObject pool(JsonObject entry) {
        JsonObject pool = new JsonObject();
        pool.addProperty("rolls", 1.0);
        pool.addProperty("bonus_rolls", 0.0);
        pool.add("entries", array(entry));
        return pool;
    }

    private static JsonObject table(ResourceLocation block, JsonObject pool) {
        JsonObject table = new JsonObject();
        table.addProperty("type", "minecraft:block");
        table.add("pools", array(pool));
        table.addProperty("random_sequence", sequence(block));
        return table;
    }

    private static String sequence(ResourceLocation block) {
        return block.getNamespace() + ":blocks/" + block.getPath();
    }

    static JsonArray array(JsonElement... elements) {
        JsonArray a = new JsonArray();
        for (JsonElement e : elements) a.add(e);
        return a;
    }
}
