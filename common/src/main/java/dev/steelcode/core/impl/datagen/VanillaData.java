// SPDX-License-Identifier: LGPL-3.0-or-later
package dev.steelcode.core.impl.datagen;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.server.MinecraftServer;
import org.jetbrains.annotations.ApiStatus;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

/**
 * Reads vanilla data JSON from the Minecraft jar itself (datagen only) and replaces ids, so trees and complex
 * loot tables match vanilla exactly without copying them by hand.
 */
@ApiStatus.Internal
public final class VanillaData {

    private VanillaData() {
    }

    /**
     * @param path         path inside {@code data/minecraft/}, e.g. {@code "loot_table/blocks/oak_leaves.json"}
     * @param replacements pairs {@code "minecraft:oak_log", "mymod:maple_log"} (replaced as whole ids)
     */
    public static JsonObject json(String path, String... replacements) {
        String full = "/data/minecraft/" + path;
        try (InputStream in = MinecraftServer.class.getResourceAsStream(full)) {
            if (in == null) throw new IllegalStateException("Cannot find " + full + " in the Minecraft jar");
            String text = new String(in.readAllBytes(), StandardCharsets.UTF_8);
            for (int i = 0; i < replacements.length; i += 2) {
                text = text.replace("\"" + replacements[i] + "\"", "\"" + replacements[i + 1] + "\"");
            }
            return JsonParser.parseString(text).getAsJsonObject();
        } catch (IOException e) {
            throw new IllegalStateException("Error reading " + full, e);
        }
    }
}
