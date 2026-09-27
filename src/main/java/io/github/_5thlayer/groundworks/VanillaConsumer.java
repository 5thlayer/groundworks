// SPDX-FileCopyrightText: 2026 5thlayer
// SPDX-License-Identifier: MIT

package io.github._5thlayer.groundworks;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.block.Block;

/**
 * The Consumer Groundworks carries for vanilla (ADR 0005), wired as any Consumer is: it makes its
 * statements through the same hooks a Consumer mod does, and they add up with every other
 * Consumer's.
 *
 * <p>What it reaches is data, not code. Its {@linkplain Placements#optIn Opt-in} is the block tag
 * {@link #PLAN_OPT_IN}, shipped holding every vanilla block whose placed state depends on how it is
 * placed, and the namespaces {@link GroundworksConfig#PLAN_OPT_IN_NAMESPACES} lists. A pack
 * developer changes either with no code. What an opted-in block does is vanilla's own: its
 * {@linkplain Placements#vanillaPlan Vanilla Plan} asks the game.
 */
public final class VanillaConsumer {

    /** The blocks that get a Vanilla Plan, and so a preview. */
    public static final TagKey<Block> PLAN_OPT_IN =
            TagKey.create(Registries.BLOCK, Identifier.fromNamespaceAndPath(Groundworks.MOD_ID, "plan_opt_in"));

    private VanillaConsumer() {
    }

    /** Makes the statements, at mod construction as a Consumer mod does. */
    static void register() {
        Placements.optIn(VanillaConsumer::optsIn);
    }

    /**
     * Whether the tag holds the block or the config lists its namespace. The tag is read as it is
     * now, so a datapack reload reaches the next plan.
     */
    private static boolean optsIn(Block block) {
        return block.defaultBlockState().is(PLAN_OPT_IN)
                || GroundworksConfig.listsNamespace(BuiltInRegistries.BLOCK.getKey(block).getNamespace());
    }
}
