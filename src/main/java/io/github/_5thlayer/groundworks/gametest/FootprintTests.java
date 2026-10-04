// SPDX-FileCopyrightText: 2026 5thlayer
// SPDX-License-Identifier: MIT

package io.github._5thlayer.groundworks.gametest;

import java.util.List;

import io.github._5thlayer.groundworks.Footprint;
import io.github._5thlayer.groundworks.FootprintPartBlock;
import io.github._5thlayer.groundworks.Groundworks;
import io.github._5thlayer.groundworks.Height;
import io.github._5thlayer.groundworks.PlacementPlan;
import io.github._5thlayer.groundworks.Placements;
import io.github._5thlayer.groundworks.Refusal;
import io.github._5thlayer.groundworks.Rotate;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.capabilities.BlockCapabilityCache;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.transfer.energy.EnergyHandler;
import org.jspecify.annotations.Nullable;

/**
 * Footprints, on {@link TestFootprint}: an origin, a part forward, one above and one to the side.
 * The player stands west of {@link #ORIGIN} looking east and clicks the floor's top under it, so the
 * footprint faces west, back at the player. Breaking it drops the origin's item through a loot
 * table in the {@code footprints} test pack, so those tests run in its batch.
 */
final class FootprintTests {

    private static final BlockPos ORIGIN = new BlockPos(2, 1, 2);
    private static final Direction PLACED_FACING = Direction.WEST;
    private static final String PACK = "footprints";

    private FootprintTests() {
    }

    static void register(GroundworksGameTests.Registrar tests) {
        tests.test("a_footprint_is_placed_whole_facing_the_player_for_one_item", 20, FootprintTests::placedWhole);
        tests.test("a_footprint_with_a_position_taken_is_refused_whole_and_places_nothing", 20,
                FootprintTests::refusedWhole);
        tests.test("a_footprint_raised_onto_a_taken_spot_is_drawn_refused_whole", 20, FootprintTests::raisedOntoATakenSpot);
        tests.test("a_footprint_part_forwards_an_energy_lookup_to_its_origin", 20, FootprintTests::forwardsEnergy);
        tests.test("a_capability_cache_beside_a_footprint_part_sees_it_placed_and_broken", 20,
                FootprintTests::cacheSeesPlacedAndBroken);
        tests.test("a_piston_does_not_move_a_footprint_part", 40, FootprintTests::pistonLeavesAPart);
        tests.test("rotate_in_place_on_a_part_turns_the_footprint_whole_and_keeps_its_block_entity", 20,
                FootprintTests::turnsWhole);
        tests.test("a_footprint_with_no_room_to_turn_is_refused_whole_and_nothing_changes", 20,
                FootprintTests::turnRefused);
        tests.test("a_part_numbered_outside_its_shape_is_an_orphan_clicked_replaced_over_turned_and_broken_alone", 20,
                FootprintTests::outOfShapeOrphan);
        var withLoot = tests.withPack(PACK);
        withLoot.test("breaking_a_footprint_part_breaks_it_whole_and_drops_its_item_once", 20, helper ->
                brokenWhole(helper, ORIGIN.above()));
        withLoot.test("breaking_a_footprint_origin_breaks_it_whole_and_drops_its_item_once", 20, helper ->
                brokenWhole(helper, ORIGIN));
        withLoot.test("a_creative_player_breaking_a_footprint_part_breaks_it_whole_and_drops_nothing", 20,
                FootprintTests::creativeBreak);
        withLoot.test("a_footprint_part_removed_by_a_command_takes_the_footprint_down_and_drops_its_item_once", 20,
                FootprintTests::removedByCommand);
    }

    private static void placedWhole(GameTestHelper helper) {
        ListeningPlayer player = holding(helper, 2);
        PlacementPlan plan = plan(helper, player);
        if (plan == null || plan.isRefused() || plan.blocks().size() != 4) {
            helper.fail("the plan was " + plan + ", not the whole footprint accepted");
        }
        click(helper, player);
        expectStanding(helper, PLACED_FACING);
        if (player.getMainHandItem().getCount() != 1) {
            helper.fail("the placement charged " + (2 - player.getMainHandItem().getCount()) + " items, not 1");
        }
        helper.succeed();
    }

