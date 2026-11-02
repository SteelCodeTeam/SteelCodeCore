// SPDX-License-Identifier: LGPL-3.0-or-later
package dev.steelcode.core.api.effect;

import dev.architectury.registry.registries.RegistrySupplier;
import dev.steelcode.core.content.effect.CoreMobEffect;
import dev.steelcode.core.impl.registry.RegistryContext;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import org.jetbrains.annotations.ApiStatus;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * A status effect. Obtain one with {@code REG.effect(name)}.
 * <pre>{@code
 * // Bleeding: 1 damage per second
 * RegistrySupplier<MobEffect> BLEEDING = REG.effect("bleeding").harmful().color(0x8A0303)
 *         .onTick((entity, amp) -> { entity.hurt(entity.damageSources().magic(), 1 + amp); return true; }, 20)
 *         .build();
 *
 * // Lightness: +20% speed per level
 * REG.effect("lightness").beneficial().color(0xBFEFFF)
 *         .attribute(Attributes.MOVEMENT_SPEED, 0.2, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL).build();
 * }</pre>
 * Icon: {@code textures/mob_effect/<name>.png} (18x18). The returned {@code RegistrySupplier} is also a
 * {@code Holder<MobEffect>}, so it can be passed directly to potions, food effects and {@code addEffect}.
 */
public final class EffectBuilder {

    private final RegistryContext registry;
    private final String name;
    private MobEffectCategory category = MobEffectCategory.NEUTRAL;
    private int color = 0xFFFFFF;
    private CoreMobEffect.Tick tick;
    private int interval = 20;
    private final List<Object[]> attributes = new ArrayList<>();
    private String displayName;

    /** Internal: use {@code REG.effect(name)}. */
    @ApiStatus.Internal
    public EffectBuilder(RegistryContext registry, String name) {
        this.registry = registry;
        this.name = Objects.requireNonNull(name);
    }

    /**
     * Beneficial effect (blue name, default).
     * @return this builder
     */
    public EffectBuilder beneficial() { this.category = MobEffectCategory.BENEFICIAL; return this; }
    /**
     * Harmful effect (red name).
     * @return this builder
     */
    public EffectBuilder harmful() { this.category = MobEffectCategory.HARMFUL; return this; }
    /**
     * Neutral effect.
     * @return this builder
     */
    public EffectBuilder neutral() { this.category = MobEffectCategory.NEUTRAL; return this; }

    /**
     * @param rgb color of particles and potion bottles, {@code 0xRRGGBB}
     * @return this builder
     */
    public EffectBuilder color(int rgb) {
        this.color = rgb;
        return this;
    }

    /**
     * Runs logic periodically while the effect is active.
     * @param tick     the logic; return {@code false} to remove the effect
     * @param interval ticks between calls (20 = every second)
     * @return this builder
     */
    public EffectBuilder onTick(CoreMobEffect.Tick tick, int interval) {
        this.tick = tick;
        this.interval = interval;
        return this;
    }

    /**
     * Modifies an attribute while active; the amount is multiplied by the level.
     * @param attribute      the attribute, e.g. {@code Attributes.MOVEMENT_SPEED}
     * @param amountPerLevel amount per effect level
     * @param operation      how the amount is applied
     * @return this builder
     */
    public EffectBuilder attribute(Holder<Attribute> attribute, double amountPerLevel, AttributeModifier.Operation operation) {
        attributes.add(new Object[]{attribute, amountPerLevel, operation});
        return this;
    }

    /**
     * @param english English name instead of the one derived from the id
     * @return this builder
     */
    public EffectBuilder displayName(String english) {
        this.displayName = english;
        return this;
    }

    /**
     * Registers the effect and queues its name.
     * @return the effect supplier
     */
    public RegistrySupplier<MobEffect> build() {
        MobEffectCategory cat = category;
        int c = color, iv = interval;
        CoreMobEffect.Tick t = tick;
        List<Object[]> attrs = List.copyOf(attributes);
        String ns = registry.modId();
        RegistrySupplier<MobEffect> effect = registry.registerEffect(name, () -> {
            CoreMobEffect e = new CoreMobEffect(cat, c, t, iv);
            int i = 0;
            for (Object[] a : attrs) {
                @SuppressWarnings("unchecked")
                Holder<Attribute> attr = (Holder<Attribute>) a[0];
                e.addAttributeModifier(attr, ResourceLocation.fromNamespaceAndPath(ns, "effect." + name + "." + i++),
                        (double) a[1], (AttributeModifier.Operation) a[2]);
            }
            return e;
        });
        registry.expectTexture("assets/" + ns + "/textures/mob_effect/" + name + ".png");
        String key = "effect." + ns + "." + name;
        registry.name(key, name, "self", name);
        if (displayName != null) registry.lang("en_us").set(key, displayName);
        return effect;
    }
}
