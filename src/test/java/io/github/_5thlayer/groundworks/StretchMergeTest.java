// SPDX-FileCopyrightText: 2026 5thlayer
// SPDX-License-Identifier: MIT

package io.github._5thlayer.groundworks;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.List;

import io.github._5thlayer.groundworks.PlacementPlan.Placed;

import net.minecraft.core.BlockPos;
import org.junit.jupiter.api.Test;

/**
 * A stretch's plan from its legs' plans: a later leg wins a position an earlier one takes, and
 * replaces there as the later leg does; the first refusal is kept. Its states are null here, since
 * the merge reads none.
 */
class StretchMergeTest {

    private static final BlockPos A = new BlockPos(0, 64, 0);
    private static final BlockPos B = new BlockPos(1, 64, 0);
    private static final BlockPos C = new BlockPos(1, 64, 1);

    private static Placed at(BlockPos pos) {
        return new Placed(pos, null);
    }

    private static List<BlockPos> positions(PlacementPlan plan) {
        return plan.blocks().stream().map(Placed::pos).toList();
    }

    @Test
    void legsJoinInTheirOrderAndASharedPositionIsLaidOnce() {
        PlacementPlan merged = Stretches.merged(List.of(PlacementPlan.accepted(List.of(at(A), at(B))),
                PlacementPlan.accepted(List.of(at(B), at(C)))), null);
        assertEquals(List.of(A, B, C), positions(merged));
        assertNull(merged.refusal());
    }

    @Test
    void aLaterLegThatPlacesWhereAnEarlierReplacesNoLongerReplacesThere() {
        PlacementPlan merged = Stretches.merged(List.of(PlacementPlan.replacing(List.of(at(A), at(B)), null),
                PlacementPlan.accepted(List.of(at(B), at(C)))), null);
        assertEquals(List.of(A), merged.replaces());
    }

    @Test
    void aLaterLegThatReplacesWhereAnEarlierPlacesReplacesThere() {
        PlacementPlan merged = Stretches.merged(List.of(PlacementPlan.accepted(List.of(at(A), at(B))),
                PlacementPlan.replacing(List.of(at(B)), null)), null);
        assertEquals(List.of(B), merged.replaces());
    }

    @Test
    void theFirstLegsRefusalIsKept() {
        PlacementPlan merged = Stretches.merged(List.of(PlacementPlan.accepted(List.of(at(A))),
                PlacementPlan.refused(List.of(at(B)), Refusal.Stretch.NOT_ENOUGH_ITEMS),
                PlacementPlan.refused(List.of(at(C)), Refusal.Stretch.MAY_NOT_BUILD)), null);
        assertEquals(Refusal.Stretch.NOT_ENOUGH_ITEMS, merged.refusal());
        assertEquals(List.of(A, B, C), positions(merged));
    }

    @Test
    void theStretchsOwnRefusalComesBeforeAnyLegs() {
        PlacementPlan merged = Stretches.merged(List.of(PlacementPlan.refused(List.of(at(A)),
                Refusal.Stretch.MAY_NOT_BUILD)), Refusal.Stretch.BEHIND_THE_LOOK);
        assertEquals(Refusal.Stretch.BEHIND_THE_LOOK, merged.refusal());
    }
}
