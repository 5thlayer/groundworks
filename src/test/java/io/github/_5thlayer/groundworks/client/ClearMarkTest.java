// SPDX-FileCopyrightText: 2026 5thlayer
// SPDX-License-Identifier: MIT

package io.github._5thlayer.groundworks.client;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;

import io.github._5thlayer.groundworks.PlacementPlan;
import io.github._5thlayer.groundworks.PlacementPlan.Placed;
import io.github._5thlayer.groundworks.Refusal;

import net.minecraft.core.BlockPos;
import org.junit.jupiter.api.Test;

class ClearMarkTest {

    private static final BlockPos PLACED = new BlockPos(0, 0, 0);
    private static final BlockPos GRASS = new BlockPos(1, 0, 0);

    private static PlacementPlan clearingGrass(Refusal refusal) {
        return new PlacementPlan(List.of(new Placed(PLACED, null)), List.of(), List.of(GRASS), refusal);
    }

    @Test
    void anAcceptedPlanMarksWhatItClears() {
        assertEquals(List.of(GRASS), ClearMark.marked(clearingGrass(null)));
    }

    @Test
    void aRefusedPlanMarksNothingSinceItClearsNothing() {
        assertEquals(List.of(), ClearMark.marked(clearingGrass(Refusal.Vanilla.VANILLA)));
    }

    @Test
    void aPlanClearingNothingMarksNothing() {
        assertEquals(List.of(), ClearMark.marked(PlacementPlan.accepted(List.of(new Placed(PLACED, null)))));
    }
}
