// SPDX-FileCopyrightText: 2026 5thlayer
// SPDX-License-Identifier: MIT

package io.github._5thlayer.groundworks.gametest;

import java.util.List;

import io.github._5thlayer.groundworks.PlacementPlan;
import io.github._5thlayer.groundworks.Placements;
import io.github._5thlayer.groundworks.Refusal;
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
 * A Vanilla Plan names its placed block's Companions, the blocks it puts down itself once placed:
 * a door's upper half, a bed's head, a tall flower's top. The plan asked before the click, as the
 * preview asks it, names each Companion as the click then lays it, and nothing that lays the plan
 * places one itself, so a door still leaves two blocks and costs one item.
 *
 * <p>Each test clicks the top of a block, the player standing west of it looking east. The Stretch
 * and the Fast Replace run with {@link #COMPANIONS}, a datapack switched on for a batch of its own.
 */
final class VanillaCompanionTests {

    private static final BlockPos FLOOR = new BlockPos(2, 0, 2);

    /**
     * The pack under {@code gametest_packs/} that puts {@code #minecraft:doors} in {@code
     * groundworks:stretches} and oak planks and the oak door in a Replace group of their own.
     */
    private static final String COMPANIONS = "companions";

    private VanillaCompanionTests() {
    }

    static void register(GroundworksGameTests.Registrar tests) {
        tests.optIn(Blocks.SUNFLOWER);
        tests.test("vanilla_companion_a_door_s_plan_names_its_upper_half_above_it_as_the_click_lays_it", 20,
                helper -> plannedAsLaid(helper, Blocks.STONE, Blocks.OAK_DOOR, Direction.UP));
        tests.test("vanilla_companion_a_bed_s_plan_names_its_head_where_it_faces_as_the_click_lays_it", 20,
                helper -> plannedAsLaid(helper, Blocks.STONE, Blocks.RED_BED, Direction.EAST));
        tests.test("vanilla_companion_a_sunflower_s_plan_names_its_top_above_it_as_the_click_lays_it", 20,
                helper -> plannedAsLaid(helper, Blocks.DIRT, Blocks.SUNFLOWER, Direction.UP));
        tests.test("vanilla_companion_a_refused_door_still_names_its_upper_half", 20,
                VanillaCompanionTests::refusedDoor);
        var paired = tests.withPack(COMPANIONS);
        paired.test("vanilla_companion_a_stretch_of_doors_names_each_door_s_upper_half", 20,
                VanillaCompanionTests::stretchOfDoors);
        paired.test("vanilla_companion_a_stretch_of_doors_lays_four_two_block_doors_for_one_door_each", 20,
                VanillaCompanionTests::stretchOfDoorsLaid);
        paired.test("vanilla_companion_a_fast_replace_with_a_door_leaves_one_two_block_door_for_one_item", 20,
                VanillaCompanionTests::fastReplaceWithADoor);
    }

    /**
     * {@code block} clicked on top of {@code ground}: the plan places one block, at the lower or
     * foot part, and names one Companion, toward {@code companion}, the same state with its other
     * part; the click lays both exactly as planned and charges one item.
     */
    private static void plannedAsLaid(GameTestHelper helper, Block ground, Block block, Direction companion) {
        helper.setBlock(FLOOR, ground);
        ListeningPlayer player = holding(helper, new ItemStack(block, 2));
        PlacementPlan plan = planAt(helper, player, FLOOR);
        if (plan == null || plan.isRefused() || plan.blocks().size() != 1) {
            throw helper.assertionException(FLOOR, "the plan is " + plan + ", not one " + block + " accepted");
        }
        PlacementPlan.Placed placed = plan.blocks().getFirst();
        PlacementPlan.Placed expected = new PlacementPlan.Placed(placed.pos().relative(companion), otherPart(placed.state()));
        if (!plan.companions().equals(List.of(expected))) {
            helper.fail("the plan's Companions are " + plan.companions() + ", not " + expected, FLOOR.above());
        }

        clickAt(helper, player, FLOOR);
        expectLaid(helper, placed);
        expectLaid(helper, expected);
        expectHeld(helper, player, block, 1);
        helper.succeed();
    }

    /**
     * A door clicked on top of a bottom slab, whose top can't hold it: the plan is refused, and
     * still names the upper half where it would have gone.
     */
    private static void refusedDoor(GameTestHelper helper) {
        BlockPos slab = FLOOR.above();
        helper.setBlock(slab, Blocks.OAK_SLAB);
        ListeningPlayer player = holding(helper, new ItemStack(Blocks.OAK_DOOR, 2));
        PlacementPlan plan = planAt(helper, player, slab);
        if (plan == null || plan.refusal() != Refusal.Vanilla.VANILLA || plan.blocks().size() != 1) {
            throw helper.assertionException(slab, "the plan is " + plan + ", not one door refused by vanilla");
        }
        PlacementPlan.Placed placed = plan.blocks().getFirst();
        PlacementPlan.Placed expected = new PlacementPlan.Placed(placed.pos().above(), otherPart(placed.state()));
        if (!plan.companions().equals(List.of(expected))) {
            helper.fail("the refused plan's Companions are " + plan.companions() + ", not " + expected, slab.above());
        }
        helper.succeed();
    }

    /** Doors stretched east four from a start on the floor: four doors placed, each with its upper half above it. */
    private static void stretchOfDoors(GameTestHelper helper) {
        BlockPos start = new BlockPos(1, 0, 2);
        ListeningPlayer player = holding(helper, new ItemStack(Blocks.OAK_DOOR, 16));
        player.setShiftKeyDown(true);
        clickAt(helper, player, start);
        player.setShiftKeyDown(false);
        BlockPos end = start.east(3);
        PlacementPlan plan = planAt(helper, player, end);
        if (plan == null || plan.isRefused() || plan.blocks().size() != 4) {
            throw helper.assertionException(end, "the stretch's plan is " + plan + ", not four doors accepted");
        }
        List<PlacementPlan.Placed> expected = plan.blocks().stream()
                .peek(placed -> {
                    if (!placed.state().is(Blocks.OAK_DOOR)) {
                        helper.fail("the stretch plans " + placed.state() + ", not a door", helper.relativePos(placed.pos()));
                    }
                })
                .map(placed -> new PlacementPlan.Placed(placed.pos().above(), otherPart(placed.state())))
                .toList();
        if (!plan.companions().equals(expected)) {
            helper.fail("the stretch's Companions are " + plan.companions() + ", not " + expected, end.above(2));
        }
        helper.succeed();
    }

    /**
     * Doors stretched east four from a start on the floor, the end clicked: each door laid as
     * planned with its upper half above it, and four doors taken.
     */
    private static void stretchOfDoorsLaid(GameTestHelper helper) {
        BlockPos start = new BlockPos(1, 0, 2);
        ListeningPlayer player = holding(helper, new ItemStack(Blocks.OAK_DOOR, 16));
        player.setShiftKeyDown(true);
        clickAt(helper, player, start);
        player.setShiftKeyDown(false);
        BlockPos end = start.east(3);
        PlacementPlan plan = planAt(helper, player, end);
        if (plan == null || plan.isRefused() || plan.blocks().size() != 4 || plan.companions().size() != 4) {
            throw helper.assertionException(end, "the stretch's plan is " + plan + ", not four doors accepted");
        }

        clickAt(helper, player, end);
        plan.blocks().forEach(placed -> expectLaid(helper, placed));
        plan.companions().forEach(companion -> expectLaid(helper, companion));
        expectHeld(helper, player, Blocks.OAK_DOOR, 12);
        helper.succeed();
    }

    /**
     * Oak planks replaced by an oak door: the plan replaces the planks and names the upper half
     * above as its Companion; the click leaves one door of two blocks, nothing above it, and takes
     * one door.
     */
    private static void fastReplaceWithADoor(GameTestHelper helper) {
        BlockPos aimed = FLOOR.above();
        helper.setBlock(aimed, Blocks.OAK_PLANKS);
        ListeningPlayer player = holding(helper, new ItemStack(Blocks.OAK_DOOR, 2));
        PlacementPlan plan = planAt(helper, player, aimed);
        BlockPos absolute = helper.absolutePos(aimed);
        if (plan == null || plan.isRefused() || !plan.replaces().equals(List.of(absolute)) || plan.blocks().size() != 1) {
            throw helper.assertionException(aimed, "the plan is " + plan + ", not a door replacing the planks");
        }
        PlacementPlan.Placed placed = plan.blocks().getFirst();
        PlacementPlan.Placed expected = new PlacementPlan.Placed(absolute.above(), otherPart(placed.state()));
        if (!plan.companions().equals(List.of(expected))) {
            helper.fail("the replace's Companions are " + plan.companions() + ", not " + expected, aimed.above());
        }

        clickAt(helper, player, aimed);
        expectLaid(helper, placed);
        expectLaid(helper, expected);
        if (!helper.getBlockState(aimed.above(2)).isAir()) {
            helper.fail("the replace put " + helper.getBlockState(aimed.above(2)) + " above the door", aimed.above(2));
        }
        expectHeld(helper, player, Blocks.OAK_DOOR, 1);
        helper.succeed();
    }

    /** The same state with its other part: a lower half's upper, a bed's foot's head. */
    private static BlockState otherPart(BlockState state) {
        if (state.hasProperty(BlockStateProperties.DOUBLE_BLOCK_HALF)) {
            return state.setValue(BlockStateProperties.DOUBLE_BLOCK_HALF, DoubleBlockHalf.UPPER);
        }
        return state.setValue(BlockStateProperties.BED_PART, BedPart.HEAD);
    }

    private static void expectLaid(GameTestHelper helper, PlacementPlan.Placed placed) {
        BlockState laid = helper.getLevel().getBlockState(placed.pos());
        if (!laid.equals(placed.state())) {
            helper.fail("planned " + placed.state() + ", the click laid " + laid, helper.relativePos(placed.pos()));
        }
    }

    private static void expectHeld(GameTestHelper helper, ListeningPlayer player, Block block, int count) {
        int held = player.getInventory().countItem(block.asItem());
        if (held != count) {
            helper.fail("the player holds " + held + " " + block + ", not " + count);
        }
    }

    private static @Nullable PlacementPlan planAt(GameTestHelper helper, ListeningPlayer player, BlockPos aim) {
        return Placements.planFor(player.level(), player, InteractionHand.MAIN_HAND, player.getMainHandItem(), onTop(helper, aim));
    }

    private static void clickAt(GameTestHelper helper, ListeningPlayer player, BlockPos aim) {
        player.gameMode.useItemOn(player, helper.getLevel(), player.getMainHandItem(), InteractionHand.MAIN_HAND,
                onTop(helper, aim));
    }

    private static ListeningPlayer holding(GameTestHelper helper, ItemStack stack) {
        ListeningPlayer player = new ListeningPlayer(helper, new BlockPos(0, 1, 2));
        player.setYRot(Direction.EAST.toYRot());
        player.setYHeadRot(Direction.EAST.toYRot());
        player.setItemInHand(InteractionHand.MAIN_HAND, stack);
        return player;
    }

    private static BlockHitResult onTop(GameTestHelper helper, BlockPos pos) {
        BlockPos absolute = helper.absolutePos(pos);
        return new BlockHitResult(Vec3.atCenterOf(absolute).relative(Direction.UP, 0.5), Direction.UP, absolute, false);
    }
}
