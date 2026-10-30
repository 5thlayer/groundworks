// SPDX-FileCopyrightText: 2026 5thlayer
// SPDX-License-Identifier: MIT

package io.github._5thlayer.groundworks.gametest;

import java.util.List;

import io.github._5thlayer.groundworks.PlacementPlan;
import io.github._5thlayer.groundworks.Placements;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

/**
 * What a capture leaves out, on the tall platform: a block standing partly outside the Selection,
 * left out whole and named, and a block in {@code groundworks:blueprint_deny}, left out silently.
 * Each Selection is a row from {@link #FIRST} eastwards, and its paste is planned on the floor's
 * top under {@link #SPOT}.
 */
final class BlueprintLeftOutTests {

    private static final BlockPos FIRST = new BlockPos(1, 1, 1);
    private static final BlockPos SPOT = new BlockPos(7, 1, 7);
    private static final String PACK = "empties_blueprint_deny";

    private BlueprintLeftOutTests() {
    }

    static void register(GroundworksGameTests.Registrar tests) {
        tests.tallTest("blueprint_a_selection_through_a_door_leaves_the_whole_door_out_and_names_it", 20,
                BlueprintLeftOutTests::doorLeftOut);
        tests.tallTest("blueprint_a_selection_through_a_footprint_leaves_the_footprint_out", 20,
                BlueprintLeftOutTests::footprintLeftOut);
        tests.tallTest("blueprint_a_selection_wholly_around_a_door_takes_it", 20,
                BlueprintLeftOutTests::wholeDoorTaken);
        tests.tallTest("blueprint_bedrock_in_a_selection_is_left_out_with_no_message", 20,
                BlueprintLeftOutTests::bedrockLeftOutSilently);
        tests.withPack(PACK).tallTest("blueprint_a_pack_emptying_the_deny_tag_captures_bedrock_in_creative", 20,
                BlueprintLeftOutTests::bedrockTakenWithoutTheTag);
    }

    /** A door from {@link #FIRST} up, and stone east of its lower half: a one-high row cuts the door through. */
    private static void doorLeftOut(GameTestHelper helper) {
        ListeningPlayer player = creative(helper);
        door(helper, FIRST);
        helper.setBlock(FIRST.east(), Blocks.STONE);
        capture(helper, player, FIRST, FIRST.east());
        Component told = lastTold(helper, player, "message.groundworks.blueprint_copied_left_out");
        if (!mentions(told, "message.groundworks.blueprint_left_out.straddling")
                || !mentions(told, Blocks.OAK_DOOR.getDescriptionId())) {
            helper.fail("the player was told " + told + ", which doesn't name the door left out as straddling");
        }
        expectPlanned(helper, player, List.of(Blocks.STONE.defaultBlockState()));
        helper.succeed();
    }

    private static void wholeDoorTaken(GameTestHelper helper) {
        ListeningPlayer player = creative(helper);
        door(helper, FIRST);
        capture(helper, player, FIRST, FIRST.above());
        lastTold(helper, player, "message.groundworks.blueprint_copied");
        PlacementPlan plan = plan(helper, player);
        if (plan == null || plan.blocks().size() != 2) {
            helper.fail("the paste's plan was " + plan + ", not the door's two halves");
        }
        helper.succeed();
    }

    /** The tests' Footprint with its origin at {@link #FIRST}: a Selection of the origin alone cuts through it. */
    private static void footprintLeftOut(GameTestHelper helper) {
        ListeningPlayer player = creative(helper);
        List<BlockPos> positions = TestFootprint.FOOTPRINT.positions(helper.absolutePos(FIRST), Direction.NORTH);
        for (int i = 0; i < positions.size(); i++) {
            helper.getLevel().setBlock(positions.get(i), TestFootprint.FOOTPRINT.stateAt(i, Direction.NORTH), 3);
        }
        capture(helper, player, FIRST, FIRST);
        Component told = lastTold(helper, player, "message.groundworks.blueprint_copied_left_out");
        if (!mentions(told, "message.groundworks.blueprint_left_out.straddling")) {
            helper.fail("the player was told " + told + ", not that the footprint straddles the Selection");
        }
        expectPlanned(helper, player, List.of());
        helper.succeed();
    }

