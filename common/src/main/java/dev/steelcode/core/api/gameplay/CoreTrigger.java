// SPDX-License-Identifier: LGPL-3.0-or-later
package dev.steelcode.core.api.gameplay;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.advancements.critereon.ContextAwarePredicate;
import net.minecraft.advancements.critereon.EntityPredicate;
import net.minecraft.advancements.critereon.SimpleCriterionTrigger;
import net.minecraft.server.level.ServerPlayer;
import org.jetbrains.annotations.ApiStatus;

import java.util.Optional;

/**
 * An advancement criterion that you fire from code. Obtain one with {@code REG.trigger(name)}.
 * <pre>{@code
 * RegistrySupplier<CoreTrigger> USED_POWER = REG.trigger("used_power");
 * USED_POWER.get().fire(serverPlayer);
 * REG.advancements().child("first_power", ROOT, ICON).trigger(USED_POWER).build();
 * }</pre>
 * In advancement JSON it is {@code "trigger": "<modid>:<name>"}, with an optional {@code player} predicate.
 */
public final class CoreTrigger extends SimpleCriterionTrigger<CoreTrigger.Instance> {

    /**
     * The criterion instance read from advancement JSON.
     * @param player optional predicate the player must match
     */
    public record Instance(Optional<ContextAwarePredicate> player) implements SimpleCriterionTrigger.SimpleInstance {
        /** JSON codec of {@link Instance}. */
        public static final Codec<Instance> CODEC = RecordCodecBuilder.create(i -> i.group(
                EntityPredicate.ADVANCEMENT_CODEC.optionalFieldOf("player").forGetter(Instance::player)
        ).apply(i, Instance::new));
    }

    /** Internal: use {@code REG.trigger(name)}. */
    @ApiStatus.Internal
    public CoreTrigger() {
    }

    @Override
    public Codec<Instance> codec() {
        return Instance.CODEC;
    }

    /**
     * Fires the trigger: every advancement criterion using it is checked for this player.
     * @param player the player
     */
    public void fire(ServerPlayer player) {
        trigger(player, instance -> true);
    }
}
