// SPDX-FileCopyrightText: 2026 5thlayer
// SPDX-License-Identifier: MIT

package io.github._5thlayer.groundworks;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.core.BlockPos;
import org.jspecify.annotations.Nullable;

/**
 * What a Dismantle would take up from a start to an end, as the start's {@link DismantleFamily}
 * answers: the positions it takes, the positions it draws, or a {@link Refusal}.
 *
 * <p>It draws everything it takes and may draw more: a belt's tiles take their wedges through their
 * own removal, so the wedges are drawn but never taken. A refused span takes and draws nothing, and
 * the preview draws only the start.
 */
public record DismantleSpan(List<BlockPos> takes, List<BlockPos> draws, @Nullable Refusal refusal) {

    public DismantleSpan {
        takes = List.copyOf(takes);
        draws = List.copyOf(draws);
        if (!draws.containsAll(takes)) {
            throw new IllegalArgumentException("a span draws " + draws + ", not everything it takes: " + takes);
        }
    }

    /** A span that takes up these positions and draws only them. */
    public static DismantleSpan taking(List<BlockPos> takes) {
        return taking(takes, List.of());
    }

    /** A span that takes up these positions and also draws {@code alsoDrawn}. */
    public static DismantleSpan taking(List<BlockPos> takes, List<BlockPos> alsoDrawn) {
        List<BlockPos> draws = new ArrayList<>(takes);
        draws.addAll(alsoDrawn);
        return new DismantleSpan(takes, draws, null);
    }

    /** A span that would not go through. */
    public static DismantleSpan refused(Refusal refusal) {
        return new DismantleSpan(List.of(), List.of(), refusal);
    }

    public boolean isRefused() {
        return refusal != null;
    }
}
