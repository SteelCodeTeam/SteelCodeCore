// SPDX-License-Identifier: LGPL-3.0-or-later
package dev.steelcode.core.impl.compat;

import mcp.mobius.waila.api.IBlockAccessor;
import mcp.mobius.waila.api.IBlockComponentProvider;
import mcp.mobius.waila.api.IClientRegistrar;
import mcp.mobius.waila.api.IPluginConfig;
import mcp.mobius.waila.api.ITooltip;
import mcp.mobius.waila.api.IWailaClientPlugin;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.Block;
import org.jetbrains.annotations.ApiStatus;

/** WTHIT client plugin: extra block tooltip lines. Declared in {@code waila_plugins.json} (both loaders). */
@ApiStatus.Internal
public class WthitCompat implements IWailaClientPlugin {
    @Override
    public void register(IClientRegistrar registrar) {
        registrar.body(new IBlockComponentProvider() {
            @Override
            public void appendBody(ITooltip tooltip, IBlockAccessor accessor, IPluginConfig config) {
                Component line = ViewerData.tooltip(accessor.getBlockState());
                if (line != null) tooltip.addLine(line);
            }
        }, Block.class);
    }
}
