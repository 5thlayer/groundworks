// SPDX-FileCopyrightText: 2026 5thlayer
// SPDX-License-Identifier: MIT

package io.github._5thlayer.groundworks.gametest;

import java.util.List;
import java.util.stream.IntStream;

import io.github._5thlayer.groundworks.FastReplace;
import io.github._5thlayer.groundworks.Groundworks;
import io.github._5thlayer.groundworks.PlacementPlan;
import io.github._5thlayer.groundworks.Placements;
import io.github._5thlayer.groundworks.Raise;
import io.github._5thlayer.groundworks.Refusal;
import io.github._5thlayer.groundworks.Stretches;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.Identifier;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * A Stretch through blocks of the held item's own Replace group: it replaces each in place, at its
 * own height, and hands back what breaking it drops, as a Fast Replace does (5thlayer/groundworks#39).
 *
 * <p>The tests run with {@link #STRETCHES_THROUGH_GROUND} switched on: its tag {@code road} groups
 * dirt, coarse dirt, podzol and stone bricks, and stone bricks and iron blocks stretch. Podzol
 * drops dirt, so a hand-back is the replaced block's drops and not its item. The ground is a row of
 * it on the floor, along z 2 from x 1 to x 5, and each stretch is of stone bricks, started on the
 * row's west end looking east, the player standing off the row to the north-west. Every click goes
 * through the server player's own {@code useItemOn}, as in {@link VanillaStretchTests}.
 *
 * <p>Gold blocks are in {@link FastReplaceTests#stateGroups the tests' group} whose builder
 * replaces a column, so its replace spans blocks; quartz blocks and quartz bricks in one whose
 * builder {@linkplain ReplacesOneBlock replaces one block}.
 */
final class StretchReplaceTests {

    /** The pack under {@code gametest_packs/} that groups the road and makes its items stretch. */
    private static final String STRETCHES_THROUGH_GROUND = "stretches_through_ground";

    private static final BlockPos WEST_END = new BlockPos(1, 1, 2);
    private static final BlockPos EAST_END = new BlockPos(5, 1, 2);
    private static final int HELD = 16;

    private StretchReplaceTests() {
    }

    static void register(GroundworksGameTests.Registrar tests) {
        var road = tests.withPack(STRETCHES_THROUGH_GROUND);
        road.test("a_stretch_through_its_group_replaces_each_block_for_one_item_and_hands_back_its_drops", 20,
                StretchReplaceTests::replacesTheRow);
        road.test("a_stretch_leaves_a_block_of_its_group_one_above_its_height_untouched", 20,
                StretchReplaceTests::oneUpIsUntouched);
        road.test("a_stretch_goes_round_a_block_outside_its_group_at_its_height", 20,
                StretchReplaceTests::goesRoundOutsiders);
        road.test("a_stretch_ending_in_a_block_outside_its_group_is_refused_whole", 20,
                StretchReplaceTests::refusedByAnOutsider);
        road.test("a_start_aimed_at_a_block_of_the_group_lies_in_it", 20, StretchReplaceTests::startLiesIn);
        road.test("a_start_raised_once_over_the_group_lays_on_top_of_it", 20, StretchReplaceTests::raisedLaysOnTop);
        road.test("a_block_that_already_is_the_held_block_stays_as_it_stands_neither_charged_nor_handed_back", 20,
                StretchReplaceTests::sameBlockStays);
        road.test("a_plant_on_a_replaced_block_goes_with_no_drop_and_one_at_the_height_is_overwritten", 20,
                StretchReplaceTests::plants);
        road.test("sugar_cane_on_a_replaced_block_goes_whole_with_no_drop_even_after_its_tick", 20,
                StretchReplaceTests::sugarCane);
        road.test("a_stretch_whose_hand_back_has_no_room_is_refused_whole_and_changes_nothing", 20,
                StretchReplaceTests::noRoom);
        road.test("a_creative_stretch_through_its_group_changes_no_inventory", 20, StretchReplaceTests::creative);
        road.test("a_group_whose_replace_spans_blocks_is_an_obstacle_to_a_stretch", 20,
                StretchReplaceTests::builderGroupIsAnObstacle);
        road.test("a_stretch_goes_through_a_group_whose_builder_replaces_one_block_and_hands_back_drops_not_its_refund", 20,
                StretchReplaceTests::oneBlockBuilderGroup);
        road.test("a_column_through_its_group_replaces_each_block", 20, StretchReplaceTests::column);
        road.test("a_consumer_asks_a_block_s_replace_group_and_whether_a_stretch_goes_through_it", 20,
                StretchReplaceTests::groupQuery);
    }

    /** Podzol and coarse dirt from end to end: five stone bricks in their place, five items charged, five drops back. */
    private static void replacesTheRow(GameTestHelper helper) {
        layRow(helper, Blocks.PODZOL);
        helper.setBlock(new BlockPos(4, 1, 2), Blocks.COARSE_DIRT);
        ListeningPlayer player = holding(helper, Items.STONE_BRICKS);
        start(helper, player, WEST_END);
        PlacementPlan plan = plan(helper, player, EAST_END);
        List<BlockPos> row = row(helper);
        if (plan == null || plan.isRefused() || !sorted(plan.replaces()).equals(row)
                || !sorted(plan.blocks().stream().map(PlacementPlan.Placed::pos).toList()).equals(row)) {
            helper.fail("the plan was " + plan + ", not a replace of the row " + row);
        }
        click(helper, player, EAST_END, false);
        expectRow(helper, Blocks.STONE_BRICKS);
        expectHeld(helper, player, HELD - 5);
        expectCount(helper, player, Items.DIRT, 4);
        expectCount(helper, player, Items.COARSE_DIRT, 1);
        expectCount(helper, player, Items.PODZOL, 0);
        helper.succeed();
    }

    /** Started on the floor, the stretch runs a block above it, under a podzol one up, which it leaves. */
    private static void oneUpIsUntouched(GameTestHelper helper) {
        BlockPos above = new BlockPos(3, 2, 2);
        helper.setBlock(above, Blocks.PODZOL);
        ListeningPlayer player = holding(helper, Items.STONE_BRICKS);
        start(helper, player, new BlockPos(1, 0, 2));
        click(helper, player, new BlockPos(5, 0, 2), false);
        expectRow(helper, Blocks.STONE_BRICKS);
        expectBlock(helper, above, Blocks.PODZOL);
        expectHeld(helper, player, HELD - 5);
        expectCount(helper, player, Items.DIRT, 0);
        helper.succeed();
    }

    /** A cobblestone in the podzol row is in no group: the stretch detours round it, which stands. */
    private static void goesRoundOutsiders(GameTestHelper helper) {
        layRow(helper, Blocks.PODZOL);
        BlockPos outsider = new BlockPos(3, 1, 2);
        helper.setBlock(outsider, Blocks.COBBLESTONE);
        ListeningPlayer player = holding(helper, Items.STONE_BRICKS);
        start(helper, player, WEST_END);
        PlacementPlan plan = plan(helper, player, EAST_END);
        if (plan == null || plan.isRefused()
                || plan.blocks().stream().anyMatch(placed -> placed.pos().equals(helper.absolutePos(outsider)))) {
            helper.fail("the plan was " + plan + ", not a detour round the cobblestone");
        }
        click(helper, player, EAST_END, false);
        expectBlock(helper, outsider, Blocks.COBBLESTONE);
        expectBlock(helper, WEST_END, Blocks.STONE_BRICKS);
        expectBlock(helper, EAST_END, Blocks.STONE_BRICKS);
        helper.succeed();
    }

    /** The row's east end is cobblestone, in no group: no detour reaches an end in it, so the stretch is refused and lays nothing. */
    private static void refusedByAnOutsider(GameTestHelper helper) {
        layRow(helper, Blocks.PODZOL);
        helper.setBlock(EAST_END, Blocks.COBBLESTONE);
        ListeningPlayer player = holding(helper, Items.STONE_BRICKS);
        start(helper, player, WEST_END);
        PlacementPlan plan = plan(helper, player, EAST_END);
        if (plan == null || !(plan.refusal() instanceof Refusal.At at) || !at.pos().equals(helper.absolutePos(EAST_END))) {
            helper.fail("the plan was " + plan + ", not refused at the cobblestone");
        }
        click(helper, player, EAST_END, false);
        for (int x = 1; x <= 4; x++) {
            expectBlock(helper, new BlockPos(x, 1, 2), Blocks.PODZOL);
        }
        expectBlock(helper, EAST_END, Blocks.COBBLESTONE);
        expectHeld(helper, player, HELD);
        expectCount(helper, player, Items.DIRT, 0);
        helper.succeed();
    }

    /** A sneak-click on the top of the row's west end stores the start in the podzol, not on it. */
    private static void startLiesIn(GameTestHelper helper) {
        layRow(helper, Blocks.PODZOL);
        ListeningPlayer player = holding(helper, Items.STONE_BRICKS);
        start(helper, player, WEST_END);
        var stored = Stretches.storedOn(player.level(), player.getMainHandItem());
        if (stored == null || !stored.start().equals(helper.absolutePos(WEST_END))) {
            helper.fail("the start was stored at " + (stored == null ? null : stored.start()) + ", not in the podzol",
                    WEST_END);
        }
        helper.succeed();
    }

    /** Raised once before the start is stored, the stretch lies on the podzol, which stays. */
    private static void raisedLaysOnTop(GameTestHelper helper) {
        layRow(helper, Blocks.PODZOL);
        ListeningPlayer player = holding(helper, Items.STONE_BRICKS);
        Raise.press(player, false);
        start(helper, player, WEST_END);
        click(helper, player, EAST_END, false);
        expectRow(helper, Blocks.PODZOL);
        for (BlockPos pos : row(helper)) {
            if (!helper.getLevel().getBlockState(pos.above()).is(Blocks.STONE_BRICKS)) {
                helper.fail("no stone bricks laid on top of the podzol", helper.relativePos(pos.above()));
            }
        }
        expectHeld(helper, player, HELD - 5);
        expectCount(helper, player, Items.DIRT, 0);
        helper.succeed();
    }

    /** Stone bricks already standing at x 3: the plan leaves them out, so four are laid, charged and handed back for. */
    private static void sameBlockStays(GameTestHelper helper) {
        layRow(helper, Blocks.PODZOL);
        BlockPos standing = new BlockPos(3, 1, 2);
        helper.setBlock(standing, Blocks.STONE_BRICKS);
        ListeningPlayer player = holding(helper, Items.STONE_BRICKS);
        start(helper, player, WEST_END);
        PlacementPlan plan = plan(helper, player, EAST_END);
        if (plan == null || plan.isRefused()
                || plan.blocks().stream().anyMatch(placed -> placed.pos().equals(helper.absolutePos(standing)))) {
            helper.fail("the plan was " + plan + ", drawing the stone bricks already standing");
        }
        click(helper, player, EAST_END, false);
        expectRow(helper, Blocks.STONE_BRICKS);
        expectHeld(helper, player, HELD - 4);
        expectCount(helper, player, Items.DIRT, 4);
        helper.succeed();
    }

    /**
     * A poppy on the podzol at x 2 has nothing to stand on once it is stone bricks, and goes, leaving
     * no item; a snow layer on the floor at x 4, at the stretch's height, is overwritten, drawn as no
     * replace and handing nothing back.
     */
    private static void plants(GameTestHelper helper) {
        layRow(helper, Blocks.PODZOL);
        BlockPos poppy = new BlockPos(2, 2, 2);
        helper.setBlock(poppy, Blocks.POPPY);
        BlockPos snow = new BlockPos(4, 1, 2);
        helper.setBlock(snow, Blocks.SNOW);
        ListeningPlayer player = holding(helper, Items.STONE_BRICKS);
        start(helper, player, WEST_END);
        PlacementPlan plan = plan(helper, player, EAST_END);
        if (plan == null || plan.isRefused() || plan.replaces().contains(helper.absolutePos(snow))
                || plan.blocks().stream().noneMatch(placed -> placed.pos().equals(helper.absolutePos(snow)))) {
            helper.fail("the plan was " + plan + ", not laying over the snow layer as no replace");
        }
        click(helper, player, EAST_END, false);
        expectRow(helper, Blocks.STONE_BRICKS);
        if (!helper.getBlockState(poppy).isAir()) {
            helper.fail("the poppy still stands, as " + helper.getBlockState(poppy), poppy);
        }
        expectNoItems(helper);
        expectHeld(helper, player, HELD - 5);
        expectCount(helper, player, Items.DIRT, 4);
        expectCount(helper, player, Items.POPPY, 0);
        expectCount(helper, player, Items.SNOWBALL, 0);
        helper.succeed();
    }

    /**
     * Sugar cane two high on the podzol at x 3, watered from the side, breaks on its tick once the
     * podzol is stone bricks, not as the block goes down: it is cleared with the stretch, and no
     * item lies in the world ticks later.
     */
    private static void sugarCane(GameTestHelper helper) {
        layRow(helper, Blocks.PODZOL);
        helper.setBlock(new BlockPos(3, 1, 1), Blocks.WATER);
        BlockPos cane = new BlockPos(3, 2, 2);
        helper.setBlock(cane, Blocks.SUGAR_CANE);
        helper.setBlock(cane.above(), Blocks.SUGAR_CANE);
        ListeningPlayer player = holding(helper, Items.STONE_BRICKS);
        start(helper, player, WEST_END);
        click(helper, player, EAST_END, false);
        expectRow(helper, Blocks.STONE_BRICKS);
        helper.runAfterDelay(5, () -> {
            for (BlockPos pos : List.of(cane, cane.above())) {
                if (!helper.getBlockState(pos).isAir()) {
                    helper.fail("the sugar cane still stands, as " + helper.getBlockState(pos), pos);
                }
            }
            expectNoItems(helper);
            expectCount(helper, player, Items.SUGAR_CANE, 0);
            helper.succeed();
        });
    }

    /** An inventory full of dirt takes back none of the podzol's: the plan and the click are refused, and nothing changes. */
    private static void noRoom(GameTestHelper helper) {
        layRow(helper, Blocks.PODZOL);
        ListeningPlayer player = holding(helper, Items.STONE_BRICKS);
        player.fillInventory();
        start(helper, player, WEST_END);
        PlacementPlan plan = plan(helper, player, EAST_END);
        if (plan == null || plan.refusal() != Refusal.Stretch.NO_ROOM_TO_RETURN) {
            helper.fail("the plan was " + plan + ", not refused for no room to hand back");
        }
        click(helper, player, EAST_END, false);
        expectRow(helper, Blocks.PODZOL);
        expectHeld(helper, player, HELD);
        if (!player.heard.contains("message.groundworks.stretch_no_room_to_return")) {
            helper.fail("the player was told " + player.heard + ", not that there is no room");
        }
        helper.succeed();
    }

    /** A creative player with a full inventory: the row is laid, the held stack is not charged, and nothing is handed back. */
    private static void creative(GameTestHelper helper) {
        layRow(helper, Blocks.PODZOL);
        ListeningPlayer player = holding(helper, Items.STONE_BRICKS);
        player.setGameMode(GameType.CREATIVE);
        player.fillInventory();
        int dirt = player.getInventory().countItem(Items.DIRT);
        start(helper, player, WEST_END);
        click(helper, player, EAST_END, false);
        expectRow(helper, Blocks.STONE_BRICKS);
        expectHeld(helper, player, HELD);
        expectCount(helper, player, Items.DIRT, dirt);
        helper.succeed();
    }

    /** Gold blocks are in a group whose builder replaces a column: an iron stretch goes round the one in its way. */
    private static void builderGroupIsAnObstacle(GameTestHelper helper) {
        BlockPos gold = new BlockPos(3, 1, 2);
        helper.setBlock(gold, Blocks.GOLD_BLOCK);
        ListeningPlayer player = holding(helper, Items.IRON_BLOCK);
        start(helper, player, new BlockPos(1, 0, 2));
        PlacementPlan plan = plan(helper, player, new BlockPos(5, 0, 2));
        if (plan == null || plan.isReplace()
                || plan.blocks().stream().anyMatch(placed -> placed.pos().equals(helper.absolutePos(gold)))) {
            helper.fail("the plan was " + plan + ", going through the gold block");
        }
        click(helper, player, new BlockPos(5, 0, 2), false);
        expectBlock(helper, gold, Blocks.GOLD_BLOCK);
        expectCount(helper, player, Items.GOLD_BLOCK, 0);
        helper.succeed();
    }

    /**
     * Quartz blocks are in a group whose builder replaces one block and says so: a stretch of
     * quartz bricks starts in the row and replaces it whole, laying its own blocks, handing back
     * each quartz block it drops and never the builder's stick.
     */
    private static void oneBlockBuilderGroup(GameTestHelper helper) {
        layRow(helper, Blocks.QUARTZ_BLOCK);
        ListeningPlayer player = holding(helper, Items.QUARTZ_BRICKS);
        start(helper, player, WEST_END);
        var stored = Stretches.storedOn(player.level(), player.getMainHandItem());
        if (stored == null || !stored.start().equals(helper.absolutePos(WEST_END))) {
            helper.fail("the start was stored at " + (stored == null ? null : stored.start()) + ", not in the quartz", WEST_END);
        }
        PlacementPlan plan = plan(helper, player, EAST_END);
        if (plan == null || plan.isRefused() || !sorted(plan.replaces()).equals(row(helper))) {
            helper.fail("the plan was " + plan + ", not a replace of the quartz row");
        }
        click(helper, player, EAST_END, false);
        expectRow(helper, Blocks.QUARTZ_BRICKS);
        expectHeld(helper, player, HELD - 5);
        expectCount(helper, player, Items.QUARTZ_BLOCK, 5);
        expectCount(helper, player, Items.STICK, 0);
        helper.succeed();
    }

    /** A column of three podzol, a start in its foot, two presses of Raise: three stone bricks, three dirt back. */
    private static void column(GameTestHelper helper) {
        for (int y = 1; y <= 3; y++) {
            helper.setBlock(new BlockPos(1, y, 2), Blocks.PODZOL);
        }
        ListeningPlayer player = holding(helper, Items.STONE_BRICKS);
        start(helper, player, WEST_END);
        Raise.press(player, false);
        Raise.press(player, false);
        PlacementPlan plan = plan(helper, player, WEST_END);
        if (plan == null || plan.isRefused() || plan.replaces().size() != 3) {
            helper.fail("the plan was " + plan + ", not a replace of the column's three podzol");
        }
        click(helper, player, WEST_END, false);
        for (int y = 1; y <= 3; y++) {
            expectBlock(helper, new BlockPos(1, y, 2), Blocks.STONE_BRICKS);
        }
        expectHeld(helper, player, HELD - 3);
        expectCount(helper, player, Items.DIRT, 3);
        helper.succeed();
    }

    /** The public query a Consumer's leg builder asks: the vanilla tag's group, a stated group's builder, and none. */
    private static void groupQuery(GameTestHelper helper) {
        FastReplace.Group road = FastReplace.groupOf(Blocks.PODZOL);
        if (road == null || !road.id().equals(Identifier.fromNamespaceAndPath(Groundworks.MOD_ID, "replace_group/road"))
                || road.spansBlocks()) {
            helper.fail("podzol's group was " + road + ", not the road tag's");
        }
        FastReplace.Group oneBlock = FastReplace.groupOf(Blocks.QUARTZ_BLOCK);
        if (oneBlock == null || oneBlock.builder() == null || oneBlock.spansBlocks()
                || !FastReplace.stretchesThrough(Blocks.QUARTZ_BRICKS, Blocks.QUARTZ_BLOCK)) {
            helper.fail("the quartz block's group was " + oneBlock + ", not one whose builder replaces one block");
        }
        FastReplace.Group column = FastReplace.groupOf(Blocks.GOLD_BLOCK);
        if (column == null || !column.spansBlocks()) {
            helper.fail("the gold block's group was " + column + ", not one whose replace spans blocks");
        }
        if (FastReplace.groupOf(Blocks.COBBLESTONE) != null) {
            helper.fail("cobblestone is in " + FastReplace.groupOf(Blocks.COBBLESTONE));
        }
        if (!FastReplace.stretchesThrough(Blocks.STONE_BRICKS, Blocks.PODZOL)
                || !FastReplace.stretchesThrough(Blocks.STONE_BRICKS, Blocks.STONE_BRICKS)) {
            helper.fail("a stone-brick stretch doesn't go through podzol or stone bricks");
        }
        if (FastReplace.stretchesThrough(Blocks.IRON_BLOCK, Blocks.GOLD_BLOCK)
                || FastReplace.stretchesThrough(Blocks.STONE_BRICKS, Blocks.COBBLESTONE)) {
            helper.fail("a stretch goes through a block whose group's replace spans blocks, or one outside its group");
        }
        helper.succeed();
    }

    private static void expectNoItems(GameTestHelper helper) {
        List<ItemEntity> items = helper.getLevel().getEntitiesOfClass(ItemEntity.class,
                new AABB(helper.absolutePos(new BlockPos(0, 0, 0)).getCenter(), helper.absolutePos(new BlockPos(7, 4, 7)).getCenter()).inflate(2));
        if (!items.isEmpty()) {
            helper.fail("the stretch left " + items.stream().map(ItemEntity::getItem).toList() + " in the world");
        }
    }

    private static void layRow(GameTestHelper helper, Block block) {
        for (int x = 1; x <= 5; x++) {
            helper.setBlock(new BlockPos(x, 1, 2), block);
        }
    }

    /** The row's positions, absolute and sorted. */
    private static List<BlockPos> row(GameTestHelper helper) {
        return sorted(IntStream.rangeClosed(1, 5).mapToObj(x -> helper.absolutePos(new BlockPos(x, 1, 2))).toList());
    }

    private static List<BlockPos> sorted(List<BlockPos> positions) {
        return positions.stream().sorted().toList();
    }

    private static void expectRow(GameTestHelper helper, Block block) {
        for (int x = 1; x <= 5; x++) {
            expectBlock(helper, new BlockPos(x, 1, 2), block);
        }
    }

    private static void expectBlock(GameTestHelper helper, BlockPos pos, Block block) {
        BlockState there = helper.getBlockState(pos);
        if (!there.is(block)) {
            helper.fail("expected " + block + ", found " + there, pos);
        }
    }

    private static void expectHeld(GameTestHelper helper, ListeningPlayer player, int held) {
        int count = player.getMainHandItem().getCount();
        if (count != held) {
            helper.fail("the player holds " + count + ", expected " + held);
        }
    }

    private static void expectCount(GameTestHelper helper, ListeningPlayer player, Item item, int count) {
        int there = player.getInventory().countItem(item);
        if (there != count) {
            helper.fail("the inventory holds " + there + " " + item + ", expected " + count);
        }
    }

    private static void start(GameTestHelper helper, ListeningPlayer player, BlockPos aimed) {
        click(helper, player, aimed, true);
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

    private static ListeningPlayer holding(GameTestHelper helper, Item item) {
        ListeningPlayer player = new ListeningPlayer(helper, new BlockPos(0, 1, 0));
        player.setYRot(Direction.EAST.toYRot());
        player.setYHeadRot(Direction.EAST.toYRot());
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(item, HELD));
        return player;
    }

    private static BlockHitResult onTop(GameTestHelper helper, BlockPos pos) {
        BlockPos absolute = helper.absolutePos(pos);
        return new BlockHitResult(Vec3.atCenterOf(absolute).relative(Direction.UP, 0.5), Direction.UP, absolute, false);
    }
}
