// SPDX-License-Identifier: LGPL-3.0-or-later
package dev.steelcode.core.api.world;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import dev.architectury.hooks.item.tool.AxeItemHooks;
import dev.architectury.registry.registries.DeferredSupplier;
import dev.architectury.registry.registries.RegistrySupplier;
import dev.steelcode.core.api.block.BlockBuilder;
import dev.steelcode.core.api.block.BlockFamily;
import dev.steelcode.core.api.block.BlockFamilyBuilder;
import dev.steelcode.core.api.block.Drops;
import dev.steelcode.core.api.block.FamilyPreset;
import dev.steelcode.core.api.block.MiningTool;
import dev.steelcode.core.api.block.RenderLayer;
import dev.steelcode.core.api.block.Variant;
import dev.steelcode.core.api.data.Recipes;
import dev.steelcode.core.api.item.ItemBuilder;
import dev.steelcode.core.impl.datagen.VanillaData;
import dev.steelcode.core.impl.registry.BlockEntry;
import dev.steelcode.core.impl.registry.FeatureEntry;
import dev.steelcode.core.impl.registry.RegistryContext;
import dev.steelcode.core.impl.util.Refs;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.FlowerPotBlock;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.SaplingBlock;
import net.minecraft.world.level.block.grower.TreeGrower;
import net.minecraft.world.level.levelgen.GenerationStep;
import org.jetbrains.annotations.ApiStatus;

import java.util.Objects;
import java.util.Optional;
import java.util.function.UnaryOperator;

/**
 * A complete wood set in one line. Obtain one with {@code REG.wood(name)}.
 * <pre>{@code
 * WoodSet MAPLE = REG.wood("maple")
 *         .tree(TreeShape.FANCY_OAK)
 *         .generate("minecraft:is_forest", 2)        // 2 trees per chunk in forests
 *         .build();
 * }</pre>
 * Creates {@code maple_log}, {@code maple_wood}, {@code stripped_maple_log}, {@code stripped_maple_wood},
 * {@code maple_planks} with its family (stairs, slab, fence, fence gate, button, pressure plate, door, trapdoor),
 * {@code maple_leaves}, {@code maple_sapling}, {@code potted_maple_sapling}, signs, hanging signs, boats, chest boats
 * and the tree. Includes loot (leaves drop saplings and sticks like oak), tags, recipes, axe stripping,
 * flammability, composting and fuel.
 * <p>
 * Textures in {@code block/}: {@code maple_log}, {@code maple_log_top}, {@code stripped_maple_log},
 * {@code stripped_maple_log_top}, {@code maple_planks}, {@code maple_leaves}, {@code maple_sapling},
 * {@code maple_door_top}, {@code maple_door_bottom}, {@code maple_trapdoor}. In {@code item/}: {@code maple_door},
 * {@code maple_sign}, {@code maple_hanging_sign}, {@code maple_boat}, {@code maple_chest_boat}. Signs use
 * {@code entity/signs/maple.png}, {@code entity/signs/hanging/maple.png} and {@code gui/hanging_signs/maple.png}; boats use {@code entity/boat/maple.png}
 * and {@code entity/chest_boat/maple.png}.
 */
public final class WoodBuilder {

    private final RegistryContext registry;
    private final String name;
    private TreeShape shape = TreeShape.OAK;
    private boolean tintedLeaves;
    private UnaryOperator<BlockFamilyBuilder> planks = f -> f.stairs().slab().fence().fenceGate().button().pressurePlate()
            .door().trapdoor();
    private String generateBiomes;
    private java.util.function.BooleanSupplier generateIf = () -> true;
    private int perChunk;
    private int rarity;
    private int leavesLight = -1;
    private boolean signs = true;
    private boolean boats = true;
    private DeferredSupplier<CreativeModeTab> tab;

    /** Internal: use {@code REG.wood(name)}. */
    @ApiStatus.Internal
    public WoodBuilder(RegistryContext registry, String name) {
        this.registry = registry;
        this.name = Objects.requireNonNull(name);
    }

