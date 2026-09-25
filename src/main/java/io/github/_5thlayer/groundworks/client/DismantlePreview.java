// SPDX-FileCopyrightText: 2026 5thlayer
// SPDX-License-Identifier: MIT

package io.github._5thlayer.groundworks.client;

import java.util.List;

import io.github._5thlayer.groundworks.DismantleSpan;
import io.github._5thlayer.groundworks.DismantleStart;
import io.github._5thlayer.groundworks.Dismantles;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;

/**
 * The Dismantle's preview: while a dismantling tool is held with a live start, the span to the aim
 * outlined in red, or only the start where the span would be refused or nothing is aimed at. It
 * takes the frame, ahead of any Consumer's {@link PlacementPreviewEvent.Takeover}.
 */
final class DismantlePreview {

    private DismantlePreview() {
    }

    static void onTakeover(PlacementPreviewEvent.Takeover event) {
        ClientLevel level = event.getLevel();
        ItemStack stack = event.getStack();
        if (!Dismantles.isTool(stack)) {
            return;
        }
        DismantleStart start = Dismantles.liveStart(level, stack);
        if (start == null) {
            return;
        }
        DismantleSpan span = event.getHitResult() instanceof BlockHitResult hit && hit.getType() == HitResult.Type.BLOCK
                ? Dismantles.spanTo(level, stack, hit.getBlockPos())
                : null;
        Outline.draw(event.getGeometry(), span == null || span.isRefused() ? List.of(start.pos()) : span.draws());
        event.setCanceled(true);
    }
}