    private static void refusedWhole(GameTestHelper helper) {
        BlockPos taken = forwardOf(ORIGIN, PLACED_FACING);
        helper.setBlock(taken, Blocks.STONE);
        ListeningPlayer player = holding(helper, 2);
        PlacementPlan plan = plan(helper, player);
        if (plan == null || plan.refusal() != Refusal.Footprint.BLOCKED) {
            helper.fail("the plan was " + plan + ", not refused as blocked");
        }
        click(helper, player);
        for (BlockPos pos : List.of(ORIGIN, ORIGIN.above(), ORIGIN.north())) {
            if (!helper.getBlockState(pos).isAir()) {
                helper.fail("a refused footprint placed " + helper.getBlockState(pos), pos);
            }
        }
        if (!helper.getBlockState(taken).is(Blocks.STONE) || player.getMainHandItem().getCount() != 2) {
            helper.fail("a refused footprint changed the taken block or charged an item");
        }
        helper.succeed();
    }

    /** A height moves the click onto a spot the player chose, so a taken one is drawn red rather than not at all. */
    private static void raisedOntoATakenSpot(GameTestHelper helper) {
        helper.setBlock(ORIGIN.above(), Blocks.STONE);
        ListeningPlayer player = holding(helper, 2);
        player.getMainHandItem().set(Groundworks.HEIGHT.get(), new Height(1));
        PlacementPlan plan = plan(helper, player);
        if (plan == null || plan.refusal() != Refusal.Footprint.BLOCKED) {
            helper.fail("the plan was " + plan + ", not refused as blocked");
        }
        helper.succeed();
    }

    /** A part has no block entity, so it tells a cache of its capabilities itself, laid and gone. */
    private static void cacheSeesPlacedAndBroken(GameTestHelper helper) {
        BlockPos part = helper.absolutePos(ORIGIN.above());
        BlockCapabilityCache<EnergyHandler, @Nullable Direction> cache =
                BlockCapabilityCache.create(Capabilities.Energy.BLOCK, helper.getLevel(), part, Direction.UP);
        if (cache.getCapability() != null) {
            helper.fail("the cache saw energy before the footprint stood");
        }
        ListeningPlayer player = place(helper);
        EnergyHandler atOrigin = helper.getLevel().getCapability(Capabilities.Energy.BLOCK, helper.absolutePos(ORIGIN), Direction.UP);
        if (atOrigin == null || cache.getCapability() != atOrigin) {
            helper.fail("the cache saw " + cache.getCapability() + " once the footprint stood, not the origin's " + atOrigin);
        }
        player.gameMode.destroyBlock(helper.absolutePos(ORIGIN));
        if (cache.getCapability() != null) {
            helper.fail("the cache still saw " + cache.getCapability() + " once the footprint was broken");
        }
        helper.succeed();
    }

    /** A piston pushing one part would strand the rest, so the part doesn't move and the footprint stands. */
    private static void pistonLeavesAPart(GameTestHelper helper) {
        place(helper);
        BlockPos part = forwardOf(ORIGIN, PLACED_FACING);
        BlockPos piston = part.east();
        helper.setBlock(piston, Blocks.PISTON.defaultBlockState().setValue(DirectionalBlock.FACING, Direction.WEST));
        helper.setBlock(piston.east(), Blocks.REDSTONE_BLOCK);
        helper.runAfterDelay(10, () -> {
            expectStanding(helper, PLACED_FACING);
            helper.succeed();
        });
    }

    private static void forwardsEnergy(GameTestHelper helper) {
        place(helper);
        EnergyHandler atOrigin = helper.getLevel().getCapability(Capabilities.Energy.BLOCK, helper.absolutePos(ORIGIN), Direction.UP);
        for (BlockPos part : partsOf(helper, PLACED_FACING)) {
            EnergyHandler atPart = helper.getLevel().getCapability(Capabilities.Energy.BLOCK, part, Direction.UP);
            if (atOrigin == null || atPart != atOrigin) {
                helper.fail("the part at " + part + " answered " + atPart + ", not the origin's " + atOrigin);
            }
        }
        helper.succeed();
    }

