// SPDX-License-Identifier: LGPL-3.0-or-later
package dev.steelcode.core.api.world;

/** Tree shape: the vanilla configured feature is copied with your log and leaves swapped in. */
public enum TreeShape {
    /** Like oak trees. */
    OAK("oak", "oak_log", "oak_leaves"),
    /** Like large (fancy) oak trees. */
    FANCY_OAK("fancy_oak", "oak_log", "oak_leaves"),
    /** Like birch trees. */
    BIRCH("birch", "birch_log", "birch_leaves"),
    /** Like spruce trees. */
    SPRUCE("spruce", "spruce_log", "spruce_leaves"),
    /** Like acacia trees. */
    ACACIA("acacia", "acacia_log", "acacia_leaves"),
    /** Like cherry trees. */
    CHERRY("cherry", "cherry_log", "cherry_leaves"),
    /** Like small jungle trees (without vines). */
    JUNGLE("jungle_tree_no_vine", "jungle_log", "jungle_leaves");

    final String template;
    final String log;
    final String leaves;

    TreeShape(String template, String log, String leaves) {
        this.template = template;
        this.log = log;
        this.leaves = leaves;
    }
}
