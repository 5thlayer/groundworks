// SPDX-FileCopyrightText: 2026 5thlayer
// SPDX-License-Identifier: MIT

package io.github._5thlayer.groundworks.client;

import com.mojang.blaze3d.platform.InputConstants;
import io.github._5thlayer.groundworks.NextAssemblyPayload;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;
import net.neoforged.neoforge.client.settings.KeyConflictContext;
import net.neoforged.neoforge.client.settings.KeyModifier;
import org.lwjgl.glfw.GLFW;

/**
 * The keys for Next Assembly ({@code V}) and its reverse ({@code Shift+V}), in the game only, two
 * actions as Rotate's are, so each rebinds alone. A press only tells the server; the stack's choice
 * comes back on the synced stack, and the preview redraws from it.
 */
final class NextAssemblyKeys {

    private static final KeyMapping NEXT = new KeyMapping("key.groundworks.next_assembly",
            KeyConflictContext.IN_GAME, KeyModifier.NONE, InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_V, GroundworksClient.KEYS);
    private static final KeyMapping PREVIOUS = new KeyMapping("key.groundworks.previous_assembly",
            KeyConflictContext.IN_GAME, KeyModifier.SHIFT, InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_V, GroundworksClient.KEYS);

    private NextAssemblyKeys() {
    }

    static void onRegisterKeys(RegisterKeyMappingsEvent event) {
        event.register(NEXT);
        event.register(PREVIOUS);
    }

    static void onClientTick(ClientTickEvent.Post event) {
        while (NEXT.consumeClick()) {
            press(false);
        }
        while (PREVIOUS.consumeClick()) {
            press(true);
        }
    }

    private static void press(boolean reverse) {
        if (Minecraft.getInstance().player != null) {
            ClientPacketDistributor.sendToServer(new NextAssemblyPayload(reverse));
        }
    }
}
