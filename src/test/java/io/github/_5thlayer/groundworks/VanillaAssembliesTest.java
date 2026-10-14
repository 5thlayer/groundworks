// SPDX-FileCopyrightText: 2026 5thlayer
// SPDX-License-Identifier: MIT

package io.github._5thlayer.groundworks;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;

import org.junit.jupiter.api.Test;

/**
 * The Assemblies Groundworks ships for vanilla, against the shapes vanilla's own code checks:
 * {@code BeaconBlockEntity.updateBase}, {@code ConduitBlockEntity.updateShape}, the golem patterns
 * of {@code CarvedPumpkinBlock} and {@code PortalShape}. Offsets are from the Key, placed on an up
 * face by a player looking east, so forward is east and the player's right is south.
 */
class VanillaAssembliesTest {

    private static final Identifier BEACON_BASE = Identifier.withDefaultNamespace("beacon_base_blocks");
    private static final Identifier CONDUIT_FRAME = Identifier.fromNamespaceAndPath("groundworks", "conduit_frame_blocks");
    private static final BlockPos SPOT = new BlockPos(0, 64, 0);

    @Test
    void aBeaconPyramidIsEveryLayerABeaconChecksUpToItsLevel() {
        for (int level = 1; level <= 4; level++) {
            beaconPyramid(level);
        }
    }

    private static void beaconPyramid(int level) {
        Map<BlockPos, AssemblyShape.Legend> shape = shape("beacon/level_" + level, Identifier.withDefaultNamespace("beacon"));
        Set<BlockPos> base = new HashSet<>();
        for (int step = 1; step <= level; step++) {
            for (int x = -step; x <= step; x++) {
                for (int z = -step; z <= step; z++) {
                    base.add(new BlockPos(x, -step, z));
                }
            }
        }
        assertEquals(base, offsets(shape, legend -> isTag(legend, BEACON_BASE, "iron_block")));
        assertEquals(base.size() + 1, shape.size());
        assertEquals(List.of(9, 34, 83, 164).get(level - 1), base.size());
    }

    @Test
    void theFullConduitFrameIsEveryPositionAConduitCountsWithItsInsideEmpty() {
        Map<BlockPos, AssemblyShape.Legend> shape = shape("conduit/frame_42", Identifier.withDefaultNamespace("conduit"));
        Set<BlockPos> frame = new HashSet<>();
        Set<BlockPos> inside = new HashSet<>();
        for (int x = -2; x <= 2; x++) {
            for (int y = -2; y <= 2; y++) {
                for (int z = -2; z <= 2; z++) {
                    int ax = Math.abs(x), ay = Math.abs(y), az = Math.abs(z);
                    if ((ax > 1 || ay > 1 || az > 1)
                            && (x == 0 && (ay == 2 || az == 2) || y == 0 && (ax == 2 || az == 2) || z == 0 && (ax == 2 || ay == 2))) {
                        frame.add(new BlockPos(x, y, z));
                    } else if (ax <= 1 && ay <= 1 && az <= 1 && (x != 0 || y != 0 || z != 0)) {
                        inside.add(new BlockPos(x, y, z));
                    }
                }
            }
        }
        assertEquals(42, frame.size());
        assertEquals(frame, offsets(shape, legend -> isTag(legend, CONDUIT_FRAME, "prismarine")));
        assertEquals(inside, offsets(shape, legend -> legend instanceof AssemblyShape.Legend.Empty));
        assertEquals(42 + 26 + 1, shape.size());
    }

    /** The ring stands upright across the player's view, so the conduit's water below it isn't ground. */
    @Test
    void theConduitRingIsSixteenFrameBlocksUprightRoundItsEmptyInside() {
        Map<BlockPos, AssemblyShape.Legend> shape = shape("conduit/frame_16", Identifier.withDefaultNamespace("conduit"));
        Set<BlockPos> ring = new HashSet<>();
        Set<BlockPos> inside = new HashSet<>();
        for (int y = -2; y <= 2; y++) {
            for (int z = -2; z <= 2; z++) {
                if (Math.abs(y) == 2 || Math.abs(z) == 2) {
                    ring.add(new BlockPos(0, y, z));
                } else if (y != 0 || z != 0) {
                    inside.add(new BlockPos(0, y, z));
                }
            }
        }
        assertEquals(16, ring.size());
        assertEquals(ring, offsets(shape, legend -> isTag(legend, CONDUIT_FRAME, "prismarine")));
        assertEquals(inside, offsets(shape, legend -> legend instanceof AssemblyShape.Legend.Empty));
        assertEquals(16 + 8 + 1, shape.size());
    }

