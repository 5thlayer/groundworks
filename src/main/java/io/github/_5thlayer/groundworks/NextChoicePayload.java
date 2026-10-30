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

/** Next Choice, or its reverse, was pressed. The client only says which; the server steps the held item's choice. */
public record NextChoicePayload(boolean reverse) implements CustomPacketPayload {

    public static final Type<NextChoicePayload> TYPE =
            new Type<>(Identifier.fromNamespaceAndPath(Groundworks.MOD_ID, "next_choice"));

    static final StreamCodec<ByteBuf, NextChoicePayload> STREAM_CODEC =
            ByteBufCodecs.BOOL.map(NextChoicePayload::new, NextChoicePayload::reverse);

    /** Bumped when a payload's shape changes, so a client on the old shape is refused rather than misread. */
    private static final String VERSION = "1";

    @Override
    public Type<NextChoicePayload> type() {
        return TYPE;
    }

    static void register(RegisterPayloadHandlersEvent event) {
        event.registrar(VERSION).playToServer(TYPE, STREAM_CODEC, NextChoicePayload::handle);
    }

    private static void handle(NextChoicePayload payload, IPayloadContext context) {
        if (context.player() instanceof ServerPlayer player) {
            NextChoice.press(player, payload.reverse());
        }
    }
}
