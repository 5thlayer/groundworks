// SPDX-FileCopyrightText: 2026 5thlayer
// SPDX-License-Identifier: MIT

package io.github._5thlayer.groundworks;

import java.util.function.Supplier;

import com.mojang.serialization.MapCodec;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.PushReaction;
import net.minecraft.world.phys.BlockHitResult;

/**
 * A Part of a {@linkplain Footprint}: one of its blocks other than its Origin (ADR 0009). Each
 * footprint has its own part block, which its Consumer registers from this class, with properties
 * that drop nothing of their own.
 *
 * <p>Not drawn, since the origin's renderer draws the whole, so it hides none of its neighbours'
 * faces, and with no block entity: its number and its footprint's {@linkplain Footprint#FACING
 * facing} name where the origin stands, so nothing is stored that a reload could lose. A piston
 * doesn't move it, since one part moved would strand the rest. Never held and never placed alone.
 * It answers for its origin: a click on it is the origin's, a lookup of energy, fluid or items is
 * forwarded to the origin, and Jade shows the origin's line. Breaking it breaks the footprint, as
 * {@link Footprint} has it.
 */
public class FootprintPartBlock extends Block {

    public static final IntegerProperty PART = IntegerProperty.create("part", 1, FootprintShape.MAX_PARTS);

    private final Supplier<Footprint> footprint;
    private final MapCodec<FootprintPartBlock> codec;

    public FootprintPartBlock(Properties properties, Supplier<Footprint> footprint) {
        super(properties.noOcclusion().pushReaction(PushReaction.BLOCK));
        this.footprint = footprint;
        this.codec = simpleCodec(props -> new FootprintPartBlock(props, footprint));
        registerDefaultState(getStateDefinition().any().setValue(Footprint.FACING, Direction.NORTH).setValue(PART, 1));
    }

    public Footprint footprint() {
        return footprint.get();
    }

    @Override
    protected MapCodec<? extends Block> codec() {
        return codec;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(Footprint.FACING, PART);
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.INVISIBLE;
    }

    @Override
    protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos,
                                          Player player, InteractionHand hand, BlockHitResult hit) {
        BlockPos origin = footprint().standingOrigin(level, pos, state);
        if (origin == null) {
            return InteractionResult.TRY_WITH_EMPTY_HAND;
        }
        return level.getBlockState(origin).useItemOn(stack, level, player, hand, hit.withPosition(origin));
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player,
                                               BlockHitResult hit) {
        BlockPos origin = footprint().standingOrigin(level, pos, state);
        if (origin == null) {
            return InteractionResult.PASS;
        }
        return level.getBlockState(origin).useWithoutItem(level, player, hit.withPosition(origin));
    }

    /**
     * A part with no block entity tells a cache of its capabilities itself when they change: when
     * it is laid, turned, or goes, since what it forwards is its origin's.
     */
    @Override
    protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean movedByPiston) {
        level.invalidateCapabilities(pos);
    }

    /** A player's break breaks the origin with their tool, then goes itself. */
    @Override
    public boolean onDestroyedByPlayer(BlockState state, Level level, BlockPos pos, Player player, ItemStack toolStack,
                                       boolean willHarvest, FluidState fluid) {
        if (level instanceof ServerLevel server && !Footprint.relaying()) {
            footprint().partBroken(server, pos, state, player, toolStack, willHarvest);
        }
        return super.onDestroyedByPlayer(state, level, pos, player, toolStack, willHarvest, fluid);
    }

    /** Going any other way, by an explosion or a command, breaks the origin with no tool. */
    @Override
    protected void affectNeighborsAfterRemoval(BlockState state, ServerLevel level, BlockPos pos, boolean movedByPiston) {
        level.invalidateCapabilities(pos);
        if (!Footprint.relaying()) {
            footprint().partBroken(level, pos, state, null, ItemStack.EMPTY, true);
        }
    }
}
