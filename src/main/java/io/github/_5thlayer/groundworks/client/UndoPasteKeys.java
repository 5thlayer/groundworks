// SPDX-FileCopyrightText: 2026 5thlayer
// SPDX-License-Identifier: MIT

package io.github._5thlayer.groundworks.client;

import com.mojang.blaze3d.platform.InputConstants;
import io.github._5thlayer.groundworks.UndoPastePayload;
import net.minecraft.client.KeyMapping;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;
import net.neoforged.neoforge.client.settings.KeyConflictContext;
import net.neoforged.neoforge.client.settings.KeyModifier;
import org.lwjgl.glfw.GLFW;

/**
 * The key for Undo Paste ({@code U}), in the game only. A press only tells the server, which takes
 * back the held sheet's latest paste and says what it did on the action bar.
 */
final class UndoPasteKeys {

    private static final KeyMapping UNDO_PASTE = new KeyMapping("key.groundworks.undo_paste",
            KeyConflictContext.IN_GAME, KeyModifier.NONE, InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_U, GroundworksClient.KEYS);

    private UndoPasteKeys() {
    }

    static void onRegisterKeys(RegisterKeyMappingsEvent event) {
        event.register(UNDO_PASTE);
    }

    static void onClientTick(ClientTickEvent.Post event) {
        while (UNDO_PASTE.consumeClick()) {
            ClientPacketDistributor.sendToServer(UndoPastePayload.INSTANCE);
        }
    }
}
