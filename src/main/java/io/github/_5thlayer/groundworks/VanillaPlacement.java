// SPDX-FileCopyrightText: 2026 5thlayer
// SPDX-License-Identifier: MIT

package io.github._5thlayer.groundworks;

import io.github._5thlayer.groundworks.mixin.BlockItemInvoker;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;

/**
 * A block item's placement as vanilla's own {@code BlockItem#place} decides it, asked of whatever
 * level the context reads: the real world for a {@linkplain Placements#vanillaPlan Vanilla Plan},
 * the world as an Assembly will stand for its Key. It follows vanilla's chain in vanilla's order --
 * the block enabled, the context's {@code canPlace}, then {@code updatePlacementContext}, then the
 * state the item places, which it may choose itself, as a sign chooses a wall sign where it would
 * hang -- so that facing, replaceable blocks and survival come out as vanilla has them, written
 * here once.
 *
 * <p>Where the item places no state, the block's own state for the placement is what is drawn
 * refused, since the item doesn't say which state it turned down, and it says why as far as it can:
 * the state would not survive, where the item says it must, as scaffolding's needn't; or the item
 * refused it for something else, such as something in its way.
 *
 * @param context the context the placement goes through, moved by {@code updatePlacementContext}
 *     as scaffolding moves up a tower of itself, or {@code null} before there is one
 * @param state the state placed, or the block's own state for the placement where it is refused,
 *     or {@code null} where there is none
 * @param outcome whether it is placed, or how far vanilla got before refusing it
 */
record VanillaPlacement(@Nullable BlockPlaceContext context, @Nullable BlockState state, Outcome outcome) {

    /** How far vanilla's chain gets. */
    enum Outcome {
        /** The block is not enabled in this world. */
        DISABLED,
        /** The context's position is not one a block may go: taken, or outside the world. */
        CANNOT_PLACE,
        /** The item's {@code updatePlacementContext} has no context for the placement. */
        NO_CONTEXT,
        /** Neither the item nor its block has a state for the placement. */
        NO_STATE,
        /** The block's state would not survive where it goes, and the item says it must. */
        WOULD_NOT_SURVIVE,
        /** The item places no state there, though its block's own would survive: something is in its way, say. */
        REFUSED,
        /** The item places {@link #state} at the context's position. */
        PLACES,
    }

    /** Vanilla's placement of this item through this context. */
    static VanillaPlacement of(BlockItem item, BlockPlaceContext context) {
        Level level = context.getLevel();
        if (!item.getBlock().isEnabled(level.enabledFeatures())) {
            return new VanillaPlacement(null, null, Outcome.DISABLED);
        }
        if (!context.canPlace()) {
            return new VanillaPlacement(context, null, Outcome.CANNOT_PLACE);
        }
        BlockPlaceContext updated = item.updatePlacementContext(context);
        if (updated == null) {
            return new VanillaPlacement(null, null, Outcome.NO_CONTEXT);
        }
        BlockState placing = ((BlockItemInvoker) item).groundworks$placementState(updated);
        if (placing != null) {
            return new VanillaPlacement(updated, placing, Outcome.PLACES);
        }
        BlockState wanted = item.getBlock().getStateForPlacement(updated);
        if (wanted == null) {
            return new VanillaPlacement(updated, null, Outcome.NO_STATE);
        }
        boolean survives = !((BlockItemInvoker) item).groundworks$mustSurvive()
                || wanted.canSurvive(updated.getLevel(), updated.getClickedPos());
        return new VanillaPlacement(updated, wanted, survives ? Outcome.REFUSED : Outcome.WOULD_NOT_SURVIVE);
    }
}
