// SPDX-FileCopyrightText: 2026 5thlayer
// SPDX-License-Identifier: MIT

package io.github._5thlayer.groundworks;

import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;

/**
 * A blank sheet's stored first {@linkplain Blueprints Corner}, kept on the held stack as {@link
 * Groundworks#BLUEPRINT_CORNER}: where it is, and in which dimension, since one in another
 * dimension is no Corner.
 */
public record BlueprintCorner(ResourceKey<Level> dimension, BlockPos pos) {

    public BlueprintCorner {
        pos = pos.immutable();
    }

    static DataComponentType<BlueprintCorner> componentType() {
        return DataComponentType.<BlueprintCorner>builder()
                .persistent(RecordCodecBuilder.create(instance -> instance.group(
                        Level.RESOURCE_KEY_CODEC.fieldOf("dimension").forGetter(BlueprintCorner::dimension),
                        BlockPos.CODEC.fieldOf("pos").forGetter(BlueprintCorner::pos)
                ).apply(instance, BlueprintCorner::new)))
                .networkSynchronized(StreamCodec.composite(
                        ResourceKey.streamCodec(Registries.DIMENSION), BlueprintCorner::dimension,
                        BlockPos.STREAM_CODEC, BlueprintCorner::pos,
                        BlueprintCorner::new))
                .build();
    }
}
