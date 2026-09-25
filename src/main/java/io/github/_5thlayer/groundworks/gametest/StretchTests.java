// SPDX-FileCopyrightText: 2026 5thlayer
// SPDX-License-Identifier: MIT

package io.github._5thlayer.groundworks.gametest;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import io.github._5thlayer.groundworks.Groundworks;
import io.github._5thlayer.groundworks.Height;
import io.github._5thlayer.groundworks.PlacementPlan;
import io.github._5thlayer.groundworks.Placements;
import io.github._5thlayer.groundworks.Raise;
import io.github._5thlayer.groundworks.Refusal;
import io.github._5thlayer.groundworks.StoredStretch;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * The Stretch on the tests' own stretch-able item, whose legs are a {@linkplain LineOfArrows line
 * of arrows} and whose rise climbs straight up in place. Every click goes through the server
 * player's own {@code useItemOn} and {@code useItem}, so through the events Groundworks answers,
 * and every press through {@link Raise#press}, as the keys' payload does. The preview's markers are
 * checked by hand.
 *
 * <p>Each stretch starts on the floor at {@link #START}, looking east, and so lays from one block
 * above it, and each end is aimed at the floor's top.
 */
final class StretchTests {

    private static final BlockPos START = new BlockPos(1, 0, 2);

    private StretchTests() {
    }

    static void register(GroundworksGameTests.Registrar tests) {
        tests.test("a_flat_stretch_lays_exactly_the_preview_s_plan_and_charges_one_item_per_block", 20,
                StretchTests::flat);
        tests.test("raise_twice_after_the_start_climbs_2_right_after_it_then_runs_level", 20,
                StretchTests::climbAfterTheStart);
        tests.test("a_start_stored_at_3_starts_the_stretch_3_up_in_mid_air", 20, StretchTests::startInMidAir);
        tests.test("a_second_leg_starts_level_after_its_anchor_and_its_own_lower_descends_right_after_it", 20,
                StretchTests::secondLeg);
        tests.test("an_end_aimed_at_a_wall_at_the_stretch_s_height_is_refused_and_nothing_is_laid_or_charged", 20,
                StretchTests::intoAWall);
        tests.test("an_end_behind_the_look_is_refused", 20, StretchTests::behindTheLook);
        tests.test("not_enough_items_refuses_the_stretch_whole", 20, StretchTests::notEnoughItems);
        tests.test("a_replaced_block_is_returned_to_the_inventory", 20, StretchTests::returned);
        tests.test("no_room_to_return_refuses_the_stretch_whole", 20, StretchTests::noRoomToReturn);
        tests.test("laying_resets_the_stored_stretch_and_the_height", 20, StretchTests::layingResets);
        tests.test("clearing_resets_the_stored_stretch_and_the_height", 20, StretchTests::clearingResets);
        tests.test("with_no_stretch_stored_a_click_places_one_block_as_planned", 20, StretchTests::single);
    }

    private static void flat(GameTestHelper helper) {
        ListeningPlayer player = holding(helper, 16);
        start(helper, player);
        BlockPos end = new BlockPos(5, 0, 2);
        List<BlockPos> laid = layAsPlanned(helper, player, end);
        expectLaid(helper, laid, line(1, 5, 1, 2));
        expectHeld(helper, player, 11);
        helper.succeed();
    }

    private static void climbAfterTheStart(GameTestHelper helper) {
        ListeningPlayer player = holding(helper, 16);
        start(helper, player);
        press(player, 2);
        List<BlockPos> laid = layAsPlanned(helper, player, new BlockPos(5, 0, 2));
        List<BlockPos> expected = new ArrayList<>(List.of(new BlockPos(1, 1, 2), new BlockPos(1, 2, 2)));
        expected.addAll(line(1, 5, 3, 2));
        expectLaid(helper, laid, expected);
        expectHeld(helper, player, 9);
        helper.succeed();
    }

    private static void startInMidAir(GameTestHelper helper) {
        ListeningPlayer player = holding(helper, 16);
        press(player, 3);
        start(helper, player);
        if (!Raise.heightOf(player, player.getMainHandItem()).equals(Height.NONE)) {
            helper.fail("storing the start left the height at " + Raise.heightOf(player, player.getMainHandItem()));
        }
        List<BlockPos> laid = layAsPlanned(helper, player, new BlockPos(4, 0, 2));
        expectLaid(helper, laid, line(1, 4, 4, 2));
        helper.succeed();
    }

    /** Raise 2 on the first leg, an anchor, then Lower 1 on the second, which turns south at the anchor. */
    private static void secondLeg(GameTestHelper helper) {
        ListeningPlayer player = holding(helper, 32);
        start(helper, player);
        press(player, 2);
        click(helper, player, new BlockPos(4, 0, 2), true);
        StoredStretch stored = player.getMainHandItem().get(Groundworks.STRETCH.get());
        if (stored == null || !stored.anchorPositions().equals(List.of(helper.absolutePos(new BlockPos(4, 3, 2))))
                || !Raise.heightOf(player, player.getMainHandItem()).equals(Height.NONE)) {
            helper.fail("the anchor stored " + stored + " at height " + Raise.heightOf(player, player.getMainHandItem()));
        }
        press(player, -1);
        List<BlockPos> laid = layAsPlanned(helper, player, new BlockPos(4, 0, 5));
        List<BlockPos> expected = new ArrayList<>(List.of(new BlockPos(1, 1, 2), new BlockPos(1, 2, 2)));
        expected.addAll(line(1, 4, 3, 2));
        expected.add(new BlockPos(4, 2, 2));
        for (int z = 3; z <= 5; z++) {
            expected.add(new BlockPos(4, 2, z));
        }
        expectLaid(helper, laid, expected);
        helper.succeed();
    }

    private static void intoAWall(GameTestHelper helper) {
        BlockPos wall = new BlockPos(5, 1, 2);
        helper.setBlock(wall, Blocks.STONE);
        helper.setBlock(wall.above(), Blocks.STONE);
        ListeningPlayer player = holding(helper, 16);
        start(helper, player);
        // Aimed at the wall's top, which picks the wall's column; the stretch's height there is the wall's lower block.
        BlockPos top = wall.above();
        PlacementPlan plan = plan(helper, player, top);
        if (plan == null || !(plan.refusal() instanceof Refusal.At at) || !at.pos().equals(helper.absolutePos(wall))) {
            helper.fail("the plan was " + plan + ", not refused at the wall", wall);
        }
        click(helper, player, top, true);
        click(helper, player, top, false);
        expectNothingLaidOrCharged(helper, player, 16, line(1, 4, 1, 2));
        if (!player.getMainHandItem().get(Groundworks.STRETCH.get()).anchors().isEmpty()) {
            helper.fail("a refused sneak-click stored an anchor");
        }
        if (!player.heard.equals(List.of("message.groundworks.stretch_started", "message.groundworks.gametest_blocked",
                "message.groundworks.gametest_blocked"))) {
            helper.fail("the player was told " + player.heard);
        }
        helper.succeed();
    }

    private static void behindTheLook(GameTestHelper helper) {
        ListeningPlayer player = holding(helper, 16);
        BlockPos start = new BlockPos(4, 0, 2);
        click(helper, player, start, true);
        BlockPos behind = new BlockPos(2, 0, 2);
        expectRefused(helper, player, behind, Refusal.Stretch.BEHIND_THE_LOOK);
        click(helper, player, behind, false);
        expectNothingLaidOrCharged(helper, player, 16, line(2, 4, 1, 2));
        helper.succeed();
    }

    private static void notEnoughItems(GameTestHelper helper) {
        ListeningPlayer player = holding(helper, 4);
        start(helper, player);
        BlockPos end = new BlockPos(5, 0, 2);
        expectRefused(helper, player, end, Refusal.Stretch.NOT_ENOUGH_ITEMS);
        click(helper, player, end, false);
        expectNothingLaidOrCharged(helper, player, 4, line(1, 5, 1, 2));
        helper.succeed();
    }

    private static void returned(GameTestHelper helper) {
        BlockPos replaced = new BlockPos(3, 1, 2);
        helper.setBlock(replaced, LineOfArrows.REPLACED);
        ListeningPlayer player = holding(helper, 16);
        start(helper, player);
        List<BlockPos> laid = layAsPlanned(helper, player, new BlockPos(5, 0, 2));
        expectLaid(helper, laid, line(1, 5, 1, 2));
        expectHeld(helper, player, 11);
        if (!player.getInventory().hasAnyMatching(stack -> stack.is(Items.LIGHT_BLUE_GLAZED_TERRACOTTA))) {
            helper.fail("the replaced block was not returned", replaced);
        }
        helper.succeed();
    }

    private static void noRoomToReturn(GameTestHelper helper) {
        BlockPos replaced = new BlockPos(3, 1, 2);
        helper.setBlock(replaced, LineOfArrows.REPLACED);
        ListeningPlayer player = holding(helper, 16);
        var inventory = player.getInventory().getNonEquipmentItems();
        for (int slot = 0; slot < inventory.size(); slot++) {
            if (inventory.get(slot).isEmpty()) {
                inventory.set(slot, new ItemStack(Items.DIRT, 64));
            }
        }
        start(helper, player);
        BlockPos end = new BlockPos(5, 0, 2);
        expectRefused(helper, player, end, Refusal.Stretch.NO_ROOM_TO_RETURN);
        click(helper, player, end, false);
        expectNothingLaidOrCharged(helper, player, 16, List.of(new BlockPos(1, 1, 2), new BlockPos(2, 1, 2)));
        if (!helper.getBlockState(replaced).is(LineOfArrows.REPLACED)) {
            helper.fail("the refused stretch replaced the light blue terracotta with " + helper.getBlockState(replaced), replaced);
        }
        helper.succeed();
    }

    private static void layingResets(GameTestHelper helper) {
        ListeningPlayer player = holding(helper, 16);
        start(helper, player);
        click(helper, player, new BlockPos(3, 0, 2), true);
        press(player, 1);
        click(helper, player, new BlockPos(3, 0, 5), false);
        expectReset(helper, player);
        helper.succeed();
    }

    private static void clearingResets(GameTestHelper helper) {
        ListeningPlayer player = holding(helper, 16);
        start(helper, player);
        press(player, 1);
        player.setShiftKeyDown(true);
        player.gameMode.useItem(player, helper.getLevel(), player.getMainHandItem(), InteractionHand.MAIN_HAND);
        player.setShiftKeyDown(false);
        expectReset(helper, player);
        if (!player.heard.getLast().equals("message.groundworks.stretch_cleared")) {
            helper.fail("the player was told " + player.heard);
        }
        helper.succeed();
    }

    /** The item places as any block item does until a start is stored, and its block still names vanilla's item. */
    private static void single(GameTestHelper helper) {
        ListeningPlayer player = holding(helper, 16);
        BlockPos floor = new BlockPos(4, 0, 4);
        PlacementPlan plan = plan(helper, player, floor);
        click(helper, player, floor, false);
        BlockPos placed = helper.absolutePos(floor.above());
        if (plan == null || !plan.blocks().equals(List.of(new PlacementPlan.Placed(placed, LineOfArrows.pointing(Direction.EAST))))
                || !helper.getBlockState(floor.above()).equals(LineOfArrows.pointing(Direction.EAST))) {
            helper.fail("the plan was " + plan + ", the click placed " + helper.getBlockState(floor.above()), floor.above());
        }
        expectHeld(helper, player, 15);
        if (LineOfArrows.ARROW.asItem() != Items.MAGENTA_GLAZED_TERRACOTTA) {
            helper.fail("magenta glazed terracotta names " + LineOfArrows.ARROW.asItem() + " as their item");
        }
        helper.succeed();
    }

    /**
     * Lays the stretch to an end aimed at {@code floor}'s top and answers where the plan asked first
     * put arrows, failing unless the click laid exactly that. The positions are absolute, since
     * {@code GameTestHelper#relativePos} turns even an unrotated test half round.
     */
    private static List<BlockPos> layAsPlanned(GameTestHelper helper, ListeningPlayer player, BlockPos floor) {
        PlacementPlan plan = plan(helper, player, floor);
        if (plan == null || plan.isRefused()) {
            helper.fail("the plan was " + plan, floor);
        }
        click(helper, player, floor, false);
        List<BlockPos> laid = new ArrayList<>();
        for (PlacementPlan.Placed placed : plan.blocks()) {
            BlockState there = helper.getLevel().getBlockState(placed.pos());
            if (!there.equals(placed.state())) {
                helper.fail("the plan put " + placed.state() + " at " + placed.pos() + ", the click laid " + there);
            }
            laid.add(placed.pos());
        }
        return laid;
    }

    private static void expectLaid(GameTestHelper helper, List<BlockPos> laid, List<BlockPos> expected) {
        Set<BlockPos> wanted = new LinkedHashSet<>(expected.stream().map(helper::absolutePos).toList());
        if (!new LinkedHashSet<>(laid).equals(wanted) || laid.size() != wanted.size()) {
            helper.fail("the stretch laid " + laid + ", expected " + wanted);
        }
        for (int x = 0; x < 9; x++) {
            for (int y = 1; y < 5; y++) {
                for (int z = 0; z < 9; z++) {
                    BlockPos pos = new BlockPos(x, y, z);
                    if (helper.getBlockState(pos).is(LineOfArrows.ARROW) && !wanted.contains(helper.absolutePos(pos))) {
                        helper.fail("a arrow was laid outside the plan", pos);
                    }
                }
            }
        }
    }

    private static void expectRefused(GameTestHelper helper, ListeningPlayer player, BlockPos floor, Refusal refusal) {
        PlacementPlan plan = plan(helper, player, floor);
        if (plan == null || plan.refusal() != refusal) {
            helper.fail("the plan was " + plan + ", not refused " + refusal, floor);
        }
    }

    private static void expectNothingLaidOrCharged(GameTestHelper helper, ListeningPlayer player, int held,
                                                   List<BlockPos> unlaid) {
        for (BlockPos pos : unlaid) {
            if (helper.getBlockState(pos).is(LineOfArrows.ARROW)) {
                helper.fail("a refused stretch laid a arrow", pos);
            }
        }
        expectHeld(helper, player, held);
        if (!player.getMainHandItem().has(Groundworks.STRETCH.get())) {
            helper.fail("a refused stretch dropped the stretch being drawn");
        }
    }

    private static void expectHeld(GameTestHelper helper, ListeningPlayer player, int held) {
        int count = player.getMainHandItem().getCount();
        if (count != held) {
            helper.fail("the player holds " + count + ", expected " + held);
        }
    }

    private static void expectReset(GameTestHelper helper, ListeningPlayer player) {
        ItemStack stack = player.getMainHandItem();
        if (stack.has(Groundworks.STRETCH.get()) || stack.has(Groundworks.HEIGHT.get())) {
            helper.fail("the stack kept " + stack.getComponentsPatch());
        }
    }

    /** Arrows from {@code fromX} to {@code toX} at height {@code y}, in row {@code z}. */
    private static List<BlockPos> line(int fromX, int toX, int y, int z) {
        List<BlockPos> line = new ArrayList<>();
        for (int x = fromX; x <= toX; x++) {
            line.add(new BlockPos(x, y, z));
        }
        return line;
    }

    private static void start(GameTestHelper helper, ListeningPlayer player) {
        click(helper, player, START, true);
    }

    private static @Nullable PlacementPlan plan(GameTestHelper helper, ListeningPlayer player, BlockPos pos) {
        return Placements.planFor(player.level(), player, InteractionHand.MAIN_HAND, player.getMainHandItem(), onTop(helper, pos));
    }

    private static void click(GameTestHelper helper, ListeningPlayer player, BlockPos pos, boolean sneaking) {
        player.setShiftKeyDown(sneaking);
        player.gameMode.useItemOn(player, helper.getLevel(), player.getMainHandItem(), InteractionHand.MAIN_HAND,
                onTop(helper, pos));
        player.setShiftKeyDown(false);
    }

    /** Presses Raise {@code height} times, or Lower as many times if it is negative. */
    private static void press(ListeningPlayer player, int height) {
        for (int press = 0; press < Math.abs(height); press++) {
            Raise.press(player, height < 0);
        }
    }

    private static ListeningPlayer holding(GameTestHelper helper, int count) {
        ListeningPlayer player = new ListeningPlayer(helper, new BlockPos(0, 1, 0));
        player.setYRot(Direction.EAST.toYRot());
        player.setYHeadRot(Direction.EAST.toYRot());
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(GroundworksGameTests.STRETCHES_ARROWS.get(), count));
        return player;
    }

    private static BlockHitResult onTop(GameTestHelper helper, BlockPos pos) {
        BlockPos absolute = helper.absolutePos(pos);
        return new BlockHitResult(Vec3.atCenterOf(absolute).relative(Direction.UP, 0.5), Direction.UP, absolute, false);
    }
}
