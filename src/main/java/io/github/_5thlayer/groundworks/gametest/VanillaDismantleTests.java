// SPDX-FileCopyrightText: 2026 5thlayer
// SPDX-License-Identifier: MIT

package io.github._5thlayer.groundworks.gametest;

import java.util.function.IntFunction;

import io.github._5thlayer.groundworks.Groundworks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.RailShape;
import net.minecraft.world.level.block.state.properties.WallSide;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

/**
 * The vanilla Consumer's Dismantle families: the block tags under {@code
 * groundworks:dismantle_family/}, taken up with the tool vanilla breaks their blocks with. Each
 * test lays a line along x at z 1, from x 0 to x 4, with the states it names, so a fence connects
 * only where the test says it does.
 */
final class VanillaDismantleTests {

    /** The pack under {@code gametest_packs/} that adds a family of logs and takes rails out of theirs. */
    static final String CHANGES_FAMILIES = "changes_dismantle_families";

    private static final int LINE = 1;
    private static final int BESIDE = 2;

    private VanillaDismantleTests() {
    }

    static void register(GroundworksGameTests.Registrar tests) {
        tests.test("an_axe_takes_up_a_span_of_connected_oak_fences", 20, helper -> {
            ListeningPlayer player = line(helper, Items.IRON_AXE, x -> alongX(Blocks.OAK_FENCE, x));
            takesUp(helper, player, Blocks.OAK_FENCE);
        });
        tests.test("fences_side_by_side_that_do_not_connect_are_not_joined", 20, helper -> {
            ListeningPlayer player = line(helper, Items.IRON_AXE, x -> alongX(Blocks.OAK_FENCE, x));
            for (int x = 0; x <= 4; x++) {
                place(helper, at(x, BESIDE), alongX(Blocks.OAK_FENCE, x));
            }
            click(helper, player, at(0, LINE), true);
            click(helper, player, at(4, BESIDE), false);
            expectLine(helper, LINE, Blocks.OAK_FENCE);
            expectLine(helper, BESIDE, Blocks.OAK_FENCE);
            helper.assertTrue(player.heard.contains("message.groundworks.dismantle_not_joined"), "told they aren't joined");
            helper.succeed();
        });
        tests.test("a_pickaxe_takes_up_a_span_of_rails", 20, helper ->
                takesUp(helper, line(helper, Items.IRON_PICKAXE, x -> Blocks.RAIL.defaultBlockState()
                        .setValue(BlockStateProperties.RAIL_SHAPE, RailShape.EAST_WEST)),
                        Blocks.RAIL));
        tests.test("a_pickaxe_takes_up_a_span_of_stone_walls", 20, helper ->
                takesUp(helper, line(helper, Items.IRON_PICKAXE, x -> wallAlongX(x)), Blocks.COBBLESTONE_WALL));
        tests.test("a_pickaxe_takes_up_a_span_of_iron_bars", 20, helper ->
                takesUp(helper, line(helper, Items.IRON_PICKAXE, x -> alongX(Blocks.IRON_BARS, x)), Blocks.IRON_BARS));
        tests.test("a_pickaxe_sneak_click_on_an_oak_fence_passes_on", 20, helper -> {
            ListeningPlayer player = line(helper, Items.IRON_PICKAXE, x -> alongX(Blocks.OAK_FENCE, x));
            click(helper, player, at(0, LINE), true);
            helper.assertFalse(player.getMainHandItem().has(Groundworks.DISMANTLE_START.get()), "no start stored");
            helper.succeed();
        });
        tests.test("a_span_of_oak_and_nether_brick_fences_is_refused_for_the_tool", 20, helper -> {
            ListeningPlayer player = line(helper, Items.IRON_AXE,
                    x -> alongX(x == 2 ? Blocks.NETHER_BRICK_FENCE : Blocks.OAK_FENCE, x));
            click(helper, player, at(0, LINE), true);
            click(helper, player, at(4, LINE), false);
            helper.assertBlockPresent(Blocks.OAK_FENCE, at(0, LINE));
            helper.assertBlockPresent(Blocks.NETHER_BRICK_FENCE, at(2, LINE));
            helper.assertTrue(player.heard.contains("message.groundworks.dismantle_wrong_tool"), "told the tool is wrong");
            helper.succeed();
        });
        tests.test("a_span_from_a_fence_to_a_wall_is_not_the_same_kind", 20, helper -> {
            ListeningPlayer player = line(helper, Items.IRON_AXE, x -> x < 3 ? alongX(Blocks.OAK_FENCE, x) : wallAlongX(x));
            click(helper, player, at(0, LINE), true);
            click(helper, player, at(4, LINE), false);
            helper.assertBlockPresent(Blocks.OAK_FENCE, at(0, LINE));
            helper.assertTrue(player.heard.contains("message.groundworks.dismantle_not_same_kind"), "told it is not the same kind");
            helper.succeed();
        });

        var changed = tests.withPack(CHANGES_FAMILIES);
        changed.test("a_datapack_adding_a_family_tag_makes_a_new_family", 20, helper ->
                takesUp(helper, line(helper, Items.IRON_AXE, x -> Blocks.OAK_LOG.defaultBlockState()
                        .setValue(BlockStateProperties.AXIS, Direction.Axis.X)), Blocks.OAK_LOG));
        changed.test("a_datapack_removing_rails_from_their_family_takes_them_out", 20, helper -> {
            ListeningPlayer player = line(helper, Items.IRON_PICKAXE, x -> Blocks.RAIL.defaultBlockState());
            click(helper, player, at(0, LINE), true);
            helper.assertFalse(player.getMainHandItem().has(Groundworks.DISMANTLE_START.get()), "no start stored");
            helper.succeed();
        });
    }

