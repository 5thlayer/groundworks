// SPDX-FileCopyrightText: 2026 5thlayer
// SPDX-License-Identifier: MIT

package io.github._5thlayer.groundworks.gametest;

import java.util.Map;

import io.github._5thlayer.groundworks.Assemblies;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.inventory.BeaconMenu;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BeaconBlockEntity;
import net.minecraft.world.level.block.entity.ConduitBlockEntity;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

/**
 * The Assemblies Groundworks ships for vanilla, each placed by a real click and judged by vanilla's
 * own rule: the golem spawns, the beacon reaches its level, the conduit activates, the portal frame
 * lights. They load in every batch, so these tests need no pack.
 *
 * <p>The player stands west of the spot looking east, so forward is east and the player's right is
 * south. The golems stand on the platform; the taller shapes on the tall platform, round its middle.
 */
final class VanillaAssemblyTests {

    private static final BlockPos PLAYER = new BlockPos(0, 1, 2);
    private static final BlockPos SPOT = new BlockPos(2, 1, 2);

    private static final BlockPos TALL_PLAYER = new BlockPos(0, 1, 5);
    private static final BlockPos TALL_SPOT = new BlockPos(5, 1, 5);

    private static final int MAIN_SLOT = 20;

    private VanillaAssemblyTests() {
    }

    static void register(GroundworksGameTests.Registrar tests) {
        tests.test("a_carved_pumpkin_places_an_iron_golem_which_spawns", 20, VanillaAssemblyTests::ironGolem);
        tests.test("a_carved_pumpkin_places_a_snow_golem_after_next_assembly_which_spawns", 20,
                VanillaAssemblyTests::snowGolem);
        tests.tallTest("a_beacon_places_its_level_four_pyramid_all_iron_and_reaches_level_four", 300,
                VanillaAssemblyTests::beaconPyramid);
        tests.tallTest("a_conduit_places_its_sixteen_block_ring_underwater_and_activates", 200,
                VanillaAssemblyTests::conduitRing);
        tests.tallTest("a_conduit_places_its_full_frame_underwater_after_next_assembly_and_activates", 200,
                VanillaAssemblyTests::conduitFullFrame);
        tests.tallTest("obsidian_places_alone_then_the_unlit_portal_frame_after_next_assembly_which_lights", 40,
                VanillaAssemblyTests::portalFrame);
    }

    private static void ironGolem(GameTestHelper helper) {
        ListeningPlayer player = holding(helper, PLAYER, Items.CARVED_PUMPKIN, 1);
        stock(player, Items.IRON_BLOCK, 4);
        click(helper, player, SPOT);
        if (helper.getEntities(EntityType.IRON_GOLEM).isEmpty()) {
            helper.fail("no iron golem spawned from the placed body and pumpkin");
        }
        expectHeld(helper, player, Map.of(Items.CARVED_PUMPKIN, 0, Items.IRON_BLOCK, 0));
        helper.succeed();
    }

    private static void snowGolem(GameTestHelper helper) {
        ListeningPlayer player = holding(helper, PLAYER, Items.CARVED_PUMPKIN, 1);
        stock(player, Items.SNOW_BLOCK, 2);
        Assemblies.press(player, false);
        click(helper, player, SPOT);
        if (helper.getEntities(EntityType.SNOW_GOLEM).isEmpty()) {
            helper.fail("no snow golem spawned from the placed body and pumpkin");
        }
        expectHeld(helper, player, Map.of(Items.CARVED_PUMPKIN, 0, Items.SNOW_BLOCK, 0));
        helper.succeed();
    }

