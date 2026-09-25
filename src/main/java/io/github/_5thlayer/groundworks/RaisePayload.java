// SPDX-FileCopyrightText: 2026 5thlayer
// SPDX-License-Identifier: MIT

package io.github._5thlayer.groundworks;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/** Raise or Lower was pressed. The client only says which; the server decides what it moves. */
public record RaisePayload(boolean lower) implements CustomPacketPayload {

    public static final Type<RaisePayload> TYPE =
            new Type<>(Identifier.fromNamespaceAndPath(Groundworks.MOD_ID, "raise"));

    static final StreamCodec<ByteBuf, RaisePayload> STREAM_CODEC =
            ByteBufCodecs.BOOL.map(RaisePayload::new, RaisePayload::lower);

    /** Bumped when a payload's shape changes, so a client on the old shape is refused rather than misread. */
    private static final String VERSION = "1";

    @Override
    public Type<RaisePayload> type() {
        return TYPE;
    }

    static void register(RegisterPayloadHandlersEvent event) {
        event.registrar(VERSION).playToServer(TYPE, STREAM_CODEC, RaisePayload::handle);
    }

    private static void handle(RaisePayload payload, IPayloadContext context) {
        if (context.player() instanceof ServerPlayer player) {
            Raise.press(player, payload.lower());
        }
    }
}
