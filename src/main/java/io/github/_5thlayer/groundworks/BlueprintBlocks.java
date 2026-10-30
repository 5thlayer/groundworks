// SPDX-FileCopyrightText: 2026 5thlayer
// SPDX-License-Identifier: MIT

package io.github._5thlayer.groundworks;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.IntStream;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.network.VarInt;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.BoundingBox;

/**
 * A {@linkplain Blueprints Blueprint}'s blocks: the box's size, and each position's full block
 * state, air among them, as a palette and an index into it per position, x fastest, then z, then
 * y, so the bottom layer comes first. What the client is sent to draw a paste's preview, and what
 * the world data keeps.
 */
record BlueprintBlocks(Vec3i size, List<BlockState> palette, int[] indices) {

    BlueprintBlocks {
        palette = List.copyOf(palette);
        if (indices.length != size.getX() * size.getY() * size.getZ()) {
            throw new IllegalArgumentException(indices.length + " positions for a box of " + size);
        }
    }

    // Built when asked for rather than as constants: a block state's codecs read the block registry.
    static Codec<BlueprintBlocks> codec() {
        return RecordCodecBuilder.create(instance -> instance.group(
                Vec3i.CODEC.fieldOf("size").forGetter(BlueprintBlocks::size),
                BlockState.CODEC.listOf().fieldOf("palette").forGetter(BlueprintBlocks::palette),
                Codec.INT_STREAM.xmap(IntStream::toArray, IntStream::of).fieldOf("blocks")
                        .forGetter(BlueprintBlocks::indices)
        ).apply(instance, BlueprintBlocks::new));
    }

    static StreamCodec<ByteBuf, BlueprintBlocks> streamCodec() {
        return StreamCodec.composite(
                Vec3i.STREAM_CODEC, BlueprintBlocks::size,
                ByteBufCodecs.idMapper(Block.BLOCK_STATE_REGISTRY).apply(ByteBufCodecs.list()), BlueprintBlocks::palette,
                StreamCodec.<ByteBuf, int[]>of(BlueprintBlocks::writeIndices, BlueprintBlocks::readIndices),
                BlueprintBlocks::indices,
                BlueprintBlocks::new);
    }

    private static void writeIndices(ByteBuf buffer, int[] indices) {
        VarInt.write(buffer, indices.length);
        for (int index : indices) {
            VarInt.write(buffer, index);
        }
    }

    private static int[] readIndices(ByteBuf buffer) {
        int[] indices = new int[VarInt.read(buffer)];
        for (int i = 0; i < indices.length; i++) {
            indices[i] = VarInt.read(buffer);
        }
        return indices;
    }

    /**
     * The blocks standing in the box, both corners included, as the level holds them now, each
     * position left out captured as air.
     */
    static BlueprintBlocks capture(Level level, BoundingBox box, Set<BlockPos> leftOut) {
        Vec3i size = new Vec3i(box.getXSpan(), box.getYSpan(), box.getZSpan());
        List<BlockState> palette = new ArrayList<>();
        Map<BlockState, Integer> indexOf = new HashMap<>();
        int[] indices = new int[size.getX() * size.getY() * size.getZ()];
        BlockPos.MutableBlockPos at = new BlockPos.MutableBlockPos();
        int i = 0;
        for (int y = 0; y < size.getY(); y++) {
            for (int z = 0; z < size.getZ(); z++) {
                for (int x = 0; x < size.getX(); x++) {
                    at.set(box.minX() + x, box.minY() + y, box.minZ() + z);
                    BlockState state = leftOut.contains(at) ? Blocks.AIR.defaultBlockState() : level.getBlockState(at);
                    indices[i++] = indexOf.computeIfAbsent(state, added -> {
                        palette.add(added);
                        return palette.size() - 1;
                    });
                }
            }
        }
        return new BlueprintBlocks(size, palette, indices);
    }

    /** One position of the box and the state captured there. */
    record Captured(BlockPos local, BlockState state) {
    }

    /** Every position of the box with what was captured there, air among them, in the box's order. */
    List<Captured> blocks() {
        List<Captured> blocks = new ArrayList<>(indices.length);
        int i = 0;
        for (int y = 0; y < size.getY(); y++) {
            for (int z = 0; z < size.getZ(); z++) {
                for (int x = 0; x < size.getX(); x++) {
                    blocks.add(new Captured(new BlockPos(x, y, z), palette.get(indices[i++])));
                }
            }
        }
        return blocks;
    }

    /** How many of the box's positions hold something other than air. */
    int standing() {
        int standing = 0;
        for (int index : indices) {
            if (!palette.get(index).isAir()) {
                standing++;
            }
        }
        return standing;
    }
}
