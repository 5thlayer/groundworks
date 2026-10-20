// SPDX-FileCopyrightText: 2026 5thlayer
// SPDX-License-Identifier: MIT

package io.github._5thlayer.groundworks.gametest;

import java.util.List;

import io.github._5thlayer.groundworks.Groundworks;
import io.github._5thlayer.groundworks.PlacementPlan;
import io.github._5thlayer.groundworks.Placements;
import io.github._5thlayer.groundworks.QuarterTurn;
import io.github._5thlayer.groundworks.Rotate;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.AmethystClusterBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.HopperBlock;
import net.minecraft.world.level.block.LadderBlock;
import net.minecraft.world.level.block.TrapDoorBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

/**
 * Rotate the Plan against a side face (ADR 0012): the player stands east of a stone wall looking
 * west and aims its east face, so the block goes in the spot east of the wall. A block that faces
 * the face it is placed on turns with the press; one that hangs on that face has nothing to turn to
 * and the press is refused, as it is with nothing aimed; a turn whose plan would not stand is passed
 * over, unless the plan drawn is refused already. Every press goes through {@link Rotate#press}, as
 * the key's payload does.
 */
final class SideFaceRotateTests {

    private SideFaceRotateTests() {
    }

    static void register(GroundworksGameTests.Registrar tests) {
        tests.test("rotate_on_a_side_turns_a_trapdoor_placed_against_it", 20, SideFaceRotateTests::trapdoorTurns);
        tests.test("rotate_on_a_side_four_times_carries_no_turn", 20, SideFaceRotateTests::fourPressesUnturned);
        tests.test("rotate_on_a_side_is_refused_for_a_ladder_that_hangs_on_it", 20, SideFaceRotateTests::ladderRefused);
        tests.test("rotate_on_a_side_passes_over_a_turn_that_would_not_stand", 20,
                SideFaceRotateTests::amethystPassesOver);
        tests.test("rotate_on_a_side_still_turns_a_plan_refused_for_something_else", 20,
                SideFaceRotateTests::refusedPlanTurns);
        tests.test("rotate_on_a_side_with_nothing_aimed_is_refused", 20, SideFaceRotateTests::nothingAimed);
        tests.withPack("assemblies").test("rotate_on_a_side_turns_an_assemblys_key_once", 20,
                SideFaceRotateTests::assemblyKeyTurnedOnce);
    }

    /** For 0 to 3 presses, each changing the drawn plan, the trapdoor goes down facing the face turned that many quarters, as planned. */
    private static void trapdoorTurns(GameTestHelper helper) {
        for (int presses = 0; presses < 4; presses++) {
            BlockPos wall = new BlockPos(2, 1, 1 + 2 * presses);
            ListeningPlayer player = facingTheWall(helper, wall, new ItemStack(Items.OAK_TRAPDOOR, 4));
            BlockHitResult hit = onTheEastFace(helper, wall);
            PlacementPlan drawn = plan(player, hit);
            for (int press = 0; press < presses; press++) {
                Rotate.press(player, hit, false);
                PlacementPlan turned = plan(player, hit);
                if (turned == null || turned.equals(drawn)) {
                    helper.fail("press " + (press + 1) + " left the plan " + turned, wall.east());
                }
                drawn = turned;
            }
            if (!Rotate.turnOf(player.getMainHandItem()).equals(QuarterTurn.of(presses)) || !player.heard.isEmpty()) {
                helper.fail(presses + " presses turned the stack " + Rotate.turnOf(player.getMainHandItem())
                        + " and told " + player.heard, wall.east());
            }
            BlockState placed = placeAsPlanned(helper, player, hit, wall.east());
            Direction facing = QuarterTurn.of(presses).turn(Direction.EAST);
            if (!placed.is(Blocks.OAK_TRAPDOOR) || placed.getValue(TrapDoorBlock.FACING) != facing) {
                helper.fail("after " + presses + " presses placed " + placed + ", expected a trapdoor facing " + facing,
                        wall.east());
            }
        }
        helper.succeed();
    }

