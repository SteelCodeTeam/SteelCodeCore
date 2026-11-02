// SPDX-License-Identifier: LGPL-3.0-or-later
package dev.steelcode.core.api.world;

import dev.architectury.registry.registries.RegistrySupplier;
import dev.steelcode.core.api.block.BlockFamily;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;

/**
 * Everything created by {@link WoodBuilder}.
 * @param name          wood name, e.g. {@code "maple"}
 * @param log           the log
 * @param wood          the wood (bark on all sides)
 * @param strippedLog   the stripped log
 * @param strippedWood  the stripped wood
 * @param planks        the planks family (stairs, slab, fence, door...)
 * @param leaves        the leaves
 * @param sapling       the sapling
 * @param pottedSapling the potted sapling
 * @param logsTag       block and item tag {@code <modid>:<name>_logs} with log, wood and stripped variants
 * @param tree          id of the tree's configured feature
 * @param sign          the sign item, unless {@code noSigns()}
 * @param hangingSign   the hanging sign item, unless {@code noSigns()}
 * @param boat          the boat item, unless {@code noBoats()}
 * @param chestBoat     the chest boat item, unless {@code noBoats()}
 */
public record WoodSet(String name, RegistrySupplier<Block> log, RegistrySupplier<Block> wood,
                      RegistrySupplier<Block> strippedLog, RegistrySupplier<Block> strippedWood, BlockFamily planks,
                      RegistrySupplier<Block> leaves, RegistrySupplier<Block> sapling, RegistrySupplier<Block> pottedSapling,
                      ResourceLocation logsTag, ResourceLocation tree,
                      java.util.Optional<RegistrySupplier<net.minecraft.world.item.Item>> sign,
                      java.util.Optional<RegistrySupplier<net.minecraft.world.item.Item>> hangingSign,
                      java.util.Optional<RegistrySupplier<net.minecraft.world.item.Item>> boat,
                      java.util.Optional<RegistrySupplier<net.minecraft.world.item.Item>> chestBoat) {
}
