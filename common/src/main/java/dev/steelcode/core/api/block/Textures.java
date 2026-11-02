// SPDX-License-Identifier: LGPL-3.0-or-later
package dev.steelcode.core.api.block;

import dev.steelcode.core.impl.util.Refs;
import net.minecraft.resources.ResourceLocation;

/**
 * The textures of a block. Usually all three are the same; pillars, logs or quartz use separate side, top and
 * bottom textures.
 * @param side   side texture id
 * @param top    top texture id
 * @param bottom bottom texture id
 */
public record Textures(ResourceLocation side, ResourceLocation top, ResourceLocation bottom) {

    /**
     * @param texture the texture for every face
     * @return uniform textures
     */
    public static Textures all(ResourceLocation texture) {
        return new Textures(texture, texture, texture);
    }

    /**
     * @param namespace namespace for paths without one
     * @param side      side texture
     * @param top       top texture
     * @param bottom    bottom texture
     * @return the textures
     */
    public static Textures of(String namespace, String side, String top, String bottom) {
        return new Textures(Refs.parse(namespace, side), Refs.parse(namespace, top), Refs.parse(namespace, bottom));
    }

    /**
     * @param id a block id
     * @return its conventional texture, {@code <namespace>:block/<path>}
     */
    public static ResourceLocation blockTexture(ResourceLocation id) {
        return id.withPrefix("block/");
    }

    /**
     * @return whether all three faces use the same texture
     */
    public boolean isUniform() {
        return side.equals(top) && side.equals(bottom);
    }
}
