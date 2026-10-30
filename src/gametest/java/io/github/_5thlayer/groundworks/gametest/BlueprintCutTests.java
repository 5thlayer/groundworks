// SPDX-FileCopyrightText: 2026 5thlayer
// SPDX-License-Identifier: MIT

package io.github._5thlayer.groundworks.gametest;

import java.util.List;

import io.github._5thlayer.groundworks.NextChoice;
import io.github._5thlayer.groundworks.PlacementPlan;
import io.github._5thlayer.groundworks.Placements;
import io.github._5thlayer.groundworks.UndoPaste;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.WallTorchBlock;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

/**
 * A Cut with paper, the vanilla Consumer's sheet, on the tall platform: a box from {@link #FIRST}
 * to {@link #SECOND}, chosen by Next Choice on the blank sheet, captured, and pasted on the floor
 * under {@link #SPOT}, where the box lands from {@link #ORIGIN}. The player stands in survival
 * with nothing but the sheet, so a paste that goes through was not charged.
 */
final class BlueprintCutTests {

    private static final BlockPos FIRST = new BlockPos(1, 1, 1);
    private static final BlockPos SECOND = FIRST.offset(2, 2, 2);
    private static final BlockPos SPOT = new BlockPos(7, 1, 7);
    private static final BlockPos ORIGIN = new BlockPos(6, 1, 6);
    private static final BlockPos CHEST = FIRST.offset(1, 0, 1);
    private static final BlockState STONE = Blocks.STONE.defaultBlockState();

    private BlueprintCutTests() {
    }

    static void register(GroundworksGameTests.Registrar tests) {
        tests.tallTest("blueprint_cut_moves_a_room_with_its_full_chest_uncharged_and_blanks_the_sheet", 20,
                BlueprintCutTests::movesARoom);
        tests.tallTest("blueprint_cut_next_choice_steps_back_to_copy", 20, BlueprintCutTests::backToCopy);
        tests.tallTest("blueprint_cut_a_second_sheet_finds_it_gone_after_the_first_paste", 20,
                BlueprintCutTests::secondSheetFindsItGone);
        tests.tallTest("blueprint_cut_sand_above_falls_and_a_torch_on_an_outside_wall_drops", 60,
                BlueprintCutTests::outsideFallsAndBreaks);
        tests.tallTest("blueprint_cut_leaves_bedrock_and_a_claimed_block_in_the_world", 20,
                BlueprintCutTests::bedrockAndClaimStay);
        tests.tallTest("blueprint_cut_sneak_use_in_the_air_is_refused", 20, BlueprintCutTests::neverCleared);
        tests.tallTest("blueprint_cut_sheet_survives_lava_and_never_despawns", 60, BlueprintCutTests::sheetSurvivesLava);
        tests.tallTest("blueprint_cut_torch_that_cant_stand_drops_at_the_player_s_feet", 20,
                BlueprintCutTests::torchDrops);
        tests.tallTest("blueprint_cut_undo_paste_is_refused", 20, BlueprintCutTests::undoRefused);
    }

    /**
     * Planks, a chest full of cobblestone and glass: the Corner stored before Cut is chosen still
     * bounds the capture, which takes the room out with nothing dropped; the paste lays it
     * with the chest's contents, though the player holds none of its blocks, and blanks the sheet.
     */
    private static void movesARoom(GameTestHelper helper) {
        ListeningPlayer player = holding(helper);
        helper.setBlock(FIRST, Blocks.OAK_PLANKS);
        helper.setBlock(CHEST, Blocks.CHEST);
        fill(helper, CHEST);
        helper.setBlock(SECOND, Blocks.GLASS);
        player.setShiftKeyDown(true);
        click(helper, player, FIRST);
        player.setShiftKeyDown(false);
        NextChoice.press(player, false);
        expectHeard(helper, player, "message.groundworks.blueprint_cutting");
        click(helper, player, SECOND);
        expectHeard(helper, player, "message.groundworks.blueprint_cut");
        BlockPos.betweenClosed(FIRST, SECOND).forEach(pos -> helper.assertBlockPresent(Blocks.AIR, pos));
        helper.assertEntityNotPresent(EntityType.ITEM);
        PlacementPlan plan = Placements.planFor(player.level(), player, InteractionHand.MAIN_HAND,
                player.getMainHandItem(), topOf(helper.absolutePos(SPOT.below())));
        if (plan == null || plan.isRefused() || plan.blocks().size() != 3) {
            helper.fail("the Cut's plan was " + plan + ", not its three blocks, uncharged");
        }
        paste(helper, player, SPOT);
        helper.assertBlockPresent(Blocks.OAK_PLANKS, ORIGIN);
        helper.assertBlockPresent(Blocks.CHEST, ORIGIN.offset(1, 0, 1));
        helper.assertBlockPresent(Blocks.GLASS, ORIGIN.offset(2, 2, 2));
        if (!(helper.getBlockEntity(ORIGIN.offset(1, 0, 1), ChestBlockEntity.class) instanceof ChestBlockEntity chest)
                || chest.getItem(0).getCount() != 64 || chest.getItem(chest.getContainerSize() - 1).getCount() != 64) {
            helper.fail("the pasted chest doesn't hold the cut chest's contents", ORIGIN.offset(1, 0, 1));
        }
        helper.assertEntityNotPresent(EntityType.ITEM);
        expectBlank(helper, player);
        helper.succeed();
    }

