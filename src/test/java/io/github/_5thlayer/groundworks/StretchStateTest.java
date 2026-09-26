// SPDX-FileCopyrightText: 2026 5thlayer
// SPDX-License-Identifier: MIT

package io.github._5thlayer.groundworks;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

import java.util.List;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import org.junit.jupiter.api.Test;

/**
 * A stretch's stored state, its height and its turn, through the gestures: which gesture a click
 * is, and what start, anchor, lay and clear make of them.
 */
class StretchStateTest {

    private static final ResourceKey<Level> OVERWORLD =
            ResourceKey.create(Registries.DIMENSION, Identifier.withDefaultNamespace("overworld"));
    private static final BlockPos SPOT = new BlockPos(1, 64, 2);

    private static final QuarterTurn QUARTER = QuarterTurn.of(1);

    private static StoredStretch storedAt(BlockPos start, StoredStretch.Anchor... anchors) {
        return new StoredStretch(OVERWORLD, start, Direction.EAST, List.of(anchors));
    }

    @Test
    void theStartTakesTheHeldHeightAndUsesItUp() {
        StretchState started = new StretchState(null, new Height(3), QuarterTurn.NONE)
                .started(OVERWORLD, SPOT, Direction.EAST);
        assertEquals(new StretchState(storedAt(new BlockPos(1, 67, 2)), Height.NONE, QuarterTurn.NONE), started);
    }

    @Test
    void theStartTakesTheLookTurnedByTheHeldTurnAndUsesItUp() {
        StretchState started = new StretchState(null, Height.NONE, QUARTER).started(OVERWORLD, SPOT, Direction.EAST);
        assertEquals(new StretchState(new StoredStretch(OVERWORLD, SPOT, Direction.SOUTH, List.of()), Height.NONE,
                QuarterTurn.NONE), started);
    }

    @Test
    void theStartTakesTheLookReverseTurnedByAReverseRotate() {
        StretchState started = new StretchState(null, Height.NONE, QuarterTurn.of(-1))
                .started(OVERWORLD, SPOT, Direction.EAST);
        assertEquals(Direction.NORTH, started.stored().look());
    }

    @Test
    void anAnchorLeavesTheTurnForTheNextStart() {
        StretchState anchored = new StretchState(storedAt(SPOT), Height.NONE, QUARTER).anchored(new BlockPos(5, 70, 2));
        assertEquals(QUARTER, anchored.turn());
    }

    @Test
    void anAnchorFreezesTheHeightIntoTheLegEndingThereAndUsesItUp() {
        StretchState anchored = new StretchState(storedAt(SPOT), new Height(2), QuarterTurn.NONE)
                .anchored(new BlockPos(5, 70, 2));
        assertEquals(new StretchState(storedAt(SPOT, new StoredStretch.Anchor(5, 2, 2)), Height.NONE, QuarterTurn.NONE),
                anchored);
    }

    @Test
    void anAnchorTakesItsSpotFromAboveOnly() {
        assertEquals(new StretchState(storedAt(SPOT), Height.NONE, QuarterTurn.NONE).anchored(new BlockPos(5, 70, 2)),
                new StretchState(storedAt(SPOT), Height.NONE, QuarterTurn.NONE).anchored(new BlockPos(5, 10, 2)));
    }

    @Test
    void anAnchorAtTheSameSpotAsTheLastIsIgnored() {
        StretchState state = new StretchState(storedAt(SPOT, new StoredStretch.Anchor(5, 2, 1)), new Height(-1),
                QuarterTurn.NONE);
        assertSame(state, state.anchored(new BlockPos(5, 40, 2)));
    }

    @Test
    void anAnchorAtTheStartIsIgnored() {
        StretchState state = new StretchState(storedAt(SPOT), new Height(1), QuarterTurn.NONE);
        assertSame(state, state.anchored(new BlockPos(1, 90, 2)));
    }

    @Test
    void layingOrClearingResetsTheStoredStateAndTheHeightAndLeavesTheTurnForTheNextStart() {
        StretchState state = new StretchState(storedAt(SPOT, new StoredStretch.Anchor(5, 2, 1)), new Height(2),
                QUARTER);
        assertEquals(new StretchState(null, Height.NONE, QUARTER), state.reset());
    }

    @Test
    void eachAnchorStandsAtTheStretchsHeightThere() {
        StoredStretch stored = storedAt(new BlockPos(0, 64, 0),
                new StoredStretch.Anchor(5, 0, 2), new StoredStretch.Anchor(5, 5, -3));
        assertEquals(List.of(new BlockPos(5, 66, 0), new BlockPos(5, 63, 5)), stored.anchorPositions());
    }

    @Test
    void aSneakClickStartsOrAnchorsAndAClickLaysOrPassesOn() {
        assertEquals(Stretches.Click.START, Stretches.Click.of(true, false));
        assertEquals(Stretches.Click.ANCHOR, Stretches.Click.of(true, true));
        assertEquals(Stretches.Click.LAY, Stretches.Click.of(false, true));
        assertEquals(Stretches.Click.PASS, Stretches.Click.of(false, false));
    }
}
