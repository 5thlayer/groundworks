// SPDX-FileCopyrightText: 2026 5thlayer
// SPDX-License-Identifier: MIT

package io.github._5thlayer.groundworks.gametest;

import java.util.List;

import io.github._5thlayer.groundworks.PlacementPlan;
import io.github._5thlayer.groundworks.Placements;
import io.github._5thlayer.groundworks.Refusal;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ScaffoldingBlock;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * An opted-in vanilla block's Vanilla Plan is what the click places: the plan asked before the
 * click, as the preview asks it, names the block and state the click then puts down, and where
 * vanilla refuses the placement the plan is refused there and the click places nothing. An item
 * whose block needn't survive where it goes, as scaffolding's needn't, is planned where vanilla
 * places it although it won't stand there.
 */
final class VanillaPlanTests {

    private static final BlockPos FLOOR = new BlockPos(4, 0, 4);

    private VanillaPlanTests() {
    }

    static void register(GroundworksGameTests.Registrar tests) {
        tests.optIn(Blocks.POPPY, Blocks.SCAFFOLDING, Blocks.OAK_SIGN);
        for (Direction look : Direction.Plane.HORIZONTAL) {
            String looking = look.getSerializedName();
            tests.test("a_furnace_looking_" + looking + "_is_placed_as_planned", 20,
                    helper -> placedAsPlanned(helper, Blocks.FURNACE, look));
            tests.test("stairs_looking_" + looking + "_are_placed_as_planned", 20,
                    helper -> placedAsPlanned(helper, Blocks.OAK_STAIRS, look));
        }
        // A poppy needs soil, so vanilla refuses it on the stone floor once it has a position.
        tests.test("a_poppy_on_stone_is_planned_refused_and_not_placed", 20, VanillaPlanTests::refusedAndNotPlaced);
        // Scaffolding's item needn't survive: vanilla places it past a bridge's reach, and it falls.
        tests.test("scaffolding_past_its_reach_is_placed_as_planned", 20, VanillaPlanTests::scaffoldingPastItsReach);
        // A sign's item chooses the wall sign where the sign would hang on the face it is placed against.
        tests.test("a_sign_against_a_wall_is_planned_and_placed_on_the_wall", 20, VanillaPlanTests::signOnAWall);
    }

    private static void placedAsPlanned(GameTestHelper helper, Block block, Direction look) {
        Player player = holding(helper, block, look);
        BlockHitResult hit = onTop(helper, FLOOR);
        PlacementPlan plan = planFor(player, hit);
        if (plan == null || plan.blocks().size() != 1) {
            throw helper.assertionException(FLOOR, "the library plans " + plan + ", not one " + name(block));
        }
        if (plan.isRefused()) {
            helper.fail("the library refuses " + name(block) + " with " + plan.refusal(), FLOOR);
        }
        PlacementPlan.Placed planned = plan.blocks().getFirst();
        // A facing the look doesn't reach would agree whatever way the player looked.
        if (planned.state().getValue(BlockStateProperties.HORIZONTAL_FACING).getAxis() != look.getAxis()) {
            helper.fail("the plan's " + name(block) + " faces across the look " + look + ", so this proves little", FLOOR);
        }

        helper.useBlock(FLOOR, player, hit);
        var placed = helper.getLevel().getBlockState(planned.pos());
        if (!placed.equals(planned.state())) {
            helper.fail("the plan was " + planned.state() + ", the click placed " + placed, FLOOR.above());
        }
        helper.succeed();
    }

    private static void refusedAndNotPlaced(GameTestHelper helper) {
        Player player = holding(helper, Blocks.POPPY, Direction.NORTH);
        BlockHitResult hit = onTop(helper, FLOOR);
        PlacementPlan plan = planFor(player, hit);
        if (plan == null) {
            throw helper.assertionException(FLOOR, "the library plans nothing, so no refusal is drawn");
        }
        if (plan.refusal() != Refusal.Vanilla.VANILLA) {
            helper.fail("the library's plan is refused with " + plan.refusal() + ", not vanilla's refusal", FLOOR);
        }
        var above = helper.absolutePos(FLOOR.above());
        if (!plan.blocks().equals(List.of(new PlacementPlan.Placed(above, Blocks.POPPY.defaultBlockState())))) {
            helper.fail("the refused plan is drawn at " + plan.blocks() + ", not where the poppy would go", FLOOR);
        }

        helper.useBlock(FLOOR, player, hit);
        helper.assertBlockNotPresent(Blocks.POPPY, FLOOR.above());
        helper.succeed();
    }

