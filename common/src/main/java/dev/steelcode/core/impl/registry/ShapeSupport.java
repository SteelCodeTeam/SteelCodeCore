// SPDX-License-Identifier: LGPL-3.0-or-later
package dev.steelcode.core.impl.registry;

import dev.steelcode.core.api.block.RenderLayer;
import dev.steelcode.core.api.block.Variant;
import dev.steelcode.core.api.data.Recipes;
import dev.steelcode.core.content.block.VerticalSlabBlock;
import dev.steelcode.core.impl.util.Refs;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.ButtonBlock;
import net.minecraft.world.level.block.CarpetBlock;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.FenceBlock;
import net.minecraft.world.level.block.FenceGateBlock;
import net.minecraft.world.level.block.IronBarsBlock;
import net.minecraft.world.level.block.PressurePlateBlock;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.TrapDoorBlock;
import net.minecraft.world.level.block.WallBlock;
import net.minecraft.world.level.block.WeightedPressurePlateBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.properties.BlockSetType;
import net.minecraft.world.level.block.state.properties.WoodType;
import net.minecraft.world.level.material.PushReaction;
import org.jetbrains.annotations.ApiStatus;

/** Shared by families and shaped single blocks: default classes, tags, fuel, render layer and recipes. */
@ApiStatus.Internal
public final class ShapeSupport {

    private ShapeSupport() {
    }

    /** Behaviour of doors, buttons and pressure plates. */
    public record Behaviour(BlockSetType setType, WoodType woodType, int buttonTicks, int weightedPlate) {
        public static Behaviour of(boolean wood, BlockSetType setType, WoodType woodType, int buttonTicks, int weightedPlate) {
            return new Behaviour(setType != null ? setType : (wood ? BlockSetType.OAK : BlockSetType.STONE),
                    woodType != null ? woodType : WoodType.OAK,
                    buttonTicks > 0 ? buttonTicks : (wood ? 30 : 20), weightedPlate);
        }
    }

    /** Property tweaks each shape needs (no collision for buttons, no occlusion for doors...). */
    public static BlockBehaviour.Properties adjust(Variant v, BlockBehaviour.Properties p) {
        return switch (v) {
            case WALL, FENCE_GATE -> p.forceSolidOn();
            case BUTTON -> p.noCollission().strength(0.5F).pushReaction(PushReaction.DESTROY);
            case PRESSURE_PLATE -> p.forceSolidOn().noCollission().strength(0.5F).pushReaction(PushReaction.DESTROY);
            case DOOR -> p.noOcclusion().pushReaction(PushReaction.DESTROY);
            case TRAPDOOR -> p.noOcclusion().isValidSpawn((state, level, pos, entity) -> false);
            case PANE -> p.noOcclusion();
            case CARPET -> p.strength(0.1F);
            default -> p;
        };
    }

    /** The vanilla (or SteelCodeCore) class of each shape; properties are already adjusted. */
    public static Block create(Variant v, Block base, BlockBehaviour.Properties p, Behaviour b) {
        return switch (v) {
            case STAIRS -> new StairBlock(base.defaultBlockState(), p);
            case SLAB -> new SlabBlock(p);
            case VERTICAL_SLAB -> new VerticalSlabBlock(p);
            case WALL -> new WallBlock(p);
            case FENCE -> new FenceBlock(p);
            case FENCE_GATE -> new FenceGateBlock(b.woodType(), p);
            case BUTTON -> new ButtonBlock(b.setType(), b.buttonTicks(), p);
            case PRESSURE_PLATE -> b.weightedPlate() > 0
                    ? new WeightedPressurePlateBlock(b.weightedPlate(), b.setType(), p)
                    : new PressurePlateBlock(b.setType(), p);
            case DOOR -> new DoorBlock(b.setType(), p);
            case TRAPDOOR -> new TrapDoorBlock(b.setType(), p);
            case PANE -> new IronBarsBlock(p);
            case CARPET -> new CarpetBlock(p);
            case PILLAR -> new RotatedPillarBlock(p);
            case BASE, POLISHED, CHISELED, CRACKED, CUT, SMOOTH, BRICKS, TILES -> new Block(p);
            default ->
                    throw new IllegalArgumentException(v + " is not a family shape");
        };
    }

