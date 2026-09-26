// SPDX-FileCopyrightText: 2026 5thlayer
// SPDX-License-Identifier: MIT

package io.github._5thlayer.groundworks;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import net.minecraft.core.BlockPos;
import org.jspecify.annotations.Nullable;

/**
 * Every span a Dismantle confirms in one click, planned again: the queued spans in the order they
 * were queued, then the span the click ends. It goes through whole or not at all. A position two
 * spans take is taken once, and each family's positions are kept together, so its
 * {@linkplain DismantleFamily#beforeTaking hook} runs once over all of them.
 */
public record DismantlePass(List<BlockPos> starts, @Nullable Planned refused,
                            Map<DismantleFamily, List<BlockPos>> takes, List<BlockPos> draws) {

    /** One span of the pass: its family, its start, and what it would take up now. */
    public record Planned(DismantleFamily family, DismantleStart start, DismantleSpan span) {
    }

    public static DismantlePass of(List<Planned> spans) {
        List<BlockPos> starts = spans.stream().map(planned -> planned.start().pos()).toList();
        for (Planned planned : spans) {
            if (planned.span().isRefused()) {
                return new DismantlePass(starts, planned, Map.of(), List.of());
            }
        }
        Set<BlockPos> taken = new LinkedHashSet<>();
        Map<DismantleFamily, List<BlockPos>> takes = new LinkedHashMap<>();
        Set<BlockPos> draws = new LinkedHashSet<>();
        for (Planned planned : spans) {
            for (BlockPos pos : planned.span().takes()) {
                if (taken.add(pos)) {
                    takes.computeIfAbsent(planned.family(), family -> new ArrayList<>()).add(pos);
                }
            }
            draws.addAll(planned.span().draws());
        }
        return new DismantlePass(starts, null, takes, List.copyOf(draws));
    }

    public boolean isRefused() {
        return refused != null;
    }
}
