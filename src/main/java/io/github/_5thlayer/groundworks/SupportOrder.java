// SPDX-FileCopyrightText: 2026 5thlayer
// SPDX-License-Identifier: MIT

package io.github._5thlayer.groundworks;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;

/**
 * The order a {@linkplain Blueprints paste}'s blocks are laid in, support first, and the blocks
 * that can't stand where they land at all.
 *
 * <p>Laid in rounds: first every block that stands on the world as it is, in the Blueprint's own
 * order; then every block that stands on the world and what the rounds before laid; and so on,
 * until a round lays nothing. A torch is so laid after its wall, a rail after its floor, a lantern
 * after the ceiling it hangs from, wherever each comes in the Blueprint. What is left once a round
 * lays nothing can't stand, as a torch whose wall stayed outside the box: left out of a Copy's
 * plan, and dropped as an item by a Cut's.
 *
 * @param laid the positions laid, in their order
 * @param cantStand the positions that can't stand, in the Blueprint's order
 */
record SupportOrder(List<BlockPos> laid, List<BlockPos> cantStand) {

    SupportOrder {
        laid = List.copyOf(laid);
        cantStand = List.copyOf(cantStand);
    }

    /** What stands, once a round's blocks are laid. */
    @FunctionalInterface
    interface Stands {
        /** Which positions stand on the world and on {@code laid}, the positions the rounds before laid. */
        Predicate<BlockPos> on(Set<BlockPos> laid);
    }

    /** The support-first order of these positions, given in the Blueprint's order. */
    static SupportOrder of(List<BlockPos> positions, Stands stands) {
        List<BlockPos> laid = new ArrayList<>(positions.size());
        Set<BlockPos> laidSet = new HashSet<>();
        List<BlockPos> remaining = positions;
        while (!remaining.isEmpty()) {
            Predicate<BlockPos> standing = stands.on(Collections.unmodifiableSet(laidSet));
            List<BlockPos> round = new ArrayList<>();
            List<BlockPos> still = new ArrayList<>();
            for (BlockPos pos : remaining) {
                (standing.test(pos) ? round : still).add(pos);
            }
            if (round.isEmpty()) {
                break;
            }
            laid.addAll(round);
            laidSet.addAll(round);
            remaining = still;
        }
        return new SupportOrder(laid, remaining);
    }

    /**
     * Blocks in their support-first order, and those that can't stand where they land.
     *
     * @param laid the blocks laid, in their order
     * @param cantStand the blocks that can't stand, in the order given
     */
    record Standing(List<PlacementPlan.Placed> laid, List<PlacementPlan.Placed> cantStand) {

        Standing {
            laid = List.copyOf(laid);
            cantStand = List.copyOf(cantStand);
        }
    }

    /**
     * These blocks, given in the Blueprint's order, ordered support first in the world: each asked
     * whether it survives, by vanilla's own rule, on the world as the rounds before leave it, their
     * {@linkplain Placements#companionsOf Companions} included. The result for each block is
     * whether it can stand there, which a Copy's paste leaves out and a Cut's drops.
     *
     * @param absent the entity left out of the planned level, the placing player, or none
     */
    static Standing inWorld(Level level, @Nullable Entity absent, List<PlacementPlan.Placed> blocks) {
        Map<BlockPos, BlockState> states = new LinkedHashMap<>();
        blocks.forEach(placed -> states.put(placed.pos(), placed.state()));
        SupportOrder order = of(List.copyOf(states.keySet()), laid -> {
            Map<BlockPos, BlockState> planned = new HashMap<>();
            List<PlacementPlan.Placed> laidBlocks = new ArrayList<>(laid.size());
            for (BlockPos pos : laid) {
                planned.put(pos, states.get(pos));
                laidBlocks.add(new PlacementPlan.Placed(pos, states.get(pos)));
            }
            Placements.companionsOf(laidBlocks).forEach(companion ->
                    planned.putIfAbsent(companion.pos(), companion.state()));
            PlannedLevel plannedLevel = new PlannedLevel(level, planned, absent);
            return pos -> states.get(pos).canSurvive(plannedLevel, pos);
        });
        return new Standing(placedAt(order.laid(), states), placedAt(order.cantStand(), states));
    }

    private static List<PlacementPlan.Placed> placedAt(List<BlockPos> positions, Map<BlockPos, BlockState> states) {
        return positions.stream().map(pos -> new PlacementPlan.Placed(pos, states.get(pos))).toList();
    }
}
