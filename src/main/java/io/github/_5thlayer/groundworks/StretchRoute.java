// SPDX-FileCopyrightText: 2026 5thlayer
// SPDX-License-Identifier: MIT

package io.github._5thlayer.groundworks;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import org.jspecify.annotations.Nullable;

/**
 * A Stretch's route seen from above, Beltworks' rule as it stood: each leg runs along its first
 * direction, then turns once toward its next anchor. The first leg's first direction is the look
 * stored with the start, and a later leg's is the way the leg before it ends, so a stretch never
 * starts a leg by turning back on itself. Heights play no part.
 */
final class StretchRoute {

    private StretchRoute() {
    }

    /**
     * The columns of each leg, from {@code start} through each of {@code ends} in turn, or
     * {@code null} when an end lies behind the way its leg heads. Each leg's columns run from its
     * first anchor to its last, so a leg's last column is the next one's first.
     */
    static @Nullable List<List<Leg.Column>> legs(BlockPos start, Direction look, List<BlockPos> ends) {
        List<List<Leg.Column>> legs = new ArrayList<>();
        int x = start.getX();
        int z = start.getZ();
        Direction heading = look;
        for (BlockPos end : ends) {
            List<Leg.Column> leg = leg(x, z, heading, end.getX(), end.getZ());
            if (leg == null) {
                return null;
            }
            legs.add(leg);
            Leg.Column last = leg.getLast();
            x = last.x();
            z = last.z();
            heading = last.travel();
        }
        return legs;
    }

    // Along the heading as far as the end goes that way, then from the turn across to it.
    private static @Nullable List<Leg.Column> leg(int x, int z, Direction heading, int endX, int endZ) {
        Direction right = heading.getClockWise();
        int dx = endX - x;
        int dz = endZ - z;
        int along = dx * heading.getStepX() + dz * heading.getStepZ();
        int aside = dx * right.getStepX() + dz * right.getStepZ();
        if (along < 0) {
            return null;
        }
        List<Leg.Column> columns = new ArrayList<>();
        for (int i = 0; i < along; i++) {
            columns.add(new Leg.Column(x + heading.getStepX() * i, z + heading.getStepZ() * i, heading));
        }
        int turnX = x + heading.getStepX() * along;
        int turnZ = z + heading.getStepZ() * along;
        Direction turn = aside == 0 ? heading : aside > 0 ? right : right.getOpposite();
        for (int i = 0; i <= Math.abs(aside); i++) {
            columns.add(new Leg.Column(turnX + turn.getStepX() * i, turnZ + turn.getStepZ() * i, turn));
        }
        return columns;
    }
}
