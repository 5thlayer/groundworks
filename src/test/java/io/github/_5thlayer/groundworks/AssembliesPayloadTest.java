// SPDX-FileCopyrightText: 2026 5thlayer
// SPDX-License-Identifier: MIT

package io.github._5thlayer.groundworks;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Map;

import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

/** The declarations a server sends its clients, as they cross the wire and as a client keeps them until it leaves. */
class AssembliesPayloadTest {

    private static final Identifier LODESTONE = Identifier.withDefaultNamespace("lodestone");
    private static final Identifier TOWER_ID = Identifier.fromNamespaceAndPath("test", "tower");
    private static final Identifier PLINTH_ID = Identifier.fromNamespaceAndPath("test", "plinth");

    private static final AssemblyShape TOWER = parse("""
            {
              "key": "K",
              "legend": {"K": "minecraft:lodestone", "s": "minecraft:stone", "c": "minecraft:cobblestone"},
              "layers": [["c ", "ss"], ["  ", "K "]],
              "starts_alone": true
            }""");

    private static final AssemblyShape PLINTH = parse("""
            {
              "key": "K",
              "legend": {
                "K": "minecraft:target",
                "b": {"tag": "minecraft:beacon_base_blocks", "default": "minecraft:iron_block"},
                "o": "empty"
              },
              "layers": [["  ", "bb"], ["o ", "K "]]
            }""");

    @AfterEach
    void leave() {
        Assemblies.forget();
    }

    @Test
    void theDeclarationsCrossTheWireAsTheyWereSent() {
        ByteBuf buffer = Unpooled.buffer();
        AssembliesPayload.STREAM_CODEC.encode(buffer, new AssembliesPayload(Map.of(TOWER_ID, TOWER, PLINTH_ID, PLINTH)));
        Map<Identifier, AssemblyShape> received = AssembliesPayload.STREAM_CODEC.decode(buffer).declarations();

        assertEquals(Map.of(TOWER_ID, LODESTONE, PLINTH_ID, Identifier.withDefaultNamespace("target")),
                Map.of(TOWER_ID, received.get(TOWER_ID).keyBlock(), PLINTH_ID, received.get(PLINTH_ID).keyBlock()));
        assertTrue(received.get(TOWER_ID).startsAlone());
        for (Identifier id : received.keySet()) {
            AssemblyShape sent = id.equals(TOWER_ID) ? TOWER : PLINTH;
            assertEquals(sent.positions(BlockPos.ZERO, Direction.UP, Direction.WEST),
                    received.get(id).positions(BlockPos.ZERO, Direction.UP, Direction.WEST));
        }
    }

    @Test
    void aClientKeepsWhatItWasSentApartFromTheServerAndForgetsItAsItLeaves() {
        Assemblies.received(Map.of(TOWER_ID, TOWER));
        assertEquals(1, Assemblies.keyedBy(true, LODESTONE).size());
        assertEquals(0, Assemblies.keyedBy(false, LODESTONE).size());

        Assemblies.forget();
        assertEquals(0, Assemblies.keyedBy(true, LODESTONE).size());
    }

    private static AssemblyShape parse(String json) {
        return AssemblyShape.CODEC.parse(JsonOps.INSTANCE, JsonParser.parseString(json)).getOrThrow();
    }
}
