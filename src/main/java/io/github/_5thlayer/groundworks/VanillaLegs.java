// SPDX-FileCopyrightText: 2026 5thlayer
// SPDX-License-Identifier: MIT

package io.github._5thlayer.groundworks;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.DirectionalPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;

/**
 * What the {@linkplain VanillaConsumer vanilla Consumer}'s stretches are built of: the held item,
 * laid at each position of the leg as vanilla places it, a player looking along the leg (ADR 0005).
 * It claims the block items in {@link VanillaConsumer#STRETCHES}.
 *
 * <p>A rise of k climbs one block per column right after the leg's first anchor, along its first
 * direction, so its top is k columns on, and the leg runs level after it. A player laying it looks
 * up the rise, so stairs climb as a staircase either way; the level run looks the way it travels.
 * The rise never turns, or the leg is refused whole.
 *
 * <p>Each block is asked of vanilla as {@link Placements#vanillaPlan} asks it, one at a time
 * against the world as it stands: a spot vanilla refuses, taken or unable to hold the block, is
 * refused there, so Groundworks goes round it. Groundworks sets no block state itself. What a block
 * becomes beside the rest of the stretch -- stairs joining at a corner, a rail sloping up to the
 * next, a fence reaching its neighbour -- vanilla does as the stretch is laid, so the preview draws
 * each block as it would be placed alone.
 */
final class VanillaLegs implements LegBuilder {

    /** The vanilla Consumer's own refusals. */
    enum Refused implements Refusal {
        /** The rise turns, or doesn't fit, before the leg's last anchor. */
        RISE_DOES_NOT_FIT
    }

    /** Where the leg lays a block, and the way a player laying it looks. */
    record Spot(BlockPos pos, Direction facing) {
    }

    @Override
    public boolean claims(Item item) {
        return item instanceof BlockItem && BuiltInRegistries.ITEM.wrapAsHolder(item).is(VanillaConsumer.STRETCHES);
    }

    @Override
    public PlacementPlan build(Level level, Item item, Leg leg) {
        BlockItem blockItem = (BlockItem) item;
        ItemStack stack = new ItemStack(item);
        List<PlacementPlan.Placed> blocks = new ArrayList<>();
        Refusal refusal = riseFits(leg) ? null : Refused.RISE_DOES_NOT_FIT;
        for (Spot spot : spots(leg)) {
            Laying context = new Laying(level, spot, stack);
            PlacementPlan alone = Placements.vanillaPlan(blockItem, context);
            if (alone == null) {
                // Vanilla has no plan where the spot is taken, and it is still drawn there.
                BlockState state = blockItem.getBlock().getStateForPlacement(context);
                blocks.add(new PlacementPlan.Placed(spot.pos(), state != null ? state : blockItem.getBlock().defaultBlockState()));
            } else {
                blocks.addAll(alone.blocks());
            }
            if (refusal == null && (alone == null || alone.isRefused())) {
                refusal = new Refusal.At(Refusal.Vanilla.VANILLA, spot.pos());
            }
        }
        return new PlacementPlan(blocks, List.of(), refusal);
    }

    @Override
    public Component message(Refusal refusal) {
        return refusal == Refused.RISE_DOES_NOT_FIT
                ? Component.translatable("message.groundworks.stretch_rise_does_not_fit")
                : Component.translatable("message.groundworks.stretch_vanilla_refused");
    }

    /** Each position of the leg in order, from its first anchor, and the way a player laying it looks. */
    static List<Spot> spots(Leg leg) {
        int climb = Math.abs(leg.rise());
        int step = Integer.signum(leg.rise());
        List<Spot> spots = new ArrayList<>();
        for (int i = 0; i < leg.route().size(); i++) {
            Leg.Column column = leg.route().get(i);
            boolean rising = i <= climb;
            BlockPos pos = new BlockPos(column.x(), leg.from().getY() + step * Math.min(i, climb), column.z());
            spots.add(new Spot(pos, rising && step < 0 ? column.travel().getOpposite() : column.travel()));
        }
        return spots;
    }

    /** Whether each column of the rise, up to its top, runs the leg's first way. */
    static boolean riseFits(Leg leg) {
        int climb = Math.abs(leg.rise());
        if (climb >= leg.route().size()) {
            return false;
        }
        Direction first = leg.route().getFirst().travel();
        for (int i = 1; i <= climb; i++) {
            if (leg.route().get(i).travel() != first) {
                return false;
            }
        }
        return true;
    }

    /**
     * Vanilla's placement at a spot, as a dispenser's is, looking the spot's way and down at where
     * the block goes. It clicks the face a player looking that way would, so a log lies along the
     * leg and stairs and slabs take their bottom half. It has no player, so the height and turn a
     * held stack carries play no part: a stretch's height is its rises, and its look is its start's.
     */
    private static final class Laying extends DirectionalPlaceContext {

        private final Direction facing;

        Laying(Level level, Spot spot, ItemStack stack) {
            super(level, spot.pos(), spot.facing(), stack, spot.facing().getOpposite());
            this.facing = spot.facing();
        }

        @Override
        public Direction getNearestLookingDirection() {
            return facing;
        }

        @Override
        public Direction getNearestLookingVerticalDirection() {
            return Direction.DOWN;
        }

        @Override
        public Direction[] getNearestLookingDirections() {
            return new Direction[]{facing, Direction.DOWN, facing.getClockWise(), facing.getCounterClockWise(),
                    Direction.UP, facing.getOpposite()};
        }
    }
}
