// SPDX-FileCopyrightText: 2026 5thlayer
// SPDX-License-Identifier: MIT

package io.github._5thlayer.groundworks.client;

import java.util.Collection;
import java.util.Map;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;

import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.neoforged.neoforge.client.event.SubmitCustomGeometryEvent;
import org.joml.Vector3f;

/** A red outline round each of the given blocks, which marks what a
 * {@link PlacementPreviewEvent.Takeover} would take up, or one in a colour of Groundworks' own, round
 * a whole block or a block's own shape. */
public final class Outline {

    private static final int COLOUR = 0xFFFF4040;
    private static final float WIDTH = 2.5F;
    // A hair outside the block, or vanilla's own outline of the aimed block hides its edges.
    private static final double MARGIN = 0.004;
    private static final VoxelShape OUTLINE = Shapes.create(new AABB(0, 0, 0, 1, 1, 1).inflate(MARGIN));

    private Outline() {
    }

    public static void draw(SubmitCustomGeometryEvent event, Collection<BlockPos> positions) {
        draw(event, positions, COLOUR);
    }

    static void draw(SubmitCustomGeometryEvent event, Collection<BlockPos> positions, int colour) {
        for (BlockPos pos : positions) {
            draw(event, pos, OUTLINE, colour);
        }
    }

    /** An outline round each given shape, at its block, a hair outside it as the whole block's is. */
    static void draw(SubmitCustomGeometryEvent event, Map<BlockPos, VoxelShape> shapes, int colour) {
        shapes.forEach((pos, shape) -> {
            VoxelShape outside = Shapes.empty();
            for (AABB box : shape.toAabbs()) {
                outside = Shapes.or(outside, Shapes.create(box.inflate(MARGIN)));
            }
            draw(event, pos, outside, colour);
        });
    }

    private static void draw(SubmitCustomGeometryEvent event, BlockPos pos, VoxelShape shape, int colour) {
        Vec3 camera = event.getLevelRenderState().cameraRenderState.pos;
        PoseStack poseStack = event.getPoseStack();
        SubmitNodeCollector collector = event.getSubmitNodeCollector();
        poseStack.pushPose();
        poseStack.translate(pos.getX() - camera.x(), pos.getY() - camera.y(), pos.getZ() - camera.z());
        collector.submitCustomGeometry(poseStack, RenderTypes.lines(), (pose, buffer) ->
                shape.forAllEdges((x1, y1, z1, x2, y2, z2) -> line(buffer, pose, colour, x1, y1, z1, x2, y2, z2)));
        poseStack.popPose();
    }

    static void line(VertexConsumer buffer, PoseStack.Pose pose, int colour,
                     double x1, double y1, double z1, double x2, double y2, double z2) {
        Vector3f normal = new Vector3f((float) (x2 - x1), (float) (y2 - y1), (float) (z2 - z1)).normalize();
        buffer.addVertex(pose, (float) x1, (float) y1, (float) z1).setColor(colour).setNormal(pose, normal).setLineWidth(WIDTH);
        buffer.addVertex(pose, (float) x2, (float) y2, (float) z2).setColor(colour).setNormal(pose, normal).setLineWidth(WIDTH);
    }
}
