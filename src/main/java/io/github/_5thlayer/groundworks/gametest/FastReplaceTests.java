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
import io.github._5thlayer.groundworks.Refusal;
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
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * Fast Replace on the tests' own {@linkplain #stateGroups Replace groups}: lime, pink and magenta
 * glazed terracotta replace one another, and gray is a group of its own. Every click goes through
 * the server player's own {@code useItemOn}, so through the event Groundworks answers, and every
 * plan through {@link Placements#planFor}, as the preview asks it.
 *
 * <p>Each test replaces the block at {@link #AIMED}, aimed at its top, with the player standing
 * west of it looking east and holding the item in hotbar slot 3.
 */
final class FastReplaceTests {

    private static final BlockPos AIMED = new BlockPos(2, 1, 2);
    private static final int HELD_SLOT = 3;

    private FastReplaceTests() {
    }

    /** The tests' groups, stated at mod construction as a Consumer's are. */
    static void stateGroups() {
        FastReplace.group(id("gametest_glazed"), block -> block == Blocks.LIME_GLAZED_TERRACOTTA
                || block == Blocks.PINK_GLAZED_TERRACOTTA || block == Blocks.MAGENTA_GLAZED_TERRACOTTA);
        FastReplace.group(id("gametest_gray"), block -> block == Blocks.GRAY_GLAZED_TERRACOTTA);
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
        BlockPos claimed = helper.absolutePos(AIMED);
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
     * block and the click put down exactly the planned state there, and nothing above it.
     */
    private static void replaceAsPlanned(GameTestHelper helper, ListeningPlayer player, Block held) {
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
        return Placements.planFor(player.level(), player, InteractionHand.MAIN_HAND, player.getMainHandItem(), onTop(helper, AIMED));
    }

    private static void click(GameTestHelper helper, ListeningPlayer player, boolean sneaking) {
        player.setShiftKeyDown(sneaking);
        player.gameMode.useItemOn(player, helper.getLevel(), player.getMainHandItem(), InteractionHand.MAIN_HAND,
                onTop(helper, AIMED));
        player.setShiftKeyDown(false);
    }

    private static ListeningPlayer holding(GameTestHelper helper, ItemStack stack) {
        ListeningPlayer player = new ListeningPlayer(helper, new BlockPos(0, 1, 2));
        player.setYRot(Direction.EAST.toYRot());
        player.setYHeadRot(Direction.EAST.toYRot());
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
