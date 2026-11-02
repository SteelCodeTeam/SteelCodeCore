// SPDX-License-Identifier: LGPL-3.0-or-later
package dev.steelcode.core.api.item;

import dev.architectury.registry.registries.RegistrySupplier;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.Item;

import java.util.Optional;

/**
 * Tools, armor and gear created by {@link EquipmentBuilder}. Pieces that were not created are {@code Optional.empty()}.
 * @param name          the equipment prefix
 * @param sword         the sword
 * @param pickaxe       the pickaxe
 * @param axe           the axe
 * @param shovel        the shovel
 * @param hoe           the hoe
 * @param helmet        the helmet
 * @param chestplate    the chestplate
 * @param leggings      the leggings
 * @param boots         the boots
 * @param armorMaterial the armor material backing the armor pieces
 * @param bow           the bow
 * @param crossbow      the crossbow
 * @param fishingRod    the fishing rod
 * @param shield        the shield
 * @param horseArmor    the horse armor
 */
public record EquipmentSet(String name, Optional<RegistrySupplier<Item>> sword, Optional<RegistrySupplier<Item>> pickaxe,
                           Optional<RegistrySupplier<Item>> axe, Optional<RegistrySupplier<Item>> shovel,
                           Optional<RegistrySupplier<Item>> hoe, Optional<RegistrySupplier<Item>> helmet,
                           Optional<RegistrySupplier<Item>> chestplate, Optional<RegistrySupplier<Item>> leggings,
                           Optional<RegistrySupplier<Item>> boots, Optional<RegistrySupplier<ArmorMaterial>> armorMaterial,
                           Optional<RegistrySupplier<Item>> bow, Optional<RegistrySupplier<Item>> crossbow,
                           Optional<RegistrySupplier<Item>> fishingRod, Optional<RegistrySupplier<Item>> shield,
                           Optional<RegistrySupplier<Item>> horseArmor) {
}
