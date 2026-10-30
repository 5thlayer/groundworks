// SPDX-FileCopyrightText: 2026 5thlayer
// SPDX-License-Identifier: MIT

package io.github._5thlayer.groundworks;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Vec3i;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Where a {@linkplain Blueprints Blueprint}'s box lands for a paste: its bottom centre resting on
 * the aimed face, at the spot a block placed at the aim would take, in the orientation it was
 * captured in. On a top face the bottom layer is centred on the spot; under a bottom face the top
 * layer is; against a side the box reaches away from the face, its near side on the spot, centred
 * along the face and its bottom level with the spot, so a paste never stands in what was aimed at.
 * A side of even length has the spot just past its middle.
 *
 * <p>A {@linkplain Rotate Rotate the Plan} turn turns the box a quarter clockwise, seen from above,
 * per step from its captured orientation, each layer at its own height, and the turned box lands
 * by its turned footprint as an unturned one would.
 *
 * @param origin where the turned box's lowest corner lands
 * @param size the box's size as captured, unturned
 * @param turn how far the box is turned from its captured orientation
 */
record BlueprintLanding(BlockPos origin, Vec3i size, QuarterTurn turn) {

    BlueprintLanding {
        origin = origin.immutable();
    }

    static BlueprintLanding of(Vec3i size, BlockPos spot, Direction face) {
        return of(size, spot, face, QuarterTurn.NONE);
    }

    static BlueprintLanding of(Vec3i captured, BlockPos spot, Direction face, QuarterTurn turn) {
        Vec3i size = turn.quarters() % 2 == 0 ? captured : new Vec3i(captured.getZ(), captured.getY(), captured.getX());
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
        return new BlueprintLanding(new BlockPos(x, y, z), captured, turn);
    }

    /** Where the box's block at {@code local}, counted from its lowest corner as captured, lands. */
    BlockPos at(BlockPos local) {
        int x = local.getX();
        int z = local.getZ();
        int lastX = size.getX() - 1;
        int lastZ = size.getZ() - 1;
        return switch (turn.quarters()) {
            case 1 -> origin.offset(lastZ - z, local.getY(), x);
            case 2 -> origin.offset(lastX - x, local.getY(), lastZ - z);
            case 3 -> origin.offset(z, local.getY(), lastX - x);
            default -> origin.offset(local);
        };
    }

    /** A captured block's state, turned with the box where it lands, so its own facing turns with it. */
    BlockState turned(LevelAccessor level, BlockPos pos, BlockState state) {
        return state.rotate(level, pos, switch (turn.quarters()) {
            case 1 -> Rotation.CLOCKWISE_90;
            case 2 -> Rotation.CLOCKWISE_180;
            case 3 -> Rotation.COUNTERCLOCKWISE_90;
            default -> Rotation.NONE;
        });
    }
}
