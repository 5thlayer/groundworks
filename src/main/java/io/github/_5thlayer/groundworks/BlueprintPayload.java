// SPDX-FileCopyrightText: 2026 5thlayer
// SPDX-License-Identifier: MIT

package io.github._5thlayer.groundworks;

import java.util.UUID;

import io.netty.buffer.ByteBuf;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * A {@linkplain Blueprints Blueprint}'s blocks, and whether it is a Cut, which pastes uncharged,
 * sent to a client while it holds a sheet naming it, so its Placement Preview draws the paste (ADR
 * 0014). What a Cut's blocks hold stays on the server.
 */
public record BlueprintPayload(UUID id, BlueprintBlocks blocks, boolean cut) implements CustomPacketPayload {

    public static final Type<BlueprintPayload> TYPE =
            new Type<>(Identifier.fromNamespaceAndPath(Groundworks.MOD_ID, "blueprint"));

    /** Bumped when a payload's shape changes, so a client on the old shape is refused rather than misread. */
    private static final String VERSION = "2";

    @Override
    public Type<BlueprintPayload> type() {
        return TYPE;
    }

    static void register(RegisterPayloadHandlersEvent event) {
        StreamCodec<ByteBuf, BlueprintPayload> codec = StreamCodec.composite(
                UUIDUtil.STREAM_CODEC, BlueprintPayload::id,
                BlueprintBlocks.streamCodec(), BlueprintPayload::blocks,
                ByteBufCodecs.BOOL, BlueprintPayload::cut,
                BlueprintPayload::new);
        event.registrar(VERSION).playToClient(TYPE, codec, BlueprintPayload::handle);
    }

    private static void handle(BlueprintPayload payload, IPayloadContext context) {
        Blueprints.received(payload.id(), payload.blocks(), payload.cut());
    }
}
