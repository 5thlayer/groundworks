// SPDX-FileCopyrightText: 2026 5thlayer
// SPDX-License-Identifier: MIT

package io.github._5thlayer.groundworks.gametest;

import io.github._5thlayer.groundworks.PlacementPlan;
import io.github._5thlayer.groundworks.Refusal;
import io.github._5thlayer.groundworks.ReplaceBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;

/**
 * The tests' {@linkplain ReplaceBuilder replace builder} whose replace is the aimed block alone, as
 * a belt tile's tier swap is, and which {@linkplain #spansBlocks says so}: it puts the held block's
 * default state there, and hands back a stick, so a test can tell its refund from the old block's
 * drops.
 */
final class ReplacesOneBlock implements ReplaceBuilder {

    @Override
    public @Nullable PlacementPlan plan(Level level, @Nullable Player player, ItemStack held, BlockPos aimed,
                                        BlockState old) {
        BlockState state = ((BlockItem) held.getItem()).getBlock().defaultBlockState();
        return old.is(state.getBlock()) ? null : PlacementPlan.replacing(new PlacementPlan.Placed(aimed, state), null);
    }

    @Override
    public ItemStack refund(Level level, BlockPos aimed, BlockState old) {
        return new ItemStack(Items.STICK);
    }

    @Override
    public boolean spansBlocks() {
        return false;
    }

    @Override
    public Component message(Refusal refusal) {
        throw new IllegalArgumentException("asked of " + refusal + ", though the builder refuses nothing");
    }
}
