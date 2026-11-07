// SPDX-FileCopyrightText: 2026 5thlayer
// SPDX-License-Identifier: MIT

package io.github._5thlayer.groundworks;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.Vec3i;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

/**
 * What a sheet's tooltip says of the {@linkplain Blueprints Blueprint} it holds, written beside
 * the Blueprint's id at its capture, since the client has the Blueprint itself only while the
 * sheet is held: whether it is a Cut, how many blocks stand in it, and the box's size.
 */
public record BlueprintSummary(boolean cut, int blocks, Vec3i size) {

    static DataComponentType<BlueprintSummary> componentType() {
        return DataComponentType.<BlueprintSummary>builder()
                .persistent(RecordCodecBuilder.create(instance -> instance.group(
                        Codec.BOOL.fieldOf("cut").forGetter(BlueprintSummary::cut),
                        Codec.INT.fieldOf("blocks").forGetter(BlueprintSummary::blocks),
                        Vec3i.CODEC.fieldOf("size").forGetter(BlueprintSummary::size)
                ).apply(instance, BlueprintSummary::new)))
                .networkSynchronized(StreamCodec.composite(
                        ByteBufCodecs.BOOL, BlueprintSummary::cut,
                        ByteBufCodecs.VAR_INT, BlueprintSummary::blocks,
                        Vec3i.STREAM_CODEC, BlueprintSummary::size,
                        BlueprintSummary::new))
                .build();
    }
}
