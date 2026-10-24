// SPDX-FileCopyrightText: 2026 5thlayer
// SPDX-License-Identifier: MIT

package io.github._5thlayer.groundworks;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Predicate;

import io.github._5thlayer.groundworks.mixin.UseOnContextInvoker;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BedPart;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.phys.BlockHitResult;
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
 * refusals are theirs, and owning them is unbounded. Vanilla's blocks are opted in by the
 * {@linkplain VanillaConsumer vanilla Consumer}, through a tag a pack can change.
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
     * Whether a Placement Preview is drawn for this item wherever it is aimed: it plans its own
     * placement, its block is opted in, or it {@linkplain Stretches stretches}. A {@linkplain
     * FastReplace Fast Replace} is drawn whatever this says, where it is aimed at a block of its
     * group; {@link Raise} and {@link Rotate} ask this alone.
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
     * <p>It is the plan of the {@linkplain Feature feature} that takes the click, in the one order the click
     * walks as well: a {@linkplain Stretches Stretch} being drawn with the main hand's stack, a held
     * {@linkplain Assemblies Key}'s Assembly, or a {@linkplain FastReplace Fast Replace}, each drawn
     * whether or not the item is. A sneak-click that would store a stretch's start places nothing,
     * so it has no plan.
     */
    @Nullable
    public static PlacementPlan planFor(Level level, @Nullable Player player, InteractionHand hand,
                                        ItemStack stack, BlockHitResult hit) {
        Item item = stack.getItem();
        Feature.Claim claim = Feature.claimOf(level, player, hand, stack, hit);
        if (claim != null) {
            return claim.plan();
        }
        if (!isDrawn(item)) {
            return null;
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
     * <p>It is {@linkplain VanillaPlacement vanilla's own placement}, asked of the world as it
     * stands, because the whole value of deferring is that facing, replaceable blocks and state
     * survival come out right without the library having an opinion.
     *
     * <p>Where vanilla would refuse before there is even a position -- an unplaceable context, a
     * null state -- there is no plan, so nothing is drawn. Where it refuses <em>at</em> a position,
     * that position draws red. A spot moved by the held stack's {@linkplain Raise height} is a
     * position the player chose, so one that is taken, or outside the world, refuses there rather
     * than drawing nothing. So is a block whose Companion's spot is taken or outside the world, as a
     * door's with no room above or a bed's with no room for its head: vanilla has no state for it,
     * and the block is refused whole where the player aimed, faced the way they look. A block with
     * no state for any other reason, as a ladder with nothing to hang on, still draws nothing.
     *
     * <p>Vanilla answers with the one part the player places, and puts down the rest in the block's
     * {@code setPlacedBy}, so the plan names that rest as its {@linkplain #companionsOf Companions},
     * refused or not.
     */
    @Nullable
    public static PlacementPlan vanillaPlan(BlockItem item, BlockPlaceContext context) {
        PlacementPlan plan = placedPlan(item, context);
        return plan == null ? null
                : new PlacementPlan(plan.blocks(), plan.replaces(), plan.clears(), companionsOf(plan.blocks()), plan.refusal());
    }

    @Nullable
    private static PlacementPlan placedPlan(BlockItem item, BlockPlaceContext context) {
        VanillaPlacement placement = VanillaPlacement.of(item, context);
        return switch (placement.outcome()) {
            case DISABLED, NO_CONTEXT -> null;
            case NO_STATE -> companionBlocked(item, placement.context());
            case CANNOT_PLACE -> {
                if (Raise.heightOf(context).equals(Height.NONE)) {
                    yield null;
                }
                // The spot may be why a block has no state for it, and the refusal is still drawn there.
                BlockState state = item.getBlock().getStateForPlacement(context);
                yield PlacementPlan.refused(context.getClickedPos(),
                        state != null ? state : item.getBlock().defaultBlockState(), Refusal.Vanilla.VANILLA);
            }
            case WOULD_NOT_SURVIVE, REFUSED -> PlacementPlan.refused(placement.context().getClickedPos(),
                    placement.state(), Refusal.Vanilla.VANILLA);
            case PLACES -> PlacementPlan.accepted(placement.context().getClickedPos(), placement.state());
        };
    }

    /**
     * The block refused whole where a Companion of its default state, faced the way the player
     * looks, has no room, as vanilla's own check for a door, a tall plant or a bed reads it; or
     * {@code null} where that isn't why it has no state.
     */
    @Nullable
    private static PlacementPlan companionBlocked(BlockItem item, BlockPlaceContext context) {
        BlockState state = item.getBlock().defaultBlockState();
        if (state.hasProperty(BlockStateProperties.HORIZONTAL_FACING)) {
            state = state.setValue(BlockStateProperties.HORIZONTAL_FACING, context.getHorizontalDirection());
        }
        BlockPos pos = context.getClickedPos();
        Level level = context.getLevel();
        boolean blocked = companionsOf(List.of(new PlacementPlan.Placed(pos, state))).stream()
                .map(PlacementPlan.Placed::pos)
                .anyMatch(at -> level.isOutsideBuildHeight(at) || !level.getWorldBorder().isWithinBounds(at)
                        || !level.getBlockState(at).canBeReplaced(context));
        return blocked ? PlacementPlan.refused(pos, state, Refusal.Vanilla.VANILLA) : null;
    }

    /**
     * The Companions vanilla puts down once these blocks are placed: the upper half above a lower
     * {@code DOUBLE_BLOCK_HALF}, as a door's or a tall plant's, and the head a bed's foot faces.
     *
     * <p>Read from the placed states' own properties rather than asked of the game, since asking
     * means running {@code setPlacedBy}, which changes the world, and which a bed skips on the
     * client. A modded block that uses the same properties gets the same Companions.
     */
    static List<PlacementPlan.Placed> companionsOf(List<PlacementPlan.Placed> blocks) {
        List<PlacementPlan.Placed> companions = new ArrayList<>();
        for (PlacementPlan.Placed placed : blocks) {
            BlockState state = placed.state();
            if (state.getOptionalValue(BlockStateProperties.DOUBLE_BLOCK_HALF).orElse(null) == DoubleBlockHalf.LOWER) {
                companions.add(new PlacementPlan.Placed(placed.pos().above(),
                        state.setValue(BlockStateProperties.DOUBLE_BLOCK_HALF, DoubleBlockHalf.UPPER)));
            }
            Direction facing = state.getOptionalValue(BlockStateProperties.HORIZONTAL_FACING).orElse(null);
            if (state.getOptionalValue(BlockStateProperties.BED_PART).orElse(null) == BedPart.FOOT && facing != null) {
                companions.add(new PlacementPlan.Placed(placed.pos().relative(facing),
                        state.setValue(BlockStateProperties.BED_PART, BedPart.HEAD)));
            }
        }
        return companions;
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

    /**
     * Whether this context is a {@linkplain FastReplace Fast Replace}'s, which plans the held item
     * in the aimed block's place: an item's own plan may ask, to plan a replace its own way.
     */
    public static boolean isReplacing(BlockPlaceContext context) {
        return context instanceof ReplacingContext;
    }
}
