// SPDX-FileCopyrightText: 2026 5thlayer
// SPDX-License-Identifier: MIT

package io.github._5thlayer.groundworks;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import io.github._5thlayer.groundworks.Leg.Column;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;

/**
 * The search for a Detour round a leg's obstacles, asked of an item that refuses a leg at the
 * first column it passes that is blocked, at the leg's height, and records every route it is asked.
 * Legs here run east from the origin, so right of travel is south, +z.
 */
class DetoursTest {

    private static final BlockPos FROM = new BlockPos(0, 64, 0);

    private enum ItemRefusal implements Refusal {
        BLOCKED,
        TOO_STEEP
    }

    /** An item whose obstacles are {@code blocked} columns, or that refuses every leg not at a position. */
    private static final class Item {

        private final Set<List<Integer>> blocked;
        private final @Nullable Refusal refusesAll;
        final List<List<Column>> asked = new ArrayList<>();

        Item(Set<List<Integer>> blocked, @Nullable Refusal refusesAll) {
            this.blocked = blocked;
            this.refusesAll = refusesAll;
        }

        @SafeVarargs
        Item(List<Integer>... blocked) {
            this(Set.of(blocked), null);
        }

        PlacementPlan build(Leg leg) {
            asked.add(leg.route());
            if (refusesAll != null) {
                return PlacementPlan.refused(List.of(), refusesAll);
            }
            for (Column column : leg.route()) {
                if (blocked.contains(List.of(column.x(), column.z()))) {
                    return PlacementPlan.refused(List.of(),
                            new Refusal.At(ItemRefusal.BLOCKED, new BlockPos(column.x(), leg.to().getY(), column.z())));
                }
            }
            return PlacementPlan.accepted(List.of());
        }
    }

    private static Leg east(int to) {
        List<Column> route = new ArrayList<>();
        for (int x = 0; x <= to; x++) {
            route.add(new Column(x, 0, Direction.EAST));
        }
        return new Leg(FROM, 0, route);
    }

    private static List<Integer> at(int x, int z) {
        return List.of(x, z);
    }

    /** The columns of a route, each with the way it leaves toward the next, the last the way it arrives. */
    private static List<Column> route(int... xz) {
        List<Column> route = new ArrayList<>();
        for (int i = 0; i < xz.length; i += 2) {
            int next = i + 2 < xz.length ? i + 2 : i;
            int prev = i + 2 < xz.length ? i : i - 2;
            Direction travel = Direction.getNearest(xz[next] - xz[prev], 0, xz[next + 1] - xz[prev + 1], Direction.UP);
            route.add(new Column(xz[i], xz[i + 1], travel));
        }
        return route;
    }

    private static PlacementPlan plan(Item item, Leg leg, BlockPos standing) {
        return Detours.plan(leg, standing, item::build);
    }

    @Test
    void aLegWithNoObstacleIsItsOwnRoute() {
        Item item = new Item();
        PlacementPlan plan = plan(item, east(4), new BlockPos(0, 64, -2));
        assertNull(plan.refusal());
        assertEquals(List.of(east(4).route()), item.asked);
    }

    @Test
    void aSingleObstacleIsDetouredHuggingItOnTheSideThePlayerStandsOn() {
        Item item = new Item(at(3, 0));
        PlacementPlan plan = plan(item, east(6), new BlockPos(0, 64, -2));
        assertNull(plan.refusal());
        // North of an east leg is left of travel.
        assertEquals(route(0, 0, 1, 0, 2, 0, 2, -1, 3, -1, 4, -1, 4, 0, 5, 0, 6, 0), item.asked.getLast());
    }

    @Test
    void thePlayerOnTheOtherSideTakesTheOtherSide() {
        Item item = new Item(at(3, 0));
        plan(item, east(6), new BlockPos(5, 64, 2));
        assertEquals(route(0, 0, 1, 0, 2, 0, 2, 1, 3, 1, 4, 1, 4, 0, 5, 0, 6, 0), item.asked.getLast());
    }

    @Test
    void aTieWithThePlayerOnTheLegsLineGoesRightOfTravel() {
        Item item = new Item(at(3, 0));
        plan(item, east(6), new BlockPos(-3, 70, 0));
        assertEquals(route(0, 0, 1, 0, 2, 0, 2, 1, 3, 1, 4, 1, 4, 0, 5, 0, 6, 0), item.asked.getLast());
    }

    @Test
    void theShorterSideIsTakenOverThePlayersSide() {
        // The player stands north, but north is walled two blocks deep and south only one.
        Item item = new Item(at(3, 0), at(3, -1), at(3, -2), at(3, 1));
        plan(item, east(6), new BlockPos(0, 64, -3));
        assertEquals(route(0, 0, 1, 0, 2, 0, 2, 1, 2, 2, 3, 2, 4, 2, 4, 1, 4, 0, 5, 0, 6, 0), item.asked.getLast());
    }

    @Test
    void anObstacleMetOnADetourIsDetouredToo() {
        // South hugs the first obstacle into the second, and north is then the shorter way round.
        Item item = new Item(at(3, 0), at(4, 1));
        PlacementPlan plan = plan(item, east(7), new BlockPos(0, 64, 3));
        assertNull(plan.refusal());
        assertEquals(route(0, 0, 1, 0, 2, 0, 2, 1, 3, 1, 4, 1, 4, 0, 5, 0, 6, 0, 7, 0), item.asked.get(1));
        assertEquals(route(0, 0, 1, 0, 2, 0, 2, -1, 3, -1, 4, -1, 4, 0, 5, 0, 6, 0, 7, 0), item.asked.getLast());
    }