    /** Vanilla's {@code aisle("~^~", "###", "~#~")}, arms across the player's view, every {@code ~} air. */
    @Test
    void anIronGolemIsFourIronBlocksUnderItsPumpkinWithTheSpacesBesideItsHeadAndLegEmpty() {
        Map<BlockPos, AssemblyShape.Legend> shape = shape("golem/iron", Identifier.withDefaultNamespace("carved_pumpkin"));
        assertEquals(Set.of(new BlockPos(0, -1, 0), new BlockPos(0, -1, -1), new BlockPos(0, -1, 1), new BlockPos(0, -2, 0)),
                offsets(shape, legend -> isBlock(legend, "iron_block")));
        assertEquals(Set.of(new BlockPos(0, 0, -1), new BlockPos(0, 0, 1), new BlockPos(0, -2, -1), new BlockPos(0, -2, 1)),
                offsets(shape, legend -> legend instanceof AssemblyShape.Legend.Empty));
        assertEquals(9, shape.size());
    }

    @Test
    void aSnowGolemIsTwoSnowBlocksUnderItsPumpkin() {
        Map<BlockPos, AssemblyShape.Legend> shape = shape("golem/snow", Identifier.withDefaultNamespace("carved_pumpkin"));
        assertEquals(Set.of(new BlockPos(0, -1, 0), new BlockPos(0, -2, 0)), offsets(shape, legend -> isBlock(legend, "snow_block")));
        assertEquals(3, shape.size());
    }

    /** A 2×3 inside, the smallest {@code PortalShape} lights, framed by ten obsidian with no corners. */
    @Test
    void aNetherPortalFrameIsTenObsidianWithoutCornersRoundATwoByThreeEmptyInsideAndStartsAlone() {
        AssemblyShape declared = read("nether_portal");
        assertTrue(declared.startsAlone());
        Map<BlockPos, AssemblyShape.Legend> shape = shape("nether_portal", Identifier.withDefaultNamespace("obsidian"));
        Set<BlockPos> frame = new HashSet<>();
        Set<BlockPos> inside = new HashSet<>();
        for (int y = 0; y <= 4; y++) {
            for (int z = -1; z <= 2; z++) {
                boolean side = z == -1 || z == 2;
                boolean end = y == 0 || y == 4;
                if (side && !end || end && !side) {
                    frame.add(new BlockPos(0, y, z));
                } else if (!side) {
                    inside.add(new BlockPos(0, y, z));
                }
            }
        }
        assertEquals(10, frame.size());
        assertEquals(frame, offsets(shape, legend -> isBlock(legend, "obsidian")));
        assertEquals(inside, offsets(shape, legend -> legend instanceof AssemblyShape.Legend.Empty));
        assertEquals(16, shape.size());
        assertFalse(read("beacon/level_1").startsAlone());
    }

    /** The shape placed at {@link #SPOT}, each position by its offset from the Key, which is checked to be this block. */
    private static Map<BlockPos, AssemblyShape.Legend> shape(String path, Identifier key) {
        AssemblyShape shape = read(path);
        assertEquals(key, shape.keyBlock());
        List<AssemblyShape.At> positions = shape.positions(SPOT, Direction.UP, Direction.WEST);
        BlockPos keyPos = positions.getLast().pos();
        assertEquals(new AssemblyShape.Legend.Block(key), positions.getLast().legend());
        return positions.stream().collect(Collectors.toMap(at -> at.pos().subtract(keyPos), AssemblyShape.At::legend));
    }

    private static Set<BlockPos> offsets(Map<BlockPos, AssemblyShape.Legend> shape, Predicate<AssemblyShape.Legend> which) {
        return shape.entrySet().stream().filter(entry -> which.test(entry.getValue())).map(Map.Entry::getKey)
                .collect(Collectors.toSet());
    }

    private static boolean isBlock(AssemblyShape.Legend legend, String block) {
        return legend.equals(new AssemblyShape.Legend.Block(Identifier.withDefaultNamespace(block)));
    }

    private static boolean isTag(AssemblyShape.Legend legend, Identifier tag, String defaultBlock) {
        return legend instanceof AssemblyShape.Legend.Tag(var named, var fallback)
                && named.equals(tag) && fallback.equals(Identifier.withDefaultNamespace(defaultBlock));
    }

    private static AssemblyShape read(String path) {
        String resource = "/data/groundworks/groundworks/assembly/" + path + ".json";
        try (Reader reader = new InputStreamReader(Objects.requireNonNull(
                VanillaAssembliesTest.class.getResourceAsStream(resource), resource), StandardCharsets.UTF_8)) {
            return AssemblyShape.CODEC.parse(JsonOps.INSTANCE, JsonParser.parseReader(reader)).getOrThrow();
        } catch (java.io.IOException e) {
            throw new java.io.UncheckedIOException(e);
        }
    }
}
