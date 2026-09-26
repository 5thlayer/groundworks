// SPDX-FileCopyrightText: 2026 5thlayer
// SPDX-License-Identifier: MIT

package io.github._5thlayer.groundworks.client;

import java.util.List;

import com.mojang.blaze3d.vertex.PoseStack;
import io.github._5thlayer.groundworks.StoredStretch;
import io.github._5thlayer.groundworks.Stretches;

import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.event.SubmitCustomGeometryEvent;

/**
 * The Stretch's markers: while a stretch is being drawn with the held stack, an outline round its
 * start with an arrow the way its first leg heads, and an outline round each anchor added since. A
 * Marker, so they stay whatever the aim while the player looks for the end. The stretch itself is
 * the Placement Preview's, as the plan a click would lay.
 *
 * <p>Before one is, while the player sneaks, the start a sneak-click would store is drawn the same
 * way at the aim, with the look turned by Rotate, so the way the stretch will run is seen before it
 * starts.
 */
final class StretchPreview {

    private static final int COLOUR = 0xC0FFFFFF;
    // Just over the block's top, so the arrow isn't hidden in the outline's own face.
    private static final double ARROW_HEIGHT = 1.004;

    private StretchPreview() {
    }

    static void onMarker(PlacementPreviewEvent.Marker event) {
        ItemStack stack = event.getStack();
        if (Stretches.builderOf(stack.getItem()) == null) {
            return;
        }
        SubmitCustomGeometryEvent geometry = event.getGeometry();
        StoredStretch stored = Stretches.storedOn(event.getLevel(), stack);
        if (stored == null) {
            StoredStretch started = event.getHitResult() instanceof BlockHitResult hit && hit.getType() == HitResult.Type.BLOCK
                    ? Stretches.startedAt(event.getLevel(), event.getPlayer(), stack, hit)
                    : null;
            if (started != null) {
                Outline.draw(geometry, List.of(started.start()), COLOUR);
                arrow(geometry, started.start(), started.look());
            }
            return;
        }
        Outline.draw(geometry, stored.anchorPositions(), COLOUR);
        Outline.draw(geometry, List.of(stored.start()), COLOUR);
        arrow(geometry, stored.start(), stored.look());
    }

    private static void arrow(SubmitCustomGeometryEvent geometry, BlockPos at, Direction look) {
        Vec3 camera = geometry.getLevelRenderState().cameraRenderState.pos;
        PoseStack poseStack = geometry.getPoseStack();
        poseStack.pushPose();
        poseStack.translate(at.getX() - camera.x(), at.getY() - camera.y(), at.getZ() - camera.z());
        geometry.getSubmitNodeCollector().submitCustomGeometry(poseStack, RenderTypes.lines(), (pose, buffer) -> {
            double tipX = 0.5 + 0.35 * look.getStepX();
            double tipZ = 0.5 + 0.35 * look.getStepZ();
            Outline.line(buffer, pose, COLOUR, 0.5 - 0.35 * look.getStepX(), ARROW_HEIGHT, 0.5 - 0.35 * look.getStepZ(),
                    tipX, ARROW_HEIGHT, tipZ);
            Direction left = look.getCounterClockWise();
            for (int side : new int[] {1, -1}) {
                Outline.line(buffer, pose, COLOUR, tipX, ARROW_HEIGHT, tipZ,
                        tipX - 0.2 * look.getStepX() + side * 0.2 * left.getStepX(), ARROW_HEIGHT,
                        tipZ - 0.2 * look.getStepZ() + side * 0.2 * left.getStepZ());
            }
        });
        poseStack.popPose();
    }
}