    /** 164 base blocks, 9×9 to 3×3; the player holds more iron than gold, so all of it is iron. */
    private static void beaconPyramid(GameTestHelper helper) {
        ListeningPlayer player = holding(helper, TALL_PLAYER, Items.BEACON, 1);
        player.getInventory().setItem(20, new ItemStack(Items.IRON_BLOCK, 64));
        player.getInventory().setItem(21, new ItemStack(Items.IRON_BLOCK, 64));
        player.getInventory().setItem(22, new ItemStack(Items.IRON_BLOCK, 36));
        player.getInventory().setItem(23, new ItemStack(Items.GOLD_BLOCK, 10));
        for (int press = 0; press < 3; press++) {
            Assemblies.press(player, false);
        }
        click(helper, player, TALL_SPOT);
        BlockPos beacon = TALL_SPOT.above(4);
        expectStanding(helper, Map.of(beacon, Blocks.BEACON));
        for (int step = 1; step <= 4; step++) {
            for (int dx = -step; dx <= step; dx++) {
                for (int dz = -step; dz <= step; dz++) {
                    BlockPos base = beacon.offset(dx, -step, dz);
                    if (!helper.getBlockState(base).is(Blocks.IRON_BLOCK)) {
                        helper.fail("found " + helper.getBlockState(base) + ", not iron", base);
                    }
                }
            }
        }
        expectHeld(helper, player, Map.of(Items.BEACON, 0, Items.IRON_BLOCK, 0, Items.GOLD_BLOCK, 10));
        helper.succeedWhen(() -> {
            BeaconBlockEntity entity = helper.getBlockEntity(beacon, BeaconBlockEntity.class);
            int levels = ((BeaconMenu) entity.createMenu(0, player.getInventory(), player)).getLevels();
            helper.assertValueEqual(4, levels, "the beacon's level");
        });
    }

    /** The ring stands upright across the player's view, the conduit two above the ground in its middle. */
    private static void conduitRing(GameTestHelper helper) {
        flood(helper);
        ListeningPlayer player = holding(helper, TALL_PLAYER, Items.CONDUIT, 1);
        stock(player, Items.PRISMARINE, 16);
        click(helper, player, TALL_SPOT);
        BlockPos conduit = TALL_SPOT.above(2);
        expectStanding(helper, Map.of(TALL_SPOT, Blocks.PRISMARINE, TALL_SPOT.above(4), Blocks.PRISMARINE,
                conduit.north(2), Blocks.PRISMARINE, conduit.south(2), Blocks.PRISMARINE,
                conduit.north(), Blocks.WATER, conduit.above(), Blocks.WATER));
        expectHeld(helper, player, Map.of(Items.CONDUIT, 0, Items.PRISMARINE, 0));
        expectActive(helper, conduit);
    }

    private static void conduitFullFrame(GameTestHelper helper) {
        flood(helper);
        ListeningPlayer player = holding(helper, TALL_PLAYER, Items.CONDUIT, 1);
        stock(player, Items.PRISMARINE, 42);
        Assemblies.press(player, false);
        click(helper, player, TALL_SPOT);
        BlockPos conduit = TALL_SPOT.above(2);
        expectStanding(helper, Map.of(conduit.east(2), Blocks.PRISMARINE, conduit.west(2), Blocks.PRISMARINE,
                conduit.north(2).east(2), Blocks.PRISMARINE, conduit.above(2).south(), Blocks.PRISMARINE,
                conduit.above().east().south(), Blocks.WATER, conduit.below().east(), Blocks.WATER));
        expectHeld(helper, player, Map.of(Items.CONDUIT, 0, Items.PRISMARINE, 0));
        expectActive(helper, conduit);
    }

