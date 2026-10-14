// SPDX-FileCopyrightText: 2026 5thlayer
// SPDX-License-Identifier: MIT

package io.github._5thlayer.groundworks;

import com.mojang.serialization.Codec;

import net.minecraft.core.component.DataComponentType;
import net.minecraft.network.codec.ByteBufCodecs;
import org.jspecify.annotations.Nullable;

/**
 * Which of its Key's choices the held stack places, stepped by Next Assembly: an index into the
 * Key's Assemblies by their ids, then the Key alone, last. A stack with none stored is on its
 * starting choice, and one stepped back to it stores none, so it stacks again with a fresh one. A
 * choice past the Key's choices, after a {@code /reload} took some away, wraps when it is read.
 */
public record AssemblyChoice(int index) {

    /** The choice a stack is on, of {@code choices}, with {@code stored} on it and {@code start} its starting choice. */
    public static int current(@Nullable AssemblyChoice stored, int start, int choices) {
        return stored == null ? start : Math.floorMod(stored.index, choices);
    }

    /**
     * The choice after a press of Next Assembly, or of its reverse, one step on and round, or
     * {@code null} where that is the start, which a stack stores as no choice at all.
     */
    public static @Nullable AssemblyChoice next(@Nullable AssemblyChoice stored, int start, int choices, boolean reverse) {
        int next = Math.floorMod(current(stored, start, choices) + (reverse ? -1 : 1), choices);
        return next == start ? null : new AssemblyChoice(next);
    }

    // Built when the component registers, as QuarterTurn's is.
    static DataComponentType<AssemblyChoice> componentType() {
        return DataComponentType.<AssemblyChoice>builder()
                .persistent(Codec.intRange(0, Integer.MAX_VALUE).xmap(AssemblyChoice::new, AssemblyChoice::index))
                .networkSynchronized(ByteBufCodecs.VAR_INT.map(AssemblyChoice::new, AssemblyChoice::index))
                .build();
    }
}
