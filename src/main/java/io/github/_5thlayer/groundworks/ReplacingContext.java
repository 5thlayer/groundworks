// SPDX-FileCopyrightText: 2026 5thlayer
// SPDX-License-Identifier: MIT

package io.github._5thlayer.groundworks;

import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import org.jspecify.annotations.Nullable;

/**
 * The place context a {@linkplain FastReplace Fast Replace} asks the held item's plan through: its
 * clicked block counts as replaceable, vanilla's own {@code replaceClicked}, so the plan lands on
 * the aimed block and vanilla's taken check lets it (ADR 0008). It has no {@linkplain
 * Raise#heightOf(BlockPlaceContext) height}, since a replace targets the block aimed at. An item's plan asks {@link Placements#isReplacing}
 * whether it is planning one.
 */
final class ReplacingContext extends BlockPlaceContext {

    ReplacingContext(Level level, @Nullable Player player, InteractionHand hand, ItemStack stack, BlockHitResult hit) {
        super(level, player, hand, stack, hit);
        replaceClicked = true;
    }
}
