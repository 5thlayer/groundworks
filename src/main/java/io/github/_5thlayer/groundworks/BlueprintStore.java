// SPDX-FileCopyrightText: 2026 5thlayer
// SPDX-License-Identifier: MIT

package io.github._5thlayer.groundworks;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import com.mojang.serialization.Codec;
import net.minecraft.core.UUIDUtil;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;
import org.jspecify.annotations.Nullable;

/**
 * Every {@linkplain Blueprints Blueprint} of a world, by the id its sheet holds: the server's
 * world data, shared by every dimension (ADR 0014), saved with the world.
 */
public final class BlueprintStore extends SavedData {

    /** The store's place in the world data, {@code data/groundworks/blueprints.dat}. */
    public static final SavedDataType<BlueprintStore> TYPE = new SavedDataType<>(
            Identifier.fromNamespaceAndPath(Groundworks.MOD_ID, "blueprints"),
            ignored -> new BlueprintStore(Map.of()),
            ignored -> Codec.unboundedMap(UUIDUtil.STRING_CODEC, Blueprint.codec())
                    .xmap(BlueprintStore::new, store -> store.blueprints));

    private final Map<UUID, Blueprint> blueprints;

    private BlueprintStore(Map<UUID, Blueprint> blueprints) {
        this.blueprints = new HashMap<>(blueprints);
    }

    /** The server's store, read from the world data the first time it is asked for. */
    static BlueprintStore of(MinecraftServer server) {
        return server.getDataStorage().computeIfAbsent(TYPE);
    }

    @Nullable Blueprint get(UUID id) {
        return blueprints.get(id);
    }

    void put(UUID id, Blueprint blueprint) {
        blueprints.put(id, blueprint);
        setDirty();
    }

    void remove(UUID id) {
        if (blueprints.remove(id) != null) {
            setDirty();
        }
    }
}