    /**
     * @param shape tree shape (default {@link TreeShape#OAK})
     * @return this builder
     */
    public WoodBuilder tree(TreeShape shape) {
        this.shape = Objects.requireNonNull(shape);
        return this;
    }

    /**
     * Leaves take the biome foliage color; paint the texture in grayscale, like oak leaves.
     * @return this builder
     */
    public WoodBuilder tintedLeaves() {
        this.tintedLeaves = true;
        return this;
    }

    /**
     * Glowing leaves.
     * @param level light level 0-15
     * @return this builder
     */
    public WoodBuilder leavesLight(int level) {
        this.leavesLight = level;
        return this;
    }

    /**
     * Variants of the planks family. Defaults to the vanilla set; use {@code f -> f.createAll()} for every shape.
     * @param planks configures the planks family
     * @return this builder
     */
    public WoodBuilder planks(UnaryOperator<BlockFamilyBuilder> planks) {
        this.planks = Objects.requireNonNull(planks);
        return this;
    }

    /**
     * Generates the tree in the world.
     * @param biomeTag biome tag, e.g. {@code "minecraft:is_forest"}
     * @param perChunk attempts per chunk
     * @return this builder
     */
    public WoodBuilder generate(String biomeTag, int perChunk) {
        this.generateBiomes = biomeTag;
        this.perChunk = perChunk;
        this.rarity = 0;
        return this;
    }

    /**
     * @param condition the tree only generates when this is true at server start (e.g. a config value)
     * @return this builder
     */
    public WoodBuilder generateIf(java.util.function.BooleanSupplier condition) {
        this.generateIf = Objects.requireNonNull(condition);
        return this;
    }

    /**
     * Generates the tree rarely.
     * @param biomeTag biome tag
     * @param chunks   one tree every {@code chunks} chunks
     * @return this builder
     */
    public WoodBuilder generateRare(String biomeTag, int chunks) {
        this.generateBiomes = biomeTag;
        this.rarity = chunks;
        this.perChunk = 0;
        return this;
    }

    /**
     * Creates no signs or hanging signs.
     * @return this builder
     */
    public WoodBuilder noSigns() {
        this.signs = false;
        return this;
    }

    /**
     * Creates no boats or chest boats.
     * @return this builder
     */
    public WoodBuilder noBoats() {
        this.boats = false;
        return this;
    }

    /**
     * Creative tab of every item of the set.
     * @param tab the tab
     * @return this builder
     */
    public WoodBuilder tab(DeferredSupplier<CreativeModeTab> tab) {
        this.tab = tab;
        return this;
    }

    // ================================================================ build

