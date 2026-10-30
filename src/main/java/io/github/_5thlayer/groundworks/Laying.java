// SPDX-FileCopyrightText: 2026 5thlayer
// SPDX-License-Identifier: MIT

package io.github._5thlayer.groundworks;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BooleanSupplier;
import java.util.function.Function;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.common.util.BlockSnapshot;
import net.neoforged.neoforge.event.EventHooks;
import org.jspecify.annotations.Nullable;

/**
 * A click that lays a {@link PlacementPlan}, a {@linkplain Stretches Stretch}'s, an {@linkplain
 * Assemblies Assembly}'s or a {@linkplain FastReplace Fast Replace}'s, refused, guarded, charged,
 * laid and told the same way for each: each feature plans its blocks and names its own refusals,
 * and the laying does the rest.
 *
 * <p>A plan refused for several reasons is refused for one, in one order: where the player may not
 * build at a position the plan changes, whatever else refuses it; then the plan's own reason; then
 * the items the player lacks; then no room for what comes back. A claim mod guarding NeoForge's
 * place event is asked last, on the click alone, at every position the plan changes and before any
 * of them changes, since the preview asks the plan every frame; it is asked only of a plan that would go
 * through, so no event is heard for a block that never goes down.
 *
 * @param plan what the click lays, refused or not
 * @param charge what laying it takes from the player and hands back
 * @param reasons the feature's own names for the refusals the laying makes
 */
record Laying(PlacementPlan plan, Charge charge, Reasons reasons) {

    /**
     * A feature's names for the refusals the laying makes for it, each the feature's own so a check
     * keeps comparing its own enum.
     *
     * @param mayNotBuild where the player may not build, a claim mod's guard among it
     * @param notEnoughItems where the player lacks the cost, or {@code null} for a feature whose
     *     cost is always held, as a Fast Replace's held item is
     * @param noRoomToReturn where what comes back would not fit, or {@code null} for a feature
     *     that hands nothing back, as an Assembly does
     */
    record Reasons(Refusal mayNotBuild, @Nullable Refusal notEnoughItems, @Nullable Refusal noRoomToReturn) {
    }

    /**
     * The plan, with its charge, refused for the first reason in the one order. The player affords
     * it where the main inventory holds the charge's cost.
     */
    static Laying of(Level level, @Nullable Player player, PlacementPlan plan, Charge charge, Reasons reasons) {
        return of(level, player, plan, charge, reasons, () -> charge.affordable(player));
    }

    /**
     * The same, with whether the player affords it said by the feature, as an Assembly's fill says
     * for the blocks its tag positions take.
     */
    static Laying of(Level level, @Nullable Player player, PlacementPlan plan, Charge charge, Reasons reasons,
            BooleanSupplier affordable) {
        if (player == null) {
            return new Laying(plan, charge, reasons);
        }
        Refusal refusal = refusal(Charge.mayBuildAll(level, player, changed(plan)), plan.refusal(), affordable,
                () -> charge.fits(player), reasons);
        return new Laying(refused(plan, refusal), charge, reasons);
    }

    /**
     * The first reason, in the one order, that refuses a plan: may-not-build, whatever else refuses
     * it; then the plan's own; then the items lacking; then no room for what comes back. What isn't
     * reached isn't asked.
     */
    static @Nullable Refusal refusal(boolean mayBuild, @Nullable Refusal own, BooleanSupplier affordable,
            BooleanSupplier fits, Reasons reasons) {
        if (!mayBuild) {
            return reasons.mayNotBuild();
        }
        if (own != null) {
            return own;
        }
        if (reasons.notEnoughItems() != null && !affordable.getAsBoolean()) {
            return reasons.notEnoughItems();
        }
        if (reasons.noRoomToReturn() != null && !fits.getAsBoolean()) {
            return reasons.noRoomToReturn();
        }
        return null;
    }

    /** Every position the plan changes: those it clears, then those it puts a block at. */
    static List<BlockPos> changed(PlacementPlan plan) {
        List<BlockPos> changed = new ArrayList<>(plan.clears());
        plan.blocks().forEach(placed -> changed.add(placed.pos()));
        return changed;
    }

    /** The same plan, refused for {@code refusal}, or the plan as it is where that is its own. */
    static PlacementPlan refused(PlacementPlan plan, @Nullable Refusal refusal) {
        return refusal == plan.refusal() ? plan
                : new PlacementPlan(plan.blocks(), plan.replaces(), plan.clears(), plan.companions(), plan.leftOut(),
                        refusal);
    }

    /**
     * The click, on the server: the player told the plan's refusal, or the may-not-build one where
     * a claim mod guards a position, and nothing changed; otherwise {@code lays} lays it.
     *
     * @param message what the player is told of a refusal, by the feature
     * @param lays the feature's laying: {@link #lay} for a Stretch and a Fast Replace, its own for an
     *     Assembly, whose Key goes last
     * @return whether it was laid
     */
    boolean carryOut(Level level, Player player, Function<Refusal, Component> message, Runnable lays) {
        if (plan.isRefused()) {
            tell(player, message.apply(plan.refusal()));
            return false;
        }
        for (BlockPos pos : changed(plan)) {
            if (EventHooks.onBlockPlace(player, BlockSnapshot.create(level.dimension(), level, pos), Direction.UP)) {
                tell(player, message.apply(reasons.mayNotBuild()));
                return false;
            }
        }
        lays.run();
        return true;
    }

    /**
     * Empties the positions the plan clears, then takes the charge, puts down the plan's blocks in
     * its order, hands back what comes back, and tells each block it was placed by the player with
     * {@code with}, a copy of the stack taken before the charge, which may take its last item. An
     * Assembly, whose Key goes last, lays its own way.
     */
    void lay(Level level, Player player, ItemStack with) {
        clear(level);
        charge.lay(level, player, plan);
        for (PlacementPlan.Placed placed : plan.blocks()) {
            Charge.tellPlaced(level, player, placed.pos(), with);
        }
    }

    /** Empties the positions the plan clears, leaving the fluid that was in one. */
    void clear(Level level) {
        for (BlockPos pos : plan.clears()) {
            level.setBlock(pos, emptied(level.getBlockState(pos)), Block.UPDATE_ALL);
        }
    }

    /** What a cleared position holds: nothing, or the fluid that was there. */
    static BlockState emptied(BlockState there) {
        return there.getFluidState().createLegacyBlock();
    }

    /** A refusal's reason, wherever it stands. */
    static Refusal reason(Refusal refusal) {
        return refusal instanceof Refusal.At at ? at.reason() : refusal;
    }

    /** Puts a message on the player's action bar. */
    static void tell(Player player, Component message) {
        if (player instanceof ServerPlayer server) {
            server.sendSystemMessage(message, true);
        }
    }
}
