// SPDX-FileCopyrightText: 2026 5thlayer
// SPDX-License-Identifier: MIT

package io.github._5thlayer.groundworks;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.core.BlockPos;
import org.junit.jupiter.api.Test;

/** The span's own rules, as a family answers it. */
class DismantleSpanTest {

    /** A family's own reason, as each family names them. */
    private enum FamilyRefusal implements Refusal {
        OFF_LINE
    }

    private static final BlockPos TILE = new BlockPos(0, 1, 0);
    private static final BlockPos NEXT = new BlockPos(1, 1, 0);
    private static final BlockPos WEDGE = new BlockPos(0, 0, 0);

    @Test
    void aSpanDrawsWhatItTakes() {
        DismantleSpan span = DismantleSpan.taking(List.of(TILE, NEXT));
        assertFalse(span.isRefused());
        assertEquals(List.of(TILE, NEXT), span.takes());
        assertEquals(List.of(TILE, NEXT), span.draws());
    }

    @Test
    void aSpanMayDrawMoreThanItTakes() {
        DismantleSpan span = DismantleSpan.taking(List.of(TILE), List.of(WEDGE));
        assertEquals(List.of(TILE), span.takes());
        assertEquals(List.of(TILE, WEDGE), span.draws());
    }

    @Test
    void aSpanCannotTakeWhatItDoesNotDraw() {
        assertThrows(IllegalArgumentException.class, () -> new DismantleSpan(List.of(TILE), List.of(WEDGE), null));
    }

    @Test
    void aRefusedSpanTakesAndDrawsNothing() {
        DismantleSpan span = DismantleSpan.refused(FamilyRefusal.OFF_LINE);
        assertTrue(span.isRefused());
        assertSame(FamilyRefusal.OFF_LINE, span.refusal());
        assertTrue(span.takes().isEmpty());
        assertTrue(span.draws().isEmpty());
    }

    @Test
    void aSpanKeepsItsPositionsWhenTheFamilysListChanges() {
        List<BlockPos> takes = new ArrayList<>(List.of(TILE));
        DismantleSpan span = DismantleSpan.taking(takes);
        takes.add(NEXT);
        assertEquals(List.of(TILE), span.takes());
    }
}
