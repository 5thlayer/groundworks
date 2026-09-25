// SPDX-FileCopyrightText: 2026 5thlayer
// SPDX-License-Identifier: MIT

package io.github._5thlayer.groundworks.gametest;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiFunction;

import io.github._5thlayer.groundworks.Groundworks;
import io.github._5thlayer.groundworks.QuarterTurn;
import io.github._5thlayer.groundworks.Rotate;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.WallTorchBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BedPart;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.level.BlockEvent;

/**
 * Rotate in Place on vanilla blocks: with nothing rotatable held, a press turns the block under the
 * crosshair a quarter, if a statement covers it, by its own contract or else vanilla's turn. Every
 * press goes through {@link Rotate#press} with the aimed block, which is what the key's payload
 * calls.
 */
final class RotateInPlaceTests {

    private static final BlockPos AIMED = new BlockPos(4, 1, 4);

    /** Where a test has put a claim: the place event there is cancelled, as a claim mod cancels it. */
    private static final Set<BlockPos> CLAIMED = ConcurrentHashMap.newKeySet();

    /**
     * A stated block for each way a block orients, and the state a quarter turn clockwise gives
     * from one: a carved pumpkin faces a heading, a log lies along an axis, and a skull on the
     * floor faces one of sixteen ways.
     */
    private static final List<Turning> TURNING = List.of(
            new Turning(Blocks.CARVED_PUMPKIN.defaultBlockState().setValue(BlockStateProperties.HORIZONTAL_FACING, Direction.EAST),
                    (state, quarters) -> state.setValue(BlockStateProperties.HORIZONTAL_FACING,
                            QuarterTurn.of(quarters).turn(state.getValue(BlockStateProperties.HORIZONTAL_FACING)))),
            new Turning(Blocks.OAK_LOG.defaultBlockState().setValue(BlockStateProperties.AXIS, Direction.Axis.X),
                    (state, quarters) -> quarters % 2 == 0 ? state : state.setValue(BlockStateProperties.AXIS, Direction.Axis.Z)),
            new Turning(Blocks.SKELETON_SKULL.defaultBlockState().setValue(BlockStateProperties.ROTATION_16, 1),
                    (state, quarters) -> state.setValue(BlockStateProperties.ROTATION_16, Math.floorMod(1 + 4 * quarters, 16))));

    private RotateInPlaceTests() {
    }

    static void register(GroundworksGameTests.Registrar tests) {
        tests.optIn(Blocks.COBBLESTONE);
        tests.turnsInPlace(Blocks.CARVED_PUMPKIN, Blocks.OAK_LOG, Blocks.SKELETON_SKULL, Blocks.STONE, Blocks.OAK_DOOR,
                Blocks.RED_BED, Blocks.WALL_TORCH, GroundworksGameTests.REFUSES_TO_TURN.get());
        for (Turning turning : TURNING) {
            tests.test("rotate_in_place_turns_" + BuiltInRegistries.BLOCK.getKey(turning.start().getBlock()).getPath()
                    + "_a_quarter_each_press_both_ways", 20, helper -> turnsEachPress(helper, turning));
        }
        // Not opted in, so no Placement Preview is drawn for it and it is not rotatable.
        tests.test("rotate_with_an_undrawn_oriented_block_held_turns_the_aimed_block", 20,
                helper -> succeeds(helper, () -> assertHeldFallsThrough(helper, Blocks.BLAST_FURNACE)));
        tests.test("rotate_with_a_block_with_no_orientation_held_turns_the_aimed_block", 20,
                helper -> succeeds(helper, () -> assertHeldFallsThrough(helper, Blocks.COBBLESTONE)));
        // Oriented, and vanilla would turn it, but no statement covers it.
        tests.test("rotate_leaves_a_block_no_statement_covers_alone", 20, helper -> succeeds(helper,
                () -> assertLeftAlone(helper, Map.of(AIMED, Blocks.JACK_O_LANTERN.defaultBlockState()), AIMED)));
        // A log standing upright turns into itself, and stone has nothing to turn.
        tests.test("rotate_leaves_a_block_whose_turn_changes_nothing_alone_and_says_nothing", 20, helper -> succeeds(helper, () -> {
            assertLeftAlone(helper, Map.of(AIMED, Blocks.OAK_LOG.defaultBlockState()), AIMED);
            assertLeftAlone(helper, Map.of(AIMED, Blocks.STONE.defaultBlockState()), AIMED);
        }));
        tests.test("rotate_on_a_door_half_re_derives_it_against_its_other_half", 20,
                helper -> succeeds(helper, () -> doorReDerives(helper)));
        tests.test("rotate_on_a_bed_half_re_derives_it_against_its_other_half", 20,
                helper -> succeeds(helper, () -> bedReDerives(helper)));
        tests.test("rotate_leaves_a_block_that_would_no_longer_stand_as_it_was", 20,
                helper -> succeeds(helper, () -> wouldNotStand(helper)));
        tests.test("rotate_refuses_a_block_whose_contract_refuses_and_says_why", 20,
                helper -> succeeds(helper, () -> contractRefuses(helper)));
        tests.test("rotate_leaves_a_block_in_a_claim_alone", 20, helper -> succeeds(helper, () -> claimGuards(helper)));
        tests.test("rotate_leaves_a_block_out_of_reach_alone", 20, helper -> succeeds(helper, () -> outOfReach(helper)));
    }

