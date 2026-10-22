// SPDX-FileCopyrightText: 2026 5thlayer
// SPDX-License-Identifier: MIT

package io.github._5thlayer.groundworks;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.jspecify.annotations.Nullable;

/**
 * A pure ledger over an inventory's slots: what a cost lacks, whether a hand-back fits once the
 * cost is taken, and the taking and the giving themselves (ADR 0008). It knows slots as counts, not
 * as game stacks, so it runs on a plain JVM; {@link Charge} and {@link HandBack} are its views over
 * a player's inventory.
 *
 * <p>A cost is taken from the held slot first, then from the others in order. What comes back goes
 * into the held slot when taking freed it, then onto a stack of its own kind, then into the first
 * empty slot of the main slots. The slots after the main ones, such as an off hand, take a stack of
 * its own kind but no empty fill.
 *
 * @param <I> what a cost counts, an item
 */
final class Ledger<I> {

    /**
     * One slot or one returned stack: {@code count} of {@code item}, of the stacking {@code kind}
     * (equal kinds stack), which holds up to {@code max}. An empty slot has a count of 0.
     */
    record Stack<I>(@Nullable I item, @Nullable Object kind, int count, int max) {

        static <I> Stack<I> empty() {
            return new Stack<>(null, null, 0, 0);
        }

        boolean isEmpty() {
            return count <= 0;
        }

        Stack<I> withCount(int count) {
            return count <= 0 ? empty() : new Stack<>(item, kind, count, max);
        }
    }

    private final List<Stack<I>> slots;
    private final int held;
    private final int main;

    /**
     * Over {@code slots}, the {@code held} one the cost is taken from first and freed for what comes
     * back, the first {@code main} of them taking an empty fill.
     */
    Ledger(List<Stack<I>> slots, int held, int main) {
        this.slots = new ArrayList<>(slots);
        this.held = held;
        this.main = main;
    }

    /** The count in slot {@code index}. */
    int count(int index) {
        return slots.get(index).count();
    }

    /** How many of each of the cost's items the slots lack, in the cost's order: none when they hold it. */
    Map<I, Integer> missing(Map<I, Integer> cost) {
        Map<I, Integer> have = new LinkedHashMap<>();
        for (Stack<I> slot : slots) {
            if (!slot.isEmpty() && cost.containsKey(slot.item())) {
                have.merge(slot.item(), slot.count(), Integer::sum);
            }
        }
        Map<I, Integer> missing = new LinkedHashMap<>();
        cost.forEach((item, count) -> {
            int lacking = count - have.getOrDefault(item, 0);
            if (lacking > 0) {
                missing.put(item, lacking);
            }
        });
        return missing;
    }

    /**
     * Takes {@code cost}, the held slot first, and answers whether that took the held stack's last
     * item, freeing its slot for what comes back.
     */
    boolean take(Map<I, Integer> cost) {
        boolean heldBefore = !slots.get(held).isEmpty();
        cost.forEach((item, count) -> {
            int toTake = count - takeFrom(held, item, count);
            for (int i = 0; i < slots.size() && toTake > 0; i++) {
                toTake -= takeFrom(i, item, toTake);
            }
        });
        return heldBefore && slots.get(held).isEmpty();
    }

    private int takeFrom(int index, I item, int toTake) {
        Stack<I> slot = slots.get(index);
        if (slot.isEmpty() || !slot.item().equals(item)) {
            return 0;
        }
        int taken = Math.min(toTake, slot.count());
        slots.set(index, slot.withCount(slot.count() - taken));
        return taken;
    }

    /**
     * Whether {@code returned} fits once {@code cost} is taken, without changing the ledger: proved
     * on a copy.
     */
    boolean fits(Map<I, Integer> cost, List<Stack<I>> returned) {
        if (returned.isEmpty()) {
            return true;
        }
        Ledger<I> copy = new Ledger<>(slots, held, main);
        boolean freed = copy.take(cost);
        return copy.give(freed, returned);
    }

    /** Puts {@code returned} back, into the held slot first when {@code freed}, and answers whether all of it fit. */
    boolean give(boolean freed, List<Stack<I>> returned) {
        for (Stack<I> back : returned) {
            int rest = back.count();
            if (freed && slots.get(held).isEmpty() && rest > 0) {
                int moved = Math.min(rest, back.max());
                slots.set(held, back.withCount(moved));
                rest -= moved;
            }
            for (int i = 0; i < slots.size() && rest > 0; i++) {
                Stack<I> slot = slots.get(i);
                if (!slot.isEmpty() && slot.kind().equals(back.kind())) {
                    int moved = Math.max(0, Math.min(rest, slot.max() - slot.count()));
                    slots.set(i, slot.withCount(slot.count() + moved));
                    rest -= moved;
                }
            }
            for (int i = 0; i < main && rest > 0; i++) {
                if (slots.get(i).isEmpty()) {
                    int moved = Math.min(rest, back.max());
                    slots.set(i, back.withCount(moved));
                    rest -= moved;
                }
            }
            if (rest > 0) {
                return false;
            }
        }
        return true;
    }
}
