// SPDX-FileCopyrightText: 2026 5thlayer
// SPDX-License-Identifier: MIT

package io.github._5thlayer.groundworks;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;

/**
 * One Leg of a Stretch, as Groundworks hands it to the {@link LegBuilder} that builds it (ADR
 * 0004): its first anchor, at the height the stretch has there, its rise, and its route seen from
 * above, as the ordered columns it passes from its first anchor to its last, and whether its first
 * anchor is the stretch's start and its last the stretch's end, rather than intermediate Anchors.
 *
 * <p>The leg rises or falls by {@code rise} right after its first anchor, along its first
 * direction, and then runs level to {@link #to its last anchor}. Groundworks owns that shape; what
 * a rise is built of, slopes or a straight climb, is the builder's.
 */
public record Leg(BlockPos from, int rise, List<Column> route, boolean startsStretch, boolean endsStretch) {

    /** A column the leg passes, seen from above, and the way the leg travels through it. */
    public record Column(int x, int z, Direction travel) {
    }

    public Leg {
        from = from.immutable();
        route = List.copyOf(route);
    }

    /** The only leg of a stretch, which both starts and ends it. */
    public Leg(BlockPos from, int rise, List<Column> route) {
        this(from, rise, route, true, true);
    }

    /** This leg taking {@code route} instead, between the same anchors of the same stretch. */
    Leg withRoute(List<Column> route) {
        return new Leg(from, rise, route, startsStretch, endsStretch);
    }

    /**
     * The legs of a stretch from {@code start}, one per route with the rise at the same index, each
     * leg's first anchor at the height the leg before it runs level at. The last ends the stretch
     * only when {@code reachesEnd}, not when the legs are drawn short of an end they can't reach.
     */
    static List<Leg> ofStretch(BlockPos start, List<List<Column>> routes, List<Integer> rises, boolean reachesEnd) {
        List<Leg> legs = new ArrayList<>();
        int y = start.getY();
        for (int i = 0; i < routes.size(); i++) {
            Column first = routes.get(i).getFirst();
            Leg leg = new Leg(new BlockPos(first.x(), y, first.z()), rises.get(i), routes.get(i), i == 0,
                    reachesEnd && i == routes.size() - 1);
            legs.add(leg);
            y = leg.to().getY();
        }
        return legs;
    }

    /** The leg's last anchor, at the height the leg runs level at after its rise. */
    public BlockPos to() {
        Column last = route.getLast();
        return new BlockPos(last.x(), from.getY() + rise, last.z());
    }
}