    /** A second press of Next Choice on the blank sheet steps back to Copy, which leaves the world as it was. */
    private static void backToCopy(GameTestHelper helper) {
        ListeningPlayer player = holding(helper);
        helper.setBlock(FIRST, STONE);
        NextChoice.press(player, false);
        NextChoice.press(player, false);
        expectHeard(helper, player, "message.groundworks.blueprint_copying");
        capture(helper, player, FIRST, FIRST);
        expectHeard(helper, player, "message.groundworks.blueprint_copied");
        helper.assertBlockPresent(Blocks.STONE, FIRST);
        helper.succeed();
    }

    private static void secondSheetFindsItGone(GameTestHelper helper) {
        ListeningPlayer player = holding(helper);
        helper.setBlock(FIRST, STONE);
        cut(helper, player, FIRST, FIRST);
        ItemStack second = player.getMainHandItem().copy();
        paste(helper, player, SPOT);
        helper.assertBlockPresent(Blocks.STONE, ORIGIN.offset(1, 0, 1));
        player.setItemInHand(InteractionHand.MAIN_HAND, second);
        paste(helper, player, SPOT.offset(-4, 0, 0));
        expectHeard(helper, player, "message.groundworks.blueprint_gone");
        helper.assertBlockPresent(Blocks.AIR, ORIGIN.offset(-3, 0, 1));
        expectBlank(helper, player);
        helper.succeed();
    }

    /**
     * Stone fills the box's middle column, with sand on it above the box, and a torch outside the
     * box hangs on the stone at its first Corner: once cut, the sand falls through the box and the
     * torch breaks and drops.
     */
    private static void outsideFallsAndBreaks(GameTestHelper helper) {
        ListeningPlayer player = holding(helper);
        BlockPos column = FIRST.offset(1, 0, 1);
        for (int y = 0; y < 3; y++) {
            helper.setBlock(column.above(y), STONE);
        }
        helper.setBlock(column.above(3), Blocks.SAND);
        helper.setBlock(FIRST, STONE);
        BlockPos hung = FIRST.west();
        helper.setBlock(hung, Blocks.WALL_TORCH.defaultBlockState().setValue(WallTorchBlock.FACING, Direction.WEST));
        cut(helper, player, FIRST, SECOND);
        helper.succeedWhen(() -> {
            helper.assertBlockPresent(Blocks.SAND, column);
            helper.assertBlockPresent(Blocks.AIR, column.above(3));
            helper.assertBlockPresent(Blocks.AIR, hung);
            helper.assertItemEntityPresent(Items.TORCH, hung, 2);
        });
    }

    /** Bedrock stays silently, and a claimed block stays with a message; the stone between them is cut. */
    private static void bedrockAndClaimStay(GameTestHelper helper) {
        ListeningPlayer player = holding(helper);
        helper.setBlock(FIRST, STONE);
        helper.setBlock(FIRST.east(), Blocks.BEDROCK);
        helper.setBlock(FIRST.east(2), Blocks.DIRT);
        BlockPos claimed = helper.absolutePos(FIRST.east(2));
        RotateInPlaceTests.CLAIMED.add(claimed);
        try {
            cut(helper, player, FIRST, FIRST.east(2), "message.groundworks.blueprint_cut_left_out");
        } finally {
            RotateInPlaceTests.CLAIMED.remove(claimed);
        }
        if (!mentions(player.messages.getLast(), "message.groundworks.blueprint_left_out.unbreakable")
                || mentions(player.messages.getLast(), "message.groundworks.blueprint_left_out.straddling")) {
            helper.fail("the player was told " + player.messages.getLast() + ", not of a block they can't break");
        }
        helper.assertBlockPresent(Blocks.AIR, FIRST);
        helper.assertBlockPresent(Blocks.BEDROCK, FIRST.east());
        helper.assertBlockPresent(Blocks.DIRT, FIRST.east(2));
        paste(helper, player, SPOT);
        BlockPos.betweenClosed(SPOT.offset(-3, 0, -3), SPOT.offset(3, 2, 3)).forEach(pos -> {
            BlockState there = helper.getBlockState(pos);
            if (there.is(Blocks.BEDROCK) || there.is(Blocks.DIRT)) {
                helper.fail("the paste laid " + there, pos);
            }
        });
        helper.succeed();
    }

    private static void neverCleared(GameTestHelper helper) {
        ListeningPlayer player = holding(helper);
        helper.setBlock(FIRST, STONE);
        cut(helper, player, FIRST, FIRST);
        player.setShiftKeyDown(true);
        player.gameMode.useItem(player, helper.getLevel(), player.getMainHandItem(), InteractionHand.MAIN_HAND);
        player.setShiftKeyDown(false);
        expectHeard(helper, player, "message.groundworks.blueprint_cant_clear_a_cut");
        paste(helper, player, SPOT);
        helper.assertBlockPresent(Blocks.STONE, ORIGIN.offset(1, 0, 1));
        helper.succeed();
    }

