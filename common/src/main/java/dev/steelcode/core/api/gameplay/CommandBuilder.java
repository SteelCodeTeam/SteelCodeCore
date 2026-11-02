// SPDX-License-Identifier: LGPL-3.0-or-later
package dev.steelcode.core.api.gameplay;

import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.ArgumentBuilder;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import dev.steelcode.core.impl.registry.RegistryContext;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.arguments.coordinates.BlockPosArgument;
import net.minecraft.commands.arguments.item.ItemArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import org.jetbrains.annotations.ApiStatus;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * Commands under {@code /<modid> ...} with typed arguments, without touching Brigadier. Obtain one with
 * {@code REG.command(name)}.
 * <pre>{@code
 * REG.command("mana").permission(2)
 *         .sub("set", c -> c.arg("player", CommandBuilder.player()).arg("amount", CommandBuilder.integer(0, 1000))
 *                 .executes(ctx -> { MANA.set(ctx.player("player"), ctx.integer("amount")); ctx.reply("Done"); }))
 *         .sub("get", c -> c.executes(ctx -> ctx.reply("Mana: " + MANA.get(ctx.self()))))
 *         .build();
 * // /mymod mana set Steve 50   ·   /mymod mana get
 * }</pre>
 * Arguments are added in order; an action set with {@code executes} runs when the command ends after the last
 * argument of its node. Throwing a {@code CommandSyntaxException} (e.g. from {@link Ctx#self()} on the console) shows
 * the usual red error.
 */
public final class CommandBuilder {

    /**
     * An argument type. Use the static factories below.
     * @param type creates the Brigadier argument type
     */
    public record Arg(Function<CommandBuildContext, ArgumentType<?>> type) {
    }

    /**
     * @return a single player (name or selector)
     */
    public static Arg player() { return new Arg(ctx -> EntityArgument.player()); }
    /**
     * @return one or more players (selector)
     */
    public static Arg players() { return new Arg(ctx -> EntityArgument.players()); }
    /**
     * @param min minimum value
     * @param max maximum value
     * @return an integer argument
     */
    public static Arg integer(int min, int max) { return new Arg(ctx -> IntegerArgumentType.integer(min, max)); }
    /**
     * @param min minimum value
     * @param max maximum value
     * @return a decimal argument
     */
    public static Arg decimal(double min, double max) { return new Arg(ctx -> DoubleArgumentType.doubleArg(min, max)); }
    /**
     * @return {@code true}/{@code false}
     */
    public static Arg bool() { return new Arg(ctx -> BoolArgumentType.bool()); }
    /**
     * @return a single word (no spaces)
     */
    public static Arg word() { return new Arg(ctx -> StringArgumentType.word()); }
    /**
     * @return the rest of the line, spaces included; must be the last argument
     */
    public static Arg text() { return new Arg(ctx -> StringArgumentType.greedyString()); }
    /**
     * @return an item id
     */
    public static Arg item() { return new Arg(ItemArgument::item); }
    /**
     * @return block coordinates (supports {@code ~})
     */
    public static Arg blockPos() { return new Arg(ctx -> BlockPosArgument.blockPos()); }

    /** What your action receives: the source and typed access to the arguments. */
    public static final class Ctx {
        private final CommandContext<CommandSourceStack> context;

        Ctx(CommandContext<CommandSourceStack> context) {
            this.context = context;
        }

        /**
         * @return the raw command source
         */
        public CommandSourceStack source() { return context.getSource(); }
        /**
         * @return the player running the command
         * @throws CommandSyntaxException if it is run by the console or a command block
         */
        public ServerPlayer self() throws CommandSyntaxException { return context.getSource().getPlayerOrException(); }
        /**
         * @param name argument name
         * @return the player argument
         * @throws CommandSyntaxException if the selector matches no player
         */
        public ServerPlayer player(String name) throws CommandSyntaxException { return EntityArgument.getPlayer(context, name); }
        /**
         * @param name argument name
         * @return the players argument
         * @throws CommandSyntaxException if the selector matches no player
         */
        public Collection<ServerPlayer> players(String name) throws CommandSyntaxException { return EntityArgument.getPlayers(context, name); }
        /**
         * @param name argument name
         * @return the integer argument
         */
        public int integer(String name) { return IntegerArgumentType.getInteger(context, name); }
        /**
         * @param name argument name
         * @return the decimal argument
         */
        public double decimal(String name) { return DoubleArgumentType.getDouble(context, name); }
        /**
         * @param name argument name
         * @return the boolean argument
         */
        public boolean bool(String name) { return BoolArgumentType.getBool(context, name); }
        /**
         * @param name argument name
         * @return a {@link CommandBuilder#word()} or {@link CommandBuilder#text()} argument
         */
        public String string(String name) { return StringArgumentType.getString(context, name); }
        /**
         * @param name argument name
         * @return the item argument
         */
        public Item item(String name) { return ItemArgument.getItem(context, name).getItem(); }
        /**
         * @param name argument name
         * @return the position argument
         * @throws CommandSyntaxException if the position is not loaded
         */
        public BlockPos blockPos(String name) throws CommandSyntaxException { return BlockPosArgument.getLoadedBlockPos(context, name); }

        /**
         * Sends a success message to the source.
         * @param message the text
         */
        public void reply(String message) {
            context.getSource().sendSuccess(() -> Component.literal(message), false);
        }

        /**
         * Sends a success message to the source.
         * @param message the text component
         */
        public void reply(Component message) {
            context.getSource().sendSuccess(() -> message, false);
        }

        /**
         * Sends a red failure message to the source.
         * @param message the text
         */
        public void error(String message) {
            context.getSource().sendFailure(Component.literal(message));
        }
    }

    /** The code run by a command. */
    @FunctionalInterface
    public interface Action {
        /**
         * @param ctx the command context
         * @throws CommandSyntaxException to report a usage error
         */
        void run(Ctx ctx) throws CommandSyntaxException;
    }

    private final RegistryContext registry;
    private final String name;
    private int permission;
    private final Node root = new Node();

    /** A command node: arguments in order, an optional action and sub-commands. */
    public static final class Node {
        private final List<Object[]> args = new ArrayList<>();
        private final List<Object[]> subs = new ArrayList<>();
        private Action action;

        private Node() {
        }

        /**
         * Appends an argument.
         * @param name argument name, used to read it from {@link Ctx}
         * @param type argument type
         * @return this node
         */
        public Node arg(String name, Arg type) {
            args.add(new Object[]{name, type});
            return this;
        }

        /**
         * Adds a literal sub-command after this node's arguments.
         * @param literal the sub-command word
         * @param config  configures the sub-command node
         * @return this node
         */
        public Node sub(String literal, Consumer<Node> config) {
            Node n = new Node();
            config.accept(n);
            subs.add(new Object[]{literal, n});
            return this;
        }

        /**
         * @param action runs when the command ends at this node
         * @return this node
         */
        public Node executes(Action action) {
            this.action = action;
            return this;
        }

        @SuppressWarnings({"unchecked", "rawtypes"})
        ArgumentBuilder<CommandSourceStack, ?> attach(ArgumentBuilder<CommandSourceStack, ?> start, CommandBuildContext ctx) {
            List<ArgumentBuilder<CommandSourceStack, ?>> chain = new ArrayList<>();
            chain.add(start);
            for (Object[] a : args) {
                chain.add(RequiredArgumentBuilder.argument((String) a[0], ((Arg) a[1]).type().apply(ctx)));
            }
            ArgumentBuilder last = chain.get(chain.size() - 1);
            if (action != null) {
                Action act = action;
                last.executes(c -> {
                    act.run(new Ctx((CommandContext<CommandSourceStack>) c));
                    return 1;
                });
            }
            for (Object[] s : subs) {
                last.then(((Node) s[1]).attach(Commands.literal((String) s[0]), ctx));
            }
            for (int i = chain.size() - 1; i > 0; i--) chain.get(i - 1).then((ArgumentBuilder) chain.get(i));
            return start;
        }
    }

    /** Internal: use {@code REG.command(name)}. */
    @ApiStatus.Internal
    public CommandBuilder(RegistryContext registry, String name) {
        this.registry = registry;
        this.name = Objects.requireNonNull(name);
    }

    /**
     * @param level 0 everyone (default), 2 operators (like {@code /give}), 4 server console
     * @return this builder
     */
    public CommandBuilder permission(int level) {
        this.permission = level;
        return this;
    }

    /**
     * Appends an argument to the root node.
     * @param name argument name
     * @param type argument type
     * @return this builder
     */
    public CommandBuilder arg(String name, Arg type) {
        root.arg(name, type);
        return this;
    }

    /**
     * Adds a sub-command to the root node.
     * @param literal the sub-command word
     * @param config  configures the sub-command
     * @return this builder
     */
    public CommandBuilder sub(String literal, Consumer<Node> config) {
        root.sub(literal, config);
        return this;
    }

    /**
     * @param action runs on {@code /<modid> <name>} (after the root arguments)
     * @return this builder
     */
    public CommandBuilder executes(Action action) {
        root.executes(action);
        return this;
    }

    /** Registers the command on every server start (and in single player). */
    public void build() {
        int perm = permission;
        registry.addCommand((dispatcher, ctx) -> {
            // The permission goes on this command's node: the shared /<modid> node is merged by Brigadier, so a
            // requirement there would come from whichever command registered first.
            LiteralArgumentBuilder<CommandSourceStack> literal = Commands.literal(name).requires(s -> s.hasPermission(perm));
            root.attach(literal, ctx);
            dispatcher.register(Commands.literal(registry.modId()).then(literal));
        });
    }
}
