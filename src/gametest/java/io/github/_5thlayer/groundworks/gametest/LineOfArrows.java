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
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import org.jspecify.annotations.Nullable;

/**
 * The legs of the tests' own stretch-able item: a line of magenta glazed terracotta, whose arrow
 * points the way the leg travels through each block, as it would placed by a player looking that
 * way. A rise goes straight up or down in place at the leg's first anchor, as a pipe's would. It
 * replaces light blue glazed terracotta, as a belt replaces another tier's tile, and is refused at
 * the first spot where anything else that isn't replaceable stands.
 */
final class LineOfArrows implements LegBuilder {

    static final Block ARROW = Blocks.MAGENTA_GLAZED_TERRACOTTA;
    static final Block REPLACED = Blocks.LIGHT_BLUE_GLAZED_TERRACOTTA;

    enum Blocked implements Refusal {
        BLOCKED
    }

    /** The arrow pointing along {@code travel}: glazed terracotta faces the player who placed it. */
    static BlockState pointing(Direction travel) {
        return ARROW.defaultBlockState().setValue(BlockStateProperties.HORIZONTAL_FACING, travel.getOpposite());
    }

    @Override
    public boolean claims(Item item) {
        return item == GroundworksGameTests.STRETCHES_ARROWS.get();
    }

    @Override
    public PlacementPlan build(Level level, Item item, Leg leg) {
        List<PlacementPlan.Placed> blocks = new ArrayList<>();
        List<BlockPos> replaces = new ArrayList<>();
        Refusal refusal = null;
        BlockPos from = leg.from();
        BlockState climb = pointing(leg.route().getFirst().travel());
        int step = Integer.signum(leg.rise());
        for (int dy = 0; dy != leg.rise(); dy += step) {
            refusal = place(level, from.above(dy), climb, blocks, replaces, refusal);
        }
        for (Leg.Column column : leg.route()) {
            refusal = place(level, new BlockPos(column.x(), from.getY() + leg.rise(), column.z()),
                    pointing(column.travel()), blocks, replaces, refusal);
        }
        return new PlacementPlan(blocks, replaces, refusal);
    }

    private static @Nullable Refusal place(Level level, BlockPos pos, BlockState state, List<PlacementPlan.Placed> blocks,
                                           List<BlockPos> replaces, @Nullable Refusal refusal) {
        BlockState there = level.getBlockState(pos);
        blocks.add(new PlacementPlan.Placed(pos, state));
        if (there.is(REPLACED)) {
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
