// SPDX-FileCopyrightText: 2026 5thlayer
// SPDX-License-Identifier: MIT

package io.github._5thlayer.groundworks;

import java.util.ArrayList;
import java.util.List;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;

/**
 * A Stretch being drawn, kept on the held stack as {@link Groundworks#STRETCH}: its start in its
 * dimension, at the start's own height, the look stored with it, and each anchor added since, with
 * the rise of the leg that ends there. The rise of the leg being drawn is the stack's
 * {@linkplain Raise height}, kept apart as ever.
 */
public record StoredStretch(ResourceKey<Level> dimension, BlockPos start, Direction look, List<Anchor> anchors) {

    /**
     * An anchor seen from above and the rise of the leg that ends at it. Its height is the
     * stretch's height there, so it is not stored.
     */
    public record Anchor(int x, int z, int rise) {

        private static final Codec<Anchor> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Codec.INT.fieldOf("x").forGetter(Anchor::x),
                Codec.INT.fieldOf("z").forGetter(Anchor::z),
                Codec.INT.fieldOf("rise").forGetter(Anchor::rise)
        ).apply(instance, Anchor::new));

        private static final StreamCodec<ByteBuf, Anchor> STREAM_CODEC = StreamCodec.composite(
                ByteBufCodecs.VAR_INT, Anchor::x,
                ByteBufCodecs.VAR_INT, Anchor::z,
                ByteBufCodecs.VAR_INT, Anchor::rise,
                Anchor::new);
    }

    public StoredStretch {
        start = start.immutable();
        anchors = List.copyOf(anchors);
    }

    /** This stretch with one more anchor. */
    StoredStretch with(Anchor anchor) {
        List<Anchor> more = new ArrayList<>(anchors);
        more.add(anchor);
        return new StoredStretch(dimension, start, look, more);
    }

    /** Each anchor where it stands: seen from above where it was aimed, at the stretch's height there. */
    public List<BlockPos> anchorPositions() {
        List<BlockPos> positions = new ArrayList<>();
        int y = start.getY();
        for (Anchor anchor : anchors) {
            y += anchor.rise();
            positions.add(new BlockPos(anchor.x(), y, anchor.z()));
        }
        return positions;
    }

    /** The last anchor, or the start while there is none: where the leg being drawn begins. */
    BlockPos last() {
        return anchors.isEmpty() ? start : anchorPositions().getLast();
    }

    // Built when the component registers, as DismantleStart's is.
    static DataComponentType<StoredStretch> componentType() {
        return DataComponentType.<StoredStretch>builder()
                .persistent(RecordCodecBuilder.create(instance -> instance.group(
                        Level.RESOURCE_KEY_CODEC.fieldOf("dimension").forGetter(StoredStretch::dimension),
                        BlockPos.CODEC.fieldOf("start").forGetter(StoredStretch::start),
                        Direction.CODEC.fieldOf("look").forGetter(StoredStretch::look),
                        Anchor.CODEC.listOf().fieldOf("anchors").forGetter(StoredStretch::anchors)
                ).apply(instance, StoredStretch::new)))
                .networkSynchronized(StreamCodec.composite(
                        ResourceKey.streamCodec(Registries.DIMENSION), StoredStretch::dimension,
                        BlockPos.STREAM_CODEC, StoredStretch::start,
                        Direction.STREAM_CODEC, StoredStretch::look,
                        Anchor.STREAM_CODEC.apply(ByteBufCodecs.list()), StoredStretch::anchors,
                        StoredStretch::new))
                .build();
    }
}
