// SPDX-FileCopyrightText: 2026 5thlayer
// SPDX-License-Identifier: MIT

package io.github._5thlayer.groundworks.gametest;

import java.util.List;
import java.util.function.UnaryOperator;

import io.github._5thlayer.groundworks.PlacementPlan;
import io.github._5thlayer.groundworks.Placements;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BedPart;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * A Vanilla Plan names its block's Companions, the part vanilla puts down itself once the block is
 * placed: a door's upper half, a bed's head, a tall plant's top. The click places them as planned,
 * through the block's own {@code setPlacedBy}, and nothing that lays the plan places them again or
 * charges for them.
 */
final class VanillaCompanionTests {

    /** Stretches oak doors, and groups them with smooth stone for a Fast Replace. */
    private static final String LAYS_COMPANIONS = "lays_companions";

    private static final BlockPos FLOOR = new BlockPos(4, 0, 4);
    private static final BlockPos START = new BlockPos(1, 0, 2);

    private VanillaCompanionTests() {
    }

    static void register(GroundworksGameTests.Registrar tests) {
        tests.optIn(Blocks.SUNFLOWER);
        tests.test("vanilla_companion_a_door_names_its_upper_half", 20,
                helper -> namesOne(helper, Blocks.OAK_DOOR, Direction.UP,
                        lower -> lower.setValue(BlockStateProperties.DOUBLE_BLOCK_HALF, DoubleBlockHalf.UPPER)));
        tests.test("vanilla_companion_a_bed_names_its_head", 20,
                helper -> namesOne(helper, Blocks.RED_BED, Direction.EAST,
                        foot -> foot.setValue(BlockStateProperties.BED_PART, BedPart.HEAD)));
        tests.test("vanilla_companion_a_sunflower_names_its_top", 20, helper -> {
            helper.setBlock(FLOOR, Blocks.DIRT);
            namesOne(helper, Blocks.SUNFLOWER, Direction.UP,
                    lower -> lower.setValue(BlockStateProperties.DOUBLE_BLOCK_HALF, DoubleBlockHalf.UPPER));
        });
        var packed = tests.withPack(LAYS_COMPANIONS);
        packed.test("vanilla_companion_a_stretch_of_doors_names_each_door_s_upper_half", 20,
                VanillaCompanionTests::stretchOfDoors);
        packed.test("vanilla_companion_a_fast_replace_with_a_door_lays_one_door_and_charges_one", 20,
                VanillaCompanionTests::fastReplaceWithADoor);
    }

    /**
     * The block placed on the floor, looking east, plans one Companion, {@code toward} the placed
     * block, the placed state as {@code companion} makes it; the click places it there.
     */
    private static void namesOne(GameTestHelper helper, Block block, Direction toward,
                                 UnaryOperator<BlockState> companion) {
        ListeningPlayer player = holding(helper, block, new BlockPos(0, 1, 4));
        PlacementPlan plan = planAt(helper, player, FLOOR);
        if (plan == null || plan.isRefused() || plan.blocks().size() != 1) {
            throw helper.assertionException(FLOOR, "the plan is " + plan + ", not one " + block);
        }
        PlacementPlan.Placed placed = plan.blocks().getFirst();
        var expected = List.of(new PlacementPlan.Placed(placed.pos().relative(toward), companion.apply(placed.state())));
        if (!plan.companions().equals(expected)) {
            helper.fail("the plan's Companions are " + plan.companions() + ", expected " + expected, FLOOR);
        }
        clickAt(helper, player, FLOOR);
        expectStanding(helper, plan.blocks());
        expectStanding(helper, expected);
        expectHeld(helper, player, 15);
        helper.succeed();
    }

    /** Four doors east from the start: each planned door names its upper half, and the click lays four whole doors. */
    private static void stretchOfDoors(GameTestHelper helper) {
        ListeningPlayer player = holding(helper, Blocks.OAK_DOOR, new BlockPos(0, 1, 0));
        clickAt(helper, player, START, true);
        BlockPos end = START.east(3);
        PlacementPlan plan = planAt(helper, player, end);
        if (plan == null || plan.isRefused() || plan.blocks().size() != 4) {
            throw helper.assertionException(end, "the plan is " + plan + ", not four doors");
        }
        if (plan.blocks().stream().anyMatch(placed -> !placed.state().is(Blocks.OAK_DOOR))) {
            throw helper.assertionException(end, "the plan lays " + plan.blocks() + ", not four doors");
        }
        var expected = plan.blocks().stream()
                .map(placed -> new PlacementPlan.Placed(placed.pos().above(),
                        placed.state().setValue(BlockStateProperties.DOUBLE_BLOCK_HALF, DoubleBlockHalf.UPPER)))
                .toList();
        if (!plan.companions().equals(expected)) {
            helper.fail("the plan lays " + plan.blocks() + " with Companions " + plan.companions()
                    + ", not four doors with their upper halves", end);
        }
        clickAt(helper, player, end, false);
        expectStanding(helper, plan.blocks());
        expectStanding(helper, expected);
        expected.forEach(upper -> expectEmptyAbove(helper, upper.pos()));
        expectHeld(helper, player, 16 - 4);
        helper.succeed();
    }

