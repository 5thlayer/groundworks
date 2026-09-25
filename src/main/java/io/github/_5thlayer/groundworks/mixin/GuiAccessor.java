// SPDX-FileCopyrightText: 2026 5thlayer
// SPDX-License-Identifier: MIT

package io.github._5thlayer.groundworks.mixin;

import net.minecraft.client.gui.Gui;
import net.minecraft.network.chat.Component;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/**
 * The action bar's current message and how long it has left, which are private. The held stack's
 * height is kept on the action bar only while nothing else is showing there.
 */
@Mixin(Gui.class)
public interface GuiAccessor {

    @Accessor("overlayMessageString")
    @Nullable
    Component groundworks$overlayMessage();

    @Accessor("overlayMessageTime")
    int groundworks$overlayMessageTime();

    @Accessor("overlayMessageTime")
    void groundworks$setOverlayMessageTime(int ticks);
}
