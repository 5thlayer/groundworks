// SPDX-FileCopyrightText: 2026 5thlayer
// SPDX-License-Identifier: MIT

package io.github._5thlayer.groundworks.gametest;

import java.util.List;
import java.util.function.Function;

import io.github._5thlayer.groundworks.Groundworks;
import io.github._5thlayer.groundworks.PlacementPlan;
import io.github._5thlayer.groundworks.Placements;
import io.github._5thlayer.groundworks.QuarterTurn;
import io.github._5thlayer.groundworks.Rotate;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.RotationSegment;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

/**
 * Rotate the Plan on vanilla blocks: an opted-in oriented block held and pressed places turned from
 * the look, the plan asked before the click agrees, and the turn goes where the stack goes. Every
 * press goes through {@link Rotate#press}, which is what the key's payload calls; the key itself is
 * checked by hand.
 */
final class RotateTests {

    /**
     * An opted-in block for each way a placement reads the look, and what it places looking a way:
     * a furnace reads the horizontal direction, an observer the nearest looking direction, a skull
     * on the floor the rotation.
     */
    private static final List<Oriented> ORIENTED = List.of(
            new Oriented(Blocks.FURNACE,
                    look -> Blocks.FURNACE.defaultBlockState().setValue(BlockStateProperties.HORIZONTAL_FACING, look.getOpposite())),
            new Oriented(Blocks.OBSERVER,
                    look -> Blocks.OBSERVER.defaultBlockState().setValue(BlockStateProperties.FACING, look)),
            new Oriented(Blocks.SKELETON_SKULL,
                    look -> Blocks.SKELETON_SKULL.defaultBlockState().setValue(BlockStateProperties.ROTATION_16,
                            RotationSegment.convertToSegment(look.toYRot()))));

    private RotateTests() {
    }

    static void register(GroundworksGameTests.Registrar tests) {
        tests.optIn(Blocks.FURNACE, Blocks.OBSERVER, Blocks.SKELETON_SKULL, Blocks.COBBLESTONE);
        for (Direction look : List.of(Direction.EAST, Direction.SOUTH)) {
            for (boolean sneaking : List.of(false, true)) {
                for (boolean reverse : List.of(false, true)) {
                    tests.test((reverse ? "reverse_rotate" : "rotate") + "_the_plan_looking_"
                                    + look.getSerializedName() + (sneaking ? "_sneaking" : ""), 20,
                            helper -> placesTurned(helper, look, sneaking, reverse));
                }
            }
        }
        tests.test("an_unpressed_stack_and_one_turned_back_carry_no_turn", 20, RotateTests::noTurnCarried);
        tests.test("a_turn_stays_on_the_rest_of_a_stack_and_goes_with_its_last_item", 20,
                RotateTests::turnGoesWithTheLastItem);
        tests.test("a_turned_stack_in_the_off_hand_places_unturned", 20, RotateTests::offHandUnturned);
        // Not opted in, so no Placement Preview is drawn for it, and Rotate never turns what isn't drawn.
        tests.test("rotate_leaves_an_oriented_block_that_is_not_opted_in_unturned", 20,
                helper -> notTurned(helper, Blocks.BLAST_FURNACE));
        tests.test("rotate_leaves_an_opted_in_block_with_no_orientation_unturned", 20,
                helper -> notTurned(helper, Blocks.COBBLESTONE));
    }

    /** For 0 to 3 presses, each oriented block places the look turned that many quarters, as planned. */
    private static void placesTurned(GameTestHelper helper, Direction look, boolean sneaking, boolean reverse) {
        for (int presses = 0; presses < 4; presses++) {
            Direction turned = look;
            for (int press = 0; press < presses; press++) {
                turned = reverse ? turned.getCounterClockWise() : turned.getClockWise();
            }
            for (int row = 0; row < ORIENTED.size(); row++) {
                Oriented oriented = ORIENTED.get(row);
                Player player = holding(helper, oriented.block(), look);
                player.setShiftKeyDown(sneaking);
                press(player, presses, reverse);
                BlockPos floor = new BlockPos(1 + 2 * presses, 0, 1 + 3 * row);
                BlockState placed = placeAsPlanned(helper, player, floor);
                BlockState expected = oriented.placedLooking().apply(turned);
                if (!placed.equals(expected)) {
                    helper.fail(name(oriented.block()) + " looking " + look + " after " + presses + " presses placed "
                            + placed + ", expected " + expected, floor.above());
                }
            }
        }
        helper.succeed();
    }

    private static void noTurnCarried(GameTestHelper helper) {
        Player player = holding(helper, Blocks.FURNACE, Direction.EAST);
        if (player.getMainHandItem().has(Groundworks.QUARTER_TURN.get())) {
            helper.fail("an unpressed stack carries a turn");
        }
        Rotate.press(player, null, false);
        Rotate.press(player, null, true);
        if (player.getMainHandItem().has(Groundworks.QUARTER_TURN.get())) {
            helper.fail("a stack turned back to none still carries the turn, so it won't stack with an unturned one");
        }
        helper.succeed();
    }

