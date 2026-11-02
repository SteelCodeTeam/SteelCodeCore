// SPDX-License-Identifier: LGPL-3.0-or-later
package dev.steelcode.core.api.data;

import dev.architectury.platform.Platform;
import dev.steelcode.core.SteelCodeCore;
import org.jetbrains.annotations.ApiStatus;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

/**
 * A simple config file, {@code config/<modid>.toml}, shared by client and server. Obtain it with {@code REG.config()}.
 * <pre>{@code
 * public static final CoreConfig CFG = REG.config();
 * public static final CoreConfig.Value<Boolean> TIN_ORE = CFG.bool("tin_ore", true, "Generate tin ore");
 * public static final CoreConfig.Value<Integer> MAX_HEAT = CFG.integer("max_heat", 100, 1, 1000, "Maximum heat");
 *
 * REG.material("tin").metal().generate(o -> o.size(9).count(10).trapezoid(-24, 56).enabledIf(TIN_ORE::get)).build();
 * if (MAX_HEAT.get() > 50) { ... }
 * }</pre>
 * The file is written with defaults and comments the first time a value is read. Values edited by the player are
 * kept; invalid or out-of-range values fall back to the default with a warning. Values are read once per game
 * start; there is no live reload and no client/server sync.
 */
public final class CoreConfig {

    private final String modId;
    private final List<Value<?>> values = new ArrayList<>();
    private boolean loaded;

    /**
     * Internal: use {@code REG.config()}.
     * @param modId the mod id
     */
    @ApiStatus.Internal
    public CoreConfig(String modId) {
        this.modId = modId;
    }

    /**
     * Declares a boolean value.
     * @param key     key in the file
     * @param def     default value
     * @param comment comment written above it
     * @return the value handle
     */
    public Value<Boolean> bool(String key, boolean def, String comment) {
        return add(new Value<>(key, def, comment, "true / false", s -> switch (s.toLowerCase()) {
            case "true" -> true;
            case "false" -> false;
            default -> null;
        }));
    }

    /**
     * Declares an integer value.
     * @param key     key in the file
     * @param def     default value
     * @param min     minimum accepted value
     * @param max     maximum accepted value
     * @param comment comment written above it
     * @return the value handle
     */
    public Value<Integer> integer(String key, int def, int min, int max, String comment) {
        return add(new Value<>(key, def, comment, min + " - " + max, s -> {
            try {
                int v = Integer.parseInt(s);
                return v >= min && v <= max ? v : null;
            } catch (NumberFormatException e) {
                return null;
            }
        }));
    }

    /**
     * Declares a decimal value.
     * @param key     key in the file
     * @param def     default value
     * @param min     minimum accepted value
     * @param max     maximum accepted value
     * @param comment comment written above it
     * @return the value handle
     */
    public Value<Double> decimal(String key, double def, double min, double max, String comment) {
        return add(new Value<>(key, def, comment, min + " - " + max, s -> {
            try {
                double v = Double.parseDouble(s);
                return v >= min && v <= max ? v : null;
            } catch (NumberFormatException e) {
                return null;
            }
        }));
    }

    /**
     * Declares a text value.
     * @param key     key in the file
     * @param def     default value
     * @param comment comment written above it
     * @return the value handle
     */
    public Value<String> text(String key, String def, String comment) {
        return add(new Value<>(key, def, comment, "texto", s -> s));
    }

    /**
     * @return the config file path
     */
    public Path path() {
        return Platform.getConfigFolder().resolve(modId + ".toml");
    }

    private <T> Value<T> add(Value<T> value) {
        if (loaded) throw new IllegalStateException("Declare every config value before reading any of them");
        values.add(value);
        return value;
    }

    private synchronized void load() {
        if (loaded) return;
        loaded = true;
        Map<String, String> raw = new HashMap<>();
        Path file = path();
        try {
            if (Files.exists(file)) {
                for (String line : Files.readAllLines(file, StandardCharsets.UTF_8)) {
                    String l = line.strip();
                    if (l.isEmpty() || l.startsWith("#") || !l.contains("=")) continue;
                    int eq = l.indexOf('=');
                    String v = l.substring(eq + 1).strip();
                    if (v.startsWith("\"") && v.endsWith("\"") && v.length() >= 2) v = v.substring(1, v.length() - 1);
                    raw.put(l.substring(0, eq).strip(), v);
                }
            }
        } catch (IOException e) {
            SteelCodeCore.LOGGER.error("Could not read {}", file, e);
        }
        StringBuilder out = new StringBuilder("# " + modId + " configuration (generated by SteelCodeCore)\n");
        for (Value<?> v : values) {
            v.resolve(raw.get(v.key));
            out.append('\n');
            if (v.comment != null && !v.comment.isEmpty()) out.append("# ").append(v.comment).append('\n');
            out.append("# Default: ").append(v.def).append("  (").append(v.range).append(")\n");
            Object current = v.value;
            out.append(v.key).append(" = ").append(current instanceof String ? "\"" + current + "\"" : current).append('\n');
        }
        try {
            Files.createDirectories(file.getParent());
            Files.writeString(file, out.toString(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            SteelCodeCore.LOGGER.error("Could not write {}", file, e);
        }
    }

    /**
     * One config value. Reading it loads the file on first use.
     * @param <T> the value type
     */
    public final class Value<T> {
        private final String key;
        private final T def;
        private final String comment;
        private final String range;
        private final Function<String, T> parser;
        private T value;

        private Value(String key, T def, String comment, String range, Function<String, T> parser) {
            this.key = key;
            this.def = def;
            this.comment = comment;
            this.range = range;
            this.parser = parser;
            this.value = def;
        }

        private void resolve(String raw) {
            if (raw == null) {
                value = def;
                return;
            }
            T parsed = parser.apply(raw);
            if (parsed == null) {
                SteelCodeCore.LOGGER.warn("[{}] Invalid config value for '{}': '{}'; using {}", modId, key, raw, def);
                value = def;
            } else {
                value = parsed;
            }
        }

        /**
         * @return the current value (the default if missing or invalid)
         */
        public T get() {
            load();
            return value;
        }

        /**
         * @return the key in the file
         */
        public String key() {
            return key;
        }
    }
}
