// SPDX-FileCopyrightText: 2026 5thlayer
// SPDX-License-Identifier: MIT

package io.github._5thlayer.groundworks.gametest;

import io.github._5thlayer.groundworks.Dismantles;
import io.github._5thlayer.groundworks.Groundworks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

/**
 * The Dismantle on the tests' own family, a {@linkplain RowOfTerracotta row of cyan terracotta},
 * with the tests' own tool. Every click goes through the server player's own {@code useItemOn} and
 * {@code useItem}, so through the events Groundworks answers. Each test lays two rows on the floor,
 * along x at z 1 and z 3, from x 0 to x 4.
 */
final class DismantleTests {

    private static final int FIRST = 1;
    private static final int SECOND = 3;

    private DismantleTests() {
    }

    static void register(GroundworksGameTests.Registrar tests) {
        tests.test("a_sneak_click_with_a_start_stored_queues_the_span_and_takes_nothing", 20, DismantleTests::queues);
        tests.test("a_click_with_a_start_in_progress_takes_up_the_queued_span_and_its_own", 20,
                DismantleTests::confirmsWithAStart);
        tests.test("a_click_with_no_start_takes_up_the_queued_spans_wherever_it_aims", 20,
                DismantleTests::confirmsQueueOnly);
        tests.test("two_overlapping_spans_take_each_block_once", 20, DismantleTests::overlap);
        tests.test("a_queued_span_whose_start_is_gone_refuses_the_whole_pass", 20, DismantleTests::staleRefuses);
        tests.test("a_sneak_click_off_the_row_is_refused_and_keeps_the_start", 20, DismantleTests::offRow);
        tests.test("a_third_span_is_refused_as_queue_full", 20, DismantleTests::queueFull);
        tests.test("a_sneak_use_in_the_air_clears_the_start_and_the_queue", 20, DismantleTests::clears);
    }

    private static void queues(GameTestHelper helper) {
        ListeningPlayer player = rows(helper);
        click(helper, player, at(0, FIRST), true);
        click(helper, player, at(4, FIRST), true);
        helper.assertValueEqual(1, Dismantles.queued(held(player)).size(), "queued spans");
        helper.assertFalse(held(player).has(Groundworks.DISMANTLE_START.get()), "the start is cleared");
        expectRow(helper, FIRST, 0, 4, true);
        helper.assertTrue(player.heard.contains("message.groundworks.dismantle_queued"), "told it is queued");
        helper.succeed();
    }

    private static void confirmsWithAStart(GameTestHelper helper) {
        ListeningPlayer player = rows(helper);
        click(helper, player, at(0, FIRST), true);
        click(helper, player, at(2, FIRST), true);
        click(helper, player, at(1, SECOND), true);
        click(helper, player, at(3, SECOND), false);
        expectRow(helper, FIRST, 0, 2, false);
        expectRow(helper, FIRST, 3, 4, true);
        expectRow(helper, SECOND, 1, 3, false);
        expectRow(helper, SECOND, 0, 0, true);
        expectRow(helper, SECOND, 4, 4, true);
        expectEmptied(helper, player);
        expectHanded(helper, player, 6);
        helper.succeed();
    }

    private static void confirmsQueueOnly(GameTestHelper helper) {
        ListeningPlayer player = rows(helper);
        click(helper, player, at(0, FIRST), true);
        click(helper, player, at(4, FIRST), true);
        click(helper, player, at(0, SECOND), true);
        click(helper, player, at(1, SECOND), true);
        // Aimed at the floor, off both rows.
        click(helper, player, new BlockPos(2, 0, 2), false);
        expectRow(helper, FIRST, 0, 4, false);
        expectRow(helper, SECOND, 0, 1, false);
        expectRow(helper, SECOND, 2, 4, true);
        expectEmptied(helper, player);
        helper.succeed();
    }

    private static void overlap(GameTestHelper helper) {
        ListeningPlayer player = rows(helper);
        click(helper, player, at(0, FIRST), true);
        click(helper, player, at(3, FIRST), true);
        click(helper, player, at(1, FIRST), true);
        click(helper, player, at(4, FIRST), false);
        expectRow(helper, FIRST, 0, 4, false);
        expectHanded(helper, player, 5);
        helper.succeed();
    }

