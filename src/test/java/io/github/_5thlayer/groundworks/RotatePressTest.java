// SPDX-FileCopyrightText: 2026 5thlayer
// SPDX-License-Identifier: MIT

package io.github._5thlayer.groundworks;

import static org.junit.jupiter.api.Assertions.assertEquals;

import io.github._5thlayer.groundworks.RotatePress.Nothing;
import io.github._5thlayer.groundworks.RotatePress.Outcome;
import io.github._5thlayer.groundworks.RotatePress.PlanTurned;
import io.github._5thlayer.groundworks.RotatePress.Refused;
import io.github._5thlayer.groundworks.RotatePress.TurnedInPlace;
import io.github._5thlayer.groundworks.TurnsInPlace.Verdict;

import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;

/**
 * The Rotate press rule over a stand-in state, a string: the held item first, then the aimed block
 * if a statement covers it and the player may turn it there, by its own contract or else vanilla's
 * turn. The Pack's {@code PlacedTurnTest} is ported here, less its deny list and refit, which the
 * block's own contract replaces.
 */
class RotatePressTest {

    /** A stand-in aimed block: a statement covers it, the player may turn it, it has no contract, and vanilla turns it and it stands. */
    private record StandIn(String state, boolean isStated, boolean mayTurn, @Nullable Verdict<String> ownTurn,
                         String vanillaTurn, boolean stands) implements RotatePress.Aimed<String> {

        static StandIn turnable(String state) {
            return new StandIn(state, true, true, null, state + "+turned", true);
        }

        StandIn unstated() {
            return new StandIn(state, false, mayTurn, ownTurn, vanillaTurn, stands);
        }

        StandIn mayNotTurn() {
            return new StandIn(state, isStated, false, ownTurn, vanillaTurn, stands);
        }

        StandIn answering(Verdict<String> own) {
            return new StandIn(state, isStated, mayTurn, own, vanillaTurn, stands);
        }

        StandIn vanillaTurnsTo(String turned) {
            return new StandIn(state, isStated, mayTurn, ownTurn, turned, stands);
        }

        StandIn wouldNotStand() {
            return new StandIn(state, isStated, mayTurn, ownTurn, vanillaTurn, false);
        }

        @Override
        public boolean stands(String turned) {
            return stands;
        }
    }

    private static final StandIn TILE = StandIn.turnable("tile");

    private static Outcome<String> pressAimed(StandIn aimed) {
        return RotatePress.decide(null, false, aimed);
    }

    @Test
    void rotatableHeldBeatsAimed() {
        assertEquals(new PlanTurned<String>(QuarterTurn.of(1)), RotatePress.decide(QuarterTurn.NONE, false, TILE));
        assertEquals(new PlanTurned<String>(QuarterTurn.of(3)), RotatePress.decide(QuarterTurn.NONE, true, TILE));
        assertEquals(new PlanTurned<String>(QuarterTurn.NONE), RotatePress.decide(QuarterTurn.of(3), false, null));
        assertEquals(new PlanTurned<String>(QuarterTurn.of(1)), RotatePress.decide(QuarterTurn.of(2), true, null));
    }

    @Test
    void noAimIsNothing() {
        assertEquals(new Nothing<String>(), RotatePress.decide(null, false, null));
    }

    @Test
    void unstatedIsNothing() {
        assertEquals(new Nothing<String>(), pressAimed(TILE.unstated()));
        assertEquals(new Nothing<String>(), pressAimed(TILE.unstated().answering(TurnsInPlace.refused("machine"))));
    }

    @Test
    void mayNotTurnIsNothing() {
        assertEquals(new Nothing<String>(), pressAimed(TILE.mayNotTurn()));
        assertEquals(new Nothing<String>(), pressAimed(TILE.mayNotTurn().answering(TurnsInPlace.refused("machine"))));
    }

    @Test
    void vanillaByDefault() {
        assertEquals(new TurnedInPlace<>("tile+turned"), pressAimed(TILE));
    }

    @Test
    void vanillaUnchangedIsNothing() {
        assertEquals(new Nothing<String>(), pressAimed(StandIn.turnable("stone").vanillaTurnsTo("stone")));
    }

    @Test
    void noLongerStandingIsNothing() {
        assertEquals(new Nothing<String>(), pressAimed(StandIn.turnable("wall torch").wouldNotStand()));
    }

    @Test
    void contractAnswers() {
        assertEquals(new TurnedInPlace<>("machine, contents kept"),
                pressAimed(StandIn.turnable("machine").answering(TurnsInPlace.turned("machine, contents kept"))));
        assertEquals(new Refused<String>("footprint"),
                pressAimed(StandIn.turnable("machine").answering(TurnsInPlace.refused("footprint"))));
    }

    @Test
    void contractUnturnedIsNothing() {
        assertEquals(new Nothing<String>(), pressAimed(StandIn.turnable("machine").answering(TurnsInPlace.unturned())));
    }

    @Test
    void contractTurnedToSameIsNothing() {
        assertEquals(new Nothing<String>(), pressAimed(StandIn.turnable("machine").answering(TurnsInPlace.turned("machine"))));
    }

    @Test
    void contractNotAskedToStand() {
        assertEquals(new TurnedInPlace<>("machine turned"),
                pressAimed(StandIn.turnable("machine").wouldNotStand().answering(TurnsInPlace.turned("machine turned"))));
    }
}