    /** A door replacing smooth stone: the plan names its upper half, and the click leaves one two-block door. */
    private static void fastReplaceWithADoor(GameTestHelper helper) {
        BlockPos aimed = new BlockPos(2, 1, 2);
        helper.setBlock(aimed, Blocks.SMOOTH_STONE);
        ListeningPlayer player = holding(helper, Blocks.OAK_DOOR, new BlockPos(0, 1, 2));
        PlacementPlan plan = planAt(helper, player, aimed);
        BlockPos absolute = helper.absolutePos(aimed);
        if (plan == null || plan.isRefused() || !plan.replaces().equals(List.of(absolute)) || plan.blocks().size() != 1) {
            throw helper.assertionException(aimed, "the plan is " + plan + ", not a door replacing the stone");
        }
        PlacementPlan.Placed lower = plan.blocks().getFirst();
        var expected = List.of(new PlacementPlan.Placed(absolute.above(),
                lower.state().setValue(BlockStateProperties.DOUBLE_BLOCK_HALF, DoubleBlockHalf.UPPER)));
        if (!plan.companions().equals(expected)) {
            helper.fail("the plan's Companions are " + plan.companions() + ", expected " + expected, aimed);
        }
        clickAt(helper, player, aimed, false);
        expectStanding(helper, plan.blocks());
        expectStanding(helper, expected);
        expectEmptyAbove(helper, absolute.above());
        expectHeld(helper, player, 15);
        helper.succeed();
    }

    private static void expectStanding(GameTestHelper helper, List<PlacementPlan.Placed> planned) {
        for (PlacementPlan.Placed placed : planned) {
            BlockState standing = helper.getLevel().getBlockState(placed.pos());
            if (!standing.equals(placed.state())) {
                helper.fail("planned " + placed.state() + ", the click left " + standing, helper.relativePos(placed.pos()));
            }
        }
    }

    private static void expectEmptyAbove(GameTestHelper helper, BlockPos absolute) {
        BlockState above = helper.getLevel().getBlockState(absolute.above());
        if (!above.isAir()) {
            helper.fail("the click also placed " + above + " above the Companion", helper.relativePos(absolute.above()));
        }
    }

    private static void expectHeld(GameTestHelper helper, ListeningPlayer player, int held) {
        int count = player.getMainHandItem().getCount();
        if (count != held) {
            helper.fail("the player holds " + count + ", expected " + held);
        }
    }

    private static ListeningPlayer holding(GameTestHelper helper, Block block, BlockPos standing) {
        ListeningPlayer player = new ListeningPlayer(helper, standing);
        player.setYRot(Direction.EAST.toYRot());
        player.setYHeadRot(Direction.EAST.toYRot());
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(block, 16));
        return player;
    }

    private static @Nullable PlacementPlan planAt(GameTestHelper helper, ListeningPlayer player, BlockPos aim) {
        return Placements.planFor(player.level(), player, InteractionHand.MAIN_HAND, player.getMainHandItem(), onTop(helper, aim));
    }

    private static void clickAt(GameTestHelper helper, ListeningPlayer player, BlockPos aim) {
        clickAt(helper, player, aim, false);
    }

    private static void clickAt(GameTestHelper helper, ListeningPlayer player, BlockPos aim, boolean sneaking) {
        player.setShiftKeyDown(sneaking);
        player.gameMode.useItemOn(player, helper.getLevel(), player.getMainHandItem(), InteractionHand.MAIN_HAND,
                onTop(helper, aim));
        player.setShiftKeyDown(false);
    }

    private static BlockHitResult onTop(GameTestHelper helper, BlockPos pos) {
        BlockPos absolute = helper.absolutePos(pos);
        return new BlockHitResult(Vec3.atCenterOf(absolute).relative(Direction.UP, 0.5), Direction.UP, absolute, false);
    }
}
