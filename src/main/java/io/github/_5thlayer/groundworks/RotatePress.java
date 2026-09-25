// SPDX-FileCopyrightText: 2026 5thlayer
// SPDX-License-Identifier: MIT

package io.github._5thlayer.groundworks;

import io.github._5thlayer.groundworks.TurnsInPlace.Verdict;
import org.jspecify.annotations.Nullable;

/**
 * What a press of Rotate or Reverse Rotate does, over any block state {@code S} (ADR 0003). As in
 * Factorio, it takes the held item before the aimed block. In order:
 *
 * <ol>
 *   <li>A rotatable held item Rotates the Plan.
 *   <li>Otherwise nothing, if no block is aimed, no Consumer's statement covers it, or the player
 *       may not turn it where it stands.
 *   <li>Otherwise a block with its own {@linkplain TurnsInPlace contract} answers for itself.
 *   <li>Otherwise vanilla's turn, unless the block would no longer stand.
 * </ol>
 *
 * A turn that changes nothing is nothing. {@link Rotate#press} is this rule over the world.
 */
final class RotatePress {

    private RotatePress() {
    }

    sealed interface Outcome<S> {
    }

    /** The held stack's new turn. */
    record PlanTurned<S>(QuarterTurn turn) implements Outcome<S> {
    }

    /** The aimed block's new state. */
    record TurnedInPlace<S>(S state) implements Outcome<S> {
    }

    /** The aimed block is left as it was, and the player is told why. */
    record Refused<S>(String reason) implements Outcome<S> {
    }

    record Nothing<S>() implements Outcome<S> {
    }

    /**
     * The block under the crosshair, as the rule asks of it. Each question is asked only once the
     * rule reaches it, so a block that is never turned is never asked how it would turn, and the
     * claim guard's event fires only for a block that might.
     */
    interface Aimed<S> {

        S state();

        /** Whether some Consumer has stated Rotate in Place turns it. */
        boolean isStated();

        /** Whether the player may turn it where it stands, which a claim may forbid. */
        boolean mayTurn();

        /** The block's own contract's answer, or {@code null} if it has none. */
        @Nullable
        Verdict<S> ownTurn();

        /** Vanilla's turn of it, reshaped against its neighbours. */
        S vanillaTurn();

        /** Whether this state of it would stand where it is. */
        boolean stands(S turned);
    }

    /**
     * @param heldTurn the held stack's turn if it is rotatable, or {@code null} if it is not
     * @param aimed the block under the crosshair, or {@code null} if none
     */
    static <S> Outcome<S> decide(@Nullable QuarterTurn heldTurn, boolean reverse, @Nullable Aimed<S> aimed) {
        if (heldTurn != null) {
            return new PlanTurned<>(reverse ? heldTurn.reverseRotate() : heldTurn.rotate());
        }
        if (aimed == null || !aimed.isStated() || !aimed.mayTurn()) {
            return new Nothing<>();
        }
        S state = aimed.state();
        Verdict<S> own = aimed.ownTurn();
        if (own != null) {
            return switch (own) {
                case TurnsInPlace.Turned<S>(S turned) -> turnedUnlessUnchanged(state, turned);
                case TurnsInPlace.Refused<S>(String reason) -> new Refused<>(reason);
                case TurnsInPlace.Unturned<S>() -> new Nothing<>();
            };
        }
        S turned = aimed.vanillaTurn();
        return aimed.stands(turned) ? turnedUnlessUnchanged(state, turned) : new Nothing<>();
    }

    private static <S> Outcome<S> turnedUnlessUnchanged(S state, S turned) {
        return turned.equals(state) ? new Nothing<>() : new TurnedInPlace<>(turned);
    }
}
