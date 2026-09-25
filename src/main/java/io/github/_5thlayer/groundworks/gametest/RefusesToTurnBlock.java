// SPDX-FileCopyrightText: 2026 5thlayer
// SPDX-License-Identifier: MIT

package io.github._5thlayer.groundworks.gametest;

import com.mojang.serialization.MapCodec;
import io.github._5thlayer.groundworks.TurnsInPlace;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;

/**
 * A block that refuses Rotate in Place by its own contract, as a Consumer's machine that turns only
 * whole does. It faces a way, and vanilla would turn it, so a refusal is the contract's and not
 * vanilla's. Vanilla has no such block, so this is the tests' one block of their own.
 */
final class RefusesToTurnBlock extends HorizontalDirectionalBlock implements TurnsInPlace {

    static final String REASON = "message.groundworks.gametest.refuses_to_turn";

    private static final MapCodec<RefusesToTurnBlock> CODEC = simpleCodec(RefusesToTurnBlock::new);

    RefusesToTurnBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected MapCodec<? extends HorizontalDirectionalBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    @Override
    public Verdict<BlockState> turnInPlace(BlockState state, Level level, BlockPos pos, boolean reverse) {
        return TurnsInPlace.refused(REASON);
    }
}
