// SPDX-FileCopyrightText: 2026 5thlayer
// SPDX-License-Identifier: MIT

package io.github._5thlayer.groundworks.gametest;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import io.github._5thlayer.groundworks.BlueprintStore;
import io.github._5thlayer.groundworks.UndoPaste;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.LevelResource;
import net.minecraft.world.level.storage.SavedDataStorage;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

/**
 * Undo Paste with paper, the vanilla Consumer's sheet, on the tall platform: a Copy of planks
 * beside cobblestone, one over the other, captured from {@link #FIRST} and pasted on the floor
 * under {@link #SPOT}, where it lands from {@link #ORIGIN}. Both blocks drop themselves, so what an
 * undo hands back is what the paste was charged.
 */
final class BlueprintUndoTests {

    private static final BlockPos FIRST = new BlockPos(1, 1, 1);
    private static final BlockPos SECOND = new BlockPos(1, 2, 1);
    private static final BlockState PLANKS = Blocks.OAK_PLANKS.defaultBlockState();
    private static final BlockState COBBLESTONE = Blocks.COBBLESTONE.defaultBlockState();

    private static final BlockPos SPOT = new BlockPos(7, 1, 7);
    private static final BlockPos ORIGIN = SPOT;

    private static final int MAIN_SLOT = 20;

    private BlueprintUndoTests() {
    }

    static void register(GroundworksGameTests.Registrar tests) {
        tests.tallTest("blueprint_undo_paste_takes_up_the_paste_and_hands_back_its_drops", 20,
                BlueprintUndoTests::takesUpThePaste);
        tests.tallTest("blueprint_undo_paste_leaves_a_block_replaced_since", 20, BlueprintUndoTests::leavesReplaced);
        tests.tallTest("blueprint_undo_paste_hands_a_creative_player_nothing", 20, BlueprintUndoTests::creativeGetsNothing);
        tests.tallTest("blueprint_undo_paste_takes_back_ten_pastes_latest_first_and_refuses_the_eleventh", 20,
                BlueprintUndoTests::tenDeep);
        tests.tallTest("blueprint_undo_paste_in_another_dimension_is_refused", 20, BlueprintUndoTests::otherDimension);
        tests.tallTest("blueprint_undo_paste_on_a_cleared_copy_is_refused", 20, BlueprintUndoTests::clearedCopy);
        tests.tallTest("blueprint_undo_paste_with_no_paste_is_refused", 20, BlueprintUndoTests::noPaste);
        tests.tallTest("blueprint_undo_paste_history_survives_a_save_and_reload_of_the_world_data", 20,
                BlueprintUndoTests::survivesReload);
    }

    private static void takesUpThePaste(GameTestHelper helper) {
        ListeningPlayer player = holding(helper);
        build(helper);
        copy(helper, player);
        stock(player, Map.of(Items.OAK_PLANKS, 1, Items.COBBLESTONE, 1));
        paste(helper, player, SPOT);
        helper.assertBlockPresent(Blocks.OAK_PLANKS, ORIGIN);
        helper.assertBlockPresent(Blocks.COBBLESTONE, ORIGIN.above());
        expectHeld(helper, player, Map.of(Items.OAK_PLANKS, 0, Items.COBBLESTONE, 0));
        UndoPaste.press(player);
        helper.assertBlockPresent(Blocks.AIR, ORIGIN);
        helper.assertBlockPresent(Blocks.AIR, ORIGIN.above());
        expectHeld(helper, player, Map.of(Items.OAK_PLANKS, 1, Items.COBBLESTONE, 1));
        expectHeard(helper, player, "message.groundworks.blueprint_undone");
        expectBuild(helper);
        helper.succeed();
    }

    private static void leavesReplaced(GameTestHelper helper) {
        ListeningPlayer player = holding(helper);
        build(helper);
        copy(helper, player);
        stock(player, Map.of(Items.OAK_PLANKS, 1, Items.COBBLESTONE, 1));
        paste(helper, player, SPOT);
        helper.setBlock(ORIGIN.above(), Blocks.STONE_BRICKS);
        UndoPaste.press(player);
        helper.assertBlockPresent(Blocks.AIR, ORIGIN);
        helper.assertBlockPresent(Blocks.STONE_BRICKS, ORIGIN.above());
        expectHeld(helper, player, Map.of(Items.OAK_PLANKS, 1, Items.COBBLESTONE, 0, Items.STONE_BRICKS, 0));
        helper.succeed();
    }

    private static void creativeGetsNothing(GameTestHelper helper) {
        ListeningPlayer player = creative(helper);
        build(helper);
        copy(helper, player);
        paste(helper, player, SPOT);
        UndoPaste.press(player);
        helper.assertBlockPresent(Blocks.AIR, ORIGIN);
        helper.assertBlockPresent(Blocks.AIR, ORIGIN.above());
        expectHeld(helper, player, Map.of(Items.OAK_PLANKS, 0, Items.COBBLESTONE, 0));
        helper.succeed();
    }

    /** Eleven pastes along the floor; ten presses take back all but the first, latest first. */
    private static void tenDeep(GameTestHelper helper) {
        ListeningPlayer player = creative(helper);
        build(helper);
        copy(helper, player);
        List<BlockPos> spots = new ArrayList<>();
        for (int i = 0; i < 11; i++) {
            spots.add(new BlockPos(3 + i % 6, 1, 5 + 3 * (i / 6)));
        }
        spots.forEach(spot -> paste(helper, player, spot));
        for (int undone = 1; undone <= 10; undone++) {
            UndoPaste.press(player);
            BlockPos latest = spots.get(spots.size() - undone);
            helper.assertBlockPresent(Blocks.AIR, latest);
            BlockPos earlier = spots.get(spots.size() - undone - 1);
            helper.assertBlockPresent(Blocks.OAK_PLANKS, earlier);
        }
        player.heard.clear();
        UndoPaste.press(player);
        expectHeard(helper, player, "message.groundworks.blueprint_nothing_to_undo");
        helper.assertBlockPresent(Blocks.OAK_PLANKS, spots.getFirst());
        helper.assertBlockPresent(Blocks.COBBLESTONE, spots.getFirst().above());
        helper.succeed();
    }

