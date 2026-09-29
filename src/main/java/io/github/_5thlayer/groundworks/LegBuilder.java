// SPDX-FileCopyrightText: 2026 5thlayer
// SPDX-License-Identifier: MIT

package io.github._5thlayer.groundworks;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;

/**
 * What builds a Stretch's legs for the items it {@linkplain #claims claims}, supplied by a Consumer
 * and {@linkplain Stretches#register registered} at mod construction. An item it claims is
 * stretch-able, and needs no code of its own: the Pack can make another mod's item stretch.
 *
 * <p>Groundworks runs the gesture, the anchors, the route seen from above, the legs' shape, the
 * charging and the preview for every item alike (ADR 0004). A builder answers only what a
 * {@link Leg} is built of.
 */
public interface LegBuilder {

    /** Whether this builder builds the legs of stretches of this item. */
    boolean claims(Item item);

    /**
     * The blocks that build {@code leg} of a stretch of {@code item}: each a position and a
     * blockstate, the positions among them that replace what stands there, or a refusal of the
     * builder's own, still carrying its blocks so it is drawn where they would have gone. One that
     * stands at a position, such as the block in the leg's way, is an {@link Refusal.At}.
     *
     * <p>Asked on both sides, by the preview and by the click on the server, which is the authority,
     * so it reads the world without changing it. A stretch refused anywhere is refused whole.
     */
    PlacementPlan build(Level level, Item item, Leg leg);

    /**
     * Whether the preview draws this builder's blocks reshaped against their neighbours, as the
     * game reshapes a block when the next one goes down beside it: each planned block against the
     * planned blocks beside it and the world elsewhere, once over the whole Stretch, its corners
     * included. Only the preview: the plan laid is {@link #build}'s. False unless the builder says so.
     */
    default boolean reshapesAgainstNeighbours() {
        return false;
    }

    /** What the player is told when a leg is refused with one of the builder's own refusals, never an {@link Refusal.At}. */
    Component message(Refusal refusal);
}
