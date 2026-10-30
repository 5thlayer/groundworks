// SPDX-FileCopyrightText: 2026 5thlayer
// SPDX-License-Identifier: MIT

package io.github._5thlayer.groundworks;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/**
 * Next Choice, or its reverse, decided on the server: the main hand's item steps to its next
 * choice, or its previous, and round. Each kind of item that has choices is asked in turn, the
 * first that takes the press stepping it; a held Key steps through its Assemblies and the Key
 * alone, and a sheet holding a Blueprint between skipping its air and pasting it. Any other item is left as it is.
 */
public final class NextChoice {

    private NextChoice() {
    }

    /** Steps the main hand's choice one on, or back when {@code reverse}. */
    public static void press(Player player, boolean reverse) {
        ItemStack held = player.getMainHandItem();
        if (!Assemblies.stepChoice(player.level(), held, reverse)) {
            Blueprints.stepChoice(player, held, reverse);
        }
    }
}
