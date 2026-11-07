// SPDX-FileCopyrightText: 2026 5thlayer
// SPDX-License-Identifier: MIT

package io.github._5thlayer.groundworks.client;

import java.util.List;

import io.github._5thlayer.groundworks.Blueprints;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;

/**
 * A sheet's tooltip: what {@link Blueprints#tooltipOf} says of the stack, drawn under its name
 * from the stack's own components, so a sheet in a chest or a far slot says what it holds with
 * nothing asked of the server.
 */
final class SheetTooltip {

    private SheetTooltip() {
    }

    static void onTooltip(ItemTooltipEvent event) {
        List<Component> lines = Blueprints.tooltipOf(event.getItemStack());
        if (!lines.isEmpty()) {
            event.getToolTip().addAll(Math.min(1, event.getToolTip().size()), lines);
        }
    }
}
