// SPDX-FileCopyrightText: 2026 5thlayer
// SPDX-License-Identifier: MIT

package io.github._5thlayer.groundworks.client;

import java.util.List;

import io.github._5thlayer.groundworks.BlueprintCorner;
import io.github._5thlayer.groundworks.BlueprintSelection;
import io.github._5thlayer.groundworks.Blueprints;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import org.jspecify.annotations.Nullable;

/**
 * A blank sheet's Marker: its stored first Corner, outlined whatever the aim, and the Selection
 * from it to the aimed block, both included, outlined whole, with the blocks its capture would
 * {@linkplain BlueprintSelection leave out and tell of} outlined red. A paste's own preview is the
 * plan's, drawn by the {@link PlacementPreview}.
 */
final class BlueprintPreview {

    private static final int COLOUR = 0xC0FFFFFF;
    private static final int REASK_TICKS = 10;

    private static @Nullable BoundingBox shownBox;
    private static long shownAt;
    private static List<BlockPos> shown = List.of();

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
            Outline.draw(event.getGeometry(), leftOut(event, BoundingBox.fromCorners(min, max)));
        }
    }

    /**
     * The blocks the capture would leave out and tell of, asked again when the Selection changes
     * and every few ticks, since asking walks the whole box.
     */
    private static List<BlockPos> leftOut(PlacementPreviewEvent.Marker event, BoundingBox box) {
        long tick = event.getLevel().getGameTime();
        if (!box.equals(shownBox) || tick - shownAt >= REASK_TICKS || tick < shownAt) {
            shown = BlueprintSelection.shown(event.getLevel(), event.getPlayer(), box);
            shownBox = box;
            shownAt = tick;
        }
        return shown;
    }
}