    private static void staleRefuses(GameTestHelper helper) {
        ListeningPlayer player = rows(helper);
        click(helper, player, at(0, FIRST), true);
        click(helper, player, at(2, FIRST), true);
        helper.setBlock(at(0, FIRST), Blocks.AIR);
        click(helper, player, at(0, SECOND), true);
        click(helper, player, at(4, SECOND), false);
        expectRow(helper, FIRST, 1, 4, true);
        expectRow(helper, SECOND, 0, 4, true);
        helper.assertValueEqual(1, Dismantles.queued(held(player)).size(), "queued spans kept");
        helper.assertTrue(held(player).has(Groundworks.DISMANTLE_START.get()), "the start is kept");
        helper.assertTrue(player.heard.contains("message.groundworks.dismantle_start_gone"), "told the start is gone");
        helper.succeed();
    }

    private static void offRow(GameTestHelper helper) {
        ListeningPlayer player = rows(helper);
        click(helper, player, at(1, FIRST), true);
        click(helper, player, at(3, SECOND), true);
        helper.assertValueEqual(helper.absolutePos(at(1, FIRST)),
                held(player).get(Groundworks.DISMANTLE_START.get()).pos(), "the start stays");
        helper.assertTrue(Dismantles.queued(held(player)).isEmpty(), "nothing queued");
        helper.assertTrue(player.heard.contains("message.groundworks.gametest_off_row"), "told it is off the row");
        helper.succeed();
    }

    private static void queueFull(GameTestHelper helper) {
        ListeningPlayer player = rows(helper);
        click(helper, player, at(0, FIRST), true);
        click(helper, player, at(1, FIRST), true);
        click(helper, player, at(0, SECOND), true);
        click(helper, player, at(1, SECOND), true);
        click(helper, player, at(3, FIRST), true);
        helper.assertValueEqual(Dismantles.MAX_SPANS, Dismantles.queued(held(player)).size(), "queued spans");
        helper.assertFalse(held(player).has(Groundworks.DISMANTLE_START.get()), "no start stored");
        helper.assertTrue(player.heard.contains("message.groundworks.dismantle_queue_full"), "told the queue is full");
        helper.succeed();
    }

    private static void clears(GameTestHelper helper) {
        ListeningPlayer player = rows(helper);
        click(helper, player, at(0, FIRST), true);
        click(helper, player, at(2, FIRST), true);
        click(helper, player, at(0, SECOND), true);
        player.setShiftKeyDown(true);
        player.gameMode.useItem(player, helper.getLevel(), player.getMainHandItem(), InteractionHand.MAIN_HAND);
        player.setShiftKeyDown(false);
        expectEmptied(helper, player);
        expectRow(helper, FIRST, 0, 4, true);
        helper.succeed();
    }

    private static BlockPos at(int x, int z) {
        return new BlockPos(x, 1, z);
    }

    /** Lays both rows and hands a survival player the tool, standing between them. */
    private static ListeningPlayer rows(GameTestHelper helper) {
        for (int x = 0; x <= 4; x++) {
            helper.setBlock(at(x, FIRST), RowOfTerracotta.MEMBER);
            helper.setBlock(at(x, SECOND), RowOfTerracotta.MEMBER);
        }
        ListeningPlayer player = new ListeningPlayer(helper, new BlockPos(2, 0, 2));
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(GroundworksGameTests.DISMANTLES.get()));
        return player;
    }

    private static ItemStack held(ListeningPlayer player) {
        return player.getMainHandItem();
    }

    private static void click(GameTestHelper helper, ListeningPlayer player, BlockPos pos, boolean sneaking) {
        BlockPos absolute = helper.absolutePos(pos);
        player.setShiftKeyDown(sneaking);
        player.gameMode.useItemOn(player, helper.getLevel(), player.getMainHandItem(), InteractionHand.MAIN_HAND,
                new BlockHitResult(Vec3.atCenterOf(absolute).relative(Direction.UP, 0.5), Direction.UP, absolute, false));
        player.setShiftKeyDown(false);
    }

    private static void expectRow(GameTestHelper helper, int z, int fromX, int toX, boolean standing) {
        for (int x = fromX; x <= toX; x++) {
            if (standing) {
                helper.assertBlockPresent(RowOfTerracotta.MEMBER, at(x, z));
            } else {
                helper.assertBlockPresent(Blocks.AIR, at(x, z));
            }
        }
    }

    private static void expectEmptied(GameTestHelper helper, ListeningPlayer player) {
        helper.assertFalse(held(player).has(Groundworks.DISMANTLE_START.get()), "no start stored");
        helper.assertFalse(held(player).has(Groundworks.DISMANTLE_QUEUE.get()), "no span queued");
    }

    private static void expectHanded(GameTestHelper helper, ListeningPlayer player, int count) {
        helper.assertValueEqual(count, player.getInventory().countItem(RowOfTerracotta.MEMBER.asItem()), "terracotta handed");
    }
}
