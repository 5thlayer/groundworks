// SPDX-FileCopyrightText: 2026 5thlayer
// SPDX-License-Identifier: MIT

package io.github._5thlayer.groundworks;

import java.util.List;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;

/**
 * What a held item would do at an aimed spot: the positions it would fill, the blockstate at each,
 * and a {@link Refusal} or none.
 *
 * <p>The preview draws a plan and the click executes one, so there is exactly one rule and the
 * preview cannot drift from what placing actually does. That matters because a player builds
 * against a preview, and a preview that lies is worse than none.
 *
 * <p><b>A refused plan still carries its blocks.</b> That is deliberate: a refusal has to be drawn
 * somewhere, and the only honest place is where the blocks would have gone. A plan with no blocks
 * at all is not a refusal, it is the absence of a plan, and callers express that with {@code null}
 * rather than an empty one -- see {@link Placements#planFor}.
 *
 * <p><b>A Footprint is one plan and refuses whole.</b> A footprint that places every part in one
 * gesture is one plan with one refusal, and so is an {@linkplain Assemblies Assembly}; drawing one
 * part red and the rest translucent would promise a partial placement the game never performs.
 *
 * <p>{@code replaces} names the placed blocks that swap out what already stands there, drawn apart
 * from the rest, and is empty for an ordinary placement. A plan may replace some of its blocks and
 * place the rest.
 *
 * <p>{@code clears} names the positions the plan would empty without placing there, as an Assembly's
 * empty positions lose the grass standing in them, so the preview marks them apart from the
 * blocks; none of them is among {@code blocks}, and it is empty for a plan that clears nothing. A
 * position the clearing leaves as it is, air or a fluid that stays, is not named. A refused plan
 * still names them, as it does its blocks, and clears nothing.
 *
 * <p>{@code companions} names the Companions: the blocks a placed block puts down itself once it is
 * placed, as a door's lower half puts down its upper half. The preview draws them as it draws the
 * blocks, but no path that lays a plan places them or charges for them, since the placed block lays
 * them itself; none of them is among {@code blocks}, and it is empty for a plan with none.
 *
 * <p>{@code leftOut} names the positions the plan leaves out, each alone, while the rest is laid:
 * a {@linkplain Blueprints paste}'s block that can't stand where it lands, as a torch whose wall
 * stayed outside the box. The preview outlines them red, a block at a time, since unlike a refusal
 * they refuse nothing else; none of them is among {@code blocks}, nothing is laid or charged there,
 * and it is empty for a plan that leaves nothing out.
 */
public record PlacementPlan(List<Placed> blocks, List<BlockPos> replaces, List<BlockPos> clears,
                            List<Placed> companions, List<BlockPos> leftOut, @Nullable Refusal refusal) {

    /** One block the plan would put down. */
    public record Placed(BlockPos pos, BlockState state) {
    }

    public PlacementPlan {
        blocks = List.copyOf(blocks);
        replaces = List.copyOf(replaces);
        clears = List.copyOf(clears);
        companions = List.copyOf(companions);
        leftOut = List.copyOf(leftOut);
    }

    /** A plan that leaves nothing out. */
    public PlacementPlan(List<Placed> blocks, List<BlockPos> replaces, List<BlockPos> clears,
                         List<Placed> companions, @Nullable Refusal refusal) {
        this(blocks, replaces, clears, companions, List.of(), refusal);
    }

    /** A plan with no Companions. */
    public PlacementPlan(List<Placed> blocks, List<BlockPos> replaces, List<BlockPos> clears,
                         @Nullable Refusal refusal) {
        this(blocks, replaces, clears, List.of(), refusal);
    }

    /** A plan that clears nothing. */
    public PlacementPlan(List<Placed> blocks, List<BlockPos> replaces, @Nullable Refusal refusal) {
        this(blocks, replaces, List.of(), refusal);
    }

    /** A plan that would go through. */
    public static PlacementPlan accepted(List<Placed> blocks) {
        return new PlacementPlan(blocks, List.of(), null);
    }

    /** A plan that would not, drawn where its blocks would have gone. */
    public static PlacementPlan refused(List<Placed> blocks, Refusal refusal) {
        return new PlacementPlan(blocks, List.of(), refusal);
    }

    /** A replace, or its refusal when {@code refusal} is not null. */
    public static PlacementPlan replacing(Placed block, @Nullable Refusal refusal) {
        return replacing(List.of(block), refusal);
    }

    /** A replace of several blocks as one, like a column; refused whole. */
    public static PlacementPlan replacing(List<Placed> blocks, @Nullable Refusal refusal) {
        return new PlacementPlan(blocks, blocks.stream().map(Placed::pos).toList(), refusal);
    }

    /** A one-block plan that would go through. */
    public static PlacementPlan accepted(BlockPos pos, BlockState state) {
        return accepted(List.of(new Placed(pos, state)));
    }

    /** A one-block plan that would not. */
    public static PlacementPlan refused(BlockPos pos, BlockState state, Refusal refusal) {
        return refused(List.of(new Placed(pos, state)), refusal);
    }

    public boolean isRefused() {
        return refusal != null;
    }

    public boolean isReplace() {
        return !replaces.isEmpty();
    }
}
