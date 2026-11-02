// SPDX-License-Identifier: LGPL-3.0-or-later
package dev.steelcode.core.neoforge.impl.compat;

import dev.steelcode.core.impl.compat.ReiCompat;
import me.shedaniel.rei.forge.REIPluginClient;
import org.jetbrains.annotations.ApiStatus;

/** On NeoForge, REI discovers plugins through this annotation. */
@REIPluginClient
@ApiStatus.Internal
public class ReiCompatNeoForge extends ReiCompat {}
