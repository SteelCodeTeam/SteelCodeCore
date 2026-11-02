---
title: Gameplay
order: 8
---
# Gameplay

## Networking

Typed packets declared in `common`; handlers run on the game thread.

```java
// Client -> server
public static final CoreNetwork.Channel<Integer> CAST = REG.network().toServer("cast_power",
        ByteBufCodecs.VAR_INT, (player, power) -> Powers.cast(player, power));
CAST.sendToServer(3);

// Server -> client, with a CompoundTag (no codec needed)
public static final CoreNetwork.Channel<CompoundTag> SHAKE =
        REG.network().toClient("shake", tag -> Camera.shake(tag.getFloat("power")));
SHAKE.sendTo(serverPlayer, tag);
SHAKE.sendToAll(server, tag);
```

Declare channels in static fields so both sides register them identically. Always validate what the server
receives. Sending to players without a connection (fake players from other mods) is silently skipped.

## Player data

A value stored on every player, saved with the world (Fabric data attachments / NeoForge attachment types):

```java
public static final PlayerData<Integer> MANA = REG.playerData("mana", Codec.INT, () -> 100)
        .copyOnDeath()                       // keep it when dying
        .synced(ByteBufCodecs.VAR_INT)       // send it to the owning client
        .build();

MANA.set(player, 80);                        // server
MANA.update(player, m -> m - 10);            // server
int shown = MANA.getClient();                // client: the local player's value
```

Synced values are resent on join, respawn and dimension change. Use immutable values (records, primitives,
strings) and change them through `set` / `update`.

## World data

```java
public static final WorldData<Integer> METEORS = REG.worldData("meteors", Codec.INT, () -> 0);

METEORS.update(level, n -> n + 1);           // per dimension
int total = METEORS.getGlobal(server);       // the overworld's value
```

Stored as vanilla `SavedData` (`data/<modid>_<name>.dat`), identical on both loaders. Server side only.

## Key bindings

```java
REG.keybind("dash", GLFW.GLFW_KEY_R)
        .displayName("Dash")
        .onPress(player -> player.addDeltaMovement(player.getLookAngle().scale(1.5)))   // runs on the server
        .onPressClient(() -> { /* client-side feedback */ })
        .build();
```

The key appears in Controls under your mod's category and is rebindable. `onPress` sends a packet automatically,
so server logic needs no networking code.

## HUD

```java
REG.hud("mana").anchor(HudBuilder.Anchor.BOTTOM_LEFT, 10, 30)
        .bar(() -> MANA.getClient() / 100f, 81, 5, 0xFF3399FF)
        .text(() -> Component.literal("Mana " + MANA.getClient()), 0xFFFFFFFF)
        .visibleWhen(() -> MANA.getClient() < 100)
        .build();
```

With both a bar and a text, the text sits above the bar. Colors are ARGB — keep the `0xFF` alpha byte. Elements
are hidden in spectator mode. It is safe to declare in common code; it is only drawn on the client.

## Commands

```java
REG.command("mana").permission(2)
        .sub("get", n -> n.executes(ctx -> ctx.reply("Mana: " + MANA.get(ctx.self()))))
        .sub("set", n -> n.arg("player", CommandBuilder.player()).arg("value", CommandBuilder.integer(0, 1000))
                .executes(ctx -> { MANA.set(ctx.player("player"), ctx.integer("value")); ctx.reply("Done"); }))
        .build();
// /mymod mana get   ·   /mymod mana set Steve 50
```

Argument types: `player`, `players`, `integer`, `decimal`, `bool`, `word`, `text` (rest of the line), `item`,
`blockPos`. Each command has its own permission level, even though they share the `/<modid>` root.

## Statistics and advancement triggers

```java
public static final CoreStat SPELLS = REG.stat("spells_cast");          // Statistics -> General
SPELLS.award(player, 1);

public static final RegistrySupplier<CoreTrigger> USED_POWER = REG.trigger("used_power");
USED_POWER.get().fire(serverPlayer);
REG.advancements().child("first_power", ROOT, WAND).trigger(USED_POWER).build();
```

In advancement JSON written by hand, the trigger id is `<modid>:<name>`.
