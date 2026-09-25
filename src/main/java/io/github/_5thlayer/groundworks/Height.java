// SPDX-FileCopyrightText: 2026 5thlayer
// SPDX-License-Identifier: MIT

package io.github._5thlayer.groundworks;

import com.mojang.serialization.Codec;

import net.minecraft.core.component.DataComponentType;
import net.minecraft.network.codec.ByteBufCodecs;
import org.jspecify.annotations.Nullable;

/**
 * How far the held stack's next placement is moved straight up, in blocks, by Raise and Lower
 * (ADR 0004); negative is down. It is capped by the player's reach in whole blocks, and a height
 * stored beyond the cap -- the reach shrank since -- is clamped when it is read, never rewritten
 * in advance. {@link #NONE} is also what a stack with no height reads as.
 */
public record Height(int blocks) {

    public static final Height NONE = new Height(0);

    /** This height as the player may use it, within {@code cap} blocks either way. */
    public Height clampedTo(int cap) {
        return new Height(Math.clamp(blocks, -cap, cap));
    }

    /**
     * A press of Raise, or of Lower, from this height as clamped to {@code cap}: one block up or
     * down, or {@code null} if that would pass the cap, which refuses the press.
     */
    public @Nullable Height step(boolean lower, int cap) {
        int stepped = clampedTo(cap).blocks + (lower ? -1 : 1);
        return Math.abs(stepped) > cap ? null : new Height(stepped);
    }

    // Built when the component registers, as QuarterTurn's is.
    static DataComponentType<Height> componentType() {
        return DataComponentType.<Height>builder()
                .persistent(Codec.INT.xmap(Height::new, Height::blocks))
                .networkSynchronized(ByteBufCodecs.VAR_INT.map(Height::new, Height::blocks))
                .build();
    }
}
