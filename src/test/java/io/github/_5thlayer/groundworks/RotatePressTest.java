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

import java.util.Arrays;
import java.util.List;
import java.util.Set;

import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;

/**
 * The Rotate press rule over a stand-in state and plan, strings: the held item first, to the next
 * turn whose plan the preview would show, then the aimed block if a statement covers it and the
 * player may turn it there, by its own contract or else vanilla's turn. The Pack's {@code
 * PlacedTurnTest} is ported here, less its deny list and refit, which the block's own contract
 * replaces.
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

    /**
     * A stand-in held stack: its turn, and the plan drawn with each turn, by quarters, {@code null}
     * where none is drawn, those in {@code refused} drawn red.
     */
    private record Held(QuarterTurn turn, boolean turnsAStart, List<@Nullable String> plans, Set<String> refused)
            implements RotatePress.Held<String> {

        /** A furnace's: each turn draws its own plan. */
        static Held turning(int quarters) {
            return new Held(QuarterTurn.of(quarters), false, List.of("north", "east", "south", "west"), Set.of());
        }

        /** A wall torch's: every turn draws the same plan. */
        static Held hanging() {
            return new Held(QuarterTurn.NONE, false, List.of("on the wall", "on the wall", "on the wall", "on the wall"),
                    Set.of());
        }

        static Held aimingNothing(int quarters) {
            return new Held(QuarterTurn.of(quarters), false, Arrays.asList(null, null, null, null), Set.of());
        }

        Held refusing(String... plans) {
            return new Held(turn, turnsAStart, this.plans, Set.of(plans));
        }

        Held turningAStart() {
            return new Held(turn, true, plans, refused);
        }

        @Override
        public @Nullable String planWith(QuarterTurn turn) {
            return plans.get(turn.quarters());
        }

        @Override
        public boolean isRefused(String plan) {
            return refused.contains(plan);
        }
    }

    private static final StandIn TILE = StandIn.turnable("tile");

    private static Outcome<String> pressAimed(StandIn aimed) {
        return RotatePress.<String, String>decide(null, false, aimed);
    }

    @Test
    void rotatableHeldBeatsAimed() {
        assertEquals(new PlanTurned<String>(QuarterTurn.of(1)), RotatePress.decide(Held.turning(0), false, TILE));
        assertEquals(new PlanTurned<String>(QuarterTurn.of(3)), RotatePress.decide(Held.turning(0), true, TILE));
        assertEquals(new PlanTurned<String>(QuarterTurn.NONE), RotatePress.decide(Held.turning(3), false, null));
        assertEquals(new PlanTurned<String>(QuarterTurn.of(1)), RotatePress.decide(Held.turning(2), true, null));
    }

    @Test
    void nothingDrawnIsRefused() {
        assertEquals(new Refused<String>(Rotate.NOTHING_DRAWN), RotatePress.decide(Held.aimingNothing(1), false, TILE));
    }

    @Test
    void anUnchangedPlanIsRefused() {
        assertEquals(new Refused<String>(Rotate.UNCHANGED), RotatePress.decide(Held.hanging(), false, TILE));
        assertEquals(new Refused<String>(Rotate.UNCHANGED), RotatePress.decide(Held.hanging(), true, null));
    }

    @Test
    void aRefusedTurnIsPassedOver() {
        assertEquals(new PlanTurned<String>(QuarterTurn.of(2)),
                RotatePress.decide(Held.turning(0).refusing("east"), false, null));
        assertEquals(new PlanTurned<String>(QuarterTurn.of(2)),
                RotatePress.decide(Held.turning(0).refusing("west"), true, null));
    }

    @Test
    void onlyRefusedTurnsAreRefused() {
        assertEquals(new Refused<String>(Rotate.UNCHANGED),
                RotatePress.decide(Held.turning(0).refusing("east", "south", "west"), false, null));
    }

    @Test
    void aRefusedPlanTurnsToAnyOther() {
        assertEquals(new PlanTurned<String>(QuarterTurn.of(1)),
                RotatePress.decide(Held.turning(0).refusing("north", "east", "south", "west"), false, null));
    }

    @Test
    void aStartsTurnIsNotChecked() {
        assertEquals(new PlanTurned<String>(QuarterTurn.of(1)), RotatePress.decide(Held.hanging().turningAStart(), false, null));
        assertEquals(new PlanTurned<String>(QuarterTurn.of(3)),
                RotatePress.decide(Held.aimingNothing(0).turningAStart(), true, null));
    }

    @Test
    void noAimIsNothing() {
        assertEquals(new Nothing<String>(), RotatePress.<String, String>decide(null, false, null));
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