    /** Runs checks that fail by throwing, and succeeds once they all pass: a test succeeds once, at its end. */
    private static void succeeds(GameTestHelper helper, Runnable checks) {
        checks.run();
        helper.succeed();
    }

    /** A claim mod's guard, stood in for: the place event is cancelled at each {@linkplain #CLAIMED claimed} position. */
    static void guardClaims() {
        NeoForge.EVENT_BUS.addListener(BlockEvent.EntityPlaceEvent.class, event -> {
            if (CLAIMED.contains(event.getPos())) {
                event.setCanceled(true);
            }
        });
    }

    /** Each press turns a quarter, clockwise or back, and four bring it back to where it started, keeping its block entity. */
    private static void turnsEachPress(GameTestHelper helper, Turning turning) {
        helper.setBlock(AIMED, turning.start());
        BlockEntity entity = helper.getLevel().getBlockEntity(helper.absolutePos(AIMED));
        ListeningPlayer player = new ListeningPlayer(helper, AIMED.north(2));
        for (boolean reverse : List.of(false, true)) {
            for (int press = 1; press <= 4; press++) {
                Rotate.press(player, helper.absolutePos(AIMED), reverse);
                BlockState expected = turning.turned().apply(turning.start(), reverse ? -press : press);
                if (!helper.getBlockState(AIMED).equals(expected)) {
                    helper.fail((reverse ? "Reverse Rotate" : "Rotate") + " press " + press + " left "
                            + helper.getBlockState(AIMED) + ", expected " + expected, AIMED);
                }
            }
        }
        if (helper.getLevel().getBlockEntity(helper.absolutePos(AIMED)) != entity) {
            helper.fail("turning " + helper.getBlockState(AIMED) + " replaced its block entity", AIMED);
        }
        if (!player.heard.isEmpty()) {
            helper.fail("a turn that went through named a refusal: " + player.heard, AIMED);
        }
        helper.succeed();
    }

