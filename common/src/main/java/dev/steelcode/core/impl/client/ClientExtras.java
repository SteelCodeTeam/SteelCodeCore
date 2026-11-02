// SPDX-License-Identifier: LGPL-3.0-or-later
package dev.steelcode.core.impl.client;

import com.mojang.blaze3d.platform.InputConstants;
import dev.architectury.event.events.client.ClientGuiEvent;
import dev.architectury.event.events.client.ClientTickEvent;
import dev.architectury.registry.client.keymappings.KeyMappingRegistry;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.ApiStatus;

import java.util.function.BooleanSupplier;
import java.util.function.Supplier;

/** Key bindings and HUD (client only). */
@ApiStatus.Internal
public final class ClientExtras {

    private ClientExtras() {
    }

    /** Custom HUD drawing. Call only from client code. */
    @FunctionalInterface
    public interface HudRenderer {
        void render(GuiGraphics graphics, DeltaTracker delta);
    }

    public static void keyBind(String name, int key, String category, Runnable onPress) {
        KeyMapping mapping = new KeyMapping(name, InputConstants.Type.KEYSYM, key, category);
        KeyMappingRegistry.register(mapping);
        ClientTickEvent.CLIENT_POST.register(mc -> {
            while (mapping.consumeClick()) {
                if (mc.player != null) onPress.run();
            }
        });
    }

    /** A HUD element built by {@code HudBuilder} (client code only). */
    public static void hud(HudRenderer renderer) {
        ClientGuiEvent.RENDER_HUD.register((graphics, delta) -> {
            Minecraft mc = Minecraft.getInstance();
            if (mc.options.hideGui || mc.player == null) return;
            renderer.render(graphics, delta);
        });
    }

    public static void hudElement(int anchor, int ox, int oy, int width, int height, int barColor, int textColor,
                                  Supplier<Float> bar, Supplier<Component> text, BooleanSupplier visible) {
        hud((graphics, delta) -> {
            Minecraft mc = Minecraft.getInstance();
            if (mc.player == null || mc.player.isSpectator() || !visible.getAsBoolean()) return;
            Component line = text != null ? text.get() : null;
            int textW = line != null ? mc.font.width(line) : 0;
            int textH = line != null ? mc.font.lineHeight + (bar != null ? 2 : 0) : 0;
            int w = Math.max(bar != null ? width : 0, textW);
            int h = textH + (bar != null ? height : 0);
            int sw = graphics.guiWidth(), sh = graphics.guiHeight();
            int x = switch (anchor % 3) {
                case 0 -> ox;
                case 1 -> (sw - w) / 2 + ox;
                default -> sw - w - ox;
            };
            int y = anchor == 6 ? (sh - h) / 2 + oy : anchor < 3 ? oy : sh - h - oy;
            if (anchor == 6) x = (sw - w) / 2 + ox;
            if (line != null) graphics.drawString(mc.font, line, x, y, textColor, true);
            if (bar != null) {
                int by = y + textH;
                float f = Math.max(0, Math.min(1, bar.get()));
                graphics.fill(x - 1, by - 1, x + width + 1, by + height + 1, 0xC0000000);
                graphics.fill(x, by, x + Math.round(width * f), by + height, barColor);
            }
        });
    }
}
