// SPDX-FileCopyrightText: 2026 5thlayer
// SPDX-License-Identifier: MIT

package io.github._5thlayer.groundworks;

import java.util.List;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import org.jspecify.annotations.Nullable;

/**
 * What the held stack keeps about a Stretch, the stretch being drawn and its height, and what each
 * gesture makes of them (ADR 0004). The height is used up twice: storing the start makes it the
 * start's own height, and storing an anchor freezes it into the leg that ends there. Laying the
 * stretch or clearing it resets both.
 */
record StretchState(@Nullable StoredStretch stored, Height height) {

    static final StretchState NONE = new StretchState(null, Height.NONE);

    /** A start stored at {@code spot}, where the aim would place with no height, at the height held. */
    StretchState started(ResourceKey<Level> dimension, BlockPos spot, Direction look) {
        return new StretchState(new StoredStretch(dimension, spot.above(height.blocks()), look, List.of()),
                Height.NONE);
    }

    /**
     * An anchor aimed at {@code aimed}, which counts only seen from above, holding the height as
     * the leg's rise. One at the same spot as the last anchor, or as the start, is ignored.
     */
    StretchState anchored(BlockPos aimed) {
        BlockPos last = stored.last();
        if (last.getX() == aimed.getX() && last.getZ() == aimed.getZ()) {
            return this;
        }
        return new StretchState(stored.with(new StoredStretch.Anchor(aimed.getX(), aimed.getZ(), height.blocks())),
                Height.NONE);
    }

    /** The stretch laid, or cleared: nothing stored and no height. */
    StretchState reset() {
        return NONE;
    }
}
