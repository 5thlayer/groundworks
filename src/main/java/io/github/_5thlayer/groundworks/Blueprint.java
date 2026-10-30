// SPDX-FileCopyrightText: 2026 5thlayer
// SPDX-License-Identifier: MIT

package io.github._5thlayer.groundworks;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.UUIDUtil;

/**
 * One {@linkplain Blueprints Blueprint} as the world data keeps it (ADR 0014): its blocks, whether
 * it is a Copy, who captured it, and when it was last held or pasted, in milliseconds since the
 * epoch, which a prune of unused Copies reads; and its last few pastes, latest last, which {@link
 * UndoPaste} takes back.
 */
record Blueprint(BlueprintBlocks blocks, Kind kind, UUID owner, long lastUsed, List<BlueprintPaste> pastes) {

    /** How many of a Copy's pastes Undo Paste can take back. */
    static final int UNDO_DEPTH = 10;

    Blueprint {
        pastes = List.copyOf(pastes);
    }

    /** A Blueprint not yet pasted. */
    Blueprint(BlueprintBlocks blocks, Kind kind, UUID owner, long lastUsed) {
        this(blocks, kind, owner, lastUsed, List.of());
    }

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
                Codec.LONG.fieldOf("last_used").forGetter(Blueprint::lastUsed),
                BlueprintPaste.codec().listOf().optionalFieldOf("pastes", List.of()).forGetter(Blueprint::pastes)
        ).apply(instance, Blueprint::new));
    }

    /** The same Blueprint, last used at {@code now}. */
    Blueprint usedAt(long now) {
        return new Blueprint(blocks, kind, owner, now, pastes);
    }

    /** The same Blueprint with this paste its latest, the oldest dropped past {@link #UNDO_DEPTH}. */
    Blueprint pasted(BlueprintPaste paste) {
        List<BlueprintPaste> kept = new ArrayList<>(pastes);
        kept.add(paste);
        return new Blueprint(blocks, kind, owner, lastUsed,
                kept.subList(Math.max(0, kept.size() - UNDO_DEPTH), kept.size()));
    }

    /** The same Blueprint without its latest paste. */
    Blueprint undone() {
        return new Blueprint(blocks, kind, owner, lastUsed, pastes.subList(0, Math.max(0, pastes.size() - 1)));
    }
}
