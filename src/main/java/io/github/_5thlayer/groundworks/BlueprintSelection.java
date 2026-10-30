// SPDX-FileCopyrightText: 2026 5thlayer
// SPDX-License-Identifier: MIT

package io.github._5thlayer.groundworks;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentUtils;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BedPart;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import org.jspecify.annotations.Nullable;

/**
 * What a {@linkplain Blueprints Selection}'s capture leaves out, and why: each block in the box
 * that some {@link LeftOut reason} leaves out, the first reason that does. A block left out is
 * captured as air. What the capture tells the player, and what the Selection's outline draws red
 * while a Corner is stored, are the blocks left out for a {@linkplain LeftOut#isTold told} reason.
 */
public final class BlueprintSelection {

    private BlueprintSelection() {
    }

    /** A capture being asked of: the level, the player capturing, and the Selection's box. */
    public record Capturing(Level level, @Nullable Player player, BoundingBox box) {
    }

    /**
     * Why a capture leaves a block out, asked in this order, the first that leaves a block out
     * naming it. A new reason is a new constant: its message, or none for a silent one, and its rule.
     */
    public enum LeftOut {
        /** A block a Consumer {@linkplain Blueprints#denies denies} Blueprints, as bedrock: left out silently. */
        DENIED(null) {
            @Override
            boolean leaves(Capturing capturing, BlockPos pos, BlockState state) {
                return Blueprints.isDenied(state);
            }
        },
        /**
         * A block standing partly outside the box, as a door with one half in it, a bed, or a
         * {@link Footprint} partly inside: each of its positions inside the box is left out.
         */
        STRADDLING("message.groundworks.blueprint_left_out.straddling") {
            @Override
            boolean leaves(Capturing capturing, BlockPos pos, BlockState state) {
                for (BlockPos at : wholeOf(capturing.level(), pos, state)) {
                    if (!capturing.box().isInside(at)) {
                        return true;
                    }
                }
                return false;
            }
        };

        private final @Nullable String message;

        LeftOut(@Nullable String message) {
            this.message = message;
        }

        /** Whether this leaves the block at {@code pos}, standing in the box, out. */
        abstract boolean leaves(Capturing capturing, BlockPos pos, BlockState state);

        /** Whether the player is told of, and shown, the blocks left out for this reason. */
        public boolean isTold() {
            return message != null;
        }
    }

    /** The blocks the capture of this box leaves out, by position, in the box's order, with why. */
    static Map<BlockPos, LeftOut> leftOut(Level level, @Nullable Player player, BoundingBox box) {
        Capturing capturing = new Capturing(level, player, box);
        Map<BlockPos, LeftOut> leftOut = new LinkedHashMap<>();
        for (int y = box.minY(); y <= box.maxY(); y++) {
            for (int z = box.minZ(); z <= box.maxZ(); z++) {
                for (int x = box.minX(); x <= box.maxX(); x++) {
                    BlockPos pos = new BlockPos(x, y, z);
                    BlockState state = level.getBlockState(pos);
                    if (state.isAir()) {
                        continue;
                    }
                    for (LeftOut reason : LeftOut.values()) {
                        if (reason.leaves(capturing, pos, state)) {
                            leftOut.put(pos, reason);
                            break;
                        }
                    }
                }
            }
        }
        return leftOut;
    }

    /**
     * The blocks the Selection's outline draws red: those the capture of this box leaves out for a
     * told reason, or none for a box over the server's cap, which the capture refuses.
     */
    public static List<BlockPos> shown(Level level, @Nullable Player player, BoundingBox box) {
        if ((long) box.getXSpan() * box.getYSpan() * box.getZSpan() > GroundworksConfig.maxSelection()) {
            return List.of();
        }
        List<BlockPos> shown = new ArrayList<>();
        leftOut(level, player, box).forEach((pos, reason) -> {
            if (reason.isTold()) {
                shown.add(pos);
            }
        });
        return shown;
    }

    /**
     * What the player is told of the told reasons' blocks left out, one part per reason, each with
     * its count and the first such block's name; or {@code null} where none was.
     */
    static @Nullable Component told(Level level, Map<BlockPos, LeftOut> leftOut) {
        Map<LeftOut, Integer> counts = new LinkedHashMap<>();
        Map<LeftOut, BlockState> first = new LinkedHashMap<>();
        leftOut.forEach((pos, reason) -> {
            if (reason.isTold()) {
                counts.merge(reason, 1, Integer::sum);
                first.putIfAbsent(reason, level.getBlockState(pos));
            }
        });
        if (counts.isEmpty()) {
            return null;
        }
        List<Component> parts = new ArrayList<>();
        for (LeftOut reason : LeftOut.values()) {
            Integer count = counts.get(reason);
            if (count != null) {
                parts.add(Component.translatable(reason.message, count, first.get(reason).getBlock().getName()));
            }
        }
        return ComponentUtils.formatList(parts, Component.literal(", "));
    }

    /**
     * Every position the block at {@code pos} stands on, its own among them: a whole {@link
     * Footprint} from its standing origin, both halves of a {@code DOUBLE_BLOCK_HALF} block, both
     * parts of a bed, or the position alone.
     */
    private static List<BlockPos> wholeOf(Level level, BlockPos pos, BlockState state) {
        Footprint footprint = Footprint.of(state);
        if (footprint != null) {
            BlockPos origin = footprint.standingOrigin(level, pos, state);
            return origin == null ? List.of(pos)
                    : footprint.positions(origin, level.getBlockState(origin).getValue(Footprint.FACING));
        }
        DoubleBlockHalf half = state.getOptionalValue(BlockStateProperties.DOUBLE_BLOCK_HALF).orElse(null);
        if (half != null) {
            return List.of(pos, half == DoubleBlockHalf.LOWER ? pos.above() : pos.below());
        }
        BedPart part = state.getOptionalValue(BlockStateProperties.BED_PART).orElse(null);
        Direction facing = state.getOptionalValue(BlockStateProperties.HORIZONTAL_FACING).orElse(null);
        if (part != null && facing != null) {
            return List.of(pos, pos.relative(part == BedPart.FOOT ? facing : facing.getOpposite()));
        }
        return List.of(pos);
    }
}
