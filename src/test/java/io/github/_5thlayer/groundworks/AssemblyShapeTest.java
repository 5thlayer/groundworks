// SPDX-FileCopyrightText: 2026 5thlayer
// SPDX-License-Identifier: MIT

package io.github._5thlayer.groundworks;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.JsonOps;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;

import org.junit.jupiter.api.Test;

class AssemblyShapeTest {

    private static final AssemblyShape.Legend STONE = new AssemblyShape.Legend.Block(Identifier.withDefaultNamespace("stone"));
    private static final AssemblyShape.Legend COBBLE = new AssemblyShape.Legend.Block(Identifier.withDefaultNamespace("cobblestone"));
    private static final AssemblyShape.Legend KEY = new AssemblyShape.Legend.Block(Identifier.withDefaultNamespace("lodestone"));

    /**
     * Seen from above with the player at the bottom of the page looking up: cobblestone forward of
     * the Key's column, stone under the Key and stone to the player's right, the Key on top.
     */
    private static final String TOWER = """
            {
              "key": "K",
              "legend": {"K": "minecraft:lodestone", "s": "minecraft:stone", "c": "minecraft:cobblestone"},
              "layers": [
                ["c ",
                 "ss"],
                ["  ",
                 "K "]
              ]
            }""";

    @Test
    void aDeclarationReadsItsKeyBlockAndStartsOnAnAssemblyUnlessItSaysOtherwise() {
        AssemblyShape shape = parse(TOWER).getOrThrow();
        assertEquals(Identifier.withDefaultNamespace("lodestone"), shape.keyBlock());
        assertFalse(shape.startsAlone());
        assertTrue(parse(TOWER.replace("\"key\": \"K\",", "\"key\": \"K\", \"starts_alone\": true,"))
                .getOrThrow().startsAlone());
    }

    @Test
    void aShapeRestsOnAnUpFaceWithTheKeysColumnOnTheSpotAndTheKeyLast() {
        AssemblyShape shape = parse(TOWER).getOrThrow();
        BlockPos spot = new BlockPos(10, 64, 10);
        // Looking east, so the Key faces west, back at the player: forward is east, the right is south.
        List<AssemblyShape.At> at = shape.positions(spot, Direction.UP, Direction.WEST);
        assertEquals(List.of(
                new AssemblyShape.At(spot.east(), COBBLE),
                new AssemblyShape.At(spot, STONE),
                new AssemblyShape.At(spot.south(), STONE),
                new AssemblyShape.At(spot.above(), KEY)), at);
    }

    @Test
    void theShapeTurnsToFaceThePlayerAsAFootprintDoes() {
        AssemblyShape shape = parse(TOWER).getOrThrow();
        BlockPos spot = BlockPos.ZERO;
        // Looking north: forward is north, the right is east.
        assertEquals(List.of(spot.north(), spot, spot.east(), spot.above()),
                positionsOf(shape.positions(spot, Direction.UP, Direction.SOUTH)));
        // Looking south: forward is south, the right is west.
        assertEquals(List.of(spot.south(), spot, spot.west(), spot.above()),
                positionsOf(shape.positions(spot, Direction.UP, Direction.NORTH)));
        // Looking west: forward is west, the right is north.
        assertEquals(List.of(spot.west(), spot, spot.north(), spot.above()),
                positionsOf(shape.positions(spot, Direction.UP, Direction.EAST)));
    }

    @Test
    void onASideFaceTheBottomLayerStandsOnTheSpotAndUnderAFaceTheTopLayerHangsFromIt() {
        AssemblyShape shape = parse(TOWER).getOrThrow();
        BlockPos spot = BlockPos.ZERO;
        assertEquals(List.of(spot.east(), spot, spot.south(), spot.above()),
                positionsOf(shape.positions(spot, Direction.WEST, Direction.WEST)));
        BlockPos below = spot.below();
        assertEquals(List.of(below.east(), below, below.south(), spot),
                positionsOf(shape.positions(spot, Direction.DOWN, Direction.WEST)));
    }

    @Test
    void aShapeWithNoKeyOrTwoIsRejected() {
        assertError(TOWER.replace("\"K \"]", "\"  \"]"), "no Key");
        assertError(TOWER.replace("\"K \"]", "\"KK\"]"), "2 Keys");
    }

