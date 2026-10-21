// SPDX-FileCopyrightText: 2026 5thlayer
// SPDX-License-Identifier: MIT

package io.github._5thlayer.groundworks.gametest;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import io.github._5thlayer.groundworks.Assemblies;
import io.github._5thlayer.groundworks.Groundworks;
import io.github._5thlayer.groundworks.PlacementPlan;
import io.github._5thlayer.groundworks.Placements;
import io.github._5thlayer.groundworks.Refusal;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DoublePlantBlock;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.WallTorchBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * Assemblies, from the {@code assemblies} test pack: its tower, keyed by lodestone, stands
 * cobblestone forward of the Key's column, one stone under the Key and one to the player's right.
 * Its glowstone Assembly's Key is not in {@code groundworks:assembly_keys}; its cartography table's
 * is a Key stated in code alone, as a Consumer mod may state one. Two malformed
 * declarations are skipped, as is the smithing table's, which names fire, a block with no item, and
 * a target's declaration naming a tag the game lacks; the loom's names a tag with no blocks, and loads. Its
 * plinth, keyed by a target, is a 2×2 base of beacon base blocks, iron by default, with the target
 * over the base's near left block; its gate is a bookshelf with stone to the player's right and the
 * two positions forward of them kept empty. Its porch is a crafting table with stairs forward of it
 * and the position to their right kept empty; its cactus stands on sand with cobblestone to its
 * right, beside which it would not stand; its oak door has stone over it, where vanilla's
 * placement wants the door's upper half. Its sconce is a torch over nothing with stone forward of
 * it, on cobblestone; its golem trap is a note block with cobblestone to the player's left and an
 * iron golem's body and pumpkin to its right, the golem's leg beside it and the position beside
 * the leg's far side kept empty. Its barrel stands on the tests' block that notes it was told it
 * was placed.
 *
 * <p>The player stands west of {@link #SPOT} looking east and clicks the floor's top under it, so
 * forward is east and the player's right is south.
 *
 * <p>Next Assembly runs on the {@code assembly_choices} pack: a lodestone stands on one stone, its
 * first Assembly, or on two, its second; a fletching table has two declarations, the second by id
 * starting alone. The Key placed last is proved on vanilla's own Assemblies, a golem, in {@link
 * VanillaAssemblyTests}.
 */
final class AssemblyTests {

    private static final String PACK = "assemblies";
    private static final BlockPos SPOT = new BlockPos(2, 1, 2);
    private static final BlockPos KEY = SPOT.above();
    private static final BlockPos FORWARD = SPOT.east();
    private static final BlockPos RIGHT = SPOT.south();
    private static final BlockPos FORWARD_RIGHT = FORWARD.south();
    private static final List<BlockPos> BASE = List.of(SPOT, RIGHT, FORWARD, FORWARD_RIGHT);

    /** Next Assembly's pack, and a second spot, clear of the first's Assembly, for a second click. */
    private static final String CHOICES_PACK = "assembly_choices";
    private static final BlockPos OTHER_SPOT = SPOT.north(2);

    /** Where a main-inventory slot is, out of the hotbar. */
    private static final int MAIN_SLOT = 20;
    private static final int OTHER_MAIN_SLOT = 30;

    private AssemblyTests() {
    }

