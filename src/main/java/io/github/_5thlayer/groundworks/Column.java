// SPDX-FileCopyrightText: 2026 5thlayer
// SPDX-License-Identifier: MIT

package io.github._5thlayer.groundworks;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;

/**
 * A Column, as Groundworks hands it to the {@link LegBuilder} that builds it (ADR 0006): a Stretch
 * straight up or down from its start, by the height Raise and Lower set after the start was stored,
 * with no Leg. Its blocks are placed as the start was, with the look stored with it, each clicked
 * on the {@linkplain #face face} of the block before it, so a chain stands on its axis and a ladder
 * faces off the wall its look finds.
 */
public record Column(BlockPos from, int height, Direction look) {

    public Column {
        from = from.immutable();
    }

    /**
     * Whether a drag with a start at {@code start} and the end aimed at {@code aimed} draws a
     * Column, for an item that stretches as one: always, unless the item also {@code stretchesInLegs},
     * when only the start's own column seen from above draws one. A stretch with anchors never does.
     */
    static boolean drawn(boolean stretchesInLegs, boolean noAnchors, BlockPos start, BlockPos aimed) {
        return noAnchors && (!stretchesInLegs || start.getX() == aimed.getX() && start.getZ() == aimed.getZ());
    }

    /** Each position, from the start to the far end. */
    public List<BlockPos> positions() {
        List<BlockPos> positions = new ArrayList<>();
        for (int i = 0; i <= Math.abs(height); i++) {
            positions.add(from.above(Integer.signum(height) * i));
        }
        return positions;
    }

    /** The face each block is clicked on: the way the column grows, up unless it is lowered. */
    public Direction face() {
        return height < 0 ? Direction.DOWN : Direction.UP;
    }
}
