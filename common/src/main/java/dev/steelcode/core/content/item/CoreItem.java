// SPDX-License-Identifier: LGPL-3.0-or-later
package dev.steelcode.core.content.item;

import dev.steelcode.core.api.item.ItemBuilder;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;

import java.util.List;

/**
 * An item whose simple behaviour comes from lambdas set in {@code ItemBuilder}: tooltip, use in the air, use on
 * blocks and hitting entities. No item class needed; extend it if you want to override more.
 */
public class CoreItem extends Item {

    /** Use in the air (right click). */
    @FunctionalInterface
    public interface Use {
        /**
         * @param level  the level
         * @param player the player
         * @param hand   the hand holding the item
         * @param stack  the used stack
         * @return the vanilla result, or {@code null} for the normal behaviour
         */
        InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand, ItemStack stack);
    }

    /** Use on a block. */
    @FunctionalInterface
    public interface UseOn {
        /**
         * @param context the use context (position, face, player...)
         * @return the vanilla result, or {@code null} for the normal behaviour
         */
        InteractionResult useOn(UseOnContext context);
    }

    /** Hitting an entity with the item. */
    @FunctionalInterface
    public interface Hit {
        /**
         * @param stack    the item stack
         * @param target   the entity hit
         * @param attacker the attacker
         */
        void hit(ItemStack stack, LivingEntity target, LivingEntity attacker);
    }

    /**
     * The behaviour of a {@link CoreItem}. Any action may be {@code null}.
     * @param tooltipKeys translation keys of the tooltip lines
     * @param use         use in the air
     * @param useOn       use on a block
     * @param hit         hitting an entity
     */
    public record Behaviour(List<String> tooltipKeys, Use use, UseOn useOn, Hit hit) {
    }

    private final Behaviour behaviour;

    /**
     * @param properties item properties
     * @param behaviour  the behaviour
     */
    public CoreItem(Properties properties, Behaviour behaviour) {
        super(properties);
        this.behaviour = behaviour;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        for (String key : behaviour.tooltipKeys()) tooltip.add(Component.translatable(key).withStyle(net.minecraft.ChatFormatting.GRAY));
        super.appendHoverText(stack, context, tooltip, flag);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        if (behaviour.use() != null) {
            InteractionResultHolder<ItemStack> r = behaviour.use().use(level, player, hand, player.getItemInHand(hand));
            if (r != null) return r;
        }
        return super.use(level, player, hand);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        if (behaviour.useOn() != null) {
            InteractionResult r = behaviour.useOn().useOn(context);
            if (r != null) return r;
        }
        return super.useOn(context);
    }

    @Override
    public boolean hurtEnemy(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        if (behaviour.hit() != null) behaviour.hit().hit(stack, target, attacker);
        return super.hurtEnemy(stack, target, attacker);
    }
}
