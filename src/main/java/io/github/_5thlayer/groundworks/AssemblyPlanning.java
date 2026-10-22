// SPDX-FileCopyrightText: 2026 5thlayer
// SPDX-License-Identifier: MIT

package io.github._5thlayer.groundworks;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;

/**
 * An {@linkplain Assemblies Assembly}'s plan at its placed positions, from what the {@link Ground}
 * says of them: which positions want a block and which are cleared, the {@linkplain
 * AssemblyShape#fill fill}, why each taken position is taken, and the one refusal the plan makes
 * for itself. It reads no world of its own, so the world it plans on is whatever the ground says.
 */
final class AssemblyPlanning {

    private AssemblyPlanning() {
    }

    /** What the planning asks of the world, as the click finds it, at the shape's positions. */
    interface Ground {

        /** Whether this side has the block of this id: a client may be sent a declaration naming a block it lacks. */
        boolean knows(Identifier block);

        /** Whether what stands here is a block this legend takes, which stays and is not charged. */
        boolean holds(BlockPos pos, AssemblyShape.Legend.Laid legend);

        /** Whether a placement replaces what stands here, as it does air, water or grass. */
        boolean replaceable(BlockPos pos);

        /** Whether clearing this position changes it: it holds more than nothing or a fluid. */
        boolean clearChanges(BlockPos pos);

        /**
         * The other half of the double plant standing here, tall grass's, where it stands and
         * clearing it changes it, or {@code null}.
         */
        @Nullable BlockPos otherHalf(BlockPos pos);

        /** Whether this position is inside the world. */
        boolean inWorld(BlockPos pos);

        /** The state this block, one this side {@linkplain #knows knows}, goes down in here, turned with the Key. */
        BlockState state(Identifier block, BlockPos pos);

        /** Whether an entity, the placing player's own body aside, is in the way of this state here. */
        boolean entityInTheWay(BlockPos pos, BlockState state);

        /**
         * The Key's placement at {@code keyPos}, asked of the world as clearing {@code cleared} and
         * laying {@code others} leaves it.
         */
        KeyPlacing key(BlockPos keyPos, List<BlockPos> cleared, List<PlacementPlan.Placed> others);
    }

    /**
     * The state the Key goes down in, and why it can't go down, if it can't.
     *
     * @param refusal {@link Refusal.Assembly#TAKEN TAKEN} where the Key has no room, no state for
     *     placement, or an entity in its way, {@link Refusal.Assembly#KEY_WOULD_NOT_STAND
     *     KEY_WOULD_NOT_STAND} where it has a state that wouldn't survive, or none where it goes down
     */
    record KeyPlacing(BlockState state, Refusal.@Nullable Assembly refusal) {
    }

    /** Why a position is {@linkplain Refusal.Assembly#TAKEN taken}. */
    enum Taken {
        /** It holds a block a placement can't replace, and the shape doesn't take. */
        NOT_REPLACEABLE,
        /** It is outside the world. */
        OUTSIDE_THE_WORLD,
        /** An entity other than the placing player is in the way of the block it gets. */
        ENTITY_IN_THE_WAY,
        /** It is the Key's, and vanilla's placement has no room for the Key there. */
        KEY_REFUSED,
    }

    /**
     * An Assembly's plan, and what it was planned from.
     *
     * @param plan the shape's blocks, the Key last, its clears, and its own refusal or none
     * @param taken why each taken position is taken, in the order they were found
     * @param missing what the inventory lacks for the plan, by block id, none when it affords it
     */
    record Planned(PlacementPlan plan, Map<BlockPos, Taken> taken, Map<Identifier, Integer> missing) {
    }

