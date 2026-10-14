// SPDX-FileCopyrightText: 2026 5thlayer
// SPDX-License-Identifier: MIT

package io.github._5thlayer.groundworks;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;

import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;

import org.junit.jupiter.api.Test;

/**
 * Rotate the Plan on an Assembly: the held stack's quarter-turn turns the way the Key faces, and
 * the shape turns with it about the Key.
 */
class AssemblyShapeTurnTest {

    /** Cobblestone forward of the Key's column, stone under the Key and stone to the player's right, the Key on top. */
    private static final String TOWER = """
            {
              "key": "K",
              "legend": {"K": "minecraft:lodestone", "s": "minecraft:stone", "c": "minecraft:cobblestone"},
              "layers": [
                ["c ",
                 "ss"],
                ["  ",
                 "K "]
              ]
            }""";

    private static final BlockPos SPOT = new BlockPos(10, 64, 10);

    @Test
    void aQuarterTurnTurnsTheShapeClockwiseAboutTheKeyOnTopOfTheFacing() {
        // Looking east the Key faces west: forward is east, the right is south. A quarter
        // clockwise, forward is south and the right is west.
        assertEquals(List.of(SPOT.south(), SPOT, SPOT.west(), SPOT.above()),
                positionsTurned(QuarterTurn.NONE.rotate(), Direction.WEST));
    }

    @Test
    void aQuarterTurnBackTurnsItTheOtherWay() {
        // A quarter back from looking east: forward is north and the right is east.
        assertEquals(List.of(SPOT.north(), SPOT, SPOT.east(), SPOT.above()),
                positionsTurned(QuarterTurn.NONE.reverseRotate(), Direction.WEST));
    }

    @Test
    void fourQuartersBringTheShapeBackAndTheKeyNeverMoves() {
        QuarterTurn turn = QuarterTurn.NONE;
        for (int quarter = 0; quarter < 4; quarter++) {
            assertEquals(SPOT.above(), positionsTurned(turn, Direction.NORTH).getLast());
            turn = turn.rotate();
        }
        assertEquals(positionsTurned(QuarterTurn.NONE, Direction.NORTH), positionsTurned(turn, Direction.NORTH));
    }

    /** The tower's positions with the Key facing {@code facing} turned by {@code turn}, as a placement context turns it. */
    private static List<BlockPos> positionsTurned(QuarterTurn turn, Direction facing) {
        AssemblyShape shape = AssemblyShape.CODEC.parse(JsonOps.INSTANCE, JsonParser.parseString(TOWER)).getOrThrow();
        return shape.positions(SPOT, Direction.UP, turn.turn(facing)).stream().map(AssemblyShape.At::pos).toList();
    }
}
