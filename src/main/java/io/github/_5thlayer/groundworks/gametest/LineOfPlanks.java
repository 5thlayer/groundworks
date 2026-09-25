// SPDX-FileCopyrightText: 2026 5thlayer
// SPDX-License-Identifier: MIT

package io.github._5thlayer.groundworks.gametest;

import java.util.ArrayList;
import java.util.List;

import io.github._5thlayer.groundworks.Leg;
import io.github._5thlayer.groundworks.LegBuilder;
import io.github._5thlayer.groundworks.PlacementPlan;
import io.github._5thlayer.groundworks.Refusal;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;

/**
 * The legs of the tests' own stretch-able item: a line of oak planks, whose rise goes straight up
 * or down in place at the leg's first anchor, as a pipe's would. It replaces a birch plank, as a
 * belt replaces another tier's tile, and is refused at the first spot where anything else that
 * isn't replaceable stands.
 */
final class LineOfPlanks implements LegBuilder {

    static final BlockState PLANK = Blocks.OAK_PLANKS.defaultBlockState();

    enum Blocked implements Refusal {
        BLOCKED
    }

    @Override
    public boolean claims(Item item) {
        return item == GroundworksGameTests.STRETCHES_PLANKS.get();
    }

    @Override
    public PlacementPlan build(Level level, Item item, Leg leg) {
        List<PlacementPlan.Placed> blocks = new ArrayList<>();
        List<BlockPos> replaces = new ArrayList<>();
        Refusal refusal = null;
        BlockPos from = leg.from();
        int step = Integer.signum(leg.rise());
        for (int dy = 0; dy != leg.rise(); dy += step) {
            refusal = place(level, from.above(dy), blocks, replaces, refusal);
        }
        for (Leg.Column column : leg.route()) {
            refusal = place(level, new BlockPos(column.x(), from.getY() + leg.rise(), column.z()), blocks, replaces, refusal);
        }
        return new PlacementPlan(blocks, replaces, refusal);
    }

    private static @Nullable Refusal place(Level level, BlockPos pos, List<PlacementPlan.Placed> blocks,
                                           List<BlockPos> replaces, @Nullable Refusal refusal) {
        BlockState there = level.getBlockState(pos);
        blocks.add(new PlacementPlan.Placed(pos, PLANK));
        if (there.is(Blocks.BIRCH_PLANKS)) {
            replaces.add(pos);
        } else if (refusal == null && (!there.canBeReplaced() || !level.isInWorldBounds(pos))) {
            return new Refusal.At(Blocked.BLOCKED, pos);
        }
        return refusal;
    }

    @Override
    public Component message(Refusal refusal) {
        return Component.translatableWithFallback("message.groundworks.gametest_blocked", "Something stands in the way.");
    }
}
