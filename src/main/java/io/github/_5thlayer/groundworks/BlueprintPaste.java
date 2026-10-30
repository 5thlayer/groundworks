// SPDX-FileCopyrightText: 2026 5thlayer
// SPDX-License-Identifier: MIT

package io.github._5thlayer.groundworks;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;
import java.util.stream.LongStream;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

/**
 * One paste of a Copy, as its {@linkplain UndoPaste Undo Paste} history keeps it with the Copy's
 * world data: the dimension it was laid in, and each block it laid, in the order it laid them, with
 * the state it stood in once laid, as a palette and an index into it per block.
 */
record BlueprintPaste(ResourceKey<Level> dimension, long[] positions, List<BlockState> palette, int[] indices) {

    BlueprintPaste {
        palette = List.copyOf(palette);
        if (indices.length != positions.length) {
            throw new IllegalArgumentException(indices.length + " states for " + positions.length + " positions");
        }
    }

    /** The paste of these blocks, by position in the order laid, in this dimension. */
    static BlueprintPaste of(ResourceKey<Level> dimension, Map<BlockPos, BlockState> laid) {
        long[] positions = new long[laid.size()];
        int[] indices = new int[laid.size()];
        List<BlockState> palette = new ArrayList<>();
        Map<BlockState, Integer> indexOf = new HashMap<>();
        int i = 0;
        for (Map.Entry<BlockPos, BlockState> entry : laid.entrySet()) {
            positions[i] = entry.getKey().asLong();
            indices[i++] = indexOf.computeIfAbsent(entry.getValue(), added -> {
                palette.add(added);
                return palette.size() - 1;
            });
        }
        return new BlueprintPaste(dimension, positions, palette, indices);
    }

    // Built when asked for rather than as a constant: a block state's codec reads the block registry.
    static Codec<BlueprintPaste> codec() {
        return RecordCodecBuilder.create(instance -> instance.group(
                Level.RESOURCE_KEY_CODEC.fieldOf("dimension").forGetter(BlueprintPaste::dimension),
                Codec.LONG_STREAM.xmap(LongStream::toArray, LongStream::of).fieldOf("positions")
                        .forGetter(BlueprintPaste::positions),
                BlockState.CODEC.listOf().fieldOf("palette").forGetter(BlueprintPaste::palette),
                Codec.INT_STREAM.xmap(IntStream::toArray, IntStream::of).fieldOf("blocks")
                        .forGetter(BlueprintPaste::indices)
        ).apply(instance, BlueprintPaste::new));
    }

    /** One block the paste laid, and the state it stood in once laid. */
    record Laid(BlockPos pos, BlockState state) {
    }

    /** Every block the paste laid, in the order it laid them. */
    List<Laid> laid() {
        List<Laid> laid = new ArrayList<>(positions.length);
        for (int i = 0; i < positions.length; i++) {
            laid.add(new Laid(BlockPos.of(positions[i]), palette.get(indices[i])));
        }
        return laid;
    }
}
