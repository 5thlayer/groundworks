// SPDX-FileCopyrightText: 2026 5thlayer
// SPDX-License-Identifier: MIT

package io.github._5thlayer.groundworks;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import java.util.Map;
import java.util.Set;

import net.minecraft.core.BlockPos;

import org.junit.jupiter.api.Test;

/**
 * A paste's support-first order: what stands on the world alone goes first, in the Blueprint's
 * order, then what stands on that, and so on; what never stands is left out.
 */
class SupportOrderTest {

    private static final BlockPos WALL = new BlockPos(0, 1, 0);
    private static final BlockPos TORCH = new BlockPos(1, 1, 0);
    private static final BlockPos FLOOR = new BlockPos(0, 0, 0);
    private static final BlockPos RAIL = new BlockPos(0, 1, 1);
    private static final BlockPos LADDER = new BlockPos(2, 1, 0);

    /** Each position's support, by the positions it needs laid first; one missing stands alone. */
    private static SupportOrder.Stands needing(Map<BlockPos, Set<BlockPos>> needs) {
        return laid -> pos -> laid.containsAll(needs.getOrDefault(pos, Set.of()));
    }

    @Test
    void aBlockStandingAloneKeepsTheBlueprintsOrder() {
        SupportOrder order = SupportOrder.of(List.of(FLOOR, WALL, RAIL), needing(Map.of()));
        assertEquals(List.of(FLOOR, WALL, RAIL), order.laid());
        assertEquals(List.of(), order.cantStand());
    }

    @Test
    void aBlockIsLaidAfterWhatItNeedsWhereverItComesInTheBlueprint() {
        // The torch comes before its wall in the Blueprint's order, as a lower x does.
        SupportOrder order = SupportOrder.of(List.of(TORCH, WALL), needing(Map.of(TORCH, Set.of(WALL))));
        assertEquals(List.of(WALL, TORCH), order.laid());
    }

    @Test
    void aChainOfSupportsIsLaidInItsOrder() {
        SupportOrder order = SupportOrder.of(List.of(LADDER, TORCH, WALL),
                needing(Map.of(LADDER, Set.of(TORCH), TORCH, Set.of(WALL))));
        assertEquals(List.of(WALL, TORCH, LADDER), order.laid());
    }

    @Test
    void aBlockThatNeverStandsIsLeftOutAndTheRestLaid() {
        BlockPos outside = new BlockPos(9, 9, 9);
        SupportOrder order = SupportOrder.of(List.of(TORCH, FLOOR, RAIL, LADDER),
                needing(Map.of(TORCH, Set.of(outside), RAIL, Set.of(FLOOR), LADDER, Set.of(TORCH))));
        assertEquals(List.of(FLOOR, RAIL), order.laid());
        assertEquals(List.of(TORCH, LADDER), order.cantStand());
    }
}
