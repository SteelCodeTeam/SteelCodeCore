// SPDX-License-Identifier: LGPL-3.0-or-later
package dev.steelcode.core.api.effect;

import dev.architectury.registry.registries.RegistrySupplier;
import dev.steelcode.core.impl.platform.CorePlatform;
import dev.steelcode.core.impl.registry.RegistryContext;
import dev.steelcode.core.impl.util.Refs;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.Potion;
import net.minecraft.world.item.alchemy.Potions;
import org.jetbrains.annotations.ApiStatus;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Supplier;

/**
 * A potion (drinkable, splash, lingering and tipped arrow names) with its brewing-stand recipes.
 * Obtain one with {@code REG.potion(name)}.
 * <pre>{@code
 * REG.potion("bleeding").effect(BLEEDING, 45, 0)
 *         .brewedFrom(Potions.AWKWARD, Items.REDSTONE_BLOCK)
 *         .longVersion(90)          // + redstone
 *         .strongVersion(22, 1)     // + glowstone
 *         .build();
 * }</pre>
 * Long and strong versions are registered as {@code long_<name>} and {@code strong_<name>}.
 */
public final class PotionBuilder {

    private final RegistryContext registry;
    private final String name;
    private final List<Object[]> effects = new ArrayList<>();
    private Holder<Potion> base;
    private Object ingredient;
    private int longSeconds;
    private int strongSeconds;
    private int strongAmplifier;

    /** Internal: use {@code REG.potion(name)}. */
    @ApiStatus.Internal
    public PotionBuilder(RegistryContext registry, String name) {
        this.registry = registry;
        this.name = Objects.requireNonNull(name);
    }

    /**
     * Adds an effect; call several times for several effects.
     * @param effect    the effect
     * @param seconds   duration
     * @param amplifier 0 = level I
     * @return this builder
     */
    public PotionBuilder effect(Holder<MobEffect> effect, int seconds, int amplifier) {
        effects.add(new Object[]{effect, seconds, amplifier});
        return this;
    }

    /**
     * Adds the brewing recipe {@code base} + {@code ingredient}.
     * @param base       base potion, e.g. {@code Potions.AWKWARD}
     * @param ingredient brewing ingredient: item, supplier or id
     * @return this builder
     */
    public PotionBuilder brewedFrom(Holder<Potion> base, Object ingredient) {
        this.base = base;
        this.ingredient = ingredient;
        return this;
    }

    /**
     * Adds a long version, brewed with redstone.
     * @param seconds duration of the long version
     * @return this builder
     */
    public PotionBuilder longVersion(int seconds) {
        this.longSeconds = seconds;
        return this;
    }

    /**
     * Adds a strong version, brewed with glowstone.
     * @param seconds   duration of the strong version
     * @param amplifier amplifier of the strong version (1 = level II)
     * @return this builder
     */
    public PotionBuilder strongVersion(int seconds, int amplifier) {
        this.strongSeconds = seconds;
        this.strongAmplifier = amplifier;
        return this;
    }

    /**
     * Registers the potion(s), their names and brewing recipes.
     * @return the base potion
     * @throws IllegalStateException if no effect was added
     */
    public RegistrySupplier<Potion> build() {
        if (effects.isEmpty()) throw new IllegalStateException("Potion '" + name + "' has no effects");
        String potionName = registry.modId() + "." + name;
        RegistrySupplier<Potion> main = register(name, potionName, -1, -1);
        RegistrySupplier<Potion> longP = longSeconds > 0 ? register("long_" + name, potionName, longSeconds, -1) : null;
        RegistrySupplier<Potion> strong = strongSeconds > 0 ? register("strong_" + name, potionName, strongSeconds, strongAmplifier) : null;

        if (base != null) {
            Object ing = ingredient;
            Supplier<Item> item = () -> (Item) BuiltInRegistries.ITEM.get(Refs.id(ing));
            registry.afterRegister(() -> {
                CorePlatform.brewing(base, item, main);
                if (longP != null) CorePlatform.brewing(main, () -> Items.REDSTONE, longP);
                if (strong != null) CorePlatform.brewing(main, () -> Items.GLOWSTONE_DUST, strong);
            });
        }
        for (String kind : new String[]{"potion", "splash_potion", "lingering_potion", "tipped_arrow"}) {
            String key = "item.minecraft." + kind + ".effect." + potionName;
            registry.name(key, potionName, kind, name);
        }
        return main;
    }

    private RegistrySupplier<Potion> register(String id, String potionName, int seconds, int amplifier) {
        List<Object[]> e = List.copyOf(effects);
        return registry.registerPotion(id, () -> {
            MobEffectInstance[] instances = new MobEffectInstance[e.size()];
            for (int i = 0; i < e.size(); i++) {
                @SuppressWarnings("unchecked")
                Holder<MobEffect> effect = (Holder<MobEffect>) e.get(i)[0];
                int s = seconds > 0 ? seconds : (int) e.get(i)[1];
                int a = amplifier >= 0 ? amplifier : (int) e.get(i)[2];
                instances[i] = new MobEffectInstance(effect, s * 20, a);
            }
            return new Potion(potionName, instances);
        });
    }

    /**
     * @return {@code Potions.AWKWARD}, the usual base potion
     */
    public static Holder<Potion> awkward() {
        return Potions.AWKWARD;
    }
}
