// SPDX-FileCopyrightText: 2026 5thlayer
// SPDX-License-Identifier: MIT

package io.github._5thlayer.groundworks.gametest;

import java.util.List;

import io.github._5thlayer.groundworks.GroundworksConfig;
import io.github._5thlayer.groundworks.PlacementPlan;
import io.github._5thlayer.groundworks.Placements;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * The vanilla Consumer's Opt-in is data: a block gets a Vanilla Plan when {@code
 * groundworks:plan_opt_in} holds it or the server config lists its namespace. Stairs are the
 * fixture, since the shipped tag holds them through {@code #minecraft:stairs}; no test opts them in
 * in code.
 *
 * <p>Two of the tests run with {@link #REMOVES_STAIRS}, a datapack that takes {@code
 * #minecraft:stairs} out of the tag, as a pack developer would. It is switched on for their batch
 * alone, so the rest of the tests see the shipped tag. The Rotate tests that need an oriented block
 * that is not opted in share it.
 */
final class PlanOptInTests {

    /** The pack under {@code gametest_packs/} whose tag file removes {@code #minecraft:stairs}. */
    static final String REMOVES_STAIRS = "removes_stairs_from_plan_opt_in";

    private static final BlockPos FLOOR = new BlockPos(4, 0, 4);

    private PlanOptInTests() {
    }

    static void register(GroundworksGameTests.Registrar tests) {
        tests.test("stairs_get_a_vanilla_plan_from_the_shipped_tag", 20, helper -> {
            planned(helper, Blocks.OAK_STAIRS, "the shipped tag holds #minecraft:stairs");
            helper.succeed();
        });
        tests.test("a_stone_block_gets_no_vanilla_plan", 20, helper -> {
            notPlanned(helper, Blocks.STONE, "its placed state never depends on how it is placed");
            helper.succeed();
        });

        var removed = tests.withPack(REMOVES_STAIRS);
        removed.test("a_datapack_removing_stairs_from_the_tag_turns_their_plan_off", 20, helper -> {
            notPlanned(helper, Blocks.OAK_STAIRS, "#minecraft:stairs is removed from the tag");
            helper.succeed();
        });
        removed.test("a_namespace_in_the_config_opts_every_block_of_it_in", 20, helper -> {
            notPlanned(helper, Blocks.OAK_STAIRS, "the config names no namespace yet");
            var namespaces = GroundworksConfig.PLAN_OPT_IN_NAMESPACES;
            var before = namespaces.get();
            // Set and put back within the one tick, so no other test sees it.
            namespaces.set(List.of("minecraft"));
            try {
                // Stone too, which no tag holds: every block of the namespace, not only the tagged.
                planned(helper, Blocks.OAK_STAIRS, "the config names minecraft");
                planned(helper, Blocks.STONE, "the config names minecraft");
            } finally {
                namespaces.set(before);
            }
            helper.succeed();
        });
    }

    private static void planned(GameTestHelper helper, Block block, String because) {
        PlacementPlan plan = planFor(helper, block);
        if (plan == null || plan.isRefused()) {
            helper.fail(block + " is planned " + plan + ", though " + because, FLOOR);
        }
    }

    private static void notPlanned(GameTestHelper helper, Block block, String because) {
        PlacementPlan plan = planFor(helper, block);
        if (plan != null) {
            helper.fail(block + " is planned " + plan + ", though " + because, FLOOR);
        }
    }

    /** The plan the library draws for a player holding this block and aiming at the floor's top. */
    private static @Nullable PlacementPlan planFor(GameTestHelper helper, Block block) {
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(block, 16));
        var ground = helper.absolutePos(FLOOR);
        var hit = new BlockHitResult(Vec3.atCenterOf(ground).relative(Direction.UP, 0.5), Direction.UP, ground, false);
        return Placements.planFor(player.level(), player, InteractionHand.MAIN_HAND, player.getMainHandItem(), hit);
    }
}
