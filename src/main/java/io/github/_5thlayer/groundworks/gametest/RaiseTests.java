// SPDX-FileCopyrightText: 2026 5thlayer
// SPDX-License-Identifier: MIT

package io.github._5thlayer.groundworks.gametest;

import java.util.List;

import io.github._5thlayer.groundworks.Groundworks;
import io.github._5thlayer.groundworks.Height;
import io.github._5thlayer.groundworks.PlacementPlan;
import io.github._5thlayer.groundworks.Placements;
import io.github._5thlayer.groundworks.Raise;
import io.github._5thlayer.groundworks.Rotate;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.Half;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

/**
 * Raise and Lower on vanilla blocks: an opted-in block held and pressed places straight up or down
 * from where it would go, in mid-air if need be, and the plan asked before the click agrees. Every
 * press goes through {@link Raise#press}, which is what the keys' payload calls; the keys, the
 * guide and the action bar are checked by hand.
 */
final class RaiseTests {

    /** A stone column standing on the floor, whose east face at this block the tests click. */
    private static final BlockPos WALL = new BlockPos(2, 2, 2);

    private RaiseTests() {
    }

    static void register(GroundworksGameTests.Registrar tests) {
        tests.optIn(Blocks.COBBLESTONE, Blocks.FURNACE, Blocks.OAK_STAIRS);
        for (int height : List.of(0, 1, 2, -1)) {
            tests.test("a_block_" + (height < 0 ? "lowered_" + -height : "raised_" + height)
                            + "_against_a_wall_is_placed_at_the_moved_spot", 20,
                    helper -> placedAtTheMovedSpot(helper, height));
        }
        for (int height : List.of(1, -1)) {
            tests.test("stairs_" + (height < 0 ? "lowered" : "raised") + "_against_a_wall_keep_the_half_clicked", 20,
                    helper -> keepTheHalfClicked(helper, height));
        }
        tests.test("a_block_raised_into_a_taken_spot_is_refused_and_not_placed", 20, RaiseTests::refusedAtATakenSpot);
        tests.test("a_height_stays_on_the_rest_of_a_stack_and_goes_with_its_last_item", 20,
                RaiseTests::heightGoesWithTheLastItem);
        // Not opted in, so no Placement Preview is drawn for it, and Raise never moves what isn't drawn.
        tests.test("raise_and_lower_leave_a_block_that_is_not_opted_in_unchanged", 20, RaiseTests::notRaised);
        tests.test("a_stack_turned_and_raised_places_turned_and_raised", 20, RaiseTests::turnedAndRaised);
        tests.test("a_press_past_the_reach_is_refused_and_told", 20, RaiseTests::refusedPastTheReach);
        tests.test("a_height_stored_beyond_the_reach_places_at_the_reach", 20, RaiseTests::clampedWhenRead);
    }

    private static void placedAtTheMovedSpot(GameTestHelper helper, int height) {
        buildWall(helper);
        Player player = holding(helper, Blocks.COBBLESTONE, Direction.WEST);
        press(player, height);
        BlockPos unmoved = WALL.east();
        BlockPos moved = unmoved.above(height);
        BlockState placed = placeAsPlanned(helper, player, WALL, onFace(helper, WALL, Direction.EAST), moved);
        if (!placed.is(Blocks.COBBLESTONE)) {
            helper.fail("the click placed " + placed + " at the moved spot", moved);
        }
        if (height != 0 && !helper.getBlockState(unmoved).isAir()) {
            helper.fail("the click placed " + helper.getBlockState(unmoved) + " where the block would go unmoved", unmoved);
        }
        helper.succeed();
    }

