// SPDX-FileCopyrightText: 2026 5thlayer
// SPDX-License-Identifier: MIT

package io.github._5thlayer.groundworks;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.Property;
import org.jspecify.annotations.Nullable;

/**
 * Rotate and Reverse Rotate (ADR 0003). A press Rotates the Plan of a rotatable held stack: its
 * next placement turns a quarter from the way the player looks, and the turn stays on the stack,
 * as {@link Groundworks#QUARTER_TURN}, until its last item is placed.
 *
 * <p>Nothing implements a contract to be turned. The look a placement context reports is turned
 * by the stack's turn, so the plan and the click, which both read that context, turn together,
 * and every block whose placement reads the look turns with no code of its own.
 */
public final class Rotate {

    private Rotate() {
    }

    /** The held stack's turn, which a Consumer's own plan reads if it reads the look some other way. */
    public static QuarterTurn turnOf(ItemStack stack) {
        return stack.getOrDefault(Groundworks.QUARTER_TURN.get(), QuarterTurn.NONE);
    }

    /**
     * The turn a block placement's look is turned by: the held stack's, in the main hand only,
     * since that is the hand the Placement Preview draws, and a turn is never one the player
     * cannot see.
     */
    public static QuarterTurn turnOf(BlockPlaceContext context) {
        return context.getHand() == InteractionHand.MAIN_HAND ? turnOf(context.getItemInHand()) : QuarterTurn.NONE;
    }

    /**
     * Whether a press turns this stack: a Placement Preview is drawn for it, so the turn is never
     * one the player cannot see, and its block has a facing, an axis or a sixteen-way rotation.
     * Vanilla cannot say whether a block's placement reads the look, so a block with one of those
     * is taken to.
     */
    public static boolean isRotatable(ItemStack stack) {
        return stack.getItem() instanceof BlockItem item && Placements.isDrawn(item) && orients(item.getBlock());
    }

    private static boolean orients(Block block) {
        for (Property<?> property : block.getStateDefinition().getProperties()) {
            if (property.getValueClass() == Direction.class || property.getValueClass() == Direction.Axis.class
                    || property == BlockStateProperties.ROTATION_16) {
                return true;
            }
        }
        return false;
    }

    /**
     * A press of Rotate, or of Reverse Rotate, decided on the server. It turns the main hand's
     * stack if that is rotatable; otherwise nothing happens. The aimed block is the block under the
     * crosshair, if any, and is not yet turned.
     */
    public static void press(Player player, @Nullable BlockPos aimed, boolean reverse) {
        ItemStack held = player.getMainHandItem();
        if (isRotatable(held)) {
            rotatePlan(held, reverse);
        }
    }

    // No turn is no component, so a stack turned back stacks again with one never turned.
    private static void rotatePlan(ItemStack stack, boolean reverse) {
        QuarterTurn turned = reverse ? turnOf(stack).reverseRotate() : turnOf(stack).rotate();
        if (turned.equals(QuarterTurn.NONE)) {
            stack.remove(Groundworks.QUARTER_TURN.get());
        } else {
            stack.set(Groundworks.QUARTER_TURN.get(), turned);
        }
    }
}
