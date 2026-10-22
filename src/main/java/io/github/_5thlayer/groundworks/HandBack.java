// SPDX-FileCopyrightText: 2026 5thlayer
// SPDX-License-Identifier: MIT

package io.github._5thlayer.groundworks;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.jspecify.annotations.Nullable;

/**
 * What a replace hands back for the one item it charges from the held stack, by the rules the library's
 * {@linkplain FastReplace Fast Replace} and {@linkplain Stretches Stretch} hand back by (ADR 0008),
 * for a Consumer's item that plans its own replace and lays it itself.
 *
 * <p>What comes back goes into the slot the charge freed, the held one when it took its last item,
 * and otherwise where the game puts an item back: onto a stack of its own, then into the first
 * empty slot of the main inventory. A player with infinite materials is charged nothing and handed
 * nothing. Check {@link #fits} before anything changes and refuse the click when it fails, as a
 * Fast Replace refuses with {@link Refusal.FastReplace#NO_ROOM_TO_RETURN}; then {@link #charge}.
 */
public final class HandBack {

    private HandBack() {
    }

    /**
     * Whether {@code returned} fits once one item of the stack in {@code hand} is charged, as {@link
     * #charge} puts it. It is proved in the main inventory and the held slot alone, so for the main
     * hand, where the game would also fill a stack in the off hand, it may refuse what would fit; it
     * never passes what would drop.
     */
    public static boolean fits(@Nullable Player player, InteractionHand hand, List<ItemStack> returned) {
        if (player == null || player.hasInfiniteMaterials()) {
            return true;
        }
        return fits(player, hand, Map.of(player.getItemInHand(hand).getItem(), 1), returned);
    }

    /**
     * Charges one item of the stack in {@code hand} and hands {@code returned} back, after {@link
     * #fits} said it fits.
     */
    public static void charge(@Nullable Player player, InteractionHand hand, List<ItemStack> returned) {
        if (player == null || player.hasInfiniteMaterials()) {
            return;
        }
        boolean freed = take(player, hand, Map.of(player.getItemInHand(hand).getItem(), 1));
        give(player, hand, freed, returned);
    }

    /** Whether {@code returned} fits once {@code cost}, a count of each of its items, is {@linkplain #take taken}. */
    static boolean fits(Player player, InteractionHand hand, Map<Item, Integer> cost, List<ItemStack> returned) {
        return returned.isEmpty() || view(player, hand).ledger.fits(cost, stacksOf(returned));
    }

    /**
     * Takes {@code cost}, a count of each of its items, from the main inventory, the stack in {@code
     * hand} first, and answers whether that took the held stack's last item, freeing its slot for
     * what comes back.
     */
    static boolean take(Player player, InteractionHand hand, Map<Item, Integer> cost) {
        View view = view(player, hand);
        boolean freed = view.ledger.take(cost);
        for (int i = 0; i < view.slots.size(); i++) {
            ItemStack slot = view.slots.get(i);
            slot.shrink(slot.getCount() - view.ledger.count(i));
        }
        return freed;
    }

    /** How many of each of the cost's items the main inventory lacks, in the cost's order. */
    static Map<Item, Integer> missing(Player player, Map<Item, Integer> cost) {
        return view(player, InteractionHand.MAIN_HAND).ledger.missing(cost);
    }

    /** Hands {@code returned} back, into the held slot first when {@code freed}. */
    static void give(Player player, InteractionHand hand, boolean freed, List<ItemStack> returned) {
        for (ItemStack back : returned) {
            if (freed && player.getItemInHand(hand).isEmpty()) {
                putInHand(player, hand, back.copy());
            } else {
                player.getInventory().placeItemBackInInventory(back.copy());
            }
        }
    }

    /** A player's slots and the ledger over them: the main inventory, and the off hand after it for the off hand. */
    private record View(List<ItemStack> slots, Ledger<Item> ledger) {
    }

    private static View view(Player player, InteractionHand hand) {
        // The stacks themselves, so taking from them takes from the inventory.
        List<ItemStack> slots = new ArrayList<>(player.getInventory().getNonEquipmentItems());
        int main = slots.size();
        int held;
        if (hand == InteractionHand.MAIN_HAND) {
            held = player.getInventory().getSelectedSlot();
        } else {
            slots.add(player.getItemInHand(hand));
            held = main;
        }
        return new View(slots, new Ledger<>(stacksOf(slots), held, main));
    }

    private static List<Ledger.Stack<Item>> stacksOf(List<ItemStack> stacks) {
        List<Ledger.Stack<Item>> list = new ArrayList<>();
        for (ItemStack stack : stacks) {
            list.add(stack.isEmpty() ? Ledger.Stack.empty()
                    : new Ledger.Stack<>(stack.getItem(), new Kind(stack.copyWithCount(1)), stack.getCount(),
                            stack.getMaxStackSize()));
        }
        return list;
    }

    /** What stacks together: the same item with the same components. */
    private record Kind(ItemStack proto) {

        @Override
        public boolean equals(Object other) {
            return other instanceof Kind kind && ItemStack.isSameItemSameComponents(proto, kind.proto);
        }

        @Override
        public int hashCode() {
            return ItemStack.hashItemAndComponents(proto);
        }
    }

    /** Puts {@code stack} in {@code hand}, the main hand's as the inventory's own slot, as a placement refills it. */
    private static void putInHand(Player player, InteractionHand hand, ItemStack stack) {
        if (hand == InteractionHand.MAIN_HAND) {
            player.getInventory().setItem(player.getInventory().getSelectedSlot(), stack);
        } else {
            player.setItemInHand(hand, stack);
        }
    }
}
