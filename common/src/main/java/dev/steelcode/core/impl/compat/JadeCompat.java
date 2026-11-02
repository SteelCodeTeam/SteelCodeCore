// SPDX-License-Identifier: LGPL-3.0-or-later
package dev.steelcode.core.impl.compat;

import dev.steelcode.core.SteelCodeCore;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import org.jetbrains.annotations.ApiStatus;
import snownee.jade.api.BlockAccessor;
import snownee.jade.api.IBlockComponentProvider;
import snownee.jade.api.ITooltip;
import snownee.jade.api.IWailaClientRegistration;
import snownee.jade.api.IWailaPlugin;
import snownee.jade.api.WailaPlugin;
import snownee.jade.api.config.IPluginConfig;

/** Jade plugin: extra block tooltip lines. Found via {@code @WailaPlugin} on NeoForge and the "jade" entrypoint on Fabric. */
@WailaPlugin(SteelCodeCore.MOD_ID)
@ApiStatus.Internal
public class JadeCompat implements IWailaPlugin {
    private static final ResourceLocation UID = ResourceLocation.fromNamespaceAndPath(SteelCodeCore.MOD_ID, "block_info");

    @Override
    public void registerClient(IWailaClientRegistration registration) {
        registration.registerBlockComponent(new IBlockComponentProvider() {
            @Override
            public void appendTooltip(ITooltip tooltip, BlockAccessor accessor, IPluginConfig config) {
                Component line = ViewerData.tooltip(accessor.getBlockState());
                if (line != null) tooltip.add(line);
            }

            @Override
            public ResourceLocation getUid() { return UID; }
        }, Block.class);
    }
}
