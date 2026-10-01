// SPDX-FileCopyrightText: 2026 5thlayer
// SPDX-License-Identifier: MIT

package io.github._5thlayer.groundworks;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.block.state.properties.WallSide;
import org.jspecify.annotations.Nullable;

/**
 * The vanilla Consumer's {@linkplain DismantleFamily Dismantle families} (ADR 0005), one per block
 * tag under {@code groundworks:dismantle_family/}, read as the tags are now. A span never leaves
 * the tag its start is in: an end in another is not the same kind.
 *
 * <p>A span is the {@linkplain ShortestPath shortest joined path} through the tag's blocks. A block
 * whose state says which sides it connects on, as fences, walls, panes and bars do, is joined to a
 * member beside it only where both connect toward each other, and to one above or below by
 * touching. Any other block, as a rail, is joined to every member it touches, a step up or down
 * included, since a rail on a slope climbs one.
 *
 * <p>The tool is vanilla's: the one correct for the block's drops, so a span gives what breaking
 * each of its blocks with the held tool gives.
 */
final class VanillaFamilies implements DismantleFamily {

    static final String FOLDER = "dismantle_family/";

    /** The blocks already warned of, so a block in two family tags is warned of once. */
    private static final Set<Block> WARNED = ConcurrentHashMap.newKeySet();

    private static final Map<Direction, BooleanProperty> SIDES = Map.of(
            Direction.NORTH, BlockStateProperties.NORTH, Direction.EAST, BlockStateProperties.EAST,
            Direction.SOUTH, BlockStateProperties.SOUTH, Direction.WEST, BlockStateProperties.WEST);

    private static final Map<Direction, EnumProperty<WallSide>> WALL_SIDES = Map.of(
            Direction.NORTH, BlockStateProperties.NORTH_WALL, Direction.EAST, BlockStateProperties.EAST_WALL,
            Direction.SOUTH, BlockStateProperties.SOUTH_WALL, Direction.WEST, BlockStateProperties.WEST_WALL);

    /**
     * The family tag a block is in, or {@code null}. A block in two belongs to the first by id, and
     * is warned of.
     */
    static @Nullable TagKey<Block> familyOf(BlockState state) {
        return VanillaTags.firstUnder(state.getBlock(), FOLDER, "Dismantle family", WARNED);
    }

    @Override
    public boolean claims(BlockState state) {
        return familyOf(state) != null;
    }

    @Override
    public boolean acceptsTool(ItemStack tool, BlockState state) {
        return tool.isCorrectToolForDrops(state);
    }

    @Override
    public DismantleSpan span(Level level, BlockPos start, BlockPos end) {
        TagKey<Block> family = familyOf(level.getBlockState(start));
        if (family == null || !family.equals(familyOf(level.getBlockState(end)))) {
            return DismantleSpan.refused(Refusal.Dismantle.NOT_SAME_KIND);
        }
        ShortestPath.Result<BlockPos> path = ShortestPath.between(start.immutable(), end.immutable(), new Joined(level, family));
        return path.refusal() == null ? DismantleSpan.taking(path.path()) : DismantleSpan.refused(path.refusal());
    }

    @Override
    public Component message(Refusal refusal) {
        return switch ((ShortestPath.Refused) refusal) {
            case OUTSIDE_FAMILY -> Component.translatable("message.groundworks.dismantle_outside_family");
            case NOT_JOINED -> Component.translatable("message.groundworks.dismantle_not_joined");
            case TIED -> Component.translatable("message.groundworks.dismantle_tied");
        };
    }

    /** Whether the state says which sides it connects on. */
    static boolean hasSides(BlockState state) {
        return state.hasProperty(BlockStateProperties.NORTH) || state.hasProperty(BlockStateProperties.NORTH_WALL);
    }

    /** Whether a block with sides connects toward {@code side}. */
    static boolean connects(BlockState state, Direction side) {
        if (state.hasProperty(SIDES.get(side))) {
            return state.getValue(SIDES.get(side));
        }
        return state.hasProperty(WALL_SIDES.get(side)) && state.getValue(WALL_SIDES.get(side)) != WallSide.NONE;
    }

    /**
     * Whether two touching members are joined: {@code b} is beside {@code a}, above or below it, or
     * a step up or down beside it.
     */
    static boolean joined(BlockState a, BlockState b, BlockPos from, BlockPos to) {
        int dy = to.getY() - from.getY();
        int dx = to.getX() - from.getX();
        int dz = to.getZ() - from.getZ();
        Direction side = dx == 0 && dz == 0 ? null : Direction.getApproximateNearest(dx, 0, dz);
        if (!hasSides(a) && !hasSides(b)) {
            return true;
        }
        if (side == null) {
            return true;
        }
        return dy == 0 && connects(a, side) && connects(b, side.getOpposite());
    }

    /** The tag's blocks as a graph: every block touching another by a face, or a step up or down beside it. */
    private record Joined(Level level, TagKey<Block> family) implements ShortestPath.Graph<BlockPos> {

        @Override
        public boolean member(BlockPos pos) {
            return family.equals(familyOf(level.getBlockState(pos)));
        }

        @Override
        public Iterable<BlockPos> neighbours(BlockPos pos) {
            List<BlockPos> touching = new ArrayList<>();
            touching.add(pos.above());
            touching.add(pos.below());
            for (Direction side : Direction.Plane.HORIZONTAL) {
                BlockPos beside = pos.relative(side);
                touching.add(beside);
                touching.add(beside.above());
                touching.add(beside.below());
            }
            return touching;
        }

        @Override
        public boolean joined(BlockPos a, BlockPos b) {
            return VanillaFamilies.joined(level.getBlockState(a), level.getBlockState(b), a, b);
        }
    }
}
