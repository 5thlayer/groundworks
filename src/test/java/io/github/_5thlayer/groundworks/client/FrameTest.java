// SPDX-FileCopyrightText: 2026 5thlayer
// SPDX-License-Identifier: MIT

package io.github._5thlayer.groundworks.client;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.ArrayList;
import java.util.List;

import io.github._5thlayer.groundworks.PlacementPlan;
import io.github._5thlayer.groundworks.PlacementPlan.Placed;
import io.github._5thlayer.groundworks.Refusal;

import net.minecraft.core.BlockPos;
import net.neoforged.bus.api.BusBuilder;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.IEventBus;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;

/**
 * One frame's order, on a bus of its own. The aim is empty, since nothing here reads it: the order
 * is the contract Consumers compile against.
 */
class FrameTest {

    private static final Aim AIM = new Aim(null, null, null, null, null);
    private static final PlacementPlan PLAN = PlacementPlan.accepted(List.of(new Placed(new BlockPos(0, 0, 0), null)));

    private final IEventBus bus = BusBuilder.builder().build();
    private final List<String> calls = new ArrayList<>();

    private void run(@Nullable PlacementPlan plan) {
        PlacementPreview.frame(bus, AIM, () -> {
            calls.add("plan");
            return plan;
        }, drawn -> calls.add("draw"));
    }

    @Test
    void withNoTakeoverTheMarkerThenThePlanThenItsOverlaysDraw() {
        bus.addListener(PlacementPreviewEvent.Marker.class, event -> calls.add("marker"));
        bus.addListener(PlacementPreviewEvent.Takeover.class, event -> calls.add("takeover passes"));
        bus.addListener(PlacementPreviewEvent.Overlay.class, event -> calls.add("overlay"));
        run(PLAN);
        assertEquals(List.of("marker", "takeover passes", "plan", "draw", "overlay"), calls);
    }

    @Test
    void theFirstTakeoverToDrawCancelsTheRestAndThePlanButNotTheMarker() {
        bus.addListener(PlacementPreviewEvent.Marker.class, event -> calls.add("marker"));
        bus.addListener(EventPriority.HIGH, PlacementPreviewEvent.Takeover.class, event -> {
            calls.add("takeover draws");
            event.setCanceled(true);
        });
        bus.addListener(EventPriority.LOW, PlacementPreviewEvent.Takeover.class, event -> calls.add("second takeover"));
        bus.addListener(PlacementPreviewEvent.Overlay.class, event -> calls.add("overlay"));
        run(PLAN);
        assertEquals(List.of("marker", "takeover draws"), calls);
    }

    @Test
    void noPlanDrawsNothingAndFiresNoOverlay() {
        bus.addListener(PlacementPreviewEvent.Overlay.class, event -> calls.add("overlay"));
        run(null);
        assertEquals(List.of("plan"), calls);
    }

    @Test
    void aPlanWithNoBlocksDrawsNothingAndFiresNoOverlay() {
        bus.addListener(PlacementPreviewEvent.Overlay.class, event -> calls.add("overlay"));
        run(PlacementPlan.accepted(List.of()));
        assertEquals(List.of("plan"), calls);
    }

    @Test
    void aRefusedPlanStillGetsItsOverlaysWithItsTint() {
        PlacementPlan refused = PlacementPlan.refused(PLAN.blocks(), Refusal.Vanilla.VANILLA);
        List<PlacementPreviewEvent.Overlay> overlays = new ArrayList<>();
        bus.addListener(PlacementPreviewEvent.Overlay.class, overlays::add);
        run(refused);
        assertEquals(1, overlays.size());
        assertEquals(refused, overlays.getFirst().getPlan());
        assertEquals(Tint.REFUSED, overlays.getFirst().getTint());
    }
}
