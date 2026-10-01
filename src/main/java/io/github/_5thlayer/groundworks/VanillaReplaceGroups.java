// SPDX-FileCopyrightText: 2026 5thlayer
// SPDX-License-Identifier: MIT

package io.github._5thlayer.groundworks;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.block.Block;
import org.jspecify.annotations.Nullable;

/**
 * The vanilla Consumer's {@linkplain FastReplace Replace groups} (ADR 0008), one per block tag
 * under {@code groundworks:replace_group/}, read as the tags are now. Groundworks ships none, so a
 * vanilla install replaces nothing until a pack developer fills one. A group's id is its tag's.
 */
final class VanillaReplaceGroups {

    static final String FOLDER = "replace_group/";

    /** The blocks already warned of, so a block in two group tags is warned of once. */
    private static final Set<Block> WARNED = ConcurrentHashMap.newKeySet();

    private VanillaReplaceGroups() {
    }

    /**
     * The id of the group tag a block is in, or {@code null}. A block in two belongs to the first by
     * id, and is warned of.
     */
    static @Nullable Identifier groupOf(Block block) {
        TagKey<Block> tag = VanillaTags.firstUnder(block, FOLDER, "Replace group", WARNED);
        return tag == null ? null : tag.location();
    }
}
