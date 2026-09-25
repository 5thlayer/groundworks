// SPDX-FileCopyrightText: 2026 5thlayer
// SPDX-License-Identifier: MIT

package io.github._5thlayer.groundworks;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import io.github._5thlayer.groundworks.PlanHull.Cell;
import io.github._5thlayer.groundworks.PlanHull.Face;
import io.github._5thlayer.groundworks.PlanHull.Side;

import org.junit.jupiter.api.Test;

class PlanHullTest {

    @Test
    void aSingleBlockShowsAllSixFaces() {
        assertEquals(6, PlanHull.boundary(List.of(new Cell(0, 0, 0))).size());
    }

    @Test
    void twoTouchingBlocksHideTheFaceBetweenThem() {
        Set<Face> faces = PlanHull.boundary(List.of(new Cell(0, 0, 0), new Cell(1, 0, 0)));
        assertEquals(10, faces.size());
        assertFalse(faces.contains(new Face(new Cell(0, 0, 0), Side.EAST)));
        assertFalse(faces.contains(new Face(new Cell(1, 0, 0), Side.WEST)));
    }

    /** A solid box shows its surface area and nothing inside, whatever its proportions. */
    @Test
    void aSolidBoxShowsOnlyItsOuterShell() {
        int[][] sizes = {{1, 1, 1}, {3, 3, 3}, {2, 5, 3}, {7, 2, 4}};
        for (int[] size : sizes) {
            int w = size[0];
            int h = size[1];
            int d = size[2];
            List<Cell> cells = new ArrayList<>();
            for (int x = 0; x < w; x++) {
                for (int y = 0; y < h; y++) {
                    for (int z = 0; z < d; z++) {
                        cells.add(new Cell(x, y, z));
                    }
                }
            }
            Set<Face> faces = PlanHull.boundary(cells);
            String box = w + "x" + h + "x" + d;
            assertEquals(2 * (w * d + w * h + d * h), faces.size(), box);
            for (Face face : faces) {
                assertFalse(cells.contains(face.cell().step(face.side())), box + " draws an inner face " + face);
            }
        }
    }

    /**
     * A hole follows the footprint rather than the bounding box: a 3x3 ring keeps the four faces
     * that look into its empty middle.
     */
    @Test
    void aHoleKeepsItsInwardFaces() {
        List<Cell> ring = new ArrayList<>();
        for (int x = 0; x < 3; x++) {
            for (int z = 0; z < 3; z++) {
                if (x != 1 || z != 1) {
                    ring.add(new Cell(x, 0, z));
                }
            }
        }
        Set<Face> faces = PlanHull.boundary(ring);
        assertEquals(8 * 6 - 2 * 8, faces.size());
        assertTrue(faces.contains(new Face(new Cell(1, 0, 0), Side.SOUTH)));
        assertTrue(faces.contains(new Face(new Cell(1, 0, 2), Side.NORTH)));
        assertTrue(faces.contains(new Face(new Cell(0, 0, 1), Side.EAST)));
        assertTrue(faces.contains(new Face(new Cell(2, 0, 1), Side.WEST)));
    }

    /** The client maps a side to Minecraft's {@code Direction} by name. */
    @Test
    void sidesAreNamedAsMinecraftsDirections() {
        assertEquals(Set.of("DOWN", "UP", "NORTH", "SOUTH", "WEST", "EAST"),
                Set.copyOf(List.of(Side.values()).stream().map(Side::name).toList()));
    }
}