    /** Holding what isn't rotatable, a press turns the aimed block and leaves the stack unturned. */
    private static void assertHeldFallsThrough(GameTestHelper helper, Block held) {
        Turning pumpkin = TURNING.getFirst();
        helper.setBlock(AIMED, pumpkin.start());
        ListeningPlayer player = new ListeningPlayer(helper, AIMED.north(2));
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(held, 2));
        Rotate.press(player, helper.absolutePos(AIMED), false);
        if (player.getMainHandItem().has(Groundworks.QUARTER_TURN.get())) {
            helper.fail("a press turned the held " + name(held), AIMED);
        }
        BlockState expected = pumpkin.turned().apply(pumpkin.start(), 1);
        if (!helper.getBlockState(AIMED).equals(expected)) {
            helper.fail("a press with " + name(held) + " held left the aimed block " + helper.getBlockState(AIMED)
                    + ", expected " + expected, AIMED);
        }
    }

    // Vanilla's turn of one half alone would face it across the other; reshaped against it, the half takes its facing back.
    private static void doorReDerives(GameTestHelper helper) {
        BlockState lower = Blocks.OAK_DOOR.defaultBlockState()
                .setValue(DoorBlock.FACING, Direction.EAST).setValue(DoorBlock.HALF, DoubleBlockHalf.LOWER);
        assertLeftAlone(helper, Map.of(AIMED, lower, AIMED.above(), lower.setValue(DoorBlock.HALF, DoubleBlockHalf.UPPER)),
                AIMED, AIMED.above());
    }

    // A bed half turned alone points away from its other half, so reshaped it is no bed: it would no longer stand.
    private static void bedReDerives(GameTestHelper helper) {
        BlockState foot = Blocks.RED_BED.defaultBlockState()
                .setValue(BedBlock.FACING, Direction.EAST).setValue(BedBlock.PART, BedPart.FOOT);
        assertLeftAlone(helper, Map.of(AIMED, foot, AIMED.east(), foot.setValue(BedBlock.PART, BedPart.HEAD)),
                AIMED, AIMED.east());
    }

    // A torch on a wall's south face, turned, would hang on air.
    private static void wouldNotStand(GameTestHelper helper) {
        assertLeftAlone(helper, Map.of(AIMED.north(), Blocks.STONE.defaultBlockState(),
                AIMED, Blocks.WALL_TORCH.defaultBlockState().setValue(WallTorchBlock.FACING, Direction.SOUTH)), AIMED);
    }

    private static void contractRefuses(GameTestHelper helper) {
        BlockState refusing = GroundworksGameTests.REFUSES_TO_TURN.get().defaultBlockState()
                .setValue(RefusesToTurnBlock.FACING, Direction.EAST);
        helper.setBlock(AIMED, refusing);
        for (boolean reverse : List.of(false, true)) {
            ListeningPlayer player = new ListeningPlayer(helper, AIMED.north(2));
            Rotate.press(player, helper.absolutePos(AIMED), reverse);
            if (!helper.getBlockState(AIMED).equals(refusing)) {
                helper.fail("a refused turn changed " + refusing + " to " + helper.getBlockState(AIMED), AIMED);
            }
            if (!player.heard.equals(List.of(RefusesToTurnBlock.REASON))) {
                helper.fail("a refused turn named " + player.heard + ", expected " + RefusesToTurnBlock.REASON, AIMED);
            }
        }
    }

    /** In a claim the press changes nothing; out of it, the same press turns the block, so the claim is what stopped it. */
    private static void claimGuards(GameTestHelper helper) {
        BlockPos claimed = helper.absolutePos(AIMED);
        CLAIMED.add(claimed);
        try {
            assertLeftAlone(helper, Map.of(AIMED, TURNING.getFirst().start()), AIMED);
        } finally {
            CLAIMED.remove(claimed);
        }
        assertHeldFallsThrough(helper, Blocks.COBBLESTONE);
    }

    // A modified client may name any block; one beyond the player's reach is none.
    private static void outOfReach(GameTestHelper helper) {
        BlockState start = TURNING.getFirst().start();
        helper.setBlock(AIMED, start);
        ListeningPlayer player = new ListeningPlayer(helper, AIMED.north(10));
        Rotate.press(player, helper.absolutePos(AIMED), false);
        if (!helper.getBlockState(AIMED).equals(start)) {
            helper.fail("a press from ten blocks away turned " + start + " to " + helper.getBlockState(AIMED), AIMED);
        }
    }

    /**
     * Sets {@code placed} and presses both ways at each of {@code aimed}, failing unless every block
     * placed is as it was after each press and the player is told nothing.
     */
    private static void assertLeftAlone(GameTestHelper helper, Map<BlockPos, BlockState> placed, BlockPos... aimed) {
        placed.forEach(helper::setBlock);
        Map<BlockPos, BlockState> before = new HashMap<>();
        placed.keySet().forEach(pos -> before.put(pos, helper.getBlockState(pos)));
        for (BlockPos pos : aimed) {
            for (boolean reverse : List.of(false, true)) {
                ListeningPlayer player = new ListeningPlayer(helper, pos.north(2));
                Rotate.press(player, helper.absolutePos(pos), reverse);
                before.forEach((kept, state) -> {
                    if (!helper.getBlockState(kept).equals(state)) {
                        helper.fail("a press at " + pos + " changed " + state + " to " + helper.getBlockState(kept), kept);
                    }
                });
                if (!player.heard.isEmpty()) {
                    helper.fail("a press that changed nothing named " + player.heard, pos);
                }
            }
        }
    }

    private static String name(Block block) {
        return BuiltInRegistries.BLOCK.getKey(block).toString();
    }

    /** A block's state before any press, and its state turned a number of quarters clockwise, negative the other way. */
    private record Turning(BlockState start, BiFunction<BlockState, Integer, BlockState> turned) {
    }
}
