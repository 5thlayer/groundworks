// SPDX-FileCopyrightText: 2026 5thlayer
// SPDX-License-Identifier: MIT

package io.github._5thlayer.groundworks.gametest;

import java.util.List;

import io.github._5thlayer.groundworks.BlueprintSummary;
import io.github._5thlayer.groundworks.Blueprints;
import io.github._5thlayer.groundworks.Groundworks;
import io.github._5thlayer.groundworks.NextChoice;
import io.github._5thlayer.groundworks.SheetChoice;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Vec3i;
import net.minecraft.core.component.DataComponents;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.EventHooks;
import org.jspecify.annotations.Nullable;

/**
 * A stamped sheet is told from paper (#87): named Blueprint, glinting, and carrying the summary
 * its tooltip says, all gone when the sheet is blank again; one stamped before that catches up. The box from {@link #FIRST} to {@link
 * #SECOND} holds two stone blocks and six air, on the tall platform.
 */
final class BlueprintSheetTests {

    private static final BlockPos FIRST = new BlockPos(1, 1, 1);
    private static final BlockPos SECOND = new BlockPos(2, 2, 2);
    private static final BlockPos SPOT = new BlockPos(7, 1, 7);
    private static final BlueprintSummary TWO_STONE = new BlueprintSummary(false, 2, new Vec3i(2, 2, 2));

    private BlueprintSheetTests() {
    }

    static void register(GroundworksGameTests.Registrar tests) {
        tests.tallTest("blueprint_sheet_a_copy_is_named_blueprint_glints_and_says_copy_and_its_size", 20,
                BlueprintSheetTests::copyIsMarked);
        tests.tallTest("blueprint_sheet_a_cut_is_named_blueprint_glints_and_says_cut_and_its_size", 20,
                BlueprintSheetTests::cutIsMarked);
        tests.tallTest("blueprint_sheet_clearing_a_copy_leaves_no_stamp", 20, BlueprintSheetTests::clearedIsPlain);
        tests.tallTest("blueprint_sheet_pasting_a_cut_leaves_no_stamp", 20, BlueprintSheetTests::pastedCutIsPlain);
        tests.tallTest("blueprint_sheet_next_choice_says_pasting_air_and_leaves_the_summary", 20,
                BlueprintSheetTests::pastingAirIsSaid);
        tests.tallTest("blueprint_sheet_stamped_with_only_an_id_catches_up_when_held_keeping_its_choice", 20,
                BlueprintSheetTests::catchesUpWhenHeld);
        tests.tallTest("blueprint_sheet_blank_with_a_corner_says_where_and_is_not_stamped", 20,
                BlueprintSheetTests::cornerIsSaid);
    }

    private static void copyIsMarked(GameTestHelper helper) {
        ListeningPlayer player = holding(helper);
        twoStone(helper);
        capture(helper, player);
        expectStamped(helper, player.getMainHandItem(), TWO_STONE);
        expectTooltip(helper, player.getMainHandItem(), List.of("tooltip.groundworks.blueprint_copy",
                "tooltip.groundworks.blueprint_size"));
        helper.succeed();
    }

    private static void cutIsMarked(GameTestHelper helper) {
        ListeningPlayer player = holding(helper);
        twoStone(helper);
        NextChoice.press(player, false);
        capture(helper, player);
        expectStamped(helper, player.getMainHandItem(), new BlueprintSummary(true, 2, new Vec3i(2, 2, 2)));
        expectTooltip(helper, player.getMainHandItem(), List.of("tooltip.groundworks.blueprint_cut",
                "tooltip.groundworks.blueprint_size"));
        helper.succeed();
    }

    private static void clearedIsPlain(GameTestHelper helper) {
        ListeningPlayer player = holding(helper);
        twoStone(helper);
        capture(helper, player);
        player.setShiftKeyDown(true);
        player.gameMode.useItem(player, helper.getLevel(), player.getMainHandItem(), InteractionHand.MAIN_HAND);
        player.setShiftKeyDown(false);
        expectPlain(helper, player.getMainHandItem());
        helper.succeed();
    }

    private static void pastedCutIsPlain(GameTestHelper helper) {
        ListeningPlayer player = holding(helper);
        twoStone(helper);
        NextChoice.press(player, false);
        capture(helper, player);
        click(helper, player, SPOT.below());
        helper.assertBlockPresent(Blocks.STONE, SPOT.offset(-1, 0, -1));
        expectPlain(helper, player.getMainHandItem());
        helper.succeed();
    }

    private static void pastingAirIsSaid(GameTestHelper helper) {
        ListeningPlayer player = holding(helper);
        twoStone(helper);
        capture(helper, player);
        NextChoice.press(player, false);
        expectStamped(helper, player.getMainHandItem(), TWO_STONE);
        if (player.getMainHandItem().get(Groundworks.SHEET_CHOICE.get()) != SheetChoice.PASTE_AIR) {
            helper.fail("the sheet stores " + player.getMainHandItem().get(Groundworks.SHEET_CHOICE.get()));
        }
        expectTooltip(helper, player.getMainHandItem(), List.of("tooltip.groundworks.blueprint_copy",
                "tooltip.groundworks.blueprint_size", "tooltip.groundworks.blueprint_pasting_air"));
        NextChoice.press(player, false);
        expectTooltip(helper, player.getMainHandItem(), List.of("tooltip.groundworks.blueprint_copy",
                "tooltip.groundworks.blueprint_size"));
        helper.succeed();
    }

