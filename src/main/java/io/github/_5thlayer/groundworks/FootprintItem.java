// SPDX-FileCopyrightText: 2026 5thlayer
// SPDX-License-Identifier: MIT

package io.github._5thlayer.groundworks;

import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.context.BlockPlaceContext;
import org.jspecify.annotations.Nullable;

/**
 * The item that places a whole {@linkplain Footprint Footprint} in one click, or nothing at
 * all (ADR 0009). An item that must extend another class plans through {@link
 * Footprint#plan} and lays through {@link Footprint#place}, as this one does.
 *
 * <p>Replaces {@code BlockItem.place} whole: the single-block flow has no hook for the other
 * blocks, and a partial footprint that ate the item is the failure every footprint refuses.
 */
public class FootprintItem extends BlockItem implements PlansPlacement {

    private final Footprint footprint;

    public FootprintItem(Footprint footprint, Properties properties) {
        super(footprint.origin().get(), properties);
        this.footprint = footprint;
    }

    @Override
    public @Nullable PlacementPlan plan(BlockPlaceContext context) {
        return footprint.plan(context);
    }

    @Override
    public InteractionResult place(BlockPlaceContext context) {
        return footprint.place(this, context) ? InteractionResult.SUCCESS : InteractionResult.FAIL;
    }
}
