// SPDX-FileCopyrightText: 2026 5thlayer
// SPDX-License-Identifier: MIT

package io.github._5thlayer.groundworks.client;

import java.util.Optional;

import com.mojang.blaze3d.platform.InputConstants;
import io.github._5thlayer.groundworks.RotatePayload;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;
import net.neoforged.neoforge.client.settings.KeyConflictContext;
import net.neoforged.neoforge.client.settings.KeyModifier;
import org.lwjgl.glfw.GLFW;

/**
 * The keys for Rotate ({@code R}) and Reverse Rotate ({@code Shift+R}), in the game only, so they
 * never take a screen's {@code R}. Two actions rather than a modifier, so each rebinds alone. A
 * press only tells the server; the stack's turn comes back on the synced stack, and the preview
 * redraws from it.
 */
final class RotateKeys {

    private static final KeyMapping ROTATE = new KeyMapping("key.groundworks.rotate",
            KeyConflictContext.IN_GAME, KeyModifier.NONE, InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_R, GroundworksClient.KEYS);
    private static final KeyMapping REVERSE_ROTATE = new KeyMapping("key.groundworks.reverse_rotate",
            KeyConflictContext.IN_GAME, KeyModifier.SHIFT, InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_R, GroundworksClient.KEYS);

    private RotateKeys() {
    }

    static void onRegisterKeys(RegisterKeyMappingsEvent event) {
        event.register(ROTATE);
        event.register(REVERSE_ROTATE);
    }

    static void onClientTick(ClientTickEvent.Post event) {
        while (ROTATE.consumeClick()) {
            press(false);
        }
        while (REVERSE_ROTATE.consumeClick()) {
            press(true);
        }
    }

    private static void press(boolean reverse) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null) {
            return;
        }
        Optional<BlockPos> aimed = minecraft.hitResult instanceof BlockHitResult hit
                && hit.getType() == HitResult.Type.BLOCK ? Optional.of(hit.getBlockPos()) : Optional.empty();
        ClientPacketDistributor.sendToServer(new RotatePayload(reverse, aimed));
    }
}