    private static void fourPressesUnturned(GameTestHelper helper) {
        BlockPos wall = new BlockPos(2, 1, 2);
        ListeningPlayer player = facingTheWall(helper, wall, new ItemStack(Items.OAK_TRAPDOOR, 4));
        BlockHitResult hit = onTheEastFace(helper, wall);
        PlacementPlan start = plan(player, hit);
        for (boolean reverse : List.of(false, true)) {
            for (int press = 0; press < 4; press++) {
                Rotate.press(player, hit, reverse);
            }
            if (player.getMainHandItem().has(Groundworks.QUARTER_TURN.get())) {
                helper.fail("four " + (reverse ? "Reverse Rotates" : "Rotates") + " left a turn on the stack");
            }
            PlacementPlan back = plan(player, hit);
            if (back == null || !back.equals(start)) {
                helper.fail("four " + (reverse ? "Reverse Rotates" : "Rotates") + " left the plan " + back
                        + ", not the one drawn before them, " + start, wall.east());
            }
        }
        helper.succeed();
    }

    /** A ladder hangs on the face whatever the turn, so the press is refused and the stack keeps no turn. */
    private static void ladderRefused(GameTestHelper helper) {
        BlockPos wall = new BlockPos(2, 1, 2);
        for (boolean reverse : List.of(false, true)) {
            ListeningPlayer player = facingTheWall(helper, wall, new ItemStack(Items.LADDER, 4));
            BlockHitResult hit = onTheEastFace(helper, wall);
            Rotate.press(player, hit, reverse);
            if (player.getMainHandItem().has(Groundworks.QUARTER_TURN.get()) || !player.heard.equals(List.of(Rotate.UNCHANGED))) {
                helper.fail("a press against a ladder's wall turned the stack " + Rotate.turnOf(player.getMainHandItem())
                        + " and told " + player.heard);
            }
            BlockState placed = placeAsPlanned(helper, player, hit, wall.east());
            if (!placed.is(Blocks.LADDER) || placed.getValue(LadderBlock.FACING) != Direction.EAST) {
                helper.fail("placed " + placed + ", expected a ladder on the wall facing east", wall.east());
            }
            helper.setBlock(wall.east(), Blocks.AIR);
        }
        helper.succeed();
    }

    /**
     * An amethyst cluster stands on the face it is placed against. A quarter turn would stand it on
     * nothing; a half turn stands it on the stone east of its spot, so one press takes the half turn.
     */
    private static void amethystPassesOver(GameTestHelper helper) {
        BlockPos wall = new BlockPos(2, 1, 2);
        helper.setBlock(wall.east(2), Blocks.STONE);
        ListeningPlayer player = facingTheWall(helper, wall, new ItemStack(Items.AMETHYST_CLUSTER, 4));
        BlockHitResult hit = onTheEastFace(helper, wall);
        Rotate.press(player, hit, false);
        if (!Rotate.turnOf(player.getMainHandItem()).equals(QuarterTurn.of(2)) || !player.heard.isEmpty()) {
            helper.fail("the press turned the stack " + Rotate.turnOf(player.getMainHandItem()) + " and told "
                    + player.heard + ", expected a half turn");
        }
        BlockState placed = placeAsPlanned(helper, player, hit, wall.east());
        if (!placed.is(Blocks.AMETHYST_CLUSTER) || placed.getValue(AmethystClusterBlock.FACING) != Direction.WEST) {
            helper.fail("placed " + placed + ", expected an amethyst cluster facing west", wall.east());
        }
        helper.succeed();
    }

    /** A cow stands in the trapdoor's spot, so every turn is refused; the press still turns, as the player may turn before clearing it. */
    private static void refusedPlanTurns(GameTestHelper helper) {
        BlockPos wall = new BlockPos(2, 1, 2);
        helper.spawnWithNoFreeWill(EntityType.COW, wall.east());
        ListeningPlayer player = facingTheWall(helper, wall, new ItemStack(Items.OAK_TRAPDOOR, 4));
        BlockHitResult hit = onTheEastFace(helper, wall);
        PlacementPlan drawn = plan(player, hit);
        if (drawn == null || !drawn.isRefused()) {
            helper.fail("with a cow in the way the plan was " + drawn + ", expected one refused");
        }
        Rotate.press(player, hit, false);
        if (!Rotate.turnOf(player.getMainHandItem()).equals(QuarterTurn.of(1)) || !player.heard.isEmpty()) {
            helper.fail("the press turned the stack " + Rotate.turnOf(player.getMainHandItem()) + " and told "
                    + player.heard + ", expected a quarter turn");
        }
        helper.succeed();
    }

