// SPDX-FileCopyrightText: 2026 5thlayer
// SPDX-License-Identifier: MIT

package io.github._5thlayer.groundworks;

import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;

/**
 * Raise and Lower (ADR 0004). A press moves the held stack's next placement one block up or down,
 * wherever a Placement Preview is drawn for it, and the height stays on the stack, as
 * {@link Groundworks#HEIGHT}, until its last item is placed. It is capped by the player's reach in
 * whole blocks.
 *
 * <p>Nothing implements a contract to be raised. The position a block placement's context reports
 * is moved by the stack's height, so the plan and the click, which both read that context, move
 * together, and every block that places by vanilla rules moves with no code of its own. A Consumer
 * whose plan reads the height some other way asks {@link #heightOf(BlockPlaceContext)}.
 */
public final class Raise {

    static final String AT_REACH = "message.groundworks.height_at_reach";

    private Raise() {
    }

    /** How far the player may raise or lower a placement: their block interaction range, in whole blocks. */
    public static int capOf(Player player) {
        return (int) Math.floor(player.blockInteractionRange());
    }

    /**
     * The held stack's height as this player may use it, clamped to their reach. A stack no
     * Placement Preview is drawn for has none, whatever it carries, so a height is never one the
     * player cannot see.
     */
    public static Height heightOf(Player player, ItemStack stack) {
        return Placements.isDrawn(stack.getItem())
                ? stack.getOrDefault(Groundworks.HEIGHT.get(), Height.NONE).clampedTo(capOf(player))
                : Height.NONE;
    }

    /**
     * The height a block placement is moved by: the held stack's, in the main hand only, since that
     * is the hand the Placement Preview draws, and only with a player, whose reach caps it. The
     * context's position is already moved by it, so a plan reads this only to know it was, and one
     * that works out a spot of its own from the moved position names it with
     * {@link BlockPlaceContext#at}, which is not moved again.
     */
    public static Height heightOf(BlockPlaceContext context) {
        Player player = context.getPlayer();
        return player != null && context.getHand() == InteractionHand.MAIN_HAND
                ? heightOf(player, context.getItemInHand())
                : Height.NONE;
    }

    /**
     * A press of Raise, or of Lower, decided on the server. It moves the main hand's stack if a
     * Placement Preview is drawn for it, so the height is never one the player cannot see, and
     * does nothing otherwise. A press past the player's reach changes nothing and is told on the
     * action bar.
     */
    public static void press(Player player, boolean lower) {
        ItemStack held = player.getMainHandItem();
        if (!Placements.isDrawn(held.getItem())) {
            return;
        }
        Height stored = held.getOrDefault(Groundworks.HEIGHT.get(), Height.NONE);
        Height stepped = stored.step(lower, capOf(player));
        if (stepped == null) {
            player.sendOverlayMessage(Component.translatable(AT_REACH));
        } else {
            setHeight(held, stepped);
        }
    }

    // No height is no component, so a stack lowered back stacks again with one never raised.
    private static void setHeight(ItemStack stack, Height height) {
        if (height.equals(Height.NONE)) {
            stack.remove(Groundworks.HEIGHT.get());
        } else {
            stack.set(Groundworks.HEIGHT.get(), height);
        }
    }
}
