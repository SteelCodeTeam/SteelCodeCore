// SPDX-License-Identifier: LGPL-3.0-or-later
package dev.steelcode.core.api.data;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import dev.steelcode.core.api.gameplay.CoreTrigger;
import dev.steelcode.core.impl.registry.AdvancementEntry;
import dev.steelcode.core.impl.registry.RegistryContext;
import dev.steelcode.core.impl.util.Refs;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.ApiStatus;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Advancements of your mod, in their own tab. Obtain it with {@code REG.advancements()}.
 * <pre>{@code
 * ResourceLocation ROOT = REG.advancements().root("root", RUBY).title("SteelCode").description("Start the adventure")
 *         .background("minecraft:textures/block/calcite.png").obtain(RUBY).build();
 * REG.advancements().child("all_metals", ROOT, TIN_INGOT).title("Metallurgist").description("Get every metal")
 *         .obtainAll(TIN_INGOT, SILVER_INGOT).frame(Advancements.Frame.GOAL).xp(50).build();
 * }</pre>
 * Titles and descriptions are English; translate them with
 * {@code REG.lang(code).set("advancements.<modid>.<id>.title", ...)} and {@code ".description"}.
 */
public final class Advancements {

    /** Icon frame and toast style. */
    public enum Frame {
        /** Regular advancement. */
        TASK,
        /** Goal (rounded frame). */
        GOAL,
        /** Challenge (spiky frame, purple toast). */
        CHALLENGE
    }

    private final RegistryContext registry;

    /** Internal: use {@code REG.advancements()}. */
    @ApiStatus.Internal
    public Advancements(RegistryContext registry) {
        this.registry = registry;
    }

    /**
     * A root advancement, which creates the tab. Its background defaults to stone; root advancements do not toast.
     * @param name advancement name
     * @param icon icon item
     * @return the builder
     */
    public Builder root(String name, Object icon) {
        return new Builder(name, null, icon).background("minecraft:textures/gui/advancements/backgrounds/stone.png");
    }

    /**
     * A child advancement.
     * @param name   advancement name
     * @param parent parent id, as returned by {@link Builder#build()}
     * @param icon   icon item
     * @return the builder
     */
    public Builder child(String name, ResourceLocation parent, Object icon) {
        return new Builder(name, parent, icon);
    }

    /**
     * Configures one advancement. Without criteria it uses {@code minecraft:tick}, so it is granted right away (handy for roots).
     */
    public final class Builder {
        private final String name;
        private final ResourceLocation parent;
        private final Object icon;
        private String title;
        private String description = "";
        private String background;
        private Frame frame = Frame.TASK;
        private int xp;
        private boolean hidden;
        private boolean all;
        private final Map<String, JsonObject> criteria = new LinkedHashMap<>();

        private Builder(String name, ResourceLocation parent, Object icon) {
            this.name = Objects.requireNonNull(name);
            this.parent = parent;
            this.icon = Objects.requireNonNull(icon);
        }

        /**
         * @param english title
         * @return this builder
         */
        public Builder title(String english) { this.title = english; return this; }
        /**
         * @param english description
         * @return this builder
         */
        public Builder description(String english) { this.description = english; return this; }
        /**
         * @param texture tab background (root only), e.g. {@code "minecraft:textures/block/calcite.png"}
         * @return this builder
         */
        public Builder background(String texture) { this.background = texture; return this; }
        /**
         * @param frame icon frame
         * @return this builder
         */
        public Builder frame(Frame frame) { this.frame = frame; return this; }
        /**
         * @param xp experience reward
         * @return this builder
         */
        public Builder xp(int xp) { this.xp = xp; return this; }
        /**
         * Hidden until completed.
         * @return this builder
         */
        public Builder hidden() { this.hidden = true; return this; }

        /**
         * Requires obtaining any one of these items.
         * @param items items, suppliers or ids
         * @return this builder
         */
        public Builder obtain(Object... items) {
            for (Object item : items) criteria.put("has_" + Refs.id(item).getPath(), inventory(item));
            return this;
        }

        /**
         * Requires obtaining all of these items.
         * @param items items, suppliers or ids
         * @return this builder
         */
        public Builder obtainAll(Object... items) {
            this.all = true;
            return obtain(items);
        }

        /**
         * Requires placing a block.
         * @param block the block
         * @return this builder
         */
        public Builder placed(Object block) {
            JsonObject blockPred = new JsonObject();
            blockPred.addProperty("blocks", Refs.id(block).toString());
            JsonObject pred = new JsonObject();
            pred.add("block", blockPred);
            JsonObject loc = new JsonObject();
            loc.addProperty("condition", "minecraft:location_check");
            loc.add("predicate", pred);
            JsonArray location = new JsonArray();
            location.add(loc);
            JsonObject conditions = new JsonObject();
            conditions.add("location", location);
            return criterion("placed_" + Refs.id(block).getPath(), "minecraft:placed_block", conditions);
        }

