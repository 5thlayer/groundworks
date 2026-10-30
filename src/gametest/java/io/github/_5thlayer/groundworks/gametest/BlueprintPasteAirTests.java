// SPDX-FileCopyrightText: 2026 5thlayer
// SPDX-License-Identifier: MIT

package io.github._5thlayer.groundworks.gametest;

import java.util.List;

import io.github._5thlayer.groundworks.NextChoice;
import io.github._5thlayer.groundworks.PlacementPlan;
import io.github._5thlayer.groundworks.Placements;
import io.github._5thlayer.groundworks.Refusal;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * A sheet's choice to skip its Blueprint's air or paste it, stepped by Next Choice, on the tall
 * platform: a hollow box of stone, three by three by three, captured from {@link #FIRST} to {@link
 * #SECOND} with air at its heart, pasted on the floor's top under {@link #SPOT}, so its heart lands
 * at {@link #HEART}.
 */
final class BlueprintPasteAirTests {

    private static final BlockPos FIRST = new BlockPos(1, 1, 1);
    private static final BlockPos SECOND = new BlockPos(3, 3, 3);
    private static final BlockPos SPOT = new BlockPos(7, 1, 7);
    /** Where the box's lowest corner lands, one west and one north of the spot. */
    private static final BlockPos ORIGIN = new BlockPos(6, 1, 6);
    /** Where the box's air, at its heart, lands. */
    private static final BlockPos HEART = ORIGIN.offset(1, 1, 1);
    /** The stone of the box's shell, which a survival paste is charged. */
    private static final int SHELL = 26;
    private static final int MAIN_SLOT = 20;

    private BlueprintPasteAirTests() {
    }

    static void register(GroundworksGameTests.Registrar tests) {
        tests.tallTest("blueprint_next_choice_toggles_pasting_air_on_a_held_blueprint", 20,
                BlueprintPasteAirTests::toggles);
        tests.tallTest("blueprint_pasting_air_clears_the_inside_handing_its_drops_back", 20,
                BlueprintPasteAirTests::clearsTheInside);
        tests.tallTest("blueprint_pasting_air_hands_a_cleared_chest_and_its_contents_back_the_rest_at_the_feet", 20,
                BlueprintPasteAirTests::handsTheChestBack);
        tests.tallTest("blueprint_pasting_air_in_creative_hands_nothing_back", 20,
                BlueprintPasteAirTests::creativeHandedNothing);
        tests.tallTest("blueprint_pasting_air_over_bedrock_is_refused_whole", 20, BlueprintPasteAirTests::bedrock);
        tests.tallTest("blueprint_skipping_air_leaves_the_inside_untouched", 20, BlueprintPasteAirTests::skipsAir);
    }

    private static void toggles(GameTestHelper helper) {
        ListeningPlayer player = copied(helper);
        helper.setBlock(HEART, Blocks.DIRT);
        expectClears(helper, player, List.of());
        NextChoice.press(player, false);
        expectClears(helper, player, List.of(helper.absolutePos(HEART)));
        NextChoice.press(player, false);
        expectClears(helper, player, List.of());
        NextChoice.press(player, true);
        expectClears(helper, player, List.of(helper.absolutePos(HEART)));
        helper.succeed();
    }

    private static void clearsTheInside(GameTestHelper helper) {
        ListeningPlayer player = copied(helper);
        NextChoice.press(player, false);
        player.setGameMode(GameType.SURVIVAL);
        player.getInventory().setItem(MAIN_SLOT, new ItemStack(Items.STONE, SHELL));
        helper.setBlock(HEART, Blocks.DIRT);
        paste(helper, player);
        helper.assertBlockPresent(Blocks.AIR, HEART);
        helper.assertBlockPresent(Blocks.STONE, ORIGIN);
        expectHeld(helper, player, Items.STONE, 0);
        expectHeld(helper, player, Items.DIRT, 1);
        helper.succeed();
    }

    /** A full inventory has room for one stack once the stone is taken: the rest drops at the feet. */
    private static void handsTheChestBack(GameTestHelper helper) {
        ListeningPlayer player = copied(helper);
        NextChoice.press(player, false);
        player.setGameMode(GameType.SURVIVAL);
        player.getInventory().setItem(MAIN_SLOT, new ItemStack(Items.STONE, SHELL));
        player.fillInventory();
        helper.setBlock(HEART, Blocks.CHEST);
        if (!(helper.getBlockEntity(HEART, ChestBlockEntity.class) instanceof ChestBlockEntity chest)) {
            helper.fail("no chest stands at the heart");
            return;
        }
        chest.setItem(0, new ItemStack(Items.DIAMOND, 64));
        chest.setItem(1, new ItemStack(Items.EMERALD, 64));
        paste(helper, player);
        helper.assertBlockPresent(Blocks.AIR, HEART);
        AABB feet = player.getBoundingBox().inflate(1.5);
        expectHandedBack(helper, player, feet, Items.CHEST, 1);
        expectHandedBack(helper, player, feet, Items.DIAMOND, 64);
        expectHandedBack(helper, player, feet, Items.EMERALD, 64);
        helper.succeed();
    }

    private static void creativeHandedNothing(GameTestHelper helper) {
        ListeningPlayer player = copied(helper);
        NextChoice.press(player, false);
        helper.setBlock(HEART, Blocks.DIRT);
        paste(helper, player);
        helper.assertBlockPresent(Blocks.AIR, HEART);
        expectHeld(helper, player, Items.DIRT, 0);
        helper.succeed();
    }

    private static void bedrock(GameTestHelper helper) {
        ListeningPlayer player = copied(helper);
        NextChoice.press(player, false);
        helper.setBlock(HEART, Blocks.BEDROCK);
        PlacementPlan plan = plan(helper, player);
        if (plan == null || !(plan.refusal() instanceof Refusal.At at) || at.reason() != Refusal.Blueprint.CANT_BREAK
                || !at.pos().equals(helper.absolutePos(HEART))) {
            helper.fail("the plan was " + plan + ", not refused as unbreakable at the bedrock");
        }
        paste(helper, player);
        expectHeard(helper, player, "message.groundworks.blueprint_cant_break");
        helper.assertBlockPresent(Blocks.BEDROCK, HEART);
        helper.assertBlockPresent(Blocks.AIR, ORIGIN);
        helper.succeed();
    }

    private static void skipsAir(GameTestHelper helper) {
        ListeningPlayer player = copied(helper);
        helper.setBlock(HEART, Blocks.DIRT);
        paste(helper, player);
        helper.assertBlockPresent(Blocks.DIRT, HEART);
        helper.assertBlockPresent(Blocks.STONE, ORIGIN);
        helper.succeed();
    }

    // ---- Helpers ---------------------------------------------------------------------------------

    /** A creative player holding a sheet the hollow box is copied onto. */
    private static ListeningPlayer copied(GameTestHelper helper) {
        BlockPos.betweenClosed(FIRST, SECOND).forEach(pos -> helper.setBlock(pos, Blocks.STONE));
        helper.setBlock(FIRST.offset(1, 1, 1), Blocks.AIR);
        ListeningPlayer player = new ListeningPlayer(helper, new BlockPos(5, 1, 0));
        player.setGameMode(GameType.CREATIVE);
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.PAPER));
        player.setShiftKeyDown(true);
        click(helper, player, FIRST);
        player.setShiftKeyDown(false);
        click(helper, player, SECOND);
        expectHeard(helper, player, "message.groundworks.blueprint_copied");
        return player;
    }

    private static void paste(GameTestHelper helper, ListeningPlayer player) {
        click(helper, player, SPOT.below());
    }

    private static @Nullable PlacementPlan plan(GameTestHelper helper, ListeningPlayer player) {
        return Placements.planFor(player.level(), player, InteractionHand.MAIN_HAND, player.getMainHandItem(),
                topOf(helper.absolutePos(SPOT.below())));
    }

    private static void click(GameTestHelper helper, ListeningPlayer player, BlockPos pos) {
        player.gameMode.useItemOn(player, helper.getLevel(), player.getMainHandItem(), InteractionHand.MAIN_HAND,
                topOf(helper.absolutePos(pos)));
    }

    private static BlockHitResult topOf(BlockPos absolute) {
        return new BlockHitResult(Vec3.atCenterOf(absolute).relative(Direction.UP, 0.5), Direction.UP, absolute, false);
    }

    private static void expectClears(GameTestHelper helper, ListeningPlayer player, List<BlockPos> clears) {
        PlacementPlan plan = plan(helper, player);
        if (plan == null || plan.isRefused() || !plan.clears().equals(clears)) {
            helper.fail("the plan was " + plan + ", not accepted clearing " + clears);
        }
    }

    private static void expectHeld(GameTestHelper helper, ListeningPlayer player, Item item, int count) {
        int held = player.getInventory().countItem(item);
        if (held != count) {
            helper.fail("the player holds " + held + " " + item + ", not " + count);
        }
    }

    /** That {@code count} of the item is in the inventory or lies at the player's feet, and none anywhere else. */
    private static void expectHandedBack(GameTestHelper helper, ListeningPlayer player, AABB feet, Item item,
            int count) {
        int handed = player.getInventory().countItem(item);
        int elsewhere = 0;
        for (ItemEntity entity : helper.getLevel().getEntitiesOfClass(ItemEntity.class,
                new AABB(helper.absolutePos(BlockPos.ZERO)).inflate(16))) {
            if (entity.getItem().is(item)) {
                if (feet.intersects(entity.getBoundingBox())) {
                    handed += entity.getItem().getCount();
                } else {
                    elsewhere += entity.getItem().getCount();
                }
            }
        }
        if (handed != count || elsewhere != 0) {
            helper.fail(handed + " " + item + " were handed back and " + elsewhere + " dropped elsewhere, not " + count
                    + " handed back");
        }
    }

    private static void expectHeard(GameTestHelper helper, ListeningPlayer player, String key) {
        if (!player.heard.contains(key)) {
            helper.fail("the player heard " + player.heard + ", not " + key);
        }
    }
}