    private static void turnsWhole(GameTestHelper helper) {
        ListeningPlayer player = place(helper);
        var entity = helper.getBlockEntity(ORIGIN, TestFootprint.HoldsEnergy.class);
        List<BlockPos> before = TestFootprint.FOOTPRINT.positions(helper.absolutePos(ORIGIN), PLACED_FACING);
        // An empty hand, since a held rotatable item takes the press for its plan.
        player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
        Rotate.press(player, helper.absolutePos(ORIGIN.above()), false);
        Direction turned = PLACED_FACING.getClockWise();
        expectStanding(helper, turned);
        List<BlockPos> after = TestFootprint.FOOTPRINT.positions(helper.absolutePos(ORIGIN), turned);
        for (BlockPos old : before) {
            if (!after.contains(old) && !helper.getLevel().getBlockState(old).isAir()) {
                helper.fail("the turn left " + helper.getLevel().getBlockState(old) + " at " + old + ", where the footprint stood");
            }
        }
        if (helper.getBlockEntity(ORIGIN, TestFootprint.HoldsEnergy.class) != entity) {
            helper.fail("the turn replaced the origin's block entity", ORIGIN);
        }
        if (!player.heard.isEmpty()) {
            helper.fail("a turn that went through named a refusal: " + player.heard);
        }
        helper.succeed();
    }

    private static void turnRefused(GameTestHelper helper) {
        ListeningPlayer player = place(helper);
        BlockPos taken = TestFootprint.FOOTPRINT.positions(helper.absolutePos(ORIGIN), PLACED_FACING.getClockWise()).get(1);
        helper.getLevel().setBlockAndUpdate(taken, Blocks.STONE.defaultBlockState());
        // An empty hand, since a held rotatable item takes the press for its plan.
        player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
        Rotate.press(player, helper.absolutePos(ORIGIN.above()), false);
        expectStanding(helper, PLACED_FACING);
        if (!helper.getLevel().getBlockState(taken).is(Blocks.STONE) || !player.heard.equals(List.of(Footprint.TURN_BLOCKED))) {
            helper.fail("the refused turn changed the taken block or told " + player.heard);
        }
        helper.succeed();
    }

    private static void brokenWhole(GameTestHelper helper, BlockPos broken) {
        ListeningPlayer player = place(helper);
        player.gameMode.destroyBlock(helper.absolutePos(broken));
        expectGone(helper);
        expectDropped(helper, 1);
        helper.succeed();
    }

    private static void creativeBreak(GameTestHelper helper) {
        ListeningPlayer player = place(helper);
        player.setGameMode(GameType.CREATIVE);
        player.gameMode.destroyBlock(helper.absolutePos(ORIGIN.above()));
        expectGone(helper);
        expectDropped(helper, 0);
        helper.succeed();
    }

    private static void removedByCommand(GameTestHelper helper) {
        place(helper);
        helper.setBlock(ORIGIN.above(), Blocks.AIR);
        expectGone(helper);
        expectDropped(helper, 1);
        helper.succeed();
    }

    /**
     * A part numbered past its shape, as one left in a world from before its Consumer shrank the
     * shape, names no origin: the preview plans no replace of it, though the held item is in its
     * group, a click with the item or without passes, it forwards nothing, Rotate in Place leaves
     * it, and a break removes it alone. Any of these that asked where its origin stands threw.
     */
    private static void outOfShapeOrphan(GameTestHelper helper) {
        int number = TestFootprint.SHAPE.partCount() + 2;
        BlockState orphan = TestFootprint.FOOTPRINT.stateAt(1, PLACED_FACING).setValue(FootprintPartBlock.PART, number);
        helper.setBlock(ORIGIN, orphan);
        BlockPos at = helper.absolutePos(ORIGIN);
        BlockHitResult onTop = new BlockHitResult(Vec3.atCenterOf(at).relative(Direction.UP, 0.5), Direction.UP, at, false);
        ListeningPlayer player = holding(helper, 2);

        PlacementPlan plan = Placements.planFor(player.level(), player, InteractionHand.MAIN_HAND, player.getMainHandItem(), onTop);
        if (plan != null && plan.replaces().contains(at)) {
            helper.fail("the preview planned a replace of an orphan part: " + plan, ORIGIN);
        }
        player.gameMode.useItemOn(player, helper.getLevel(), player.getMainHandItem(), InteractionHand.MAIN_HAND, onTop);
        player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
        player.gameMode.useItemOn(player, helper.getLevel(), ItemStack.EMPTY, InteractionHand.MAIN_HAND, onTop);
        if (!helper.getBlockState(ORIGIN).equals(orphan)) {
            helper.fail("a click changed the orphan part to " + helper.getBlockState(ORIGIN), ORIGIN);
        }
        if (helper.getLevel().getCapability(Capabilities.Energy.BLOCK, at, Direction.UP) != null) {
            helper.fail("an orphan part forwarded an energy lookup", ORIGIN);
        }
        Rotate.press(player, at, false);
        if (!helper.getBlockState(ORIGIN).equals(orphan)) {
            helper.fail("Rotate in Place changed the orphan part to " + helper.getBlockState(ORIGIN), ORIGIN);
        }
        player.gameMode.destroyBlock(at);
        if (!helper.getBlockState(ORIGIN).isAir()) {
            helper.fail("breaking the orphan part left " + helper.getBlockState(ORIGIN), ORIGIN);
        }
        if (!helper.getEntities(EntityType.ITEM).isEmpty()) {
            helper.fail("breaking the orphan part dropped " + helper.getEntities(EntityType.ITEM));
        }
        helper.succeed();
    }