    /**
     * Its plane runs across the player's view: the Key and the obsidian to the player's right make
     * its bottom, and its inside is two wide and three tall over them.
     */
    private static void portalFrame(GameTestHelper helper) {
        ListeningPlayer player = holding(helper, TALL_PLAYER, Items.OBSIDIAN, 11);
        BlockPos alone = TALL_SPOT.south(4);
        click(helper, player, alone);
        expectStanding(helper, Map.of(alone, Blocks.OBSIDIAN));
        if (!helper.getBlockState(alone.above()).isAir()) {
            helper.fail("a fresh obsidian came with " + helper.getBlockState(alone.above()), alone.above());
        }
        Assemblies.press(player, false);
        click(helper, player, TALL_SPOT);
        BlockPos right = TALL_SPOT.south();
        expectStanding(helper, Map.of(TALL_SPOT, Blocks.OBSIDIAN, right, Blocks.OBSIDIAN,
                TALL_SPOT.north().above(), Blocks.OBSIDIAN, TALL_SPOT.north().above(3), Blocks.OBSIDIAN,
                right.south().above(), Blocks.OBSIDIAN, right.south().above(3), Blocks.OBSIDIAN,
                TALL_SPOT.above(4), Blocks.OBSIDIAN, right.above(4), Blocks.OBSIDIAN));
        for (BlockPos corner : new BlockPos[] {TALL_SPOT.north(), right.south(), TALL_SPOT.north().above(4),
                right.south().above(4), TALL_SPOT.above(), right.above(3), TALL_SPOT.above(5)}) {
            if (!helper.getBlockState(corner).isAir()) {
                helper.fail("found " + helper.getBlockState(corner) + " outside the frame or inside it", corner);
            }
        }
        expectHeld(helper, player, Map.of(Items.OBSIDIAN, 0));

        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.FLINT_AND_STEEL));
        BlockPos key = helper.absolutePos(TALL_SPOT);
        player.gameMode.useItemOn(player, helper.getLevel(), player.getMainHandItem(), InteractionHand.MAIN_HAND,
                new BlockHitResult(Vec3.atCenterOf(key).relative(Direction.UP, 0.5), Direction.UP, key, false));
        helper.succeedWhen(() -> expectStanding(helper, Map.of(TALL_SPOT.above(), Blocks.NETHER_PORTAL,
                right.above(3), Blocks.NETHER_PORTAL)));
    }

    /** Water five wide, deep and tall round the tall spot, where a conduit's frame goes. */
    private static void flood(GameTestHelper helper) {
        for (int dx = -2; dx <= 2; dx++) {
            for (int dy = 0; dy <= 4; dy++) {
                for (int dz = -2; dz <= 2; dz++) {
                    helper.setBlock(TALL_SPOT.offset(dx, dy, dz), Blocks.WATER);
                }
            }
        }
    }

    private static void expectActive(GameTestHelper helper, BlockPos conduit) {
        expectStanding(helper, Map.of(conduit, Blocks.CONDUIT));
        helper.succeedWhen(() -> helper.assertTrue(helper.getBlockEntity(conduit, ConduitBlockEntity.class).isActive(),
                "the conduit is not active"));
    }

    private static void expectStanding(GameTestHelper helper, Map<BlockPos, Block> blocks) {
        blocks.forEach((pos, block) -> {
            if (!helper.getBlockState(pos).is(block)) {
                helper.fail("found " + helper.getBlockState(pos) + ", not " + block, pos);
            }
        });
    }

    private static void expectHeld(GameTestHelper helper, ListeningPlayer player, Map<Item, Integer> counts) {
        counts.forEach((item, count) -> {
            int held = player.getInventory().countItem(item);
            if (held != count) {
                helper.fail("the player holds " + held + " " + item + ", not " + count);
            }
        });
    }

    private static void stock(ListeningPlayer player, Item item, int count) {
        player.getInventory().setItem(MAIN_SLOT, new ItemStack(item, count));
    }

    private static ListeningPlayer holding(GameTestHelper helper, BlockPos at, Item key, int count) {
        ListeningPlayer player = new ListeningPlayer(helper, at);
        player.setYRot(Direction.EAST.toYRot());
        player.setYHeadRot(Direction.EAST.toYRot());
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(key, count));
        return player;
    }

    /** A click on the top of the floor block under this spot. */
    private static void click(GameTestHelper helper, ListeningPlayer player, BlockPos spot) {
        BlockPos floor = helper.absolutePos(spot.below());
        player.gameMode.useItemOn(player, helper.getLevel(), player.getMainHandItem(), InteractionHand.MAIN_HAND,
                new BlockHitResult(Vec3.atCenterOf(floor).relative(Direction.UP, 0.5), Direction.UP, floor, false));
    }
}
