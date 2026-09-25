// SPDX-FileCopyrightText: 2026 5thlayer
// SPDX-License-Identifier: MIT

package io.github._5thlayer.groundworks;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.List;

import io.github._5thlayer.groundworks.Leg.Column;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import org.junit.jupiter.api.Test;

/** The route of a stretch seen from above: the columns each leg passes, and where it turns. */
class StretchRouteTest {

    private static final BlockPos START = new BlockPos(0, 64, 0);

    @Test
    void oneLegRunsAlongTheLook() {
        assertEquals(List.of(List.of(
                        new Column(0, 0, Direction.EAST), new Column(1, 0, Direction.EAST),
                        new Column(2, 0, Direction.EAST), new Column(3, 0, Direction.EAST))),
                StretchRoute.legs(START, Direction.EAST, List.of(new BlockPos(3, 64, 0))));
    }

    @Test
    void theAimedBlocksHeightIsNotTheRoutes() {
        assertEquals(StretchRoute.legs(START, Direction.EAST, List.of(new BlockPos(3, 64, 0))),
                StretchRoute.legs(START, Direction.EAST, List.of(new BlockPos(3, 70, 0))));
    }

    @Test
    void anLRunsAlongTheLookThenTurnsOnceTowardItsEnd() {
        // Right of east is south, which is +z.
        assertEquals(List.of(List.of(
                        new Column(0, 0, Direction.EAST), new Column(1, 0, Direction.EAST),
                        new Column(2, 0, Direction.SOUTH), new Column(2, 1, Direction.SOUTH),
                        new Column(2, 2, Direction.SOUTH))),
                StretchRoute.legs(START, Direction.EAST, List.of(new BlockPos(2, 64, 2))));
    }

    @Test
    void anEndBesideTheStartTurnsAtOnce() {
        assertEquals(List.of(List.of(
                        new Column(0, 0, Direction.NORTH), new Column(0, -1, Direction.NORTH))),
                StretchRoute.legs(START, Direction.EAST, List.of(new BlockPos(0, 64, -1))));
    }

    @Test
    void anEndAtTheStartIsTheStartAlone() {
        assertEquals(List.of(List.of(new Column(0, 0, Direction.EAST))),
                StretchRoute.legs(START, Direction.EAST, List.of(START)));
    }

    @Test
    void eachLaterLegStartsAtItsAnchorHeadingTheWayThePreviousEnded() {
        // The second leg heads south from its anchor, and right of south is west, which is -x.
        assertEquals(List.of(
                        List.of(new Column(0, 0, Direction.EAST), new Column(1, 0, Direction.EAST),
                                new Column(2, 0, Direction.SOUTH), new Column(2, 1, Direction.SOUTH),
                                new Column(2, 2, Direction.SOUTH)),
                        List.of(new Column(2, 2, Direction.SOUTH), new Column(2, 3, Direction.SOUTH),
                                new Column(2, 4, Direction.WEST), new Column(1, 4, Direction.WEST),
                                new Column(0, 4, Direction.WEST)),
                        List.of(new Column(0, 4, Direction.WEST), new Column(-1, 4, Direction.WEST))),
                StretchRoute.legs(START, Direction.EAST,
                        List.of(new BlockPos(2, 64, 2), new BlockPos(0, 64, 4), new BlockPos(-1, 64, 4))));
    }

    @Test
    void anEndBehindTheLookHasNoRoute() {
        assertNull(StretchRoute.legs(START, Direction.EAST, List.of(new BlockPos(-1, 64, 3))));
    }

    @Test
    void anEndBehindTheWayTheLastLegEndedHasNoRoute() {
        assertNull(StretchRoute.legs(START, Direction.EAST, List.of(new BlockPos(3, 64, 0), new BlockPos(2, 64, 0))));
    }
}
