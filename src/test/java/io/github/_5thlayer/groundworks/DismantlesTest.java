// SPDX-FileCopyrightText: 2026 5thlayer
// SPDX-License-Identifier: MIT

package io.github._5thlayer.groundworks;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;

import io.github._5thlayer.groundworks.Dismantles.Click;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.junit.jupiter.api.Test;

/**
 * The Dismantle's rules that read no world: which family a block belongs to, when a stored start
 * is live, what a click does, and when a span is refused before its family is asked. Its states
 * are null and its families answer by rule, since none of these rules reads a state itself.
 */
class DismantlesTest {

    private static final ResourceKey<Level> OVERWORLD = dimension("overworld");
    private static final ResourceKey<Level> NETHER = dimension("the_nether");
    private static final BlockPos START = new BlockPos(0, 1, 0);
    private static final BlockPos END = new BlockPos(3, 1, 0);

    private enum FamilyRefusal implements Refusal {
        OFF_LINE
    }

    /** A family that claims by a fixed answer and records whether its span was asked for. */
    private static final class Family implements DismantleFamily {

        private final boolean claims;
        private final boolean same;
        private final DismantleSpan span;
        private final List<String> asked = new ArrayList<>();

        Family(boolean claims, boolean same, DismantleSpan span) {
            this.claims = claims;
            this.same = same;
            this.span = span;
        }

        Family(boolean claims) {
            this(claims, true, DismantleSpan.taking(List.of(START, END)));
        }

        @Override
        public boolean claims(BlockState state) {
            return claims;
        }

        @Override
        public DismantleSpan span(Level level, BlockPos start, BlockPos end) {
            asked.add("span");
            return span;
        }

        @Override
        public Component message(Refusal refusal) {
            return Component.literal("family's own");
        }

        @Override
        public boolean isSameStart(BlockState stored, BlockState now) {
            return same;
        }
    }

    private static ResourceKey<Level> dimension(String name) {
        return ResourceKey.create(Registries.DIMENSION, Identifier.withDefaultNamespace(name));
    }

    private static DismantleStart startIn(ResourceKey<Level> dimension) {
        return new DismantleStart(dimension, START, null);
    }

    @Test
    void aBlockBelongsToTheFirstFamilyThatClaimsIt() {
        Family first = new Family(true);
        Family second = new Family(true);
        assertSame(first, Dismantles.familyOf(List.of(new Family(false), first, second), null));
    }

    @Test
    void aBlockNoFamilyClaimsBelongsToNone() {
        assertNull(Dismantles.familyOf(List.of(new Family(false)), null));
        assertNull(Dismantles.familyOf(List.of(), null));
    }

    @Test
    void aStartIsLiveWhileItsFamilyCountsItTheSameStart() {
        assertTrue(Dismantles.isLive(startIn(OVERWORLD), new Family(true, true, null), OVERWORLD, pos -> null));
    }

    @Test
    void aStartItsFamilyNoLongerCountsTheSameIsDead() {
        assertFalse(Dismantles.isLive(startIn(OVERWORLD), new Family(true, false, null), OVERWORLD, pos -> null));
    }

    @Test
    void aStartWithNoFamilyIsDead() {
        assertFalse(Dismantles.isLive(startIn(OVERWORLD), null, OVERWORLD, pos -> null));
    }

    @Test
    void aStartInAnotherDimensionIsDeadWithoutReadingTheWorld() {
        List<BlockPos> read = new ArrayList<>();
        assertFalse(Dismantles.isLive(startIn(NETHER), new Family(true), OVERWORLD, pos -> {
            read.add(pos);
            return null;
        }));
        assertTrue(read.isEmpty());
    }

    @Test
    void anEndOutsideTheStartsFamilyIsNotTheSameKindAndTheFamilyIsNotAsked() {
        Family family = new Family(false);
        DismantleSpan span = Dismantles.span(family, null, START, END, null);
        assertSame(Refusal.Dismantle.NOT_SAME_KIND, span.refusal());
        assertTrue(family.asked.isEmpty());
    }

    @Test
    void anEndInTheStartsFamilyGetsTheFamilysSpan() {
        DismantleSpan refused = DismantleSpan.refused(FamilyRefusal.OFF_LINE);
        Family family = new Family(true, true, refused);
        assertSame(refused, Dismantles.span(family, null, START, END, null));
        assertEquals(List.of("span"), family.asked);
    }

    /** Named, so a splitter at the end of a belt's span reads as "not a Belt Tile". */
    @Test
    void notTheSameKindNamesTheStartsBlock() {
        Component start = Component.literal("Belt Tile");
        Component told = Dismantles.message(new Family(true), start, Refusal.Dismantle.NOT_SAME_KIND);
        assertEquals(Component.translatable("message.groundworks.dismantle_not_same_kind", start), told);
    }

    @Test
    void aFamilysRefusalIsToldInTheFamilysWords() {
        assertEquals(Component.literal("family's own"),
                Dismantles.message(new Family(true), Component.literal("Belt Tile"), FamilyRefusal.OFF_LINE));
    }

    @Test
    void aSneakClickOnAMemberStoresAStartWhetherOrNotOneIsStored() {
        assertEquals(Click.STORE, Click.of(true, true, false));
        assertEquals(Click.STORE, Click.of(true, true, true));
    }

    @Test
    void aSneakClickOnAnythingElsePassesOn() {
        assertEquals(Click.PASS, Click.of(true, false, false));
        assertEquals(Click.PASS, Click.of(true, false, true));
    }

    @Test
    void aClickWithALiveStartTakesUpWhateverItAims() {
        assertEquals(Click.TAKE_UP, Click.of(false, true, true));
        assertEquals(Click.TAKE_UP, Click.of(false, false, true));
    }

    @Test
    void aClickWithNoLiveStartPassesOn() {
        assertEquals(Click.PASS, Click.of(false, true, false));
        assertEquals(Click.PASS, Click.of(false, false, false));
    }
}
