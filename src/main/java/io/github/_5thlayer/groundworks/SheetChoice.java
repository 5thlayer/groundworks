// SPDX-FileCopyrightText: 2026 5thlayer
// SPDX-License-Identifier: MIT

package io.github._5thlayer.groundworks;

import java.util.List;
import java.util.Locale;

import com.mojang.serialization.Codec;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.network.codec.ByteBufCodecs;
import org.jspecify.annotations.Nullable;

/**
 * What a {@linkplain Blueprints sheet} is set to do, stepped by Next Choice among the choices its
 * state offers: a sheet holding a Blueprint skips its air, its start, or pastes it. A sheet with
 * none stored is on its start, and one stepped back to it stores none, so it stacks again with one
 * never touched.
 */
public enum SheetChoice {
    /** A paste leaves what stands where the Blueprint has air. */
    SKIP_AIR("message.groundworks.blueprint_skipping_air"),
    /** A paste clears what stands where the Blueprint has air, as the player would break it. */
    PASTE_AIR("message.groundworks.blueprint_pasting_air");

    /** The choices of a sheet holding a Blueprint, its start first. */
    static final List<SheetChoice> PASTE = List.of(SKIP_AIR, PASTE_AIR);

    private final String message;

    SheetChoice(String message) {
        this.message = message;
    }

    /** The translation key of what the player is told when the sheet steps to this choice. */
    String message() {
        return message;
    }

    /**
     * The choice of {@code choices} a sheet storing {@code stored} is on: its start where it stores
     * none, or one these choices don't offer.
     */
    static SheetChoice current(@Nullable SheetChoice stored, List<SheetChoice> choices) {
        return stored != null && choices.contains(stored) ? stored : choices.getFirst();
    }

    /** The choice after a press of Next Choice, or of its reverse, one step on among {@code choices} and round. */
    static SheetChoice next(@Nullable SheetChoice stored, List<SheetChoice> choices, boolean reverse) {
        int index = choices.indexOf(current(stored, choices));
        return choices.get(Math.floorMod(index + (reverse ? -1 : 1), choices.size()));
    }

    private static final Codec<SheetChoice> CODEC = Codec.STRING.xmap(
            name -> valueOf(name.toUpperCase(Locale.ROOT)), choice -> choice.name().toLowerCase(Locale.ROOT));

    // Built when the component registers, as QuarterTurn's is.
    static DataComponentType<SheetChoice> componentType() {
        return DataComponentType.<SheetChoice>builder()
                .persistent(CODEC)
                .networkSynchronized(ByteBufCodecs.idMapper(index -> values()[index], SheetChoice::ordinal))
                .build();
    }
}
