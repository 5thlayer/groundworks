// SPDX-FileCopyrightText: 2026 5thlayer
// SPDX-License-Identifier: MIT

package io.github._5thlayer.groundworks;

import java.util.Map;

import com.mojang.serialization.Codec;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * The server's {@linkplain Assemblies Assembly} declarations, sent to a client as it joins and after
 * every reload, so its preview draws what the server's click places.
 */
public record AssembliesPayload(Map<Identifier, AssemblyShape> declarations) implements CustomPacketPayload {

    public static final Type<AssembliesPayload> TYPE =
            new Type<>(Identifier.fromNamespaceAndPath(Groundworks.MOD_ID, "assemblies"));

    static final StreamCodec<ByteBuf, AssembliesPayload> STREAM_CODEC =
            ByteBufCodecs.fromCodec(Codec.unboundedMap(Identifier.CODEC, AssemblyShape.CODEC))
                    .map(AssembliesPayload::new, AssembliesPayload::declarations);

    /** Bumped when a payload's shape changes, so a client on the old shape is refused rather than misread. */
    private static final String VERSION = "1";

    @Override
    public Type<AssembliesPayload> type() {
        return TYPE;
    }

    static void register(RegisterPayloadHandlersEvent event) {
        event.registrar(VERSION).playToClient(TYPE, STREAM_CODEC, AssembliesPayload::handle);
    }

    private static void handle(AssembliesPayload payload, IPayloadContext context) {
        Assemblies.received(payload.declarations());
    }
}
