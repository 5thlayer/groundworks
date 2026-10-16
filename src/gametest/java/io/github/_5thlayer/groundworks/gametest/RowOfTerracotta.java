// SPDX-FileCopyrightText: 2026 5thlayer
// SPDX-License-Identifier: MIT

package io.github._5thlayer.groundworks.gametest;

import java.util.ArrayList;
import java.util.List;

import io.github._5thlayer.groundworks.DismantleFamily;
import io.github._5thlayer.groundworks.DismantleSpan;
import io.github._5thlayer.groundworks.Refusal;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

/**
 * The tests' own {@linkplain io.github._5thlayer.groundworks.DismantleFamily Dismantle family}: a
 * straight row of cyan terracotta, taken from start to end along one axis. A span between two
 * blocks not on one axis, or with anything else between them, is refused as off the row, as a
 * belt's span off its line is.
 */
final class RowOfTerracotta implements DismantleFamily {

    static final Block MEMBER = Blocks.CYAN_TERRACOTTA;

    enum OffRow implements Refusal {
        OFF_ROW
    }

    @Override
    public boolean claims(BlockState state) {
        return state.is(MEMBER);
    }

    @Override
    public DismantleSpan span(Level level, BlockPos start, BlockPos end) {
        int dx = Integer.signum(end.getX() - start.getX());
        int dy = Integer.signum(end.getY() - start.getY());
        int dz = Integer.signum(end.getZ() - start.getZ());
        if (Math.abs(dx) + Math.abs(dy) + Math.abs(dz) > 1) {
            return DismantleSpan.refused(OffRow.OFF_ROW);
        }
        List<BlockPos> row = new ArrayList<>();
        BlockPos pos = start;
        while (true) {
            if (!claims(level.getBlockState(pos))) {
                return DismantleSpan.refused(OffRow.OFF_ROW);
            }
            row.add(pos);
            if (pos.equals(end)) {
                return DismantleSpan.taking(row);
            }
            pos = pos.offset(dx, dy, dz);
        }
    }

    @Override
    public Component message(Refusal refusal) {
        return Component.translatable("message.groundworks.gametest_off_row");
    }
}
