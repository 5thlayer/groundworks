// SPDX-FileCopyrightText: 2026 5thlayer
// SPDX-License-Identifier: MIT

package io.github._5thlayer.groundworks;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;

/**
 * One {@linkplain Assemblies Assembly}'s declaration, read and checked: the shape of real blocks
 * placed in one step from its Key (ADR 0011), and where each of them stands for a placement.
 *
 * <h2>The data format</h2>
 *
 * <p>A JSON file under {@code data/<namespace>/groundworks/assembly/}:
 *
 * <pre>{@code
 * {
 *   "key": "K",
 *   "legend": {"K": "minecraft:lodestone", "s": "minecraft:stone", "c": "minecraft:cobblestone"},
 *   "layers": [
 *     ["c ",
 *      "ss"],
 *     ["  ",
 *      "K "]
 *   ],
 *   "starts_alone": false
 * }
 * }</pre>
 *
 * <ul>
 *   <li>{@code layers}: the shape, its bottom layer first. Each layer is drawn as seen from above
 *       by a player at the bottom of the page looking up the page: its first row is the farthest
 *       forward and its last the nearest, and each row's first letter is on the player's left and
 *       its last on their right. Every layer has as many rows as every other, and every row as many
 *       letters. A space is not part of the shape: the world there is left alone.
 *   <li>{@code legend}: what each letter stands for, a block's id.
 *   <li>{@code key}: the letter that marks the Key, which the shape holds exactly once and the
 *       legend names a block for: the block whose item places the Assembly.
 *   <li>{@code starts_alone}: whether a fresh stack of the Key places the Key alone until Next
 *       Assembly steps it on; false when left out. One of a Key's declarations saying so is
 *       enough; otherwise a fresh stack starts on the Key's first Assembly by id.
 * </ul>
 *
 * <p>The frame is the {@linkplain FootprintShape Footprint}'s (ADR 0009), with the Key as its
 * origin: forward is {@code x}, the player's left is {@code z}, up is {@code y}, and a placement
 * turns it by the way the Key faces as {@link FootprintShape.Local#inWorld} does. So in the example
 * the cobblestone stands forward of the Key's column, one stone under the Key and one to the
 * player's right of it.
 *
 * <p>The block ids are not checked here, since this is read with no registries: whoever loads it
 * does.
 */
public final class AssemblyShape {

    /** What a letter of the legend stands for. */
    public sealed interface Legend {

        /** A block, by its id. */
        record Block(Identifier id) implements Legend {
        }
    }

    /** Where one block of a placed shape stands, and what its letter stands for. */
    public record At(BlockPos pos, Legend legend) {
    }

    /** One block of the shape, in the Key's frame, and what its letter stands for. */
    private record Part(FootprintShape.Local offset, Legend legend) {
    }

    private static final Codec<Legend> LEGEND_CODEC = Identifier.CODEC.xmap(Legend.Block::new,
            legend -> ((Legend.Block) legend).id());

    private record Declaration(String key, Map<String, Legend> legend, List<List<String>> layers, boolean startsAlone) {

