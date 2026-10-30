// SPDX-FileCopyrightText: 2026 5thlayer
// SPDX-License-Identifier: MIT

package io.github._5thlayer.groundworks.gametest;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import io.github._5thlayer.groundworks.BlueprintPrune;
import io.github._5thlayer.groundworks.Blueprints;
import io.github._5thlayer.groundworks.GroundworksConfig;
import io.github._5thlayer.groundworks.NextChoice;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.PlainTextContents;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.server.permissions.LevelBasedPermissionSet;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.EventHooks;

/**
 * Pruning, and an operator's recovery of a Cut: a one-stone Copy or Cut captured at {@link #FIRST}
 * with paper, and pasted on the floor under a spot. A pruning test stands Blueprints' clock where
 * it needs it and prunes every Copy unused for the server's prune age, so each runs in a batch of
 * its own, where it moves no other test's clock and prunes no other test's Copy.
 */
final class BlueprintPruneTests {

    private static final BlockPos FIRST = new BlockPos(1, 1, 1);
    private static final BlockPos SPOT = new BlockPos(7, 1, 7);
    /** Where the stone lands, pasted at {@link #SPOT}. */
    private static final BlockPos LANDED = new BlockPos(7, 1, 7);
    /** When each pruning test's Blueprint is captured, on Blueprints' clock. */
    private static final long CAPTURED = Instant.parse("2026-01-01T00:00:00Z").toEpochMilli();

    private BlueprintPruneTests() {
    }

    static void register(GroundworksGameTests.Registrar tests) {
        tests.testAlone("blueprint_prune_drops_a_copy_unused_past_the_age_and_its_sheet_reads_blank", 20,
                BlueprintPruneTests::dropsAnUnusedCopy);
        tests.testAlone("blueprint_prune_keeps_a_cut_past_the_age", 20, BlueprintPruneTests::keepsACut);
        tests.testAlone("blueprint_prune_holding_a_copy_resets_its_age", 20, BlueprintPruneTests::holdingResets);
        tests.testAlone("blueprint_prune_pasting_a_copy_resets_its_age", 20, BlueprintPruneTests::pastingResets);
        tests.test("blueprint_command_lists_a_cut_and_gives_a_sheet_that_pastes_it_once", 20,
                BlueprintPruneTests::recoversACut);
        tests.test("blueprint_command_is_refused_to_a_non_operator", 20, BlueprintPruneTests::refusedToANonOperator);
    }

    /** Unheld and unpasted past the age, the Copy is gone: held, the sheet is blank again and says so. */
    private static void dropsAnUnusedCopy(GameTestHelper helper) {
        onTheClock(helper, () -> {
            ListeningPlayer player = holding(helper);
            helper.setBlock(FIRST, Blocks.STONE);
            capture(helper, player);
            at(pastTheAge(CAPTURED));
            BlueprintPrune.prune(helper.getLevel().getServer());
            EventHooks.firePlayerTickPost(player);
            expectBlank(helper, player);
            expectHeard(helper, player, "message.groundworks.blueprint_gone");
        });
    }

    private static void keepsACut(GameTestHelper helper) {
        onTheClock(helper, () -> {
            ListeningPlayer player = holding(helper);
            helper.setBlock(FIRST, Blocks.STONE);
            NextChoice.press(player, false);
            capture(helper, player);
            expectHeard(helper, player, "message.groundworks.blueprint_cut");
            at(pastTheAge(CAPTURED));
            BlueprintPrune.prune(helper.getLevel().getServer());
            click(helper, player, SPOT.below());
            helper.assertBlockPresent(Blocks.STONE, LANDED);
        });
    }

    /**
     * Taken in hand ten days after its capture, the Copy is used then: a prune as the age has
     * passed since its capture, but not since it was held, keeps it.
     */
    private static void holdingResets(GameTestHelper helper) {
        onTheClock(helper, () -> {
            ListeningPlayer player = holding(helper);
            helper.setBlock(FIRST, Blocks.STONE);
            capture(helper, player);
            at(CAPTURED + Duration.ofDays(10).toMillis());
            EventHooks.firePlayerTickPost(player);
            at(pastTheAge(CAPTURED));
            BlueprintPrune.prune(helper.getLevel().getServer());
            player.getInventory().add(new ItemStack(Items.STONE));
            click(helper, player, SPOT.below());
            helper.assertBlockPresent(Blocks.STONE, LANDED);
        });
    }

    /** Pasted ten days after its capture, the Copy is used then, and a prune as at the age past its capture keeps it. */
    private static void pastingResets(GameTestHelper helper) {
        onTheClock(helper, () -> {
            ListeningPlayer player = holding(helper);
            helper.setBlock(FIRST, Blocks.STONE);
            capture(helper, player);
            player.getInventory().add(new ItemStack(Items.STONE, 2));
            at(CAPTURED + Duration.ofDays(10).toMillis());
            click(helper, player, SPOT.below());
            helper.assertBlockPresent(Blocks.STONE, LANDED);
            at(pastTheAge(CAPTURED));
            BlueprintPrune.prune(helper.getLevel().getServer());
            click(helper, player, SPOT.offset(-4, 0, 0).below());
            helper.assertBlockPresent(Blocks.STONE, LANDED.offset(-4, 0, 0));
        });
    }

    /**
     * Runs the test with Blueprints' clock standing at {@link #CAPTURED}, moved by {@link #at},
     * and sets it back to the wall clock after, passed or failed; the test passes where the body
     * returns.
     */
    private static void onTheClock(GameTestHelper helper, Runnable body) {
        at(CAPTURED);
        try {
            body.run();
        } finally {
            BlueprintPrune.clock(System::currentTimeMillis);
        }
        helper.succeed();
    }

