// SPDX-FileCopyrightText: 2026 5thlayer
// SPDX-License-Identifier: MIT

package io.github._5thlayer.groundworks.gametest;

import java.util.Map;

import io.github._5thlayer.groundworks.BlueprintCorner;
import io.github._5thlayer.groundworks.BlueprintStore;
import io.github._5thlayer.groundworks.Groundworks;
import io.github._5thlayer.groundworks.PlacementPlan;
import io.github._5thlayer.groundworks.Placements;
import io.github._5thlayer.groundworks.Refusal;
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
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.LevelResource;
import net.minecraft.world.level.storage.SavedDataStorage;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * Blueprints with paper, the vanilla Consumer's sheet: a Copy captured with two clicks and pasted
 * elsewhere, on the tall platform.
 *
 * <p>The build stands in the box from {@link #FIRST} to {@link #SECOND}, two by two by two: stone
 * at the first Corner, oak stairs facing east beside it, an oak log lying east-west over the far
 * side, and glass at the second Corner, the rest air. It is pasted on the floor's top under {@link
 * #SPOT}, so it lands from {@link #ORIGIN}, one west and one north of the spot.
 */
final class BlueprintTests {

    private static final BlockPos FIRST = new BlockPos(1, 1, 1);
    private static final BlockPos SECOND = new BlockPos(2, 2, 2);
    private static final BlockState STONE = Blocks.STONE.defaultBlockState();
    private static final BlockState STAIRS = Blocks.OAK_STAIRS.defaultBlockState()
            .setValue(StairBlock.FACING, Direction.EAST);
    private static final BlockState LOG = Blocks.OAK_LOG.defaultBlockState()
            .setValue(RotatedPillarBlock.AXIS, Direction.Axis.X);
    private static final BlockState GLASS = Blocks.GLASS.defaultBlockState();
    /** The build's blocks, by where they stand in the box from its lowest corner. */
    private static final Map<BlockPos, BlockState> BUILD = Map.of(
            new BlockPos(0, 0, 0), STONE,
            new BlockPos(1, 0, 0), STAIRS,
            new BlockPos(0, 1, 1), LOG,
            new BlockPos(1, 1, 1), GLASS);
    /** What the build's paste is charged. */
    private static final Map<Item, Integer> COST = Map.of(Items.STONE, 1, Items.OAK_STAIRS, 1, Items.OAK_LOG, 1,
            Items.GLASS, 1);

    private static final BlockPos SPOT = new BlockPos(7, 1, 7);
    private static final BlockPos ORIGIN = new BlockPos(6, 1, 6);
    /** Where the box's air at its bottom layer's south-west lands. */
    private static final BlockPos LANDED_AIR = ORIGIN.south();

    private static final int MAIN_SLOT = 20;

    private BlueprintTests() {
    }

    static void register(GroundworksGameTests.Registrar tests) {
        tests.tallTest("blueprint_a_copy_pastes_its_captured_states_resting_on_the_aimed_face_and_skips_air", 20,
                BlueprintTests::pastesItsStates);
        tests.tallTest("blueprint_a_capture_takes_one_sheet_off_the_stack_to_stand_alone", 20,
                BlueprintTests::takesOneSheet);
        tests.tallTest("blueprint_a_capture_with_no_room_for_its_sheet_is_refused", 20,
                BlueprintTests::noRoomForTheSheet);
        tests.tallTest("blueprint_a_selection_over_the_cap_is_refused", 20, BlueprintTests::overTheCap);
        tests.tallTest("blueprint_a_selection_reaching_an_unloaded_chunk_is_refused", 20, BlueprintTests::unloaded);
        tests.tallTest("blueprint_a_survival_paste_is_charged_as_pick_block_items", 20, BlueprintTests::charged);
        tests.tallTest("blueprint_a_survival_paste_short_of_items_is_refused_naming_them_and_places_nothing", 20,
                BlueprintTests::shortOfItems);
        tests.tallTest("blueprint_a_paste_with_a_position_taken_is_refused_whole", 20, BlueprintTests::taken);
        tests.tallTest("blueprint_a_block_already_standing_as_wanted_stays_uncharged", 20,
                BlueprintTests::standingStaysUncharged);
        tests.tallTest("blueprint_a_paste_where_the_player_may_not_build_is_refused_for_that_first", 20,
                BlueprintTests::mayNotBuildFirst);
        tests.tallTest("blueprint_a_sneak_use_in_the_air_clears_a_stored_corner", 20, BlueprintTests::clearsTheCorner);
        tests.tallTest("blueprint_a_sneak_use_in_the_air_clears_a_copy", 20, BlueprintTests::clearsTheCopy);
        tests.tallTest("blueprint_a_corner_in_another_dimension_is_no_corner", 20,
                BlueprintTests::cornerElsewhereIsNone);
        tests.tallTest("blueprint_another_player_may_paste_the_sheet_in_another_dimension", 20,
                BlueprintTests::anotherPlayerElsewhere);
        tests.tallTest("blueprint_a_copy_survives_a_save_and_reload_of_the_world_data", 20,
                BlueprintTests::survivesReload);
    }

    private static void pastesItsStates(GameTestHelper helper) {
        ListeningPlayer player = creative(helper);
        build(helper);
        helper.setBlock(LANDED_AIR, Blocks.DIRT);
        copy(helper, player);
        expectBuild(helper, FIRST);
        expectHeard(helper, player, "message.groundworks.blueprint_copied");
        PlacementPlan plan = plan(helper, player);
        if (plan == null || plan.isRefused() || plan.blocks().size() != BUILD.size()) {
            helper.fail("the paste's plan was " + plan + ", not the build's four blocks accepted");
        }
        paste(helper, player);
        expectBuild(helper, ORIGIN);
        helper.assertBlockPresent(Blocks.DIRT, LANDED_AIR);
        expectBuild(helper, FIRST);
        helper.succeed();
    }

    private static void takesOneSheet(GameTestHelper helper) {
        ListeningPlayer player = holding(helper, 5);
        build(helper);
        copy(helper, player);
        ItemStack blank = player.getMainHandItem();
        if (blank.getCount() != 4 || blank.getMaxStackSize() != 64) {
            helper.fail("the held stack is " + blank + " of at most " + blank.getMaxStackSize() + ", not 4 blank sheets");
        }
        int slot = sheetSlot(player);
        if (slot < 0) {
            helper.fail("no sheet standing alone in its stack is in the inventory");
            return;
        }
        ItemStack sheet = player.getInventory().getItem(slot);
        if (sheet.getCount() != 1) {
            helper.fail("the sheet's stack holds " + sheet.getCount());
        }
        player.getInventory().setItem(slot, ItemStack.EMPTY);
        player.setItemInHand(InteractionHand.MAIN_HAND, sheet);
        player.setGameMode(GameType.CREATIVE);
        paste(helper, player);
        expectBuild(helper, ORIGIN);
        helper.succeed();
    }

    private static void noRoomForTheSheet(GameTestHelper helper) {
        ListeningPlayer player = holding(helper, 5);
        player.fillInventory();
        build(helper);
        copy(helper, player);
        expectHeard(helper, player, "message.groundworks.blueprint_no_room_for_the_sheet");
        if (player.getMainHandItem().getCount() != 5 || sheetSlot(player) >= 0) {
            helper.fail("a sheet was taken off the stack with no room for it");
        }
        helper.succeed();
    }

    private static void overTheCap(GameTestHelper helper) {
        ListeningPlayer player = holding(helper, 1);
        sneakClick(helper, player, FIRST);
        // 41 × 41 × 41 positions, over the default 32,768.
        click(helper, player, FIRST.offset(40, 40, 40));
        expectHeard(helper, player, "message.groundworks.blueprint_over_the_cap");
        expectBlank(helper, player);
        helper.succeed();
    }

    private static void unloaded(GameTestHelper helper) {
        ListeningPlayer player = holding(helper, 1);
        sneakClick(helper, player, FIRST);
        // A row of 4,001 positions, under the cap, running far past any loaded chunk.
        click(helper, player, FIRST.east(4000));
        expectHeard(helper, player, "message.groundworks.blueprint_unloaded");
        expectBlank(helper, player);
        helper.succeed();
    }

    private static void charged(GameTestHelper helper) {
        ListeningPlayer player = holding(helper, 1);
        build(helper);
        copy(helper, player);
        stock(player, Map.of(Items.STONE, 3, Items.OAK_STAIRS, 2, Items.OAK_LOG, 1, Items.GLASS, 5));
        paste(helper, player);
        expectBuild(helper, ORIGIN);
        expectHeld(helper, player, Map.of(Items.STONE, 2, Items.OAK_STAIRS, 1, Items.OAK_LOG, 0, Items.GLASS, 4));
        helper.succeed();
    }

    private static void shortOfItems(GameTestHelper helper) {
        ListeningPlayer player = holding(helper, 1);
        build(helper);
        copy(helper, player);
        stock(player, Map.of(Items.STONE, 1, Items.OAK_STAIRS, 1, Items.OAK_LOG, 1));
        PlacementPlan plan = plan(helper, player);
        if (plan == null || plan.refusal() != Refusal.Blueprint.NOT_ENOUGH_ITEMS) {
            helper.fail("the plan was " + plan + ", not refused for items lacking");
        }
        paste(helper, player);
        expectHeard(helper, player, "message.groundworks.blueprint_not_enough_items");
        expectNothingPasted(helper);
        expectHeld(helper, player, Map.of(Items.STONE, 1, Items.OAK_STAIRS, 1, Items.OAK_LOG, 1));
        helper.succeed();
    }

    private static void taken(GameTestHelper helper) {
        ListeningPlayer player = creative(helper);
        build(helper);
        copy(helper, player);
        BlockPos taken = ORIGIN.east();
        helper.setBlock(taken, Blocks.COBBLESTONE);
        PlacementPlan plan = plan(helper, player);
        if (plan == null || !(plan.refusal() instanceof Refusal.At at) || at.reason() != Refusal.Blueprint.TAKEN
                || !at.pos().equals(helper.absolutePos(taken))) {
            helper.fail("the plan was " + plan + ", not refused as taken at the cobblestone");
        }
        paste(helper, player);
        expectHeard(helper, player, "message.groundworks.blueprint_taken");
        helper.assertBlockPresent(Blocks.COBBLESTONE, taken);
        helper.assertBlockPresent(Blocks.AIR, ORIGIN);
        helper.assertBlockPresent(Blocks.AIR, ORIGIN.offset(1, 1, 1));
        helper.succeed();
    }

    private static void standingStaysUncharged(GameTestHelper helper) {
        ListeningPlayer player = holding(helper, 1);
        build(helper);
        copy(helper, player);
        helper.setBlock(ORIGIN, STONE);
        stock(player, Map.of(Items.OAK_STAIRS, 1, Items.OAK_LOG, 1, Items.GLASS, 1));
        PlacementPlan plan = plan(helper, player);
        if (plan == null || plan.isRefused() || plan.blocks().size() != 3) {
            helper.fail("the plan was " + plan + ", not the three blocks missing accepted");
        }
        paste(helper, player);
        expectBuild(helper, ORIGIN);
        expectHeld(helper, player, Map.of(Items.OAK_STAIRS, 0, Items.OAK_LOG, 0, Items.GLASS, 0));
        helper.succeed();
    }

    private static void mayNotBuildFirst(GameTestHelper helper) {
        ListeningPlayer player = creative(helper);
        build(helper);
        copy(helper, player);
        helper.setBlock(ORIGIN.east(), Blocks.COBBLESTONE);
        player.setGameMode(GameType.ADVENTURE);
        PlacementPlan plan = plan(helper, player);
        if (plan == null || plan.refusal() != Refusal.Blueprint.MAY_NOT_BUILD) {
            helper.fail("the plan was " + plan + ", not refused where the player may not build");
        }
        paste(helper, player);
        expectNothingPasted(helper);
        helper.succeed();
    }

    private static void clearsTheCorner(GameTestHelper helper) {
        ListeningPlayer player = holding(helper, 1);
        build(helper);
        sneakClick(helper, player, FIRST);
        sneakUseInTheAir(helper, player);
        expectHeard(helper, player, "message.groundworks.blueprint_corner_cleared");
        click(helper, player, SECOND);
        if (player.heard.contains("message.groundworks.blueprint_copied")) {
            helper.fail("a click after the Corner was cleared captured");
        }
        expectBlank(helper, player);
        helper.succeed();
    }

    private static void clearsTheCopy(GameTestHelper helper) {
        ListeningPlayer player = creative(helper);
        build(helper);
        copy(helper, player);
        sneakUseInTheAir(helper, player);
        expectHeard(helper, player, "message.groundworks.blueprint_cleared");
        if (!ItemStack.isSameItemSameComponents(player.getMainHandItem(), new ItemStack(Items.PAPER))) {
            helper.fail("the cleared sheet is " + player.getMainHandItem() + ", which doesn't stack with paper");
        }
        paste(helper, player);
        expectNothingPasted(helper);
        helper.succeed();
    }

    private static void cornerElsewhereIsNone(GameTestHelper helper) {
        ListeningPlayer player = holding(helper, 1);
        build(helper);
        player.getMainHandItem().set(Groundworks.BLUEPRINT_CORNER.get(),
                new BlueprintCorner(Level.NETHER, helper.absolutePos(FIRST)));
        click(helper, player, SECOND);
        if (player.heard.contains("message.groundworks.blueprint_copied")) {
            helper.fail("a Corner stored in the nether captured a Selection here");
        }
        expectBlank(helper, player);
        helper.succeed();
    }

    /** A second player, in the nether, holding a sheet the first player's Copy is on, pastes it there. */
    private static void anotherPlayerElsewhere(GameTestHelper helper) {
        ListeningPlayer owner = holding(helper, 1);
        build(helper);
        copy(helper, owner);
        ServerLevel nether = helper.getLevel().getServer().getLevel(Level.NETHER);
        BlockPos floor = new BlockPos(8, 100, 8);
        BlockPos.betweenClosed(floor.offset(-2, 0, -2), floor.offset(2, 3, 2))
                .forEach(pos -> nether.setBlock(pos, Blocks.AIR.defaultBlockState(), Block.UPDATE_CLIENTS));
        nether.setBlock(floor, STONE, Block.UPDATE_CLIENTS);
        ListeningPlayer other = new ListeningPlayer(nether, Vec3.atBottomCenterOf(floor.offset(-6, 1, 0)));
        other.setGameMode(GameType.CREATIVE);
        other.setItemInHand(InteractionHand.MAIN_HAND, owner.getMainHandItem().copy());
        other.gameMode.useItemOn(other, nether, other.getMainHandItem(), InteractionHand.MAIN_HAND, topOf(floor));
        BlockPos origin = floor.offset(-1, 1, -1);
        BUILD.forEach((local, state) -> {
            if (nether.getBlockState(origin.offset(local)) != state) {
                helper.fail("the nether holds " + nether.getBlockState(origin.offset(local)) + " at "
                        + origin.offset(local) + ", not " + state);
            }
        });
        BlockPos.betweenClosed(floor.offset(-2, 0, -2), floor.offset(2, 3, 2))
                .forEach(pos -> nether.setBlock(pos, Blocks.AIR.defaultBlockState(), Block.UPDATE_CLIENTS));
        helper.succeed();
    }

    /**
     * The world data saved, and read back from its file into a store of its own, which stands in
     * for the server's: the sheet still pastes from it.
     */
    private static void survivesReload(GameTestHelper helper) {
        ListeningPlayer player = creative(helper);
        build(helper);
        copy(helper, player);
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
        paste(helper, player);
        expectBuild(helper, ORIGIN);
        helper.succeed();
    }

    // ---- Helpers ---------------------------------------------------------------------------------

    private static void build(GameTestHelper helper) {
        BUILD.forEach((local, state) -> helper.setBlock(FIRST.offset(local), state));
    }

    private static void expectBuild(GameTestHelper helper, BlockPos origin) {
        for (int x = 0; x < 2; x++) {
            for (int y = 0; y < 2; y++) {
                for (int z = 0; z < 2; z++) {
                    BlockPos local = new BlockPos(x, y, z);
                    BlockState wanted = BUILD.get(local);
                    BlockPos at = origin.offset(local);
                    BlockState there = helper.getBlockState(at);
                    if (wanted != null && there != wanted) {
                        helper.fail("holds " + there + ", not " + wanted, at);
                    }
                }
            }
        }
    }

    private static void expectNothingPasted(GameTestHelper helper) {
        BUILD.keySet().forEach(local -> {
            BlockPos at = ORIGIN.offset(local);
            if (BUILD.get(local) == helper.getBlockState(at)) {
                helper.fail("the paste laid " + helper.getBlockState(at), at);
            }
        });
    }

    private static void copy(GameTestHelper helper, ListeningPlayer player) {
        sneakClick(helper, player, FIRST);
        click(helper, player, SECOND);
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

    private static void sneakClick(GameTestHelper helper, ListeningPlayer player, BlockPos pos) {
        player.setShiftKeyDown(true);
        click(helper, player, pos);
        player.setShiftKeyDown(false);
    }

    private static void sneakUseInTheAir(GameTestHelper helper, ListeningPlayer player) {
        player.setShiftKeyDown(true);
        player.gameMode.useItem(player, helper.getLevel(), player.getMainHandItem(), InteractionHand.MAIN_HAND);
        player.setShiftKeyDown(false);
    }

    private static BlockHitResult topOf(BlockPos absolute) {
        return new BlockHitResult(Vec3.atCenterOf(absolute).relative(Direction.UP, 0.5), Direction.UP, absolute, false);
    }

    private static ListeningPlayer holding(GameTestHelper helper, int sheets) {
        ListeningPlayer player = new ListeningPlayer(helper, new BlockPos(5, 1, 0));
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.PAPER, sheets));
        return player;
    }

    private static ListeningPlayer creative(GameTestHelper helper) {
        ListeningPlayer player = holding(helper, 1);
        player.setGameMode(GameType.CREATIVE);
        return player;
    }

    private static void stock(ListeningPlayer player, Map<Item, Integer> counts) {
        int slot = MAIN_SLOT;
        for (Map.Entry<Item, Integer> entry : counts.entrySet()) {
            player.getInventory().setItem(slot++, new ItemStack(entry.getKey(), entry.getValue()));
        }
    }

    /** The slot of a paper sheet standing alone in its stack, or -1. */
    private static int sheetSlot(ListeningPlayer player) {
        var inventory = player.getInventory().getNonEquipmentItems();
        for (int slot = 0; slot < inventory.size(); slot++) {
            ItemStack stack = inventory.get(slot);
            if (stack.is(Items.PAPER) && stack.getMaxStackSize() == 1) {
                return slot;
            }
        }
        return -1;
    }

    private static void expectBlank(GameTestHelper helper, ListeningPlayer player) {
        ItemStack held = player.getMainHandItem();
        if (held.getMaxStackSize() != 64 || held.has(Groundworks.BLUEPRINT.get())) {
            helper.fail("the held sheet is " + held + ", not blank");
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
