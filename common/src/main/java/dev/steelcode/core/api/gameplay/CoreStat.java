// SPDX-License-Identifier: LGPL-3.0-or-later
package dev.steelcode.core.api.gameplay;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.stats.Stats;
import net.minecraft.world.entity.player.Player;

/**
 * A custom statistic, shown in Statistics → General. Obtain one with {@code REG.stat(name)}.
 * <pre>{@code
 * CoreStat METALS_FORGED = REG.stat("metals_forged");
 * METALS_FORGED.award(player, 1);
 * }</pre>
 * Translation key: {@code stat.<modid>.<name>}.
 * @param id the statistic id
 */
public record CoreStat(ResourceLocation id) {

    /**
     * Adds to the statistic (server side; ignored on the client).
     * @param player the player
     * @param amount amount to add
     */
    public void award(Player player, int amount) {
        player.awardStat(id, amount);
    }

    /**
     * @param player a server player
     * @return the player's current value
     */
    public int get(ServerPlayer player) {
        return player.getStats().getValue(Stats.CUSTOM.get(id));
    }
}
