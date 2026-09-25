// SPDX-FileCopyrightText: 2026 5thlayer
// SPDX-License-Identifier: MIT

package io.github._5thlayer.groundworks;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;

import io.github._5thlayer.groundworks.PlacementPlan.Placed;

import net.minecraft.core.BlockPos;
import org.junit.jupiter.api.Test;

/** The plan's own rules. Its states are null here, since none of them reads a state. */
class PlacementPlanTest {

    /** A Consumer's own reason, as each Consumer names them. */
    private enum ConsumerRefusal implements Refusal {
        COLUMN_FULL
    }

    private static Placed at(int x, int y, int z) {
        return new Placed(new BlockPos(x, y, z), null);
    }

    @Test
    void anAcceptedPlanIsNeitherRefusedNorAReplace() {
        PlacementPlan plan = PlacementPlan.accepted(List.of(at(0, 0, 0), at(1, 0, 0)));
        assertFalse(plan.isRefused());
        assertFalse(plan.isReplace());
        assertEquals(2, plan.blocks().size());
    }

    @Test
    void aRefusedPlanStillCarriesItsBlocks() {
        PlacementPlan plan = PlacementPlan.refused(List.of(at(0, 0, 0)), Refusal.Vanilla.VANILLA);
        assertTrue(plan.isRefused());
        assertSame(Refusal.Vanilla.VANILLA, plan.refusal());
        assertEquals(List.of(at(0, 0, 0)), plan.blocks());
    }

    @Test
    void aConsumerRefusesWithItsOwnReason() {
        PlacementPlan plan = PlacementPlan.refused(List.of(at(0, 0, 0)), ConsumerRefusal.COLUMN_FULL);
        assertSame(ConsumerRefusal.COLUMN_FULL, plan.refusal());
    }

    @Test
    void aReplaceReplacesEveryBlockItPlaces() {
        PlacementPlan plan = PlacementPlan.replacing(List.of(at(0, 0, 0), at(0, 1, 0)), null);
        assertTrue(plan.isReplace());
        assertFalse(plan.isRefused());
        assertEquals(List.of(new BlockPos(0, 0, 0), new BlockPos(0, 1, 0)), plan.replaces());
    }

    @Test
    void aRefusedReplaceIsBoth() {
        PlacementPlan plan = PlacementPlan.replacing(at(0, 0, 0), ConsumerRefusal.COLUMN_FULL);
        assertTrue(plan.isReplace());
        assertTrue(plan.isRefused());
    }

    @Test
    void aPlanKeepsItsBlocksWhenTheCallersListChanges() {
        List<Placed> blocks = new ArrayList<>(List.of(at(0, 0, 0)));
        PlacementPlan plan = PlacementPlan.accepted(blocks);
        blocks.add(at(1, 0, 0));
        assertEquals(1, plan.blocks().size());
    }
}
