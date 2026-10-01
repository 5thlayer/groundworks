// SPDX-FileCopyrightText: 2026 5thlayer
// SPDX-License-Identifier: MIT

package io.github._5thlayer.groundworks;

import net.minecraft.core.BlockPos;

/**
 * Why a {@link PlacementPlan} or a {@link DismantleSpan} would not go through.
 *
 * <p>Open, with no methods: the library names only {@link Vanilla#VANILLA} and its
 * {@link Dismantle}, {@link Stretch} and {@link FastReplace} reasons, and each Consumer names its own reasons in an enum
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
        /** A queued span's start is gone, or its family no longer counts it the same start. */
        START_GONE,
        /** The span takes a block its family doesn't accept the held tool for. */
        WRONG_TOOL,
    }

    /** The library's own reasons for a {@linkplain Stretches Stretch}, which it tells the player itself. */
    enum Stretch implements Refusal {
        /** The aim lies behind the way the leg being drawn heads, so no leg can turn back to it. */
        BEHIND_THE_LOOK,
        /** The player holds fewer of the item than the stretch would lay. */
        NOT_ENOUGH_ITEMS,
        /** What the stretch replaces would not fit in the player's inventory once it is charged. */
        NO_ROOM_TO_RETURN,
        /**
         * The player may not build at some position the stretch lays: they may not build at all, as
         * in adventure mode, or the position is outside the world or protected, as spawn is.
         */
        MAY_NOT_BUILD,
        /** A sneak-click while a Column is drawn: a Column has no anchors between its ends (ADR 0006). */
        NO_ANCHOR_IN_A_COLUMN,
    }

    /**
     * The library's own reasons for a {@linkplain io.github._5thlayer.groundworks.FastReplace Fast
     * Replace}, which it tells the player itself.
     */
    enum FastReplace implements Refusal {
        /** What the replace hands back would not fit in the player's inventory once it is charged. */
        NO_ROOM_TO_RETURN,
        /**
         * The player may not build at some position the replace puts a block: they may not build at
         * all, as in adventure mode, or the position is outside the world, protected as spawn is, or
         * guarded by a protection mod through the place event.
         */
        MAY_NOT_BUILD,
        /** The held item's own plan doesn't put a block in the aimed block's place. */
        PLANS_ELSEWHERE,
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
