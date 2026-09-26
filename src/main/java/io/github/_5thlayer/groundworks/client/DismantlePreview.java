// SPDX-FileCopyrightText: 2026 5thlayer
// SPDX-License-Identifier: MIT

package io.github._5thlayer.groundworks.client;

import java.util.ArrayList;
import java.util.List;

import io.github._5thlayer.groundworks.DismantlePass;
import io.github._5thlayer.groundworks.DismantleStart;
import io.github._5thlayer.groundworks.Dismantles;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;

/**
 * The Dismantle's preview: while a dismantling tool is held with a live start or queued spans, the
 * pass a click would confirm outlined in red, every queued span and the span to the aim, or only the
 * starts where the pass would be refused. With nothing aimed at, the start is drawn alone. It takes
 * the frame, ahead of any Consumer's {@link PlacementPreviewEvent.Takeover}.
 */
final class DismantlePreview {

    private DismantlePreview() {
    }

    static void onTakeover(PlacementPreviewEvent.Takeover event) {
        ClientLevel level = event.getLevel();
        ItemStack stack = event.getStack();
        BlockPos aimed = event.getHitResult() instanceof BlockHitResult hit && hit.getType() == HitResult.Type.BLOCK
                ? hit.getBlockPos()
                : null;
        DismantlePass pass = Dismantles.passTo(level, stack, aimed);
        if (pass == null) {
            return;
        }
        List<BlockPos> drawn = new ArrayList<>(pass.isRefused() ? pass.starts() : pass.draws());
        DismantleStart start = Dismantles.liveStart(level, stack);
        if (start != null && (aimed == null || pass.isRefused()) && !drawn.contains(start.pos())) {
            drawn.add(start.pos());
        }
        Outline.draw(event.getGeometry(), drawn);
        event.setCanceled(true);
    }
}
