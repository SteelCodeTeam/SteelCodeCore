// SPDX-License-Identifier: LGPL-3.0-or-later
package dev.steelcode.core.impl.util;

import com.google.gson.JsonObject;
import dev.architectury.registry.registries.DeferredSupplier;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.ItemLike;
import org.jetbrains.annotations.ApiStatus;

import java.util.function.Supplier;

/**
 * "Flexible" item references: recipes, drops and tags accept whatever you have at hand.
 * <ul>
 *     <li>a {@code RegistrySupplier} / {@code DeferredSupplier} of your mod</li>
 *     <li>{@code Items.IRON_INGOT}, {@code Blocks.STONE} (any {@code ItemLike})</li>
 *     <li>a {@code ResourceLocation} or {@code String} ("minecraft:stick"; no namespace = minecraft)</li>
 *     <li>a {@code TagKey} or a String starting with {@code #} ("#c:ingots/tin") for a tag</li>
 * </ul>
 * They are resolved lazily (at datagen), so you can reference things that are not registered yet.
 */
@ApiStatus.Internal
public final class Refs {

    private Refs() {
    }

    public static boolean isTag(Object ref) {
        return ref instanceof TagKey<?> || (ref instanceof String s && s.startsWith("#"));
    }

    /** The item or tag id (without '#'). */
    public static ResourceLocation id(Object ref) {
        if (ref instanceof DeferredSupplier<?> d) return d.getId();
        if (ref instanceof ResourceLocation rl) return rl;
        if (ref instanceof TagKey<?> tag) return tag.location();
        if (ref instanceof String s) return parse("minecraft", s.startsWith("#") ? s.substring(1) : s);
        if (ref instanceof ItemLike item) return BuiltInRegistries.ITEM.getKey(item.asItem());
        if (ref instanceof Supplier<?> sup && sup.get() instanceof ItemLike item) {
            return BuiltInRegistries.ITEM.getKey(item.asItem());
        }
        throw new IllegalArgumentException("Cannot convert to an item or tag: " + ref);
    }

    /** A runtime ingredient (for repair materials). Call it at game time, not at registration. */
    public static net.minecraft.world.item.crafting.Ingredient runtimeIngredient(Object ref) {
        if (isTag(ref)) {
            return net.minecraft.world.item.crafting.Ingredient.of(
                    TagKey.create(net.minecraft.core.registries.Registries.ITEM, id(ref)));
        }
        if (ref instanceof Supplier<?> sup && sup.get() instanceof ItemLike item) {
            return net.minecraft.world.item.crafting.Ingredient.of(item);
        }
        if (ref instanceof ItemLike item) return net.minecraft.world.item.crafting.Ingredient.of(item);
        return net.minecraft.world.item.crafting.Ingredient.of(BuiltInRegistries.ITEM.get(id(ref)));
    }

    /** {@code {"item": "..."}} or {@code {"tag": "..."}}. */
    public static JsonObject ingredient(Object ref) {
        JsonObject o = new JsonObject();
        o.addProperty(isTag(ref) ? "tag" : "item", id(ref).toString());
        return o;
    }

    /** "ns:id" or "#ns:tag" (tag and item predicate format). */
    public static String entry(Object ref) {
        return (isTag(ref) ? "#" : "") + id(ref);
    }

    public static ResourceLocation parse(String defaultNamespace, String value) {
        return value.contains(":") ? ResourceLocation.parse(value)
                : ResourceLocation.fromNamespaceAndPath(defaultNamespace, value);
    }

    /** gold_stairs -> "Gold Stairs". */
    public static String titleCase(String path) {
        StringBuilder sb = new StringBuilder();
        for (String word : path.split("[_/]")) {
            if (word.isEmpty()) continue;
            if (!sb.isEmpty()) sb.append(' ');
            sb.append(Character.toUpperCase(word.charAt(0))).append(word.substring(1));
        }
        return sb.toString();
    }
}
