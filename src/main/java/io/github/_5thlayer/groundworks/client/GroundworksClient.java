// SPDX-FileCopyrightText: 2026 5thlayer
// SPDX-License-Identifier: MIT

package io.github._5thlayer.groundworks.client;

import io.github._5thlayer.groundworks.Groundworks;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;

/**
 * The client half: the Placement Preview, which has no keybind, no toggle and no state beyond its
 * own cache, and the Dismantle's preview, which takes the frame before any Consumer's Takeover.
 */
@Mod(value = Groundworks.MOD_ID, dist = Dist.CLIENT)
public final class GroundworksClient {

    public GroundworksClient() {
        NeoForge.EVENT_BUS.addListener(PlacementPreview::onSubmitGeometry);
        NeoForge.EVENT_BUS.addListener(EventPriority.HIGHEST, DismantlePreview::onTakeover);
    }
}
