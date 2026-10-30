// SPDX-FileCopyrightText: 2026 5thlayer
// SPDX-License-Identifier: MIT

package io.github._5thlayer.groundworks;

import static org.junit.jupiter.api.Assertions.assertEquals;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Vec3i;

import org.junit.jupiter.api.Test;

/** Where a Blueprint's box lands for an aim: its bottom centre resting on the aimed face. */
class BlueprintLandingTest {

    private static final BlockPos SPOT = new BlockPos(10, 64, 20);

    @Test
    void onATopFaceTheBottomLayerIsCentredOnTheSpot() {
        BlueprintLanding landing = BlueprintLanding.of(new Vec3i(3, 2, 5), SPOT, Direction.UP);
        assertEquals(new BlockPos(9, 64, 18), landing.at(BlockPos.ZERO));
        assertEquals(new BlockPos(11, 65, 22), landing.at(new BlockPos(2, 1, 4)));
    }

    @Test
    void anEvenSideHasTheSpotJustPastItsMiddle() {
        BlueprintLanding landing = BlueprintLanding.of(new Vec3i(4, 1, 2), SPOT, Direction.UP);
        assertEquals(new BlockPos(8, 64, 19), landing.at(BlockPos.ZERO));
    }

    @Test
    void underABottomFaceTheTopLayerIsOnTheSpot() {
        BlueprintLanding landing = BlueprintLanding.of(new Vec3i(3, 4, 3), SPOT, Direction.DOWN);
        assertEquals(new BlockPos(9, 61, 19), landing.at(BlockPos.ZERO));
        assertEquals(new BlockPos(11, 64, 21), landing.at(new BlockPos(2, 3, 2)));
    }

    @Test
    void onASideFaceTheBoxRestsOnTheSpotAndReachesAwayFromTheFace() {
        Vec3i size = new Vec3i(3, 2, 3);
        assertEquals(new BlockPos(10, 64, 19), BlueprintLanding.of(size, SPOT, Direction.EAST).at(BlockPos.ZERO));
        assertEquals(new BlockPos(8, 64, 19), BlueprintLanding.of(size, SPOT, Direction.WEST).at(BlockPos.ZERO));
        assertEquals(new BlockPos(9, 64, 20), BlueprintLanding.of(size, SPOT, Direction.SOUTH).at(BlockPos.ZERO));
        assertEquals(new BlockPos(9, 64, 18), BlueprintLanding.of(size, SPOT, Direction.NORTH).at(BlockPos.ZERO));
    }

    @Test
    void aQuarterTurnTurnsTheBoxClockwiseSeenFromAboveAboutItsLanding() {
        BlueprintLanding landing = BlueprintLanding.of(new Vec3i(3, 1, 2), SPOT, Direction.UP, QuarterTurn.of(1));
        assertEquals(new BlockPos(10, 64, 19), landing.at(BlockPos.ZERO));
        assertEquals(new BlockPos(9, 64, 21), landing.at(new BlockPos(2, 0, 1)));
        assertEquals(new BlockPos(10, 64, 21), landing.at(new BlockPos(2, 0, 0)));
    }

    @Test
    void aHalfTurnKeepsTheFootprintAndSwapsItsCorners() {
        BlueprintLanding landing = BlueprintLanding.of(new Vec3i(3, 1, 2), SPOT, Direction.UP, QuarterTurn.of(2));
        assertEquals(new BlockPos(11, 64, 20), landing.at(BlockPos.ZERO));
        assertEquals(new BlockPos(9, 64, 19), landing.at(new BlockPos(2, 0, 1)));
    }

    @Test
    void threeQuartersTurnTheBoxCounterclockwise() {
        BlueprintLanding landing = BlueprintLanding.of(new Vec3i(3, 1, 2), SPOT, Direction.UP, QuarterTurn.of(3));
        assertEquals(new BlockPos(9, 64, 21), landing.at(BlockPos.ZERO));
        assertEquals(new BlockPos(10, 64, 19), landing.at(new BlockPos(2, 0, 1)));
    }

    @Test
    void aTurnedBoxRestsOnASideFaceByItsTurnedFootprint() {
        BlueprintLanding landing = BlueprintLanding.of(new Vec3i(3, 1, 1), SPOT, Direction.EAST, QuarterTurn.of(1));
        assertEquals(new BlockPos(10, 64, 19), landing.at(BlockPos.ZERO));
        assertEquals(new BlockPos(10, 64, 21), landing.at(new BlockPos(2, 0, 0)));
    }

    @Test
    void aTurnLeavesEachLayerAtItsHeight() {
        BlueprintLanding landing = BlueprintLanding.of(new Vec3i(2, 3, 2), SPOT, Direction.UP, QuarterTurn.of(1));
        assertEquals(new BlockPos(10, 66, 19), landing.at(new BlockPos(0, 2, 0)));
    }
}
