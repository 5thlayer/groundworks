// SPDX-FileCopyrightText: 2026 5thlayer
// SPDX-License-Identifier: MIT

package io.github._5thlayer.groundworks;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import java.util.stream.IntStream;

import io.github._5thlayer.groundworks.Leg.Column;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import org.junit.jupiter.api.Test;

/** A stretch's legs, as Groundworks hands them to the item that builds them. */
class LegTest {

    private static final BlockPos START = new BlockPos(0, 64, 0);

    @Test
    void theOnlyLegOfAStretchStartsAndEndsIt() {
        List<Leg> legs = Leg.ofStretch(START, List.of(east(0, 0, 3)), List.of(0), true);
        assertEquals(List.of(new Leg(START, 0, east(0, 0, 3), true, true)), legs);
    }

    @Test
    void inAThreeLegStretchOnlyTheFirstStartsItAndOnlyTheLastEndsIt() {
        List<Leg> legs = Leg.ofStretch(START, List.of(east(0, 0, 2), east(2, 0, 2), east(4, 0, 2)), List.of(0, 0, 0), true);
        assertEquals(List.of(true, false, false), legs.stream().map(Leg::startsStretch).toList());
        assertEquals(List.of(false, false, true), legs.stream().map(Leg::endsStretch).toList());
    }

    @Test
    void legsDrawnShortOfAnEndTheyCantReachNeverEndTheStretch() {
        List<Leg> legs = Leg.ofStretch(START, List.of(east(0, 0, 2), east(2, 0, 2)), List.of(0, 0), false);
        assertEquals(List.of(false, false), legs.stream().map(Leg::endsStretch).toList());
        assertEquals(true, legs.getFirst().startsStretch());
    }

    @Test
    void eachLegStartsAtTheHeightTheLegBeforeItRunsLevelAt() {
        List<Leg> legs = Leg.ofStretch(START, List.of(east(0, 0, 2), east(2, 0, 2), east(4, 0, 2)), List.of(2, 0, -3), true);
        assertEquals(List.of(START, new BlockPos(2, 66, 0), new BlockPos(4, 66, 0)),
                legs.stream().map(Leg::from).toList());
        assertEquals(new BlockPos(6, 63, 0), legs.getLast().to());
    }

    /** The columns of a leg running east {@code length} blocks from ({@code x}, {@code z}). */
    private static List<Column> east(int x, int z, int length) {
        return IntStream.rangeClosed(0, length)
                .mapToObj(i -> new Column(x + i, z, Direction.EAST))
                .toList();
    }
}
