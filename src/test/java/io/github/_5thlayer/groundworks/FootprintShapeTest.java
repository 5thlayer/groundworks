// SPDX-FileCopyrightText: 2026 5thlayer
// SPDX-License-Identifier: MIT

package io.github._5thlayer.groundworks;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Vec3i;

import org.junit.jupiter.api.Test;

class FootprintShapeTest {

    private static final FootprintShape.Local FORWARD = new FootprintShape.Local(1, 0, 0);
    private static final FootprintShape.Local UP = new FootprintShape.Local(0, 1, 0);
    private static final FootprintShape.Local SIDE = new FootprintShape.Local(0, 0, 1);

    @Test
    void aFootprintStartsAtItsOriginAndNumbersItsPartsFromOne() {
        FootprintShape shape = FootprintShape.of(FORWARD, UP);
        assertEquals(2, shape.partCount());
        assertEquals(FORWARD, shape.offsetOfPart(1));
        assertEquals(UP, shape.offsetOfPart(2));
        assertThrows(IllegalArgumentException.class, () -> shape.offsetOfPart(0));
        assertThrows(IllegalArgumentException.class, () -> shape.offsetOfPart(3));
    }

    @Test
    void aFootprintThatNamesAPositionTwiceOrDoesNotStartAtItsOriginIsRejected() {
        assertThrows(IllegalArgumentException.class, () -> FootprintShape.of(FORWARD, FORWARD));
        assertThrows(IllegalArgumentException.class, () -> FootprintShape.of(new FootprintShape.Local(0, 0, 0)));
        assertThrows(IllegalArgumentException.class, () -> new FootprintShape(List.of(FORWARD)));
    }

    @Test
    void aFootprintHasAtMost124Parts() {
        assertEquals(26, FootprintShape.standing(3, 3).partCount());
        assertEquals(74, FootprintShape.standing(5, 3).partCount());
        assertEquals(124, line(124).partCount());
        assertThrows(IllegalArgumentException.class, () -> line(125));
    }

    private static FootprintShape line(int parts) {
        FootprintShape.Local[] line = new FootprintShape.Local[parts];
        for (int i = 0; i < parts; i++) {
            line[i] = new FootprintShape.Local(i + 1, 0, 0);
        }
        return FootprintShape.of(line);
    }

    /** Craftworks' Assemblers.square(2): y, then x, then z, the origin left out. */
    @Test
    void aSquareOfThreeIsCraftworksAssemblerShapeInTheSameOrder() {
        List<FootprintShape.Local> expected = new java.util.ArrayList<>();
        for (int y = 0; y < 2; y++) {
            for (int x = -1; x <= 1; x++) {
                for (int z = -1; z <= 1; z++) {
                    if (x != 0 || y != 0 || z != 0) {
                        expected.add(new FootprintShape.Local(x, y, z));
                    }
                }
            }
        }
        FootprintShape square = FootprintShape.square(3, 2);
        assertEquals(17, square.partCount());
        assertEquals(expected, square.offsets().subList(1, square.offsets().size()));
        assertEquals(new FootprintShape.Local(-1, 0, -1), square.offsetOfPart(1));
        assertEquals(new FootprintShape.Local(1, 1, 1), square.offsetOfPart(17));
    }

    @Test
    void aSquareHasItsTilesByTilesBlocksPerLayerUpToTheCap() {
        assertEquals(74, FootprintShape.square(5, 3).partCount());
        assertEquals(124, FootprintShape.square(5, 5).partCount());
        assertEquals(new FootprintShape.Local(0, 0, 0), FootprintShape.square(5, 3).offsets().getFirst());
        assertThrows(IllegalArgumentException.class, () -> FootprintShape.square(5, 6));
        assertThrows(IllegalArgumentException.class, () -> FootprintShape.square(4, 1));
        assertThrows(IllegalArgumentException.class, () -> FootprintShape.square(3, 0));
    }

    @Test
    void aStandingFootprintWithNoCentreBlockIsRejected() {
        assertThrows(IllegalArgumentException.class, () -> FootprintShape.standing(2, 1));
    }

    /** The turn the Pack's machines and the models drawn over them were built on. */
    @Test
    void eachFacingTurnsTheFrameAsThePacksMachinesWereBuilt() {
        FootprintShape.Local local = new FootprintShape.Local(1, 2, 3);
        assertEquals(new Vec3i(3, 2, 1), local.inWorld(Direction.NORTH));
        assertEquals(new Vec3i(1, 2, -3), local.inWorld(Direction.WEST));
        assertEquals(new Vec3i(-3, 2, -1), local.inWorld(Direction.SOUTH));
        assertEquals(new Vec3i(-1, 2, 3), local.inWorld(Direction.EAST));
    }

    @Test
    void aClockwiseFacingTurnsEveryOffsetAQuarterClockwiseSeenFromAbove() {
        for (FootprintShape.Local local : List.of(FORWARD, SIDE, new FootprintShape.Local(2, 1, -1))) {
            for (Direction facing : Direction.Plane.HORIZONTAL) {
                Vec3i before = local.inWorld(facing);
                // Clockwise seen from above takes north (-z) to east (+x).
                assertEquals(new Vec3i(-before.getZ(), before.getY(), before.getX()), local.inWorld(facing.getClockWise()));
            }
        }
    }

    @Test
    void positionsAreTheOriginFirstThenEachPart() {
        FootprintShape shape = FootprintShape.of(FORWARD, UP);
        BlockPos origin = new BlockPos(10, 64, 10);
        assertEquals(List.of(origin, origin.south(), origin.above()), shape.positions(origin, Direction.NORTH));
    }

    @Test
    void aPartFindsItsOriginFromItsOwnPositionNumberAndFacing() {
        FootprintShape shape = FootprintShape.standing(3, 3);
        BlockPos origin = new BlockPos(-4, 70, 8);
        for (Direction facing : Direction.Plane.HORIZONTAL) {
            List<BlockPos> positions = shape.positions(origin, facing);
            for (int part = 1; part <= shape.partCount(); part++) {
                assertEquals(origin, shape.originOf(positions.get(part), part, facing));
            }
        }
    }

    /** A part left from before its Consumer shrank the shape names no origin, rather than throwing. */
    @Test
    void aPartNumberedOutsideTheShapeHasNoOrigin() {
        FootprintShape shape = FootprintShape.of(FORWARD, UP);
        BlockPos part = new BlockPos(3, 64, 3);
        assertNull(shape.originOf(part, 3, Direction.NORTH));
        assertNull(shape.originOf(part, 0, Direction.NORTH));
        assertFalse(shape.hasPart(3));
        assertTrue(shape.hasPart(2));
    }

    @Test
    void aStandingFootprintIsCentredOnItsOriginAndRisesFromIt() {
        Set<BlockPos> positions = new HashSet<>(FootprintShape.standing(3, 1).positions(BlockPos.ZERO, Direction.NORTH));
        assertEquals(9, positions.size());
        for (BlockPos pos : positions) {
            assertEquals(0, pos.getZ(), "one tile deep along the facing");
            assertTrue(Math.abs(pos.getX()) <= 1 && pos.getY() >= 0 && pos.getY() <= 2, pos.toString());
        }
    }
}
