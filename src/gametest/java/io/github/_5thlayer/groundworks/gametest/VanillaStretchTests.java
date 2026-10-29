// SPDX-FileCopyrightText: 2026 5thlayer
// SPDX-License-Identifier: MIT

package io.github._5thlayer.groundworks.gametest;

import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;

import io.github._5thlayer.groundworks.Groundworks;
import io.github._5thlayer.groundworks.PlacementPlan;
import io.github._5thlayer.groundworks.Placements;
import io.github._5thlayer.groundworks.Raise;
import io.github._5thlayer.groundworks.Refusal;
import io.github._5thlayer.groundworks.Stretches;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ChainBlock;
import net.minecraft.world.level.block.LadderBlock;
import net.minecraft.world.level.block.RailBlock;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.SnowLayerBlock;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.TrapDoorBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Half;
import net.minecraft.world.level.block.state.properties.RailShape;
import net.minecraft.world.level.block.state.properties.SlabType;
import net.minecraft.world.level.block.state.properties.StairsShape;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * The vanilla Consumer's Stretch: an item in {@code groundworks:stretches} is laid at each position
 * of the route through vanilla placement, a rise included. Stairs and rails are the fixtures, since
 * the shipped tag holds them through {@code #minecraft:stairs} and {@code #minecraft:rails}; no
 * test states their builder in code.
 *
 * <p>Each stretch starts on the floor at {@link #START}, looking east, and so lays from one block
 * above it. Every click goes through the server player's own {@code useItemOn}, and every press
 * through {@link Raise#press}, as in {@link StretchTests}.
 */
final class VanillaStretchTests {

    private static final BlockPos START = new BlockPos(1, 0, 2);

    /** The floor block {@link #layEastOver} aims the stretch's end at, east of {@link #START}. */
    private static final BlockPos END_EAST = new BlockPos(5, 0, 2);

    /** What a stretch from {@link #START} to {@link #END_EAST} lays: one block above the floor, all the way. */
    private static final List<BlockPos> LINE_EAST = IntStream.rangeClosed(START.getX(), END_EAST.getX())
            .mapToObj(x -> new BlockPos(x, START.getY() + 1, START.getZ())).toList();

    /** Where {@link #layEastOver} sets the held block first, between the line's ends. */
    private static final List<BlockPos> STANDING_EAST = List.of(new BlockPos(2, 1, 2), new BlockPos(4, 1, 2));

    /** Stretches snow layers and oak trapdoors, which the shipped tag doesn't hold. */
    private static final String STRETCHES_SNOW_AND_TRAPDOORS = "stretches_snow_and_trapdoors";

    private VanillaStretchTests() {
    }

    static void register(GroundworksGameTests.Registrar tests) {
        tests.test("stairs_stretched_over_flat_ground_lay_a_line_joined_as_vanilla_joins_it", 20,
                VanillaStretchTests::flatStairs);
        tests.test("stairs_stretched_over_a_rise_lay_a_staircase_facing_up_the_leg", 20,
                VanillaStretchTests::risingStairs);
        tests.test("stairs_stretched_down_a_fall_lay_a_staircase_facing_back_up_the_leg", 20,
                VanillaStretchTests::fallingStairs);
        tests.test("stairs_stretched_over_a_rise_past_the_players_reach_climb_it", 20,
                VanillaStretchTests::risingStairsPastTheReach);
        tests.test("a_raise_past_the_build_height_while_a_stretch_is_drawn_is_refused_and_told", 20,
                VanillaStretchTests::refusedPastTheBuildHeight);
        tests.test("fences_stretched_round_a_corner_to_a_standing_fence_are_planned_joined_as_they_are_laid", 20,
                helper -> plannedAsLaid(helper, Blocks.OAK_FENCE));
        tests.test("walls_stretched_round_a_corner_to_a_standing_wall_are_planned_joined_as_they_are_laid", 20,
                helper -> plannedAsLaid(helper, Blocks.COBBLESTONE_WALL));
        tests.test("stretch_of_slabs_over_single_slabs_of_its_kind_makes_them_double_slabs", 20,
                VanillaStretchTests::slabsOverSlabs);
        var withPack = tests.withPack(STRETCHES_SNOW_AND_TRAPDOORS);
        withPack.test("stretch_of_snow_layers_over_snow_layers_adds_a_layer_to_each", 20,
                VanillaStretchTests::snowOverSnow);
        withPack.test("stretch_of_trapdoors_in_a_line_hangs_them_in_their_bottom_half_on_the_block_ahead", 20,
                VanillaStretchTests::trapdoorsInALine);
        tests.test("rails_stretched_over_a_rise_slope_up_it", 20, VanillaStretchTests::risingRails);
        tests.test("rails_stretched_over_a_rise_with_nothing_under_it_are_refused_where_vanilla_refuses", 20,
                VanillaStretchTests::railsInMidAir);
        tests.test("ladders_stretched_as_a_column_climb_the_wall_by_the_raised_height", 20,
                VanillaStretchTests::ladderColumn);
        tests.test("a_ladder_column_with_a_gap_in_its_wall_is_refused_whole", 20,
                VanillaStretchTests::ladderColumnWithAGap);
        tests.test("chains_stretched_as_a_column_stand_upright_past_the_players_reach", 20,
                VanillaStretchTests::chainColumn);
        tests.test("a_stretch_tells_each_block_it_lays_it_was_placed_with_the_held_item", 20,
                VanillaStretchTests::tellsEachBlockPlaced);
        tests.test("a_column_over_a_claimed_position_is_refused_whole_on_the_click", 20,
                VanillaStretchTests::claimedColumn);
        tests.test("a_stone_block_does_not_stretch", 20, helper -> {
            if (Stretches.builderOf(Blocks.STONE.asItem()) != null) {
                helper.fail("stone stretches, though the shipped tag doesn't hold it");
            }
            helper.succeed();
        });
        tests.withPack(PlanOptInTests.REMOVES_STAIRS).test("a_datapack_taking_stairs_out_of_the_tag_stops_them_stretching",
                20, VanillaStretchTests::takenOutOfTheTag);
    }

    /** East four, then south round a corner: every stair faces the way it runs, and the one before the corner turns. */
    private static void flatStairs(GameTestHelper helper) {
        ListeningPlayer player = holding(helper, Blocks.OAK_STAIRS, 16);
        start(helper, player);
        BlockPos end = new BlockPos(5, 0, 4);
        List<BlockPos> planned = plannedPositions(helper, player, end);
        click(helper, player, end, false);
        Map<BlockPos, Direction> expected = Map.of(
                new BlockPos(1, 1, 2), Direction.EAST, new BlockPos(2, 1, 2), Direction.EAST,
                new BlockPos(3, 1, 2), Direction.EAST, new BlockPos(4, 1, 2), Direction.EAST,
                new BlockPos(5, 1, 2), Direction.SOUTH, new BlockPos(5, 1, 3), Direction.SOUTH,
                new BlockPos(5, 1, 4), Direction.SOUTH);
        expectPlannedWhereLaid(helper, planned, expected.keySet().stream().toList());
        expected.forEach((pos, facing) -> expectStair(helper, pos, facing));
        if (helper.getBlockState(new BlockPos(4, 1, 2)).getValue(StairBlock.SHAPE) == StairsShape.STRAIGHT) {
            helper.fail("the stair before the corner stayed straight, not joined to the corner",
                    new BlockPos(4, 1, 2));
        }
        expectHeld(helper, player, 16 - expected.size());
        helper.succeed();
    }

    /**
     * East four, then south round a corner, to one already standing beyond the end: each planned
     * block is drawn as it stands once the stretch is laid, joined to the next and to the standing
     * one, which the plan leaves alone.
     */
    private static void plannedAsLaid(GameTestHelper helper, Block block) {
        BlockPos standing = new BlockPos(5, 1, 5);
        helper.setBlock(standing, block);
        BlockState before = helper.getBlockState(standing);
        ListeningPlayer player = holding(helper, block, 16);
        start(helper, player);
        BlockPos end = new BlockPos(5, 0, 4);
        PlacementPlan plan = plan(helper, player, end);
        if (plan == null || plan.isRefused()) {
            helper.fail("the plan was " + plan, end);
        }
        if (plan.blocks().stream().anyMatch(placed -> placed.pos().equals(helper.absolutePos(standing)))) {
            helper.fail("the plan redrew the block already standing", standing);
        }
        click(helper, player, end, false);
        for (PlacementPlan.Placed placed : plan.blocks()) {
            BlockState laid = helper.getLevel().getBlockState(placed.pos());
            if (!laid.equals(placed.state())) {
                helper.fail("planned " + placed.state() + ", laid " + laid, helper.relativePos(placed.pos()));
            }
        }
        if (helper.getBlockState(standing).equals(before)) {
            helper.fail("the stretch's end didn't join the block already standing", standing);
        }
        helper.succeed();
    }

    /** Two bottom slabs of the held kind: each merges with the one laid there into a double slab, as a player's would. */
    private static void slabsOverSlabs(GameTestHelper helper) {
        layEastOver(helper, Blocks.OAK_SLAB, STANDING_EAST);
        LINE_EAST.forEach(pos -> expectSlab(helper, pos, STANDING_EAST.contains(pos) ? SlabType.DOUBLE : SlabType.BOTTOM));
        helper.succeed();
    }

    /** Two single snow layers: each gains a layer, and the rest lie as one. */
    private static void snowOverSnow(GameTestHelper helper) {
        layEastOver(helper, Blocks.SNOW, STANDING_EAST);
        LINE_EAST.forEach(pos -> expectSnow(helper, pos, STANDING_EAST.contains(pos) ? 2 : 1));
        helper.succeed();
    }

    /** Each trapdoor hangs on the block ahead of it, in its bottom half, as a player clicking that face places it. */
    private static void trapdoorsInALine(GameTestHelper helper) {
        layEastOver(helper, Blocks.OAK_TRAPDOOR, List.of());
        LINE_EAST.forEach(pos -> expectTrapdoor(helper, pos, Direction.WEST, Half.BOTTOM));
        helper.succeed();
    }

    /**
     * Sets {@code block} at each of {@code standing}, then stretches it from the start to {@link #END_EAST}:
     * the plan covers {@link #LINE_EAST}, and each of its positions is charged one held item.
     */
    private static void layEastOver(GameTestHelper helper, Block block, List<BlockPos> standing) {
        standing.forEach(pos -> helper.setBlock(pos, block));
        ListeningPlayer player = holding(helper, block, 16);
        start(helper, player);
        expectPlannedWhereLaid(helper, plannedPositions(helper, player, END_EAST), LINE_EAST);
        click(helper, player, END_EAST, false);
        expectHeld(helper, player, 16 - LINE_EAST.size());
    }

    /** A wall east of the start's column, three presses of Raise, and a click: four ladders. */
    private static void ladderColumn(GameTestHelper helper) {
        for (int y = 1; y <= 4; y++) {
            helper.setBlock(new BlockPos(2, y, 2), Blocks.STONE);
        }
        ListeningPlayer player = holding(helper, Blocks.LADDER, 16);
        start(helper, player);
        press(player, 3);
        click(helper, player, new BlockPos(3, 0, 5), true);
        if (Stretches.storedOn(player.level(), player.getMainHandItem()).anchors().size() != 0) {
            helper.fail("a sneak-click added an anchor to a column");
        }
        click(helper, player, START, false);
        for (int y = 1; y <= 4; y++) {
            BlockState laid = helper.getBlockState(new BlockPos(1, y, 2));
            if (!laid.is(Blocks.LADDER) || laid.getValue(LadderBlock.FACING) != Direction.WEST) {
                helper.fail("expected a ladder facing west, found " + laid, new BlockPos(1, y, 2));
            }
        }
        expectHeld(helper, player, 12);
        helper.succeed();
    }

    private static void ladderColumnWithAGap(GameTestHelper helper) {
        helper.setBlock(new BlockPos(2, 1, 2), Blocks.STONE);
        helper.setBlock(new BlockPos(2, 3, 2), Blocks.STONE);
        ListeningPlayer player = holding(helper, Blocks.LADDER, 16);
        start(helper, player);
        press(player, 2);
        PlacementPlan plan = plan(helper, player, START);
        if (plan == null || !(plan.refusal() instanceof Refusal.At at) || !at.pos().equals(helper.absolutePos(new BlockPos(1, 2, 2)))) {
            helper.fail("expected the column refused at the gap, the plan was " + plan);
        }
        click(helper, player, START, false);
        helper.assertBlockNotPresent(Blocks.LADDER, new BlockPos(1, 1, 2));
        expectHeld(helper, player, 16);
        helper.succeed();
    }

    /** A reach of two, and three presses of Raise past it, which caps no column's length: four upright chains. */
    private static void chainColumn(GameTestHelper helper) {
        ListeningPlayer player = holding(helper, Blocks.IRON_CHAIN, 16);
        player.getAttribute(Attributes.BLOCK_INTERACTION_RANGE).setBaseValue(2.0);
        start(helper, player);
        press(player, 3);
        click(helper, player, START, false);
        for (int y = 1; y <= 4; y++) {
            BlockState laid = helper.getBlockState(new BlockPos(1, y, 2));
            if (!laid.is(Blocks.IRON_CHAIN) || laid.getValue(ChainBlock.AXIS) != Direction.Axis.Y) {
                helper.fail("expected an upright chain, found " + laid, new BlockPos(1, y, 2));
            }
        }
        expectHeld(helper, player, 12);
        helper.succeed();
    }

    /** Every block a stretch lays is told it was placed by the player, with the held item, as vanilla tells one. */
    private static void tellsEachBlockPlaced(GameTestHelper helper) {
        ListeningPlayer player = holding(helper, GroundworksGameTests.TELLS_PLACED.get(), 16);
        start(helper, player);
        click(helper, player, new BlockPos(5, 0, 2), false);
        for (int x = 1; x <= 5; x++) {
            BlockPos pos = new BlockPos(x, 1, 2);
            helper.assertBlockPresent(GroundworksGameTests.TELLS_PLACED.get(), pos);
            Item told = TellsPlacedBlock.TOLD.remove(helper.absolutePos(pos));
            if (told != GroundworksGameTests.TELLS_PLACED_ITEM.get()) {
                helper.fail("the block laid was told it was placed with " + told, pos);
            }
        }
        helper.succeed();
    }

    /**
     * A claim mod cancels the place event at one position up the Column. The preview fires no event,
     * so it draws the Column accepted; the click asks each position and lays nothing.
     */
    private static void claimedColumn(GameTestHelper helper) {
        ListeningPlayer player = holding(helper, Blocks.IRON_CHAIN, 16);
        start(helper, player);
        press(player, 3);
        BlockPos claimed = helper.absolutePos(new BlockPos(1, 3, 2));
        RotateInPlaceTests.CLAIMED.add(claimed);
        try {
            PlacementPlan plan = plan(helper, player, START);
            if (plan == null || plan.isRefused()) {
                helper.fail("the plan was " + plan + ", not drawn accepted");
            }
            click(helper, player, START, false);
        } finally {
            RotateInPlaceTests.CLAIMED.remove(claimed);
        }
        for (int y = 1; y <= 4; y++) {
            helper.assertBlockNotPresent(Blocks.IRON_CHAIN, new BlockPos(1, y, 2));
        }
        expectHeld(helper, player, 16);
        if (!player.heard.getLast().equals("message.groundworks.stretch_may_not_build")) {
            helper.fail("the player was told " + player.heard);
        }
        helper.succeed();
    }

    private static void risingStairs(GameTestHelper helper) {
        ListeningPlayer player = holding(helper, Blocks.OAK_STAIRS, 16);
        start(helper, player);
        press(player, 2);
        click(helper, player, new BlockPos(5, 0, 2), false);
        for (BlockPos pos : List.of(new BlockPos(1, 1, 2), new BlockPos(2, 2, 2), new BlockPos(3, 3, 2),
                new BlockPos(4, 3, 2), new BlockPos(5, 3, 2))) {
            expectStair(helper, pos, Direction.EAST);
        }
        helper.succeed();
    }

    /** A reach of two, and a rise of three past it, which caps no leg's rise: the staircase climbs all three. */
    private static void risingStairsPastTheReach(GameTestHelper helper) {
        ListeningPlayer player = holding(helper, Blocks.OAK_STAIRS, 16);
        player.getAttribute(Attributes.BLOCK_INTERACTION_RANGE).setBaseValue(2.0);
        start(helper, player);
        press(player, 3);
        click(helper, player, new BlockPos(5, 0, 2), false);
        for (BlockPos pos : List.of(new BlockPos(1, 1, 2), new BlockPos(2, 2, 2), new BlockPos(3, 3, 2),
                new BlockPos(4, 4, 2), new BlockPos(5, 4, 2))) {
            expectStair(helper, pos, Direction.EAST);
        }
        expectHeld(helper, player, 11);
        helper.succeed();
    }

    /** With a start stored, Raise steps up to the world's height and no further, and says so. */
    private static void refusedPastTheBuildHeight(GameTestHelper helper) {
        ListeningPlayer player = holding(helper, Blocks.OAK_STAIRS, 16);
        start(helper, player);
        int cap = helper.getLevel().getHeight();
        press(player, cap + 1);
        int height = Raise.heightOf(player, player.getMainHandItem()).blocks();
        if (height != cap || !player.heard.getLast().equals("message.groundworks.height_at_build_limit")) {
            helper.fail(cap + 1 + " presses with a build height of " + cap + " raised " + height
                    + " and told " + player.heard);
        }
        helper.succeed();
    }

    /** Started 2 up, then lowered 2: the fall's stairs face west, back up it, and the level run after it east. */
    private static void fallingStairs(GameTestHelper helper) {
        ListeningPlayer player = holding(helper, Blocks.OAK_STAIRS, 16);
        press(player, 2);
        start(helper, player);
        press(player, -2);
        click(helper, player, new BlockPos(5, 0, 2), false);
        expectStair(helper, new BlockPos(1, 3, 2), Direction.WEST);
        expectStair(helper, new BlockPos(2, 2, 2), Direction.WEST);
        expectStair(helper, new BlockPos(3, 1, 2), Direction.WEST);
        expectStair(helper, new BlockPos(4, 1, 2), Direction.EAST);
        expectStair(helper, new BlockPos(5, 1, 2), Direction.EAST);
        helper.succeed();
    }

    /** A step of stone one up from x 2 on: the rail at its foot slopes up to it, and those on it lie flat. */
    private static void risingRails(GameTestHelper helper) {
        for (int x = 2; x <= 5; x++) {
            helper.setBlock(new BlockPos(x, 1, 2), Blocks.STONE);
        }
        ListeningPlayer player = holding(helper, Blocks.RAIL, 16);
        start(helper, player);
        press(player, 1);
        click(helper, player, new BlockPos(5, 1, 2), false);
        expectRail(helper, new BlockPos(1, 1, 2), RailShape.ASCENDING_EAST);
        for (int x = 2; x <= 5; x++) {
            expectRail(helper, new BlockPos(x, 2, 2), RailShape.EAST_WEST);
        }
        helper.succeed();
    }

    private static void railsInMidAir(GameTestHelper helper) {
        ListeningPlayer player = holding(helper, Blocks.RAIL, 16);
        start(helper, player);
        press(player, 1);
        BlockPos end = new BlockPos(5, 0, 2);
        PlacementPlan plan = plan(helper, player, end);
        if (plan == null || !(plan.refusal() instanceof Refusal.At at) || at.reason() != Refusal.Vanilla.VANILLA) {
            helper.fail("the plan was " + plan + ", not refused where vanilla refuses a rail", end);
        }
        click(helper, player, end, false);
        for (int x = 1; x <= 5; x++) {
            for (int y = 1; y <= 2; y++) {
                if (helper.getBlockState(new BlockPos(x, y, 2)).is(Blocks.RAIL)) {
                    helper.fail("a refused stretch laid a rail", new BlockPos(x, y, 2));
                }
            }
        }
        expectHeld(helper, player, 16);
        helper.succeed();
    }

    /** With stairs out of the tag, a sneak-click stores no start and places a stair as vanilla does. */
    private static void takenOutOfTheTag(GameTestHelper helper) {
        if (Stretches.builderOf(Blocks.OAK_STAIRS.asItem()) != null) {
            helper.fail("stairs still stretch with #minecraft:stairs taken out of the tag");
        }
        ListeningPlayer player = holding(helper, Blocks.OAK_STAIRS, 16);
        start(helper, player);
        if (player.getMainHandItem().has(Groundworks.STRETCH.get())) {
            helper.fail("a sneak-click stored a stretch's start");
        }
        expectStair(helper, START.above(), Direction.EAST);
        helper.succeed();
    }

    private static void expectStair(GameTestHelper helper, BlockPos pos, Direction facing) {
        BlockState there = helper.getBlockState(pos);
        if (!there.is(Blocks.OAK_STAIRS) || there.getValue(StairBlock.FACING) != facing
                || there.getValue(StairBlock.HALF) != Blocks.OAK_STAIRS.defaultBlockState().getValue(StairBlock.HALF)) {
            helper.fail("laid " + there + ", not a bottom stair facing " + facing, pos);
        }
        BlockPos absolute = helper.absolutePos(pos);
        if (!Block.updateFromNeighbourShapes(there, helper.getLevel(), absolute).equals(there)) {
            helper.fail("laid " + there + ", not joined to its neighbours as vanilla joins stairs", pos);
        }
    }

    private static void expectRail(GameTestHelper helper, BlockPos pos, RailShape shape) {
        BlockState there = helper.getBlockState(pos);
        if (!there.is(Blocks.RAIL) || there.getValue(RailBlock.SHAPE) != shape) {
            helper.fail("laid " + there + ", not a rail " + shape, pos);
        }
    }

    private static void expectSlab(GameTestHelper helper, BlockPos pos, SlabType type) {
        BlockState there = helper.getBlockState(pos);
        if (!there.is(Blocks.OAK_SLAB) || there.getValue(SlabBlock.TYPE) != type) {
            helper.fail("laid " + there + ", not a " + type + " oak slab", pos);
        }
    }

    private static void expectSnow(GameTestHelper helper, BlockPos pos, int layers) {
        BlockState there = helper.getBlockState(pos);
        if (!there.is(Blocks.SNOW) || there.getValue(SnowLayerBlock.LAYERS) != layers) {
            helper.fail("laid " + there + ", not " + layers + " snow layers", pos);
        }
    }

    private static void expectTrapdoor(GameTestHelper helper, BlockPos pos, Direction facing, Half half) {
        BlockState there = helper.getBlockState(pos);
        if (!there.is(Blocks.OAK_TRAPDOOR) || there.getValue(TrapDoorBlock.FACING) != facing
                || there.getValue(TrapDoorBlock.HALF) != half) {
            helper.fail("laid " + there + ", not a " + half + " oak trapdoor facing " + facing, pos);
        }
    }

    private static void expectPlannedWhereLaid(GameTestHelper helper, List<BlockPos> planned, List<BlockPos> laid) {
        if (!planned.stream().sorted().toList().equals(laid.stream().map(helper::absolutePos).sorted().toList())) {
            helper.fail("the plan put blocks at " + planned + ", expected " + laid);
        }
    }

    private static void expectHeld(GameTestHelper helper, ListeningPlayer player, int held) {
        int count = player.getMainHandItem().getCount();
        if (count != held) {
            helper.fail("the player holds " + count + ", expected " + held);
        }
    }

    private static List<BlockPos> plannedPositions(GameTestHelper helper, ListeningPlayer player, BlockPos floor) {
        PlacementPlan plan = plan(helper, player, floor);
        if (plan == null || plan.isRefused()) {
            helper.fail("the plan was " + plan, floor);
        }
        return plan.blocks().stream().map(PlacementPlan.Placed::pos).toList();
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

    private static ListeningPlayer holding(GameTestHelper helper, Block block, int count) {
        ListeningPlayer player = new ListeningPlayer(helper, new BlockPos(0, 1, 0));
        player.setYRot(Direction.EAST.toYRot());
        player.setYHeadRot(Direction.EAST.toYRot());
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(block, count));
        return player;
    }

    private static BlockHitResult onTop(GameTestHelper helper, BlockPos pos) {
        BlockPos absolute = helper.absolutePos(pos);
        return new BlockHitResult(Vec3.atCenterOf(absolute).relative(Direction.UP, 0.5), Direction.UP, absolute, false);
    }
}
