// SPDX-FileCopyrightText: 2026 5thlayer
// SPDX-License-Identifier: MIT

package io.github._5thlayer.placementpreview;

import net.neoforged.fml.common.Mod;

/**
 * The library's common half, which holds nothing: the plan and the opt-in are plain types, asked
 * on both sides. The drawing is {@code client.PlacementPreviewClient}.
 */
@Mod(PlacementPreviewMod.MOD_ID)
public final class PlacementPreviewMod {

    public static final String MOD_ID = "placementpreview";
}
