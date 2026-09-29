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
 * <p>Its {@linkplain Rotate#turnsInPlace statement for Rotate in Place} is the block tag {@link
 * #ROTATES_IN_PLACE}, shipped holding what {@link #PLAN_OPT_IN} holds and the wall forms of those
 * blocks, which place as blocks of their own. A tagged block takes vanilla's turn, {@code
 * BlockState.rotate}, unless it answers for itself.
 *
 * <p>Its {@linkplain Stretches Stretch} builder claims the block items in the item tag {@link
 * #STRETCHES}, shipped holding stairs, slabs, fences, walls, panes, logs and wood, and rails, and
 * {@linkplain VanillaLegs lays each through vanilla placement}.
 *
 * <p>Its {@linkplain VanillaFamilies Dismantle families} are the block tags under {@code
 * groundworks:dismantle_family/}, shipped as fences, walls, bars and rails, each taken up with the
 * tool vanilla breaks its blocks with.
 */
public final class VanillaConsumer {

    /** The blocks that get a Vanilla Plan, and so a preview. */
    public static final TagKey<Block> PLAN_OPT_IN =
            TagKey.create(Registries.BLOCK, Identifier.fromNamespaceAndPath(Groundworks.MOD_ID, "plan_opt_in"));

    /** The placed blocks Rotate in Place turns. */
    public static final TagKey<Block> ROTATES_IN_PLACE =
            TagKey.create(Registries.BLOCK, Identifier.fromNamespaceAndPath(Groundworks.MOD_ID, "rotates_in_place"));

    /** The block items whose stretches the vanilla Consumer lays. */
    public static final TagKey<Item> STRETCHES =
            TagKey.create(Registries.ITEM, Identifier.fromNamespaceAndPath(Groundworks.MOD_ID, "stretches"));

    private VanillaConsumer() {
    }

    /**
     * Makes the statements, at mod construction as a Consumer mod does, but for its Stretch
     * builder and Dismantle families. Those are registered once every mod is constructed, so they
     * are the last asked: a Consumer mod's own builder for an item in the tag builds that item's
     * legs, and its own family claims its blocks.
     */
    static void register(IEventBus modBus) {
        Placements.optIn(VanillaConsumer::optsIn);
        // Read as the tag is now, so a datapack reload reaches the next press.
        Rotate.turnsInPlace(block -> block.defaultBlockState().is(ROTATES_IN_PLACE));
        modBus.addListener(FMLCommonSetupEvent.class, event -> {
            Stretches.register(new VanillaLegs());
            Dismantles.register(new VanillaFamilies());
        });
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
