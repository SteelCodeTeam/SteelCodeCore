// SPDX-License-Identifier: LGPL-3.0-or-later
package dev.steelcode.core.api.gameplay;

import dev.architectury.networking.NetworkManager;
import dev.architectury.platform.Platform;
import dev.architectury.utils.Env;
import dev.steelcode.core.impl.registry.RegistryContext;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import org.jetbrains.annotations.ApiStatus;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

/**
 * Client ↔ server packets declared in {@code common}. Obtain it with {@code REG.network()}.
 * Handlers run on the game thread (not the network thread), so they can touch the world directly.
 * <pre>{@code
 * // Client -> server
 * CoreNetwork.Channel<Integer> CAST = REG.network().toServer("cast_power", ByteBufCodecs.VAR_INT,
 *         (player, power) -> Powers.cast(player, power));
 * CAST.sendToServer(3);                                     // from the client
 *
 * // Server -> client, with a CompoundTag (no codec needed)
 * CoreNetwork.Channel<CompoundTag> SHAKE = REG.network().toClient("shake", tag -> Camera.shake(tag.getFloat("power")));
 * SHAKE.sendTo(serverPlayer, tag);
 * }</pre>
 * Declare channels in static fields so both sides register them in the same order. Always validate data received
 * on the server: clients can send anything.
 */
public final class CoreNetwork {

    /**
     * Internal: the generic payload that carries any value.
     * @param type  packet type
     * @param value carried value
     * @param <T>   value type
     */
    public record Payload<T>(Type<Payload<T>> type, T value) implements CustomPacketPayload {
    }

    private final RegistryContext registry;
    private final List<Runnable> registrations = new ArrayList<>();

    /** Internal: use {@code REG.network()}. */
    @ApiStatus.Internal
    public CoreNetwork(RegistryContext registry) {
        this.registry = registry;
    }

    /**
     * Declares a client-to-server packet.
     * @param name    channel name, unique within your mod
     * @param codec   how to write and read the value
     * @param handler runs on the server thread with the sending player
     * @param <T>     value type
     * @return the channel, to send with {@link Channel#sendToServer}
     */
    public <T> Channel<T> toServer(String name, StreamCodec<? super RegistryFriendlyByteBuf, T> codec,
                                   BiConsumer<ServerPlayer, T> handler) {
        Channel<T> channel = new Channel<>(name, codec);
        registrations.add(() -> NetworkManager.registerReceiver(NetworkManager.Side.C2S, channel.type, channel.payloadCodec,
                (payload, ctx) -> ctx.queue(() -> handler.accept((ServerPlayer) ctx.getPlayer(), payload.value()))));
        return channel;
    }

    /**
     * Declares a client-to-server packet carrying a {@code CompoundTag}.
     * @param name    channel name
     * @param handler runs on the server thread with the sending player
     * @return the channel
     */
    public Channel<CompoundTag> toServer(String name, BiConsumer<ServerPlayer, CompoundTag> handler) {
        return toServer(name, ByteBufCodecs.COMPOUND_TAG, handler);
    }

    /**
     * Declares a server-to-client packet.
     * @param name    channel name, unique within your mod
     * @param codec   how to write and read the value
     * @param handler runs on the client thread; keep client-only classes out of common code paths
     * @param <T>     value type
     * @return the channel, to send with {@link Channel#sendTo} or {@link Channel#sendToAll}
     */
    public <T> Channel<T> toClient(String name, StreamCodec<? super RegistryFriendlyByteBuf, T> codec, Consumer<T> handler) {
        Channel<T> channel = new Channel<>(name, codec);
        registrations.add(() -> {
            if (Platform.getEnvironment() == Env.CLIENT) {
                NetworkManager.registerReceiver(NetworkManager.Side.S2C, channel.type, channel.payloadCodec,
                        (payload, ctx) -> ctx.queue(() -> handler.accept(payload.value())));
            } else {
                NetworkManager.registerS2CPayloadType(channel.type, channel.payloadCodec);
            }
        });
        return channel;
    }

    /**
     * Declares a server-to-client packet carrying a {@code CompoundTag}.
     * @param name    channel name
     * @param handler runs on the client thread
     * @return the channel
     */
    public Channel<CompoundTag> toClient(String name, Consumer<CompoundTag> handler) {
        return toClient(name, ByteBufCodecs.COMPOUND_TAG, handler);
    }

    /** Internal: registers the channels with Architectury (called by {@code register()}). */
    @ApiStatus.Internal
    public void register() {
        registrations.forEach(Runnable::run);
    }

    /**
     * A declared packet type. Sending from the wrong side does nothing useful; use the method matching the direction.
     * @param <T> value type
     */
    public final class Channel<T> {
        private final CustomPacketPayload.Type<Payload<T>> type;
        private final StreamCodec<RegistryFriendlyByteBuf, Payload<T>> payloadCodec;

        @SuppressWarnings("unchecked")
        private Channel(String name, StreamCodec<? super RegistryFriendlyByteBuf, T> codec) {
            this.type = new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(registry.modId(), name));
            StreamCodec<RegistryFriendlyByteBuf, T> c = (StreamCodec<RegistryFriendlyByteBuf, T>) codec;
            this.payloadCodec = c.map(v -> new Payload<>(type, v), Payload::value);
        }

        /**
         * Sends a value from the client to the server.
         * @param value the value
         */
        public void sendToServer(T value) {
            NetworkManager.sendToServer(new Payload<>(type, value));
        }

        /**
         * Sends a value from the server to one player. Does nothing for players without a connection (fake players).
         * @param player the receiving player
         * @param value  the value
         */
        public void sendTo(ServerPlayer player, T value) {
            if (player.connection == null) return; // fake player / no connection
            NetworkManager.sendToPlayer(player, new Payload<>(type, value));
        }

        /**
         * Sends a value from the server to every connected player.
         * @param server the server
         * @param value  the value
         */
        public void sendToAll(MinecraftServer server, T value) {
            NetworkManager.sendToPlayers(server.getPlayerList().getPlayers(), new Payload<>(type, value));
        }

        /**
         * @return the packet id, {@code <modid>:<name>}
         */
        public ResourceLocation id() {
            return type.id();
        }
    }
}
