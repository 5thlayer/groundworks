// SPDX-FileCopyrightText: 2026 5thlayer
// SPDX-License-Identifier: MIT

package io.github._5thlayer.groundworks;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Set;
import java.util.function.Function;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import org.jspecify.annotations.Nullable;

/**
 * Detours: a {@link Leg} that meets an obstacle at its own height goes round it flat, rather than
 * refusing the stretch (ADR 0004). An obstacle is where the item refuses the leg at a position; a
 * refusal that stands nowhere refuses the stretch whole and is never detoured.
 *
 * <p>The search reads no world. It looks for the shortest route seen from above between the leg's
 * two anchors, round every obstacle met so far, and asks the item for it, so an obstacle is only ever
 * what the item refuses; each new obstacle is added and the search runs again. A route stays within
 * {@link #BAND} blocks sideways of the leg's own, never behind its first anchor or past its last. It
 * leaves its first anchor and arrives at its last the way the leg does, so the rise sits where it
 * did, along the leg's first direction, and the next leg heads the same way whatever the detour.
 *
 * <p>Of the shortest routes, the one on the side of the leg the player stands on is taken, or right
 * of travel when they stand on its line, and of those the one that leaves the leg's line least, so a
 * detour hugs its obstacle. A leg that no route clears is refused with the item's refusal at the
 * first obstacle, and so is one whose route is refused not at a position, since the search can't
 * tell what to go round. The leg's first anchor and rise are kept: a detour changes only the route
 * seen from above.
 */
final class Detours {

    /** How many blocks a detour may stray sideways from its leg's own route. */
    static final int BAND = 3;

    private Detours() {
    }

    /**
     * The plan of {@code leg}, or of the detour that clears it, as {@code build} answers for a leg,
     * with the player {@code standing} there, or on the leg's line when {@code null}.
     */
    static PlacementPlan plan(Leg leg, @Nullable BlockPos standing, Function<Leg, PlacementPlan> build) {
        PlacementPlan straight = build.apply(leg);
        if (!(straight.refusal() instanceof Refusal.At first)) {
            return straight;
        }
        boolean playerRight = standsRight(leg.route(), first.pos(), standing);
        Set<Long> obstacles = new HashSet<>();
        Refusal.At at = first;
        while (obstacles.add(key(at.pos().getX(), at.pos().getZ()))) {
            List<Leg.Column> route = shortest(leg.route(), obstacles, playerRight);
            if (route == null) {
                break;
            }
            PlacementPlan detour = build.apply(new Leg(leg.from(), leg.rise(), route));
            if (!detour.isRefused()) {
                return detour;
            }
            if (!(detour.refusal() instanceof Refusal.At next)) {
                break;
            }
            at = next;
        }
        return straight;
    }

    /**
     * Whether the player stands right of the leg's travel where it meets its first obstacle, or on
     * its line there; a player nowhere stands on it.
     */
    private static boolean standsRight(List<Leg.Column> route, BlockPos obstacle, @Nullable BlockPos standing) {
        if (standing == null) {
            return true;
        }
        for (Leg.Column column : route) {
            if (column.x() == obstacle.getX() && column.z() == obstacle.getZ()) {
                Leg.Column rightward = new Leg.Column(column.x(), column.z(), column.travel().getClockWise());
                return along(rightward, standing.getX(), standing.getZ()) >= 0;
            }
        }
        return true;
    }

    /** A search's cost so far: blocks passed, then those on the side not taken, then those off the leg's line. */
    private record Cost(int length, int wrongSide, int offLine) implements Comparable<Cost> {

        Cost plus(int side, boolean playerRight) {
            boolean wrong = playerRight ? side < 0 : side > 0;
            return new Cost(length + 1, wrongSide + (wrong ? 1 : 0), offLine + (side == 0 ? 0 : 1));
        }

        @Override
        public int compareTo(Cost other) {
            int byLength = Integer.compare(length, other.length);
            if (byLength != 0) {
                return byLength;
            }
            int bySide = Integer.compare(wrongSide, other.wrongSide);
            return bySide != 0 ? bySide : Integer.compare(offLine, other.offLine);
        }
    }

    private record Reached(long column, Cost cost, int queued) {
    }

