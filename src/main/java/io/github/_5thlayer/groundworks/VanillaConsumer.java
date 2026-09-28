// SPDX-FileCopyrightText: 2026 5thlayer
// SPDX-License-Identifier: MIT

package io.github._5thlayer.groundworks;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;

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
 *
 * <p>Its {@linkplain Stretches Stretch} builder claims the block items in the item tag {@link
 * #STRETCHES}, shipped holding stairs, slabs, fences, walls, panes, logs and wood, and rails, and
 * {@linkplain VanillaLegs lays each through vanilla placement}.
 */
public final class VanillaConsumer {

    /** The blocks that get a Vanilla Plan, and so a preview. */
    public static final TagKey<Block> PLAN_OPT_IN =
            TagKey.create(Registries.BLOCK, Identifier.fromNamespaceAndPath(Groundworks.MOD_ID, "plan_opt_in"));

    /** The block items whose stretches the vanilla Consumer lays. */
    public static final TagKey<Item> STRETCHES =
            TagKey.create(Registries.ITEM, Identifier.fromNamespaceAndPath(Groundworks.MOD_ID, "stretches"));

    private VanillaConsumer() {
    }

    /**
     * Makes the statements, at mod construction as a Consumer mod does, but for its Stretch
     * builder. That one is registered once every mod is constructed, so it is the last asked, and
     * a Consumer mod's own builder for an item in the tag builds that item's legs.
     */
    static void register(IEventBus modBus) {
        Placements.optIn(VanillaConsumer::optsIn);
        modBus.addListener(FMLCommonSetupEvent.class, event -> Stretches.register(new VanillaLegs()));
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
