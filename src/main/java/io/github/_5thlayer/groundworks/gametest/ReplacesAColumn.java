// SPDX-FileCopyrightText: 2026 5thlayer
// SPDX-License-Identifier: MIT

package io.github._5thlayer.groundworks.gametest;

import java.util.List;

import io.github._5thlayer.groundworks.PlacementPlan;
import io.github._5thlayer.groundworks.ReplaceBuilder;
import io.github._5thlayer.groundworks.Refusal;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;

/**
 * The tests' {@linkplain ReplaceBuilder replace builder}, for a Replace group whose replace spans a
 * column of two blocks, as a pole's does. The aimed block is one of the column when a block of its
 * kind stands above or below it, and the plan puts the held block's default state at both. A block
 * with no partner is no column, and the builder plans nothing. It hands back an amethyst shard,
 * not the aimed block's item, and sets no orientation: the library copies none onto a builder's plan.
 *
 * <p>Each {@link Way} is a group of its own, so a test picks the builder's behaviour by the blocks it
 * stands on.
 */
final class ReplacesAColumn implements ReplaceBuilder {

    /** What the builder does with a column. */
    enum Way {
        /** Plans the replace of the whole column. */
        REPLACES,
        /** Replaces both blocks of the column and places only the upper one. */
        LEAVES_A_GAP,
        /** Plans the column and refuses it with a reason of its own, standing at the aimed block. */
        REFUSES,
    }

    /** The builder's own refusal. */
    enum Refuses implements Refusal {
        NOT_THIS_COLUMN,
    }

    static final String REFUSED = "message.groundworks.gametest_not_this_column";

    private final Way way;

    ReplacesAColumn(Way way) {
        this.way = way;
    }

    @Override
    public @Nullable PlacementPlan plan(Level level, @Nullable Player player, ItemStack held, BlockPos aimed,
                                        BlockState old) {
        BlockPos base = baseOf(level, aimed, old);
        if (base == null) {
            return null;
        }
        BlockState state = ((BlockItem) held.getItem()).getBlock().defaultBlockState();
        PlacementPlan.Placed lower = new PlacementPlan.Placed(base, state);
        PlacementPlan.Placed upper = new PlacementPlan.Placed(base.above(), state);
        return switch (way) {
            case REPLACES -> PlacementPlan.replacing(List.of(lower, upper), null);
            case LEAVES_A_GAP -> new PlacementPlan(List.of(upper), List.of(base, base.above()), null);
            case REFUSES -> PlacementPlan.replacing(List.of(lower, upper), new Refusal.At(Refuses.NOT_THIS_COLUMN, aimed));
        };
    }

    @Override
    public ItemStack refund(Level level, BlockPos aimed, BlockState old) {
        return new ItemStack(Items.AMETHYST_SHARD);
    }

    @Override
    public Component message(Refusal refusal) {
        if (refusal != Refuses.NOT_THIS_COLUMN) {
            throw new IllegalArgumentException("asked of " + refusal + ", not of the builder's own reason");
        }
        return Component.translatableWithFallback(REFUSED, "Not this column.");
    }

    /** The lower block of the column the aimed block is one of, or {@code null} when it has no partner. */
    private static @Nullable BlockPos baseOf(Level level, BlockPos aimed, BlockState old) {
        if (level.getBlockState(aimed.above()).is(old.getBlock())) {
            return aimed;
        }
        return level.getBlockState(aimed.below()).is(old.getBlock()) ? aimed.below() : null;
    }
}