    private static void turnGoesWithTheLastItem(GameTestHelper helper) {
        Player player = holding(helper, Blocks.FURNACE, Direction.EAST);
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Blocks.FURNACE, 2));
        Rotate.press(player, null, false);
        BlockState turned = ORIENTED.getFirst().placedLooking().apply(Direction.SOUTH);

        BlockPos first = new BlockPos(2, 0, 2);
        BlockState placed = placeAsPlanned(helper, player, first);
        ItemStack rest = player.getMainHandItem();
        if (!placed.equals(turned) || rest.getCount() != 1 || !Rotate.turnOf(rest).equals(QuarterTurn.of(1))) {
            helper.fail("one of two placed " + placed + " and left " + rest.getCount() + " turned " + Rotate.turnOf(rest)
                    + ", expected " + turned + " and 1 turned a quarter", first.above());
        }
        BlockPos last = new BlockPos(6, 0, 2);
        placed = placeAsPlanned(helper, player, last);
        if (!placed.equals(turned) || !player.getMainHandItem().isEmpty()) {
            helper.fail("the last placed " + placed + " and left " + player.getMainHandItem() + ", expected "
                    + turned + " and nothing", last.above());
        }
        helper.succeed();
    }

    /** The Placement Preview draws the main hand only, so a turn the player moved to the other hand is not seen and not placed. */
    private static void offHandUnturned(GameTestHelper helper) {
        Player player = holding(helper, Blocks.FURNACE, Direction.EAST);
        Rotate.press(player, null, false);
        player.setItemInHand(InteractionHand.OFF_HAND, player.getMainHandItem());
        player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
        BlockPos floor = new BlockPos(4, 0, 4);
        player.getOffhandItem().useOn(new UseOnContext(player, InteractionHand.OFF_HAND, onTop(helper, floor)));
        BlockState placed = helper.getBlockState(floor.above());
        BlockState unturned = ORIENTED.getFirst().placedLooking().apply(Direction.EAST);
        if (!placed.equals(unturned)) {
            helper.fail("a turned stack in the off hand placed " + placed + ", expected " + unturned, floor.above());
        }
        helper.succeed();
    }

    /** A press leaves the stack unturned, and the block places as if it had never been pressed. */
    private static void notTurned(GameTestHelper helper, Block block) {
        Player player = holding(helper, block, Direction.EAST);
        Rotate.press(player, null, false);
        if (player.getMainHandItem().has(Groundworks.QUARTER_TURN.get())) {
            helper.fail("Rotate turned a stack of " + name(block));
        }
        BlockPos floor = new BlockPos(4, 0, 4);
        helper.useBlock(floor, player, onTop(helper, floor));
        BlockState placed = helper.getBlockState(floor.above());
        BlockState unturned = placed.hasProperty(BlockStateProperties.HORIZONTAL_FACING)
                ? placed.setValue(BlockStateProperties.HORIZONTAL_FACING, Direction.WEST)
                : block.defaultBlockState();
        if (!placed.equals(unturned)) {
            helper.fail(name(block) + " placed " + placed + ", expected " + unturned, floor.above());
        }
        helper.succeed();
    }

    /** Clicks the top of {@code floor} and answers what it placed, failing unless the plan asked first agrees. */
    private static BlockState placeAsPlanned(GameTestHelper helper, Player player, BlockPos floor) {
        BlockHitResult hit = onTop(helper, floor);
        PlacementPlan plan = Placements.planFor(player.level(), player, InteractionHand.MAIN_HAND,
                player.getMainHandItem(), hit);
        helper.useBlock(floor, player, hit);
        BlockState placed = helper.getBlockState(floor.above());
        if (plan == null || plan.isRefused() || !plan.blocks().equals(
                List.of(new PlacementPlan.Placed(helper.absolutePos(floor.above()), placed)))) {
            helper.fail("the plan was " + plan + ", the click placed " + placed, floor.above());
        }
        return placed;
    }

    private static void press(Player player, int presses, boolean reverse) {
        for (int press = 0; press < presses; press++) {
            Rotate.press(player, null, reverse);
        }
    }

    private static Player holding(GameTestHelper helper, Block block, Direction look) {
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        player.setYRot(look.toYRot());
        // The nearest looking directions are read off the head, which a mock player doesn't turn with the body.
        player.setYHeadRot(look.toYRot());
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(block, 16));
        return player;
    }

    private static BlockHitResult onTop(GameTestHelper helper, BlockPos ground) {
        var absolute = helper.absolutePos(ground);
        return new BlockHitResult(Vec3.atCenterOf(absolute).relative(Direction.UP, 0.5), Direction.UP, absolute, false);
    }

    private static String name(Block block) {
        return BuiltInRegistries.BLOCK.getKey(block).toString();
    }

    private record Oriented(Block block, Function<Direction, BlockState> placedLooking) {
    }
}
