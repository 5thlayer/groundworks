// SPDX-FileCopyrightText: 2026 5thlayer
// SPDX-License-Identifier: MIT

package io.github._5thlayer.groundworks;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/** Undo Paste was pressed. The client only says so; the server decides what it takes back. */
public record UndoPastePayload() implements CustomPacketPayload {

    public static final UndoPastePayload INSTANCE = new UndoPastePayload();

    public static final Type<UndoPastePayload> TYPE =
            new Type<>(Identifier.fromNamespaceAndPath(Groundworks.MOD_ID, "undo_paste"));

    static final StreamCodec<ByteBuf, UndoPastePayload> STREAM_CODEC = StreamCodec.unit(INSTANCE);

    /** Bumped when a payload's shape changes, so a client on the old shape is refused rather than misread. */
    private static final String VERSION = "1";

    @Override
    public Type<UndoPastePayload> type() {
        return TYPE;
    }

    static void register(RegisterPayloadHandlersEvent event) {
        event.registrar(VERSION).playToServer(TYPE, STREAM_CODEC, UndoPastePayload::handle);
    }

    private static void handle(UndoPastePayload payload, IPayloadContext context) {
        if (context.player() instanceof ServerPlayer player) {
            UndoPaste.press(player);
        }
    }
}
