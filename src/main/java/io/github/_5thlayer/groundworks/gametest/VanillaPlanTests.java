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
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * An opted-in vanilla block's Vanilla Plan is what the click places: the plan asked before the
 * click, as the preview asks it, names the block and state the click then puts down, and where
 * vanilla refuses the placement the plan is refused there and the click places nothing.
 */
final class VanillaPlanTests {

    private static final BlockPos FLOOR = new BlockPos(4, 0, 4);

    private VanillaPlanTests() {
    }

    static void register(GroundworksGameTests.Registrar tests) {
        tests.optIn(Blocks.FURNACE, Blocks.OAK_STAIRS, Blocks.POPPY);
        for (Direction look : Direction.Plane.HORIZONTAL) {
            String looking = look.getSerializedName();
            tests.test("a_furnace_looking_" + looking + "_is_placed_as_planned", 20,
                    helper -> placedAsPlanned(helper, Blocks.FURNACE, look));
            tests.test("stairs_looking_" + looking + "_are_placed_as_planned", 20,
                    helper -> placedAsPlanned(helper, Blocks.OAK_STAIRS, look));
        }
        // A poppy needs soil, so vanilla refuses it on the stone floor once it has a position.
        tests.test("a_poppy_on_stone_is_planned_refused_and_not_placed", 20, VanillaPlanTests::refusedAndNotPlaced);
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