    /** A sheet as 0.7.0 stamped it, the id and a choice alone on it, is stamped on its first hold, its choice kept. */
    private static void catchesUpWhenHeld(GameTestHelper helper) {
        ListeningPlayer player = holding(helper);
        twoStone(helper);
        capture(helper, player);
        ItemStack old = new ItemStack(Items.PAPER);
        old.set(Groundworks.BLUEPRINT.get(), Blueprints.heldId(player.getMainHandItem()));
        old.set(Groundworks.SHEET_CHOICE.get(), SheetChoice.PASTE_AIR);
        old.set(DataComponents.MAX_STACK_SIZE, 1);
        player.setItemInHand(InteractionHand.MAIN_HAND, old);
        EventHooks.firePlayerTickPost(player);
        expectStamped(helper, player.getMainHandItem(), TWO_STONE);
        if (player.getMainHandItem().get(Groundworks.SHEET_CHOICE.get()) != SheetChoice.PASTE_AIR) {
            helper.fail("the catch-up dropped the sheet's choice");
        }
        helper.succeed();
    }

    private static void cornerIsSaid(GameTestHelper helper) {
        ListeningPlayer player = holding(helper);
        player.setShiftKeyDown(true);
        click(helper, player, FIRST);
        player.setShiftKeyDown(false);
        ItemStack sheet = player.getMainHandItem();
        if (sheet.has(DataComponents.ENCHANTMENT_GLINT_OVERRIDE) || sheet.has(Groundworks.BLUEPRINT_SUMMARY.get())
                || !sheet.getHoverName().equals(new ItemStack(Items.PAPER).getHoverName())) {
            helper.fail("a blank sheet with a Corner is stamped: " + sheet.getComponentsPatch());
        }
        expectTooltip(helper, sheet, List.of("tooltip.groundworks.blueprint_corner"));
        helper.succeed();
    }

    // ---- Helpers --------------------------------------------------------------------------------

    private static void twoStone(GameTestHelper helper) {
        helper.setBlock(FIRST, Blocks.STONE);
        helper.setBlock(SECOND, Blocks.STONE);
    }

    private static void capture(GameTestHelper helper, ListeningPlayer player) {
        player.setShiftKeyDown(true);
        click(helper, player, FIRST);
        player.setShiftKeyDown(false);
        click(helper, player, SECOND);
        if (!Blueprints.holdsBlueprint(player.getMainHandItem())) {
            helper.fail("the capture left the sheet blank; the player heard " + player.heard);
        }
    }

    private static void click(GameTestHelper helper, ListeningPlayer player, BlockPos pos) {
        BlockPos absolute = helper.absolutePos(pos);
        player.gameMode.useItemOn(player, helper.getLevel(), player.getMainHandItem(), InteractionHand.MAIN_HAND,
                new BlockHitResult(Vec3.atCenterOf(absolute).relative(Direction.UP, 0.5), Direction.UP, absolute,
                        false));
    }

    private static ListeningPlayer holding(GameTestHelper helper) {
        ListeningPlayer player = new ListeningPlayer(helper, new BlockPos(5, 1, 0));
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.PAPER));
        return player;
    }

    private static void expectStamped(GameTestHelper helper, ItemStack sheet, BlueprintSummary summary) {
        if (!Boolean.TRUE.equals(sheet.get(DataComponents.ENCHANTMENT_GLINT_OVERRIDE))) {
            helper.fail("the sheet doesn't glint: " + sheet.getComponentsPatch());
        }
        if (!"item.groundworks.blueprint".equals(key(sheet.get(DataComponents.ITEM_NAME)))) {
            helper.fail("the sheet is named " + sheet.getHoverName().getString() + ", not Blueprint");
        }
        if (!summary.equals(sheet.get(Groundworks.BLUEPRINT_SUMMARY.get()))) {
            helper.fail("the sheet's summary is " + sheet.get(Groundworks.BLUEPRINT_SUMMARY.get()) + ", not " + summary);
        }
        if (sheet.getMaxStackSize() != 1) {
            helper.fail("the sheet stacks to " + sheet.getMaxStackSize());
        }
    }

    private static void expectPlain(GameTestHelper helper, ItemStack sheet) {
        if (!ItemStack.isSameItemSameComponents(sheet, new ItemStack(Items.PAPER))) {
            helper.fail("the sheet is " + sheet.getComponentsPatch() + ", not plain paper");
        }
    }

    private static void expectTooltip(GameTestHelper helper, ItemStack sheet, List<String> keys) {
        List<String> said = Blueprints.tooltipOf(sheet).stream().map(BlueprintSheetTests::key).toList();
        if (!said.equals(keys)) {
            helper.fail("the tooltip says " + said + ", not " + keys);
        }
    }

    private static @Nullable String key(@Nullable Component component) {
        return component != null && component.getContents() instanceof TranslatableContents contents
                ? contents.getKey() : null;
    }
}
