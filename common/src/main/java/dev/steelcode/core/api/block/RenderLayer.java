// SPDX-License-Identifier: LGPL-3.0-or-later
package dev.steelcode.core.api.block;

/**
 * Client render layer of a block. SteelCodeCore registers it on both loaders.
 * Doors, trapdoors, panes, leaves, plants and ladders pick {@link #CUTOUT} automatically.
 */
public enum RenderLayer {
    /** Opaque (default). */
    SOLID,
    /** Fully transparent holes (doors, leaves, plants). */
    CUTOUT,
    /** Like {@link #CUTOUT} with mipmaps, for blocks seen from far away (leaves). */
    CUTOUT_MIPPED,
    /** Semi-transparent (stained glass, ice). */
    TRANSLUCENT
}
