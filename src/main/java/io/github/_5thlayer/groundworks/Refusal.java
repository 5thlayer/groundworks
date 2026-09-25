// SPDX-FileCopyrightText: 2026 5thlayer
// SPDX-License-Identifier: MIT

package io.github._5thlayer.groundworks;

/**
 * Why a {@link PlacementPlan} or a {@link DismantleSpan} would not go through.
 *
 * <p>Open, with no methods: the library names only {@link Vanilla#VANILLA} and its
 * {@link Dismantle} reasons, and each Consumer names its own reasons in an enum that implements
 * this. The renderer asks only whether a plan is refused, and a Consumer's checks keep comparing its
 * own enum values, which the compiler checks.
 *
 * <p>An enum rather than a message, because a refusal is asked about by checks and by the renderer,
 * and neither wants to match on prose. What the player is told is the item's own business, or for
 * a span, its {@linkplain DismantleFamily#message family's}.
 */
public interface Refusal {

    /** The library's own reason for a placement. */
    enum Vanilla implements Refusal {
        /** Vanilla would refuse: no room, the state cannot survive, the context is not placeable. */
        VANILLA,
    }

    /** The library's own reasons for a span, which it tells the player itself. */
    enum Dismantle implements Refusal {
        /** The end isn't a member of the start's family, so no family's rule can join them. */
        NOT_SAME_KIND,
    }
}