    private static void nothingAimed(GameTestHelper helper) {
        ListeningPlayer player = facingTheWall(helper, new BlockPos(2, 1, 2), new ItemStack(Items.OAK_TRAPDOOR, 4));
        Rotate.press(player, null, false);
        if (player.getMainHandItem().has(Groundworks.QUARTER_TURN.get()) || !player.heard.equals(List.of(Rotate.NOTHING_DRAWN))) {
            helper.fail("a press aiming nothing turned the stack " + Rotate.turnOf(player.getMainHandItem())
                    + " and told " + player.heard);
        }
        helper.succeed();
    }

    /**
     * The {@code assemblies} test pack's chute, a hopper Key with a stone behind it. A hopper faces
     * away from the face it is placed against, so the Key placed on the east face faces west
     * unturned, and a quarter turn applied once faces it north; applied twice it would face east.
     */
    private static void assemblyKeyTurnedOnce(GameTestHelper helper) {
        BlockPos wall = new BlockPos(2, 1, 2);
        ListeningPlayer player = facingTheWall(helper, wall, new ItemStack(Items.HOPPER, 1));
        player.setGameMode(GameType.CREATIVE);
        BlockHitResult hit = onTheEastFace(helper, wall);
        Rotate.press(player, hit, false);
        PlacementPlan plan = plan(player, hit);
        if (plan == null || plan.isRefused() || plan.blocks().size() != 2) {
            helper.fail("the plan was " + plan + ", not the chute accepted");
        }
        BlockState key = plan.blocks().getLast().state();
        if (!key.is(Blocks.HOPPER) || key.getValue(HopperBlock.FACING) != Direction.NORTH) {
            helper.fail("the plan's Key was " + key + ", expected a hopper facing north", wall.east());
        }
        player.gameMode.useItemOn(player, helper.getLevel(), player.getMainHandItem(), InteractionHand.MAIN_HAND, hit);
        if (!helper.getBlockState(wall.east()).equals(key)) {
            helper.fail("placed " + helper.getBlockState(wall.east()) + ", expected " + key, wall.east());
        }
        helper.succeed();
    }

    /** A stone wall at {@code wall}, and a survival player holding {@code held} two blocks east of it, looking west. */
    private static ListeningPlayer facingTheWall(GameTestHelper helper, BlockPos wall, ItemStack held) {
        helper.setBlock(wall, Blocks.STONE);
        ListeningPlayer player = new ListeningPlayer(helper, wall.east(3));
        player.setYRot(Direction.WEST.toYRot());
        player.setYHeadRot(Direction.WEST.toYRot());
        player.setItemInHand(InteractionHand.MAIN_HAND, held);
        return player;
    }

    private static BlockHitResult onTheEastFace(GameTestHelper helper, BlockPos wall) {
        BlockPos absolute = helper.absolutePos(wall);
        return new BlockHitResult(Vec3.atCenterOf(absolute).relative(Direction.EAST, 0.5), Direction.EAST, absolute, false);
    }

    private static PlacementPlan plan(ListeningPlayer player, BlockHitResult hit) {
        return Placements.planFor(player.level(), player, InteractionHand.MAIN_HAND, player.getMainHandItem(), hit);
    }

    /** Clicks the hit and answers what went into {@code spot}, failing unless the plan asked first agrees. */
    private static BlockState placeAsPlanned(GameTestHelper helper, ListeningPlayer player, BlockHitResult hit,
                                             BlockPos spot) {
        PlacementPlan plan = plan(player, hit);
        player.gameMode.useItemOn(player, helper.getLevel(), player.getMainHandItem(), InteractionHand.MAIN_HAND, hit);
        BlockState placed = helper.getBlockState(spot);
        if (plan == null || plan.isRefused()
                || !plan.blocks().equals(List.of(new PlacementPlan.Placed(helper.absolutePos(spot), placed)))) {
            helper.fail("the plan was " + plan + ", the click placed " + placed, spot);
        }
        return placed;
    }
}
