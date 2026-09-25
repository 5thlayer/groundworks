// SPDX-FileCopyrightText: 2026 5thlayer
// SPDX-License-Identifier: MIT

package io.github._5thlayer.groundworks.client;

import com.mojang.blaze3d.platform.InputConstants;
import io.github._5thlayer.groundworks.Height;
import io.github._5thlayer.groundworks.Raise;
import io.github._5thlayer.groundworks.RaisePayload;
import io.github._5thlayer.groundworks.mixin.GuiAccessor;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;
import net.neoforged.neoforge.client.settings.KeyConflictContext;
import net.neoforged.neoforge.client.settings.KeyModifier;
import org.lwjgl.glfw.GLFW;

/**
 * The keys for Raise ({@code G}) and Lower ({@code B}), in the game only, each rebindable alone,
 * and the held stack's height on the action bar. A press only tells the server; the stack's
 * height comes back on the synced stack, and the preview and the action bar redraw from it.
 */
final class RaiseKeys {

    private static final String HEIGHT = "message.groundworks.height";

    private static final KeyMapping RAISE = new KeyMapping("key.groundworks.raise",
            KeyConflictContext.IN_GAME, KeyModifier.NONE, InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_G, GroundworksClient.KEYS);
    private static final KeyMapping LOWER = new KeyMapping("key.groundworks.lower",
            KeyConflictContext.IN_GAME, KeyModifier.NONE, InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_B, GroundworksClient.KEYS);

    private RaiseKeys() {
    }

    static void onRegisterKeys(RegisterKeyMappingsEvent event) {
        event.register(RAISE);
        event.register(LOWER);
    }

    static void onClientTick(ClientTickEvent.Post event) {
        while (RAISE.consumeClick()) {
            ClientPacketDistributor.sendToServer(new RaisePayload(false));
        }
        while (LOWER.consumeClick()) {
            ClientPacketDistributor.sendToServer(new RaisePayload(true));
        }
        showHeight(Minecraft.getInstance());
    }

    /**
     * The net height on the action bar whenever it isn't 0, kept there tick by tick. Any other
     * message, such as a press refused at the reach, is left to show its full time first; and the
     * height's own message is taken down as soon as the height returns to 0.
     */
    private static void showHeight(Minecraft minecraft) {
        LocalPlayer player = minecraft.player;
        if (player == null) {
            return;
        }
        Gui gui = minecraft.gui;
        GuiAccessor overlay = (GuiAccessor) gui;
        Component showing = overlay.groundworks$overlayMessage();
        boolean ours = showing != null && showing.getContents() instanceof TranslatableContents message
                && message.getKey().equals(HEIGHT);
        Height height = Raise.heightOf(player, player.getMainHandItem());
        if (!height.equals(Height.NONE)) {
            if (ours || overlay.groundworks$overlayMessageTime() <= 0) {
                gui.setOverlayMessage(Component.translatable(HEIGHT, "%+d".formatted(height.blocks())), false);
            }
        } else if (ours) {
            overlay.groundworks$setOverlayMessageTime(0);
        }
    }
}
