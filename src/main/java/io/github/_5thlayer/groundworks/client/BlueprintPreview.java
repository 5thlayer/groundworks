// SPDX-FileCopyrightText: 2026 5thlayer
// SPDX-License-Identifier: MIT

package io.github._5thlayer.groundworks.client;

import java.util.List;

import io.github._5thlayer.groundworks.BlueprintCorner;
import io.github._5thlayer.groundworks.Blueprints;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;

/**
 * A blank sheet's Marker: its stored first Corner, outlined whatever the aim, and the Selection
 * from it to the aimed block, both included, outlined whole. A paste's own preview is the plan's,
 * drawn by the {@link PlacementPreview}.
 */
final class BlueprintPreview {

    private static final int COLOUR = 0xC0FFFFFF;

    private BlueprintPreview() {
    }

    static void onMarker(PlacementPreviewEvent.Marker event) {
        ItemStack stack = event.getStack();
        if (!Blueprints.isSheet(stack) || Blueprints.holdsBlueprint(stack)) {
            return;
        }
        BlueprintCorner corner = Blueprints.liveCorner(event.getLevel(), stack);
        if (corner == null) {
            return;
        }
        Outline.draw(event.getGeometry(), List.of(corner.pos()), COLOUR);
        if (event.getHitResult() instanceof BlockHitResult hit && hit.getType() == HitResult.Type.BLOCK) {
            BlockPos aimed = hit.getBlockPos();
            BlockPos min = new BlockPos(Math.min(corner.pos().getX(), aimed.getX()),
                    Math.min(corner.pos().getY(), aimed.getY()), Math.min(corner.pos().getZ(), aimed.getZ()));
            BlockPos max = new BlockPos(Math.max(corner.pos().getX(), aimed.getX()),
                    Math.max(corner.pos().getY(), aimed.getY()), Math.max(corner.pos().getZ(), aimed.getZ()));
            Outline.box(event.getGeometry(), min, max, COLOUR);
        }
    }
}
