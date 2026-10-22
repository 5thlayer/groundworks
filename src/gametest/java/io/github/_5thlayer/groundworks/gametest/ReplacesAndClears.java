// SPDX-FileCopyrightText: 2026 5thlayer
// SPDX-License-Identifier: MIT

package io.github._5thlayer.groundworks.gametest;

import java.util.List;

import io.github._5thlayer.groundworks.PlacementPlan;
import io.github._5thlayer.groundworks.Refusal;
import io.github._5thlayer.groundworks.ReplaceBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;

/**
 * The tests' {@linkplain ReplaceBuilder replace builder} whose plan replaces the aimed block with
 * the held block's default state and {@linkplain PlacementPlan#clears() clears} the block above it.
 */
final class ReplacesAndClears implements ReplaceBuilder {

    @Override
    public @Nullable PlacementPlan plan(Level level, @Nullable Player player, ItemStack held, BlockPos aimed,
                                        BlockState old) {
        BlockState state = ((BlockItem) held.getItem()).getBlock().defaultBlockState();
        if (old.is(state.getBlock())) {
            return null;
        }
        return new PlacementPlan(List.of(new PlacementPlan.Placed(aimed, state)), List.of(aimed),
                List.of(aimed.above()), null);
    }

    @Override
    public Component message(Refusal refusal) {
        throw new IllegalArgumentException("asked of " + refusal + ", though the builder refuses nothing");
    }
}
