// SPDX-FileCopyrightText: 2026 5thlayer
// SPDX-License-Identifier: MIT

package io.github._5thlayer.groundworks.gametest;

import java.util.List;
import java.util.Map;

import io.github._5thlayer.groundworks.PlacementPlan;
import io.github._5thlayer.groundworks.Placements;
import io.github._5thlayer.groundworks.Refusal;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntityType;
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
 * Assemblies, from the {@code assemblies} test pack: its tower, keyed by lodestone, stands
 * cobblestone forward of the Key's column, one stone under the Key and one to the player's right,
 * and its snow golem is two snow blocks under a carved pumpkin. Its glowstone Assembly's Key is
 * not in {@code groundworks:assembly_keys}, and two malformed declarations are skipped.
 *
 * <p>The player stands west of {@link #SPOT} looking east and clicks the floor's top under it, so
 * forward is east and the player's right is south.
 */
final class AssemblyTests {

    private static final String PACK = "assemblies";
    private static final BlockPos SPOT = new BlockPos(2, 1, 2);
    private static final BlockPos KEY = SPOT.above();
    private static final BlockPos FORWARD = SPOT.east();
    private static final BlockPos RIGHT = SPOT.south();

    /** Where a main-inventory slot is, out of the hotbar. */
    private static final int MAIN_SLOT = 20;
    private static final int OTHER_MAIN_SLOT = 30;

    private AssemblyTests() {
    }

    static void register(GroundworksGameTests.Registrar tests) {
        var withPack = tests.withPack(PACK);
        withPack.test("an_assembly_is_placed_whole_resting_on_the_face_facing_the_player_with_its_key_last", 20,
                AssemblyTests::placedWhole);
        withPack.test("an_assembly_places_its_key_last_so_a_snow_golem_spawns", 20, AssemblyTests::golemSpawns);
        withPack.test("an_assembly_is_charged_from_the_main_inventory", 20, AssemblyTests::chargedFromTheMainInventory);
        withPack.test("an_assembly_short_of_blocks_is_refused_naming_what_is_missing_and_takes_nothing", 20,
                AssemblyTests::refusedForMissing);
        withPack.test("an_assembly_is_placed_free_in_creative", 20, AssemblyTests::freeInCreative);
        withPack.test("an_assembly_with_a_position_taken_is_refused_whole_and_places_nothing", 20,
                AssemblyTests::refusedWhenTaken);
        withPack.test("an_assembly_goes_over_water_and_grass", 20, AssemblyTests::overWaterAndGrass);
        withPack.test("an_assembly_keeps_a_block_of_its_own_standing_uncharged", 20, AssemblyTests::keepsItsOwnBlock);
        withPack.test("sneak_placing_a_key_places_it_alone", 20, AssemblyTests::sneakPlacesTheKeyAlone);
        withPack.test("a_key_outside_the_tag_places_as_vanilla", 20, AssemblyTests::untaggedKeyPlacesAsVanilla);
        withPack.test("breaking_one_block_of_an_assembly_breaks_only_it", 20, AssemblyTests::breaksOneBlock);
    }

    private static void placedWhole(GameTestHelper helper) {
        ListeningPlayer player = holding(helper, Items.LODESTONE, 2);
        stock(player, MAIN_SLOT, Items.STONE, 2);
        stock(player, OTHER_MAIN_SLOT, Items.COBBLESTONE, 1);
        PlacementPlan plan = plan(helper, player);
        if (plan == null || plan.isRefused() || plan.blocks().size() != 4) {
            helper.fail("the plan was " + plan + ", not the whole tower accepted");
        }
        PlacementPlan.Placed last = plan.blocks().getLast();
        if (!last.pos().equals(helper.absolutePos(KEY)) || !last.state().is(Blocks.LODESTONE)) {
            helper.fail("the plan's last block was " + last + ", not the Key");
        }
        click(helper, player);
        expectTower(helper);
        helper.succeed();
    }

    /** Vanilla's golem rule runs as its pumpkin goes down, and sees the body only when the pumpkin goes last. */
    private static void golemSpawns(GameTestHelper helper) {
        ListeningPlayer player = holding(helper, Items.CARVED_PUMPKIN, 1);
        stock(player, MAIN_SLOT, Items.SNOW_BLOCK, 2);
        click(helper, player);
        if (helper.getEntities(EntityType.SNOW_GOLEM).isEmpty()) {
            helper.fail("no snow golem spawned from the placed body and pumpkin");
        }
        helper.succeed();
    }

    private static void chargedFromTheMainInventory(GameTestHelper helper) {
        ListeningPlayer player = holding(helper, Items.LODESTONE, 3);
        stock(player, MAIN_SLOT, Items.STONE, 5);
        stock(player, OTHER_MAIN_SLOT, Items.COBBLESTONE, 4);
        click(helper, player);
        expectTower(helper);
        expectHeld(helper, player, Map.of(Items.LODESTONE, 2, Items.STONE, 3, Items.COBBLESTONE, 3));
        helper.succeed();
    }

    private static void refusedForMissing(GameTestHelper helper) {
        ListeningPlayer player = holding(helper, Items.LODESTONE, 1);
        stock(player, MAIN_SLOT, Items.STONE, 1);
        PlacementPlan plan = plan(helper, player);
        if (plan == null || plan.refusal() != Refusal.Assembly.NOT_ENOUGH_ITEMS) {
            helper.fail("the plan was " + plan + ", not refused for blocks missing");
        }
        click(helper, player);
        expectNothingPlaced(helper);
        expectHeld(helper, player, Map.of(Items.LODESTONE, 1, Items.STONE, 1));
        if (!player.heard.contains("message.groundworks.assembly_not_enough_items")) {
            helper.fail("the player heard " + player.heard + ", not what is missing");
        }
        helper.succeed();
    }

    private static void freeInCreative(GameTestHelper helper) {
        ListeningPlayer player = holding(helper, Items.LODESTONE, 1);
        player.setGameMode(GameType.CREATIVE);
        click(helper, player);
        expectTower(helper);
        if (player.getMainHandItem().getCount() != 1) {
            helper.fail("a creative player was charged the Key");
        }
        helper.succeed();
    }

    private static void refusedWhenTaken(GameTestHelper helper) {
        helper.setBlock(FORWARD, Blocks.DIRT);
        ListeningPlayer player = holding(helper, Items.LODESTONE, 1);
        stock(player, MAIN_SLOT, Items.STONE, 2);
        stock(player, OTHER_MAIN_SLOT, Items.COBBLESTONE, 1);
        PlacementPlan plan = plan(helper, player);
        Refusal taken = new Refusal.At(Refusal.Assembly.TAKEN, helper.absolutePos(FORWARD));
        if (plan == null || !taken.equals(plan.refusal())) {
            helper.fail("the plan was " + plan + ", not refused where the dirt stands");
        }
        click(helper, player);
        for (BlockPos pos : List.of(SPOT, KEY, RIGHT)) {
            if (!helper.getBlockState(pos).isAir()) {
                helper.fail("a refused Assembly placed " + helper.getBlockState(pos), pos);
            }
        }
        if (!helper.getBlockState(FORWARD).is(Blocks.DIRT)) {
            helper.fail("a refused Assembly changed the block in its way");
        }
        expectHeld(helper, player, Map.of(Items.LODESTONE, 1, Items.STONE, 2, Items.COBBLESTONE, 1));
        if (!player.heard.contains("message.groundworks.assembly_taken")) {
            helper.fail("the player heard " + player.heard + ", not that a position is taken");
        }
        helper.succeed();
    }

    private static void overWaterAndGrass(GameTestHelper helper) {
        helper.setBlock(FORWARD, Blocks.WATER);
        helper.setBlock(RIGHT, Blocks.SHORT_GRASS);
        ListeningPlayer player = holding(helper, Items.LODESTONE, 1);
        stock(player, MAIN_SLOT, Items.STONE, 2);
        stock(player, OTHER_MAIN_SLOT, Items.COBBLESTONE, 1);
        click(helper, player);
        expectTower(helper);
        helper.succeed();
    }

    private static void keepsItsOwnBlock(GameTestHelper helper) {
        helper.setBlock(FORWARD, Blocks.COBBLESTONE);
        ListeningPlayer player = holding(helper, Items.LODESTONE, 1);
        stock(player, MAIN_SLOT, Items.STONE, 2);
        PlacementPlan plan = plan(helper, player);
        if (plan == null || plan.isRefused() || plan.blocks().size() != 3) {
            helper.fail("the plan was " + plan + ", not the tower less the cobblestone standing");
        }
        click(helper, player);
        expectTower(helper);
        expectHeld(helper, player, Map.of(Items.STONE, 0));
        helper.succeed();
    }

    private static void sneakPlacesTheKeyAlone(GameTestHelper helper) {
        ListeningPlayer player = holding(helper, Items.LODESTONE, 2);
        stock(player, MAIN_SLOT, Items.STONE, 2);
        stock(player, OTHER_MAIN_SLOT, Items.COBBLESTONE, 1);
        player.setShiftKeyDown(true);
        click(helper, player);
        expectAlone(helper, Blocks.LODESTONE);
        expectHeld(helper, player, Map.of(Items.LODESTONE, 1, Items.STONE, 2, Items.COBBLESTONE, 1));
        helper.succeed();
    }

    private static void untaggedKeyPlacesAsVanilla(GameTestHelper helper) {
        ListeningPlayer player = holding(helper, Items.GLOWSTONE, 2);
        stock(player, MAIN_SLOT, Items.STONE, 1);
        click(helper, player);
        expectAlone(helper, Blocks.GLOWSTONE);
        expectHeld(helper, player, Map.of(Items.GLOWSTONE, 1, Items.STONE, 1));
        helper.succeed();
    }

    private static void breaksOneBlock(GameTestHelper helper) {
        ListeningPlayer player = holding(helper, Items.LODESTONE, 1);
        player.setGameMode(GameType.CREATIVE);
        click(helper, player);
        expectTower(helper);
        player.gameMode.destroyBlock(helper.absolutePos(FORWARD));
        if (!helper.getBlockState(FORWARD).isAir()) {
            helper.fail("the broken block still stands");
        }
        expectStanding(helper, Map.of(SPOT, Blocks.STONE, RIGHT, Blocks.STONE, KEY, Blocks.LODESTONE));
        helper.succeed();
    }

    private static void expectTower(GameTestHelper helper) {
        expectStanding(helper, Map.of(SPOT, Blocks.STONE, RIGHT, Blocks.STONE, FORWARD, Blocks.COBBLESTONE,
                KEY, Blocks.LODESTONE));
    }

    private static void expectStanding(GameTestHelper helper, Map<BlockPos, Block> blocks) {
        blocks.forEach((pos, block) -> {
            if (!helper.getBlockState(pos).is(block)) {
                helper.fail("found " + helper.getBlockState(pos) + ", not " + block, pos);
            }
        });
    }

    /** The Key alone on the spot, as vanilla places it, and nothing of the Assembly round it. */
    private static void expectAlone(GameTestHelper helper, Block key) {
        if (!helper.getBlockState(SPOT).is(key)) {
            helper.fail("found " + helper.getBlockState(SPOT) + ", not the Key alone", SPOT);
        }
        for (BlockPos pos : List.of(KEY, FORWARD, RIGHT)) {
            if (!helper.getBlockState(pos).isAir()) {
                helper.fail("the Key alone came with " + helper.getBlockState(pos), pos);
            }
        }
    }

    private static void expectNothingPlaced(GameTestHelper helper) {
        for (BlockPos pos : List.of(SPOT, KEY, FORWARD, RIGHT)) {
            if (!helper.getBlockState(pos).isAir()) {
                helper.fail("a refused Assembly placed " + helper.getBlockState(pos), pos);
            }
        }
    }

    /** That the main inventory holds exactly these counts of these items. */
    private static void expectHeld(GameTestHelper helper, ListeningPlayer player, Map<Item, Integer> counts) {
        counts.forEach((item, count) -> {
            int held = player.getInventory().countItem(item);
            if (held != count) {
                helper.fail("the player holds " + held + " " + item + ", not " + count);
            }
        });
    }

    private static void stock(ListeningPlayer player, int slot, Item item, int count) {
        player.getInventory().setItem(slot, new ItemStack(item, count));
    }

    private static @Nullable PlacementPlan plan(GameTestHelper helper, ListeningPlayer player) {
        return Placements.planFor(player.level(), player, InteractionHand.MAIN_HAND, player.getMainHandItem(), onTheFloor(helper));
    }

    private static void click(GameTestHelper helper, ListeningPlayer player) {
        player.gameMode.useItemOn(player, helper.getLevel(), player.getMainHandItem(), InteractionHand.MAIN_HAND, onTheFloor(helper));
    }

    private static ListeningPlayer holding(GameTestHelper helper, Item key, int count) {
        ListeningPlayer player = new ListeningPlayer(helper, new BlockPos(0, 1, 2));
        player.setYRot(Direction.EAST.toYRot());
        player.setYHeadRot(Direction.EAST.toYRot());
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(key, count));
        return player;
    }

    /** The top of the floor block under {@link #SPOT}. */
    private static BlockHitResult onTheFloor(GameTestHelper helper) {
        BlockPos floor = helper.absolutePos(SPOT.below());
        return new BlockHitResult(Vec3.atCenterOf(floor).relative(Direction.UP, 0.5), Direction.UP, floor, false);
    }
}
