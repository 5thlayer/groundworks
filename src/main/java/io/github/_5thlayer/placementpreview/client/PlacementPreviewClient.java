// SPDX-FileCopyrightText: 2026 5thlayer
// SPDX-License-Identifier: MIT

package io.github._5thlayer.placementpreview.client;

import io.github._5thlayer.placementpreview.PlacementPreviewMod;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;

/**
 * The client half of the Placement Preview. One listener: the preview has no keybind, no toggle
 * and no state beyond its own cache.
 */
@Mod(value = PlacementPreviewMod.MOD_ID, dist = Dist.CLIENT)
public final class PlacementPreviewClient {

    public PlacementPreviewClient() {
        NeoForge.EVENT_BUS.addListener(PlacementPreview::onSubmitGeometry);
    }
}
