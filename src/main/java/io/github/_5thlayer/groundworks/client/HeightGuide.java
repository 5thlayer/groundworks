// SPDX-FileCopyrightText: 2026 5thlayer
// SPDX-License-Identifier: MIT

package io.github._5thlayer.groundworks.client;

import com.mojang.blaze3d.vertex.PoseStack;

import io.github._5thlayer.groundworks.Raise;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.event.SubmitCustomGeometryEvent;
import org.joml.Vector3f;
import org.jspecify.annotations.Nullable;

/**
 * A thin vertical line from the spot a placement would go unmoved to the spot {@linkplain Raise
 * Raise or Lower} moved it to, drawn with the Placement Preview, so a block in mid-air shows where
 * it was aimed from.
 */
record HeightGuide(BlockPos unmoved, BlockPos moved) {

    private static final int COLOUR = 0xC0FFFFFF;
    private static final float WIDTH = 2.5F;

    /** The guide for the held stack at this hit, or {@code null} if its placement is not moved. */
    static @Nullable HeightGuide at(Level level, Player player, ItemStack stack, BlockHitResult hit) {
        BlockPlaceContext context = new BlockPlaceContext(level, player, InteractionHand.MAIN_HAND, stack, hit);
        int height = Raise.heightOf(context).blocks();
        if (height == 0) {
            return null;
        }
        BlockPos moved = context.getClickedPos();
        return new HeightGuide(moved.below(height), moved);
    }

    void draw(SubmitCustomGeometryEvent event) {
        Vec3 camera = event.getLevelRenderState().cameraRenderState.pos;
        PoseStack poseStack = event.getPoseStack();
        float rise = moved.getY() - unmoved.getY();
        Vector3f normal = new Vector3f(0, Math.signum(rise), 0);
        poseStack.pushPose();
        poseStack.translate(unmoved.getX() - camera.x(), unmoved.getY() - camera.y(), unmoved.getZ() - camera.z());
        event.getSubmitNodeCollector().submitCustomGeometry(poseStack, RenderTypes.lines(), (pose, buffer) -> {
            buffer.addVertex(pose, 0.5F, 0.5F, 0.5F).setColor(COLOUR).setNormal(pose, normal).setLineWidth(WIDTH);
            buffer.addVertex(pose, 0.5F, 0.5F + rise, 0.5F).setColor(COLOUR).setNormal(pose, normal).setLineWidth(WIDTH);
        });
        poseStack.popPose();
    }
}
