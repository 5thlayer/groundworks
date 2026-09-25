// SPDX-FileCopyrightText: 2026 5thlayer
// SPDX-License-Identifier: MIT

package io.github._5thlayer.groundworks;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Test;

class HeightTest {

    private static final int REACH = 4;

    @Test
    void noHeightIsZero() {
        assertEquals(0, Height.NONE.blocks());
    }

    @Test
    void aHeightWithinTheCapReadsAsItIs() {
        assertEquals(new Height(3), new Height(3).clampedTo(REACH));
        assertEquals(new Height(-4), new Height(-4).clampedTo(REACH));
    }

    @Test
    void aHeightBeyondTheCapIsClampedToItBothWays() {
        assertEquals(new Height(4), new Height(7).clampedTo(REACH));
        assertEquals(new Height(-4), new Height(-9).clampedTo(REACH));
        assertEquals(Height.NONE, new Height(2).clampedTo(0));
    }

    @Test
    void raiseAndLowerStepOneBlock() {
        assertEquals(new Height(1), Height.NONE.step(false, REACH));
        assertEquals(new Height(-1), Height.NONE.step(true, REACH));
        assertEquals(Height.NONE, new Height(1).step(true, REACH));
    }

    @Test
    void aPressUpToTheCapGoesThrough() {
        assertEquals(new Height(4), new Height(3).step(false, REACH));
        assertEquals(new Height(-4), new Height(-3).step(true, REACH));
    }

    @Test
    void aPressPastTheCapIsRefusedBothWays() {
        assertNull(new Height(4).step(false, REACH));
        assertNull(new Height(-4).step(true, REACH));
    }

    @Test
    void aPressOnAHeightBeyondTheCapStepsFromTheClampedOne() {
        // Reach shrank since the height was stored: raising further is refused, lowering goes from the cap.
        assertNull(new Height(7).step(false, REACH));
        assertEquals(new Height(3), new Height(7).step(true, REACH));
        assertEquals(new Height(-3), new Height(-9).step(false, REACH));
    }

    @Test
    void withNoReachEveryPressIsRefused() {
        assertNull(Height.NONE.step(false, 0));
        assertNull(Height.NONE.step(true, 0));
    }
}
