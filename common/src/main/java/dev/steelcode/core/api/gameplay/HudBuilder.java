// SPDX-License-Identifier: LGPL-3.0-or-later
package dev.steelcode.core.api.gameplay;

import dev.steelcode.core.impl.client.ClientExtras;
import dev.steelcode.core.impl.registry.RegistryContext;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.ApiStatus;

import java.util.Objects;
import java.util.function.BooleanSupplier;
import java.util.function.Supplier;

/**
 * A HUD element (a bar and/or a text line on screen), identical on both loaders. Obtain one with
 * {@code REG.hud(name)}; it can be declared in common code, since it is only drawn on the client.
 *
 * <pre>{@code
 * REG.hud("mana").anchor(HudBuilder.Anchor.BOTTOM_LEFT, 10, 30)
 *         .bar(() -> MANA.getClient() / 100f, 81, 5, 0xFF3399FF)
 *         .text(() -> Component.literal("Mana " + MANA.getClient()), 0xFFFFFFFF)
 *         .build();
 * REG.hud("heat").anchor(HudBuilder.Anchor.TOP_RIGHT, 5, 5)
 *         .text(() -> Component.literal("Heat: " + HEAT.getClient()), 0xFFFFAA00).build();
 * }</pre>
 * With both a bar and a text, the text is drawn above the bar. Elements are hidden in spectator mode.
 * For fully custom drawing, register Architectury's {@code ClientGuiEvent.RENDER_HUD} from client-only code.
 * Colors are ARGB ({@code 0xAARRGGBB}); remember the alpha byte.
 */
public final class HudBuilder {

    /** Screen corner or edge the element is positioned from; offsets point into the screen. */
    public enum Anchor {
        /** Top-left corner. */
        TOP_LEFT,
        /** Top edge, centered. */
        TOP_CENTER,
        /** Top-right corner. */
        TOP_RIGHT,
        /** Bottom-left corner. */
        BOTTOM_LEFT,
        /** Bottom edge, centered. */
        BOTTOM_CENTER,
        /** Bottom-right corner. */
        BOTTOM_RIGHT,
        /** Center of the screen. */
        CENTER
    }

    private final RegistryContext registry;
    private final String name;
    private Anchor anchor = Anchor.TOP_LEFT;
    private int x = 5;
    private int y = 5;
    private Supplier<Float> bar;
    private int width = 80;
    private int height = 5;
    private int barColor = 0xFFFFFFFF;
    private Supplier<Component> text;
    private int textColor = 0xFFFFFFFF;
    private BooleanSupplier visible = () -> true;

    /**
     * Internal: use {@code REG.hud(name)}.
     *
     * @param registry the registry
     * @param name     element name
     */
    @ApiStatus.Internal
    public HudBuilder(RegistryContext registry, String name) {
        this.registry = registry;
        this.name = Objects.requireNonNull(name);
    }

    /**
     * Where the element is drawn (default: top-left, 5 px in).
     *
     * @param anchor  reference corner or edge
     * @param offsetX horizontal offset in pixels, towards the inside of the screen
     * @param offsetY vertical offset in pixels, towards the inside of the screen
     * @return this builder
     */
    public HudBuilder anchor(Anchor anchor, int offsetX, int offsetY) {
        this.anchor = Objects.requireNonNull(anchor);
        this.x = offsetX;
        this.y = offsetY;
        return this;
    }

    /**
     * A bar filled according to {@code fraction}, over a translucent black background.
     *
     * @param fraction fill level, clamped to 0-1, evaluated every frame
     * @param width    width in GUI pixels
     * @param height   height in GUI pixels
     * @param argb     fill color, {@code 0xAARRGGBB}
     * @return this builder
     */
    public HudBuilder bar(Supplier<Float> fraction, int width, int height, int argb) {
        this.bar = Objects.requireNonNull(fraction);
        this.width = width;
        this.height = height;
        this.barColor = argb;
        return this;
    }

    /**
     * A text line with shadow.
     *
     * @param text the text, evaluated every frame
     * @param argb text color, {@code 0xAARRGGBB}
     * @return this builder
     */
    public HudBuilder text(Supplier<Component> text, int argb) {
        this.text = Objects.requireNonNull(text);
        this.textColor = argb;
        return this;
    }

    /**
     * @param condition the element is drawn only while this is true, evaluated every frame
     * @return this builder
     */
    public HudBuilder visibleWhen(BooleanSupplier condition) {
        this.visible = Objects.requireNonNull(condition);
        return this;
    }

    /**
     * Registers the element on the client (does nothing on a dedicated server).
     *
     * @throws IllegalStateException if neither {@link #bar} nor {@link #text} was set
     */
    public void build() {
        if (bar == null && text == null) {
            throw new IllegalStateException("HUD '" + name + "': call bar(...) or text(...)");
        }
        int a = anchor.ordinal();
        int ox = x, oy = y, w = width, h = height, bc = barColor, tc = textColor;
        Supplier<Float> b = bar;
        Supplier<Component> t = text;
        BooleanSupplier v = visible;
        registry.clientEarly(() -> ClientExtras.hudElement(a, ox, oy, w, h, bc, tc, b, t, v));
    }
}