    private static void bedrockLeftOutSilently(GameTestHelper helper) {
        ListeningPlayer player = creative(helper);
        helper.setBlock(FIRST, Blocks.STONE);
        helper.setBlock(FIRST.east(), Blocks.BEDROCK);
        capture(helper, player, FIRST, FIRST.east());
        lastTold(helper, player, "message.groundworks.blueprint_copied");
        expectPlanned(helper, player, List.of(Blocks.STONE.defaultBlockState()));
        helper.succeed();
    }

    private static void bedrockTakenWithoutTheTag(GameTestHelper helper) {
        ListeningPlayer player = creative(helper);
        helper.setBlock(FIRST, Blocks.STONE);
        helper.setBlock(FIRST.east(), Blocks.BEDROCK);
        capture(helper, player, FIRST, FIRST.east());
        lastTold(helper, player, "message.groundworks.blueprint_copied");
        expectPlanned(helper, player, List.of(Blocks.STONE.defaultBlockState(), Blocks.BEDROCK.defaultBlockState()));
        player.gameMode.useItemOn(player, helper.getLevel(), player.getMainHandItem(), InteractionHand.MAIN_HAND,
                topOf(helper.absolutePos(SPOT.below())));
        boolean pasted = false;
        for (BlockPos pos : BlockPos.betweenClosed(helper.absolutePos(SPOT.offset(-2, 0, -2)),
                helper.absolutePos(SPOT.offset(2, 0, 2)))) {
            pasted |= helper.getLevel().getBlockState(pos).is(Blocks.BEDROCK);
        }
        if (!pasted) {
            helper.fail("the paste laid no bedrock");
        }
        helper.succeed();
    }

    // ---- Helpers ---------------------------------------------------------------------------------

    private static void door(GameTestHelper helper, BlockPos lower) {
        BlockState door = Blocks.OAK_DOOR.defaultBlockState();
        helper.setBlock(lower, door);
        helper.setBlock(lower.above(), door.setValue(BlockStateProperties.DOUBLE_BLOCK_HALF, DoubleBlockHalf.UPPER));
    }

    private static void capture(GameTestHelper helper, ListeningPlayer player, BlockPos first, BlockPos second) {
        player.setShiftKeyDown(true);
        click(helper, player, first);
        player.setShiftKeyDown(false);
        click(helper, player, second);
    }

    private static void click(GameTestHelper helper, ListeningPlayer player, BlockPos pos) {
        player.gameMode.useItemOn(player, helper.getLevel(), player.getMainHandItem(), InteractionHand.MAIN_HAND,
                topOf(helper.absolutePos(pos)));
    }

    private static BlockHitResult topOf(BlockPos absolute) {
        return new BlockHitResult(Vec3.atCenterOf(absolute).relative(Direction.UP, 0.5), Direction.UP, absolute, false);
    }

    private static PlacementPlan plan(GameTestHelper helper, ListeningPlayer player) {
        return Placements.planFor(player.level(), player, InteractionHand.MAIN_HAND, player.getMainHandItem(),
                topOf(helper.absolutePos(SPOT.below())));
    }

    /** The paste's plan places these states, in the box's order, and nothing else. */
    private static void expectPlanned(GameTestHelper helper, ListeningPlayer player, List<BlockState> states) {
        PlacementPlan plan = plan(helper, player);
        List<BlockState> planned = plan == null ? null : plan.blocks().stream().map(PlacementPlan.Placed::state).toList();
        if (!states.equals(planned)) {
            helper.fail("the paste's plan places " + planned + ", not " + states);
        }
    }

    private static ListeningPlayer creative(GameTestHelper helper) {
        ListeningPlayer player = new ListeningPlayer(helper, new BlockPos(5, 1, 0));
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.PAPER));
        player.setGameMode(GameType.CREATIVE);
        return player;
    }

    /** The capture's message, the last the player heard, which must be the one under {@code key}. */
    private static Component lastTold(GameTestHelper helper, ListeningPlayer player, String key) {
        if (player.heard.isEmpty() || !player.heard.getLast().equals(key)) {
            helper.fail("the player last heard " + player.heard + ", not " + key);
        }
        return player.messages.getLast();
    }

    /** Whether the message, or any part of it or its arguments, is the text under this translation key. */
    private static boolean mentions(Component message, String key) {
        if (message.getContents() instanceof TranslatableContents translatable) {
            if (translatable.getKey().equals(key)) {
                return true;
            }
            for (Object arg : translatable.getArgs()) {
                if (arg instanceof Component component && mentions(component, key)) {
                    return true;
                }
            }
        }
        return message.getSiblings().stream().anyMatch(sibling -> mentions(sibling, key));
    }
}
