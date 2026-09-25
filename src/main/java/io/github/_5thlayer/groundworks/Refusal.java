// SPDX-FileCopyrightText: 2026 5thlayer
// SPDX-License-Identifier: MIT

package io.github._5thlayer.groundworks;

import net.minecraft.core.BlockPos;

/**
 * Why a {@link PlacementPlan} or a {@link DismantleSpan} would not go through.
 *
 * <p>Open, with no methods: the library names only {@link Vanilla#VANILLA} and its
 * {@link Dismantle} and {@link Stretch} reasons, and each Consumer names its own reasons in an enum
 * that implements this. The renderer asks only whether a plan is refused, and a Consumer's checks keep comparing its
 * own enum values, which the compiler checks.
 *
 * <p>An enum rather than a message, because a refusal is asked about by checks and by the renderer,
 * and neither wants to match on prose. What the player is told is the item's own business, or for
 * a span, its {@linkplain DismantleFamily#message family's}, and for a stretch's leg, its
 * {@linkplain LegBuilder#message builder's}.
 *
 * <p>A refusal that stands at a position, such as a leg's obstacle, says so by being an {@link At}
 * (ADR 0004); any other stands nowhere in particular.
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

    /** The library's own reasons for a {@linkplain Stretches Stretch}, which it tells the player itself. */
    enum Stretch implements Refusal {
        /** The aim lies behind the way the leg being drawn heads, so no leg can turn back to it. */
        BEHIND_THE_LOOK,
        /** The player holds fewer of the item than the stretch would lay. */
        NOT_ENOUGH_ITEMS,
        /** What the stretch replaces would not fit in the player's inventory once it is charged. */
        NO_ROOM_TO_RETURN,
    }

    /**
     * A refusal that stands at a position, such as the block in a leg's way: {@code reason} is why,
     * and what the player is told is asked of it alone.
     */
    record At(Refusal reason, BlockPos pos) implements Refusal {

        public At {
            pos = pos.immutable();
        }
    }
}
