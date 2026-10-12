// SPDX-FileCopyrightText: 2026 5thlayer
// SPDX-License-Identifier: MIT

package io.github._5thlayer.groundworks;

import java.util.Collection;
import java.util.List;

import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.gameevent.GameEvent;
import org.jspecify.annotations.Nullable;

/**
 * What laying a plan costs the player in the held item and hands back, and the laying itself: the
 * step a {@linkplain Stretches Stretch} lays through, kept apart from legs and anchors so that a
 * Fast Replace can lay through it too (ADR 0008). Its caller says what the cost is and what comes
 * back, and names its own refusals.
 *
 * <p>A player with infinite materials is charged nothing and handed nothing. The cost is taken
 * from the main inventory, the held stack first, and what comes back is handed back by the rules
 * a Consumer's own replace hands back by, {@link HandBack}'s.
 */
record Charge(Item item, int cost, List<ItemStack> returned) {

    Charge {
        returned = List.copyOf(returned);
    }

    /** {@code cost} of the held item, handing {@code returned} back, or nothing to a player with infinite materials. */
    static Charge of(@Nullable Player player, Item item, int cost, List<ItemStack> returned) {
        if (player == null || player.hasInfiniteMaterials()) {
            return new Charge(item, 0, List.of());
        }
        return new Charge(item, cost, returned);
    }

    /** Whether the main inventory holds the cost. */
    boolean affordable(Player player) {
        int held = 0;
        for (ItemStack slot : player.getInventory().getNonEquipmentItems()) {
            if (slot.is(item)) {
                held += slot.getCount();
            }
        }
        return held >= cost;
    }

    /** Whether what comes back fits in the main inventory once the cost is taken, as {@link #lay} puts it. */
    boolean fits(Player player) {
        return HandBack.fits(player, InteractionHand.MAIN_HAND, item, cost, returned);
    }

    /**
     * Whether the player may build at every position: not in adventure mode, say, and each inside
     * the world and not protected, as spawn is.
     */
    static boolean mayBuildAll(Level level, Player player, Collection<BlockPos> positions) {
        if (!player.mayBuild()) {
            return false;
        }
        for (BlockPos pos : positions) {
            if (!level.isInWorldBounds(pos) || !level.mayInteract(player, pos)) {
                return false;
            }
        }
        return true;
    }

    /**
     * Takes the cost, puts down the plan's blocks in its order, and hands back what comes back. The
     * caller has refused the plan already if it doesn't {@linkplain #affordable afford} or
     * {@linkplain #fits fit} it.
     */
    void lay(Level level, Player player, PlacementPlan plan) {
        boolean freed = HandBack.take(player, InteractionHand.MAIN_HAND, item, cost);
        for (PlacementPlan.Placed placed : plan.blocks()) {
            level.setBlock(placed.pos(), placed.state(), Block.UPDATE_ALL);
        }
        HandBack.give(player, InteractionHand.MAIN_HAND, freed, returned);
        if (plan.blocks().isEmpty()) {
            return;
        }
        PlacementPlan.Placed first = plan.blocks().getFirst();
        SoundType sound = first.state().getSoundType(level, first.pos(), player);
        level.playSound(null, first.pos(), sound.getPlaceSound(), SoundSource.BLOCKS,
                (sound.getVolume() + 1.0F) / 2.0F, sound.getPitch() * 0.8F);
        level.gameEvent(GameEvent.BLOCK_PLACE, first.pos(), GameEvent.Context.of(player, first.state()));
    }
}