    /** Places the footprint with the player's click, failing unless it stands. */
    private static ListeningPlayer place(GameTestHelper helper) {
        ListeningPlayer player = holding(helper, 2);
        click(helper, player);
        expectStanding(helper, PLACED_FACING);
        return player;
    }

    /** Fails unless the origin stands facing this way and each part stands where the shape puts it, numbered. */
    private static void expectStanding(GameTestHelper helper, Direction facing) {
        List<BlockPos> positions = TestFootprint.FOOTPRINT.positions(helper.absolutePos(ORIGIN), facing);
        for (int i = 0; i < positions.size(); i++) {
            BlockState there = helper.getLevel().getBlockState(positions.get(i));
            if (!there.equals(TestFootprint.FOOTPRINT.stateAt(i, facing))) {
                helper.fail("block " + i + " of the footprint facing " + facing + " at " + positions.get(i) + " is " + there);
            }
        }
    }

    /** Fails unless no block of the footprint, facing the way it was placed, is left. */
    private static void expectGone(GameTestHelper helper) {
        for (BlockPos pos : TestFootprint.FOOTPRINT.positions(helper.absolutePos(ORIGIN), PLACED_FACING)) {
            if (!helper.getLevel().getBlockState(pos).isAir()) {
                helper.fail("the break left " + helper.getLevel().getBlockState(pos) + " at " + pos);
            }
        }
    }

    private static void expectDropped(GameTestHelper helper, int count) {
        List<ItemStack> drops = helper.getEntities(EntityType.ITEM).stream().map(ItemEntity::getItem).toList();
        int dropped = drops.stream().filter(stack -> stack.is(TestFootprint.ITEM.get())).mapToInt(ItemStack::getCount).sum();
        long other = drops.stream().filter(stack -> !stack.is(TestFootprint.ITEM.get())).count();
        if (dropped != count || other != 0) {
            helper.fail("the break dropped " + dropped + " footprint items and " + other + " others, not " + count);
        }
    }

    private static List<BlockPos> partsOf(GameTestHelper helper, Direction facing) {
        List<BlockPos> positions = TestFootprint.FOOTPRINT.positions(helper.absolutePos(ORIGIN), facing);
        return positions.subList(1, positions.size());
    }

    private static BlockPos forwardOf(BlockPos origin, Direction facing) {
        return origin.offset(TestFootprint.SHAPE.offsetOfPart(1).inWorld(facing));
    }

    private static @Nullable PlacementPlan plan(GameTestHelper helper, ListeningPlayer player) {
        return Placements.planFor(player.level(), player, InteractionHand.MAIN_HAND, player.getMainHandItem(), onTheFloor(helper));
    }

    private static void click(GameTestHelper helper, ListeningPlayer player) {
        player.gameMode.useItemOn(player, helper.getLevel(), player.getMainHandItem(), InteractionHand.MAIN_HAND, onTheFloor(helper));
    }

    private static ListeningPlayer holding(GameTestHelper helper, int count) {
        ListeningPlayer player = new ListeningPlayer(helper, new BlockPos(0, 1, 2));
        player.setYRot(Direction.EAST.toYRot());
        player.setYHeadRot(Direction.EAST.toYRot());
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(TestFootprint.ITEM.get(), count));
        return player;
    }

    /** The top of the floor block under {@link #ORIGIN}. */
    private static BlockHitResult onTheFloor(GameTestHelper helper) {
        BlockPos floor = helper.absolutePos(ORIGIN.below());
        return new BlockHitResult(Vec3.atCenterOf(floor).relative(Direction.UP, 0.5), Direction.UP, floor, false);
    }
}