    /**
     * A row of scaffolding at distances 0 to 6, the first on a stone block, clicked on top of its far
     * end, puts the next at distance 7, where its state can't survive: vanilla places it all the
     * same, since {@code ScaffoldingBlockItem#mustSurvive} is false, and it falls on its tick.
     */
    private static void scaffoldingPastItsReach(GameTestHelper helper) {
        BlockPos support = new BlockPos(0, 1, 4);
        helper.setBlock(support, Blocks.STONE);
        BlockPos end = support.above();
        for (int distance = 0; distance < ScaffoldingBlock.STABILITY_MAX_DISTANCE; distance++) {
            end = support.above().east(distance);
            helper.setBlock(end, Blocks.SCAFFOLDING.defaultBlockState()
                    .setValue(ScaffoldingBlock.DISTANCE, distance)
                    .setValue(ScaffoldingBlock.BOTTOM, distance > 0));
        }
        Player player = holding(helper, Blocks.SCAFFOLDING, Direction.EAST);
        BlockHitResult hit = onTop(helper, end);
        PlacementPlan plan = planFor(player, hit);
        BlockPos beyond = end.east();
        if (plan == null || plan.blocks().size() != 1) {
            throw helper.assertionException(beyond, "the library plans " + plan + ", not one scaffolding");
        }
        if (plan.isRefused()) {
            helper.fail("the library refuses scaffolding vanilla places, with " + plan.refusal(), beyond);
        }
        PlacementPlan.Placed planned = plan.blocks().getFirst();
        if (!planned.pos().equals(helper.absolutePos(beyond))
                || planned.state().getValue(ScaffoldingBlock.DISTANCE) != ScaffoldingBlock.STABILITY_MAX_DISTANCE) {
            helper.fail("the plan is " + planned + ", not scaffolding past the bridge's reach", beyond);
        }

        helper.useBlock(end, player, hit);
        var placed = helper.getLevel().getBlockState(planned.pos());
        if (!placed.equals(planned.state())) {
            helper.fail("the plan was " + planned.state() + ", the click placed " + placed, beyond);
        }
        helper.succeed();
    }

    /**
     * A sign clicked on the east side of a stone block, the player looking west, is planned as the
     * wall sign hanging on it, the state its item places, and placed as planned.
     */
    private static void signOnAWall(GameTestHelper helper) {
        BlockPos wall = FLOOR.above();
        helper.setBlock(wall, Blocks.STONE);
        Player player = holding(helper, Blocks.OAK_SIGN, Direction.WEST);
        var absolute = helper.absolutePos(wall);
        BlockHitResult hit = new BlockHitResult(Vec3.atCenterOf(absolute).relative(Direction.EAST, 0.5), Direction.EAST,
                absolute, false);
        PlacementPlan plan = planFor(player, hit);
        BlockPos beside = wall.east();
        var expected = Blocks.OAK_WALL_SIGN.defaultBlockState().setValue(BlockStateProperties.HORIZONTAL_FACING,
                Direction.EAST);
        if (plan == null || plan.isRefused()
                || !plan.blocks().equals(List.of(new PlacementPlan.Placed(helper.absolutePos(beside), expected)))) {
            throw helper.assertionException(beside, "the library plans " + plan + ", not the wall sign on the stone");
        }

        helper.useBlock(wall, player, hit);
        var placed = helper.getLevel().getBlockState(helper.absolutePos(beside));
        if (!placed.equals(expected)) {
            helper.fail("the plan was " + expected + ", the click placed " + placed, beside);
        }
        helper.succeed();
    }

    private static Player holding(GameTestHelper helper, Block block, Direction look) {
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        player.setYRot(look.toYRot());
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(block, 16));
        return player;
    }

    /** The plan the library draws for this click, as its preview asks for it. */
    private static @Nullable PlacementPlan planFor(Player player, BlockHitResult hit) {
        return Placements.planFor(player.level(), player, InteractionHand.MAIN_HAND, player.getMainHandItem(), hit);
    }

    private static BlockHitResult onTop(GameTestHelper helper, BlockPos ground) {
        var absolute = helper.absolutePos(ground);
        return new BlockHitResult(Vec3.atCenterOf(absolute).relative(Direction.UP, 0.5), Direction.UP, absolute, false);
    }

    private static String name(Block block) {
        return BuiltInRegistries.BLOCK.getKey(block).toString();
    }
}
