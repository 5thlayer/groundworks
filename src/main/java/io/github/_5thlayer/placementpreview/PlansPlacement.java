// SPDX-FileCopyrightText: 2026 5thlayer
// SPDX-License-Identifier: MIT

package io.github._5thlayer.placementpreview;

import net.minecraft.world.item.context.BlockPlaceContext;
import org.jspecify.annotations.Nullable;

/**
 * An item that answers for its own placement, and so is always drawn, with no opt-in.
 *
 * <p>Implemented by any item whose placement is not vanilla's. A plain {@code BlockItem} that places
 * as vanilla does is served by {@link Placements#vanillaPlan} instead, which defers to
 * {@code BlockPlaceContext} and so gets facing, replaceable blocks and "can this state survive here"
 * without restating any of it.
 *
 * <p>The contract, which the library can't enforce, is that the item's place <em>executes</em> what
 * {@link #plan} describes. A plan the click then disagrees with is a preview that lies.
 */
public interface PlansPlacement {

    /**
     * What this item would do here, or {@code null} where there is nothing to draw at all.
     *
     * <p>{@code null} is the common case for most of vanilla's refusals: the ray hits a block and
     * placement simply picks another spot rather than refusing one. A {@link PlacementPlan} with a
     * {@link Refusal} is the other thing -- a spot the player is aiming at and would be told no at,
     * which is what draws red.
     *
     * <p>Called on both sides. The client builds its own plan for the preview; the server builds
     * one on the click and is the authority.
     */
    @Nullable
    PlacementPlan plan(BlockPlaceContext context);
}
