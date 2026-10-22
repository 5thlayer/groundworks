// SPDX-FileCopyrightText: 2026 5thlayer
// SPDX-License-Identifier: MIT

package io.github._5thlayer.groundworks;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import io.github._5thlayer.groundworks.Ledger.Stack;

/** The slot rules of a cost and a hand-back, on item names in place of items, stacks of 64 unless said. */
class LedgerTest {

    private static Stack<String> stack(String item, int count) {
        return new Stack<>(item, item, count, 64);
    }

    private static Stack<String> empty() {
        return Stack.empty();
    }

    private static Map<String, Integer> counts(Object... pairs) {
        Map<String, Integer> counts = new LinkedHashMap<>();
        for (int i = 0; i < pairs.length; i += 2) {
            counts.put((String) pairs[i], (Integer) pairs[i + 1]);
        }
        return counts;
    }

    private static Ledger<String> ledger(int held, int main, Stack<String>... slots) {
        return new Ledger<>(List.of(slots), held, main);
    }

    @SafeVarargs
    private static List<Stack<String>> back(Stack<String>... stacks) {
        return new ArrayList<>(List.of(stacks));
    }

    @Test
    void aCostHeldInFullLacksNothing() {
        assertEquals(Map.of(), ledger(0, 2, stack("iron", 3), stack("iron", 6))
                .missing(counts("iron", 9)));
    }

    @Test
    void aCostLacksWhatIsShortOfEachItemInItsOrder() {
        Map<String, Integer> missing = ledger(0, 2, stack("iron", 3), stack("snow", 5))
                .missing(counts("iron", 4, "snow", 2, "pumpkin", 1));
        assertEquals(counts("iron", 1, "pumpkin", 1), missing);
        assertEquals(List.of("iron", "pumpkin"), List.copyOf(missing.keySet()));
    }

    @Test
    void aCostOfNothingLacksNothing() {
        assertEquals(Map.of(), ledger(0, 1, empty()).missing(counts("iron", 0)));
    }

    @Test
    void theHeldSlotIsTakenFromFirst() {
        Ledger<String> ledger = ledger(1, 2, stack("iron", 5), stack("iron", 5));
        assertFalse(ledger.take(counts("iron", 3)));
        assertEquals(5, ledger.count(0));
        assertEquals(2, ledger.count(1));
    }

    @Test
    void aCostOverrunsTheHeldSlotIntoTheOthers() {
        Ledger<String> ledger = ledger(1, 2, stack("iron", 5), stack("iron", 2));
        assertTrue(ledger.take(counts("iron", 4)));
        assertEquals(3, ledger.count(0));
        assertEquals(0, ledger.count(1));
    }

    @Test
    void takingTheHeldStacksLastItemFreesItsSlot() {
        assertTrue(ledger(0, 1, stack("iron", 1)).take(counts("iron", 1)));
    }

    @Test
    void takingFromAnotherSlotDoesNotFreeTheHeldOne() {
        assertFalse(ledger(0, 2, stack("snow", 1), stack("iron", 1)).take(counts("iron", 1)));
    }

    @Test
    void anEmptyHeldSlotIsNotFreed() {
        assertFalse(ledger(0, 2, empty(), stack("iron", 1)).take(counts("iron", 1)));
    }

    @Test
    void aReturnFitsInTheHeldSlotTheCostFreed() {
        Ledger<String> ledger = ledger(0, 1, stack("iron", 1));
        assertTrue(ledger.fits(counts("iron", 1), back(stack("lime", 3))));
    }

    @Test
    void aReturnDoesNotFitInAFullInventoryWhenTheCostFreedNothing() {
        Ledger<String> ledger = ledger(0, 2, stack("iron", 5), stack("snow", 64));
        assertFalse(ledger.fits(counts("iron", 1), back(stack("lime", 3))));
    }

    @Test
    void aReturnStacksOntoItsOwnKindBeforeAnEmptySlot() {
        Ledger<String> ledger = ledger(0, 2, stack("iron", 5), stack("lime", 60));
        assertTrue(ledger.fits(counts("iron", 1), back(stack("lime", 4))));
        assertFalse(ledger.fits(counts("iron", 1), back(stack("lime", 5))));
    }

    @Test
    void aReturnSpillsOverIntoTheFirstEmptyMainSlot() {
        Ledger<String> ledger = ledger(0, 3, stack("iron", 5), stack("lime", 60), empty());
        assertTrue(ledger.fits(counts("iron", 1), back(stack("lime", 10))));
    }

    @Test
    void theSlotsAfterTheMainOnesTakeNoEmptyFill() {
        Ledger<String> ledger = ledger(0, 1, stack("iron", 5), empty());
        assertFalse(ledger.fits(counts("iron", 1), back(stack("lime", 1))));
    }

    @Test
    void theSlotsAfterTheMainOnesTakeAStackOfTheirOwnKind() {
        Ledger<String> ledger = ledger(0, 1, stack("iron", 5), stack("lime", 1));
        assertTrue(ledger.fits(counts("iron", 1), back(stack("lime", 1))));
    }

    @Test
    void anOffHandHeldSlotIsFreedFirstToo() {
        Ledger<String> ledger = ledger(1, 1, stack("snow", 64), stack("iron", 1));
        assertTrue(ledger.fits(counts("iron", 1), back(stack("lime", 1))));
    }

    @Test
    void nothingToReturnAlwaysFits() {
        assertTrue(ledger(0, 1, stack("iron", 1)).fits(counts("iron", 1), List.of()));
    }

    @Test
    void severalReturnedStacksShareTheRoom() {
        Ledger<String> ledger = ledger(0, 2, stack("iron", 5), stack("snow", 64));
        assertFalse(ledger.fits(counts("iron", 5), back(stack("lime", 1), stack("sand", 1))));
        assertTrue(ledger.fits(counts("iron", 5), back(stack("lime", 1))));
    }

    @Test
    void fitsChangesNothing() {
        Ledger<String> ledger = ledger(0, 1, stack("iron", 2));
        ledger.fits(counts("iron", 2), back(stack("lime", 1)));
        assertEquals(2, ledger.count(0));
    }

    @Test
    void aGivenStackLandsInTheFreedSlot() {
        Ledger<String> ledger = ledger(0, 2, stack("iron", 1), stack("snow", 64));
        boolean freed = ledger.take(counts("iron", 1));
        assertTrue(ledger.give(freed, back(stack("lime", 2))));
        assertEquals(2, ledger.count(0));
    }

    @Test
    void aSecondReturnedStackOfTheFreedKindStacksOntoTheHeldSlot() {
        Ledger<String> ledger = ledger(0, 1, stack("iron", 1));
        assertTrue(ledger.fits(counts("iron", 1), back(stack("lime", 60), stack("lime", 4))));
        assertFalse(ledger.fits(counts("iron", 1), back(stack("lime", 60), stack("lime", 5))));
    }

    @Test
    void aStackOfAnotherKindOfTheSameItemDoesNotStack() {
        Ledger<String> full = ledger(0, 1, stack("iron", 5), new Stack<>("lime", "tagged", 1, 64));
        assertFalse(full.fits(counts("iron", 1), back(stack("lime", 1))));
    }
}