    @Test
    void aWallWiderThanTheBandRefusesWithTheFirstObstacle() {
        Item item = new Item(Set.of(at(3, -3), at(3, -2), at(3, -1), at(3, 0), at(3, 1), at(3, 2), at(3, 3)), null);
        Leg leg = east(6);
        PlacementPlan plan = plan(item, leg, new BlockPos(0, 64, 2));
        assertEquals(new Refusal.At(ItemRefusal.BLOCKED, new BlockPos(3, 64, 0)), plan.refusal());
        assertEquals(leg.route(), item.asked.getFirst());
    }

    @Test
    void aWallOnlyAsWideAsTheBandIsGoneRoundJustPastIt() {
        Item item = new Item(Set.of(at(3, -2), at(3, -1), at(3, 0), at(3, 1), at(3, 2), at(3, 3)), null);
        PlacementPlan plan = plan(item, east(6), new BlockPos(0, 64, 2));
        assertNull(plan.refusal());
        assertEquals(3, Detours.BAND);
        assertEquals(route(0, 0, 1, 0, 2, 0, 2, -1, 2, -2, 2, -3, 3, -3, 4, -3, 4, -2, 4, -1, 4, 0, 5, 0, 6, 0),
                item.asked.getLast());
    }

    @Test
    void aRefusalNotAtAPositionIsNeverDetoured() {
        Item item = new Item(Set.of(), ItemRefusal.TOO_STEEP);
        PlacementPlan plan = plan(item, east(6), new BlockPos(0, 64, 2));
        assertSame(ItemRefusal.TOO_STEEP, plan.refusal());
        assertEquals(1, item.asked.size());
    }

    @Test
    void anObstacleAtAnAnchorIsNeverDetoured() {
        Item item = new Item(at(6, 0));
        PlacementPlan plan = plan(item, east(6), new BlockPos(0, 64, 2));
        assertEquals(new Refusal.At(ItemRefusal.BLOCKED, new BlockPos(6, 64, 0)), plan.refusal());
        assertEquals(1, item.asked.size());
    }

    @Test
    void aDetourArrivesAtItsLastAnchorTheWayTheLegDoes() {
        // Just before the end, where going round would arrive from the side.
        Item item = new Item(at(5, 0));
        PlacementPlan plan = plan(item, east(6), new BlockPos(0, 64, 2));
        assertEquals(new Refusal.At(ItemRefusal.BLOCKED, new BlockPos(5, 64, 0)), plan.refusal());
    }

    @Test
    void aDetourLeavesItsFirstAnchorTheWayTheLegDoes() {
        // Just after the start, where going round would leave it, and so its rise, sideways.
        Item item = new Item(at(1, 0));
        PlacementPlan plan = plan(item, east(6), new BlockPos(0, 64, 2));
        assertEquals(new Refusal.At(ItemRefusal.BLOCKED, new BlockPos(1, 64, 0)), plan.refusal());
        assertEquals(1, item.asked.size());
    }

    @Test
    void aDetourNeverGoesBehindItsFirstAnchor() {
        // East two, then south four, walled across the second line but for a gap behind the start.
        Leg leg = new Leg(FROM, 0, route(0, 0, 1, 0, 2, 0, 2, 1, 2, 2, 2, 3, 2, 4));
        Item item = new Item(Set.of(at(0, 2), at(1, 2), at(2, 2), at(3, 2), at(4, 2), at(5, 2)), null);
        PlacementPlan plan = plan(item, leg, new BlockPos(0, 64, 2));
        assertEquals(new Refusal.At(ItemRefusal.BLOCKED, new BlockPos(2, 64, 2)), plan.refusal());
    }

    @Test
    void aDetourKeepsTheLegsFirstAnchorAndRise() {
        Item item = new Item(at(2, 0));
        List<Leg> legs = new ArrayList<>();
        Detours.plan(new Leg(FROM, 2, east(4).route()), new BlockPos(0, 64, 2), leg -> {
            legs.add(leg);
            return item.build(leg);
        });
        assertEquals(FROM, legs.getLast().from());
        assertEquals(2, legs.getLast().rise());
    }

    @Test
    void aDetourKeepsWhetherTheLegStartsOrEndsItsStretch() {
        Item item = new Item(at(2, 0));
        for (boolean starts : new boolean[] {true, false}) {
            for (boolean ends : new boolean[] {true, false}) {
                List<Leg> legs = new ArrayList<>();
                Detours.plan(new Leg(FROM, 0, east(4).route(), starts, ends), new BlockPos(0, 64, 2), leg -> {
                    legs.add(leg);
                    return item.build(leg);
                });
                assertEquals(2, legs.size());
                assertEquals(List.of(starts, ends), List.of(legs.getLast().startsStretch(), legs.getLast().endsStretch()));
            }
        }
    }

    @Test
    void anLsTurnIsCutInsideWhenThePlayerStandsThere() {
        // East two, then south two; its turn (2, 0) is blocked, and the player stands inside it.
        Leg leg = new Leg(FROM, 0, route(0, 0, 1, 0, 2, 0, 2, 1, 2, 2));
        Item item = new Item(at(2, 0));
        PlacementPlan plan = plan(item, leg, new BlockPos(0, 64, 2));
        assertNull(plan.refusal());
        assertEquals(route(0, 0, 1, 0, 1, 1, 2, 1, 2, 2), item.asked.getLast());
    }
}
