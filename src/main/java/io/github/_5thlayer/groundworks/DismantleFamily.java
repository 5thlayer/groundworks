// SPDX-FileCopyrightText: 2026 5thlayer
// SPDX-License-Identifier: MIT

package io.github._5thlayer.groundworks;

import java.util.List;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

/**
 * A kind of connected block that a Dismantle takes up as one span, supplied by a Consumer and
 * {@linkplain Dismantles#register registered} at mod construction.
 *
 * <p>Groundworks runs the gesture, the stored start, the removal and the preview for every family
 * alike, and a family answers only what is particular to its blocks. A span never crosses from one
 * family to another: an end the start's family doesn't {@linkplain #claims claim} is refused as
 * {@linkplain Refusal.Dismantle#NOT_SAME_KIND not the same kind} before the family is asked.
 */
public interface DismantleFamily {

    /** Whether this block is one of the family's, and so can be a span's start or end. */
    boolean claims(BlockState state);

    /**
     * The block a click on {@code clicked} names, such as the tile a wedge stands under. It is the
     * clicked block itself by default. Whether it is a member is still asked of {@link #claims}.
     */
    default BlockPos names(BlockGetter level, BlockPos clicked) {
        return clicked;
    }

    /**
     * The span from {@code start} to {@code end}, both of them members: what it takes, what it
     * draws, or a refusal of the family's own. Asked on both sides, by the preview each frame and by
     * the click on the server, which is the authority, so it reads the world without changing it.
     */
    DismantleSpan span(Level level, BlockPos start, BlockPos end);

    /** What the player is told when a span is refused with one of the family's own refusals. */
    Component message(Refusal refusal);

    /**
     * Whether a stored start is still the start that was stored, given the block stored with it and
     * the block there now. A start that isn't is no start. By default it is the same block.
     */
    default boolean isSameStart(BlockState stored, BlockState now) {
        return stored.getBlock() == now.getBlock();
    }

    /**
     * Run once over every position a span takes, before any of them is removed, returning what the
     * player is handed on top of the blocks' own drops, such as the items a belt carries. Runs in
     * creative too, where nothing is handed over. By default it hands over nothing.
     */
    default List<ItemStack> beforeTaking(Level level, List<BlockPos> taken) {
        return List.of();
    }
}