        static final Codec<Declaration> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Codec.STRING.fieldOf("key").forGetter(Declaration::key),
                Codec.unboundedMap(Codec.STRING, LEGEND_CODEC).fieldOf("legend").forGetter(Declaration::legend),
                Codec.STRING.listOf(1, Integer.MAX_VALUE).listOf(1, Integer.MAX_VALUE).fieldOf("layers")
                        .forGetter(Declaration::layers),
                Codec.BOOL.optionalFieldOf("starts_alone", false).forGetter(Declaration::startsAlone)
        ).apply(instance, Declaration::new));
    }

    /** A declaration as its JSON reads, checked as it is read: a malformed one is an error naming what is wrong. */
    public static final Codec<AssemblyShape> CODEC = Declaration.CODEC.comapFlatMap(AssemblyShape::of,
            shape -> shape.declaration);

    private final Declaration declaration;
    private final Identifier keyBlock;
    private final List<Part> parts;
    /** The Key's layer counted from the bottom, and the layers below and above it. */
    private final int keyLayer;
    private final int layerCount;

    private AssemblyShape(Declaration declaration, Identifier keyBlock, List<Part> parts, int keyLayer) {
        this.declaration = declaration;
        this.keyBlock = keyBlock;
        this.parts = List.copyOf(parts);
        this.keyLayer = keyLayer;
        this.layerCount = declaration.layers().size();
    }

    private static DataResult<AssemblyShape> of(Declaration declaration) {
        if (declaration.key().length() != 1) {
            return DataResult.error(() -> "the key is one letter, got '" + declaration.key() + "'");
        }
        Map<Character, Legend> legend = new LinkedHashMap<>();
        for (Map.Entry<String, Legend> entry : declaration.legend().entrySet()) {
            if (entry.getKey().length() != 1 || entry.getKey().equals(" ")) {
                return DataResult.error(() -> "a legend names one letter, not a space, got '" + entry.getKey() + "'");
            }
            legend.put(entry.getKey().charAt(0), entry.getValue());
        }
        char key = declaration.key().charAt(0);
        if (!(legend.get(key) instanceof Legend.Block keyBlock)) {
            return DataResult.error(() -> "the legend names no block for the key '" + key + "'");
        }
        List<List<String>> layers = declaration.layers();
        int rows = layers.getFirst().size();
        int width = layers.getFirst().getFirst().length();
        for (List<String> layer : layers) {
            if (layer.size() != rows || layer.stream().anyMatch(row -> row.length() != width)) {
                return DataResult.error(() -> "every layer has " + rows + " rows of " + width
                        + " letters, as the first does, got " + layer);
            }
        }
        int keys = 0;
        int keyLayer = 0;
        int keyRow = 0;
        int keyColumn = 0;
        for (int y = 0; y < layers.size(); y++) {
            for (int row = 0; row < rows; row++) {
                String letters = layers.get(y).get(row);
                for (int column = 0; column < width; column++) {
                    char letter = letters.charAt(column);
                    if (letter != ' ' && !legend.containsKey(letter)) {
                        return DataResult.error(() -> "the legend doesn't name the letter '" + letter + "'");
                    }
                    if (letter == key) {
                        keys++;
                        keyLayer = y;
                        keyRow = row;
                        keyColumn = column;
                    }
                }
            }
        }
        if (keys != 1) {
            int found = keys;
            return DataResult.error(() -> found == 0 ? "the shape has no Key '" + key + "'"
                    : "the shape has " + found + " Keys '" + key + "', not one");
        }
        List<Part> parts = new ArrayList<>();
        for (int y = 0; y < layers.size(); y++) {
            for (int row = 0; row < rows; row++) {
                String letters = layers.get(y).get(row);
                for (int column = 0; column < width; column++) {
                    char letter = letters.charAt(column);
                    if (letter != ' ' && letter != key) {
                        parts.add(new Part(new FootprintShape.Local(keyRow - row, y - keyLayer, keyColumn - column),
                                legend.get(letter)));
                    }
                }
            }
        }
        return DataResult.success(new AssemblyShape(declaration, keyBlock.id(), parts, keyLayer));
    }

    /** The id of the block that is the Key. */
    public Identifier keyBlock() {
        return keyBlock;
    }

    /** Whether a fresh stack of the Key places the Key alone. */
    public boolean startsAlone() {
        return declaration.startsAlone();
    }

    /** Every block id the legend names, which whoever loads the shape checks. */
    public List<Identifier> blocks() {
        return declaration.legend().values().stream()
                .map(legend -> ((Legend.Block) legend).id())
                .distinct()
                .toList();
    }

    /**
     * Where each block stands for a placement at {@code spot}, the position a block placed at the
     * aim would take, against the aimed {@code face}, with the Key facing {@code facing}, a
     * horizontal way: the other blocks bottom layer first in the declaration's order, then the Key.
     *
     * <p>It rests on the face: the Key's column stands on the spot, the bottom layer level with it,
     * or under a face, the top layer.
     */
    public List<At> positions(BlockPos spot, Direction face, Direction facing) {
        int rise = face == Direction.DOWN ? keyLayer - (layerCount - 1) : keyLayer;
        BlockPos key = spot.above(rise);
        List<At> at = new ArrayList<>(parts.size() + 1);
        for (Part part : parts) {
            at.add(new At(key.offset(part.offset().inWorld(facing)), part.legend()));
        }
        at.add(new At(key, new Legend.Block(keyBlock)));
        return at;
    }
}
