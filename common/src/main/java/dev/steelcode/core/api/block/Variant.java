// SPDX-License-Identifier: LGPL-3.0-or-later
package dev.steelcode.core.api.block;

import java.util.Arrays;
import java.util.EnumSet;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * The kinds of block a family can derive from its base block, plus special model generators.
 * <p>
 * {@link #kind()} groups them:
 * <ul>
 *     <li>{@link Kind#SHAPE} ({@link #shapes()}): reuse the base texture, so they work without any new PNG.
 *     {@code createAll()} creates these.</li>
 *     <li>{@link Kind#DECORATIVE} ({@link #decorative()}): full blocks with their own texture
 *     ({@code polished_x}, {@code x_bricks}...). You provide the PNG. {@code createDecorative()} creates these.</li>
 *     <li>{@link Kind#SPECIAL}: model generators used by wood sets, colors, plants and single blocks; they are
 *     not family variants.</li>
 * </ul>
 */
public enum Variant {
    /** The family's full block (only registered when the family creates it with {@code newBase}). */
    BASE("%s", Kind.BASE),

    // -------- shapes (base texture)
    /** Stairs, {@code <prefix>_stairs}. */
    STAIRS("%s_stairs", Kind.SHAPE),
    /** Slab (and double slab), {@code <prefix>_slab}. */
    SLAB("%s_slab", Kind.SHAPE),
    /** Vertical slab, {@code <prefix>_vertical_slab}; uses {@link dev.steelcode.core.content.block.VerticalSlabBlock}. */
    VERTICAL_SLAB("%s_vertical_slab", Kind.SHAPE),
    /** Wall, {@code <prefix>_wall}. */
    WALL("%s_wall", Kind.SHAPE),
    /** Fence, {@code <prefix>_fence}. */
    FENCE("%s_fence", Kind.SHAPE),
    /** Fence gate, {@code <prefix>_fence_gate}. */
    FENCE_GATE("%s_fence_gate", Kind.SHAPE),
    /** Button, {@code <prefix>_button}. */
    BUTTON("%s_button", Kind.SHAPE),
    /** Pressure plate, {@code <prefix>_pressure_plate}. */
    PRESSURE_PLATE("%s_pressure_plate", Kind.SHAPE),
    /** Door, {@code <prefix>_door}. */
    DOOR("%s_door", Kind.SHAPE),
    /** Trapdoor, {@code <prefix>_trapdoor}. */
    TRAPDOOR("%s_trapdoor", Kind.SHAPE),
    /** Pane, {@code <prefix>_pane}. */
    PANE("%s_pane", Kind.SHAPE),
    /** Carpet, {@code <prefix>_carpet}. */
    CARPET("%s_carpet", Kind.SHAPE),

    // -------- decorative (own texture: <modid>:block/<id>)
    /** {@code polished_<prefix>}; needs its own texture. */
    POLISHED("polished_%s", Kind.DECORATIVE),
    /** {@code chiseled_<prefix>}; needs its own texture. */
    CHISELED("chiseled_%s", Kind.DECORATIVE),
    /** {@code cracked_<prefix>}; needs its own texture. */
    CRACKED("cracked_%s", Kind.DECORATIVE),
    /** {@code cut_<prefix>}; needs its own texture. */
    CUT("cut_%s", Kind.DECORATIVE),
    /** {@code smooth_<prefix>}; needs its own texture. */
    SMOOTH("smooth_%s", Kind.DECORATIVE),
    /** {@code <prefix>_bricks}; needs its own texture. */
    BRICKS("%s_bricks", Kind.DECORATIVE),
    /** {@code <prefix>_tiles}; needs its own texture. */
    TILES("%s_tiles", Kind.DECORATIVE),
    /** Orientable pillar {@code <prefix>_pillar}; textures {@code <id>} (side) and {@code <id>_top} (ends). */
    PILLAR("%s_pillar", Kind.DECORATIVE),

    // -------- special models (used by wood, colors and single blocks; not family variants)
    /** Untinted leaves ({@code cube_all}). */
    LEAVES("%s_leaves", Kind.SPECIAL),
    /** Leaves tinted with the biome foliage color ({@code block/leaves}). */
    TINTED_LEAVES("%s_leaves", Kind.SPECIAL),
    /** Cross-shaped plant (saplings, flowers). */
    CROSS("%s", Kind.SPECIAL),
    /** Potted plant; uses the plant texture. */
    POTTED("potted_%s", Kind.SPECIAL),
    /** Tinted cube (colors from one grayscale texture). */
    TINTED_CUBE("%s", Kind.SPECIAL),
    /** Standing sign; the model only has a particle texture, the vanilla renderer draws it. */
    SIGN("%s_sign", Kind.SPECIAL),
    /** Wall sign. */
    WALL_SIGN("%s_wall_sign", Kind.SPECIAL),
    /** Hanging sign. */
    HANGING_SIGN("%s_hanging_sign", Kind.SPECIAL),
    /** Wall hanging sign. */
    WALL_HANGING_SIGN("%s_wall_hanging_sign", Kind.SPECIAL),
    /** Crop with growth stages ({@code age}). */
    CROP("%s", Kind.SPECIAL),
    /** Two-block plant ({@code half=lower/upper}); textures {@code <id>_bottom} and {@code <id>_top}. */
    DOUBLE_PLANT("%s", Kind.SPECIAL),
    /** Redstone lamp; textures {@code <id>} and {@code <id>_on}. */
    LAMP("%s", Kind.SPECIAL),
    /** Ladder. */
    LADDER("%s", Kind.SPECIAL),
    /** Block facing the player; textures {@code <id>_front}, {@code <id>_side} and {@code <id>_top}. */
    ORIENTABLE("%s", Kind.SPECIAL),
    /** Connected textures; {@code <id>} (bordered) and {@code <id>_center} (borderless). */
    CONNECTED("%s", Kind.SPECIAL);

    /** Groups of variants. */
    public enum Kind {
        /** The base block itself. */
        BASE,
        /** Shapes that reuse the base texture. */
        SHAPE,
        /** Full decorative blocks with their own texture. */
        DECORATIVE,
        /** Special model generators, not family variants. */
        SPECIAL
    }

    private final String pattern;
    private final Kind kind;

    Variant(String pattern, Kind kind) {
        this.pattern = pattern;
        this.kind = kind;
    }

    /**
     * Registry path of this variant for a prefix: {@code STAIRS.idFor("gold")} is {@code "gold_stairs"}.
     *
     * @param prefix family prefix
     * @return the id path
     */
    public String idFor(String prefix) {
        return pattern.formatted(prefix);
    }

    /** @return the group of this variant */
    public Kind kind() {
        return kind;
    }

    /** @return every {@link Kind#SHAPE} variant (new mutable set) */
    public static Set<Variant> shapes() {
        return of(Kind.SHAPE);
    }

    /** @return every {@link Kind#DECORATIVE} variant (new mutable set) */
    public static Set<Variant> decorative() {
        return of(Kind.DECORATIVE);
    }

    private static Set<Variant> of(Kind kind) {
        return Arrays.stream(values()).filter(v -> v.kind == kind)
                .collect(Collectors.toCollection(() -> EnumSet.noneOf(Variant.class)));
    }
}
