// SPDX-FileCopyrightText: 2026 5thlayer
// SPDX-License-Identifier: MIT

package io.github._5thlayer.groundworks;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

/** What a cost of several items lacks against what the main inventory holds, by item names in place of items. */
class ChargeTest {

    private static Map<String, Integer> counts(Object... pairs) {
        Map<String, Integer> counts = new LinkedHashMap<>();
        for (int i = 0; i < pairs.length; i += 2) {
            counts.put((String) pairs[i], (Integer) pairs[i + 1]);
        }
        return counts;
    }

    @Test
    void aCostHeldInFullLacksNothing() {
        assertEquals(Map.of(), Charge.missing(counts("iron_block", 4, "carved_pumpkin", 1),
                counts("iron_block", 9, "carved_pumpkin", 1)));
    }

    @Test
    void aCostLacksWhatIsShortOfEachItemAndNothingOfWhatIsHeld() {
        assertEquals(counts("iron_block", 1, "carved_pumpkin", 1),
                Charge.missing(counts("iron_block", 4, "snow_block", 2, "carved_pumpkin", 1),
                        counts("iron_block", 3, "snow_block", 5)));
    }

    @Test
    void whatIsLackingKeepsTheCostsOrder() {
        Map<String, Integer> missing = Charge.missing(counts("prismarine", 16, "sea_lantern", 2, "dark_prismarine", 3),
                counts());
        assertEquals(List.of("prismarine", "sea_lantern", "dark_prismarine"), List.copyOf(missing.keySet()));
    }

    @Test
    void aCostOfNothingLacksNothing() {
        assertEquals(Map.of(), Charge.missing(counts("iron_block", 0), counts()));
    }
}
