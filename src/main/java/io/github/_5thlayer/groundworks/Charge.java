// SPDX-FileCopyrightText: 2026 5thlayer
// SPDX-License-Identifier: MIT

package io.github._5thlayer.groundworks;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

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
 * What laying a plan costs the player in items and hands back, and the laying itself: the step a
 * {@linkplain Stretches Stretch} lays through, kept apart from legs and anchors so that a Fast
 * Replace can lay through it too (ADR 0008). Its caller says what the cost is and what comes back,
 * and names its own refusals.
 *
 * <p>The cost is one or several items with a count each: a Stretch's and a Fast Replace's is the
 * held item alone. A player with infinite materials is charged nothing and handed nothing. The cost
 * is taken from the main inventory, all of it or none, the held stack first, and what comes back is
 * handed back by the rules a Consumer's own replace hands back by, {@link HandBack}'s.
 */
record Charge(Map<Item, Integer> cost, List<ItemStack> returned) {

    Charge {
        cost = Collections.unmodifiableMap(new LinkedHashMap<>(cost));
        returned = List.copyOf(returned);
    }

    /** {@code cost} of {@code item}, handing {@code returned} back, or nothing to a player with infinite materials. */
    static Charge of(@Nullable Player player, Item item, int cost, List<ItemStack> returned) {
        return of(player, Map.of(item, cost), returned);
    }

    /**
     * {@code cost}, a count of each of its items in the order they are taken, handing {@code
     * returned} back, or nothing to a player with infinite materials.
     */
    static Charge of(@Nullable Player player, Map<Item, Integer> cost, List<ItemStack> returned) {
        if (player == null || player.hasInfiniteMaterials()) {
            return new Charge(Map.of(), List.of());
        }
        return new Charge(cost, returned);
    }

    /** Whether the main inventory holds all of the cost. */
    boolean affordable(Player player) {
        return missing(player).isEmpty();
    }

    /** How many of each of the cost's items the main inventory lacks, in the cost's order: none when it affords it. */
    Map<Item, Integer> missing(Player player) {
        Map<Item, Integer> held = new HashMap<>();
        for (ItemStack slot : player.getInventory().getNonEquipmentItems()) {
            if (!slot.isEmpty() && cost.containsKey(slot.getItem())) {
                held.merge(slot.getItem(), slot.getCount(), Integer::sum);
            }
        }
        return missing(cost, held);
    }

    /** How many of each of {@code cost}'s keys {@code held} lacks, in {@code cost}'s order. */
    static <K> Map<K, Integer> missing(Map<K, Integer> cost, Map<K, Integer> held) {
        Map<K, Integer> missing = new LinkedHashMap<>();
        cost.forEach((key, count) -> {
            int lacking = count - held.getOrDefault(key, 0);
            if (lacking > 0) {
                missing.put(key, lacking);
            }
        });
        return missing;
    }

    /** Whether what comes back fits in the main inventory once the cost is taken, as {@link #lay} puts it. */
    boolean fits(Player player) {
        return HandBack.fits(player, InteractionHand.MAIN_HAND, cost, returned);
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

    /** Takes the cost and hands back what comes back, putting nothing down. */
    void pay(Player player) {
        HandBack.give(player, InteractionHand.MAIN_HAND, HandBack.take(player, InteractionHand.MAIN_HAND, cost),
                returned);
    }

    /**
     * Takes the cost, puts down the plan's blocks in its order, and hands back what comes back. The
     * caller has refused the plan already if it doesn't {@linkplain #affordable afford} or
     * {@linkplain #fits fit} it.
     */
    void lay(Level level, Player player, PlacementPlan plan) {
        boolean freed = HandBack.take(player, InteractionHand.MAIN_HAND, cost);
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
