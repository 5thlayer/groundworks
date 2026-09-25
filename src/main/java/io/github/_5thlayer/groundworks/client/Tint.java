// SPDX-FileCopyrightText: 2026 5thlayer
// SPDX-License-Identifier: MIT

package io.github._5thlayer.groundworks.client;

import io.github._5thlayer.groundworks.PlacementPlan;

import net.minecraft.core.BlockPos;

/** The colour a {@link PlacementPlan} is drawn in, as ARGB. */
final class Tint {

    /** How solid the preview is. Enough to read the block's own texture, thin enough to see through. */
    private static final int ALPHA = 0x80;

    /** White: the block's own colours, just faint. */
    static final int ACCEPTED = (ALPHA << 24) | 0xFFFFFF;

    /** Red: any reason placing would be refused, a Consumer's and vanilla's alike. */
    static final int REFUSED = (ALPHA << 24) | 0xFF4040;

    /** Blue: a replace, apart from both white and red. A refused replace draws red. */
    static final int REPLACE = (ALPHA << 24) | 0x4080FF;

    private Tint() {
    }

    /** The plan's tint as a whole, which is what its Overlays are given. */
    static int of(PlacementPlan plan) {
        if (plan.isRefused()) {
            return REFUSED;
        }
        return plan.isReplace() ? REPLACE : ACCEPTED;
    }

    /** One block's tint, since a plan may replace some of its blocks and place the rest. */
    static int of(PlacementPlan plan, BlockPos pos) {
        if (plan.isRefused()) {
            return REFUSED;
        }
        return plan.replaces().contains(pos) ? REPLACE : ACCEPTED;
    }
}
