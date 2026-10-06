// SPDX-FileCopyrightText: 2026 5thlayer
// SPDX-License-Identifier: MIT

package io.github._5thlayer.groundworks;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Vec3i;
import org.jspecify.annotations.Nullable;

/**
 * The shape of a {@linkplain Footprint Footprint}: where each of its blocks stands relative
 * to its Origin (ADR 0009), the origin first, then each Part, numbered from 1 in the order given.
 *
 * <p>The offsets are in the origin's own frame, {@code x} forward, {@code y} up and {@code z} to the
 * side, turned with the origin's facing as {@link Local#inWorld} turns them. That turn mirrors as
 * well as rotates, and is the one the FactoryWorks Pack's footprints were built on, so their shapes
 * and the models drawn over them carry over unchanged.
 */
public record FootprintShape(List<Local> offsets) {

    /** The most parts one footprint may have, which its part block numbers: a 5x5x5 block of them less the origin. */
    public static final int MAX_PARTS = 124;

    private static final Local ORIGIN = new Local(0, 0, 0);

    public FootprintShape {
        if (offsets.isEmpty() || !offsets.getFirst().equals(ORIGIN)) {
            throw new IllegalArgumentException("a footprint starts at its origin, got " + offsets);
        }
        if (new HashSet<>(offsets).size() != offsets.size()) {
            throw new IllegalArgumentException("a footprint names a position twice: " + offsets);
        }
        if (offsets.size() - 1 > MAX_PARTS) {
            throw new IllegalArgumentException("a footprint has at most " + MAX_PARTS + " parts, got " + (offsets.size() - 1));
        }
        offsets = List.copyOf(offsets);
    }

    /** The origin and these parts, numbered from 1 in this order. */
    public static FootprintShape of(Local... parts) {
        List<Local> offsets = new ArrayList<>(parts.length + 1);
        offsets.add(ORIGIN);
        offsets.addAll(List.of(parts));
        return new FootprintShape(offsets);
    }

    /**
     * Factorio's tile square, standing as tall as it is wide, with the origin at the bottom centre,
     * so the footprint stands centred on the block the player clicks. The tile width runs to the side
     * and up, the tile height forward. Both are odd, so there is a centre block.
     *
     * <p>This builds a wall: the footprint stands as tall as it is wide, and its depth along the
     * facing is the tile height. For a machine lying on the ground, a tile square of any height,
     * use {@link #square}.
     */
    public static FootprintShape standing(int tileWidth, int tileHeight) {
        if (tileWidth % 2 == 0 || tileHeight % 2 == 0) {
            throw new IllegalArgumentException("a " + tileWidth + "x" + tileHeight + " footprint has no centre block");
        }
        int halfX = tileHeight / 2;
        int halfZ = tileWidth / 2;
        List<Local> parts = new ArrayList<>();
        for (int y = 0; y < tileWidth; y++) {
            for (int x = -halfX; x <= halfX; x++) {
                for (int z = -halfZ; z <= halfZ; z++) {
                    if (x != 0 || y != 0 || z != 0) {
                        parts.add(new Local(x, y, z));
                    }
                }
            }
        }
        return of(parts.toArray(Local[]::new));
    }

    /**
     * Factorio's tile square lying on the ground: {@code tiles} by {@code tiles} blocks and {@code height}
     * blocks tall, with the origin at its bottom centre. Parts are numbered from the bottom layer up,
     * with {@code x} and then {@code z} running from {@code -half} to {@code +half}, the origin left out.
     *
     * @param tiles  the side of the square, odd so that there is a centre block
     * @param height the blocks it stands tall, at least 1
     * @throws IllegalArgumentException for an even {@code tiles}, a {@code height} below 1, or a shape of
     *                                  more than {@link #MAX_PARTS} parts
     */
    public static FootprintShape square(int tiles, int height) {
        if (tiles < 1 || tiles % 2 == 0) {
            throw new IllegalArgumentException("a " + tiles + "x" + tiles + " square has no centre block");
        }
        if (height < 1) {
            throw new IllegalArgumentException("a footprint is at least 1 block tall, got " + height);
        }
        int half = tiles / 2;
        List<Local> parts = new ArrayList<>();
        for (int y = 0; y < height; y++) {
            for (int x = -half; x <= half; x++) {
                for (int z = -half; z <= half; z++) {
                    if (x != 0 || y != 0 || z != 0) {
                        parts.add(new Local(x, y, z));
                    }
                }
            }
        }
        return of(parts.toArray(Local[]::new));
    }

    public int partCount() {
        return offsets.size() - 1;
    }

    public Local offsetOfPart(int part) {
        if (!hasPart(part)) {
            throw new IllegalArgumentException("a part is numbered 1 to " + partCount() + ", got " + part);
        }
        return offsets.get(part);
    }

    /** Where each block stands for an origin here facing this way: the origin first, then each part in its number's order. */
    public List<BlockPos> positions(BlockPos origin, Direction facing) {
        List<BlockPos> positions = new ArrayList<>(offsets.size());
        for (Local offset : offsets) {
            positions.add(origin.offset(offset.inWorld(facing)));
        }
        return positions;
    }

    /** Whether this shape has a part of this number. */
    public boolean hasPart(int number) {
        return number >= 1 && number <= partCount();
    }

    /**
     * Where the origin stands, from a part's position, its number and its footprint's facing, or
     * {@code null} for a number outside this shape, as a part left in a world from before its
     * Consumer changed the shape is numbered.
     */
    public @Nullable BlockPos originOf(BlockPos part, int number, Direction facing) {
        return hasPart(number) ? part.subtract(offsetOfPart(number).inWorld(facing)) : null;
    }

    /** One position relative to the origin, in its frame: {@code x} forward, {@code y} up, {@code z} to the side. */
    public record Local(int x, int y, int z) {

        /** This offset in the world, for an origin facing this horizontal way. */
        public Vec3i inWorld(Direction facing) {
            return switch (facing) {
                case NORTH -> new Vec3i(z, y, x);
                case WEST -> new Vec3i(x, y, -z);
                case SOUTH -> new Vec3i(-z, y, -x);
                case EAST -> new Vec3i(-x, y, z);
                case UP, DOWN -> throw new IllegalArgumentException("a footprint faces a horizontal way, got " + facing);
            };
        }
    }
}
