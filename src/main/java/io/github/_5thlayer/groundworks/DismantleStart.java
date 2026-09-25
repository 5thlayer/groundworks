// SPDX-FileCopyrightText: 2026 5thlayer
// SPDX-License-Identifier: MIT

package io.github._5thlayer.groundworks;

import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

/**
 * A Dismantle's stored start, kept on the held stack as {@link Groundworks#DISMANTLE_START}: where
 * it is, in which dimension, and the block that stood there when it was stored, which its family
 * compares with the block there now.
 */
public record DismantleStart(ResourceKey<Level> dimension, BlockPos pos, BlockState state) {

    public DismantleStart {
        pos = pos.immutable();
    }

    // Built when the component registers rather than as constants: a block state's codecs read the
    // block registry, and the rules about a start are tested with no booted game.
    static DataComponentType<DismantleStart> componentType() {
        return DataComponentType.<DismantleStart>builder()
                .persistent(RecordCodecBuilder.create(instance -> instance.group(
                        Level.RESOURCE_KEY_CODEC.fieldOf("dimension").forGetter(DismantleStart::dimension),
                        BlockPos.CODEC.fieldOf("pos").forGetter(DismantleStart::pos),
                        BlockState.CODEC.fieldOf("state").forGetter(DismantleStart::state)
                ).apply(instance, DismantleStart::new)))
                .networkSynchronized(StreamCodec.composite(
                        ResourceKey.streamCodec(Registries.DIMENSION), DismantleStart::dimension,
                        BlockPos.STREAM_CODEC, DismantleStart::pos,
                        ByteBufCodecs.idMapper(Block.BLOCK_STATE_REGISTRY), DismantleStart::state,
                        DismantleStart::new))
                .build();
    }
}
