// SPDX-FileCopyrightText: 2026 5thlayer
// SPDX-License-Identifier: MIT

package io.github._5thlayer.groundworks.client;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import io.github._5thlayer.groundworks.PlacementPlan;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * The mark on each position a {@link PlacementPlan} {@linkplain PlacementPlan#clears clears}: an
 * amber {@link Outline} round the block that goes, the grass beside a golem's legs, so its going is no
 * surprise. Amber, apart from the plan's white, blue and red, and drawn as lines rather than a
 * tint, since nothing is placed there to tint.
 */
final class ClearMark {

    /** Amber, opaque as every outline is. */
    static final int COLOUR = 0xFFFFB020;

    private ClearMark() {
    }

    /** The positions marked: what the plan clears, or none where it is refused, since then it clears nothing. */
    static List<BlockPos> marked(PlacementPlan plan) {
        return plan.isRefused() ? List.of() : plan.clears();
    }

    /**
     * Each marked position's outline, the shape of the block standing there now, or the whole block
     * where that block has no shape to outline.
     */
    static Map<BlockPos, VoxelShape> shapes(BlockGetter level, PlacementPlan plan) {
        Map<BlockPos, VoxelShape> shapes = new LinkedHashMap<>();
        for (BlockPos pos : marked(plan)) {
            VoxelShape shape = level.getBlockState(pos).getShape(level, pos);
            shapes.put(pos, shape.isEmpty() ? Shapes.block() : shape);
        }
        return shapes;
    }
}
