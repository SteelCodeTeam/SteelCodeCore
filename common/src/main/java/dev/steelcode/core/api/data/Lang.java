// SPDX-License-Identifier: LGPL-3.0-or-later
package dev.steelcode.core.api.data;

import dev.steelcode.core.impl.i18n.BuiltinLang;
import dev.steelcode.core.impl.registry.RegistryContext;
import dev.steelcode.core.impl.util.Refs;
import org.jetbrains.annotations.ApiStatus;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

/**
 * Translations for one language. Obtain one with {@code REG.lang(code)}.
 * <p>
 * Names are built automatically from <b>patterns</b> ({@code "stairs" → "%s Stairs"} / {@code "Escaleras de %s"})
 * and <b>words</b> ({@code "gold" → "oro"}), so usually you only translate your own words:
 * <pre>{@code
 * REG.lang("es_es").words("tin", "estaño", "maple", "arce");   // es_es ships with common patterns and words
 * REG.lang("es_es").set("ruby_sword", "Espada carmesí");       // a one-off override (id or full key)
 * REG.lang("fr_fr").pattern("stairs", "Escalier en %s").words("gold", "or");
 * }</pre>
 * Datagen logs which words are missing in each language. Built-in patterns and words exist for {@code en_us} and
 * {@code es_es}; other languages start empty and fall back to English for missing words.
 */
public final class Lang {

    private final String code;
    private final Map<String, String> words = new HashMap<>();
    private final Map<String, String> patterns = new HashMap<>();
    private final Map<String, String> overrides = new HashMap<>();

    /**
     * Internal: use {@code REG.lang(code)}.
     * @param code language code
     */
    @ApiStatus.Internal
    public Lang(String code) {
        this.code = code;
    }

    /**
     * @return the language code, e.g. {@code "es_es"}
     */
    public String code() {
        return code;
    }

    /**
     * Translates one word (a material, wood, color...).
     * @param word        the word as it appears in ids, e.g. {@code "tin"}
     * @param translation its translation
     * @return this
     */
    public Lang word(String word, String translation) {
        words.put(word, translation);
        return this;
    }

    /**
     * Translates several words at once: {@code words("tin", "estaño", "zinc", "zinc")}.
     * @param pairs alternating words and translations
     * @return this
     * @throws IllegalArgumentException if the arguments are not in pairs
     */
    public Lang words(String... pairs) {
        if (pairs.length % 2 != 0) throw new IllegalArgumentException("words(...) takes pairs of word and translation");
        for (int i = 0; i < pairs.length; i += 2) word(pairs[i], pairs[i + 1]);
        return this;
    }

    /**
     * Adds or replaces a pattern. {@code %s} is the word; in the {@code "colored"} pattern {@code %1$s} is the word and
     * {@code %2$s} the color.
     * @param pattern pattern id, e.g. {@code "stairs"}
     * @param format  the format, e.g. {@code "Escaleras de %s"}
     * @return this
     */
    public Lang pattern(String pattern, String format) {
        patterns.put(pattern, format);
        return this;
    }

    /**
     * Fixed text for an id ({@code "ruby_sword"}) or a full key ({@code "itemGroup.mymod.main"}).
     * @param idOrKey registry path or translation key
     * @param text    the translation
     * @return this
     */
    public Lang set(String idOrKey, String text) {
        overrides.put(idOrKey, text);
        return this;
    }

    // ================================================================ resolution (datagen)

    /**
     * Internal: translates a registered name. Words without translation are added to {@code missing}.
     * @param name    the name entry
     * @param missing collects missing words
     * @return the translated text
     */
    @ApiStatus.Internal
    public String resolve(RegistryContext.NameEntry name, Set<String> missing) {
        String fixed = overrides.getOrDefault(name.key(), overrides.get(name.id()));
        if (fixed != null) return fixed;

        if (name.pattern().equals("literal")) {
            if (!code.equals("en_us")) missing.add("text '" + name.key() + "'");
            return name.subject();
        }
        String format = patterns.get(name.pattern());
        if (format == null && name.pattern().equals("self")) format = "%s";
        if (format == null) format = BuiltinLang.pattern(code, name.pattern());
        if (format == null) {
            missing.add("pattern '" + name.pattern() + "'");
            format = BuiltinLang.pattern("en_us", name.pattern());
            if (format == null) format = "%s";
        }
        if (!format.contains("%")) return format;
        String subject = translate(name.subject(), missing);
        if (name.color() != null && name.color().startsWith("@")) {
            // Modifier: "@exposed" + "tin" -> "Exposed Tin" / "estaño expuesto", which then enters the main pattern.
            String inner = patterns.get(name.color().substring(1));
            if (inner == null) inner = BuiltinLang.pattern(code, name.color().substring(1));
            if (inner == null) inner = BuiltinLang.pattern("en_us", name.color().substring(1));
            if (inner != null) subject = String.format(inner, subject);
        } else if (name.color() != null) {
            // "mármol" + "rojo" -> "mármol rojo" ("colored" pattern), which then enters the main pattern.
            String colored = patterns.get("colored");
            if (colored == null) colored = BuiltinLang.pattern(code, "colored");
            if (colored == null) colored = BuiltinLang.pattern("en_us", "colored");
            subject = String.format(colored, subject, translate(name.color(), missing));
        }
        String text = String.format(format, subject);
        return text.isEmpty() ? text : Character.toUpperCase(text.charAt(0)) + text.substring(1);
    }

    private String translate(String word, Set<String> missing) {
        String t = words.get(word);
        if (t == null) t = BuiltinLang.word(code, word);
        if (t != null) return t;
        if (!code.equals("en_us")) missing.add(word);
        return Refs.titleCase(word);
    }

    static Set<String> newMissingSet() {
        return new TreeSet<>();
    }
}
