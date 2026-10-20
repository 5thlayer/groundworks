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
 *   <li>A rotatable held item Rotates the Plan, to the next turn the Placement Preview would show,
 *       or is refused with its reason if no turn would show.
 *   <li>Otherwise nothing, if no block is aimed, no Consumer's statement covers it, or the player
 *       may not turn it where it stands.
 *   <li>Otherwise a block with its own {@linkplain TurnsInPlace contract} answers for itself.
 *   <li>Otherwise vanilla's turn, unless the block would no longer stand.
 * </ol>
 *
 * A turn in place that changes nothing is nothing. {@link Rotate#press} is this rule over the world.
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

    /** The held stack or the aimed block is left as it was, and the player is told why. */
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
     * The rotatable held stack, as the rule asks of it, over any plan {@code P}: what the Placement
     * Preview would draw at the aim with the stack turned each way.
     */
    interface Held<P> {

        QuarterTurn turn();

        /**
         * Whether the turn is a Stretch's start's: one is stored, or a sneak-click here would store
         * one. Its preview is the stretch's, which the turn leaves alone, so the turn goes unchecked.
         */
        boolean turnsAStart();

        /** The plan drawn at the aim with the stack turned this way, or {@code null} where none is drawn. */
        @Nullable
        P planWith(QuarterTurn turn);

        boolean isRefused(P plan);
    }

    /**
     * @param held the held stack if it is rotatable, or {@code null} if it is not
     * @param aimed the block under the crosshair, or {@code null} if none
     */
    static <S, P> Outcome<S> decide(@Nullable Held<P> held, boolean reverse, @Nullable Aimed<S> aimed) {
        if (held != null) {
            return turnedPlan(held, reverse);
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

    /**
     * The next turn, the press's way, whose plan differs from the one drawn now and, unless that one
     * is refused already, is not refused: so a block that stands on the face it is placed against
     * has nothing to turn to, and one in the way of something else still turns before it is cleared.
     */
    private static <S, P> Outcome<S> turnedPlan(Held<P> held, boolean reverse) {
        QuarterTurn turn = held.turn();
        if (held.turnsAStart()) {
            return new PlanTurned<>(step(turn, reverse));
        }
        P drawn = held.planWith(turn);
        if (drawn == null) {
            return new Refused<>(Rotate.NOTHING_DRAWN);
        }
        boolean drawnRefused = held.isRefused(drawn);
        QuarterTurn next = turn;
        for (int quarter = 1; quarter < 4; quarter++) {
            next = step(next, reverse);
            P plan = held.planWith(next);
            if (plan != null && !plan.equals(drawn) && (drawnRefused || !held.isRefused(plan))) {
                return new PlanTurned<>(next);
            }
        }
        return new Refused<>(Rotate.UNCHANGED);
    }

    private static QuarterTurn step(QuarterTurn turn, boolean reverse) {
        return reverse ? turn.reverseRotate() : turn.rotate();
    }

    private static <S> Outcome<S> turnedUnlessUnchanged(S state, S turned) {
        return turned.equals(state) ? new Nothing<>() : new TurnedInPlace<>(turned);
    }
}
