// SPDX-FileCopyrightText: 2026 5thlayer
// SPDX-License-Identifier: MIT

package io.github._5thlayer.groundworks.client;

import io.github._5thlayer.groundworks.Groundworks;

import net.minecraft.client.KeyMapping;
import net.minecraft.resources.Identifier;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.common.NeoForge;

/**
 * The client half: the Placement Preview, which has no toggle and no state beyond its own cache,
 * the Dismantle's preview, which takes the frame before any Consumer's Takeover, and the keys for
 * Rotate and for Raise and Lower.
 */
@Mod(value = Groundworks.MOD_ID, dist = Dist.CLIENT)
public final class GroundworksClient {

    /** Groundworks' key category, which Rotate's keys and Raise's share. */
    static final KeyMapping.Category KEYS =
            new KeyMapping.Category(Identifier.fromNamespaceAndPath(Groundworks.MOD_ID, Groundworks.MOD_ID));

    public GroundworksClient(IEventBus modBus) {
        modBus.addListener(RegisterKeyMappingsEvent.class, event -> event.registerCategory(KEYS));
        NeoForge.EVENT_BUS.addListener(PlacementPreview::onSubmitGeometry);
        NeoForge.EVENT_BUS.addListener(EventPriority.HIGHEST, DismantlePreview::onTakeover);
        modBus.addListener(RotateKeys::onRegisterKeys);
        NeoForge.EVENT_BUS.addListener(RotateKeys::onClientTick);
        modBus.addListener(RaiseKeys::onRegisterKeys);
        NeoForge.EVENT_BUS.addListener(RaiseKeys::onClientTick);
    }
}
