// SPDX-FileCopyrightText: 2026 5thlayer
// SPDX-License-Identifier: MIT

package io.github._5thlayer.groundworks;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;

/**
 * An Assembly's plan on a ground the test lays out: why each position is taken, the one refusal
 * the plan makes, and the double plants its clearing takes whole. Its states are null here, since
 * the planning reads none.
 */
class AssemblyPlanningTest {

    private static final Identifier STONE = Identifier.withDefaultNamespace("stone");
    private static final Identifier KEY = Identifier.withDefaultNamespace("carved_pumpkin");
    private static final AssemblyShape.Legend.Block STONE_LEGEND = new AssemblyShape.Legend.Block(STONE);

    private static final BlockPos A = new BlockPos(0, 64, 0);
    private static final BlockPos B = new BlockPos(1, 64, 0);
    private static final BlockPos C = new BlockPos(2, 64, 0);
    private static final BlockPos KEY_POS = new BlockPos(0, 64, 1);

    /** A ground of air everywhere, inside the world and clear of entities, but for what a test sets. */
    private static final class FakeGround implements AssemblyPlanning.Ground {
        final Set<Identifier> unknown = new HashSet<>();
        final Set<BlockPos> holding = new HashSet<>();
        final Set<BlockPos> solid = new HashSet<>();
        final Set<BlockPos> grass = new HashSet<>();
        final Map<BlockPos, BlockPos> otherHalves = new HashMap<>();
        final Set<BlockPos> outside = new HashSet<>();
        final Set<BlockPos> entities = new HashSet<>();
        AssemblyPlanning.KeyPlacing key = new AssemblyPlanning.KeyPlacing(null, null);
        final List<BlockPos> keyAskedOnCleared = new ArrayList<>();

        @Override
        public boolean knows(Identifier block) {
            return !unknown.contains(block);
        }

        @Override
        public boolean holds(BlockPos pos, AssemblyShape.Legend.Laid legend) {
            return holding.contains(pos);
        }

        @Override
        public boolean replaceable(BlockPos pos) {
            return !solid.contains(pos);
        }

        @Override
        public boolean clearChanges(BlockPos pos) {
            return grass.contains(pos);
        }

        @Override
        public @Nullable BlockPos otherHalf(BlockPos pos) {
            return otherHalves.get(pos);
        }

        @Override
        public boolean inWorld(BlockPos pos) {
            return !outside.contains(pos);
        }

        @Override
        public BlockState state(Identifier block, BlockPos pos) {
            return null;
        }

        @Override
        public boolean entityInTheWay(BlockPos pos, BlockState state) {
            return entities.contains(pos);
        }

        @Override
        public AssemblyPlanning.KeyPlacing key(BlockPos keyPos, List<BlockPos> cleared,
                List<PlacementPlan.Placed> others) {
            keyAskedOnCleared.addAll(cleared);
            return key;
        }
    }

    private static AssemblyShape.At at(BlockPos pos, AssemblyShape.Legend legend) {
        return new AssemblyShape.At(pos, legend);
    }

    private static List<AssemblyShape.At> shape(AssemblyShape.At... others) {
        List<AssemblyShape.At> positions = new ArrayList<>(List.of(others));
        positions.add(at(KEY_POS, new AssemblyShape.Legend.Block(KEY)));
        return positions;
    }

    private static AssemblyPlanning.Planned plan(List<AssemblyShape.At> positions, FakeGround ground) {
        AssemblyPlanning.Planned planned = AssemblyPlanning.plan(positions, KEY, ground, Map.of(), tag -> List.of(),
                true);
        assertNotNull(planned);
        return planned;
    }

    @Test
    void aShapeOnClearGroundGoesThroughWithTheKeyLast() {
        PlacementPlan plan = plan(shape(at(A, STONE_LEGEND), at(B, STONE_LEGEND)), new FakeGround()).plan();
        assertNull(plan.refusal());
        assertEquals(List.of(A, B, KEY_POS), plan.blocks().stream().map(PlacementPlan.Placed::pos).toList());
    }

    @Test
    void eachTakenPositionIsTakenForItsOwnReason() {
        FakeGround ground = new FakeGround();
        ground.solid.add(A);
        ground.outside.add(B);
        ground.entities.add(C);
        ground.key = new AssemblyPlanning.KeyPlacing(null, Refusal.Assembly.TAKEN);
        AssemblyPlanning.Planned planned = plan(shape(at(A, STONE_LEGEND), at(B, STONE_LEGEND), at(C, STONE_LEGEND)),
                ground);
        assertEquals(Map.of(A, AssemblyPlanning.Taken.NOT_REPLACEABLE, B, AssemblyPlanning.Taken.OUTSIDE_THE_WORLD,
                C, AssemblyPlanning.Taken.ENTITY_IN_THE_WAY, KEY_POS, AssemblyPlanning.Taken.KEY_REFUSED),
                planned.taken());
    }

