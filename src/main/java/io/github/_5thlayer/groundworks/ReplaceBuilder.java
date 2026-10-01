// SPDX-FileCopyrightText: 2026 5thlayer
// SPDX-License-Identifier: MIT

package io.github._5thlayer.groundworks;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;

/**
 * What plans the Fast Replaces of a {@linkplain FastReplace#group(net.minecraft.resources.Identifier,
 * java.util.function.Predicate, ReplaceBuilder) Replace group} whose replace spans several blocks,
 * as a splitter's two halves or a pole's column do, supplied by the Consumer that states the group
 * (ADR 0008).
 *
 * <p>Without a builder, a group's replace is the held item's own plan for the aimed block, with
 * the old block's orientation copied onto it. With one, the plan is the builder's alone: the
 * library copies no orientation, and leaves to the builder which members replace which, the aimed
 * block's own kind included. The library still carries the plan out as it carries out its own:
 * the main hand's plain click, one held item charged and one {@linkplain #refund refund} handed
 * back however many blocks the plan spans, the room and may-build checks, the place event at each
 * position, and the swaps in the plan's order.
 */
public interface ReplaceBuilder {

    /**
     * The replace a plain click with {@code held} on {@code aimed}, a member of this builder's
     * group standing as {@code old}, carries out: each position it swaps and the state there, the
     * positions among them it {@linkplain PlacementPlan#replaces() replaces}, or a refusal of the
     * builder's own, still carrying its blocks so it is drawn where they would have gone. A plan
     * that replaces a position it puts no block at is refused, since a replace swaps and never
     * clears. {@code null} where this is no replace, and the click is the item's own.
     *
     * <p>Asked on both sides, every frame by the preview and once by the click on the server, which
     * is the authority, so it reads the world without changing it. {@code player} is {@code null}
     * only where no player aims.
     *
     * <p>The library checks only that the player may build at each position. Whether the new
     * blocks have room, survive where they go and are clear of entities is the builder's to ask,
     * as the item's own plan asks it of a replace without a builder.
     */
    @Nullable PlacementPlan plan(Level level, @Nullable Player player, ItemStack held, BlockPos aimed, BlockState old);

    /**
     * What a replace of {@code aimed}, standing as {@code old}, hands back, once for the whole plan,
     * or {@link ItemStack#EMPTY} for nothing. Asked of every plan the builder makes, refused or
     * not, wherever {@link #plan} is. By default the aimed block's item.
     */
    default ItemStack refund(Level level, BlockPos aimed, BlockState old) {
        return new ItemStack(old.getBlock().asItem());
    }

    /**
     * What the player is told when a replace is refused with one of the builder's own refusals,
     * asked of the reason itself when the refusal is an {@link Refusal.At}.
     */
    Component message(Refusal refusal);
}
