// SPDX-FileCopyrightText: 2026 5thlayer
// SPDX-License-Identifier: MIT

package io.github._5thlayer.groundworks;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.ArrayList;
import java.util.List;

import io.github._5thlayer.groundworks.Leg.Column;
import io.github._5thlayer.groundworks.VanillaLegs.Spot;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import org.junit.jupiter.api.Test;

/** Where the vanilla Consumer lays each block of a leg, and the way a player laying it would look. */
class VanillaLegsTest {

    private static final BlockPos FROM = new BlockPos(0, 64, 0);

    @Test
    void aLevelLegLaysEachColumnAtItsFirstAnchorsHeightLookingTheWayItTravels() {
        List<Column> route = List.of(new Column(0, 0, Direction.EAST), new Column(1, 0, Direction.EAST),
                new Column(2, 0, Direction.SOUTH), new Column(2, 1, Direction.SOUTH));
        assertEquals(List.of(new Spot(new BlockPos(0, 64, 0), Direction.EAST), new Spot(new BlockPos(1, 64, 0), Direction.EAST),
                        new Spot(new BlockPos(2, 64, 0), Direction.SOUTH), new Spot(new BlockPos(2, 64, 1), Direction.SOUTH)),
                VanillaLegs.spots(new Leg(FROM, 0, route)));
    }

    @Test
    void aRiseClimbsOneBlockPerColumnRightAfterTheFirstAnchorThenRunsLevel() {
        assertEquals(List.of(64, 65, 66, 66, 66), heights(VanillaLegs.spots(new Leg(FROM, 2, east(5)))));
        assertEquals(List.of(Direction.EAST, Direction.EAST, Direction.EAST, Direction.EAST, Direction.EAST),
                facings(VanillaLegs.spots(new Leg(FROM, 2, east(5)))));
    }

    @Test
    void aFallDescendsOneBlockPerColumnLookingBackUpItThenRunsLevelLookingAhead() {
        List<Spot> spots = VanillaLegs.spots(new Leg(FROM, -2, east(5)));
        assertEquals(List.of(64, 63, 62, 62, 62), heights(spots));
        assertEquals(List.of(Direction.WEST, Direction.WEST, Direction.WEST, Direction.EAST, Direction.EAST), facings(spots));
    }

    @Test
    void aRiseFitsWhenEachOfItsColumnsRunsTheLegsFirstWay() {
        assertEquals(true, VanillaLegs.riseFits(new Leg(FROM, 0, east(1))));
        assertEquals(true, VanillaLegs.riseFits(new Leg(FROM, 2, east(3))));
        assertEquals(true, VanillaLegs.riseFits(new Leg(FROM, -2, east(3))));
    }

    @Test
    void aRiseDoesNotFitALegTooShortForItOrOneThatTurnsBeforeItsTop() {
        assertEquals(false, VanillaLegs.riseFits(new Leg(FROM, 1, east(1))));
        assertEquals(false, VanillaLegs.riseFits(new Leg(FROM, 3, east(3))));
        List<Column> turning = List.of(new Column(0, 0, Direction.EAST), new Column(1, 0, Direction.EAST),
                new Column(2, 0, Direction.SOUTH), new Column(2, 1, Direction.SOUTH));
        assertEquals(true, VanillaLegs.riseFits(new Leg(FROM, 1, turning)));
        assertEquals(false, VanillaLegs.riseFits(new Leg(FROM, 2, turning)));
    }

    private static List<Column> east(int length) {
        List<Column> route = new ArrayList<>();
        for (int x = 0; x < length; x++) {
            route.add(new Column(x, 0, Direction.EAST));
        }
        return route;
    }

    private static List<Integer> heights(List<Spot> spots) {
        return spots.stream().map(spot -> spot.pos().getY()).toList();
    }

    private static List<Direction> facings(List<Spot> spots) {
        return spots.stream().map(Spot::facing).toList();
    }
}
