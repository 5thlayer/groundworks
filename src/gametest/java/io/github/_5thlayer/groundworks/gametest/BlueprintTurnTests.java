// SPDX-FileCopyrightText: 2026 5thlayer
// SPDX-License-Identifier: MIT

package io.github._5thlayer.groundworks.gametest;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import io.github._5thlayer.groundworks.Groundworks;
import io.github._5thlayer.groundworks.PlacementPlan;
import io.github._5thlayer.groundworks.Placements;
import io.github._5thlayer.groundworks.Raise;
import io.github._5thlayer.groundworks.Rotate;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * A held Blueprint's paste follows the plan verbs: Rotate and Reverse Rotate turn it a quarter from
 * its captured orientation, each block's facing with it, and Raise and Lower move it up and down,
 * capped by reach. A blank sheet isn't rotatable, so Rotate turns the block under the crosshair.
 *
 * <p>The build is a row of two along x, stone at {@link #FIRST} and oak stairs facing east east of
 * it. It is pasted on the floor's top under {@link #SPOT}: unturned it lands from one west of the
 * spot, stone west and stairs on the spot; a quarter clockwise it lands as a column along z, stone
 * one north of the spot and the stairs on it, facing south.
 */
final class BlueprintTurnTests {

    private static final BlockPos FIRST = new BlockPos(1, 1, 1);
    private static final BlockPos SECOND = FIRST.east();
    private static final BlockState STONE = Blocks.STONE.defaultBlockState();
    private static final BlockState STAIRS = Blocks.OAK_STAIRS.defaultBlockState()
            .setValue(StairBlock.FACING, Direction.EAST);

    private static final BlockPos SPOT = new BlockPos(7, 1, 7);
    /** Where the player stands: within reach of the floor under the spot, out of the paste's way. */
    private static final BlockPos STANDING = new BlockPos(4, 1, 7);

    private BlueprintTurnTests() {
    }

    static void register(GroundworksGameTests.Registrar tests) {
        tests.tallTest("blueprint_rotate_turns_the_paste_a_quarter_and_the_click_lays_it_turned", 20,
                BlueprintTurnTests::rotateTurns);
        tests.tallTest("blueprint_reverse_rotate_turns_the_paste_a_quarter_back", 20,
                BlueprintTurnTests::reverseRotateTurnsBack);
        tests.tallTest("blueprint_rotate_with_a_blank_sheet_turns_the_block_under_the_crosshair", 20,
                BlueprintTurnTests::blankSheetTurnsInPlace);
        tests.tallTest("blueprint_rotate_with_nothing_aimed_is_refused", 20, BlueprintTurnTests::nothingAimed);
        tests.tallTest("blueprint_raise_and_lower_move_the_planned_and_laid_paste", 20,
                BlueprintTurnTests::raiseAndLower);
        tests.tallTest("blueprint_raise_is_capped_by_reach", 20, BlueprintTurnTests::raiseCappedByReach);
    }

    private static void rotateTurns(GameTestHelper helper) {
        ListeningPlayer player = withCopy(helper);
        Rotate.press(player, topOf(helper.absolutePos(SPOT.below())), false);
        expectPlanned(helper, player, Map.of(SPOT.north(), STONE,
                SPOT, STAIRS.setValue(StairBlock.FACING, Direction.SOUTH)));
        paste(helper, player);
        expectState(helper, SPOT.north(), STONE);
        expectState(helper, SPOT, STAIRS.setValue(StairBlock.FACING, Direction.SOUTH));
        helper.assertBlockPresent(Blocks.AIR, SPOT.west());
        helper.succeed();
    }

    private static void reverseRotateTurnsBack(GameTestHelper helper) {
        ListeningPlayer player = withCopy(helper);
        Rotate.press(player, topOf(helper.absolutePos(SPOT.below())), true);
        Map<BlockPos, BlockState> turned = Map.of(SPOT, STONE,
                SPOT.north(), STAIRS.setValue(StairBlock.FACING, Direction.NORTH));
        expectPlanned(helper, player, turned);
        paste(helper, player);
        turned.forEach((pos, state) -> expectState(helper, pos, state));
        helper.succeed();
    }

    private static void blankSheetTurnsInPlace(GameTestHelper helper) {
        ListeningPlayer player = new ListeningPlayer(helper, STANDING);
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.PAPER));
        BlockPos aimed = STANDING.north(2);
        helper.setBlock(aimed, STAIRS);
        Rotate.press(player, topOf(helper.absolutePos(aimed)), false);
        expectState(helper, aimed, STAIRS.setValue(StairBlock.FACING, Direction.SOUTH));
        if (player.getMainHandItem().has(Groundworks.QUARTER_TURN.get())) {
            helper.fail("the blank sheet took a turn");
        }
        helper.succeed();
    }

    private static void nothingAimed(GameTestHelper helper) {
        ListeningPlayer player = withCopy(helper);
        Rotate.press(player, null, false);
        if (player.getMainHandItem().has(Groundworks.QUARTER_TURN.get())
                || !player.heard.contains(Rotate.NOTHING_DRAWN)) {
            helper.fail("the press with nothing aimed was not refused: the player heard " + player.heard);
        }
        helper.succeed();
    }

    private static void raiseAndLower(GameTestHelper helper) {
        ListeningPlayer player = withCopy(helper);
        Raise.press(player, false);
        Raise.press(player, false);
        Raise.press(player, true);
        BlockPos raised = SPOT.above();
        expectPlanned(helper, player, Map.of(raised.west(), STONE, raised, STAIRS));
        paste(helper, player);
        expectState(helper, raised.west(), STONE);
        expectState(helper, raised, STAIRS);
        helper.assertBlockPresent(Blocks.AIR, SPOT);
        helper.assertBlockPresent(Blocks.AIR, SPOT.west());
        helper.succeed();
    }

    private static void raiseCappedByReach(GameTestHelper helper) {
        ListeningPlayer player = withCopy(helper);
        int cap = Raise.capOf(player);
        for (int press = 0; press < cap; press++) {
            Raise.press(player, false);
        }
        if (!player.heard.isEmpty()) {
            helper.fail("a raise within reach was refused: the player heard " + player.heard);
        }
        Raise.press(player, false);
        if (Raise.heightOf(player, player.getMainHandItem()).blocks() != cap
                || !player.heard.equals(List.of("message.groundworks.height_at_reach"))) {
            helper.fail("the paste was raised past reach, to " + Raise.heightOf(player, player.getMainHandItem())
                    + ", the player hearing " + player.heard);
        }
        helper.succeed();
    }

    // ---- Helpers ---------------------------------------------------------------------------------

    /** A creative player holding a sheet with the build copied onto it, having heard nothing since. */
    private static ListeningPlayer withCopy(GameTestHelper helper) {
        ListeningPlayer player = new ListeningPlayer(helper, STANDING);
        player.setGameMode(GameType.CREATIVE);
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.PAPER));
        helper.setBlock(FIRST, STONE);
        helper.setBlock(SECOND, STAIRS);
        player.setShiftKeyDown(true);
        click(helper, player, FIRST);
        player.setShiftKeyDown(false);
        click(helper, player, SECOND);
        if (!player.heard.contains("message.groundworks.blueprint_copied")) {
            helper.fail("the build was not copied: the player heard " + player.heard);
        }
        player.heard.clear();
        return player;
    }

    private static void expectPlanned(GameTestHelper helper, ListeningPlayer player, Map<BlockPos, BlockState> wanted) {
        PlacementPlan plan = plan(helper, player);
        if (plan == null || plan.isRefused() || plan.blocks().size() != wanted.size()) {
            helper.fail("the paste's plan was " + plan + ", not " + wanted + " accepted");
            return;
        }
        Map<BlockPos, BlockState> planned = new HashMap<>();
        plan.blocks().forEach(placed -> planned.put(placed.pos(), placed.state()));
        wanted.forEach((pos, state) -> {
            if (planned.get(helper.absolutePos(pos)) != state) {
                helper.fail("the plan places " + planned.get(helper.absolutePos(pos)) + ", not " + state, pos);
            }
        });
    }

    private static void expectState(GameTestHelper helper, BlockPos pos, BlockState wanted) {
        if (helper.getBlockState(pos) != wanted) {
            helper.fail("holds " + helper.getBlockState(pos) + ", not " + wanted, pos);
        }
    }

    private static @Nullable PlacementPlan plan(GameTestHelper helper, ListeningPlayer player) {
        return Placements.planFor(player.level(), player, InteractionHand.MAIN_HAND, player.getMainHandItem(),
                topOf(helper.absolutePos(SPOT.below())));
    }

    private static void paste(GameTestHelper helper, ListeningPlayer player) {
        click(helper, player, SPOT.below());
    }

    private static void click(GameTestHelper helper, ListeningPlayer player, BlockPos pos) {
        player.gameMode.useItemOn(player, helper.getLevel(), player.getMainHandItem(), InteractionHand.MAIN_HAND,
                topOf(helper.absolutePos(pos)));
    }

    private static BlockHitResult topOf(BlockPos absolute) {
        return new BlockHitResult(Vec3.atCenterOf(absolute).relative(Direction.UP, 0.5), Direction.UP, absolute, false);
    }
}
