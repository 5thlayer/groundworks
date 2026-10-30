// SPDX-FileCopyrightText: 2026 5thlayer
// SPDX-License-Identifier: MIT

package io.github._5thlayer.groundworks.gametest;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import io.github._5thlayer.groundworks.PlacementPlan;
import io.github._5thlayer.groundworks.Placements;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.Container;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.WallTorchBlock;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BedPart;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * A Copy's paste stands as it was captured: what hangs or stands on another block laid after it,
 * Companions placed by their block, what a Copy carries and what it doesn't, and a block that
 * can't stand where it lands left out. On the tall platform.
 *
 * <p>Each build stands in the three-by-three-by-three box from {@link #FIRST}, and is pasted on
 * the floor's top under {@link #SPOT}, so it lands from {@link #ORIGIN}, one west and one north of
 * the spot.
 */
final class BlueprintStandingTests {

    private static final BlockPos FIRST = new BlockPos(1, 1, 1);
    private static final BlockPos SECOND = FIRST.offset(2, 2, 2);
    private static final BlockPos SPOT = new BlockPos(7, 1, 7);
    private static final BlockPos ORIGIN = new BlockPos(6, 1, 6);
    private static final BlockState STONE = Blocks.STONE.defaultBlockState();
    private static final int MAIN_SLOT = 20;

    private BlueprintStandingTests() {
    }

    static void register(GroundworksGameTests.Registrar tests) {
        tests.tallTest("blueprint_a_paste_stands_its_torches_rails_and_carpets_as_captured", 20,
                BlueprintStandingTests::torchesRailsAndCarpets);
        tests.tallTest("blueprint_a_paste_lays_doors_and_beds_whole_each_charged_once", 20,
                BlueprintStandingTests::doorsAndBeds);
        tests.tallTest("blueprint_a_copy_pastes_a_full_chest_empty_and_a_waterlogged_slab_dry", 20,
                BlueprintStandingTests::emptyAndDry);
        tests.tallTest("blueprint_a_survival_paste_skips_water_and_fire", 20,
                BlueprintStandingTests::survivalSkipsWaterAndFire);
        tests.tallTest("blueprint_a_creative_paste_lays_water_and_fire", 20,
                BlueprintStandingTests::creativeLaysWaterAndFire);
        tests.tallTest("blueprint_a_torch_whose_wall_stayed_outside_is_left_out_uncharged_and_the_rest_lays", 20,
                BlueprintStandingTests::torchLeftOut);
    }

    /**
     * A stone floor with a pillar at its north-east, a torch on the pillar's west face, which the
     * Blueprint's order comes to before the pillar, rails along the north and down the middle, and
     * carpets on the floor.
     */
    private static void torchesRailsAndCarpets(GameTestHelper helper) {
        ListeningPlayer player = creative(helper);
        floor(helper);
        helper.setBlock(FIRST.offset(2, 1, 0), STONE);
        helper.setBlock(FIRST.offset(2, 2, 0), STONE);
        helper.setBlock(FIRST.offset(1, 2, 0), Blocks.WALL_TORCH.defaultBlockState()
                .setValue(WallTorchBlock.FACING, Direction.WEST));
        helper.setBlock(FIRST.offset(0, 1, 0), Blocks.RAIL);
        helper.setBlock(FIRST.offset(1, 1, 0), Blocks.RAIL);
        helper.setBlock(FIRST.offset(1, 1, 1), Blocks.RAIL);
        helper.setBlock(FIRST.offset(1, 1, 2), Blocks.RAIL);
        helper.setBlock(FIRST.offset(0, 1, 2), Blocks.WHITE_CARPET);
        helper.setBlock(FIRST.offset(2, 1, 2), Blocks.RED_CARPET);
        Map<BlockPos, BlockState> captured = standing(helper, FIRST);
        copy(helper, player);
        paste(helper, player);
        expectStanding(helper, captured, ORIGIN);
        helper.succeed();
    }

    /** A door on a stone block and a bed facing south, pasted in survival with one of each in the inventory. */
    private static void doorsAndBeds(GameTestHelper helper) {
        ListeningPlayer player = holding(helper);
        helper.setBlock(FIRST, STONE);
        BlockState door = Blocks.OAK_DOOR.defaultBlockState().setValue(DoorBlock.FACING, Direction.SOUTH);
        helper.setBlock(FIRST.above(), door.setValue(DoorBlock.HALF, DoubleBlockHalf.LOWER));
        helper.setBlock(FIRST.above(2), door.setValue(DoorBlock.HALF, DoubleBlockHalf.UPPER));
        BlockState bed = Blocks.RED_BED.defaultBlockState().setValue(BedBlock.FACING, Direction.SOUTH);
        helper.setBlock(FIRST.offset(2, 0, 0), bed.setValue(BedBlock.PART, BedPart.FOOT));
        helper.setBlock(FIRST.offset(2, 0, 1), bed.setValue(BedBlock.PART, BedPart.HEAD));
        Map<BlockPos, BlockState> captured = standing(helper, FIRST);
        copy(helper, player);
        stock(player, Map.of(Items.STONE, 1, Items.OAK_DOOR, 1, Items.RED_BED, 1));
        PlacementPlan plan = plan(helper, player);
        if (plan == null || plan.isRefused() || plan.blocks().size() != 3 || plan.companions().size() != 2) {
            helper.fail("the plan was " + plan + ", not the stone, the door and the bed with their two Companions");
        }
        paste(helper, player);
        expectStanding(helper, captured, ORIGIN);
        expectHeld(helper, player, Map.of(Items.STONE, 0, Items.OAK_DOOR, 0, Items.RED_BED, 0));
        helper.succeed();
    }

    private static void emptyAndDry(GameTestHelper helper) {
        ListeningPlayer player = holding(helper);
        helper.setBlock(FIRST, Blocks.CHEST);
        helper.getBlockEntity(FIRST, ChestBlockEntity.class).setItem(0, new ItemStack(Items.DIAMOND, 64));
        helper.setBlock(FIRST.east(2), Blocks.OAK_SLAB.defaultBlockState().setValue(SlabBlock.WATERLOGGED, true));
        copy(helper, player);
        stock(player, Map.of(Items.CHEST, 1, Items.OAK_SLAB, 1));
        paste(helper, player);
        helper.assertBlockPresent(Blocks.CHEST, ORIGIN);
        Container pasted = helper.getBlockEntity(ORIGIN, ChestBlockEntity.class);
        if (!pasted.isEmpty()) {
            helper.fail("the pasted chest holds " + pasted.getItem(0), ORIGIN);
        }
        BlockState slab = helper.getBlockState(ORIGIN.east(2));
        if (!slab.is(Blocks.OAK_SLAB) || slab.getValue(SlabBlock.WATERLOGGED)) {
            helper.fail("holds " + slab + ", not a dry oak slab", ORIGIN.east(2));
        }
        expectHeld(helper, player, Map.of(Items.CHEST, 0, Items.OAK_SLAB, 0));
        helper.succeed();
    }

    /**
     * A floor of stone with soul soil at its north-west, a ring of stone around a water source,
     * and soul fire on the soul soil in place of the ring's north-west.
     */
    private static void waterAndFire(GameTestHelper helper) {
        floor(helper);
        helper.setBlock(FIRST, Blocks.SOUL_SOIL);
        for (int x = 0; x < 3; x++) {
            for (int z = 0; z < 3; z++) {
                helper.setBlock(FIRST.offset(x, 1, z), STONE);
            }
        }
        helper.setBlock(FIRST.offset(1, 1, 1), Blocks.WATER);
        helper.setBlock(FIRST.offset(0, 1, 0), Blocks.SOUL_FIRE);
    }

    private static void survivalSkipsWaterAndFire(GameTestHelper helper) {
        ListeningPlayer player = holding(helper);
        waterAndFire(helper);
        copy(helper, player);
        stock(player, Map.of(Items.STONE, 15, Items.SOUL_SOIL, 1));
        paste(helper, player);
        helper.assertBlockPresent(Blocks.SOUL_SOIL, ORIGIN);
        helper.assertBlockPresent(Blocks.STONE, ORIGIN.offset(2, 1, 2));
        helper.assertBlockPresent(Blocks.AIR, ORIGIN.offset(1, 1, 1));
        helper.assertBlockPresent(Blocks.AIR, ORIGIN.offset(0, 1, 0));
        expectHeld(helper, player, Map.of(Items.STONE, 0, Items.SOUL_SOIL, 0));
        helper.succeed();
    }

    private static void creativeLaysWaterAndFire(GameTestHelper helper) {
        ListeningPlayer player = creative(helper);
        waterAndFire(helper);
        Map<BlockPos, BlockState> captured = standing(helper, FIRST);
        copy(helper, player);
        paste(helper, player);
        expectStanding(helper, captured, ORIGIN);
        helper.succeed();
    }

    /**
     * A torch at the first Corner hanging on a wall west of the box, and stone at the far corner:
     * pasted in the open, the torch has no wall.
     */
    private static void torchLeftOut(GameTestHelper helper) {
        ListeningPlayer player = holding(helper);
        helper.setBlock(FIRST.west(), STONE);
        helper.setBlock(FIRST, Blocks.WALL_TORCH.defaultBlockState().setValue(WallTorchBlock.FACING, Direction.EAST));
        helper.setBlock(SECOND, STONE);
        copy(helper, player);
        stock(player, Map.of(Items.STONE, 1, Items.TORCH, 1));
        PlacementPlan plan = plan(helper, player);
        BlockPos torch = helper.absolutePos(ORIGIN);
        if (plan == null || plan.isRefused() || plan.blocks().size() != 1 || !plan.leftOut().equals(List.of(torch))) {
            helper.fail("the plan was " + plan + ", not the stone laid with the torch left out");
        }
        paste(helper, player);
        helper.assertBlockPresent(Blocks.STONE, ORIGIN.offset(2, 2, 2));
        helper.assertBlockPresent(Blocks.AIR, ORIGIN);
        expectHeld(helper, player, Map.of(Items.STONE, 0, Items.TORCH, 1));
        helper.succeed();
    }

    // ---- Helpers ---------------------------------------------------------------------------------

    private static void floor(GameTestHelper helper) {
        for (int x = 0; x < 3; x++) {
            for (int z = 0; z < 3; z++) {
                helper.setBlock(FIRST.offset(x, 0, z), STONE);
            }
        }
    }

    /** What stands in the box from {@code origin}, air included, by where it stands from the box's lowest corner. */
    private static Map<BlockPos, BlockState> standing(GameTestHelper helper, BlockPos origin) {
        Map<BlockPos, BlockState> standing = new HashMap<>();
        BlockPos.betweenClosed(BlockPos.ZERO, SECOND.subtract(FIRST))
                .forEach(local -> standing.put(local.immutable(), helper.getBlockState(origin.offset(local))));
        return standing;
    }

    private static void expectStanding(GameTestHelper helper, Map<BlockPos, BlockState> wanted, BlockPos origin) {
        wanted.forEach((local, state) -> {
            BlockState there = helper.getBlockState(origin.offset(local));
            if (there != state) {
                helper.fail("holds " + there + ", not " + state, origin.offset(local));
            }
        });
    }

    private static void copy(GameTestHelper helper, ListeningPlayer player) {
        player.setShiftKeyDown(true);
        click(helper, player, FIRST);
        player.setShiftKeyDown(false);
        click(helper, player, SECOND);
        if (!player.heard.contains("message.groundworks.blueprint_copied")) {
            helper.fail("the player heard " + player.heard + ", not the copy made");
        }
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
}
