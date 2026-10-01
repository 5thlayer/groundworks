// SPDX-FileCopyrightText: 2026 5thlayer
// SPDX-License-Identifier: MIT

package io.github._5thlayer.groundworks.gametest;

import java.util.List;

import io.github._5thlayer.groundworks.FastReplace;
import io.github._5thlayer.groundworks.Groundworks;
import io.github._5thlayer.groundworks.Height;
import io.github._5thlayer.groundworks.PlacementPlan;
import io.github._5thlayer.groundworks.Placements;
import io.github._5thlayer.groundworks.Raise;
import io.github._5thlayer.groundworks.QuarterTurn;
import io.github._5thlayer.groundworks.Refusal;
import io.github._5thlayer.groundworks.Rotate;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.Identifier;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.SlabType;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * Fast Replace on the tests' own {@linkplain #stateGroups Replace groups}: lime, pink and magenta
 * glazed terracotta replace one another, birch and jungle slabs and a birch log replace one
 * another, and gray is a group of its own. Three groups have a {@linkplain ReplacesAColumn builder}
 * that replaces a column of two blocks: iron, gold, smoker and blast furnace blocks, which it plans
 * whole; copper and lapis blocks, whose plan leaves a gap; and emerald and redstone blocks, which
 * it refuses. Every click goes through the server player's own
 * {@code useItemOn}, so through the event Groundworks answers, and every plan through {@link
 * Placements#planFor}, as the preview asks it.
 *
 * <p>Each test replaces the block at {@link #AIMED}, aimed at its top, with the player standing
 * west of it looking east and holding the item in hotbar slot 3.
 *
 * <p>The vanilla Consumer's groups, block tags under {@code groundworks:replace_group/}, are tested
 * on concrete, which no group stated here holds, with a datapack switched on for a batch of its own.
 */
final class FastReplaceTests {

    private static final BlockPos AIMED = new BlockPos(2, 1, 2);
    private static final int HELD_SLOT = 3;

    /**
     * The pack under {@code gametest_packs/} whose tags under {@code groundworks:replace_group/}
     * group concrete: {@code first} blue and red, {@code second} red and yellow, so red is in two,
     * and {@code third} green and lime glazed terracotta, which a group stated in code claims.
     */
    private static final String GROUPS_VANILLA_BLOCKS = "groups_vanilla_blocks";

    private FastReplaceTests() {
    }

    /** The tests' groups, stated at mod construction as a Consumer's are. */
    static void stateGroups() {
        FastReplace.group(id("gametest_glazed"), block -> block == Blocks.LIME_GLAZED_TERRACOTTA
                || block == Blocks.PINK_GLAZED_TERRACOTTA || block == Blocks.MAGENTA_GLAZED_TERRACOTTA);
        FastReplace.group(id("gametest_gray"), block -> block == Blocks.GRAY_GLAZED_TERRACOTTA);
        // Slabs, whose type counts what they hold, and a log, whose axis no slab has.
        FastReplace.group(id("gametest_birch"), block -> block == Blocks.BIRCH_SLAB || block == Blocks.JUNGLE_SLAB
                || block == Blocks.BIRCH_LOG);
        // The builders' groups, each a column of two blocks. Smokers and blast furnaces face a way, which a builder's plan sets.
        FastReplace.group(id("gametest_column"), block -> block == Blocks.IRON_BLOCK || block == Blocks.GOLD_BLOCK
                || block == Blocks.SMOKER || block == Blocks.BLAST_FURNACE, new ReplacesAColumn(ReplacesAColumn.Way.REPLACES));
        FastReplace.group(id("gametest_gap"), block -> block == Blocks.COPPER_BLOCK || block == Blocks.LAPIS_BLOCK,
                new ReplacesAColumn(ReplacesAColumn.Way.LEAVES_A_GAP));
        FastReplace.group(id("gametest_refuses"), block -> block == Blocks.EMERALD_BLOCK || block == Blocks.REDSTONE_BLOCK,
                new ReplacesAColumn(ReplacesAColumn.Way.REFUSES));
    }

    static void register(GroundworksGameTests.Registrar tests) {
        tests.test("a_plain_click_replaces_a_block_of_the_held_block_s_group_as_planned_and_hands_it_back", 20,
                FastReplaceTests::replaces);
        tests.test("a_replace_that_empties_the_held_stack_hands_back_into_the_held_slot", 20,
                FastReplaceTests::emptiesTheHeldStack);
        tests.test("an_item_that_plans_its_own_placement_replaces_through_its_own_plan", 20,
                FastReplaceTests::itemThatPlansItsOwnPlacement);
        tests.test("a_sneak_click_places_beside_instead_of_replacing", 20, FastReplaceTests::sneakPlacesBeside);
        tests.test("an_aimed_block_in_no_group_is_not_replaced", 20, FastReplaceTests::aimedInNoGroup);
        tests.test("a_held_block_in_another_group_does_not_replace", 20, FastReplaceTests::heldInAnotherGroup);
        tests.test("a_held_block_in_no_group_does_not_replace", 20, FastReplaceTests::heldInNoGroup);
        tests.test("a_block_is_not_replaced_by_itself", 20, FastReplaceTests::sameBlock);
        tests.test("with_a_stretch_start_stored_the_plain_click_belongs_to_the_stretch", 20,
                FastReplaceTests::stretchStartStored);
        tests.test("a_held_height_does_not_move_a_replace_and_stays_on_the_stack", 20, FastReplaceTests::heightHeld);
        tests.test("no_room_to_return_refuses_the_replace_and_changes_nothing", 20, FastReplaceTests::noRoomToReturn);
        tests.test("a_player_who_may_not_build_is_refused_and_told_and_nothing_changes", 20,
                FastReplaceTests::mayNotBuild);
        tests.test("a_claim_refusing_through_the_place_event_refuses_the_replace", 20, FastReplaceTests::claimGuards);
        tests.test("a_replace_refused_for_no_room_fires_no_place_event_in_a_claim", 20,
                FastReplaceTests::refusedFiresNoPlaceEvent);
        tests.test("an_entity_where_the_new_block_would_go_refuses_the_replace", 20, FastReplaceTests::entityInTheWay);
        tests.test("a_creative_player_replaces_for_nothing_and_is_handed_nothing_even_with_a_full_inventory", 20,
                FastReplaceTests::creativeReplaces);
        tests.test("a_replace_keeps_the_old_block_s_facing_and_the_preview_shows_it", 20, FastReplaceTests::keepsFacing);
        tests.test("a_replace_with_a_turn_held_takes_the_turned_look_and_keeps_the_turn", 20,
                FastReplaceTests::turnOverridesTheCopy);
        tests.test("a_replace_copies_no_property_but_the_orientation_so_a_double_slab_is_not_duplicated", 20,
                FastReplaceTests::doubleSlabIsNotCopied);
        tests.test("an_orientation_the_new_block_does_not_have_is_dropped", 20,
                FastReplaceTests::orientationTheNewBlockLacks);

        // A Replace group's builder, which plans a replace that spans a column of two blocks.
        tests.test("a_builder_replaces_a_column_from_its_lower_block_for_one_item_and_hands_back_what_it_names", 20,
                helper -> columnReplaced(helper, AIMED));
        tests.test("a_builder_replaces_a_column_from_its_upper_block_for_one_item_and_hands_back_what_it_names", 20,
                helper -> columnReplaced(helper, AIMED.above()));
        tests.test("a_builder_s_plan_names_both_blocks_replaced_and_both_placed", 20, FastReplaceTests::columnPlan);
        tests.test("a_builder_that_plans_nothing_leaves_the_click_to_vanilla", 20, helper ->
                passesToVanilla(helper, Blocks.GOLD_BLOCK, Items.IRON_BLOCK, false));
        tests.test("a_builder_decides_whether_a_block_replaces_its_own_kind", 20, FastReplaceTests::builderReplacesItsOwnKind);
        tests.test("a_builder_s_plan_takes_no_orientation_from_the_old_blocks", 20, FastReplaceTests::builderKeepsNoOrientation);
        tests.test("a_builder_plan_that_replaces_a_block_it_does_not_place_is_refused_and_changes_nothing", 20,
                FastReplaceTests::leavesAGap);
        tests.test("a_builder_s_own_refusal_cancels_the_click_and_the_player_is_told_its_reason", 20,
                FastReplaceTests::builderRefuses);
        tests.test("a_claim_on_any_block_of_a_builder_s_plan_refuses_the_replace", 20, FastReplaceTests::claimOnTheSecondBlock);

        // The vanilla Consumer's groups, block tags a pack developer fills, of which Groundworks ships none.
        tests.test("with_no_datapack_a_plain_click_with_concrete_on_concrete_places_beside", 20, helper ->
                passesToVanilla(helper, Blocks.RED_CONCRETE, Items.BLUE_CONCRETE, false));
        var grouped = tests.withPack(GROUPS_VANILLA_BLOCKS);
        grouped.test("a_datapack_tag_under_replace_group_makes_its_blocks_replace_one_another", 20,
                FastReplaceTests::tagGroupReplaces);
        // Red is in first, with blue, and in second, with yellow: it belongs to first, so yellow, in second alone, is not its group.
        grouped.test("a_block_in_two_replace_group_tags_belongs_to_the_first_by_id", 20, helper ->
                passesToVanilla(helper, Blocks.RED_CONCRETE, Items.YELLOW_CONCRETE, false));
        grouped.test("a_block_of_the_second_tag_alone_does_not_replace_one_in_both", 20, helper ->
                passesToVanilla(helper, Blocks.YELLOW_CONCRETE, Items.RED_CONCRETE, false));
        // Third holds green concrete and lime glazed terracotta, which a group stated in code claimed first.
        grouped.test("a_group_stated_in_code_claims_its_blocks_before_a_tag_does", 20, helper ->
                passesToVanilla(helper, Blocks.LIME_GLAZED_TERRACOTTA, Items.GREEN_CONCRETE, false));
    }

    /**
     * Gold blocks stand in a column of two, and an iron block replaces them with a click on either:
     * the plan swaps both, the click charges one iron block and hands back the one amethyst shard
     * the builder names, and not a gold block for each.
     */
    private static void columnReplaced(GameTestHelper helper, BlockPos aim) {
        setColumn(helper, Blocks.GOLD_BLOCK);
        ListeningPlayer player = holding(helper, new ItemStack(Items.IRON_BLOCK, 2));
        PlacementPlan plan = planAt(helper, player, aim);
        expectColumnPlan(helper, plan, Blocks.IRON_BLOCK);
        clickAt(helper, player, aim);
        expectColumn(helper, Blocks.IRON_BLOCK);
        expectSlot(helper, player, HELD_SLOT, Items.IRON_BLOCK, 1);
        expectHanded(helper, player, Items.AMETHYST_SHARD);
        if (player.getInventory().countItem(Items.GOLD_BLOCK) != 0) {
            helper.fail("the replace handed back " + player.getInventory().countItem(Items.GOLD_BLOCK) + " gold blocks");
        }
        if (!helper.getBlockState(AIMED.above(2)).isAir()) {
            helper.fail("the replace also placed " + helper.getBlockState(AIMED.above(2)), AIMED.above(2));
        }
        if (!player.heard.isEmpty()) {
            helper.fail("the player was told " + player.heard);
        }
        helper.succeed();
    }

    /** The plan the preview asks names both blocks of the column as replaced, so it draws both in the replace tint. */
    private static void columnPlan(GameTestHelper helper) {
        setColumn(helper, Blocks.GOLD_BLOCK);
        ListeningPlayer player = holding(helper, new ItemStack(Items.IRON_BLOCK, 2));
        expectColumnPlan(helper, planAt(helper, player, AIMED), Blocks.IRON_BLOCK);
        expectColumnPlan(helper, planAt(helper, player, AIMED.above()), Blocks.IRON_BLOCK);
        expectColumn(helper, Blocks.GOLD_BLOCK);
        helper.succeed();
    }

    /**
     * Without a builder a block is never replaced by itself; the builder decides, so an iron column
     * is replaced by an iron block, charged and handed back as any replace.
     */
    private static void builderReplacesItsOwnKind(GameTestHelper helper) {
        setColumn(helper, Blocks.IRON_BLOCK);
        ListeningPlayer player = holding(helper, new ItemStack(Items.IRON_BLOCK, 2));
        expectColumnPlan(helper, planAt(helper, player, AIMED), Blocks.IRON_BLOCK);
        clickAt(helper, player, AIMED);
        expectColumn(helper, Blocks.IRON_BLOCK);
        expectSlot(helper, player, HELD_SLOT, Items.IRON_BLOCK, 1);
        expectHanded(helper, player, Items.AMETHYST_SHARD);
        helper.succeed();
    }

    /**
     * The smokers face east, and a blast furnace whose plan took their facing would too: the
     * builder's plan is as it made it, facing north, in the plan the preview draws and in the world.
     */
    private static void builderKeepsNoOrientation(GameTestHelper helper) {
        BlockState smoker = Blocks.SMOKER.defaultBlockState().setValue(BlockStateProperties.HORIZONTAL_FACING, Direction.EAST);
        helper.setBlock(AIMED, smoker);
        helper.setBlock(AIMED.above(), smoker);
        ListeningPlayer player = holding(helper, new ItemStack(Items.BLAST_FURNACE, 2));
        PlacementPlan plan = planAt(helper, player, AIMED);
        expectColumnPlan(helper, plan, Blocks.BLAST_FURNACE);
        for (PlacementPlan.Placed placed : plan.blocks()) {
            if (placed.state().getValue(BlockStateProperties.HORIZONTAL_FACING) != Direction.NORTH) {
                helper.fail("the plan showed " + placed.state() + ", not facing north");
            }
        }
        clickAt(helper, player, AIMED);
        expectFacing(helper, AIMED, Blocks.BLAST_FURNACE, Direction.NORTH);
        expectFacing(helper, AIMED.above(), Blocks.BLAST_FURNACE, Direction.NORTH);
        helper.succeed();
    }

    /**
     * The builder replaces both copper blocks and places only the upper: a replace swaps and never
     * clears, so the plan is refused as leaving a gap, still drawn where its blocks go, and the
     * click tells so, never places beside the upper block it is aimed at, and changes nothing.
     */
    private static void leavesAGap(GameTestHelper helper) {
        setColumn(helper, Blocks.COPPER_BLOCK);
        ListeningPlayer player = holding(helper, new ItemStack(Items.LAPIS_BLOCK, 2));
        PlacementPlan plan = planAt(helper, player, AIMED.above());
        if (plan == null || plan.refusal() != Refusal.FastReplace.LEAVES_A_GAP || plan.blocks().size() != 1) {
            helper.fail("the plan was " + plan + ", not refused with " + Refusal.FastReplace.LEAVES_A_GAP);
        }
        clickAt(helper, player, AIMED.above());
        expectColumn(helper, Blocks.COPPER_BLOCK);
        if (!helper.getBlockState(AIMED.above(2)).isAir()) {
            helper.fail("the refused replace placed " + helper.getBlockState(AIMED.above(2)), AIMED.above(2));
        }
        expectSlot(helper, player, HELD_SLOT, Items.LAPIS_BLOCK, 2);
        expectNothingHanded(helper, player);
        if (!player.heard.equals(List.of("message.groundworks.fast_replace_leaves_a_gap"))) {
            helper.fail("the player was told " + player.heard);
        }
        helper.succeed();
    }

    /**
     * The builder refuses the emerald column with a reason of its own, standing at the aimed upper
     * block: the plan is drawn where its blocks go, the click is cancelled and never places beside,
     * the player is told what the builder says of the reason itself, and nothing changes.
     */
    private static void builderRefuses(GameTestHelper helper) {
        setColumn(helper, Blocks.EMERALD_BLOCK);
        ListeningPlayer player = holding(helper, new ItemStack(Items.REDSTONE_BLOCK, 2));
        PlacementPlan plan = planAt(helper, player, AIMED.above());
        if (plan == null || !(plan.refusal() instanceof Refusal.At at) || at.reason() != ReplacesAColumn.Refuses.NOT_THIS_COLUMN
                || plan.blocks().size() != 2 || plan.replaces().size() != 2) {
            helper.fail("the plan was " + plan + ", not the builder's refusal of the column");
        }
        clickAt(helper, player, AIMED.above());
        expectColumn(helper, Blocks.EMERALD_BLOCK);
        if (!helper.getBlockState(AIMED.above(2)).isAir()) {
            helper.fail("the refused replace placed " + helper.getBlockState(AIMED.above(2)), AIMED.above(2));
        }
        expectSlot(helper, player, HELD_SLOT, Items.REDSTONE_BLOCK, 2);
        expectNothingHanded(helper, player);
        if (!player.heard.equals(List.of(ReplacesAColumn.REFUSED))) {
            helper.fail("the player was told " + player.heard + ", expected " + ReplacesAColumn.REFUSED);
        }
        helper.succeed();
    }

    /**
     * A claim covers the upper block of the column alone, which the click is not aimed at: the plan
     * goes through, and the click, which asks the place event at each block it puts down, is
     * refused and changes nothing.
     */
    private static void claimOnTheSecondBlock(GameTestHelper helper) {
        setColumn(helper, Blocks.GOLD_BLOCK);
        ListeningPlayer player = holding(helper, new ItemStack(Items.IRON_BLOCK, 2));
        whileClaimed(helper, AIMED.above(), () -> clickAt(helper, player, AIMED));
        expectColumn(helper, Blocks.GOLD_BLOCK);
        expectSlot(helper, player, HELD_SLOT, Items.IRON_BLOCK, 2);
        expectNothingHanded(helper, player);
        if (!player.heard.equals(List.of("message.groundworks.fast_replace_may_not_build"))) {
            helper.fail("the player was told " + player.heard);
        }
        helper.succeed();
    }

    private static void setColumn(GameTestHelper helper, Block block) {
        helper.setBlock(AIMED, block);
        helper.setBlock(AIMED.above(), block);
    }

    private static void expectColumn(GameTestHelper helper, Block block) {
        expectBlock(helper, AIMED, block);
        expectBlock(helper, AIMED.above(), block);
    }

    private static void expectBlock(GameTestHelper helper, BlockPos pos, Block block) {
        if (!helper.getBlockState(pos).is(block)) {
            helper.fail("expected " + block + ", found " + helper.getBlockState(pos), pos);
        }
    }

    /** Fails unless the plan replaces and places both blocks of the column, as {@code block}, and would go through. */
    private static void expectColumnPlan(GameTestHelper helper, @Nullable PlacementPlan plan, Block block) {
        List<BlockPos> column = List.of(helper.absolutePos(AIMED), helper.absolutePos(AIMED.above()));
        if (plan == null || plan.isRefused() || !plan.replaces().equals(column)
                || !plan.blocks().stream().map(PlacementPlan.Placed::pos).toList().equals(column)
                || !plan.blocks().stream().allMatch(placed -> placed.state().is(block))) {
            helper.fail("the plan was " + plan + ", not a replace of both blocks of the column with " + block);
        }
    }

    /** What the refusal handed back: nothing at all. */
    private static void expectNothingHanded(GameTestHelper helper, ListeningPlayer player) {
        if (player.getInventory().countItem(Items.AMETHYST_SHARD) != 0) {
            helper.fail("the refused replace handed back " + player.getInventory().countItem(Items.AMETHYST_SHARD) + " amethyst shards");
        }
    }

    /** A blue concrete replaces a red one, as the tag {@code first} groups them, charged and handed back. */
    private static void tagGroupReplaces(GameTestHelper helper) {
        helper.setBlock(AIMED, Blocks.RED_CONCRETE);
        ListeningPlayer player = holding(helper, new ItemStack(Items.BLUE_CONCRETE, 2));
        replaceAsPlanned(helper, player, Blocks.BLUE_CONCRETE);
        expectSlot(helper, player, HELD_SLOT, Items.BLUE_CONCRETE, 1);
        expectHanded(helper, player, Items.RED_CONCRETE);
        helper.succeed();
    }

    private static void replaces(GameTestHelper helper) {
        helper.setBlock(AIMED, Blocks.PINK_GLAZED_TERRACOTTA);
        ListeningPlayer player = holding(helper, new ItemStack(Items.LIME_GLAZED_TERRACOTTA, 2));
        replaceAsPlanned(helper, player, Blocks.LIME_GLAZED_TERRACOTTA);
        expectSlot(helper, player, HELD_SLOT, Items.LIME_GLAZED_TERRACOTTA, 1);
        expectHanded(helper, player, Items.PINK_GLAZED_TERRACOTTA);
        helper.succeed();
    }

    private static void emptiesTheHeldStack(GameTestHelper helper) {
        helper.setBlock(AIMED, Blocks.PINK_GLAZED_TERRACOTTA);
        ListeningPlayer player = holding(helper, new ItemStack(Items.LIME_GLAZED_TERRACOTTA, 1));
        replaceAsPlanned(helper, player, Blocks.LIME_GLAZED_TERRACOTTA);
        expectSlot(helper, player, HELD_SLOT, Items.PINK_GLAZED_TERRACOTTA, 1);
        helper.succeed();
    }

    private static void itemThatPlansItsOwnPlacement(GameTestHelper helper) {
        helper.setBlock(AIMED, Blocks.LIME_GLAZED_TERRACOTTA);
        ListeningPlayer player = holding(helper, new ItemStack(GroundworksGameTests.PLANS_PINK.get(), 2));
        replaceAsPlanned(helper, player, Blocks.PINK_GLAZED_TERRACOTTA);
        expectSlot(helper, player, HELD_SLOT, GroundworksGameTests.PLANS_PINK.get(), 1);
        expectHanded(helper, player, Items.LIME_GLAZED_TERRACOTTA);
        helper.succeed();
    }

    private static void sneakPlacesBeside(GameTestHelper helper) {
        passesToVanilla(helper, Blocks.PINK_GLAZED_TERRACOTTA, Items.LIME_GLAZED_TERRACOTTA, true);
    }

    private static void aimedInNoGroup(GameTestHelper helper) {
        passesToVanilla(helper, Blocks.COBBLESTONE, Items.LIME_GLAZED_TERRACOTTA, false);
    }

    private static void heldInAnotherGroup(GameTestHelper helper) {
        passesToVanilla(helper, Blocks.PINK_GLAZED_TERRACOTTA, Items.GRAY_GLAZED_TERRACOTTA, false);
    }

    private static void heldInNoGroup(GameTestHelper helper) {
        passesToVanilla(helper, Blocks.PINK_GLAZED_TERRACOTTA, Items.COBBLESTONE, false);
    }

    private static void sameBlock(GameTestHelper helper) {
        passesToVanilla(helper, Blocks.LIME_GLAZED_TERRACOTTA, Items.LIME_GLAZED_TERRACOTTA, false);
    }

    /** The stretch's item places magenta glazed terracotta, in the same group as the pink it is aimed at. */
    private static void stretchStartStored(GameTestHelper helper) {
        helper.setBlock(AIMED, Blocks.PINK_GLAZED_TERRACOTTA);
        ListeningPlayer player = holding(helper, new ItemStack(GroundworksGameTests.STRETCHES_ARROWS.get(), 16));
        player.setShiftKeyDown(true);
        player.gameMode.useItemOn(player, helper.getLevel(), player.getMainHandItem(), InteractionHand.MAIN_HAND,
                onTop(helper, new BlockPos(1, 0, 4)));
        player.setShiftKeyDown(false);
        if (!player.getMainHandItem().has(Groundworks.STRETCH.get())) {
            helper.fail("the sneak-click stored no stretch start");
        }
        PlacementPlan plan = plan(helper, player);
        if (plan != null && plan.replaces().equals(List.of(helper.absolutePos(AIMED)))) {
            helper.fail("with a start stored, the plan was a Fast Replace: " + plan);
        }
        click(helper, player, false);
        if (!helper.getBlockState(AIMED).is(Blocks.PINK_GLAZED_TERRACOTTA)) {
            helper.fail("with a start stored, the click replaced the pink with " + helper.getBlockState(AIMED), AIMED);
        }
        helper.succeed();
    }

    private static void heightHeld(GameTestHelper helper) {
        helper.setBlock(AIMED, Blocks.PINK_GLAZED_TERRACOTTA);
        ListeningPlayer player = holding(helper, new ItemStack(Items.LIME_GLAZED_TERRACOTTA, 2));
        Raise.press(player, false);
        if (!Raise.heightOf(player, player.getMainHandItem()).equals(new Height(1))) {
            helper.fail("Raise held no height: " + Raise.heightOf(player, player.getMainHandItem()));
        }
        replaceAsPlanned(helper, player, Blocks.LIME_GLAZED_TERRACOTTA);
        Height height = Raise.heightOf(player, player.getMainHandItem());
        if (!height.equals(new Height(1))) {
            helper.fail("the replace left the stack's height at " + height);
        }
        helper.succeed();
    }

    private static void noRoomToReturn(GameTestHelper helper) {
        helper.setBlock(AIMED, Blocks.PINK_GLAZED_TERRACOTTA);
        ListeningPlayer player = holding(helper, new ItemStack(Items.LIME_GLAZED_TERRACOTTA, 2));
        fillInventory(player);
        expectRefusedWhole(helper, player, Refusal.FastReplace.NO_ROOM_TO_RETURN,
                "message.groundworks.fast_replace_no_room_to_return");
        helper.succeed();
    }

    /** The player is in adventure mode, so the plan is refused, and the click tells so and changes nothing. */
    private static void mayNotBuild(GameTestHelper helper) {
        helper.setBlock(AIMED, Blocks.PINK_GLAZED_TERRACOTTA);
        ListeningPlayer player = holding(helper, new ItemStack(Items.LIME_GLAZED_TERRACOTTA, 2));
        player.setGameMode(GameType.ADVENTURE);
        expectRefusedWhole(helper, player, Refusal.FastReplace.MAY_NOT_BUILD, "message.groundworks.fast_replace_may_not_build");
        helper.succeed();
    }

    /**
     * A claim mod cancels the place event at the aimed block alone: the plan, which fires no event,
     * still goes through, and the click is refused, told so, and changes nothing. Out of the claim
     * the same click replaces.
     */
    private static void claimGuards(GameTestHelper helper) {
        helper.setBlock(AIMED, Blocks.PINK_GLAZED_TERRACOTTA);
        ListeningPlayer player = holding(helper, new ItemStack(Items.LIME_GLAZED_TERRACOTTA, 2));
        whileClaimed(helper, () -> {
            PlacementPlan plan = plan(helper, player);
            if (plan == null || plan.isRefused()) {
                helper.fail("in a claim the plan was " + plan + ", not a replace that goes through");
            }
            click(helper, player, false);
        });
        expectUntouched(helper, Blocks.PINK_GLAZED_TERRACOTTA);
        expectNothingBeside(helper);
        expectSlot(helper, player, HELD_SLOT, Items.LIME_GLAZED_TERRACOTTA, 2);
        if (!player.heard.equals(List.of("message.groundworks.fast_replace_may_not_build"))) {
            helper.fail("the player was told " + player.heard);
        }
        replaceAsPlanned(helper, player, Blocks.LIME_GLAZED_TERRACOTTA);
        helper.succeed();
    }

    /**
     * A replace already refused, for no room, is not one the claim mod is asked about, as Rotate
     * asks only of a turn it will make: the player is told there is no room, and no place event
     * that a claim mod or a logger hears is fired for a block that never goes down.
     */
    private static void refusedFiresNoPlaceEvent(GameTestHelper helper) {
        helper.setBlock(AIMED, Blocks.PINK_GLAZED_TERRACOTTA);
        ListeningPlayer player = holding(helper, new ItemStack(Items.LIME_GLAZED_TERRACOTTA, 2));
        fillInventory(player);
        whileClaimed(helper, () -> click(helper, player, false));
        expectUntouched(helper, Blocks.PINK_GLAZED_TERRACOTTA);
        if (!player.heard.equals(List.of("message.groundworks.fast_replace_no_room_to_return"))) {
            helper.fail("the player was told " + player.heard);
        }
        helper.succeed();
    }

    /** An entity in the new block's way refuses, as vanilla refuses a placement: with the item's own refusal. */
    private static void entityInTheWay(GameTestHelper helper) {
        helper.setBlock(AIMED, Blocks.PINK_GLAZED_TERRACOTTA);
        Mob pig = helper.spawn(EntityType.PIG, AIMED);
        pig.setNoAi(true);
        pig.setNoGravity(true);
        pig.setInvulnerable(true);
        ListeningPlayer player = holding(helper, new ItemStack(Items.LIME_GLAZED_TERRACOTTA, 2));
        expectRefusedWhole(helper, player, Refusal.Vanilla.VANILLA, "message.groundworks.fast_replace_refused");
        helper.succeed();
    }

    /** A creative player's replace swaps the block for nothing, and hands nothing back to a full inventory. */
    private static void creativeReplaces(GameTestHelper helper) {
        helper.setBlock(AIMED, Blocks.PINK_GLAZED_TERRACOTTA);
        ListeningPlayer player = holding(helper, new ItemStack(Items.LIME_GLAZED_TERRACOTTA, 2));
        player.setGameMode(GameType.CREATIVE);
        fillInventory(player);
        replaceAsPlanned(helper, player, Blocks.LIME_GLAZED_TERRACOTTA);
        expectSlot(helper, player, HELD_SLOT, Items.LIME_GLAZED_TERRACOTTA, 2);
        if (player.getInventory().countItem(Items.PINK_GLAZED_TERRACOTTA) != 0) {
            helper.fail("the replace handed back " + player.getInventory().countItem(Items.PINK_GLAZED_TERRACOTTA) + " pink");
        }
        if (!player.heard.isEmpty()) {
            helper.fail("the player was told " + player.heard);
        }
        helper.succeed();
    }

    /**
     * The player looks north, where a fresh lime would face south, and the pink faces east: the lime
     * takes the pink's facing, and the plan the preview draws already shows it.
     */
    private static void keepsFacing(GameTestHelper helper) {
        helper.setBlock(AIMED, Blocks.PINK_GLAZED_TERRACOTTA.defaultBlockState().setValue(BlockStateProperties.HORIZONTAL_FACING, Direction.EAST));
        expectFacing(helper, AIMED, Blocks.PINK_GLAZED_TERRACOTTA, Direction.EAST);
        ListeningPlayer player = holding(helper, new ItemStack(Items.LIME_GLAZED_TERRACOTTA, 2));
        look(player, Direction.NORTH);
        PlacementPlan plan = replaceAsPlanned(helper, player, Blocks.LIME_GLAZED_TERRACOTTA);
        if (plan.blocks().getFirst().state().getValue(BlockStateProperties.HORIZONTAL_FACING) != Direction.EAST) {
            helper.fail("the plan showed " + plan.blocks().getFirst().state() + ", not facing east");
        }
        expectFacing(helper, AIMED, Blocks.LIME_GLAZED_TERRACOTTA, Direction.EAST);
        helper.succeed();
    }

    /**
     * With a quarter turn held the copy is skipped. The player looks east, so the turned look is
     * south, and glazed terracotta faces against the look: north. The pink faces east, and the turn
     * stays on the stack.
     */
    private static void turnOverridesTheCopy(GameTestHelper helper) {
        helper.setBlock(AIMED, Blocks.PINK_GLAZED_TERRACOTTA.defaultBlockState().setValue(BlockStateProperties.HORIZONTAL_FACING, Direction.EAST));
        expectFacing(helper, AIMED, Blocks.PINK_GLAZED_TERRACOTTA, Direction.EAST);
        ListeningPlayer player = holding(helper, new ItemStack(Items.LIME_GLAZED_TERRACOTTA, 2));
        Rotate.press(player, null, false);
        // A fresh lime on empty ground, with the same turn, faces the same way: the cross-check.
        BlockPos ground = new BlockPos(4, 0, 2);
        PlacementPlan fresh = Placements.planFor(player.level(), player, InteractionHand.MAIN_HAND, player.getMainHandItem(),
                onTop(helper, ground));
        if (fresh == null || fresh.blocks().getFirst().state().getValue(BlockStateProperties.HORIZONTAL_FACING) != Direction.NORTH) {
            helper.fail("a fresh lime with a quarter turn planned " + fresh + ", not facing north");
        }
        PlacementPlan plan = replaceAsPlanned(helper, player, Blocks.LIME_GLAZED_TERRACOTTA);
        if (plan.blocks().getFirst().state().getValue(BlockStateProperties.HORIZONTAL_FACING) != Direction.NORTH) {
            helper.fail("the plan showed " + plan.blocks().getFirst().state() + ", not facing north");
        }
        expectFacing(helper, AIMED, Blocks.LIME_GLAZED_TERRACOTTA, Direction.NORTH);
        if (!Rotate.turnOf(player.getMainHandItem()).equals(QuarterTurn.of(1))) {
            helper.fail("the turn on the stack is " + Rotate.turnOf(player.getMainHandItem()) + ", not a quarter");
        }
        helper.succeed();
    }

    /**
     * A double birch slab's type is no orientation, so the jungle slab is the plan's, a bottom slab
     * for a click on the top face: one held slab never buys a double one.
     */
    private static void doubleSlabIsNotCopied(GameTestHelper helper) {
        helper.setBlock(AIMED, Blocks.BIRCH_SLAB.defaultBlockState().setValue(SlabBlock.TYPE, SlabType.DOUBLE));
        ListeningPlayer player = holding(helper, new ItemStack(Items.JUNGLE_SLAB, 2));
        PlacementPlan plan = replaceAsPlanned(helper, player, Blocks.JUNGLE_SLAB);
        if (plan.blocks().getFirst().state().getValue(SlabBlock.TYPE) != SlabType.BOTTOM
                || helper.getBlockState(AIMED).getValue(SlabBlock.TYPE) != SlabType.BOTTOM) {
            helper.fail("the plan put " + plan.blocks().getFirst().state() + " and the click " + helper.getBlockState(AIMED)
                    + ", not a bottom slab", AIMED);
        }
        helper.succeed();
    }

    /** A birch log's axis is an orientation no slab has, so the slab is the plan's, and the replace goes through. */
    private static void orientationTheNewBlockLacks(GameTestHelper helper) {
        helper.setBlock(AIMED, Blocks.BIRCH_LOG.defaultBlockState().setValue(BlockStateProperties.AXIS, Direction.Axis.X));
        ListeningPlayer player = holding(helper, new ItemStack(Items.JUNGLE_SLAB, 2));
        replaceAsPlanned(helper, player, Blocks.JUNGLE_SLAB);
        if (helper.getBlockState(AIMED).getValue(SlabBlock.TYPE) != SlabType.BOTTOM) {
            helper.fail("the click put " + helper.getBlockState(AIMED) + ", not a bottom slab", AIMED);
        }
        helper.succeed();
    }

    private static void expectFacing(GameTestHelper helper, BlockPos pos, Block block, Direction facing) {
        BlockState there = helper.getBlockState(pos);
        if (!there.is(block) || there.getValue(BlockStateProperties.HORIZONTAL_FACING) != facing) {
            helper.fail(there + " is not " + block + " facing " + facing, pos);
        }
    }

    /**
     * Fails unless the plan is refused for {@code reason}, and the click, which tells the player
     * {@code message}, changes nothing: the aimed block stays, nothing is placed beside it, and the
     * held stack is whole.
     */
    private static void expectRefusedWhole(GameTestHelper helper, ListeningPlayer player, Refusal reason, String message) {
        PlacementPlan plan = plan(helper, player);
        if (plan == null || plan.refusal() != reason) {
            helper.fail("the plan was " + plan + ", not refused with " + reason);
        }
        click(helper, player, false);
        expectUntouched(helper, Blocks.PINK_GLAZED_TERRACOTTA);
        expectNothingBeside(helper);
        expectSlot(helper, player, HELD_SLOT, Items.LIME_GLAZED_TERRACOTTA, 2);
        if (!player.heard.equals(List.of(message))) {
            helper.fail("the player was told " + player.heard + ", expected " + message);
        }
    }

    /** What vanilla would have done with the click, had the replace let it fall through: a block on top. */
    private static void expectNothingBeside(GameTestHelper helper) {
        if (!helper.getBlockState(AIMED.above()).isAir()) {
            helper.fail("the refused replace placed " + helper.getBlockState(AIMED.above()) + " on top", AIMED.above());
        }
    }

    /** Runs {@code body} with a claim on the aimed block, which cancels the place event there as a claim mod does. */
    private static void whileClaimed(GameTestHelper helper, Runnable body) {
        whileClaimed(helper, AIMED, body);
    }

    /** The same, for the block at {@code pos}. */
    private static void whileClaimed(GameTestHelper helper, BlockPos pos, Runnable body) {
        BlockPos claimed = helper.absolutePos(pos);
        RotateInPlaceTests.CLAIMED.add(claimed);
        try {
            body.run();
        } finally {
            RotateInPlaceTests.CLAIMED.remove(claimed);
        }
    }

    private static void fillInventory(ListeningPlayer player) {
        var inventory = player.getInventory().getNonEquipmentItems();
        for (int slot = 0; slot < inventory.size(); slot++) {
            if (inventory.get(slot).isEmpty()) {
                inventory.set(slot, new ItemStack(Items.DIRT, 64));
            }
        }
    }

    /**
     * Asks the plan, clicks, and fails unless the plan replaced the aimed block alone with the held
     * block and the click put down exactly the planned state there, and nothing above it. Returns the plan.
     */
    private static PlacementPlan replaceAsPlanned(GameTestHelper helper, ListeningPlayer player, Block held) {
        BlockPos aimed = helper.absolutePos(AIMED);
        PlacementPlan plan = plan(helper, player);
        if (plan == null || plan.isRefused() || !plan.replaces().equals(List.of(aimed)) || plan.blocks().size() != 1
                || !plan.blocks().getFirst().pos().equals(aimed) || !plan.blocks().getFirst().state().is(held)) {
            helper.fail("the plan was " + plan + ", not a replace of the aimed block with " + held);
        }
        click(helper, player, false);
        if (!helper.getBlockState(AIMED).equals(plan.blocks().getFirst().state())) {
            helper.fail("the plan put " + plan.blocks().getFirst().state() + ", the click put " + helper.getBlockState(AIMED), AIMED);
        }
        if (!helper.getBlockState(AIMED.above()).isAir()) {
            helper.fail("the replace also placed " + helper.getBlockState(AIMED.above()), AIMED.above());
        }
        return plan;
    }

    /** The click does what vanilla does: the aimed block stays and the held block goes on top of it. */
    private static void passesToVanilla(GameTestHelper helper, Block aimed, Item held, boolean sneaking) {
        helper.setBlock(AIMED, aimed);
        ListeningPlayer player = holding(helper, new ItemStack(held, 2));
        player.setShiftKeyDown(sneaking);
        PlacementPlan plan = plan(helper, player);
        player.setShiftKeyDown(false);
        if (plan != null && plan.isReplace()) {
            helper.fail("the plan was a replace: " + plan);
        }
        click(helper, player, sneaking);
        expectUntouched(helper, aimed);
        if (!helper.getBlockState(AIMED.above()).is(((BlockItem) held).getBlock())) {
            helper.fail("vanilla placed " + helper.getBlockState(AIMED.above()) + " on top", AIMED.above());
        }
        expectSlot(helper, player, HELD_SLOT, held, 1);
        helper.succeed();
    }

    private static void expectUntouched(GameTestHelper helper, Block aimed) {
        if (!helper.getBlockState(AIMED).is(aimed)) {
            helper.fail("the aimed " + aimed + " became " + helper.getBlockState(AIMED), AIMED);
        }
    }

    private static void expectSlot(GameTestHelper helper, ListeningPlayer player, int slot, Item item, int count) {
        ItemStack there = player.getInventory().getItem(slot);
        if (!there.is(item) || there.getCount() != count) {
            helper.fail("slot " + slot + " holds " + there + ", expected " + count + " " + item);
        }
    }

    private static void expectHanded(GameTestHelper helper, ListeningPlayer player, Item item) {
        if (player.getInventory().countItem(item) != 1) {
            helper.fail("the inventory holds " + player.getInventory().countItem(item) + " " + item + ", expected 1");
        }
    }

    private static @Nullable PlacementPlan plan(GameTestHelper helper, ListeningPlayer player) {
        return planAt(helper, player, AIMED);
    }

    private static @Nullable PlacementPlan planAt(GameTestHelper helper, ListeningPlayer player, BlockPos aim) {
        return Placements.planFor(player.level(), player, InteractionHand.MAIN_HAND, player.getMainHandItem(), onTop(helper, aim));
    }

    private static void click(GameTestHelper helper, ListeningPlayer player, boolean sneaking) {
        player.setShiftKeyDown(sneaking);
        clickAt(helper, player, AIMED);
        player.setShiftKeyDown(false);
    }

    private static void clickAt(GameTestHelper helper, ListeningPlayer player, BlockPos aim) {
        player.gameMode.useItemOn(player, helper.getLevel(), player.getMainHandItem(), InteractionHand.MAIN_HAND,
                onTop(helper, aim));
    }

    private static void look(ListeningPlayer player, Direction look) {
        player.setYRot(look.toYRot());
        player.setYHeadRot(look.toYRot());
    }

    private static ListeningPlayer holding(GameTestHelper helper, ItemStack stack) {
        ListeningPlayer player = new ListeningPlayer(helper, new BlockPos(0, 1, 2));
        look(player, Direction.EAST);
        player.getInventory().setSelectedSlot(HELD_SLOT);
        player.setItemInHand(InteractionHand.MAIN_HAND, stack);
        return player;
    }

    private static BlockHitResult onTop(GameTestHelper helper, BlockPos pos) {
        BlockPos absolute = helper.absolutePos(pos);
        return new BlockHitResult(Vec3.atCenterOf(absolute).relative(Direction.UP, 0.5), Direction.UP, absolute, false);
    }

    private static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(Groundworks.MOD_ID, path);
    }
}
