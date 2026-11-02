// SPDX-License-Identifier: LGPL-3.0-or-later
package dev.steelcode.core.api.item;

import dev.architectury.registry.registries.RegistrySupplier;
import dev.steelcode.core.impl.registry.RegistryContext;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.SmithingTemplateItem;
import org.jetbrains.annotations.ApiStatus;

import java.util.List;
import java.util.Objects;

/**
 * A custom smithing template, like the netherite upgrade. Obtain one with {@code REG.smithingTemplate(name)}.
 * <pre>{@code
 * RegistrySupplier<Item> MYTHRIL_UPGRADE = REG.smithingTemplate("mythril_upgrade_smithing_template")
 *         .texts("Diamond Equipment", "Mythril Ingot", "Mythril Upgrade",
 *                "Add diamond armor, weapon, or tool", "Add Mythril Ingot")
 *         .duplicateWith(Items.DIAMOND, Blocks.DEEPSLATE)
 *         .build();
 * REG.equipment("mythril", MYTHRIL_INGOT).tools(...).upgradeFrom("minecraft:diamond", MYTHRIL_UPGRADE).build();
 * }</pre>
 * Texture: {@code item/<name>}.
 */
public final class SmithingTemplateBuilder {

    private static final List<ResourceLocation> EQUIPMENT_ICONS = List.of("empty_armor_slot_helmet", "empty_slot_sword",
                    "empty_armor_slot_chestplate", "empty_slot_pickaxe", "empty_armor_slot_leggings", "empty_slot_axe",
                    "empty_armor_slot_boots", "empty_slot_hoe", "empty_slot_shovel").stream()
            .map(n -> ResourceLocation.withDefaultNamespace("item/" + n)).toList();
    private static final List<ResourceLocation> INGOT_ICON = List.of(ResourceLocation.withDefaultNamespace("item/empty_slot_ingot"));

    private final RegistryContext registry;
    private final String name;
    private String[] texts = {"Equipment", "Ingot", "Upgrade", "Add equipment", "Add ingot"};
    private Object duplicateMaterial;
    private Object duplicateBase;

    /** Internal: use {@code REG.smithingTemplate(name)}. */
    @ApiStatus.Internal
    public SmithingTemplateBuilder(RegistryContext registry, String name) {
        this.registry = registry;
        this.name = Objects.requireNonNull(name);
    }

    /**
     * The tooltip texts, in English. Other languages: {@code REG.lang(code).set("item.<modid>.<name>.applies_to", ...)}
     * (keys {@code applies_to}, {@code ingredients}, {@code upgrade}, {@code base_slot_description},
     * {@code additions_slot_description}).
     * @param appliesTo    "Applies to:" text, e.g. {@code "Diamond Equipment"}
     * @param ingredients  "Ingredients:" text, e.g. {@code "Mythril Ingot"}
     * @param upgradeName  upgrade title
     * @param baseSlot     hint for the base slot
     * @param additionSlot hint for the addition slot
     * @return this builder
     */
    public SmithingTemplateBuilder texts(String appliesTo, String ingredients, String upgradeName,
                                         String baseSlot, String additionSlot) {
        this.texts = new String[]{appliesTo, ingredients, upgradeName, baseSlot, additionSlot};
        return this;
    }

    /**
     * Adds the duplication recipe: template + 7 {@code material} + {@code base} gives 2 templates (like netherite).
     * @param material surrounding material
     * @param base     center block
     * @return this builder
     */
    public SmithingTemplateBuilder duplicateWith(Object material, Object base) {
        this.duplicateMaterial = material;
        this.duplicateBase = base;
        return this;
    }

    /**
     * Registers the template and queues its model, texts and recipe.
     * @return the template item
     */
    public RegistrySupplier<Item> build() {
        String prefix = "item." + registry.modId() + "." + name + ".";
        String[] keys = {"applies_to", "ingredients", "upgrade", "base_slot_description", "additions_slot_description"};
        for (int i = 0; i < keys.length; i++) registry.literal(prefix + keys[i], texts[i]);
        RegistrySupplier<Item> item = registry.item(name).naming("smithing_template", name)
                .factory(p -> new SmithingTemplateItem(
                        Component.translatable(prefix + "applies_to").withStyle(ChatFormatting.BLUE),
                        Component.translatable(prefix + "ingredients").withStyle(ChatFormatting.BLUE),
                        Component.translatable(prefix + "upgrade").withStyle(ChatFormatting.GRAY),
                        Component.translatable(prefix + "base_slot_description"),
                        Component.translatable(prefix + "additions_slot_description"),
                        EQUIPMENT_ICONS, INGOT_ICON))
                .build();
        if (duplicateMaterial != null) {
            registry.autoRecipes().shaped(item, 2).pattern("#S#", "#C#", "###")
                    .define('#', duplicateMaterial).define('S', item).define('C', duplicateBase).save();
        }
        return item;
    }
}
