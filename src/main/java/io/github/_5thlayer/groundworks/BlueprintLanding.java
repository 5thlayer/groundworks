// SPDX-FileCopyrightText: 2026 5thlayer
// SPDX-License-Identifier: MIT

package io.github._5thlayer.groundworks;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Vec3i;

/**
 * Where a {@linkplain Blueprints Blueprint}'s box lands for a paste: its bottom centre resting on
 * the aimed face, at the spot a block placed at the aim would take, in the orientation it was
 * captured in. On a top face the bottom layer is centred on the spot; under a bottom face the top
 * layer is; against a side the box reaches away from the face, its near side on the spot, centred
 * along the face and its bottom level with the spot, so a paste never stands in what was aimed at.
 * A side of even length has the spot just past its middle.
 *
 * @param origin where the box's lowest corner, its block {@code (0, 0, 0)}, lands
 */
record BlueprintLanding(BlockPos origin) {

    BlueprintLanding {
        origin = origin.immutable();
    }

    static BlueprintLanding of(Vec3i size, BlockPos spot, Direction face) {
        int x = spot.getX() - size.getX() / 2;
        int y = face == Direction.DOWN ? spot.getY() - (size.getY() - 1) : spot.getY();
        int z = spot.getZ() - size.getZ() / 2;
        switch (face) {
            case EAST -> x = spot.getX();
            case WEST -> x = spot.getX() - (size.getX() - 1);
            case SOUTH -> z = spot.getZ();
            case NORTH -> z = spot.getZ() - (size.getZ() - 1);
            default -> {
            }
        }
        return new BlueprintLanding(new BlockPos(x, y, z));
    }

    /** Where the box's block at {@code local}, counted from its lowest corner, lands. */
    BlockPos at(BlockPos local) {
        return origin.offset(local);
    }
}
