// SPDX-FileCopyrightText: 2026 5thlayer
// SPDX-License-Identifier: MIT

package io.github._5thlayer.groundworks;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Test;

/**
 * Next Choice over a Key with two Assemblies, choices 0 and 1, and the Key alone, choice 2: a
 * stack with no choice stored is on its starting choice, and one stepped back to it stores none.
 */
class AssemblyChoiceTest {

    private static final int CHOICES = 3;
    private static final int FIRST = 0;
    private static final int ALONE = 2;

    @Test
    void aStackWithNoChoiceIsOnItsStart() {
        assertEquals(FIRST, AssemblyChoice.current(null, FIRST, CHOICES));
        assertEquals(ALONE, AssemblyChoice.current(null, ALONE, CHOICES));
    }

    @Test
    void nextStepsThroughEveryChoiceAndTheKeyAloneAndWraps() {
        AssemblyChoice second = AssemblyChoice.next(null, FIRST, CHOICES, false);
        assertEquals(new AssemblyChoice(1), second);
        AssemblyChoice alone = AssemblyChoice.next(second, FIRST, CHOICES, false);
        assertEquals(new AssemblyChoice(ALONE), alone);
        assertNull(AssemblyChoice.next(alone, FIRST, CHOICES, false), "back on the start stores no choice");
    }

    @Test
    void reverseStepsBack() {
        AssemblyChoice alone = AssemblyChoice.next(null, FIRST, CHOICES, true);
        assertEquals(new AssemblyChoice(ALONE), alone);
        assertEquals(new AssemblyChoice(1), AssemblyChoice.next(alone, FIRST, CHOICES, true));
    }

    @Test
    void aStackStartingAloneStepsToTheFirstAssembly() {
        assertEquals(new AssemblyChoice(FIRST), AssemblyChoice.next(null, ALONE, CHOICES, false));
    }

    @Test
    void aChoicePastTheKeysChoicesAfterAReloadWraps() {
        assertEquals(1, AssemblyChoice.current(new AssemblyChoice(4), FIRST, CHOICES));
        assertEquals(new AssemblyChoice(ALONE), AssemblyChoice.next(new AssemblyChoice(4), FIRST, CHOICES, false));
    }
}
