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

/** Next Assembly, or its reverse, was pressed. The client only says which; the server steps the held Key. */
public record NextAssemblyPayload(boolean reverse) implements CustomPacketPayload {

    public static final Type<NextAssemblyPayload> TYPE =
            new Type<>(Identifier.fromNamespaceAndPath(Groundworks.MOD_ID, "next_assembly"));

    static final StreamCodec<ByteBuf, NextAssemblyPayload> STREAM_CODEC =
            ByteBufCodecs.BOOL.map(NextAssemblyPayload::new, NextAssemblyPayload::reverse);

    /** Bumped when a payload's shape changes, so a client on the old shape is refused rather than misread. */
    private static final String VERSION = "1";

    @Override
    public Type<NextAssemblyPayload> type() {
        return TYPE;
    }

    static void register(RegisterPayloadHandlersEvent event) {
        event.registrar(VERSION).playToServer(TYPE, STREAM_CODEC, NextAssemblyPayload::handle);
    }

    private static void handle(NextAssemblyPayload payload, IPayloadContext context) {
        if (context.player() instanceof ServerPlayer player) {
            Assemblies.press(player, payload.reverse());
        }
    }
}
