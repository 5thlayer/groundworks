// SPDX-FileCopyrightText: 2026 5thlayer
// SPDX-License-Identifier: MIT

package io.github._5thlayer.groundworks;

import java.util.Locale;
import java.util.UUID;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.UUIDUtil;

/**
 * One {@linkplain Blueprints Blueprint} as the world data keeps it (ADR 0014): its blocks, whether
 * it is a Copy, who captured it, and when it was last held or pasted, in milliseconds since the
 * epoch, which a prune of unused Copies reads.
 */
record Blueprint(BlueprintBlocks blocks, Kind kind, UUID owner, long lastUsed) {

    /** What a Blueprint does to the world it was captured from, and to its sheet once pasted. */
    enum Kind {
        /** Leaves the world as it was; each paste is charged, and the sheet keeps it. */
        COPY;

        static final Codec<Kind> CODEC = Codec.STRING.xmap(name -> valueOf(name.toUpperCase(Locale.ROOT)),
                kind -> kind.name().toLowerCase(Locale.ROOT));
    }

    // Built when asked for rather than as a constant: a block state's codec reads the block registry.
    static Codec<Blueprint> codec() {
        return RecordCodecBuilder.create(instance -> instance.group(
                BlueprintBlocks.codec().fieldOf("blocks").forGetter(Blueprint::blocks),
                Kind.CODEC.fieldOf("kind").forGetter(Blueprint::kind),
                UUIDUtil.CODEC.fieldOf("owner").forGetter(Blueprint::owner),
                Codec.LONG.fieldOf("last_used").forGetter(Blueprint::lastUsed)
        ).apply(instance, Blueprint::new));
    }

    /** The same Blueprint, last used at {@code now}. */
    Blueprint usedAt(long now) {
        return new Blueprint(blocks, kind, owner, now);
    }
}
