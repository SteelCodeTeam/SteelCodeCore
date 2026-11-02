// SPDX-License-Identifier: LGPL-3.0-or-later
package dev.steelcode.core.content.effect;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;

/** A status effect whose logic is a lambda. Created by {@code REG.effect(...)}; extend it for more. */
public class CoreMobEffect extends MobEffect {

    /** Runs every {@code interval} ticks while the effect is active. */
    @FunctionalInterface
    public interface Tick {
        /**
         * @param entity    the affected entity
         * @param amplifier 0 = level I
         * @return {@code false} to remove the effect
         */
        boolean tick(LivingEntity entity, int amplifier);
    }

    private final Tick tick;
    private final int interval;

    /**
     * @param category beneficial, harmful or neutral
     * @param color    particle color, {@code 0xRRGGBB}
     * @param tick     the logic, or {@code null} for none (attribute-only effects)
     * @param interval ticks between calls, at least 1
     */
    public CoreMobEffect(MobEffectCategory category, int color, Tick tick, int interval) {
        super(category, color);
        this.tick = tick;
        this.interval = Math.max(1, interval);
    }

    @Override
    public boolean applyEffectTick(LivingEntity entity, int amplifier) {
        return tick == null || tick.tick(entity, amplifier);
    }

    @Override
    public boolean shouldApplyEffectTickThisTick(int duration, int amplifier) {
        return tick != null && duration % interval == 0;
    }
}
