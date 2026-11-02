// SPDX-License-Identifier: LGPL-3.0-or-later
package dev.steelcode.core.api.gameplay;

import com.mojang.serialization.Codec;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;
import org.jetbrains.annotations.ApiStatus;

import java.util.function.Supplier;
import java.util.function.UnaryOperator;

/**
 * A value stored per dimension, saved with the world (vanilla {@code SavedData}, identical on both loaders).
 * Obtain one with {@code REG.worldData(name, codec, default)}. Server side only.
 * <pre>{@code
 * public static final WorldData<Integer> METEORS = REG.worldData("meteors", Codec.INT, () -> 0);
 * METEORS.update(server.overworld(), n -> n + 1);
 * int total = METEORS.getGlobal(server);                       // the overworld's value
 * }</pre>
 * Saved as {@code data/<modid>_<name>.dat} inside each dimension folder.
 * @param <T> value type; treat it as immutable and use {@link #set} / {@link #update}
 */
public final class WorldData<T> {

    private final String fileName;
    private final Codec<T> codec;
    private final Supplier<T> defaultValue;

    /**
     * Internal: use {@code REG.worldData(...)}.
     * @param modId        mod id
     * @param name         data name
     * @param codec        save codec
     * @param defaultValue initial value
     */
    @ApiStatus.Internal
    public WorldData(String modId, String name, Codec<T> codec, Supplier<T> defaultValue) {
        this.fileName = modId + "_" + name;
        this.codec = codec;
        this.defaultValue = defaultValue;
    }

    /**
     * @param level a server level (dimension)
     * @return its value
     */
    public T get(ServerLevel level) {
        return storage(level).value;
    }

    /**
     * Sets the value and marks it to be saved.
     * @param level a server level
     * @param value the new value
     */
    public void set(ServerLevel level, T value) {
        Storage s = storage(level);
        s.value = value;
        s.setDirty();
    }

    /**
     * Replaces the value with a function of the current one.
     * @param level  a server level
     * @param change computes the new value
     */
    public void update(ServerLevel level, UnaryOperator<T> change) {
        set(level, change.apply(get(level)));
    }

    /**
     * @param server the server
     * @return the overworld's value, handy for server-wide data
     */
    public T getGlobal(MinecraftServer server) {
        return get(server.overworld());
    }

    /**
     * Sets the overworld's value.
     * @param server the server
     * @param value  the new value
     */
    public void setGlobal(MinecraftServer server, T value) {
        set(server.overworld(), value);
    }

    private Storage storage(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(new SavedData.Factory<>(
                () -> new Storage(defaultValue.get()), this::load, null), fileName);
    }

    private Storage load(CompoundTag tag, HolderLookup.Provider registries) {
        T value = codec.parse(registries.createSerializationContext(NbtOps.INSTANCE), tag.get("value"))
                .result().orElseGet(defaultValue);
        return new Storage(value);
    }

    private final class Storage extends SavedData {
        private T value;

        private Storage(T value) {
            this.value = value;
        }

        @Override
        public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
            codec.encodeStart(registries.createSerializationContext(NbtOps.INSTANCE), value).result()
                    .ifPresent((Tag t) -> tag.put("value", t));
            return tag;
        }
    }
}