    public static RenderLayer defaultRenderLayer(Variant v) {
        return switch (v) {
            case DOOR, TRAPDOOR, PANE -> RenderLayer.CUTOUT;
            default -> null;
        };
    }

    /** Vanilla wood fuel values, or {@code fixed} (half for slabs) if >= 0. */
    public static int fuel(Variant v, boolean wood, int fixed) {
        boolean half = v == Variant.SLAB || v == Variant.VERTICAL_SLAB;
        if (fixed >= 0) return half ? fixed / 2 : fixed;
        if (!wood) return 0;
        return switch (v) {
            case SLAB, VERTICAL_SLAB -> 150;
            case BUTTON -> 100;
            case DOOR -> 200;
            case CARPET -> 67;
            case PANE -> 0;
            default -> 300;
        };
    }

    /** Vanilla tags of each shape ({@code minecraft:stairs}, {@code wooden_doors}...). */
    public static void tags(RegistryContext registry, Variant v, String entry, boolean wood) {
        switch (v) {
            case STAIRS -> both(registry, wood ? "wooden_stairs" : "stairs", entry);
            case SLAB -> both(registry, wood ? "wooden_slabs" : "slabs", entry);
            case WALL -> both(registry, "walls", entry);
            case FENCE -> both(registry, wood ? "wooden_fences" : "fences", entry);
            case FENCE_GATE -> both(registry, "fence_gates", entry);
            case BUTTON -> both(registry, wood ? "wooden_buttons" : "buttons", entry);
            case PRESSURE_PLATE -> {
                if (wood) both(registry, "wooden_pressure_plates", entry);
                else registry.addBlockTag(ResourceLocation.withDefaultNamespace("pressure_plates"), entry);
            }
            case DOOR -> both(registry, wood ? "wooden_doors" : "doors", entry);
            case TRAPDOOR -> both(registry, wood ? "wooden_trapdoors" : "trapdoors", entry);
            default -> {
            }
        }
    }

    private static void both(RegistryContext registry, String vanillaTag, String entry) {
        ResourceLocation tag = ResourceLocation.withDefaultNamespace(vanillaTag);
        registry.addBlockTag(tag, entry);
        registry.addItemTag(tag, entry);
    }

    /**
     * Default recipe of a shape.
     *
     * @param in    crafting ingredient
     * @param slab  family slab (for panes, carpets and chiseled); may be {@code null}
     * @param base  base block (furnace recipe of cracked)
     */
    public static void recipe(Recipes r, Variant v, Object out, Object in, Object base, Object slab, Object stick, boolean wood) {
        Object fromSlab = slab != null ? slab : in;
        switch (v) {
            case STAIRS -> r.stairs(out, in);
            case SLAB -> r.slab(out, in);
            case VERTICAL_SLAB -> {
                if (slab != null) r.verticalSlabFromSlab(out, slab);
                else r.verticalSlab(out, in);
            }
            case WALL -> r.wall(out, in);
            case FENCE -> r.fence(out, in, stick);
            case FENCE_GATE -> r.fenceGate(out, in, stick);
            case BUTTON -> r.button(out, in);
            case PRESSURE_PLATE -> r.pressurePlate(out, in);
            case DOOR -> r.door(out, in);
            case TRAPDOOR -> {
                if (wood) r.woodenTrapdoor(out, in);
                else r.trapdoor(out, in);
            }
            case PANE -> r.pane(out, fromSlab);
            case CARPET -> r.carpet(out, fromSlab);
            case POLISHED, CUT, BRICKS -> r.polished(out, in);
            case CHISELED -> {
                if (slab != null) r.chiseled(out, slab);
            }
            case PILLAR -> {
                if (!wood) r.pillar(out, in);
            }
            case CRACKED -> r.cracked(out, base);
            case BASE, SMOOTH, TILES -> {
            }
        }
    }

    /** Stonecutter yield for shapes (if {@code shapes}) and decorative blocks (always). */
    public static int stonecutterYield(Variant v, boolean shapes) {
        return switch (v) {
            case STAIRS, WALL -> shapes ? 1 : 0;
            case SLAB, VERTICAL_SLAB -> shapes ? 2 : 0;
            case POLISHED, CHISELED, CUT, SMOOTH, BRICKS, TILES, PILLAR -> 1;
            default -> 0;
        };
    }

    public static String lang(String id) {
        return Refs.titleCase(id);
    }
}