    @Test
    void theRefusalIsAtTheFirstTakenPositionInThePlansOrderWhateverTookIt() {
        FakeGround ground = new FakeGround();
        // An entity is found after the solid block, but stands earlier in the shape.
        ground.entities.add(A);
        ground.solid.add(B);
        PlacementPlan plan = plan(shape(at(A, STONE_LEGEND), at(B, AssemblyShape.Legend.Empty.INSTANCE)), ground)
                .plan();
        assertEquals(new Refusal.At(Refusal.Assembly.TAKEN, A), plan.refusal());
    }

    @Test
    void aKeyVanillaRefusesIsTakenAtTheKey() {
        FakeGround ground = new FakeGround();
        ground.key = new AssemblyPlanning.KeyPlacing(null, Refusal.Assembly.TAKEN);
        assertEquals(new Refusal.At(Refusal.Assembly.TAKEN, KEY_POS),
                plan(shape(at(A, STONE_LEGEND)), ground).plan().refusal());
    }

    @Test
    void aTakenPositionComesBeforeAKeyThatWouldNotStand() {
        FakeGround ground = new FakeGround();
        ground.solid.add(B);
        ground.key = new AssemblyPlanning.KeyPlacing(null, Refusal.Assembly.KEY_WOULD_NOT_STAND);
        assertEquals(new Refusal.At(Refusal.Assembly.TAKEN, B),
                plan(shape(at(A, STONE_LEGEND), at(B, STONE_LEGEND)), ground).plan().refusal());
    }

    @Test
    void aKeyThatWouldNotStandIsRefusedAtTheKey() {
        FakeGround ground = new FakeGround();
        ground.key = new AssemblyPlanning.KeyPlacing(null, Refusal.Assembly.KEY_WOULD_NOT_STAND);
        AssemblyPlanning.Planned planned = plan(shape(at(A, STONE_LEGEND)), ground);
        assertEquals(new Refusal.At(Refusal.Assembly.KEY_WOULD_NOT_STAND, KEY_POS), planned.plan().refusal());
        assertEquals(Map.of(), planned.taken());
    }

    @Test
    void aBlockTheShapeTakesStaysUnplacedAndIsNeverTaken() {
        FakeGround ground = new FakeGround();
        ground.holding.add(A);
        ground.solid.add(A);
        PlacementPlan plan = plan(shape(at(A, STONE_LEGEND)), ground).plan();
        assertNull(plan.refusal());
        assertEquals(List.of(KEY_POS), plan.blocks().stream().map(PlacementPlan.Placed::pos).toList());
    }

    @Test
    void anEmptyPositionIsClearedOnlyWhereClearingChangesIt() {
        FakeGround ground = new FakeGround();
        ground.grass.add(A);
        PlacementPlan plan = plan(shape(at(A, AssemblyShape.Legend.Empty.INSTANCE),
                at(B, AssemblyShape.Legend.Empty.INSTANCE)), ground).plan();
        assertEquals(List.of(A), plan.clears());
    }

    @Test
    void aClearedDoublePlantTakesItsOtherHalfWithIt() {
        FakeGround ground = new FakeGround();
        ground.grass.add(A);
        ground.otherHalves.put(A, A.above());
        PlacementPlan plan = plan(shape(at(A, AssemblyShape.Legend.Empty.INSTANCE)), ground).plan();
        assertEquals(List.of(A, A.above()), plan.clears());
        // The Key is asked of the world with both halves gone.
        assertEquals(List.of(A, A.above()), ground.keyAskedOnCleared);
    }

    @Test
    void aDoublePlantsOtherHalfAtOneOfTheShapesPositionsIsLeftToTheShape() {
        FakeGround ground = new FakeGround();
        ground.grass.add(A);
        ground.otherHalves.put(A, B);
        PlacementPlan plan = plan(shape(at(A, AssemblyShape.Legend.Empty.INSTANCE), at(B, STONE_LEGEND)), ground)
                .plan();
        assertEquals(List.of(A), plan.clears());
    }

    @Test
    void aBlockThisSideLacksLeavesNothingToDraw() {
        FakeGround ground = new FakeGround();
        ground.unknown.add(STONE);
        assertNull(AssemblyPlanning.plan(shape(at(A, STONE_LEGEND)), KEY, ground, Map.of(), tag -> List.of(), true));
    }

    @Test
    void whatTheInventoryLacksIsMissing() {
        AssemblyPlanning.Planned planned = AssemblyPlanning.plan(shape(at(A, STONE_LEGEND), at(B, STONE_LEGEND)), KEY,
                new FakeGround(), Map.of(STONE, 1, KEY, 1), tag -> List.of(), false);
        assertNotNull(planned);
        assertEquals(Map.of(STONE, 1), planned.missing());
    }
}