    @Test
    void aLetterTheLegendDoesNotNameIsRejected() {
        assertError(TOWER.replace("\"c \"", "\"x \""), "'x'");
    }

    @Test
    void rowsOfUnequalLengthOrLayersOfUnequalSizeAreRejected() {
        assertError(TOWER.replace("\"c \"", "\"c\""), "rows");
        assertError(TOWER.replace("[\"  \",\n     \"K \"]", "[\"K \"]"), "rows");
    }

    @Test
    void aLegendKeyLongerThanOneLetterOrAKeyLetterTheLegendDoesNotNameIsRejected() {
        assertError(TOWER.replace("\"c\": ", "\"cc\": "), "one letter");
        assertError(TOWER.replace("\"K\": \"minecraft:lodestone\", ", ""), "'K'");
    }

    @Test
    void aDeclarationRoundTripsThroughItsCodec() {
        AssemblyShape shape = parse(TOWER).getOrThrow();
        JsonElement encoded = AssemblyShape.CODEC.encodeStart(JsonOps.INSTANCE, shape).getOrThrow();
        AssemblyShape decoded = AssemblyShape.CODEC.parse(JsonOps.INSTANCE, encoded).getOrThrow();
        assertEquals(shape.positions(BlockPos.ZERO, Direction.UP, Direction.WEST),
                decoded.positions(BlockPos.ZERO, Direction.UP, Direction.WEST));
    }

    /**
     * A base of two letters, each a beacon base block or iron by default, forward of the Key's
     * column a position kept empty, the Key on the base's left block.
     */
    private static final String PLINTH = """
            {
              "key": "K",
              "legend": {
                "K": "minecraft:target",
                "b": {"tag": "minecraft:beacon_base_blocks", "default": "minecraft:iron_block"},
                "o": "empty"
              },
              "layers": [
                ["  ",
                 "bb"],
                ["o ",
                 "K "]
              ]
            }""";

    private static final Identifier IRON = Identifier.withDefaultNamespace("iron_block");
    private static final Identifier GOLD = Identifier.withDefaultNamespace("gold_block");
    private static final Identifier EMERALD = Identifier.withDefaultNamespace("emerald_block");
    private static final Identifier STONE_BLOCK = Identifier.withDefaultNamespace("stone");
    private static final Identifier BASE_TAG = Identifier.withDefaultNamespace("beacon_base_blocks");
    private static final AssemblyShape.Legend BASE = new AssemblyShape.Legend.Tag(BASE_TAG, IRON);

    @Test
    void aLegendLetterNamesABlockATagWithItsDefaultOrAPositionKeptEmpty() {
        AssemblyShape shape = parse(PLINTH).getOrThrow();
        BlockPos spot = BlockPos.ZERO;
        assertEquals(List.of(
                new AssemblyShape.At(spot, BASE),
                new AssemblyShape.At(spot.south(), BASE),
                new AssemblyShape.At(spot.above().east(), AssemblyShape.Legend.Empty.INSTANCE),
                new AssemblyShape.At(spot.above(), new AssemblyShape.Legend.Block(Identifier.withDefaultNamespace("target")))),
                shape.positions(spot, Direction.UP, Direction.WEST));
        assertEquals(Set.of(Identifier.withDefaultNamespace("target"), IRON), Set.copyOf(shape.blocks()));
    }

    @Test
    void aTagWithNoDefaultOrAKeyThatIsATagOrEmptyIsRejected() {
        assertError(PLINTH.replace(", \"default\": \"minecraft:iron_block\"", ""), "default");
        assertError(PLINTH.replace("\"K\": \"minecraft:target\"", "\"K\": \"empty\""), "no block for the key");
        assertError(PLINTH.replace("\"K\": \"minecraft:target\"",
                "\"K\": {\"tag\": \"minecraft:logs\", \"default\": \"minecraft:oak_log\"}"), "no block for the key");
    }