    /** A sneak-click at one end and a click at the other take up the whole line and hand it over. */
    private static void takesUp(GameTestHelper helper, ListeningPlayer player, Block block) {
        click(helper, player, at(0, LINE), true);
        click(helper, player, at(4, LINE), false);
        for (int x = 0; x <= 4; x++) {
            helper.assertBlockPresent(Blocks.AIR, at(x, LINE));
        }
        helper.assertValueEqual(5, player.getInventory().countItem(block.asItem()), "blocks handed");
        helper.succeed();
    }

    /** Lays the line and hands a survival player the tool, standing beside it. */
    private static ListeningPlayer line(GameTestHelper helper, Item tool, IntFunction<BlockState> states) {
        for (int x = 0; x <= 4; x++) {
            place(helper, at(x, LINE), states.apply(x));
        }
        ListeningPlayer player = new ListeningPlayer(helper, new BlockPos(2, 1, 3));
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(tool));
        return player;
    }

    /** Sets the state as given, with no neighbour reshaping it. */
    private static void place(GameTestHelper helper, BlockPos pos, BlockState state) {
        helper.getLevel().setBlock(helper.absolutePos(pos), state, Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE);
    }

    /** A block with sides, connecting toward the line's neighbours along x and nowhere else. */
    private static BlockState alongX(Block block, int x) {
        return block.defaultBlockState()
                .setValue(BlockStateProperties.WEST, x > 0)
                .setValue(BlockStateProperties.EAST, x < 4);
    }

    private static BlockState wallAlongX(int x) {
        return Blocks.COBBLESTONE_WALL.defaultBlockState()
                .setValue(BlockStateProperties.UP, false)
                .setValue(BlockStateProperties.WEST_WALL, x > 0 ? WallSide.LOW : WallSide.NONE)
                .setValue(BlockStateProperties.EAST_WALL, x < 4 ? WallSide.LOW : WallSide.NONE);
    }

    private static BlockPos at(int x, int z) {
        return new BlockPos(x, 1, z);
    }

    private static void click(GameTestHelper helper, ListeningPlayer player, BlockPos pos, boolean sneaking) {
        BlockPos absolute = helper.absolutePos(pos);
        player.setShiftKeyDown(sneaking);
        player.gameMode.useItemOn(player, helper.getLevel(), player.getMainHandItem(), InteractionHand.MAIN_HAND,
                new BlockHitResult(Vec3.atCenterOf(absolute).relative(Direction.UP, 0.5), Direction.UP, absolute, false));
        player.setShiftKeyDown(false);
    }

    private static void expectLine(GameTestHelper helper, int z, Block block) {
        for (int x = 0; x <= 4; x++) {
            helper.assertBlockPresent(block, at(x, z));
        }
    }
}
