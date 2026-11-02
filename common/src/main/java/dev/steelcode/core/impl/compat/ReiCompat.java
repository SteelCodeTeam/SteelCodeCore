// SPDX-License-Identifier: LGPL-3.0-or-later
package dev.steelcode.core.impl.compat;

import me.shedaniel.rei.api.client.entry.filtering.base.BasicFilteringRule;
import me.shedaniel.rei.api.client.plugins.REIClientPlugin;
import me.shedaniel.rei.api.client.registry.display.DisplayRegistry;
import me.shedaniel.rei.api.common.entry.EntryStack;
import me.shedaniel.rei.api.common.util.EntryStacks;
import me.shedaniel.rei.plugin.common.displays.DefaultInformationDisplay;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.ApiStatus;

import java.util.ArrayList;
import java.util.Collection;

/** REI client plugin: info displays and hidden entries. Fabric uses the "rei_client" entrypoint; NeoForge an annotated subclass. */
@ApiStatus.Internal
public class ReiCompat implements REIClientPlugin {
    @Override
    public void registerDisplays(DisplayRegistry registry) {
        ViewerData.info().forEach((stack, lines) -> {
            DefaultInformationDisplay display = DefaultInformationDisplay.createFromEntry(EntryStacks.of(stack), stack.getHoverName());
            display.lines(lines);
            registry.add(display);
        });
    }

    @Override
    public void registerBasicEntryFiltering(BasicFilteringRule<?> rule) {
        rule.hide(() -> {
            Collection<EntryStack<?>> out = new ArrayList<>();
            for (ItemStack s : ViewerData.hidden()) out.add(EntryStacks.of(s));
            return out;
        });
    }
}
