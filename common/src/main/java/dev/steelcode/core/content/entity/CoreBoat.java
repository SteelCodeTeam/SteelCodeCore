// SPDX-License-Identifier: LGPL-3.0-or-later
package dev.steelcode.core.content.entity;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.vehicle.Boat;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;

import java.util.function.Supplier;

/** A boat of one of your woods: identical to vanilla but drops its own item. */
public class CoreBoat extends Boat {

    private final Supplier<? extends Item> drop;

    /**
     * @param type  the entity type
     * @param level the level
     * @param drop  item dropped when broken
     */
    public CoreBoat(EntityType<? extends Boat> type, Level level, Supplier<? extends Item> drop) {
        super(type, level);
        this.drop = drop;
    }

    @Override
    public Item getDropItem() {
        return drop.get();
    }
}
