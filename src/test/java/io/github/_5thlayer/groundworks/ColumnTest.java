// SPDX-FileCopyrightText: 2026 5thlayer
// SPDX-License-Identifier: MIT

package io.github._5thlayer.groundworks;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import org.junit.jupiter.api.Test;

/** A Column's blocks, and when a drag draws one rather than legs (ADR 0006). */
class ColumnTest {

    private static final BlockPos START = new BlockPos(1, 64, 2);

    @Test
    void aColumnRunsUpFromItsStartByItsHeight() {
        assertEquals(List.of(START, START.above(), START.above(2)),
                new Column(START, 2, Direction.EAST).positions());
    }

    @Test
    void aColumnRunsDownWhenLowered() {
        assertEquals(List.of(START, START.below()), new Column(START, -1, Direction.EAST).positions());
    }

    @Test
    void aColumnWithNoHeightIsItsStartAlone() {
        assertEquals(List.of(START), new Column(START, 0, Direction.EAST).positions());
    }

    @Test
    void itsBlocksAreClickedOnTheWayItGrows() {
        assertEquals(Direction.UP, new Column(START, 2, Direction.EAST).face());
        assertEquals(Direction.UP, new Column(START, 0, Direction.EAST).face());
        assertEquals(Direction.DOWN, new Column(START, -2, Direction.EAST).face());
    }

    @Test
    void anItemThatStretchesOnlyAsAColumnDrawsOneWhereverAimed() {
        assertTrue(Column.drawn(false, true, START, new BlockPos(5, 60, 9)));
    }

    @Test
    void anItemThatStretchesBothWaysDrawsAColumnOnlyOnTheStartsColumn() {
        assertTrue(Column.drawn(true, true, START, new BlockPos(1, 70, 2)));
        assertFalse(Column.drawn(true, true, START, new BlockPos(2, 64, 2)));
    }

    @Test
    void aStretchWithAnchorsIsNeverAColumn() {
        assertFalse(Column.drawn(true, false, START, START));
        assertFalse(Column.drawn(false, false, START, START));
    }
}
