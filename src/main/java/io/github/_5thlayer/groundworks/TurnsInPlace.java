// SPDX-FileCopyrightText: 2026 5thlayer
// SPDX-License-Identifier: MIT

package io.github._5thlayer.groundworks;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

/**
 * A block that answers for itself what Rotate and Reverse Rotate do to it once placed (ADR 0003):
 * a machine that keeps its contents, a block that must not turn and says why. A block without it
 * takes vanilla's turn.
 *
 * <p>Asked only of a block some Consumer has {@linkplain Rotate#turnsInPlace stated} Rotate in
 * Place turns, and only on the server, once the player may turn it where it stands. The library
 * applies a {@linkplain #turned turned} state with a full block update, so a block entity is kept;
 * a turn to the same state is nothing.
 */
public interface TurnsInPlace {

    /**
     * What a quarter turn does to this block: clockwise seen from above, or the other way if
     * {@code reverse}. It changes nothing in the world itself.
     */
    Verdict<BlockState> turnInPlace(BlockState state, Level level, BlockPos pos, boolean reverse);

    /** Turned to a state, refused with a reason, or unturned. Generic over the state so the rule it feeds is Minecraft-free. */
    sealed interface Verdict<S> {
    }

    record Turned<S>(S state) implements Verdict<S> {
    }

    /** Refused, with the translation key of the reason the player is told on the action bar. Nothing changes. */
    record Refused<S>(String reason) implements Verdict<S> {
    }

    /** Not turned, and nothing said, as a Factorio entity that cannot rotate ignores the key. */
    record Unturned<S>() implements Verdict<S> {
    }

    static <S> Verdict<S> turned(S state) {
        return new Turned<>(state);
    }

    static <S> Verdict<S> refused(String reason) {
        return new Refused<>(reason);
    }

    static <S> Verdict<S> unturned() {
        return new Unturned<>();
    }
}
