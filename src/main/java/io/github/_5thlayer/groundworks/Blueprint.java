// SPDX-FileCopyrightText: 2026 5thlayer
// SPDX-License-Identifier: MIT

package io.github._5thlayer.groundworks;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.UUIDUtil;
import net.minecraft.nbt.CompoundTag;

/**
 * One {@linkplain Blueprints Blueprint} as the world data keeps it (ADR 0014): its blocks, whether
 * it is a Copy or a Cut, who captured it, and when it was last held or pasted, in milliseconds
 * since the epoch, which a prune of unused Copies reads; a Copy's last few pastes, latest last,
 * which {@link UndoPaste} takes back; and what a Cut's blocks held, each block entity's data by its
 * place in the box, which a Copy never keeps.
 */
record Blueprint(BlueprintBlocks blocks, Kind kind, UUID owner, long lastUsed, List<BlueprintPaste> pastes,
        List<Held> held) {

    /** How many of a Copy's pastes Undo Paste can take back. */
    static final int UNDO_DEPTH = 10;

    Blueprint {
        pastes = List.copyOf(pastes);
        held = List.copyOf(held);
    }

    /** A Blueprint not yet pasted. */
    Blueprint(BlueprintBlocks blocks, Kind kind, UUID owner, long lastUsed, List<Held> held) {
        this(blocks, kind, owner, lastUsed, List.of(), held);
    }

    /** Whether it is a Cut, pasted once, uncharged, with what its blocks held. */
    boolean isCut() {
        return kind == Kind.CUT;
    }

    /** What a Cut's block held: its block entity's data, saved whole, at its place in the box. */
    record Held(BlockPos local, CompoundTag data) {

        static final Codec<Held> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                BlockPos.CODEC.fieldOf("pos").forGetter(Held::local),
                CompoundTag.CODEC.fieldOf("data").forGetter(Held::data)
        ).apply(instance, Held::new));
    }

    /** What a Blueprint does to the world it was captured from, and to its sheet once pasted. */
    enum Kind {
        /** Leaves the world as it was; each paste is charged, and the sheet keeps it. */
        COPY,
        /**
         * Takes its blocks out of the world with what they hold; it is pasted once, uncharged, and
         * the sheet is blank again. Never cleared, never undone.
         */
        CUT;

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
                BlueprintPaste.codec().listOf().optionalFieldOf("pastes", List.of()).forGetter(Blueprint::pastes),
                Held.CODEC.listOf().optionalFieldOf("held", List.of()).forGetter(Blueprint::held)
        ).apply(instance, Blueprint::new));
    }

    /** The same Blueprint, last used at {@code now}. */
    Blueprint usedAt(long now) {
        return new Blueprint(blocks, kind, owner, now, pastes, held);
    }

    /** The same Blueprint with this paste its latest, the oldest dropped past {@link #UNDO_DEPTH}. */
    Blueprint pasted(BlueprintPaste paste) {
        List<BlueprintPaste> kept = new ArrayList<>(pastes);
        kept.add(paste);
        return new Blueprint(blocks, kind, owner, lastUsed,
                kept.subList(Math.max(0, kept.size() - UNDO_DEPTH), kept.size()), held);
    }

    /** The same Blueprint without its latest paste. */
    Blueprint undone() {
        return new Blueprint(blocks, kind, owner, lastUsed, pastes.subList(0, Math.max(0, pastes.size() - 1)), held);
    }
}