    /** Stands Blueprints' clock at {@code now}. */
    private static void at(long now) {
        BlueprintPrune.clock(() -> now);
    }

    /**
     * An operator lists the Cut and is given a sheet naming it, which pastes it; the original
     * sheet then finds it gone, and is blank.
     */
    private static void recoversACut(GameTestHelper helper) {
        ListeningPlayer player = holding(helper);
        helper.setBlock(FIRST, Blocks.STONE);
        NextChoice.press(player, false);
        capture(helper, player);
        UUID id = Blueprints.heldId(player.getMainHandItem());
        CommandSourceStack operator = player.createCommandSourceStack().withPermission(LevelBasedPermissionSet.GAMEMASTER);
        run(helper, operator, "groundworks blueprint");
        if (player.messages.stream().noneMatch(message -> mentions(message, id.toString()))) {
            helper.fail("the listing " + player.messages + " doesn't name the Cut " + id);
        }
        run(helper, operator, "groundworks blueprint give " + id);
        ItemStack original = player.getMainHandItem();
        ItemStack recovered = player.getInventory().getNonEquipmentItems().stream()
                .filter(stack -> stack != original && id.equals(Blueprints.heldId(stack))).findFirst().orElse(null);
        if (recovered == null) {
            helper.fail("the operator was given no sheet naming the Cut");
            return;
        }
        player.setItemInHand(InteractionHand.MAIN_HAND, recovered);
        click(helper, player, SPOT.below());
        helper.assertBlockPresent(Blocks.STONE, LANDED);
        player.setItemInHand(InteractionHand.MAIN_HAND, original);
        click(helper, player, SPOT.offset(-4, 0, 0).below());
        expectHeard(helper, player, "message.groundworks.blueprint_gone");
        helper.assertBlockPresent(Blocks.AIR, LANDED.offset(-4, 0, 0));
        expectBlank(helper, player);
        helper.succeed();
    }

    private static void refusedToANonOperator(GameTestHelper helper) {
        ListeningPlayer player = holding(helper);
        var dispatcher = helper.getLevel().getServer().getCommands().getDispatcher();
        for (String command : new String[] {"groundworks blueprint", "groundworks blueprint give " + UUID.randomUUID()}) {
            try {
                dispatcher.execute(command, player.createCommandSourceStack());
                helper.fail("a non-operator ran /" + command);
            } catch (CommandSyntaxException expected) {
                // Refused, as an unknown command.
            }
        }
        helper.succeed();
    }

    private static void run(GameTestHelper helper, CommandSourceStack source, String command) {
        try {
            helper.getLevel().getServer().getCommands().getDispatcher().execute(command, source);
        } catch (CommandSyntaxException refused) {
            helper.fail("/" + command + " was refused: " + refused.getMessage());
        }
    }

    /** Whether the message, any part of it or any of its arguments, reads this text. */
    private static boolean mentions(Component message, String text) {
        if (message.getContents() instanceof PlainTextContents plain && plain.text().equals(text)) {
            return true;
        }
        if (message.getContents() instanceof TranslatableContents translatable) {
            for (Object arg : translatable.getArgs()) {
                if (arg.equals(text) || (arg instanceof Component component && mentions(component, text))) {
                    return true;
                }
            }
        }
        return message.getSiblings().stream().anyMatch(sibling -> mentions(sibling, text));
    }

    /** The moment just past the prune age after {@code at}. */
    private static long pastTheAge(long at) {
        return at + GroundworksConfig.copyPruneAge().toMillis() + 1;
    }

    /** The stone at {@link #FIRST} captured alone, by a sneak-click on it and a click on it. */
    private static void capture(GameTestHelper helper, ListeningPlayer player) {
        player.setShiftKeyDown(true);
        click(helper, player, FIRST);
        player.setShiftKeyDown(false);
        click(helper, player, FIRST);
        if (!Blueprints.holdsBlueprint(player.getMainHandItem())) {
            helper.fail("the capture left the sheet blank; the player heard " + player.heard);
        }
    }

    private static void click(GameTestHelper helper, ListeningPlayer player, BlockPos pos) {
        player.gameMode.useItemOn(player, helper.getLevel(), player.getMainHandItem(), InteractionHand.MAIN_HAND,
                topOf(helper.absolutePos(pos)));
    }

    private static BlockHitResult topOf(BlockPos absolute) {
        return new BlockHitResult(Vec3.atCenterOf(absolute).relative(Direction.UP, 0.5), Direction.UP, absolute, false);
    }

    private static ListeningPlayer holding(GameTestHelper helper) {
        ListeningPlayer player = new ListeningPlayer(helper, new BlockPos(5, 1, 0));
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.PAPER));
        return player;
    }

    /** The held sheet is blank again: it stacks with paper never used. */
    private static void expectBlank(GameTestHelper helper, ListeningPlayer player) {
        if (!ItemStack.isSameItemSameComponents(player.getMainHandItem(), new ItemStack(Items.PAPER))) {
            helper.fail("the sheet is " + player.getMainHandItem().getComponentsPatch() + ", not blank");
        }
    }

    private static void expectHeard(GameTestHelper helper, ListeningPlayer player, String key) {
        if (!player.heard.contains(key)) {
            helper.fail("the player heard " + player.heard + ", not " + key);
        }
    }
}
