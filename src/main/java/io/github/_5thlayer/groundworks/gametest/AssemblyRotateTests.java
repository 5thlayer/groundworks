// SPDX-FileCopyrightText: 2026 5thlayer
// SPDX-License-Identifier: MIT

package io.github._5thlayer.groundworks.gametest;

import java.util.Map;

import io.github._5thlayer.groundworks.Groundworks;
import io.github._5thlayer.groundworks.PlacementPlan;
import io.github._5thlayer.groundworks.Placements;
import io.github._5thlayer.groundworks.Rotate;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

/**
 * Rotate the Plan turns a drawn Assembly about its Key, on the {@code assemblies} test pack's
 * tower: cobblestone forward of the Key's column, one stone under the Key and one to the player's
 * right. Its Key, a lodestone, has no facing of its own, so the Assembly alone makes it rotatable.
 *
 * <p>The player stands west of {@link #SPOT} looking east and clicks the floor's top under it. A
 * quarter clockwise, the tower is placed as if they looked south: forward is south, their right is
 * west. A quarter back, as if they looked north: forward is north, their right is east.
 */
final class AssemblyRotateTests {

    private static final String PACK = "assemblies";
    private static final BlockPos SPOT = new BlockPos(2, 1, 2);
    private static final BlockPos KEY = SPOT.above();

    private AssemblyRotateTests() {
    }

    static void register(GroundworksGameTests.Registrar tests) {
        var withPack = tests.withPack(PACK);
        withPack.test("rotate_turns_a_drawn_assembly_about_its_key_even_when_the_key_has_no_facing", 20,
                AssemblyRotateTests::rotateTurns);
        withPack.test("reverse_rotate_turns_a_drawn_assembly_the_other_way", 20, AssemblyRotateTests::reverseRotateTurns);
        withPack.test("rotate_while_sneaking_with_a_key_with_no_facing_rotates_in_place", 20,
                AssemblyRotateTests::sneakingRotatesInPlace);
    }

    private static void rotateTurns(GameTestHelper helper) {
        ListeningPlayer player = holdingTheTowerKey(helper);
        Rotate.press(player, null, false);
        expectPlanAndPlaced(helper, player, SPOT.south(), SPOT.west());
    }

    private static void reverseRotateTurns(GameTestHelper helper) {
        ListeningPlayer player = holdingTheTowerKey(helper);
        Rotate.press(player, null, true);
        expectPlanAndPlaced(helper, player, SPOT.north(), SPOT.east());
    }

    /**
     * Sneaking, the lodestone places alone, which has no facing: the press turns the aimed stairs
     * in place rather than a plan no click places.
     */
    private static void sneakingRotatesInPlace(GameTestHelper helper) {
        helper.setBlock(SPOT, Blocks.OAK_STAIRS.defaultBlockState().setValue(StairBlock.FACING, Direction.NORTH));
        ListeningPlayer player = holdingTheTowerKey(helper);
        player.setShiftKeyDown(true);
        Rotate.press(player, helper.absolutePos(SPOT), false);
        BlockState stairs = helper.getBlockState(SPOT);
        if (!stairs.is(Blocks.OAK_STAIRS) || stairs.getValue(StairBlock.FACING) != Direction.EAST) {
            helper.fail("found " + stairs + ", not the stairs turned to face east", SPOT);
        }
        if (player.getMainHandItem().has(Groundworks.QUARTER_TURN.get())) {
            helper.fail("the press turned the held lodestone's plan");
        }
        helper.succeed();
    }

    /** The preview's plan and the click's placement both stand the tower turned, cobblestone forward, stone right. */
    private static void expectPlanAndPlaced(GameTestHelper helper, ListeningPlayer player, BlockPos forward,
                                            BlockPos right) {
        Map<BlockPos, Block> tower = Map.of(SPOT, Blocks.STONE, right, Blocks.STONE, forward, Blocks.COBBLESTONE,
                KEY, Blocks.LODESTONE);
        PlacementPlan plan = Placements.planFor(player.level(), player, InteractionHand.MAIN_HAND,
                player.getMainHandItem(), onTheFloor(helper));
        if (plan == null || plan.isRefused() || plan.blocks().size() != tower.size()) {
            helper.fail("the plan was " + plan + ", not the turned tower accepted");
        }
        tower.forEach((pos, block) -> {
            if (plan.blocks().stream().noneMatch(placed -> placed.pos().equals(helper.absolutePos(pos))
                    && placed.state().is(block))) {
                helper.fail("the plan " + plan + " has no " + block, pos);
            }
        });
        player.gameMode.useItemOn(player, helper.getLevel(), player.getMainHandItem(), InteractionHand.MAIN_HAND,
                onTheFloor(helper));
        tower.forEach((pos, block) -> {
            if (!helper.getBlockState(pos).is(block)) {
                helper.fail("found " + helper.getBlockState(pos) + ", not " + block, pos);
            }
        });
        helper.succeed();
    }

    private static ListeningPlayer holdingTheTowerKey(GameTestHelper helper) {
        ListeningPlayer player = new ListeningPlayer(helper, new BlockPos(0, 1, 2));
        player.setGameMode(GameType.CREATIVE);
        player.setYRot(Direction.EAST.toYRot());
        player.setYHeadRot(Direction.EAST.toYRot());
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.LODESTONE, 1));
        return player;
    }

    /** The top of the floor block under {@link #SPOT}. */
    private static BlockHitResult onTheFloor(GameTestHelper helper) {
        BlockPos floor = helper.absolutePos(SPOT.below());
        return new BlockHitResult(Vec3.atCenterOf(floor).relative(Direction.UP, 0.5), Direction.UP, floor, false);
    }
}