    /**
     * The plan for a shape placed at {@code positions}, the Key's last, or {@code null} where this
     * side lacks a block it names, so there is nothing honest to draw.
     *
     * @param held how many of each block the main inventory holds, by the block's id
     * @param members the blocks of each tag, by the tag's id, in the tag's order
     * @param free whether the player is charged nothing, as in creative
     */
    static @Nullable Planned plan(List<AssemblyShape.At> positions, Identifier keyBlock, Ground ground,
            Map<Identifier, Integer> held, Function<Identifier, List<Identifier>> members, boolean free) {
        // Which positions want a block, which want clearing, and which are taken.
        List<AssemblyShape.At> wanting = new ArrayList<>(positions.size());
        List<AssemblyShape.Legend.Laid> legends = new ArrayList<>(positions.size());
        List<BlockPos> cleared = new ArrayList<>();
        Map<BlockPos, Taken> taken = new LinkedHashMap<>();
        for (AssemblyShape.At at : positions.subList(0, positions.size() - 1)) {
            switch (at.legend()) {
                case AssemblyShape.Legend.Empty empty -> {
                    if (!ground.replaceable(at.pos())) {
                        taken.put(at.pos(), Taken.NOT_REPLACEABLE);
                    } else if (ground.clearChanges(at.pos())) {
                        cleared.add(at.pos());
                    }
                }
                case AssemblyShape.Legend.Laid laid -> {
                    // A declaration a client was sent for a block it lacks: nothing honest to draw.
                    if (laid instanceof AssemblyShape.Legend.Block named && !ground.knows(named.id())) {
                        return null;
                    }
                    // A block the shape takes, standing already, stays and is not charged.
                    if (!ground.holds(at.pos(), laid)) {
                        wanting.add(at);
                        legends.add(laid);
                    }
                }
            }
        }

        clearedWithTheirOtherHalves(ground, cleared, positions);

        legends.add(new AssemblyShape.Legend.Block(keyBlock));
        AssemblyShape.Fill fill = AssemblyShape.fill(legends, held, members, free);

        List<PlacementPlan.Placed> others = new ArrayList<>(wanting.size());
        for (int i = 0; i < wanting.size(); i++) {
            if (!ground.knows(fill.blocks().get(i))) {
                return null;
            }
            BlockPos pos = wanting.get(i).pos();
            BlockState state = ground.state(fill.blocks().get(i), pos);
            others.add(new PlacementPlan.Placed(pos, state));
            Taken why = whyTaken(ground, pos, state);
            if (why != null) {
                taken.put(pos, why);
            }
        }

        BlockPos keyPos = positions.getLast().pos();
        KeyPlacing key = ground.key(keyPos, cleared, others);
        if (key.refusal() == Refusal.Assembly.TAKEN) {
            taken.put(keyPos, Taken.KEY_REFUSED);
        }

        List<PlacementPlan.Placed> blocks = new ArrayList<>(others);
        blocks.add(new PlacementPlan.Placed(keyPos, key.state()));
        PlacementPlan plan = new PlacementPlan(blocks, List.of(), cleared, refusal(positions, taken, key));
        return new Planned(plan, taken, fill.missing());
    }

    /**
     * Why a block of the shape may not go here, or {@code null} where it may: inside the world, on
     * a block a placement replaces, and clear of entities, the placing player's own body aside.
     * What isn't reached isn't asked.
     */
    private static @Nullable Taken whyTaken(Ground ground, BlockPos pos, BlockState state) {
        if (!ground.inWorld(pos)) {
            return Taken.OUTSIDE_THE_WORLD;
        }
        if (!ground.replaceable(pos)) {
            return Taken.NOT_REPLACEABLE;
        }
        if (ground.entityInTheWay(pos, state)) {
            return Taken.ENTITY_IN_THE_WAY;
        }
        return null;
    }

    /**
     * The plan's own refusal: taken at the first taken position in the plan's order, whatever made
     * it taken; else the Key's, where it would not stand; else none.
     */
    static @Nullable Refusal refusal(List<AssemblyShape.At> positions, Map<BlockPos, Taken> taken, KeyPlacing key) {
        Optional<BlockPos> firstTaken = positions.stream().map(AssemblyShape.At::pos).filter(taken::containsKey)
                .findFirst();
        if (firstTaken.isPresent()) {
            return new Refusal.At(Refusal.Assembly.TAKEN, firstTaken.get());
        }
        if (key.refusal() == Refusal.Assembly.KEY_WOULD_NOT_STAND) {
            return new Refusal.At(Refusal.Assembly.KEY_WOULD_NOT_STAND, positions.getLast().pos());
        }
        return null;
    }

    /**
     * Adds to the cleared positions the other half of each double plant cleared, tall grass's, which
     * goes with it, so the preview marks it too; where that half stands at one of the shape's own
     * positions, the shape already says what becomes of it.
     */
    private static void clearedWithTheirOtherHalves(Ground ground, List<BlockPos> cleared,
            List<AssemblyShape.At> positions) {
        Set<BlockPos> shaped = new HashSet<>();
        positions.forEach(at -> shaped.add(at.pos()));
        for (BlockPos pos : List.copyOf(cleared)) {
            BlockPos other = ground.otherHalf(pos);
            if (other != null && !shaped.contains(other)) {
                cleared.add(other);
            }
        }
    }
}