    static void register(GroundworksGameTests.Registrar tests) {
        tests.keys(Blocks.CARTOGRAPHY_TABLE);
        var withPack = tests.withPack(PACK);
        withPack.test("an_assembly_is_placed_whole_resting_on_the_face_facing_the_player_with_its_key_last", 20,
                AssemblyTests::placedWhole);
        withPack.test("an_assembly_laid_round_the_placing_player_lifts_them_onto_its_key", 20,
                AssemblyTests::liftsThePlayerOntoTheKey);
        withPack.test("an_assembly_is_charged_from_the_main_inventory", 20, AssemblyTests::chargedFromTheMainInventory);
        withPack.test("an_assembly_short_of_blocks_is_refused_naming_what_is_missing_and_takes_nothing", 20,
                AssemblyTests::refusedForMissing);
        withPack.test("an_assembly_is_placed_free_in_creative", 20, AssemblyTests::freeInCreative);
        withPack.test("an_assembly_with_a_position_taken_is_refused_whole_and_places_nothing", 20,
                AssemblyTests::refusedWhenTaken);
        withPack.test("an_assembly_goes_over_water_and_grass", 20, AssemblyTests::overWaterAndGrass);
        withPack.test("an_assembly_keeps_a_block_of_its_own_standing_uncharged", 20, AssemblyTests::keepsItsOwnBlock);
        withPack.test("an_assembly_tells_each_block_laid_before_its_key_it_was_placed_with_its_own_item", 20,
                AssemblyTests::tellsEachBlockPlaced);
        withPack.test("sneak_placing_a_key_places_it_alone", 20, AssemblyTests::sneakPlacesTheKeyAlone);
        withPack.test("a_key_outside_the_tag_places_as_vanilla", 20, AssemblyTests::untaggedKeyPlacesAsVanilla);
        withPack.test("a_key_stated_only_in_code_places_its_assembly", 20, AssemblyTests::keyStatedInCode);
        withPack.test("breaking_one_block_of_an_assembly_breaks_only_it", 20, AssemblyTests::breaksOneBlock);
        withPack.test("an_assemblys_tag_positions_are_filled_from_the_block_held_most_of_first", 20,
                AssemblyTests::uniformFill);
        withPack.test("an_assembly_short_of_blocks_for_its_tag_positions_is_refused_and_takes_nothing", 20,
                AssemblyTests::tagPositionsShort);
        withPack.test("an_assemblys_tag_positions_all_take_the_member_held_most_of_in_creative", 20,
                AssemblyTests::tagMostHeldInCreative);
        withPack.test("an_assemblys_tag_positions_take_their_default_in_creative_holding_no_member", 20,
                AssemblyTests::tagDefaultsInCreative);
        withPack.test("an_assembly_keeps_a_block_its_tag_takes_standing_uncharged", 20,
                AssemblyTests::keepsATaggedBlock);
        withPack.test("an_assembly_clears_grass_from_its_empty_positions_and_keeps_water", 20,
                AssemblyTests::clearsEmptyPositions);
        withPack.test("an_assemblys_plan_names_the_grass_it_clears_and_not_air_or_water", 20,
                AssemblyTests::planNamesWhatItClears);
        withPack.test("an_assembly_with_an_empty_position_taken_is_refused_whole_and_places_nothing", 20,
                AssemblyTests::refusedWhenEmptyTaken);
        withPack.test("an_assembly_keeps_the_water_of_a_waterlogged_plant_at_an_empty_position", 20,
                AssemblyTests::keepsTheWaterOfAPlant);
        withPack.test("an_assembly_clearing_tall_grass_drops_nothing_and_leaves_no_half", 20,
                AssemblyTests::clearsTallGrassWithoutDrops);
        withPack.test("an_assembly_refused_for_taken_positions_points_at_the_first_in_its_order", 20,
                AssemblyTests::takenPointsAtTheFirst);
        withPack.test("an_assemblys_oriented_blocks_turn_with_it", 20, AssemblyTests::orientedBlocksTurn);
        withPack.test("an_assembly_naming_a_block_with_no_item_is_not_loaded", 20, AssemblyTests::noItemNotLoaded);
        withPack.test("an_assembly_naming_an_unknown_tag_is_not_loaded", 20, AssemblyTests::unknownTagNotLoaded);
        withPack.test("an_assembly_naming_an_empty_tag_loads_and_takes_its_default", 20,
                AssemblyTests::emptyTagLoads);
        withPack.test("an_assembly_whose_key_would_not_stand_on_it_is_refused_whole_and_places_nothing", 20,
                AssemblyTests::keyWouldNotStandRefused);
        withPack.test("an_assembly_whose_key_would_have_no_room_once_laid_is_refused_whole_and_places_nothing", 20,
                AssemblyTests::keyWithNoRoomRefused);
        withPack.test("an_assemblys_key_hangs_on_a_wall_the_shape_builds", 20, AssemblyTests::keyHangsOnItsWall);
        withPack.test("an_assembly_whose_key_vanilla_refuses_once_the_rest_is_laid_takes_back_what_still_stands",
                20, AssemblyTests::keyRefusedTakesBackTheRest);

        var withChoices = tests.withPack(CHOICES_PACK);
        withChoices.test("next_assembly_steps_through_every_choice_and_the_key_alone_and_wraps", 20,
                AssemblyTests::nextStepsAndWraps);
        withChoices.test("reverse_next_assembly_steps_back", 20, AssemblyTests::reverseStepsBack);
        withChoices.test("a_stack_remembers_its_assembly_across_clicks", 20, AssemblyTests::remembersItsChoice);
        withChoices.test("any_one_declaration_starting_alone_starts_the_key_alone", 20,
                AssemblyTests::anyOneStartsAlone);
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

    /** A block laid before the Key is told it was placed by the player, with its own item, as vanilla tells one. */
    private static void tellsEachBlockPlaced(GameTestHelper helper) {
        ListeningPlayer player = holding(helper, Items.BARREL, 1);
        stock(player, MAIN_SLOT, GroundworksGameTests.TELLS_PLACED_ITEM.get(), 1);
        click(helper, player);
        expectStanding(helper, Map.of(SPOT, GroundworksGameTests.TELLS_PLACED.get(), KEY, Blocks.BARREL));
        Item told = TellsPlacedBlock.TOLD.remove(helper.absolutePos(SPOT));
        if (told != GroundworksGameTests.TELLS_PLACED_ITEM.get()) {
            helper.fail("the block laid was told it was placed with " + told, SPOT);
        }
        helper.succeed();
    }

    /**
     * The player stands on the spot, where the stone and the Key over it go, so their body is in
     * the Key's way too: they are lifted before the Key is placed, onto it, at the x and z they
     * stood at.
     */
    private static void liftsThePlayerOntoTheKey(GameTestHelper helper) {
        ListeningPlayer player = holding(helper, Items.LODESTONE, 1);
        stock(player, MAIN_SLOT, Items.STONE, 2);
        stock(player, OTHER_MAIN_SLOT, Items.COBBLESTONE, 1);
        player.standOn(helper, SPOT);
        player.enterTheLevel();
        try {
            PlacementPlan plan = plan(helper, player);
            if (plan == null || plan.isRefused()) {
                helper.fail("the plan was " + plan + ", not accepted round the player's own body");
            }
            click(helper, player);
            expectTower(helper);
            expectHeld(helper, player, Map.of(Items.LODESTONE, 0, Items.STONE, 0, Items.COBBLESTONE, 0));
            Vec3 onTheKey = Vec3.atBottomCenterOf(helper.absolutePos(KEY.above()));
            if (!player.position().equals(onTheKey)) {
                helper.fail("the player stands at " + player.position() + ", not on the Key at " + onTheKey);
            }
        } finally {
            player.discard();
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
        // In the order the shape counts them: the cobblestone comes before the stone.
        expectMissingNamed(helper, player, List.of("1 block.minecraft.cobblestone", "1 block.minecraft.stone"));
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

    private static void keyStatedInCode(GameTestHelper helper) {
        ListeningPlayer player = holding(helper, Items.CARTOGRAPHY_TABLE, 2);
        stock(player, MAIN_SLOT, Items.STONE, 1);
        click(helper, player);
        expectStanding(helper, Map.of(SPOT, Blocks.STONE, KEY, Blocks.CARTOGRAPHY_TABLE));
        expectHeld(helper, player, Map.of(Items.CARTOGRAPHY_TABLE, 1, Items.STONE, 0));
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

    private static void nextStepsAndWraps(GameTestHelper helper) {
        ListeningPlayer player = holding(helper, Items.LODESTONE, 1);
        player.setGameMode(GameType.CREATIVE);
        expectPlanned(helper, player, 2, "the short tower, the first Assembly");
        Assemblies.press(player, false);
        expectPlanned(helper, player, 3, "the tall tower, the second Assembly");
        Assemblies.press(player, false);
        expectKeyAlone(helper, player);
        Assemblies.press(player, false);
        expectPlanned(helper, player, 2, "the short tower again");
        if (player.getMainHandItem().has(Groundworks.ASSEMBLY_CHOICE.get())) {
            helper.fail("a stack stepped back to its start still stores a choice");
        }
        helper.succeed();
    }

    /** Three gold and two iron cover the plinth's four base blocks as three gold and one iron. */
    private static void uniformFill(GameTestHelper helper) {
        ListeningPlayer player = holding(helper, Items.TARGET, 1);
        stock(player, MAIN_SLOT, Items.IRON_BLOCK, 2);
        stock(player, OTHER_MAIN_SLOT, Items.GOLD_BLOCK, 3);
        PlacementPlan plan = plan(helper, player);
        if (plan == null || plan.isRefused() || plan.blocks().size() != 5) {
            helper.fail("the plan was " + plan + ", not the whole plinth accepted");
        }
        // The preview draws the block each position gets.
        long gold = plan.blocks().stream().filter(placed -> placed.state().is(Blocks.GOLD_BLOCK)).count();
        long iron = plan.blocks().stream().filter(placed -> placed.state().is(Blocks.IRON_BLOCK)).count();
        if (gold != 3 || iron != 1) {
            helper.fail("the plan drew " + gold + " gold and " + iron + " iron, not 3 and 1");
        }
        click(helper, player);
        expectBase(helper, Blocks.GOLD_BLOCK, 3, Blocks.IRON_BLOCK, 1);
        expectStanding(helper, Map.of(KEY, Blocks.TARGET));
        expectHeld(helper, player, Map.of(Items.TARGET, 0, Items.GOLD_BLOCK, 0, Items.IRON_BLOCK, 1));
        helper.succeed();
    }

    private static void tagPositionsShort(GameTestHelper helper) {
        ListeningPlayer player = holding(helper, Items.TARGET, 1);
        stock(player, MAIN_SLOT, Items.IRON_BLOCK, 2);
        stock(player, OTHER_MAIN_SLOT, Items.GOLD_BLOCK, 1);
        PlacementPlan plan = plan(helper, player);
        if (plan == null || plan.refusal() != Refusal.Assembly.NOT_ENOUGH_ITEMS) {
            helper.fail("the plan was " + plan + ", not refused for blocks missing");
        }
        click(helper, player);
        expectNothingPlaced(helper);
        if (!helper.getBlockState(FORWARD_RIGHT).isAir()) {
            helper.fail("a refused Assembly placed " + helper.getBlockState(FORWARD_RIGHT), FORWARD_RIGHT);
        }
        expectHeld(helper, player, Map.of(Items.TARGET, 1, Items.IRON_BLOCK, 2, Items.GOLD_BLOCK, 1));
        // Two iron and one gold cover three of the four: the fourth is missing as the tag's default.
        expectMissingNamed(helper, player, List.of("1 block.minecraft.iron_block"));
        helper.succeed();
    }

    private static void reverseStepsBack(GameTestHelper helper) {
        ListeningPlayer player = holding(helper, Items.LODESTONE, 1);
        player.setGameMode(GameType.CREATIVE);
        Assemblies.press(player, true);
        expectKeyAlone(helper, player);
        Assemblies.press(player, true);
        expectPlanned(helper, player, 3, "the tall tower");
        Assemblies.press(player, true);
        expectPlanned(helper, player, 2, "the short tower");
        helper.succeed();
    }

    private static void remembersItsChoice(GameTestHelper helper) {
        ListeningPlayer player = holding(helper, Items.LODESTONE, 2);
        stock(player, MAIN_SLOT, Items.STONE, 4);
        Assemblies.press(player, false);
        click(helper, player, SPOT);
        click(helper, player, OTHER_SPOT);
        for (BlockPos spot : List.of(SPOT, OTHER_SPOT)) {
            expectStanding(helper, Map.of(spot, Blocks.STONE, spot.above(), Blocks.STONE,
                    spot.above(2), Blocks.LODESTONE));
        }
        expectHeld(helper, player, Map.of(Items.LODESTONE, 0, Items.STONE, 0));
        helper.succeed();
    }

    /** No Assembly drawn: the lodestone alone has no plan of its own, as it is drawn by no Opt-in. */
    private static void expectKeyAlone(GameTestHelper helper, ListeningPlayer player) {
        PlacementPlan plan = plan(helper, player);
        if (plan != null && plan.blocks().size() != 1) {
            helper.fail("the plan was " + plan + ", not the Key alone");
        }
    }

    private static void expectPlanned(GameTestHelper helper, ListeningPlayer player, int blocks, String what) {
        expectPlanned(helper, player, blocks, what, SPOT);
    }

    private static void expectPlanned(GameTestHelper helper, ListeningPlayer player, int blocks, String what,
                                      BlockPos spot) {
        PlacementPlan plan = Placements.planFor(player.level(), player, InteractionHand.MAIN_HAND,
                player.getMainHandItem(), onTheFloor(helper, spot));
        if (plan == null || plan.isRefused() || plan.blocks().size() != blocks) {
            helper.fail("the plan was " + plan + ", not " + what);
        }
    }

    /** Fewer gold blocks than the base has positions, but more than iron: all four are gold, and nothing is charged. */
    private static void tagMostHeldInCreative(GameTestHelper helper) {
        ListeningPlayer player = holding(helper, Items.TARGET, 1);
        player.setGameMode(GameType.CREATIVE);
        stock(player, MAIN_SLOT, Items.GOLD_BLOCK, 2);
        stock(player, OTHER_MAIN_SLOT, Items.IRON_BLOCK, 1);
        click(helper, player);
        expectBase(helper, Blocks.GOLD_BLOCK, 4, Blocks.IRON_BLOCK, 0);
        expectStanding(helper, Map.of(KEY, Blocks.TARGET));
        expectHeld(helper, player, Map.of(Items.TARGET, 1, Items.GOLD_BLOCK, 2, Items.IRON_BLOCK, 1));
        helper.succeed();
    }

    private static void tagDefaultsInCreative(GameTestHelper helper) {
        ListeningPlayer player = holding(helper, Items.TARGET, 1);
        player.setGameMode(GameType.CREATIVE);
        stock(player, MAIN_SLOT, Items.STONE, 9);
        click(helper, player);
        expectBase(helper, Blocks.IRON_BLOCK, 4, Blocks.GOLD_BLOCK, 0);
        expectStanding(helper, Map.of(KEY, Blocks.TARGET));
        expectHeld(helper, player, Map.of(Items.TARGET, 1, Items.STONE, 9));
        helper.succeed();
    }

    private static void keepsATaggedBlock(GameTestHelper helper) {
        helper.setBlock(SPOT, Blocks.GOLD_BLOCK);
        ListeningPlayer player = holding(helper, Items.TARGET, 1);
        stock(player, MAIN_SLOT, Items.IRON_BLOCK, 3);
        PlacementPlan plan = plan(helper, player);
        if (plan == null || plan.isRefused() || plan.blocks().size() != 4) {
            helper.fail("the plan was " + plan + ", not the plinth less the gold standing");
        }
        click(helper, player);
        expectStanding(helper, Map.of(SPOT, Blocks.GOLD_BLOCK, RIGHT, Blocks.IRON_BLOCK, FORWARD, Blocks.IRON_BLOCK,
                FORWARD_RIGHT, Blocks.IRON_BLOCK, KEY, Blocks.TARGET));
        expectHeld(helper, player, Map.of(Items.IRON_BLOCK, 0, Items.TARGET, 0));
        helper.succeed();
    }

    /** The grass is cleared, as a golem's arms' space must be; the water stays, as a conduit's inside does. */
    private static void clearsEmptyPositions(GameTestHelper helper) {
        grassAt(helper, FORWARD);
        helper.setBlock(FORWARD_RIGHT, Blocks.WATER);
        expectStanding(helper, Map.of(FORWARD, Blocks.SHORT_GRASS));
        ListeningPlayer player = holding(helper, Items.BOOKSHELF, 1);
        stock(player, MAIN_SLOT, Items.STONE, 1);
        click(helper, player);
        expectStanding(helper, Map.of(SPOT, Blocks.BOOKSHELF, RIGHT, Blocks.STONE, FORWARD_RIGHT, Blocks.WATER));
        if (!helper.getBlockState(FORWARD).isAir()) {
            helper.fail("the empty position still holds " + helper.getBlockState(FORWARD), FORWARD);
        }
        expectHeld(helper, player, Map.of(Items.BOOKSHELF, 0, Items.STONE, 0));
        helper.succeed();
    }

    /** What the preview marks: the grass the click clears, not the air nor the water it leaves alone. */
    private static void planNamesWhatItClears(GameTestHelper helper) {
        ListeningPlayer player = holding(helper, Items.BOOKSHELF, 1);
        stock(player, MAIN_SLOT, Items.STONE, 1);
        expectClears(helper, player, List.of(), "nothing, over air");
        grassAt(helper, FORWARD);
        helper.setBlock(FORWARD_RIGHT, Blocks.WATER);
        expectClears(helper, player, List.of(FORWARD), "the grass alone, the water kept");
        helper.succeed();
    }

    /** Grass on dirt, which stays standing as the blocks round it change, as it wouldn't on the platform's stone. */
    private static void grassAt(GameTestHelper helper, BlockPos pos) {
        helper.setBlock(pos.below(), Blocks.DIRT);
        helper.setBlock(pos, Blocks.SHORT_GRASS);
    }

    private static void expectClears(GameTestHelper helper, ListeningPlayer player, List<BlockPos> cleared,
                                     String what) {
        PlacementPlan plan = plan(helper, player);
        List<BlockPos> expected = cleared.stream().map(helper::absolutePos).toList();
        if (plan == null || plan.isRefused() || !plan.clears().equals(expected)) {
            helper.fail("the plan was " + plan + ", clearing not " + what);
        }
    }

    private static void refusedWhenEmptyTaken(GameTestHelper helper) {
        helper.setBlock(FORWARD, Blocks.DIRT);
        ListeningPlayer player = holding(helper, Items.BOOKSHELF, 1);
        stock(player, MAIN_SLOT, Items.STONE, 1);
        PlacementPlan plan = plan(helper, player);
        Refusal taken = new Refusal.At(Refusal.Assembly.TAKEN, helper.absolutePos(FORWARD));
        if (plan == null || !taken.equals(plan.refusal())) {
            helper.fail("the plan was " + plan + ", not refused where the dirt stands");
        }
        click(helper, player);
        for (BlockPos pos : List.of(SPOT, RIGHT, FORWARD_RIGHT)) {
            if (!helper.getBlockState(pos).isAir()) {
                helper.fail("a refused Assembly placed " + helper.getBlockState(pos), pos);
            }
        }
        expectStanding(helper, Map.of(FORWARD, Blocks.DIRT));
        expectHeld(helper, player, Map.of(Items.BOOKSHELF, 1, Items.STONE, 1));
        if (!player.heard.contains("message.groundworks.assembly_taken")) {
            helper.fail("the player heard " + player.heard + ", not that a position is taken");
        }
        helper.succeed();
    }

    /** Seagrass holds water: cleared from an empty position, it leaves its water source. */
    private static void keepsTheWaterOfAPlant(GameTestHelper helper) {
        helper.setBlock(FORWARD, Blocks.SEAGRASS);
        ListeningPlayer player = holding(helper, Items.BOOKSHELF, 1);
        stock(player, MAIN_SLOT, Items.STONE, 1);
        click(helper, player);
        expectStanding(helper, Map.of(SPOT, Blocks.BOOKSHELF, RIGHT, Blocks.STONE));
        BlockState forward = helper.getBlockState(FORWARD);
        if (!forward.is(Blocks.WATER) || !forward.getFluidState().isSource()) {
            helper.fail("the empty position holds " + forward + ", not the plant's water", FORWARD);
        }
        helper.succeed();
    }

    /** Its lower half at an empty position: both halves go, and nothing drops. */
    private static void clearsTallGrassWithoutDrops(GameTestHelper helper) {
        // On dirt, or the platform's stone pops it before the click.
        helper.setBlock(FORWARD.below(), Blocks.DIRT);
        helper.setBlock(FORWARD, Blocks.TALL_GRASS.defaultBlockState().setValue(DoublePlantBlock.HALF, DoubleBlockHalf.LOWER));
        helper.setBlock(FORWARD.above(),
                Blocks.TALL_GRASS.defaultBlockState().setValue(DoublePlantBlock.HALF, DoubleBlockHalf.UPPER));
        ListeningPlayer player = holding(helper, Items.BOOKSHELF, 1);
        stock(player, MAIN_SLOT, Items.STONE, 1);
        expectClears(helper, player, List.of(FORWARD, FORWARD.above()), "both halves of the tall grass");
        click(helper, player);
        expectStanding(helper, Map.of(SPOT, Blocks.BOOKSHELF, RIGHT, Blocks.STONE));
        for (BlockPos pos : List.of(FORWARD, FORWARD.above())) {
            if (!helper.getBlockState(pos).isAir()) {
                helper.fail("the tall grass left " + helper.getBlockState(pos), pos);
            }
        }
        if (!helper.getEntities(EntityType.ITEM).isEmpty()) {
            helper.fail("clearing the tall grass dropped " + helper.getEntities(EntityType.ITEM));
        }
        helper.succeed();
    }

    /** The porch's stairs come before its empty position: both taken, the refusal points at the stairs'. */
    private static void takenPointsAtTheFirst(GameTestHelper helper) {
        helper.setBlock(FORWARD, Blocks.DIRT);
        helper.setBlock(FORWARD_RIGHT, Blocks.DIRT);
        ListeningPlayer player = holding(helper, Items.CRAFTING_TABLE, 1);
        player.setGameMode(GameType.CREATIVE);
        PlacementPlan plan = plan(helper, player);
        Refusal taken = new Refusal.At(Refusal.Assembly.TAKEN, helper.absolutePos(FORWARD));
        if (plan == null || !taken.equals(plan.refusal())) {
            helper.fail("the plan was " + plan + ", not refused where the stairs go");
        }
        helper.succeed();
    }

    /**
     * The porch's stairs face up the page as declared, away from the player: looking east, they
     * face east, drawn and placed.
     */
    private static void orientedBlocksTurn(GameTestHelper helper) {
        ListeningPlayer player = holding(helper, Items.CRAFTING_TABLE, 1);
        player.setGameMode(GameType.CREATIVE);
        PlacementPlan plan = plan(helper, player);
        if (plan == null || plan.blocks().stream().noneMatch(placed -> placed.pos().equals(helper.absolutePos(FORWARD))
                && placed.state().is(Blocks.OAK_STAIRS)
                && placed.state().getValue(StairBlock.FACING) == Direction.EAST)) {
            helper.fail("the plan was " + plan + ", not the stairs facing east");
        }
        click(helper, player);
        BlockState stairs = helper.getBlockState(FORWARD);
        if (!stairs.is(Blocks.OAK_STAIRS) || stairs.getValue(StairBlock.FACING) != Direction.EAST) {
            helper.fail("found " + stairs + ", not stairs facing east", FORWARD);
        }
        helper.succeed();
    }

    /** Fire has no item to pay for it with, so its declaration is skipped and the smithing table goes alone. */
    private static void noItemNotLoaded(GameTestHelper helper) {
        ListeningPlayer player = holding(helper, Items.SMITHING_TABLE, 1);
        player.setGameMode(GameType.CREATIVE);
        click(helper, player);
        expectAlone(helper, Blocks.SMITHING_TABLE);
        helper.succeed();
    }

    /**
     * The target's declaration naming a misspelt tag sorts before its plinth, so loaded it would be
     * the target's first Assembly: skipped, the target's only Assembly is the plinth.
     */
    private static void unknownTagNotLoaded(GameTestHelper helper) {
        ListeningPlayer player = holding(helper, Items.TARGET, 1);
        player.setGameMode(GameType.CREATIVE);
        PlacementPlan plan = plan(helper, player);
        // The plinth's four base blocks and its target.
        if (plan == null || plan.isRefused() || plan.blocks().size() != 5) {
            helper.fail("the plan was " + plan + ", not the plinth");
        }
        Assemblies.press(player, false);
        expectKeyAlone(helper, player);
        helper.succeed();
    }

    /** The loom's tag is declared with no blocks: its declaration loads, and creative lays the default. */
    private static void emptyTagLoads(GameTestHelper helper) {
        ListeningPlayer player = holding(helper, Items.LOOM, 1);
        player.setGameMode(GameType.CREATIVE);
        click(helper, player);
        expectStanding(helper, Map.of(SPOT, Blocks.STONE, KEY, Blocks.LOOM));
        helper.succeed();
    }

    /**
     * The cactus stands on its sand, already there, with cobblestone to its right: it would survive
     * where it goes now, but not once the cobblestone stands beside it, so the plan, the preview's
     * and the click's, is refused whole at the Key, and nothing is laid or taken.
     */
    private static void keyWouldNotStandRefused(GameTestHelper helper) {
        helper.setBlock(SPOT, Blocks.SAND);
        ListeningPlayer player = holding(helper, Items.CACTUS, 1);
        stock(player, MAIN_SLOT, Items.COBBLESTONE, 1);
        PlacementPlan plan = plan(helper, player);
        Refusal wouldNotStand = new Refusal.At(Refusal.Assembly.KEY_WOULD_NOT_STAND, helper.absolutePos(KEY));
        if (plan == null || !wouldNotStand.equals(plan.refusal())) {
            helper.fail("the plan was " + plan + ", not refused where the cactus would go");
        }
        click(helper, player);
        for (BlockPos pos : List.of(KEY, KEY.south())) {
            if (!helper.getBlockState(pos).isAir()) {
                helper.fail("a refused Assembly placed " + helper.getBlockState(pos), pos);
            }
        }
        expectStanding(helper, Map.of(SPOT, Blocks.SAND));
        expectHeld(helper, player, Map.of(Items.CACTUS, 1, Items.COBBLESTONE, 1));
        if (!player.heard.contains("message.groundworks.assembly_key_would_not_stand")) {
            helper.fail("the player heard " + player.heard + ", not that the Key would not stand");
        }
        helper.succeed();
    }

    /**
     * The door's Key stands on the floor, and its stone goes where the door's upper half would:
     * vanilla's placement, asked of the world as it will stand, finds no room above it, so the plan
     * is refused whole at the Key and nothing is laid or taken.
     */
    private static void keyWithNoRoomRefused(GameTestHelper helper) {
        ListeningPlayer player = holding(helper, Items.OAK_DOOR, 1);
        stock(player, MAIN_SLOT, Items.STONE, 1);
        PlacementPlan plan = plan(helper, player);
        Refusal taken = new Refusal.At(Refusal.Assembly.TAKEN, helper.absolutePos(SPOT));
        if (plan == null || !taken.equals(plan.refusal())) {
            helper.fail("the plan was " + plan + ", not refused where the door would go");
        }
        click(helper, player);
        for (BlockPos pos : List.of(SPOT, KEY)) {
            if (!helper.getBlockState(pos).isAir()) {
                helper.fail("a refused Assembly placed " + helper.getBlockState(pos), pos);
            }
        }
        expectHeld(helper, player, Map.of(Items.OAK_DOOR, 1, Items.STONE, 1));
        if (!player.heard.contains("message.groundworks.assembly_taken")) {
            helper.fail("the player heard " + player.heard + ", not that a position is taken");
        }
        helper.succeed();
    }

    /**
     * The sconce's torch goes over nothing, against the stone the shape lays forward of it: on the
     * world as it stands it has nothing to hang on, but on the world as it will stand it hangs on
     * the stone, facing back at the player, drawn and placed.
     */
    private static void keyHangsOnItsWall(GameTestHelper helper) {
        ListeningPlayer player = holding(helper, Items.TORCH, 1);
        stock(player, MAIN_SLOT, Items.STONE, 1);
        stock(player, OTHER_MAIN_SLOT, Items.COBBLESTONE, 1);
        PlacementPlan plan = plan(helper, player);
        BlockState hung = Blocks.WALL_TORCH.defaultBlockState().setValue(WallTorchBlock.FACING, Direction.WEST);
        if (plan == null || plan.isRefused() || !plan.blocks().getLast().equals(
                new PlacementPlan.Placed(helper.absolutePos(KEY), hung))) {
            helper.fail("the plan was " + plan + ", not the sconce with its torch on the stone");
        }
        click(helper, player);
        expectStanding(helper, Map.of(FORWARD, Blocks.COBBLESTONE, FORWARD.above(), Blocks.STONE));
        if (!helper.getBlockState(KEY).equals(hung)) {
            helper.fail("found " + helper.getBlockState(KEY) + ", not the torch on the stone", KEY);
        }
        expectHeld(helper, player, Map.of(Items.TORCH, 0, Items.STONE, 0, Items.COBBLESTONE, 0));
        helper.succeed();
    }

    /**
     * The golem trap lays an iron golem's body and pumpkin beside its note block, with cobblestone
     * on the note block's far side: the pumpkin, laid before the Key, spawns the golem, which
     * stands in the note block's way, as the plan, which plans no entity, couldn't know. The note
     * block isn't placed, so the cobblestone, still standing, is taken back and not charged, and
     * the grass the laying cleared is put back; the golem's blocks, gone into the golem, are charged.
     */
    private static void keyRefusedTakesBackTheRest(GameTestHelper helper) {
        grassAt(helper, SPOT.south(2));
        ListeningPlayer player = holding(helper, Items.NOTE_BLOCK, 1);
        stock(player, MAIN_SLOT, Items.IRON_BLOCK, 4);
        stock(player, OTHER_MAIN_SLOT, Items.CARVED_PUMPKIN, 1);
        stock(player, MAIN_SLOT + 1, Items.COBBLESTONE, 1);
        PlacementPlan plan = plan(helper, player);
        if (plan == null || plan.isRefused()) {
            helper.fail("the plan was " + plan + ", not the golem trap accepted");
        }
        click(helper, player);
        if (helper.getEntities(EntityType.IRON_GOLEM).isEmpty()) {
            helper.fail("no golem spawned from the trap");
        }
        for (BlockPos pos : List.of(SPOT, SPOT.north())) {
            if (!helper.getBlockState(pos).isAir()) {
                helper.fail("found " + helper.getBlockState(pos) + " where the trap was taken back", pos);
            }
        }
        expectStanding(helper, Map.of(SPOT.south(2), Blocks.SHORT_GRASS));
        expectHeld(helper, player, Map.of(Items.NOTE_BLOCK, 1, Items.IRON_BLOCK, 0, Items.CARVED_PUMPKIN, 0,
                Items.COBBLESTONE, 1));
        if (!player.heard.contains("message.groundworks.assembly_key_not_placed")) {
            helper.fail("the player heard " + player.heard + ", not that the Key couldn't be placed");
        }
        helper.succeed();
    }

    /**
     * The fletching table's first declaration by id starts on its Assembly, its second on the Key
     * alone: the second is enough, so a fresh stack places the Key alone, and Next Assembly wraps
     * to the first.
     */
    private static void anyOneStartsAlone(GameTestHelper helper) {
        ListeningPlayer player = holding(helper, Items.FLETCHING_TABLE, 2);
        player.setGameMode(GameType.CREATIVE);
        click(helper, player);
        expectAlone(helper, Blocks.FLETCHING_TABLE);
        Assemblies.press(player, false);
        expectPlanned(helper, player, 2, "the fletching table's first Assembly", OTHER_SPOT);
        helper.succeed();
    }

    /** The plinth's base holds so many of one block and so many of another, and nothing else. */
    private static void expectBase(GameTestHelper helper, Block one, int ones, Block other, int others) {
        long found = BASE.stream().filter(pos -> helper.getBlockState(pos).is(one)).count();
        long foundOther = BASE.stream().filter(pos -> helper.getBlockState(pos).is(other)).count();
        if (found != ones || foundOther != others || found + foundOther != BASE.size()) {
            helper.fail("the base holds " + BASE.stream().map(helper::getBlockState).toList() + ", not " + ones + " "
                    + one + " and " + others + " " + other);
        }
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

    /**
     * That the player was told the Assembly is short and of what, each as its count and its name's
     * translation key, in the order the message names them.
     */
    private static void expectMissingNamed(GameTestHelper helper, ListeningPlayer player, List<String> named) {
        Component told = player.messages.stream()
                .filter(message -> message.getContents() instanceof TranslatableContents translatable
                        && translatable.getKey().equals("message.groundworks.assembly_not_enough_items"))
                .findFirst()
                .orElse(null);
        if (told == null) {
            helper.fail("the player heard " + player.heard + ", not what is missing");
            return;
        }
        List<String> found = new ArrayList<>();
        Component list = (Component) ((TranslatableContents) told.getContents()).getArgs()[0];
        for (Component item : list.getSiblings()) {
            if (item.getContents() instanceof TranslatableContents translatable && translatable.getArgs().length == 2
                    && translatable.getArgs()[1] instanceof Component name
                    && name.getContents() instanceof TranslatableContents nameKey) {
                found.add(translatable.getArgs()[0] + " " + nameKey.getKey());
            }
        }
        if (!found.equals(named)) {
            helper.fail("the player was told " + found + " is missing, not " + named);
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
        click(helper, player, SPOT);
    }

    private static void click(GameTestHelper helper, ListeningPlayer player, BlockPos spot) {
        player.gameMode.useItemOn(player, helper.getLevel(), player.getMainHandItem(), InteractionHand.MAIN_HAND,
                onTheFloor(helper, spot));
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
        return onTheFloor(helper, SPOT);
    }

    /** The top of the floor block under this spot. */
    private static BlockHitResult onTheFloor(GameTestHelper helper, BlockPos spot) {
        BlockPos floor = helper.absolutePos(spot.below());
        return new BlockHitResult(Vec3.atCenterOf(floor).relative(Direction.UP, 0.5), Direction.UP, floor, false);
    }
}
