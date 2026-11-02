// SPDX-License-Identifier: LGPL-3.0-or-later
package dev.steelcode.core.api.world;

import dev.architectury.registry.registries.DeferredSupplier;
import dev.architectury.registry.registries.RegistrySupplier;
import dev.steelcode.core.api.block.BlockBuilder;
import dev.steelcode.core.api.block.Drops;
import dev.steelcode.core.api.block.RenderLayer;
import dev.steelcode.core.api.block.Variant;
import dev.steelcode.core.content.block.CoreBushBlock;
import dev.steelcode.core.impl.registry.FeatureEntry;
import dev.steelcode.core.impl.registry.RegistryContext;
import dev.steelcode.core.impl.util.Refs;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DoublePlantBlock;
import net.minecraft.world.level.block.FlowerBlock;
import net.minecraft.world.level.block.FlowerPotBlock;
import net.minecraft.world.level.levelgen.GenerationStep;
import org.jetbrains.annotations.ApiStatus;

import java.util.Objects;
import java.util.Optional;

/**
 * Flowers, two-block-tall plants and bushes. Obtain one with {@code REG.plant(name)}.
 * <pre>{@code
 * // Flower with pot, dye and suspicious-stew effect; generates in forests
 * REG.plant("bluebell").flower(MobEffects.NIGHT_VISION, 5).dye(DyeColor.BLUE).generate("minecraft:is_forest", 16).build();
 *
 * // Tall flower (two blocks): textures giant_bluebell_bottom / giant_bluebell_top
 * REG.plant("giant_bluebell").flower(MobEffects.NIGHT_VISION, 5).tall().dye(DyeColor.BLUE).build();
 *
 * // Bush that only shears collect and that is replaced when building over it
 * REG.plant("dry_shrub").bush().generate("minecraft:is_badlands", 4).build();
 * }</pre>
 * Texture: {@code block/<name>} (flat item and pot use it too).
 */
public final class PlantBuilder {

    private enum Kind { FLOWER, BUSH }

    private final RegistryContext registry;
    private final String name;
    private Kind kind = Kind.FLOWER;
    private boolean tall;
    private Holder<MobEffect> stewEffect = MobEffects.SATURATION;
    private float stewSeconds = 0.35F;
    private DyeColor dye;
    private String generateBiomes;
    private int rarity;
    private boolean potted = true;
    private int light = -1;
    private DeferredSupplier<CreativeModeTab> tab;

    /** Internal: use {@code REG.plant(name)}. */
    @ApiStatus.Internal
    public PlantBuilder(RegistryContext registry, String name) {
        this.registry = registry;
        this.name = Objects.requireNonNull(name);
    }

    /**
     * A flower: flower tags (bees, small/tall flowers) and a suspicious-stew effect.
     * @param stewEffect effect given by suspicious stew made with it
     * @param seconds    effect duration
     * @return this builder
     */
    public PlantBuilder flower(Holder<MobEffect> stewEffect, float seconds) {
        this.kind = Kind.FLOWER;
        this.stewEffect = stewEffect;
        this.stewSeconds = seconds;
        return this;
    }

    /**
     * A bush or grass: only shears collect it and placing blocks replaces it.
     * @return this builder
     */
    public PlantBuilder bush() {
        this.kind = Kind.BUSH;
        this.potted = false;
        return this;
    }

    /**
     * Two blocks tall (like sunflowers or lilacs). Tall plants have no pot.
     * @return this builder
     */
    public PlantBuilder tall() {
        this.tall = true;
        this.potted = false;
        return this;
    }

    /**
     * Adds a recipe: the flower gives 1 dye (2 if tall).
     * @param color the dye color
     * @return this builder
     */
    public PlantBuilder dye(DyeColor color) {
        this.dye = color;
        return this;
    }

    /**
     * Generates patches in the world.
     * @param biomeTag biome tag, e.g. {@code "minecraft:is_forest"}
     * @param rarity   one patch every {@code rarity} chunks (vanilla flowers: 32)
     * @return this builder
     */
    public PlantBuilder generate(String biomeTag, int rarity) {
        this.generateBiomes = biomeTag;
        this.rarity = rarity;
        return this;
    }