    /**
     * The Cut's sheet and a Copy's, thrown into one pool of lava walled in stone: the Copy's
     * burns, the Cut's stays, its lifetime unlimited.
     */
    private static void sheetSurvivesLava(GameTestHelper helper) {
        ListeningPlayer player = holding(helper);
        helper.setBlock(FIRST, STONE);
        cut(helper, player, FIRST, FIRST);
        ItemStack cutSheet = player.getMainHandItem();
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.PAPER));
        capture(helper, player, FIRST.east(), FIRST.east());
        ItemStack copySheet = player.getMainHandItem();
        BlockPos pool = new BlockPos(7, 1, 7);
        BlockPos.betweenClosed(pool.offset(-1, 0, -1), pool.offset(1, 1, 1)).forEach(pos -> helper.setBlock(pos, STONE));
        helper.setBlock(pool, Blocks.LAVA);
        helper.setBlock(pool.above(), Blocks.AIR);
        ItemEntity cutItem = drop(helper, pool, cutSheet);
        ItemEntity copyItem = drop(helper, pool, copySheet);
        helper.runAfterDelay(40, () -> {
            if (!copyItem.isRemoved()) {
                helper.fail("the Copy's sheet didn't burn in the lava, so lava never reached them");
            }
            if (cutItem.isRemoved()) {
                helper.fail("the Cut's sheet burnt in the lava");
            }
            if (cutItem.getAge() >= 0) {
                helper.fail("the Cut's sheet ages toward despawning, at " + cutItem.getAge());
            }
            helper.succeed();
        });
    }

    /** A torch hung from inside the box on a wall outside it has no wall where it lands, and drops at the player's feet. */
    private static void torchDrops(GameTestHelper helper) {
        ListeningPlayer player = holding(helper);
        helper.setBlock(FIRST.west(), STONE);
        helper.setBlock(FIRST, Blocks.WALL_TORCH.defaultBlockState().setValue(WallTorchBlock.FACING, Direction.EAST));
        helper.setBlock(SECOND, STONE);
        cut(helper, player, FIRST, SECOND);
        helper.assertBlockPresent(Blocks.STONE, FIRST.west());
        paste(helper, player, SPOT);
        helper.assertBlockPresent(Blocks.STONE, ORIGIN.offset(2, 2, 2));
        helper.assertBlockPresent(Blocks.AIR, ORIGIN);
        List<ItemEntity> dropped = helper.getLevel().getEntitiesOfClass(ItemEntity.class,
                new AABB(player.blockPosition()).inflate(1), item -> item.getItem().is(Items.TORCH));
        if (dropped.size() != 1) {
            helper.fail("dropped " + dropped + " at the player's feet, not the torch");
        }
        expectBlank(helper, player);
        helper.succeed();
    }

    private static void undoRefused(GameTestHelper helper) {
        ListeningPlayer player = holding(helper);
        helper.setBlock(FIRST, STONE);
        cut(helper, player, FIRST, FIRST);
        UndoPaste.press(player);
        expectHeard(helper, player, "message.groundworks.blueprint_undo_on_a_cut");
        helper.assertBlockPresent(Blocks.AIR, FIRST);
        helper.succeed();
    }

    // ---- Helpers ---------------------------------------------------------------------------------

    private static void fill(GameTestHelper helper, BlockPos chest) {
        ChestBlockEntity entity = helper.getBlockEntity(chest, ChestBlockEntity.class);
        for (int slot = 0; slot < entity.getContainerSize(); slot++) {
            entity.setItem(slot, new ItemStack(Items.COBBLESTONE, 64));
        }
    }

    private static ItemEntity drop(GameTestHelper helper, BlockPos at, ItemStack stack) {
        Vec3 where = Vec3.atCenterOf(helper.absolutePos(at));
        ItemEntity item = new ItemEntity(helper.getLevel(), where.x, where.y, where.z, stack.copy(), 0, 0, 0);
        helper.getLevel().addFreshEntity(item);
        return item;
    }

    /** Cut chosen on the blank sheet, and the box between the two captured. */
    private static void cut(GameTestHelper helper, ListeningPlayer player, BlockPos first, BlockPos second) {
        cut(helper, player, first, second, "message.groundworks.blueprint_cut");
    }

    private static void cut(GameTestHelper helper, ListeningPlayer player, BlockPos first, BlockPos second, String told) {
        NextChoice.press(player, false);
        capture(helper, player, first, second);
        if (player.heard.isEmpty() || !player.heard.getLast().equals(told)) {
            helper.fail("the player last heard " + player.heard + ", not " + told);
        }
    }

    private static void capture(GameTestHelper helper, ListeningPlayer player, BlockPos first, BlockPos second) {
        player.setShiftKeyDown(true);
        click(helper, player, first);
        player.setShiftKeyDown(false);
        click(helper, player, second);
    }

    /** A paste resting on the floor under {@code spot}. */
    private static void paste(GameTestHelper helper, ListeningPlayer player, BlockPos spot) {
        click(helper, player, spot.below());
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