    @Test
    void aDeclarationWithTagsAndEmptiesRoundTripsThroughItsCodec() {
        AssemblyShape shape = parse(PLINTH).getOrThrow();
        JsonElement encoded = AssemblyShape.CODEC.encodeStart(JsonOps.INSTANCE, shape).getOrThrow();
        assertEquals(shape.positions(BlockPos.ZERO, Direction.UP, Direction.WEST),
                AssemblyShape.CODEC.parse(JsonOps.INSTANCE, encoded).getOrThrow()
                        .positions(BlockPos.ZERO, Direction.UP, Direction.WEST));
    }

    private static final Function<Identifier, List<Identifier>> MEMBERS =
            tag -> tag.equals(BASE_TAG) ? List.of(IRON, GOLD, EMERALD) : List.of();

    @Test
    void tagPositionsAreFilledFromTheMemberHeldMostOfFirst() {
        AssemblyShape.Fill fill = AssemblyShape.fill(List.of(BASE, BASE, BASE, BASE),
                Map.of(IRON, 1, GOLD, 3, EMERALD, 2), MEMBERS, false);
        assertEquals(List.of(GOLD, GOLD, GOLD, EMERALD), fill.blocks());
        assertEquals(Map.of(), fill.missing());
    }

    @Test
    void aFillIsUniformWhenOneMemberCoversItAll() {
        AssemblyShape.Fill fill = AssemblyShape.fill(List.of(BASE, BASE, BASE),
                Map.of(IRON, 3, GOLD, 3), MEMBERS, false);
        assertEquals(List.of(IRON, IRON, IRON), fill.blocks());
    }

    @Test
    void namedBlocksAreTakenBeforeTagPositionsShareWhatIsLeft() {
        AssemblyShape.Legend iron = new AssemblyShape.Legend.Block(IRON);
        AssemblyShape.Fill fill = AssemblyShape.fill(List.of(BASE, BASE, iron, iron),
                Map.of(IRON, 3, GOLD, 2), MEMBERS, false);
        assertEquals(List.of(GOLD, GOLD, IRON, IRON), fill.blocks());
        assertEquals(Map.of(), fill.missing());
    }

    @Test
    void whatTheInventoryCannotCoverIsMissingTagPositionsCountedAsTheirDefault() {
        AssemblyShape.Fill fill = AssemblyShape.fill(List.of(BASE, BASE, BASE, STONE, STONE),
                Map.of(GOLD, 1, STONE_BLOCK, 1), MEMBERS, false);
        assertEquals(List.of(GOLD, IRON, IRON, STONE_BLOCK, STONE_BLOCK), fill.blocks());
        assertEquals(Map.of(STONE_BLOCK, 1, IRON, 2), fill.missing());
    }

    @Test
    void whatIsMissingIsInTheOrderTheFillCountsItNamedBlocksFirstThenEachTagsShortfall() {
        AssemblyShape.Fill fill = AssemblyShape.fill(List.of(BASE, STONE, COBBLE, BASE, KEY), Map.of(), MEMBERS, false);
        assertEquals(List.of(STONE_BLOCK, Identifier.withDefaultNamespace("cobblestone"),
                Identifier.withDefaultNamespace("lodestone"), IRON), List.copyOf(fill.missing().keySet()));
    }

    @Test
    void creativeFillsEachTagPositionWithItsDefaultAndMissesNothing() {
        AssemblyShape.Fill fill = AssemblyShape.fill(List.of(BASE, BASE, STONE), Map.of(GOLD, 9), MEMBERS, true);
        assertEquals(List.of(IRON, IRON, STONE_BLOCK), fill.blocks());
        assertEquals(Map.of(), fill.missing());
    }

    private static List<BlockPos> positionsOf(List<AssemblyShape.At> at) {
        return at.stream().map(AssemblyShape.At::pos).toList();
    }

    private static DataResult<AssemblyShape> parse(String json) {
        return AssemblyShape.CODEC.parse(JsonOps.INSTANCE, JsonParser.parseString(json));
    }

    private static void assertError(String json, String naming) {
        DataResult<AssemblyShape> result = parse(json);
        assertTrue(result.isError(), "expected an error naming " + naming + " for " + json);
        String message = result.error().orElseThrow().message();
        assertTrue(message.contains(naming), "the error '" + message + "' doesn't name " + naming);
    }
}