    /** Stairs take their half from where on the face the click lands, which moves with the placement. */
    private static void keepTheHalfClicked(GameTestHelper helper, int height) {
        buildWall(helper);
        Player player = holding(helper, Blocks.OAK_STAIRS, Direction.WEST);
        press(player, height);
        BlockPos absolute = helper.absolutePos(WALL);
        for (Half half : Half.values()) {
            BlockPos moved = WALL.east().above(height);
            double y = absolute.getY() + (half == Half.TOP ? 0.75 : 0.25);
            BlockHitResult hit = new BlockHitResult(new Vec3(absolute.getX() + 1, y, absolute.getZ() + 0.5),
                    Direction.EAST, absolute, false);
            BlockState placed = placeAsPlanned(helper, player, WALL, hit, moved);
            if (placed.getValue(BlockStateProperties.HALF) != half) {
                helper.fail("a click on the " + half + " half placed " + placed, moved);
            }
            helper.setBlock(moved, Blocks.AIR);
        }
        helper.succeed();
    }

    private static void refusedAtATakenSpot(GameTestHelper helper) {
        buildWall(helper);
        BlockPos unmoved = WALL.east();
        BlockPos moved = unmoved.above();
        helper.setBlock(moved, Blocks.STONE);
        Player player = holding(helper, Blocks.COBBLESTONE, Direction.WEST);
        press(player, 1);
        BlockHitResult hit = onFace(helper, WALL, Direction.EAST);
        PlacementPlan plan = Placements.planFor(player.level(), player, InteractionHand.MAIN_HAND,
                player.getMainHandItem(), hit);
        if (plan == null || !plan.isRefused() || plan.blocks().size() != 1
                || !plan.blocks().getFirst().pos().equals(helper.absolutePos(moved))) {
            helper.fail("the plan was " + plan + ", not one block refused at the moved spot", moved);
        }
        helper.useBlock(WALL, player, hit);
        if (!helper.getBlockState(moved).is(Blocks.STONE) || !helper.getBlockState(unmoved).isAir()
                || player.getMainHandItem().getCount() != 16) {
            helper.fail("the click placed " + helper.getBlockState(moved) + " at the moved spot and "
                    + helper.getBlockState(unmoved) + " where it would go unmoved, leaving "
                    + player.getMainHandItem().getCount(), moved);
        }
        helper.succeed();
    }

