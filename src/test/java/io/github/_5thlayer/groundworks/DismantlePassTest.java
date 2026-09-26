// SPDX-FileCopyrightText: 2026 5thlayer
// SPDX-License-Identifier: MIT

package io.github._5thlayer.groundworks;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;

import io.github._5thlayer.groundworks.DismantlePass.Planned;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.junit.jupiter.api.Test;

/** A pass's rules over spans already planned: whole or nothing, each position once, each family's positions together. */
class DismantlePassTest {

    private enum FamilyRefusal implements Refusal {
        OFF_LINE
    }

    private static final class Family implements DismantleFamily {
        @Override
        public boolean claims(BlockState state) {
            return true;
        }

        @Override
        public DismantleSpan span(Level level, BlockPos start, BlockPos end) {
            throw new UnsupportedOperationException();
        }

        @Override
        public Component message(Refusal refusal) {
            return Component.empty();
        }
    }

    private static final Family BELTS = new Family();
    private static final Family PIPES = new Family();

    private static BlockPos at(int x) {
        return new BlockPos(x, 1, 0);
    }

    private static Planned planned(Family family, int start, DismantleSpan span) {
        return new Planned(family, new DismantleStart(null, at(start), null), span);
    }

    @Test
    void twoSpansOfOneFamilyTakeTheirPositionsTogether() {
        DismantlePass pass = DismantlePass.of(List.of(
                planned(BELTS, 0, DismantleSpan.taking(List.of(at(0), at(1)))),
                planned(BELTS, 5, DismantleSpan.taking(List.of(at(5), at(6))))));
        assertFalse(pass.isRefused());
        assertEquals(Map.of(BELTS, List.of(at(0), at(1), at(5), at(6))), pass.takes());
    }

    @Test
    void overlappingSpansTakeEachPositionOnce() {
        DismantlePass pass = DismantlePass.of(List.of(
                planned(BELTS, 0, DismantleSpan.taking(List.of(at(0), at(1), at(2)))),
                planned(BELTS, 1, DismantleSpan.taking(List.of(at(1), at(2), at(3))))));
        assertEquals(List.of(at(0), at(1), at(2), at(3)), pass.takes().get(BELTS));
        assertEquals(List.of(at(0), at(1), at(2), at(3)), pass.draws());
    }

    @Test
    void eachFamilyKeepsItsOwnPositions() {
        DismantlePass pass = DismantlePass.of(List.of(
                planned(BELTS, 0, DismantleSpan.taking(List.of(at(0)))),
                planned(PIPES, 9, DismantleSpan.taking(List.of(at(9))))));
        assertEquals(List.of(at(0)), pass.takes().get(BELTS));
        assertEquals(List.of(at(9)), pass.takes().get(PIPES));
    }

    @Test
    void oneRefusedSpanRefusesTheWholePassWithTheFirstReason() {
        Planned first = planned(BELTS, 5, DismantleSpan.refused(FamilyRefusal.OFF_LINE));
        DismantlePass pass = DismantlePass.of(List.of(
                planned(BELTS, 0, DismantleSpan.taking(List.of(at(0)))),
                first,
                planned(BELTS, 7, DismantleSpan.refused(Refusal.Dismantle.NOT_SAME_KIND))));
        assertTrue(pass.isRefused());
        assertSame(first, pass.refused());
        assertTrue(pass.takes().isEmpty());
        assertTrue(pass.draws().isEmpty());
    }

    @Test
    void aPassKnowsEveryStart() {
        DismantlePass pass = DismantlePass.of(List.of(
                planned(BELTS, 0, DismantleSpan.taking(List.of(at(0)))),
                planned(BELTS, 5, DismantleSpan.refused(FamilyRefusal.OFF_LINE))));
        assertEquals(List.of(at(0), at(5)), pass.starts());
    }
}
