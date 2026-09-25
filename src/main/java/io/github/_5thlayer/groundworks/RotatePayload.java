// SPDX-FileCopyrightText: 2026 5thlayer
// SPDX-License-Identifier: MIT

package io.github._5thlayer.groundworks;

import java.util.Optional;

import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * Rotate or Reverse Rotate was pressed, with the block under the crosshair if any. The client only
 * says what was pressed; the server decides what it turns.
 */
public record RotatePayload(boolean reverse, Optional<BlockPos> aimed) implements CustomPacketPayload {

    public static final Type<RotatePayload> TYPE =
            new Type<>(Identifier.fromNamespaceAndPath(Groundworks.MOD_ID, "rotate"));

    static final StreamCodec<ByteBuf, RotatePayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.BOOL, RotatePayload::reverse,
            BlockPos.STREAM_CODEC.apply(ByteBufCodecs::optional), RotatePayload::aimed,
            RotatePayload::new);

    /** Bumped when a payload's shape changes, so a client on the old shape is refused rather than misread. */
    private static final String VERSION = "1";

    @Override
    public Type<RotatePayload> type() {
        return TYPE;
    }

    static void register(RegisterPayloadHandlersEvent event) {
        event.registrar(VERSION).playToServer(TYPE, STREAM_CODEC, RotatePayload::handle);
    }

    private static void handle(RotatePayload payload, IPayloadContext context) {
        if (context.player() instanceof ServerPlayer player) {
            Rotate.press(player, payload.aimed().orElse(null), payload.reverse());
        }
    }
}
