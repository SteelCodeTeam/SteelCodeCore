// SPDX-License-Identifier: LGPL-3.0-or-later
package dev.steelcode.core.impl.compat;

import dev.architectury.registry.registries.RegistrySupplier;
import dev.steelcode.core.impl.registry.RegistryContext;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.ApiStatus;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

/** Data shared by every compatibility plugin (JEI, REI, EMI, Jade, WTHIT). */
@ApiStatus.Internal
public final class ViewerData {
    private ViewerData() {}

    /** @return info pages: stack -> translatable lines */
    public static Map<ItemStack, List<Component>> info() {
        Map<ItemStack, List<Component>> out = new LinkedHashMap<>();
        for (RegistryContext reg : RegistryContext.all()) {
            reg.viewerInfo().forEach((sup, keys) -> {
                ItemStack stack = stackOf(sup);
                if (stack.isEmpty()) return;
                List<Component> lines = new ArrayList<>();
                for (String key : keys) lines.add(Component.translatable(key));
                out.put(stack, lines);
            });
        }
        return out;
    }

    public static List<ItemStack> hidden() {
        List<ItemStack> out = new ArrayList<>();
        for (RegistryContext reg : RegistryContext.all())
            for (RegistrySupplier<?> sup : reg.hiddenInViewers()) {
                ItemStack s = stackOf(sup);
                if (!s.isEmpty()) out.add(s);
            }
        return out;
    }

    /** @return the extra Jade/WTHIT line for the looked-at block, or {@code null} */
    public static Component tooltip(BlockState state) {
        for (RegistryContext reg : RegistryContext.all())
            for (Map.Entry<RegistrySupplier<Block>, Function<BlockState, Component>> e : reg.blockTooltips().entrySet())
                if (e.getKey().isPresent() && state.is(e.getKey().get())) return e.getValue().apply(state);
        return null;
    }

    public static boolean hasTooltips() {
        for (RegistryContext reg : RegistryContext.all()) if (!reg.blockTooltips().isEmpty()) return true;
        return false;
    }

    private static ItemStack stackOf(RegistrySupplier<?> sup) {
        if (!sup.isPresent()) return ItemStack.EMPTY;
        Object o = sup.get();
        return o instanceof ItemLike like ? new ItemStack(like) : ItemStack.EMPTY;
    }
}