        /**
         * Requires killing an entity.
         * @param entityType {@code "minecraft:zombie"} or your entity type supplier
         * @return this builder
         */
        public Builder kill(Object entityType) {
            JsonObject pred = new JsonObject();
            pred.addProperty("type", Refs.id(entityType).toString());
            JsonObject cond = new JsonObject();
            cond.addProperty("condition", "minecraft:entity_properties");
            cond.addProperty("entity", "this");
            cond.add("predicate", pred);
            JsonArray entity = new JsonArray();
            entity.add(cond);
            JsonObject conditions = new JsonObject();
            conditions.add("entity", entity);
            return criterion("killed_" + Refs.id(entityType).getPath(), "minecraft:player_killed_entity", conditions);
        }

        /**
         * Requires a custom trigger ({@code REG.trigger(...)}), fired from code with {@code trigger.get().fire(player)}.
         * @param trigger the trigger
         * @return this builder
         */
        public Builder trigger(dev.architectury.registry.registries.RegistrySupplier<CoreTrigger> trigger) {
            return criterion(trigger.getId().getPath(), trigger.getId().toString(), null);
        }

        /**
         * Adds any vanilla criterion.
         * @param id         criterion name
         * @param trigger    trigger id, e.g. {@code "minecraft:enter_block"}
         * @param conditions trigger conditions JSON
         * @return this builder
         */
        public Builder criterion(String id, String trigger, JsonObject conditions) {
            JsonObject c = new JsonObject();
            c.addProperty("trigger", trigger);
            if (conditions != null) c.add("conditions", conditions);
            criteria.put(id, c);
            return this;
        }

        /**
         * Queues the advancement JSON and its texts.
         * @return the advancement id, to use as a parent
         */
        public ResourceLocation build() {
            ResourceLocation id = ResourceLocation.fromNamespaceAndPath(registry.modId(), name);
            String prefix = "advancements." + registry.modId() + "." + name.replace('/', '.');
            registry.literal(prefix + ".title", title != null ? title : Refs.titleCase(name));
            registry.literal(prefix + ".description", description);
            if (criteria.isEmpty()) criterion("tick", "minecraft:tick", null);

            Map<String, JsonObject> crit = new LinkedHashMap<>(criteria);
            boolean requireAll = all;
            Frame fr = frame;
            int reward = xp;
            boolean hide = hidden;
            String bg = parent == null ? background : null;
            registry.addAdvancement(new AdvancementEntry(id, () -> {
                JsonObject iconJson = new JsonObject();
                iconJson.addProperty("id", Refs.id(icon).toString());
                iconJson.addProperty("count", 1);
                JsonObject display = new JsonObject();
                display.add("icon", iconJson);
                display.add("title", translate(prefix + ".title"));
                display.add("description", translate(prefix + ".description"));
                display.addProperty("frame", fr.name().toLowerCase());
                display.addProperty("show_toast", parent != null);
                display.addProperty("announce_to_chat", parent != null);
                display.addProperty("hidden", hide);
                if (bg != null) display.addProperty("background", bg);

                JsonObject criteriaJson = new JsonObject();
                crit.forEach(criteriaJson::add);
                JsonArray requirements = new JsonArray();
                if (requireAll) {
                    for (String k : crit.keySet()) {
                        JsonArray one = new JsonArray();
                        one.add(k);
                        requirements.add(one);
                    }
                } else {
                    JsonArray any = new JsonArray();
                    crit.keySet().forEach(any::add);
                    requirements.add(any);
                }
                JsonObject adv = new JsonObject();
                if (parent != null) adv.addProperty("parent", parent.toString());
                adv.add("display", display);
                adv.add("criteria", criteriaJson);
                adv.add("requirements", requirements);
                if (reward > 0) {
                    JsonObject rewards = new JsonObject();
                    rewards.addProperty("experience", reward);
                    adv.add("rewards", rewards);
                }
                return adv;
            }));
            return id;
        }
    }

    private static JsonObject inventory(Object item) {
        JsonObject predicate = new JsonObject();
        predicate.addProperty("items", Refs.entry(item));
        JsonArray items = new JsonArray();
        items.add(predicate);
        JsonObject conditions = new JsonObject();
        conditions.add("items", items);
        JsonObject c = new JsonObject();
        c.addProperty("trigger", "minecraft:inventory_changed");
        c.add("conditions", conditions);
        return c;
    }

    private static JsonObject translate(String key) {
        JsonObject t = new JsonObject();
        t.addProperty("translate", key);
        return t;
    }

}
