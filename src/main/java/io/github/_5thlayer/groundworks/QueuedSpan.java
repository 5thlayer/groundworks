// SPDX-FileCopyrightText: 2026 5thlayer
// SPDX-License-Identifier: MIT

package io.github._5thlayer.groundworks;

import java.util.List;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

/**
 * A Dismantle span a sneak-click queued, kept on the held stack in {@link Groundworks#DISMANTLE_QUEUE}
 * until a click confirms the pass: its start, as stored, and its end. Nothing is taken up while it
 * waits, and the confirming click plans it again.
 */
public record QueuedSpan(DismantleStart start, BlockPos end) {

    public QueuedSpan {
        end = end.immutable();
    }

    // Built when the component registers, as a start's are.
    static DataComponentType<List<QueuedSpan>> componentType() {
        Codec<QueuedSpan> codec = RecordCodecBuilder.create(instance -> instance.group(
                DismantleStart.codec().fieldOf("start").forGetter(QueuedSpan::start),
                BlockPos.CODEC.fieldOf("end").forGetter(QueuedSpan::end)
        ).apply(instance, QueuedSpan::new));
        StreamCodec<RegistryFriendlyByteBuf, QueuedSpan> streamCodec = StreamCodec.composite(
                DismantleStart.streamCodec(), QueuedSpan::start,
                BlockPos.STREAM_CODEC, QueuedSpan::end,
                QueuedSpan::new);
        return DataComponentType.<List<QueuedSpan>>builder()
                .persistent(codec.listOf())
                .networkSynchronized(streamCodec.apply(ByteBufCodecs.list()))
                .build();
    }
}