    /**
     * Registers the whole set and queues its datagen.
     * @return everything that was created
     */
    public WoodSet build() {
        String ns = registry.modId();
        ResourceLocation logsTag = ResourceLocation.fromNamespaceAndPath(ns, name + "_logs");
        String logsTagRef = "#" + logsTag;
        ResourceLocation treeId = ResourceLocation.fromNamespaceAndPath(ns, name);
        ResourceLocation saplingId = ResourceLocation.fromNamespaceAndPath(ns, name + "_sapling");
        ResourceLocation leavesId = ResourceLocation.fromNamespaceAndPath(ns, name + "_leaves");

        // ---- logs
        RegistrySupplier<Block> log = logBlock(name + "_log", "log", Blocks.OAK_LOG, null);
        RegistrySupplier<Block> wood = logBlock(name + "_wood", "wood", Blocks.OAK_WOOD, "block/" + name + "_log");
        RegistrySupplier<Block> strippedLog = logBlock("stripped_" + name + "_log", "stripped_log", Blocks.STRIPPED_OAK_LOG, null);
        RegistrySupplier<Block> strippedWood = logBlock("stripped_" + name + "_wood", "stripped_wood", Blocks.STRIPPED_OAK_WOOD,
                "block/stripped_" + name + "_log");
        for (RegistrySupplier<Block> b : java.util.List.of(log, wood, strippedLog, strippedWood)) {
            registry.addBlockTag(logsTag, b.getId().toString());
            registry.addItemTag(logsTag, b.getId().toString());
        }
        registry.addBlockTag(ResourceLocation.withDefaultNamespace("logs_that_burn"), logsTagRef);
        registry.addItemTag(ResourceLocation.withDefaultNamespace("logs_that_burn"), logsTagRef);
        registry.addBlockTag(ResourceLocation.withDefaultNamespace("overworld_natural_logs"), log.getId().toString());
        registry.afterRegister(() -> {
            log.listen(l -> strippedLog.listen(s -> AxeItemHooks.addStrippable(l, s)));
            wood.listen(w -> strippedWood.listen(s -> AxeItemHooks.addStrippable(w, s)));
        });

        // ---- planks and their family
        BlockFamilyBuilder fb = registry.family().newBase(name + "_planks", Blocks.OAK_PLANKS)
                .preset(FamilyPreset.WOOD).baseNaming("planks", name).flammable(5, 20)
                .doorTextures("block/" + name + "_door_top", "block/" + name + "_door_bottom", "item/" + name + "_door")
                .texture(Variant.TRAPDOOR, "block/" + name + "_trapdoor");
        if (tab != null) fb.tab(tab);
        BlockFamily family = planks.apply(fb).build();
        RegistrySupplier<Block> planksBlock = family.require(Variant.BASE);

        // ---- leaves
        BlockBuilder lb = registry.block(name + "_leaves").copy(Blocks.OAK_LEAVES).factory(LeavesBlock::new)
                .model(tintedLeaves ? Variant.TINTED_LEAVES : Variant.LEAVES)
                .tint(tintedLeaves ? BlockEntry.FOLIAGE_TINT : BlockEntry.NO_TINT)
                .renderLayer(RenderLayer.CUTOUT_MIPPED)
                .naming("leaves", name)
                .drops(Drops.likeVanilla("oak_leaves", "minecraft:oak_leaves", Drops.SELF, "minecraft:oak_sapling", saplingId))
                .tool(MiningTool.HOE).tag("minecraft:leaves").itemTag("minecraft:leaves")
                .flammable(30, 60).compostable(0.3F);
        if (leavesLight >= 0) lb.light(leavesLight);
        if (tab != null) lb.tab(tab);
        RegistrySupplier<Block> leaves = lb.build();

        // ---- sapling, pot and tree
        ResourceKey<net.minecraft.world.level.levelgen.feature.ConfiguredFeature<?, ?>> treeKey =
                ResourceKey.create(Registries.CONFIGURED_FEATURE, treeId);
        TreeGrower grower = new TreeGrower(treeId.toString(), Optional.empty(), Optional.of(treeKey), Optional.empty());
        BlockBuilder sb = registry.block(name + "_sapling").copy(Blocks.OAK_SAPLING)
                .factory(p -> new SaplingBlock(grower, p)).cross().naming("sapling", name)
                .tag("minecraft:saplings").itemTag("minecraft:saplings").compostable(0.3F);
        if (tab != null) sb.tab(tab);
        RegistrySupplier<Block> sapling = sb.build();
        RegistrySupplier<Block> potted = registry.block("potted_" + name + "_sapling").copy(Blocks.POTTED_OAK_SAPLING)
                .factory(p -> new FlowerPotBlock(sapling.get(), p))
                .model(Variant.POTTED).texture("block/" + name + "_sapling").renderLayer(RenderLayer.CUTOUT)
                .naming("potted", name).noItem().tag("minecraft:flower_pots")
                .drops(Drops.likeVanilla("potted_oak_sapling", "minecraft:oak_sapling", sapling,
                        "minecraft:potted_oak_sapling", Drops.SELF))
                .build();
        registry.afterRegister(() -> sapling.listen(s -> potted.listen(p -> dev.steelcode.core.impl.platform.CorePlatform.pottedPlant(s, p))));

        TreeShape sh = shape;
        String generated = generateBiomes;
        int count = perChunk, rare = rarity;
        registry.addFeature(new FeatureEntry(treeId,
                () -> VanillaData.json("worldgen/configured_feature/" + sh.template + ".json",
                        "minecraft:" + sh.log, log.getId().toString(),
                        "minecraft:" + sh.leaves, leavesId.toString()),
                generated == null ? null : () -> treePlacement(treeId, saplingId, count, rare),
                generated == null ? null : Refs.parse("minecraft", generated.startsWith("#") ? generated.substring(1) : generated),
                GenerationStep.Decoration.VEGETAL_DECORATION, generateIf));

        // ---- recipes
        Recipes r = registry.autoRecipes();
        r.shapeless(planksBlock, 4).category("building").requires(logsTagRef).unlockedBy(logsTagRef).save();
        r.shaped(wood, 3).category("building").pattern("##", "##").define('#', log).save();
        r.shaped(strippedWood, 3).category("building").pattern("##", "##").define('#', strippedLog).save();

        // ---- signs
        RegistrySupplier<net.minecraft.world.item.Item> signItem = null, hangingItem = null;
        if (signs) {
            net.minecraft.world.level.block.state.properties.WoodType woodType =
                    new net.minecraft.world.level.block.state.properties.WoodType(ns + ":" + name,
                            net.minecraft.world.level.block.state.properties.BlockSetType.OAK);
            dev.steelcode.core.impl.platform.CorePlatform.registerWoodType(woodType);
            registry.expectTexture("assets/" + ns + "/textures/entity/signs/" + name + ".png");
            registry.expectTexture("assets/" + ns + "/textures/entity/signs/hanging/" + name + ".png");
            registry.expectTexture("assets/" + ns + "/textures/gui/hanging_signs/" + name + ".png");
            String planksTex = "block/" + name + "_planks";
            String strippedTex = "block/stripped_" + name + "_log";
            RegistrySupplier<Block> sign = signBlock(name + "_sign", Variant.SIGN, planksTex, "sign", false,
                    p -> new net.minecraft.world.level.block.StandingSignBlock(woodType, p), Blocks.OAK_SIGN, null);
            RegistrySupplier<Block> wallSign = signBlock(name + "_wall_sign", Variant.WALL_SIGN, planksTex, "sign", true,
                    p -> new net.minecraft.world.level.block.WallSignBlock(woodType, p.dropsLike(sign.get())), Blocks.OAK_WALL_SIGN, null);
            RegistrySupplier<Block> hanging = signBlock(name + "_hanging_sign", Variant.HANGING_SIGN, strippedTex, "hanging_sign", false,
                    p -> new net.minecraft.world.level.block.CeilingHangingSignBlock(woodType, p), Blocks.OAK_HANGING_SIGN, null);
            RegistrySupplier<Block> wallHanging = signBlock(name + "_wall_hanging_sign", Variant.WALL_HANGING_SIGN, strippedTex,
                    "hanging_sign", true, p -> new net.minecraft.world.level.block.WallHangingSignBlock(woodType, p.dropsLike(hanging.get())),
                    Blocks.OAK_WALL_HANGING_SIGN, null);
            registry.afterRegister(() -> sign.listen(b -> dev.steelcode.core.impl.platform.CorePlatform.supportBlockEntity(
                    net.minecraft.world.level.block.entity.BlockEntityType.SIGN, () -> b)));
            registry.afterRegister(() -> wallSign.listen(b -> dev.steelcode.core.impl.platform.CorePlatform.supportBlockEntity(
                    net.minecraft.world.level.block.entity.BlockEntityType.SIGN, () -> b)));
            registry.afterRegister(() -> hanging.listen(b -> dev.steelcode.core.impl.platform.CorePlatform.supportBlockEntity(
                    net.minecraft.world.level.block.entity.BlockEntityType.HANGING_SIGN, () -> b)));
            registry.afterRegister(() -> wallHanging.listen(b -> dev.steelcode.core.impl.platform.CorePlatform.supportBlockEntity(
                    net.minecraft.world.level.block.entity.BlockEntityType.HANGING_SIGN, () -> b)));
            for (var e : java.util.Map.of("minecraft:standing_signs", sign, "minecraft:wall_signs", wallSign,
                    "minecraft:ceiling_hanging_signs", hanging, "minecraft:wall_hanging_signs", wallHanging).entrySet()) {
                registry.addBlockTag(Refs.parse("minecraft", e.getKey()), e.getValue().getId().toString());
            }
            ItemBuilder si = registry.item(name + "_sign").naming("sign", name).stacksTo(16).tag("minecraft:signs")
                    .factory(p -> new net.minecraft.world.item.SignItem(p, sign.get(), wallSign.get()));
            ItemBuilder hi = registry.item(name + "_hanging_sign").naming("hanging_sign", name).stacksTo(16)
                    .tag("minecraft:hanging_signs")
                    .factory(p -> new net.minecraft.world.item.HangingSignItem(hanging.get(), wallHanging.get(), p));
            if (tab != null) { si.tab(tab); hi.tab(tab); }
            signItem = si.build();
            hangingItem = hi.build();
            r.shaped(signItem, 3).pattern("###", "###", " X ").define('#', planksBlock).define('X', "minecraft:stick").save();
            r.shaped(hangingItem, 6).pattern("X X", "###", "###").define('#', strippedLog).define('X', "minecraft:chain").save();
        }

        // ---- boats
        RegistrySupplier<net.minecraft.world.item.Item> boatItem = null, chestBoatItem = null;
        if (boats) {
            RegistrySupplier<net.minecraft.world.item.Item>[] boatRef = newItemRef(), chestRef = newItemRef();
            var boatType = registry.<dev.steelcode.core.content.entity.CoreBoat>registerEntity(name + "_boat", () ->
                    net.minecraft.world.entity.EntityType.Builder.<dev.steelcode.core.content.entity.CoreBoat>of(
                                    (t, level) -> new dev.steelcode.core.content.entity.CoreBoat(t, level, () -> boatRef[0].get()),
                                    net.minecraft.world.entity.MobCategory.MISC)
                            .sized(1.375F, 0.5625F).eyeHeight(0.5625F).clientTrackingRange(10).build(name + "_boat"));
            var chestType = registry.<dev.steelcode.core.content.entity.CoreChestBoat>registerEntity(name + "_chest_boat", () ->
                    net.minecraft.world.entity.EntityType.Builder.<dev.steelcode.core.content.entity.CoreChestBoat>of(
                                    (t, level) -> new dev.steelcode.core.content.entity.CoreChestBoat(t, level, () -> chestRef[0].get()),
                                    net.minecraft.world.entity.MobCategory.MISC)
                            .sized(1.375F, 0.5625F).eyeHeight(0.5625F).clientTrackingRange(10).build(name + "_chest_boat"));
            ItemBuilder bi = registry.item(name + "_boat").naming("boat", name).stacksTo(1).tag("minecraft:boats")
                    .factory(p -> new dev.steelcode.core.content.item.CoreBoatItem(boatType, p));
            ItemBuilder ci = registry.item(name + "_chest_boat").naming("chest_boat", name).stacksTo(1).tag("minecraft:chest_boats")
                    .factory(p -> new dev.steelcode.core.content.item.CoreBoatItem(chestType, p));
            if (tab != null) { bi.tab(tab); ci.tab(tab); }
            boatItem = bi.build();
            chestBoatItem = ci.build();
            boatRef[0] = boatItem;
            chestRef[0] = chestBoatItem;
            r.shaped(boatItem).category("misc").pattern("# #", "###").define('#', planksBlock).save();
            r.shapeless(chestBoatItem).category("misc").requires("minecraft:chest").requires(boatItem).save();
            registry.expectTexture("assets/" + ns + "/textures/entity/boat/" + name + ".png");
            registry.expectTexture("assets/" + ns + "/textures/entity/chest_boat/" + name + ".png");
            ResourceLocation boatTex = ResourceLocation.fromNamespaceAndPath(ns, "textures/entity/boat/" + name + ".png");
            ResourceLocation chestTex = ResourceLocation.fromNamespaceAndPath(ns, "textures/entity/chest_boat/" + name + ".png");
            registry.clientEarly(() -> dev.steelcode.core.impl.client.SteelCodeCoreClient.boat(boatType, ns, name, boatTex, false));
            registry.clientEarly(() -> dev.steelcode.core.impl.client.SteelCodeCoreClient.boat(chestType, ns, name, chestTex, true));
        }

        WoodSet set = new WoodSet(name, log, wood, strippedLog, strippedWood, family, leaves, sapling, potted, logsTag, treeId,
                java.util.Optional.ofNullable(signItem), java.util.Optional.ofNullable(hangingItem),
                java.util.Optional.ofNullable(boatItem), java.util.Optional.ofNullable(chestBoatItem));
        registry.addWood(set);
        return set;
    }

