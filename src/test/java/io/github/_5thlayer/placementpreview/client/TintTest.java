// SPDX-FileCopyrightText: 2026 5thlayer
// SPDX-License-Identifier: MIT

package io.github._5thlayer.placementpreview.client;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;

import io.github._5thlayer.placementpreview.PlacementPlan;
import io.github._5thlayer.placementpreview.PlacementPlan.Placed;
import io.github._5thlayer.placementpreview.Refusal;

import net.minecraft.core.BlockPos;
import org.junit.jupiter.api.Test;

class TintTest {

    private static final BlockPos PLACED = new BlockPos(0, 0, 0);
    private static final BlockPos REPLACED = new BlockPos(1, 0, 0);

    /** A stretch that replaces one block and places the other, as a belt stretch over a turn does. */
    private static PlacementPlan mixed(Refusal refusal) {
        return new PlacementPlan(List.of(new Placed(PLACED, null), new Placed(REPLACED, null)),
                List.of(REPLACED), refusal);
    }

    @Test
    void anAcceptedPlanIsTheBlocksOwnColours() {
        PlacementPlan plan = PlacementPlan.accepted(List.of(new Placed(PLACED, null)));
        assertEquals(Tint.ACCEPTED, Tint.of(plan));
        assertEquals(Tint.ACCEPTED, Tint.of(plan, PLACED));
    }

    @Test
    void aReplaceIsBlueOnlyWhereItReplaces() {
        PlacementPlan plan = mixed(null);
        assertEquals(Tint.REPLACE, Tint.of(plan));
        assertEquals(Tint.REPLACE, Tint.of(plan, REPLACED));
        assertEquals(Tint.ACCEPTED, Tint.of(plan, PLACED));
    }

    @Test
    void aRefusedPlanIsRedEverywhereEvenWhereItReplaces() {
        PlacementPlan plan = mixed(Refusal.Vanilla.VANILLA);
        assertEquals(Tint.REFUSED, Tint.of(plan));
        assertEquals(Tint.REFUSED, Tint.of(plan, REPLACED));
        assertEquals(Tint.REFUSED, Tint.of(plan, PLACED));
    }

    @Test
    void everyTintIsHalfSeeThrough() {
        for (int tint : new int[] {Tint.ACCEPTED, Tint.REFUSED, Tint.REPLACE}) {
            assertEquals(0x80, tint >>> 24);
        }
    }
}
