// SPDX-FileCopyrightText: 2026 5thlayer
// SPDX-License-Identifier: MIT

package io.github._5thlayer.placementpreview.client;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.HitResult;
import net.neoforged.neoforge.client.event.SubmitCustomGeometryEvent;
import org.jspecify.annotations.Nullable;

/** What one frame's preview is about: who aims what, where, and where to submit the drawing. */
record Aim(SubmitCustomGeometryEvent geometry, ClientLevel level, LocalPlayer player, ItemStack stack,
           @Nullable HitResult hit) {
}
