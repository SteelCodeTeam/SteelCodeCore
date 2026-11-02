// SPDX-License-Identifier: LGPL-3.0-or-later
package dev.steelcode.core.api.gameplay;

import dev.steelcode.core.impl.registry.RegistryContext;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.server.level.ServerPlayer;
import org.jetbrains.annotations.ApiStatus;

import java.util.Objects;
import java.util.function.Consumer;

/**
 * A rebindable key (listed in Options → Controls under your mod's category) with an action on the client and/or the
 * server. Obtain one with {@code REG.keybind(name, key)}.
 * <pre>{@code
 * REG.keybind("dash", InputConstants.KEY_R)
 *         .onPress(player -> player.addDeltaMovement(player.getLookAngle().scale(1.5)))   // server
 *         .onPressClient(() -> Minecraft.getInstance().player.playSound(SoundEvents.BREEZE_JUMP))
 *         .build();
 * }</pre>
 * Key codes are those of {@code InputConstants} / GLFW. Translation keys: {@code key.<modid>.<name>} and
 * {@code key.categories.<modid>}.
 */
public final class KeyBindBuilder {

    private final RegistryContext registry;
    private final String name;
    private final int key;
    private String english;
    private Consumer<ServerPlayer> server;
    private Runnable client;

    /** Internal: use {@code REG.keybind(name, key)}. */
    @ApiStatus.Internal
    public KeyBindBuilder(RegistryContext registry, String name, int key) {
        this.registry = registry;
        this.name = Objects.requireNonNull(name);
        this.key = key;
    }

    /**
     * Runs on the server when the key is pressed; a packet is sent automatically.
     * @param action receives the player who pressed the key
     * @return this builder
     */
    public KeyBindBuilder onPress(Consumer<ServerPlayer> action) {
        this.server = action;
        return this;
    }

    /**
     * Runs on the client when the key is pressed.
     * @param action the action
     * @return this builder
     */
    public KeyBindBuilder onPressClient(Runnable action) {
        this.client = action;
        return this;
    }

    /**
     * @param english name shown in Controls
     * @return this builder
     */
    public KeyBindBuilder displayName(String english) {
        this.english = english;
        return this;
    }

    /** Registers the key mapping on the client and the server packet if needed. */
    public void build() {
        String ns = registry.modId();
        String key = "key." + ns + "." + name;
        String category = "key.categories." + ns;
        registry.literal(key, english != null ? english : dev.steelcode.core.impl.util.Refs.titleCase(name));
        registry.literalOnce(category, dev.steelcode.core.impl.util.Refs.titleCase(ns));
        Consumer<ServerPlayer> s = server;
        CoreNetwork.Channel<Boolean> channel = s == null ? null
                : registry.network().toServer("key_" + name, ByteBufCodecs.BOOL, (player, pressed) -> s.accept(player));
        Runnable c = client;
        int code = this.key;
        registry.clientEarly(() -> dev.steelcode.core.impl.client.ClientExtras.keyBind(key, code, category, () -> {
            if (c != null) c.run();
            if (channel != null) channel.sendToServer(true);
        }));
    }
}
