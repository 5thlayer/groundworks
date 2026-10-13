// SPDX-FileCopyrightText: 2026 5thlayer
// SPDX-License-Identifier: MIT

package io.github._5thlayer.groundworks;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

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
