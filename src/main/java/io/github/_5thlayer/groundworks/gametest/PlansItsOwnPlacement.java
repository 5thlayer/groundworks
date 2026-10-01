// SPDX-FileCopyrightText: 2026 5thlayer
// SPDX-License-Identifier: MIT

package io.github._5thlayer.groundworks.gametest;

import java.util.Map;

import io.github._5thlayer.groundworks.PlacementPlan;
import io.github._5thlayer.groundworks.Placements;
import io.github._5thlayer.groundworks.PlansPlacement;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.Block;
import org.jspecify.annotations.Nullable;

/**
 * The tests' item that {@linkplain PlansPlacement plans its own placement}, as a Consumer's item
 * does, by asking for the vanilla plan of its block. It leaves the block's own item alone, so the
 * block still names vanilla's as its item.
 */
final class PlansItsOwnPlacement extends BlockItem implements PlansPlacement {

    PlansItsOwnPlacement(Block block, Item.Properties properties) {
        super(block, properties);
    }

    @Override
    public void registerBlocks(Map<Block, Item> map, Item item) {
    }

    @Override
    public @Nullable PlacementPlan plan(BlockPlaceContext context) {
        return Placements.vanillaPlan(this, context);
    }
}