    @SuppressWarnings("unchecked")
    private static RegistrySupplier<net.minecraft.world.item.Item>[] newItemRef() {
        return new RegistrySupplier[1];
    }

    private RegistrySupplier<Block> signBlock(String id, Variant variant, String particle, String pattern, boolean wall,
                                              java.util.function.Function<net.minecraft.world.level.block.state.BlockBehaviour.Properties, Block> factory,
                                              Block copy, Object unused) {
        BlockBuilder b = registry.block(id).copy(copy).factory(factory).model(variant).texture(particle)
                .naming(pattern, name).noItem().tool(MiningTool.AXE);
        if (wall) b.noLoot();
        return b.build();
    }

    private RegistrySupplier<Block> logBlock(String id, String pattern, Block copy, String sameTextureEverywhere) {
        BlockBuilder b = registry.block(id).copy(copy).pillar().naming(pattern, name).tool(MiningTool.AXE).flammable(5, 5);
        if (sameTextureEverywhere != null) b.textures(sameTextureEverywhere, sameTextureEverywhere, sameTextureEverywhere);
        if (tab != null) b.tab(tab);
        return b.build();
    }

    private static JsonObject treePlacement(ResourceLocation tree, ResourceLocation sapling, int perChunk, int rarity) {
        JsonArray p = new JsonArray();
        p.add(rarity > 0 ? Worldgen.modifier("minecraft:rarity_filter", "chance", rarity)
                : Worldgen.modifier("minecraft:count", "count", perChunk));
        p.add(Worldgen.modifier("minecraft:in_square", null, 0));
        p.add(Worldgen.modifier("minecraft:surface_water_depth_filter", "max_water_depth", 0));
        JsonObject heightmap = Worldgen.modifier("minecraft:heightmap", null, 0);
        heightmap.addProperty("heightmap", "OCEAN_FLOOR");
        p.add(heightmap);
        p.add(Worldgen.modifier("minecraft:biome", null, 0));
        JsonObject props = new JsonObject();
        props.addProperty("stage", "0");
        JsonObject state = new JsonObject();
        state.addProperty("Name", sapling.toString());
        state.add("Properties", props);
        JsonObject predicate = new JsonObject();
        predicate.addProperty("type", "minecraft:would_survive");
        predicate.add("state", state);
        JsonObject filter = Worldgen.modifier("minecraft:block_predicate_filter", null, 0);
        filter.add("predicate", predicate);
        p.add(filter);
        return Worldgen.placed(tree, p);
    }
}
