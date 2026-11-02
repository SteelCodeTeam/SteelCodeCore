// SPDX-License-Identifier: LGPL-3.0-or-later
package dev.steelcode.core.api.data;

import dev.architectury.registry.level.entity.trade.TradeRegistry;
import dev.steelcode.core.impl.registry.RegistryContext;
import dev.steelcode.core.impl.util.Refs;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.npc.VillagerProfession;
import net.minecraft.world.entity.npc.VillagerTrades;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.trading.ItemCost;
import net.minecraft.world.item.trading.MerchantOffer;
import org.jetbrains.annotations.ApiStatus;

import java.util.ArrayList;
import java.util.List;

/**
 * Villager and wandering-trader trades. Obtain it with {@code REG.trades()}.
 * <pre>{@code
 * REG.trades().villager(VillagerProfession.TOOLSMITH, 3)
 *         .sells(RUBY_PICKAXE, 1, 18)      // 18 emeralds -> 1 pickaxe
 *         .buys(RUBY, 4, 1);               // 4 rubies -> 1 emerald
 * REG.trades().wanderer(false).sells(MAPLE_SAPLING, 1, 5);
 * }</pre>
 */
public final class Trades {

    private final RegistryContext registry;
    private final List<Runnable> registrations = new ArrayList<>();

    /** Internal: use {@code REG.trades()}. */
    @ApiStatus.Internal
    public Trades(RegistryContext registry) {
        this.registry = registry;
    }

    /**
     * Trades of a profession at a level.
     * @param profession the profession, e.g. {@code VillagerProfession.TOOLSMITH}
     * @param level      1 (novice) to 5 (master)
     * @return a listing to add trades to
     */
    public Listing villager(VillagerProfession profession, int level) {
        return new Listing(profession, level, false);
    }

    /**
     * Wandering trader trades.
     * @param rare whether they go to the rare (special) offer pool
     * @return a listing to add trades to
     */
    public Listing wanderer(boolean rare) {
        return new Listing(null, rare ? 2 : 1, true);
    }

    /** Internal: registers the trades (called by {@code register()}). */
    @ApiStatus.Internal
    public void inject() {
        registrations.forEach(Runnable::run);
    }

    /** Trades added to one villager level or wandering-trader pool. */
    public final class Listing {
        private final VillagerProfession profession;
        private final int level;
        private final boolean wanderer;
        private int maxUses = 12;

        private Listing(VillagerProfession profession, int level, boolean wanderer) {
            this.profession = profession;
            this.level = level;
            this.wanderer = wanderer;
        }

        /**
         * @param uses uses before the trade locks (default 12); affects the following {@code sells}/{@code buys}
         * @return this listing
         */
        public Listing maxUses(int uses) {
            this.maxUses = uses;
            return this;
        }

        /**
         * The trader sells items for emeralds.
         * @param item     item, supplier or id
         * @param count    items given
         * @param emeralds emeralds paid by the player
         * @return this listing
         */
        public Listing sells(Object item, int count, int emeralds) {
            int uses = maxUses, xp = xpFor(level);
            add((entity, random) -> new MerchantOffer(new ItemCost(Items.EMERALD, emeralds),
                    new ItemStack(BuiltInRegistries.ITEM.get(Refs.id(item)), count), uses, xp, 0.05F));
            return this;
        }

        /**
         * The trader buys items and pays emeralds.
         * @param item     item, supplier or id
         * @param count    items taken
         * @param emeralds emeralds paid to the player
         * @return this listing
         */
        public Listing buys(Object item, int count, int emeralds) {
            int uses = maxUses, xp = xpFor(level);
            add((entity, random) -> new MerchantOffer(new ItemCost(BuiltInRegistries.ITEM.get(Refs.id(item)), count),
                    new ItemStack(Items.EMERALD, emeralds), uses, xp, 0.05F));
            return this;
        }

        private void add(VillagerTrades.ItemListing listing) {
            if (wanderer) registrations.add(() -> TradeRegistry.registerTradeForWanderingTrader(level == 2, listing));
            else registrations.add(() -> TradeRegistry.registerVillagerTrade(profession, level, listing));
        }
    }

    private static int xpFor(int level) {
        return switch (level) {
            case 1 -> 2;
            case 2 -> 10;
            case 3 -> 20;
            case 4 -> 30;
            default -> 30;
        };
    }
}