    /**
     * The shortest route from the leg's first column to its last round the obstacles, leaving
     * through the leg's own second column and arriving through its last-but-one, with each column's
     * travel the way it leaves, the last the way it arrives; or {@code null} when none does within
     * the band.
     */
    private static @Nullable List<Leg.Column> shortest(List<Leg.Column> route, Set<Long> obstacles, boolean playerRight) {
        if (route.size() < 3) {
            return null;
        }
        Leg.Column first = route.getFirst();
        Leg.Column last = route.getLast();
        long start = key(route.get(1));
        long goal = key(route.get(route.size() - 2));
        if (obstacles.contains(key(first)) || obstacles.contains(key(last))) {
            return null;
        }
        Map<Long, Integer> band = band(route);
        band.remove(key(first));
        band.remove(key(last));
        band.keySet().removeAll(obstacles);
        if (!band.containsKey(start) || !band.containsKey(goal)) {
            return null;
        }

        Map<Long, Cost> best = new HashMap<>();
        Map<Long, Long> cameFrom = new HashMap<>();
        PriorityQueue<Reached> queue = new PriorityQueue<>((a, b) -> {
            int byCost = a.cost().compareTo(b.cost());
            return byCost != 0 ? byCost : Integer.compare(a.queued(), b.queued());
        });
        int queued = 0;
        Cost origin = new Cost(2, 0, 0);
        best.put(start, origin);
        queue.add(new Reached(start, origin, queued++));
        while (!queue.isEmpty()) {
            Reached reached = queue.poll();
            if (reached.cost().compareTo(best.get(reached.column())) > 0) {
                continue;
            }
            if (reached.column() == goal) {
                break;
            }
            int x = x(reached.column());
            int z = z(reached.column());
            for (Direction step : Direction.Plane.HORIZONTAL) {
                long next = key(x + step.getStepX(), z + step.getStepZ());
                Integer side = band.get(next);
                if (side == null) {
                    continue;
                }
                Cost cost = reached.cost().plus(side, playerRight);
                Cost known = best.get(next);
                if (known == null || cost.compareTo(known) < 0) {
                    best.put(next, cost);
                    cameFrom.put(next, reached.column());
                    queue.add(new Reached(next, cost, queued++));
                }
            }
        }
        if (!best.containsKey(goal)) {
            return null;
        }

        List<Long> passed = new ArrayList<>();
        for (Long at = goal; at != null; at = cameFrom.get(at)) {
            passed.add(at);
        }
        Collections.reverse(passed);
        List<Leg.Column> columns = new ArrayList<>(List.of(first));
        for (int i = 0; i < passed.size(); i++) {
            long at = passed.get(i);
            long next = i + 1 < passed.size() ? passed.get(i + 1) : key(last);
            Direction travel = Direction.getNearest(x(next) - x(at), 0, z(next) - z(at), last.travel());
            columns.add(new Leg.Column(x(at), z(at), travel));
        }
        columns.add(last);
        return columns;
    }

    /**
     * The columns a detour may pass: each of the route's, and those up to {@link #BAND} blocks
     * sideways of one that lie neither behind the route's first column nor past its last, each with
     * the side of the route it lies on: 1 right of travel, -1 left, 0 on the route itself.
     */
    private static Map<Long, Integer> band(List<Leg.Column> route) {
        Leg.Column first = route.getFirst();
        Leg.Column last = route.getLast();
        Map<Long, Integer> band = new HashMap<>();
        for (Leg.Column column : route) {
            band.put(key(column), 0);
        }
        for (Leg.Column column : route) {
            Direction right = column.travel().getClockWise();
            for (int aside = -BAND; aside <= BAND; aside++) {
                int x = column.x() + right.getStepX() * aside;
                int z = column.z() + right.getStepZ() * aside;
                if (along(first, x, z) >= 0 && along(last, x, z) <= 0) {
                    band.putIfAbsent(key(x, z), Integer.signum(aside));
                }
            }
        }
        return band;
    }

    /** How far ahead of {@code column} a column lies, along its travel. */
    private static int along(Leg.Column column, int x, int z) {
        return (x - column.x()) * column.travel().getStepX() + (z - column.z()) * column.travel().getStepZ();
    }

    private static long key(Leg.Column column) {
        return key(column.x(), column.z());
    }

    private static long key(int x, int z) {
        return ((long) x << 32) | (z & 0xFFFFFFFFL);
    }

    private static int x(long key) {
        return (int) (key >> 32);
    }

    private static int z(long key) {
        return (int) key;
    }
}
