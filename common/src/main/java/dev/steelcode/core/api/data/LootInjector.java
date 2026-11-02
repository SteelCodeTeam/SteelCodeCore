// SPDX-License-Identifier: LGPL-3.0-or-later
package dev.steelcode.core.api.data;

import dev.architectury.event.events.common.LootEvent;
import dev.steelcode.core.impl.registry.RegistryContext;
import dev.steelcode.core.impl.util.Refs;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.storage.loot.LootPool;
import net.minecraft.world.level.storage.loot.entries.LootItem;
import net.minecraft.world.level.storage.loot.functions.SetItemCountFunction;
import net.minecraft.world.level.storage.loot.predicates.LootItemRandomChanceCondition;
import net.minecraft.world.level.storage.loot.providers.number.ConstantValue;
import net.minecraft.world.level.storage.loot.providers.number.UniformGenerator;
import org.jetbrains.annotations.ApiStatus;

import java.util.ArrayList;
import java.util.List;

/**
 * Adds items to vanilla (or other mods') loot tables at load time, without replacing their JSON.
 * Obtain it with {@code REG.loot()}.
 * <pre>{@code
 * REG.loot().add(LootInjector.DUNGEON, RUBY, 0.25f, 1, 3);           // 25%: 1-3 rubies
 * REG.loot().add(LootInjector.ALL_VILLAGE_CHESTS, TIN_INGOT, 0.4f, 2, 5);
 * REG.loot().add("minecraft:entities/zombie", TIN_NUGGET, 0.05f, 1, 1);
 * }</pre>
 * Each call adds a separate pool, so it never changes the odds of the original loot. Only built-in tables are
 * modified; tables replaced by a data pack are left alone.
 */
public final class LootInjector {

    /** Monster room chests. */
    public static final String DUNGEON = "minecraft:chests/simple_dungeon";
    /** Abandoned mineshaft chest minecarts. */
    public static final String MINESHAFT = "minecraft:chests/abandoned_mineshaft";
    /** Desert pyramid chests. */
    public static final String DESERT_PYRAMID = "minecraft:chests/desert_pyramid";
    /** Jungle temple chests. */
    public static final String JUNGLE_TEMPLE = "minecraft:chests/jungle_temple";
    /** Stronghold corridor chests. */
    public static final String STRONGHOLD_CORRIDOR = "minecraft:chests/stronghold_corridor";
    /** Buried treasure chests. */
    public static final String BURIED_TREASURE = "minecraft:chests/buried_treasure";
    /** Shipwreck treasure chests. */
    public static final String SHIPWRECK_TREASURE = "minecraft:chests/shipwreck_treasure";
    /** Nether fortress chests. */
    public static final String NETHER_FORTRESS = "minecraft:chests/nether_bridge";
    /** Bastion treasure room chests. */
    public static final String BASTION_TREASURE = "minecraft:chests/bastion_treasure";
    /** End city chests. */
    public static final String END_CITY = "minecraft:chests/end_city_treasure";
    /** Ancient city chests. */
    public static final String ANCIENT_CITY = "minecraft:chests/ancient_city";
    /** Ruined portal chests. */
    public static final String RUINED_PORTAL = "minecraft:chests/ruined_portal";
    /** Pillager outpost chests. */
    public static final String PILLAGER_OUTPOST = "minecraft:chests/pillager_outpost";
    /** Every village chest (weaponsmith, armorer, houses...). */
    public static final String ALL_VILLAGE_CHESTS = "minecraft:chests/village/*";

    private record Entry(String table, Object item, float chance, int min, int max) {
    }

    private final RegistryContext registry;
    private final List<Entry> entries = new ArrayList<>();

    /** Internal: use {@code REG.loot()}. */
    @ApiStatus.Internal
    public LootInjector(RegistryContext registry) {
        this.registry = registry;
    }

    /**
     * Adds an item to a loot table.
     * @param table  loot table id; end it with {@code *} to match every table starting that way
     * @param item   item, supplier or id
     * @param chance probability 0-1 that the item appears each time the table is rolled
     * @param min    minimum count
     * @param max    maximum count
     * @return this helper, to chain more entries
     */
    public LootInjector add(String table, Object item, float chance, int min, int max) {
        entries.add(new Entry(table, item, chance, min, max));
        return this;
    }

    /** Internal: registers the loot event (called by {@code register()}). */
    @ApiStatus.Internal
    public void inject() {
        if (entries.isEmpty()) return;
        List<Entry> list = List.copyOf(entries);
        LootEvent.MODIFY_LOOT_TABLE.register((key, context, builtin) -> {
            if (!builtin) return;
            String id = key.location().toString();
            for (Entry e : list) {
                boolean match = e.table().endsWith("*") ? id.startsWith(e.table().substring(0, e.table().length() - 1))
                        : id.equals(e.table());
                if (!match) continue;
                var item = BuiltInRegistries.ITEM.get(Refs.id(e.item()));
                context.addPool(LootPool.lootPool()
                        .setRolls(ConstantValue.exactly(1))
                        .when(LootItemRandomChanceCondition.randomChance(e.chance()))
                        .add(LootItem.lootTableItem(item)
                                .apply(SetItemCountFunction.setCount(UniformGenerator.between(e.min(), e.max())))));
            }
        });
    }

}
