// SPDX-FileCopyrightText: 2026 5thlayer
// SPDX-License-Identifier: MIT

package io.github._5thlayer.groundworks;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.function.BooleanSupplier;

import org.junit.jupiter.api.Test;

/**
 * The one order a laying refuses in, for every click that lays: may-not-build, whatever else
 * refuses; then the plan's own reason; then the items lacking; then no room for what comes back.
 */
class LayingTest {

    private static final Laying.Reasons STRETCH = new Laying.Reasons(Refusal.Stretch.MAY_NOT_BUILD,
            Refusal.Stretch.NOT_ENOUGH_ITEMS, Refusal.Stretch.NO_ROOM_TO_RETURN);
    private static final Laying.Reasons FAST_REPLACE = new Laying.Reasons(Refusal.FastReplace.MAY_NOT_BUILD, null,
            Refusal.FastReplace.NO_ROOM_TO_RETURN);
    private static final Laying.Reasons ASSEMBLY = new Laying.Reasons(Refusal.Assembly.MAY_NOT_BUILD,
            Refusal.Assembly.NOT_ENOUGH_ITEMS, null);

    private static final BooleanSupplier YES = () -> true;
    private static final BooleanSupplier NO = () -> false;

    /** A check the order must not reach. */
    private static final BooleanSupplier UNASKED = () -> {
        throw new AssertionError("asked a check past the refusal");
    };

    @Test
    void mayNotBuildRefusesWhateverElseRefuses() {
        assertEquals(Refusal.Assembly.MAY_NOT_BUILD,
                Laying.refusal(false, Refusal.Assembly.TAKEN, UNASKED, UNASKED, ASSEMBLY));
        assertEquals(Refusal.Stretch.MAY_NOT_BUILD,
                Laying.refusal(false, null, UNASKED, UNASKED, STRETCH));
    }

    @Test
    void thePlansOwnReasonComesBeforeWhatIsLackingAndRoom() {
        assertEquals(Refusal.Stretch.BEHIND_THE_LOOK,
                Laying.refusal(true, Refusal.Stretch.BEHIND_THE_LOOK, UNASKED, UNASKED, STRETCH));
    }

    @Test
    void whatIsLackingComesBeforeRoom() {
        assertEquals(Refusal.Stretch.NOT_ENOUGH_ITEMS, Laying.refusal(true, null, NO, UNASKED, STRETCH));
    }

    @Test
    void noRoomRefusesAPlanThePlayerAffords() {
        assertEquals(Refusal.Stretch.NO_ROOM_TO_RETURN, Laying.refusal(true, null, YES, NO, STRETCH));
    }

    @Test
    void aFeatureWithNoReasonForACheckIsNeverRefusedForIt() {
        assertEquals(Refusal.FastReplace.NO_ROOM_TO_RETURN, Laying.refusal(true, null, UNASKED, NO, FAST_REPLACE));
        assertNull(Laying.refusal(true, null, YES, UNASKED, ASSEMBLY));
    }

    @Test
    void aPlanNothingRefusesGoesThrough() {
        assertNull(Laying.refusal(true, null, YES, YES, STRETCH));
    }
}