    private static void heightGoesWithTheLastItem(GameTestHelper helper) {
        Player player = holding(helper, Blocks.COBBLESTONE, Direction.EAST);
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Blocks.COBBLESTONE, 2));
        press(player, 1);

        BlockPos first = new BlockPos(2, 0, 2);
        placeAsPlanned(helper, player, first, onFace(helper, first, Direction.UP), first.above(2));
        ItemStack rest = player.getMainHandItem();
        if (rest.getCount() != 1 || !Raise.heightOf(player, rest).equals(new Height(1))) {
            helper.fail("one of two placed left " + rest.getCount() + " raised " + Raise.heightOf(player, rest)
                    + ", expected 1 raised 1", first.above(2));
        }
        BlockPos last = new BlockPos(6, 0, 2);
        placeAsPlanned(helper, player, last, onFace(helper, last, Direction.UP), last.above(2));
        if (!player.getMainHandItem().isEmpty()) {
            helper.fail("the last placed left " + player.getMainHandItem() + ", expected nothing", last.above(2));
        }
        helper.succeed();
    }

    /** Both presses leave the stack as it was, and the block places where it would go. */
    private static void notRaised(GameTestHelper helper) {
        Player player = holding(helper, Blocks.BLAST_FURNACE, Direction.EAST);
        ItemStack before = player.getMainHandItem().copy();
        press(player, 1);
        press(player, -1);
        if (!ItemStack.matches(player.getMainHandItem(), before)) {
            helper.fail("a press changed a stack of blast furnaces to " + player.getMainHandItem().getComponentsPatch());
        }
        BlockPos floor = new BlockPos(4, 0, 4);
        helper.useBlock(floor, player, onFace(helper, floor, Direction.UP));
        if (!helper.getBlockState(floor.above()).is(Blocks.BLAST_FURNACE)) {
            helper.fail("a blast furnace placed " + helper.getBlockState(floor.above()), floor.above());
        }
        helper.succeed();
    }

    private static void turnedAndRaised(GameTestHelper helper) {
        Player player = holding(helper, Blocks.FURNACE, Direction.EAST);
        Rotate.press(player, null, false);
        press(player, 1);
        BlockPos floor = new BlockPos(4, 0, 4);
        BlockState placed = placeAsPlanned(helper, player, floor, onFace(helper, floor, Direction.UP), floor.above(2));
        // Looking east turned a quarter is looking south, and a furnace faces the one looking at it.
        BlockState expected = Blocks.FURNACE.defaultBlockState()
                .setValue(BlockStateProperties.HORIZONTAL_FACING, Direction.NORTH);
        if (!placed.equals(expected)) {
            helper.fail("a turned and raised furnace placed " + placed + ", expected " + expected, floor.above(2));
        }
        helper.succeed();
    }

    private static void refusedPastTheReach(GameTestHelper helper) {
        ListeningPlayer player = new ListeningPlayer(helper, new BlockPos(1, 1, 1));
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Blocks.COBBLESTONE, 16));
        int cap = Raise.capOf(player);
        press(player, cap + 1);
        Height height = Raise.heightOf(player, player.getMainHandItem());
        if (height.blocks() != cap || !player.heard.equals(List.of("message.groundworks.height_at_reach"))) {
            helper.fail(cap + 1 + " presses with a reach of " + cap + " raised " + height + " and told " + player.heard);
        }
        helper.succeed();
    }

    /** The reach shrank since the height was stored: it is clamped when read, and the placement moves by the cap. */
    private static void clampedWhenRead(GameTestHelper helper) {
        Player player = holding(helper, Blocks.COBBLESTONE, Direction.EAST);
        player.getMainHandItem().set(Groundworks.HEIGHT.get(), new Height(5));
        // Short enough that the capped placement stays inside the test's structure.
        player.getAttribute(Attributes.BLOCK_INTERACTION_RANGE).setBaseValue(2.5);
        BlockPos floor = new BlockPos(4, 0, 4);
        int cap = Raise.capOf(player);
        placeAsPlanned(helper, player, floor, onFace(helper, floor, Direction.UP), floor.above(1 + cap));
        helper.succeed();
    }

    /**
     * Clicks {@code clicked} and answers what it placed at {@code at}, failing unless the plan
     * asked first is that one block there, as the preview would draw it.
     */
    private static BlockState placeAsPlanned(GameTestHelper helper, Player player, BlockPos clicked,
                                             BlockHitResult hit, BlockPos at) {
        PlacementPlan plan = Placements.planFor(player.level(), player, InteractionHand.MAIN_HAND,
                player.getMainHandItem(), hit);
        helper.useBlock(clicked, player, hit);
        BlockState placed = helper.getBlockState(at);
        if (plan == null || plan.isRefused() || placed.isAir()
                || !plan.blocks().equals(List.of(new PlacementPlan.Placed(helper.absolutePos(at), placed)))) {
            helper.fail("the plan was " + plan + ", the click placed " + placed, at);
        }
        return placed;
    }

    private static void buildWall(GameTestHelper helper) {
        for (int y = 1; y <= 4; y++) {
            helper.setBlock(new BlockPos(WALL.getX(), y, WALL.getZ()), Blocks.STONE);
        }
    }

    /** Presses Raise {@code height} times, or Lower as many times if it is negative. */
    private static void press(Player player, int height) {
        for (int press = 0; press < Math.abs(height); press++) {
            Raise.press(player, height < 0);
        }
    }

    private static Player holding(GameTestHelper helper, Block block, Direction look) {
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        player.setYRot(look.toYRot());
        player.setYHeadRot(look.toYRot());
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(block, 16));
        return player;
    }

    private static BlockHitResult onFace(GameTestHelper helper, BlockPos pos, Direction face) {
        BlockPos absolute = helper.absolutePos(pos);
        return new BlockHitResult(Vec3.atCenterOf(absolute).relative(face, 0.5), face, absolute, false);
    }
}