    /**
     * Creates no flower pot.
     * @return this builder
     */
    public PlantBuilder noPot() {
        this.potted = false;
        return this;
    }

    /**
     * @param level light emitted by the plant, 0-15
     * @return this builder
     */
    public PlantBuilder light(int level) {
        this.light = level;
        return this;
    }

    /**
     * @param tab creative tab of the plant
     * @return this builder
     */
    public PlantBuilder tab(DeferredSupplier<CreativeModeTab> tab) {
        this.tab = tab;
        return this;
    }

    /**
     * Registers the plant (and its pot) and queues its datagen.
     * @return the created blocks
     */
    public PlantSet build() {
        boolean flower = kind == Kind.FLOWER;
        BlockBuilder b = registry.block(name).naming("self", name).renderLayer(RenderLayer.CUTOUT);
        if (light >= 0) b.light(light);
        if (tab != null) b.tab(tab);
        if (tall) {
            b.copy(flower ? Blocks.LILAC : Blocks.TALL_GRASS).factory(DoublePlantBlock::new).model(Variant.DOUBLE_PLANT)
                    .drops(Drops.likeVanilla("lilac", "minecraft:lilac", Drops.SELF));
            if (flower) b.tag("minecraft:tall_flowers").itemTag("minecraft:tall_flowers");
        } else if (flower) {
            Holder<MobEffect> effect = stewEffect;
            float seconds = stewSeconds;
            b.copy(Blocks.POPPY).factory(p -> new FlowerBlock(effect, seconds, p)).cross()
                    .tag("minecraft:small_flowers").itemTag("minecraft:small_flowers");
        } else {
            b.copy(Blocks.SHORT_GRASS).factory(CoreBushBlock::new).cross().drops(Drops.shearsOnly())
                    .tag("minecraft:replaceable_by_trees").tag("minecraft:sword_efficient");
        }
        b.compostable(flower ? 0.65F : 0.3F);
        RegistrySupplier<Block> plant = b.build();

        RegistrySupplier<Block> pot = null;
        if (potted) {
            pot = registry.block("potted_" + name).copy(Blocks.POTTED_POPPY)
                    .factory(p -> new FlowerPotBlock(plant.get(), p))
                    .model(Variant.POTTED).texture("block/" + name).renderLayer(RenderLayer.CUTOUT)
                    .naming("potted_plant", name).noItem().tag("minecraft:flower_pots")
                    .drops(Drops.likeVanilla("potted_poppy", "minecraft:poppy", plant, "minecraft:potted_poppy", Drops.SELF))
                    .build();
            RegistrySupplier<Block> potRef = pot;
            registry.afterRegister(() -> plant.listen(pl -> potRef.listen(p -> dev.steelcode.core.impl.platform.CorePlatform.pottedPlant(pl, p))));
        }
        if (dye != null) {
            registry.autoRecipes().shapeless("minecraft:" + dye.getName() + "_dye", tall ? 2 : 1).requires(plant)
                    .save(dye.getName() + "_dye_from_" + name);
        }
        if (generateBiomes != null) {
            ResourceLocation id = ResourceLocation.fromNamespaceAndPath(registry.modId(), "patch_" + name);
            boolean isTall = tall;
            String type = flower && !tall ? "minecraft:flower" : "minecraft:random_patch";
            int r = rarity;
            registry.addFeature(new FeatureEntry(id, () -> Worldgen.patchFeature(type, plant.getId(), isTall, isTall ? 96 : 64),
                    () -> Worldgen.surfacePlacement(id, r), Refs.parse("minecraft", generateBiomes.replace("#", "")),
                    GenerationStep.Decoration.VEGETAL_DECORATION));
        }
        return new PlantSet(plant, Optional.ofNullable(pot));
    }

    /**
     * What {@link PlantBuilder} creates.
     * @param plant  the plant block
     * @param potted the potted plant, unless the plant is tall or {@code noPot()} was used
     */
    public record PlantSet(RegistrySupplier<Block> plant, Optional<RegistrySupplier<Block>> potted) {
    }
}
