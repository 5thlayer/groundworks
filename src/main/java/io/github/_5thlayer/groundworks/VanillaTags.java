// SPDX-FileCopyrightText: 2026 5thlayer
// SPDX-License-Identifier: MIT

package io.github._5thlayer.groundworks;

import java.util.Comparator;
import java.util.List;
import java.util.Set;

import com.mojang.logging.LogUtils;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.block.Block;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

/**
 * The rule the vanilla Consumer reads a folder of its block tags by, one statement per tag, as its
 * {@linkplain VanillaFamilies Dismantle families} and {@linkplain VanillaReplaceGroups Replace
 * groups} are (ADR 0005): the tags are read as they are now, and a block in two belongs to the
 * first by id.
 */
final class VanillaTags {

    private static final Logger LOGGER = LogUtils.getLogger();

    private VanillaTags() {
    }

    /**
     * The first by id of the block's tags under {@code groundworks:<folder>}, or {@code null}. A
     * block in two is warned of once, as a block in two of {@code kind}, and {@code warned} keeps
     * the blocks already warned of.
     */
    static @Nullable TagKey<Block> firstUnder(Block block, String folder, String kind, Set<Block> warned) {
        List<TagKey<Block>> tags = block.builtInRegistryHolder().tags()
                .filter(tag -> tag.location().getNamespace().equals(Groundworks.MOD_ID)
                        && tag.location().getPath().startsWith(folder))
                .sorted(Comparator.comparing((TagKey<Block> tag) -> tag.location().toString()))
                .toList();
        if (tags.size() > 1 && warned.add(block)) {
            LOGGER.warn("{} is in {} {} tags, {}; it belongs to the first", block, tags.size(), kind, tags);
        }
        return tags.isEmpty() ? null : tags.getFirst();
    }
}
