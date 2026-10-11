// SPDX-FileCopyrightText: 2026 5thlayer
// SPDX-License-Identifier: MIT

package io.github._5thlayer.groundworks.gametest;

import java.util.List;

import io.github._5thlayer.groundworks.HandBack;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;

/**
 * The hand-back rules a Consumer's item calls when it plans its own replace, as the library's Fast
 * Replace and Stretch hand back by them: one held item is charged and what comes back goes into the
 * slot the charge freed, else onto a stack of its own, else into the first empty slot.
 *
 * <p>Each test calls {@link HandBack} for a player holding pink glazed terracotta, in hotbar slot 3
 * or the off hand, who is handed back lime glazed terracotta.
 */
final class HandBackTests {

    private static final int HELD_SLOT = 3;

    private HandBackTests() {
    }

    static void register(GroundworksGameTests.Registrar tests) {
        tests.test("hand_back_into_the_main_hand_slot_the_charge_freed", 20, HandBackTests::freesTheMainHand);
        tests.test("hand_back_into_the_off_hand_the_charge_freed", 20, HandBackTests::freesTheOffHand);
        tests.test("hand_back_onto_a_stack_of_its_own_when_the_held_stack_is_left_with_items", 20,
                HandBackTests::ontoAStackOfItsOwn);
        tests.test("hand_back_does_not_fit_when_the_charge_frees_no_slot_and_the_inventory_is_full", 20,
                HandBackTests::noRoom);
        tests.test("hand_back_charges_and_hands_nothing_to_a_creative_player", 20, HandBackTests::creative);
    }

    /** The last held item is charged, so what comes back goes into the held slot, and fits though every other slot is full. */
    private static void freesTheMainHand(GameTestHelper helper) {
        ListeningPlayer player = holding(helper, InteractionHand.MAIN_HAND, 1);
        fillInventory(player);
        if (!HandBack.fits(player, InteractionHand.MAIN_HAND, lime())) {
            helper.fail("the lime terracotta did not fit in the slot the charge frees");
        }
        HandBack.charge(player, InteractionHand.MAIN_HAND, lime());
        expectSlot(helper, player.getInventory().getItem(HELD_SLOT), Items.LIME_GLAZED_TERRACOTTA, 1);
        expectCount(helper, player, Items.PINK_GLAZED_TERRACOTTA, 0);
        helper.succeed();
    }

    /** The off hand's last item is charged, so what comes back goes into the off hand, and fits with the main inventory full. */
    private static void freesTheOffHand(GameTestHelper helper) {
        ListeningPlayer player = holding(helper, InteractionHand.OFF_HAND, 1);
        fillInventory(player);
        if (!HandBack.fits(player, InteractionHand.OFF_HAND, lime())) {
            helper.fail("the lime terracotta did not fit in the off hand the charge frees");
        }
        HandBack.charge(player, InteractionHand.OFF_HAND, lime());
        expectSlot(helper, player.getOffhandItem(), Items.LIME_GLAZED_TERRACOTTA, 1);
        expectCount(helper, player, Items.PINK_GLAZED_TERRACOTTA, 0);
        helper.succeed();
    }

    /** The charge leaves an item in the held slot, so what comes back joins the lime terracotta in slot 7. */
    private static void ontoAStackOfItsOwn(GameTestHelper helper) {
        ListeningPlayer player = holding(helper, InteractionHand.MAIN_HAND, 2);
        player.getInventory().setItem(7, new ItemStack(Items.LIME_GLAZED_TERRACOTTA, 5));
        HandBack.charge(player, InteractionHand.MAIN_HAND, lime());
        expectSlot(helper, player.getInventory().getItem(HELD_SLOT), Items.PINK_GLAZED_TERRACOTTA, 1);
        expectSlot(helper, player.getInventory().getItem(7), Items.LIME_GLAZED_TERRACOTTA, 6);
        expectCount(helper, player, Items.LIME_GLAZED_TERRACOTTA, 6);
        helper.succeed();
    }

    /** The charge leaves an item in the held slot and every other slot is full, so what comes back has nowhere to go. */
    private static void noRoom(GameTestHelper helper) {
        ListeningPlayer player = holding(helper, InteractionHand.MAIN_HAND, 2);
        fillInventory(player);
        if (HandBack.fits(player, InteractionHand.MAIN_HAND, lime())) {
            helper.fail("the lime terracotta fit with no slot to go in");
        }
        expectSlot(helper, player.getInventory().getItem(HELD_SLOT), Items.PINK_GLAZED_TERRACOTTA, 2);
        helper.succeed();
    }

    /** A creative player with a full inventory is told what comes back fits, is charged nothing and handed nothing. */
    private static void creative(GameTestHelper helper) {
        ListeningPlayer player = holding(helper, InteractionHand.MAIN_HAND, 2);
        player.setGameMode(GameType.CREATIVE);
        fillInventory(player);
        if (!HandBack.fits(player, InteractionHand.MAIN_HAND, lime())) {
            helper.fail("what comes back to a creative player did not fit");
        }
        HandBack.charge(player, InteractionHand.MAIN_HAND, lime());
        expectSlot(helper, player.getInventory().getItem(HELD_SLOT), Items.PINK_GLAZED_TERRACOTTA, 2);
        expectCount(helper, player, Items.LIME_GLAZED_TERRACOTTA, 0);
        helper.succeed();
    }

    /** What comes back: one lime glazed terracotta, made per call since stacks are made once items are bound. */
    private static List<ItemStack> lime() {
        return List.of(new ItemStack(Items.LIME_GLAZED_TERRACOTTA));
    }

    private static ListeningPlayer holding(GameTestHelper helper, InteractionHand hand, int count) {
        ListeningPlayer player = new ListeningPlayer(helper, new BlockPos(0, 1, 2));
        player.getInventory().setSelectedSlot(HELD_SLOT);
        player.setItemInHand(hand, new ItemStack(Items.PINK_GLAZED_TERRACOTTA, count));
        return player;
    }

    private static void fillInventory(ListeningPlayer player) {
        var inventory = player.getInventory().getNonEquipmentItems();
        for (int slot = 0; slot < inventory.size(); slot++) {
            if (inventory.get(slot).isEmpty()) {
                inventory.set(slot, new ItemStack(Items.DIRT, 64));
            }
        }
    }

    private static void expectSlot(GameTestHelper helper, ItemStack there, Item item, int count) {
        if (!there.is(item) || there.getCount() != count) {
            helper.fail("the slot holds " + there + ", expected " + count + " " + item);
        }
    }

    private static void expectCount(GameTestHelper helper, ListeningPlayer player, Item item, int count) {
        int held = player.getInventory().countItem(item);
        if (held != count) {
            helper.fail("the inventory holds " + held + " " + item + ", expected " + count);
        }
    }
}
