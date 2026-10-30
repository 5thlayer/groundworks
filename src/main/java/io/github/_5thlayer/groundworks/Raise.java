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
 * whole blocks, but only by the world's build height once a stretch's start is stored (ADR 0013).
 *
 * <p>Nothing implements a contract to be raised. The position a block placement's context reports
 * is moved by the stack's height, so the plan and the click, which both read that context, move
 * together, and every block that places by vanilla rules moves with no code of its own. A Consumer
 * whose plan reads the height some other way asks {@link #heightOf(BlockPlaceContext)}.
 */
public final class Raise {

    static final String AT_REACH = "message.groundworks.height_at_reach";
    static final String AT_BUILD_LIMIT = "message.groundworks.height_at_build_limit";

    /** The held stack's height, as stored and before any cap. */
    static final HeldValue<Height> HEIGHT = new HeldValue<>(() -> Groundworks.HEIGHT.get(), Height.NONE);

    private Raise() {
    }

    /** How far the player may raise or lower a placement: their block interaction range, in whole blocks. */
    public static int capOf(Player player) {
        return (int) Math.floor(player.blockInteractionRange());
    }

    /**
     * How far this player may raise or lower the held stack: their reach, but while a stretch's
     * start is stored, only the world's build height, so a Leg's rise and a {@link Column}'s length
     * reach past the player's arm (ADR 0013). The start's own height is set before it is stored, so
     * reach caps it as it caps a single placement's.
     */
    static int capOf(Player player, ItemStack stack) {
        return drawsStretch(player, stack) ? player.level().getHeight() : capOf(player);
    }

    /**
     * Whether a press moves this stack: a Placement Preview is drawn for its item, or it is a
     * sheet holding a {@linkplain Blueprints Blueprint}, whose paste is drawn.
     */
    private static boolean isRaised(ItemStack stack) {
        return Placements.isDrawn(stack.getItem()) || Blueprints.pastes(stack);
    }

    /** Whether a stretch is being drawn with the held stack: it stretches, and its start is stored. */
    private static boolean drawsStretch(Player player, ItemStack stack) {
        return Stretches.builderOf(stack.getItem()) != null && Stretches.storedOn(player.level(), stack) != null;
    }

    /**
     * The held stack's height as this player may use it, clamped to their reach, or to the build
     * height while a stretch is being drawn. A stack no Placement Preview is drawn for has none,
     * whatever it carries, so a height is never one the player cannot see.
     */
    public static Height heightOf(Player player, ItemStack stack) {
        return isRaised(stack)
                ? HEIGHT.get(stack).clampedTo(capOf(player, stack))
                : Height.NONE;
    }

    /**
     * The height a block placement is moved by: the held stack's, in the main hand only, since that
     * is the hand the Placement Preview draws, and only with a player, whose reach caps it. The
     * context's position is already moved by it, so a plan reads this only to know it was, and one
     * that works out a spot of its own from the moved position names it with
     * {@link BlockPlaceContext#at}, which is not moved again. A {@linkplain FastReplace Fast
     * Replace}'s context names the aimed block, so it has no height (ADR 0008).
     */
    public static Height heightOf(BlockPlaceContext context) {
        Player player = context.getPlayer();
        return player != null && context.getHand() == InteractionHand.MAIN_HAND && !Placements.isReplacing(context)
                ? heightOf(player, context.getItemInHand())
                : Height.NONE;
    }

    /**
     * A press of Raise, or of Lower, decided on the server. It moves the main hand's stack if a
     * Placement Preview is drawn for it, so the height is never one the player cannot see, and
     * does nothing otherwise. A press past the cap changes nothing and is told on the action bar:
     * the player's reach, or the world's build height while a stretch is being drawn.
     */
    public static void press(Player player, boolean lower) {
        ItemStack held = player.getMainHandItem();
        if (!isRaised(held)) {
            return;
        }
        Height stored = HEIGHT.get(held);
        Height stepped = stored.step(lower, capOf(player, held));
        if (stepped == null) {
            player.sendOverlayMessage(Component.translatable(drawsStretch(player, held) ? AT_BUILD_LIMIT : AT_REACH));
        } else {
            HEIGHT.set(held, stepped);
        }
    }
}
