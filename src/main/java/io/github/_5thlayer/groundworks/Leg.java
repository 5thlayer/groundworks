// SPDX-FileCopyrightText: 2026 5thlayer
// SPDX-License-Identifier: MIT

package io.github._5thlayer.groundworks;

import java.util.List;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;

/**
 * One Leg of a Stretch, as Groundworks hands it to the {@link LegBuilder} that builds it (ADR
 * 0004): its first anchor, at the height the stretch has there, its rise, and its route seen from
 * above, as the ordered columns it passes from its first anchor to its last.
 *
 * <p>The leg rises or falls by {@code rise} right after its first anchor, along its first
 * direction, and then runs level to {@link #to its last anchor}. Groundworks owns that shape; what
 * a rise is built of, slopes or a straight climb, is the builder's.
 */
public record Leg(BlockPos from, int rise, List<Column> route) {

    /** A column the leg passes, seen from above, and the way the leg travels through it. */
    public record Column(int x, int z, Direction travel) {
    }

    public Leg {
        from = from.immutable();
        route = List.copyOf(route);
    }

    /** The leg's last anchor, at the height the leg runs level at after its rise. */
    public BlockPos to() {
        Column last = route.getLast();
        return new BlockPos(last.x(), from.getY() + rise, last.z());
    }
}
