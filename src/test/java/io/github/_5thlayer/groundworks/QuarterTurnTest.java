// SPDX-FileCopyrightText: 2026 5thlayer
// SPDX-License-Identifier: MIT

package io.github._5thlayer.groundworks;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import net.minecraft.core.Direction;

import org.junit.jupiter.api.Test;

class QuarterTurnTest {

    @Test
    void noTurnIsZero() {
        assertEquals(0, QuarterTurn.NONE.quarters());
        assertEquals(QuarterTurn.NONE, QuarterTurn.of(0));
    }

    @Test
    void rotateWrapsFromThreeToZero() {
        QuarterTurn turn = QuarterTurn.NONE;
        int[] seen = new int[5];
        for (int press = 0; press < 5; press++) {
            seen[press] = turn.quarters();
            turn = turn.rotate();
        }
        assertArrayEquals(new int[] {0, 1, 2, 3, 0}, seen);
    }

    @Test
    void reverseRotateWrapsFromZeroToThree() {
        assertEquals(3, QuarterTurn.NONE.reverseRotate().quarters());
        assertEquals(2, QuarterTurn.NONE.reverseRotate().reverseRotate().quarters());
        assertEquals(QuarterTurn.NONE, QuarterTurn.of(1).reverseRotate());
    }

    @Test
    void ofWrapsAnyIntegerIntoZeroToThree() {
        assertEquals(1, QuarterTurn.of(5).quarters());
        assertEquals(3, QuarterTurn.of(-1).quarters());
        assertEquals(0, QuarterTurn.of(-8).quarters());
    }

    @Test
    void aTurnOutsideZeroToThreeIsRejected() {
        assertThrows(IllegalArgumentException.class, () -> new QuarterTurn(-1));
        assertThrows(IllegalArgumentException.class, () -> new QuarterTurn(4));
    }

    @Test
    void eachTurnTurnsEachHeadingClockwiseThatManyQuarters() {
        Direction[] clockwise = {Direction.NORTH, Direction.EAST, Direction.SOUTH, Direction.WEST};
        for (int start = 0; start < 4; start++) {
            for (int quarters = 0; quarters < 4; quarters++) {
                assertEquals(clockwise[(start + quarters) % 4], QuarterTurn.of(quarters).turn(clockwise[start]),
                        clockwise[start] + " turned " + quarters);
            }
        }
    }

    /** Up and down are no heading, so no turn reaches them. */
    @Test
    void aVerticalDirectionIsNotTurned() {
        for (int quarters = 0; quarters < 4; quarters++) {
            assertEquals(Direction.UP, QuarterTurn.of(quarters).turn(Direction.UP));
            assertEquals(Direction.DOWN, QuarterTurn.of(quarters).turn(Direction.DOWN));
        }
    }

    @Test
    void aYawTurnsByNinetyDegreesAQuarter() {
        assertEquals(30f, QuarterTurn.NONE.turnYaw(30f));
        assertEquals(120f, QuarterTurn.of(1).turnYaw(30f));
        assertEquals(300f, QuarterTurn.of(3).turnYaw(30f));
    }
}
