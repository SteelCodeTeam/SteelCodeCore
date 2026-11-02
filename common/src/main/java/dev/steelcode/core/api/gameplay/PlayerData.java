// SPDX-License-Identifier: LGPL-3.0-or-later
package dev.steelcode.core.api.gameplay;

import com.mojang.serialization.Codec;
import dev.architectury.event.events.common.PlayerEvent;
import dev.steelcode.core.impl.platform.CorePlatform;
import dev.steelcode.core.impl.registry.RegistryContext;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import org.jetbrains.annotations.ApiStatus;

import java.util.Objects;
import java.util.function.Supplier;
import java.util.function.UnaryOperator;

/**
 * A value stored on every player: saved with the world, optionally kept on death and synced to the owning client.
 * Uses Fabric data attachments and NeoForge attachment types under the hood. Obtain one with
 * {@code REG.playerData(name, codec, default).build()}.
 * <pre>{@code
 * public static final PlayerData<Integer> MANA = REG.playerData("mana", Codec.INT, () -> 100)
 *         .copyOnDeath().synced(ByteBufCodecs.VAR_INT).build();
 *
 * MANA.set(player, MANA.get(player) - 10);     // server; sent to the client automatically
 * int shown = MANA.getClient();                 // client: the local player's value
 * }</pre>
 * Treat values as immutable (records, primitives, strings): mutate through {@link #set} or {@link #update} so changes
 * are saved and synced.
 * @param <T> value type
 */
public final class PlayerData<T> {

    private final CorePlatform.AttachmentHandle<T> handle;
    private final Supplier<T> defaultValue;
    private final CoreNetwork.Channel<T> sync;
    private volatile T clientValue;

    private PlayerData(CorePlatform.AttachmentHandle<T> handle, Supplier<T> defaultValue, CoreNetwork.Channel<T> sync) {
        this.handle = handle;
        this.defaultValue = defaultValue;
        this.sync = sync;
    }

    /**
     * @param player any player
     * @return the stored value; on the client, the synced value (or the default if not synced)
     */
    public T get(Player player) {
        if (player.level().isClientSide) return getClient();
        return handle.get(player);
    }

    /**
     * Client only.
     * @return the local player's value, or the default before the first sync
     */
    public T getClient() {
        T v = clientValue;
        return v != null ? v : defaultValue.get();
    }

    /**
     * Sets the value. Call on the server; if {@code synced}, the owning client is updated.
     * @param player the player
     * @param value  the new value
     */
    public void set(Player player, T value) {
        if (player.level().isClientSide) throw new IllegalStateException("PlayerData.set must be called on the server");
        handle.set(player, value);
        if (player instanceof ServerPlayer sp) resync(sp);
    }

    /**
     * Replaces the value with a function of the current one, e.g. {@code update(p, m -> m + 1)}.
     * @param player the player
     * @param change computes the new value
     */
    public void update(Player player, UnaryOperator<T> change) {
        set(player, change.apply(get(player)));
    }

    /**
     * Sends the value to the client again. Done automatically on join, respawn and dimension change.
     * @param player the player; ignored if it has no connection (fake players)
     */
    public void resync(ServerPlayer player) {
        // Fake players (from other mods) or players still connecting have no connection: nothing to send to.
        if (sync != null && player.connection != null) sync.sendTo(player, handle.get(player));
    }

    // ================================================================ builder

    /**
     * Configures a {@link PlayerData}; finish with {@link #build()}.
     * @param <T> value type
     */
    public static final class Builder<T> {
        private final RegistryContext registry;
        private final String name;
        private final Codec<T> codec;
        private final Supplier<T> defaultValue;
        private boolean copyOnDeath;
        private StreamCodec<? super RegistryFriendlyByteBuf, T> syncCodec;

        /**
         * Internal: use {@code REG.playerData(...)}.
         * @param registry     the registry
         * @param name         data name
         * @param codec        save codec
         * @param defaultValue default value
         */
        @ApiStatus.Internal
        public Builder(RegistryContext registry, String name, Codec<T> codec, Supplier<T> defaultValue) {
            this.registry = registry;
            this.name = Objects.requireNonNull(name);
            this.codec = Objects.requireNonNull(codec);
            this.defaultValue = Objects.requireNonNull(defaultValue);
        }

        /**
         * Keeps the value when the player dies (otherwise it resets to the default).
         * @return this builder
         */
        public Builder<T> copyOnDeath() {
            this.copyOnDeath = true;
            return this;
        }

        /**
         * Sends the value to the owning client whenever it changes.
         * @param codec network codec of the value
         * @return this builder
         */
        public Builder<T> synced(StreamCodec<? super RegistryFriendlyByteBuf, T> codec) {
            this.syncCodec = codec;
            return this;
        }

        /**
         * Registers the attachment (and the sync packet).
         * @return the player data handle
         */
        public PlayerData<T> build() {
            ResourceLocation id = ResourceLocation.fromNamespaceAndPath(registry.modId(), name);
            CorePlatform.AttachmentHandle<T> handle = CorePlatform.attachment(id, codec, defaultValue, copyOnDeath);
            @SuppressWarnings("unchecked")
            PlayerData<T>[] self = new PlayerData[1];
            CoreNetwork.Channel<T> channel = syncCodec == null ? null
                    : registry.network().toClient("sync_" + name, syncCodec, v -> self[0].clientValue = v);
            self[0] = new PlayerData<>(handle, defaultValue, channel);
            if (channel != null) {
                PlayerData<T> data = self[0];
                registry.afterRegister(() -> {
                    PlayerEvent.PLAYER_JOIN.register(data::resync);
                    PlayerEvent.PLAYER_RESPAWN.register((player, conqueredEnd, reason) -> data.resync(player));
                    PlayerEvent.CHANGE_DIMENSION.register((player, from, to) -> data.resync(player));
                });
            }
            return self[0];
        }
    }
}
