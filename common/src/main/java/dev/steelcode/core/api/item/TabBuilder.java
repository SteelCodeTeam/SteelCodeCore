// SPDX-License-Identifier: LGPL-3.0-or-later
package dev.steelcode.core.api.item;

import dev.architectury.registry.CreativeTabRegistry;
import dev.architectury.registry.registries.RegistrySupplier;
import dev.steelcode.core.impl.registry.RegistryContext;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ItemLike;
import org.jetbrains.annotations.ApiStatus;

import java.util.Objects;
import java.util.function.Supplier;

/**
 * A creative tab of your own. Obtain one with {@code REG.tab(name)}.
 * <pre>{@code
 * public static final RegistrySupplier<CreativeModeTab> TAB = REG.tab("main")
 *         .icon(() -> MyItems.RUBY.get())
 *         .title("Ruby Mod")
 *         .build();
 * }</pre>
 * The first tab you create becomes the mod's default tab: everything registered afterwards appears in it unless
 * you choose another with {@code .tab(...)} or {@code .noTab()}.
 */
public final class TabBuilder {

    private final RegistryContext registry;
    private final String name;
    private Supplier<? extends ItemLike> icon = () -> Items.BARRIER;
    private String title;
    private boolean makeDefault;

    /** Internal: use {@code REG.tab(name)}. */
    @ApiStatus.Internal
    public TabBuilder(RegistryContext registry, String name) {
        this.registry = registry;
        this.name = Objects.requireNonNull(name);
    }

    /**
     * @param icon icon item; resolved lazily, so it may be declared later in the class
     * @return this builder
     */
    public TabBuilder icon(Supplier<? extends ItemLike> icon) {
        this.icon = Objects.requireNonNull(icon);
        return this;
    }

    /**
     * @param title English title, written to {@code en_us.json} as {@code itemGroup.<modid>.<name>}
     * @return this builder
     */
    public TabBuilder title(String title) {
        this.title = title;
        return this;
    }

    /**
     * Makes this the default tab even if it is not the first one created.
     * @return this builder
     */
    public TabBuilder makeDefault() {
        this.makeDefault = true;
        return this;
    }

    /**
     * Registers the tab.
     * @return the tab supplier, usable in {@code .tab(...)} of any builder
     */
    public RegistrySupplier<CreativeModeTab> build() {
        String key = "itemGroup." + registry.modId() + "." + name;
        Supplier<? extends ItemLike> i = icon;
        RegistrySupplier<CreativeModeTab> tab = registry.registerTab(name,
                () -> CreativeTabRegistry.create(Component.translatable(key), () -> new ItemStack(i.get())),
                title);
        if (makeDefault) registry.defaultTab(tab);
        return tab;
    }
}
