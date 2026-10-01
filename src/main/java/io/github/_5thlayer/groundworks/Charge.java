// SPDX-FileCopyrightText: 2026 5thlayer
// SPDX-License-Identifier: MIT

package io.github._5thlayer.groundworks;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Inventory;
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
 * from the main inventory, the held stack first. What comes back goes into the slot the charge
 * freed, the held slot when the charge took its last item, and otherwise where the game puts an
 * item back: onto a stack of its own, then into the first empty slot. {@link #fits} proves that
 * in the main inventory alone, though the game would also fill a stack in the offhand, so it may
 * refuse what would fit but never passes what would drop.
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
        if (returned.isEmpty()) {
            return true;
        }
        Inventory inventory = player.getInventory();
        List<ItemStack> slots = new ArrayList<>();
        for (ItemStack slot : inventory.getNonEquipmentItems()) {
            slots.add(slot.copy());
        }
        int held = inventory.getSelectedSlot();
        boolean freed = take(slots, held);
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
            for (int i = 0; i < slots.size() && !rest.isEmpty(); i++) {
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
        Inventory inventory = player.getInventory();
        int held = inventory.getSelectedSlot();
        boolean freed = take(inventory.getNonEquipmentItems(), held);
        for (PlacementPlan.Placed placed : plan.blocks()) {
            level.setBlock(placed.pos(), placed.state(), Block.UPDATE_ALL);
        }
        for (ItemStack back : returned) {
            if (freed && inventory.getItem(held).isEmpty()) {
                inventory.setItem(held, back.copy());
            } else {
                inventory.placeItemBackInInventory(back.copy());
            }
        }
        if (plan.blocks().isEmpty()) {
            return;
        }
        PlacementPlan.Placed first = plan.blocks().getFirst();
        SoundType sound = first.state().getSoundType();
        level.playSound(null, first.pos(), sound.getPlaceSound(), SoundSource.BLOCKS,
                (sound.getVolume() + 1.0F) / 2.0F, sound.getPitch() * 0.8F);
        level.gameEvent(GameEvent.BLOCK_PLACE, first.pos(), GameEvent.Context.of(player, first.state()));
    }

    /**
     * Takes the cost from these slots, the held one first, and answers whether that took the held
     * slot's last item, freeing it for what comes back.
     */
    private boolean take(List<ItemStack> slots, int held) {
        boolean wasHeld = slots.get(held).is(item);
        int toTake = cost - takeFrom(slots.get(held), cost);
        for (ItemStack slot : slots) {
            if (toTake == 0) {
                break;
            }
            toTake -= takeFrom(slot, toTake);
        }
        return wasHeld && slots.get(held).isEmpty();
    }

    private int takeFrom(ItemStack slot, int toTake) {
        if (!slot.is(item)) {
            return 0;
        }
        int taken = Math.min(toTake, slot.getCount());
        slot.shrink(taken);
        return taken;
    }
}