    /** A second player in the nether, holding the same sheet, is refused; the paste can still be undone here. */
    private static void otherDimension(GameTestHelper helper) {
        ListeningPlayer player = creative(helper);
        build(helper);
        copy(helper, player);
        paste(helper, player, SPOT);
        ServerLevel nether = helper.getLevel().getServer().getLevel(Level.NETHER);
        ListeningPlayer elsewhere = new ListeningPlayer(nether, new Vec3(8.5, 100, 8.5));
        elsewhere.setGameMode(GameType.CREATIVE);
        elsewhere.setItemInHand(InteractionHand.MAIN_HAND, player.getMainHandItem().copy());
        UndoPaste.press(elsewhere);
        expectHeard(helper, elsewhere, "message.groundworks.blueprint_undo_in_another_dimension");
        helper.assertBlockPresent(Blocks.OAK_PLANKS, ORIGIN);
        UndoPaste.press(player);
        helper.assertBlockPresent(Blocks.AIR, ORIGIN);
        helper.succeed();
    }

    private static void clearedCopy(GameTestHelper helper) {
        ListeningPlayer player = creative(helper);
        build(helper);
        copy(helper, player);
        paste(helper, player, SPOT);
        player.setShiftKeyDown(true);
        player.gameMode.useItem(player, helper.getLevel(), player.getMainHandItem(), InteractionHand.MAIN_HAND);
        player.setShiftKeyDown(false);
        expectHeard(helper, player, "message.groundworks.blueprint_cleared");
        UndoPaste.press(player);
        expectHeard(helper, player, "message.groundworks.blueprint_nothing_to_undo");
        helper.assertBlockPresent(Blocks.OAK_PLANKS, ORIGIN);
        helper.assertBlockPresent(Blocks.COBBLESTONE, ORIGIN.above());
        helper.succeed();
    }

    private static void noPaste(GameTestHelper helper) {
        ListeningPlayer player = creative(helper);
        build(helper);
        copy(helper, player);
        UndoPaste.press(player);
        expectHeard(helper, player, "message.groundworks.blueprint_nothing_to_undo");
        expectBuild(helper);
        helper.succeed();
    }

    /**
     * The world data saved, and read back from its file into a store of its own, which stands in
     * for the server's: the paste is still undone from it.
     */
    private static void survivesReload(GameTestHelper helper) {
        ListeningPlayer player = creative(helper);
        build(helper);
        copy(helper, player);
        paste(helper, player, SPOT);
        MinecraftServer server = helper.getLevel().getServer();
        server.getDataStorage().saveAndJoin();
        BlueprintStore reloaded;
        try (SavedDataStorage fresh = new SavedDataStorage(server.getWorldPath(LevelResource.DATA),
                server.getFixerUpper(), server.registryAccess())) {
            reloaded = fresh.get(BlueprintStore.TYPE);
        }
        if (reloaded == null) {
            helper.fail("the world data holds no Blueprints once saved");
            return;
        }
        server.getDataStorage().set(BlueprintStore.TYPE, reloaded);
        UndoPaste.press(player);
        helper.assertBlockPresent(Blocks.AIR, ORIGIN);
        helper.assertBlockPresent(Blocks.AIR, ORIGIN.above());
        helper.succeed();
    }

    // ---- Helpers ---------------------------------------------------------------------------------

    private static void build(GameTestHelper helper) {
        helper.setBlock(FIRST, PLANKS);
        helper.setBlock(SECOND, COBBLESTONE);
    }

    /** The build still stands where it was captured: an undo takes up only what the paste laid. */
    private static void expectBuild(GameTestHelper helper) {
        helper.assertBlockPresent(Blocks.OAK_PLANKS, FIRST);
        helper.assertBlockPresent(Blocks.COBBLESTONE, SECOND);
    }

    private static void copy(GameTestHelper helper, ListeningPlayer player) {
        player.setShiftKeyDown(true);
        click(helper, player, FIRST);
        player.setShiftKeyDown(false);
        click(helper, player, SECOND);
    }

    /** A paste resting on the floor under {@code spot}, where the build's bottom block lands. */
    private static void paste(GameTestHelper helper, ListeningPlayer player, BlockPos spot) {
        click(helper, player, spot.below());
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

    private static ListeningPlayer creative(GameTestHelper helper) {
        ListeningPlayer player = holding(helper);
        player.setGameMode(GameType.CREATIVE);
        return player;
    }

    private static void stock(ListeningPlayer player, Map<Item, Integer> counts) {
        int slot = MAIN_SLOT;
        for (Map.Entry<Item, Integer> entry : counts.entrySet()) {
            player.getInventory().setItem(slot++, new ItemStack(entry.getKey(), entry.getValue()));
        }
    }

    private static void expectHeld(GameTestHelper helper, ListeningPlayer player, Map<Item, Integer> counts) {
        counts.forEach((item, count) -> {
            int held = player.getInventory().countItem(item);
            if (held != count) {
                helper.fail("the player holds " + held + " " + item + ", not " + count);
            }
        });
    }

    private static void expectHeard(GameTestHelper helper, ListeningPlayer player, String key) {
        if (!player.heard.contains(key)) {
            helper.fail("the player heard " + player.heard + ", not " + key);
        }
    }
}
