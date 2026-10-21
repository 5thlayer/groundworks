// SPDX-FileCopyrightText: 2026 5thlayer
// SPDX-License-Identifier: MIT

package io.github._5thlayer.groundworks;

import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import org.jspecify.annotations.Nullable;

/**
 * The features that plan a click on a block, in the order they take it: a Stretch being drawn or
 * started, then an Assembly, then a Fast Replace. The click and the Placement Preview walk this one
 * order, so the feature a click carries out is the one whose plan the preview drew. A click none of
 * them takes places as the item would, and the preview draws its Vanilla Plan, if any.
 *
 * <p>A Dismantle answers the click before any of them, as it claims a click only on a member of a
 * family (ADR 0010), and it plans spans rather than placements, so it is no feature of this order.
 */
enum Feature {

    STRETCH {
        @Override
        @Nullable Claim claim(Level level, @Nullable Player player, InteractionHand hand, ItemStack stack,
                BlockHitResult hit) {
            return Stretches.takesClick(level, player, hand, stack)
                    ? new Claim(this, Stretches.planFor(level, player, stack, hit))
                    : null;
        }

        @Override
        InteractionResult useOn(Player player, InteractionHand hand, BlockHitResult hit) {
            return Stretches.useOn(player, hand, hit);
        }
    },

    ASSEMBLY {
        @Override
        @Nullable Claim claim(Level level, @Nullable Player player, InteractionHand hand, ItemStack stack,
                BlockHitResult hit) {
            if (hand != InteractionHand.MAIN_HAND) {
                return null;
            }
            PlacementPlan plan = Assemblies.planFor(level, player, stack, hit);
            return plan == null ? null : new Claim(this, plan);
        }

        @Override
        InteractionResult useOn(Player player, InteractionHand hand, BlockHitResult hit) {
            return Assemblies.useOn(player, hand, hit);
        }
    },

    FAST_REPLACE {
        @Override
        @Nullable Claim claim(Level level, @Nullable Player player, InteractionHand hand, ItemStack stack,
                BlockHitResult hit) {
            PlacementPlan plan = FastReplace.planFor(level, player, hand, stack, hit);
            return plan == null ? null : new Claim(this, plan);
        }

        @Override
        InteractionResult useOn(Player player, InteractionHand hand, BlockHitResult hit) {
            return FastReplace.useOn(player, hand, hit);
        }
    };

    /**
     * A feature's take on a click, and what it would lay: {@code null} where it lays nothing, as a
     * sneak-click storing a Stretch's start lays nothing, and the preview draws the start instead.
     */
    record Claim(Feature feature, @Nullable PlacementPlan plan) {
    }

    /**
     * Whether a click with this stack at this hit is this feature's, and what it would lay, or
     * {@code null} where the click goes on to the next. Safe on either side, reading the world
     * without touching it.
     */
    abstract @Nullable Claim claim(Level level, @Nullable Player player, InteractionHand hand, ItemStack stack,
            BlockHitResult hit);

    /** The click carried out, by the feature that {@linkplain #claim claims} it. */
    abstract InteractionResult useOn(Player player, InteractionHand hand, BlockHitResult hit);

    /** The first feature, in the order, that takes this click, or {@code null} where none does. */
    static @Nullable Claim claimOf(Level level, @Nullable Player player, InteractionHand hand, ItemStack stack,
            BlockHitResult hit) {
        for (Feature feature : values()) {
            Claim claim = feature.claim(level, player, hand, stack, hit);
            if (claim != null) {
                return claim;
            }
        }
        return null;
    }
}
