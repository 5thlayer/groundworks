// SPDX-FileCopyrightText: 2026 5thlayer
// SPDX-License-Identifier: MIT

package io.github._5thlayer.groundworks;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Predicate;

import io.github._5thlayer.groundworks.mixin.UseOnContextInvoker;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import org.jspecify.annotations.Nullable;

/**
 * The one entry point to a {@link PlacementPlan}, the {@link #vanillaPlan} a plain block item is
 * served by, and the opt-in that says which plain block items get one.
 *
 * <h2>Only opted-in blocks get a vanilla plan</h2>
 *
 * <p>An item that {@linkplain PlansPlacement plans its own placement} is always drawn. A plain
 * {@link BlockItem} is drawn only when a Consumer has {@linkplain #optIn opted its block in}, and
 * the library keeps no list of mods: each Consumer states its own rule, such as "every block in my
 * namespace", which makes a new block previewable with no code at all. Other mods' placement
 * refusals are theirs, and owning them is unbounded.
 *
 * <p>The gate is the <em>block</em>, not the item, because what a preview is about is the block the
 * item puts down.
 */
public final class Placements {

    private static final List<Predicate<? super Block>> OPTED_IN = new CopyOnWriteArrayList<>();

    private Placements() {
    }

    /**
     * Gives the plain block items whose block matches a {@linkplain #vanillaPlan vanilla plan}, and
     * so a preview.
     *
     * <p>Called at mod construction, on both sides, since the plan is asked of on both. Mods are
     * constructed in parallel, so this may be called from several threads at once.
     */
    public static void optIn(Predicate<? super Block> blocks) {
        OPTED_IN.add(blocks);
    }

    /** Whether a Consumer has opted this block in. */
    public static boolean isOptedIn(Block block) {
        for (Predicate<? super Block> blocks : OPTED_IN) {
            if (blocks.test(block)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Whether a Placement Preview is drawn for this item: it plans its own placement, its block is
     * opted in, or it {@linkplain Stretches stretches}.
     */
    static boolean isDrawn(Item item) {
        return item instanceof PlansPlacement || item instanceof BlockItem block && isOptedIn(block.getBlock())
                || Stretches.builderOf(item) != null;
    }

    /**
     * What the held stack would do at this hit, or {@code null} if there is nothing to draw.
     *
     * <p>Safe on either side, and it reads the world without touching it: the client asks it every
     * frame (behind the preview's cache) and the server asks it on the click.
     *
     * <p>While a {@linkplain Stretches Stretch} is being drawn with the main hand's stack, it is the
     * stretch that a click would lay. A sneak-click that would store a stretch's start places
     * nothing, so it has no plan.
     */
    @Nullable
    public static PlacementPlan planFor(Level level, @Nullable Player player, InteractionHand hand,
                                        ItemStack stack, BlockHitResult hit) {
        Item item = stack.getItem();
        if (!isDrawn(item)) {
            return null;
        }
        if (hand == InteractionHand.MAIN_HAND) {
            if (player != null && Stretches.startAt(level, player, stack, hit) != null) {
                return null;
            }
            PlacementPlan stretch = Stretches.planFor(level, player, stack, hit);
            if (stretch != null) {
                return stretch;
            }
        }
        return planFor(item, new BlockPlaceContext(level, player, hand, stack, hit));
    }

    /**
     * The same, for a caller that already holds a context -- the item's own place. Not gated by
     * the opt-in, since an item asking about itself has already opted in.
     */
    @Nullable
    public static PlacementPlan planFor(Item item, BlockPlaceContext context) {
        if (item instanceof PlansPlacement plans) {
            return plans.plan(context);
        }
        if (item instanceof BlockItem block) {
            return vanillaPlan(block, context);
        }
        return null;
    }

    /**
     * The plan a plain {@link BlockItem} would carry out, asked of vanilla rather than restated.
     *
     * <p>This mirrors {@code BlockItem#place}'s decision chain exactly -- {@code canPlace}, then
     * {@code updatePlacementContext}, then {@code getStateForPlacement}, then survival and
     * obstruction -- because the whole value of deferring is that facing, replaceable blocks and
     * state survival come out right without the library having an opinion. The two protected steps
     * ({@code BlockItem#canPlace}, {@code mustSurvive}) are reproduced here rather than reached
     * through an accessor mixin; a mixin for two lines would be a second thing to keep in step.
     *
     * <p>Where vanilla would refuse before there is even a position -- an unplaceable context, a
     * null state -- there is no plan, so nothing is drawn. Where it refuses <em>at</em> a position,
     * that position draws red. A spot moved by the held stack's {@linkplain Raise height} is a
     * position the player chose, so one that is taken, or outside the world, refuses there rather
     * than drawing nothing.
     */
    @Nullable
    public static PlacementPlan vanillaPlan(BlockItem item, BlockPlaceContext context) {
        if (!item.getBlock().isEnabled(context.getLevel().enabledFeatures())) {
            return null;
        }
        if (!context.canPlace()) {
            if (Raise.heightOf(context).equals(Height.NONE)) {
                return null;
            }
            // The spot may be why a block has no state for it, and the refusal is still drawn there.
            BlockState state = item.getBlock().getStateForPlacement(context);
            return PlacementPlan.refused(context.getClickedPos(),
                    state != null ? state : item.getBlock().defaultBlockState(), Refusal.Vanilla.VANILLA);
        }
        BlockPlaceContext updated = item.updatePlacementContext(context);
        if (updated == null) {
            return null;
        }
        BlockState state = item.getBlock().getStateForPlacement(updated);
        if (state == null) {
            return null;
        }
        BlockPos pos = updated.getClickedPos();
        if (!survives(state, updated, pos)) {
            return PlacementPlan.refused(pos, state, Refusal.Vanilla.VANILLA);
        }
        return PlacementPlan.accepted(pos, state);
    }

    /**
     * The block the ray actually hit, which is not always the block a plan is about.
     *
     * <p>{@code BlockPlaceContext} answers {@code getClickedPos} with where the block would
     * <em>go</em> -- the hit position itself when it is replaceable, the neighbour across the hit
     * face otherwise, moved by the held stack's {@linkplain Raise height} -- and that is the right
     * answer for placing and the wrong one for a rule about what is being aimed at, like a column
     * the aim extends. {@code UseOnContext#getHitResult} is protected, so this reads it through an
     * invoker.
     */
    public static BlockPos aimedPos(BlockPlaceContext context) {
        return ((UseOnContextInvoker) context).groundworks$hitResult().getBlockPos();
    }

    /** Vanilla's own two conditions for "this state may stand here". */
    private static boolean survives(BlockState state, BlockPlaceContext context, BlockPos pos) {
        Level level = context.getLevel();
        return state.canSurvive(level, pos)
                && level.isUnobstructed(state, pos, CollisionContext.placementContext(context.getPlayer()));
    }
}
