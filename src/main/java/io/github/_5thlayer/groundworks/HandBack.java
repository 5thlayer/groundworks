// SPDX-FileCopyrightText: 2026 5thlayer
// SPDX-License-Identifier: MIT

package io.github._5thlayer.groundworks;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Inventory;
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
        return fits(player, hand, player.getItemInHand(hand).getItem(), 1, returned);
    }

    /**
     * Charges one item of the stack in {@code hand} and hands {@code returned} back, after {@link
     * #fits} said it fits.
     */
    public static void charge(@Nullable Player player, InteractionHand hand, List<ItemStack> returned) {
        if (player == null || player.hasInfiniteMaterials()) {
            return;
        }
        boolean freed = take(player, hand, player.getItemInHand(hand).getItem(), 1);
        give(player, hand, freed, returned);
    }

    /** Whether {@code returned} fits once {@code cost} of {@code item} is {@linkplain #take taken}. */
    static boolean fits(Player player, InteractionHand hand, Item item, int cost, List<ItemStack> returned) {
        if (returned.isEmpty()) {
            return true;
        }
        Inventory inventory = player.getInventory();
        List<ItemStack> slots = new ArrayList<>();
        for (ItemStack slot : inventory.getNonEquipmentItems()) {
            slots.add(slot.copy());
        }
        int held = heldSlot(player, hand, slots, player.getItemInHand(hand).copy());
        boolean freed = take(slots, held, item, cost);
        for (ItemStack back : returned) {
            ItemStack rest = back.copy();
            if (freed && slots.get(held).isEmpty()) {
                slots.set(held, rest.split(rest.getMaxStackSize()));
            }
            for (ItemStack slot : slots) {
                if (!slot.isEmpty() && ItemStack.isSameItemSameComponents(slot, rest)) {
                    int moved = Math.max(0, Math.min(rest.getCount(), slot.getMaxStackSize() - slot.getCount()));
                    slot.grow(moved);
                    rest.shrink(moved);
                }
            }
            for (int i = 0; i < inventory.getNonEquipmentItems().size() && !rest.isEmpty(); i++) {
                if (slots.get(i).isEmpty()) {
                    slots.set(i, rest.split(rest.getMaxStackSize()));
                }
            }
            if (!rest.isEmpty()) {
                return false;
            }
        }
        return true;
    }

    /**
     * Takes {@code cost} of {@code item} from the main inventory, the stack in {@code hand} first,
     * and answers whether that took the held stack's last item, freeing its slot for what comes back.
     */
    static boolean take(Player player, InteractionHand hand, Item item, int cost) {
        // The stacks themselves, so taking from them takes from the inventory.
        List<ItemStack> slots = new ArrayList<>(player.getInventory().getNonEquipmentItems());
        return take(slots, heldSlot(player, hand, slots, player.getItemInHand(hand)), item, cost);
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

    /** Puts {@code stack} in {@code hand}, the main hand's as the inventory's own slot, as a placement refills it. */
    private static void putInHand(Player player, InteractionHand hand, ItemStack stack) {
        if (hand == InteractionHand.MAIN_HAND) {
            player.getInventory().setItem(player.getInventory().getSelectedSlot(), stack);
        } else {
            player.setItemInHand(hand, stack);
        }
    }

    /**
     * The index of the stack in {@code hand} among the main inventory's {@code slots}: the selected
     * slot, or the off hand's, which is added after them as {@code offhand}.
     */
    private static int heldSlot(Player player, InteractionHand hand, List<ItemStack> slots, ItemStack offhand) {
        if (hand == InteractionHand.MAIN_HAND) {
            return player.getInventory().getSelectedSlot();
        }
        slots.add(offhand);
        return slots.size() - 1;
    }

    private static boolean take(List<ItemStack> slots, int held, Item item, int cost) {
        boolean wasHeld = slots.get(held).is(item);
        int toTake = cost - takeFrom(slots.get(held), item, cost);
        for (ItemStack slot : slots) {
            if (toTake == 0) {
                break;
            }
            toTake -= takeFrom(slot, item, toTake);
        }
        return wasHeld && slots.get(held).isEmpty();
    }

    private static int takeFrom(ItemStack slot, Item item, int toTake) {
        if (!slot.is(item)) {
            return 0;
        }
        int taken = Math.min(toTake, slot.getCount());
        slot.shrink(taken);
        return taken;
    }
}
