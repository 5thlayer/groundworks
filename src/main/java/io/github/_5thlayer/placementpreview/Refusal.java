// SPDX-FileCopyrightText: 2026 5thlayer
// SPDX-License-Identifier: MIT

package io.github._5thlayer.placementpreview;

/**
 * Why a {@link PlacementPlan} would not go through.
 *
 * <p>Open, with no methods: the library names only {@link Vanilla#VANILLA}, and each Consumer
 * names its own reasons in an enum that implements this. The renderer asks only whether a plan is
 * refused, and a Consumer's checks keep comparing its own enum values, which the compiler checks.
 *
 * <p>An enum rather than a message, because a refusal is asked about by checks and by the renderer,
 * and neither wants to match on prose. What the player is told is the item's own business.
 */
public interface Refusal {

    /** The library's own reasons. */
    enum Vanilla implements Refusal {
        /** Vanilla would refuse: no room, the state cannot survive, the context is not placeable. */
        VANILLA,
    }
}
