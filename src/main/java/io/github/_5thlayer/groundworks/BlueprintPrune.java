// SPDX-FileCopyrightText: 2026 5thlayer
// SPDX-License-Identifier: MIT

package io.github._5thlayer.groundworks;

import java.util.function.LongSupplier;

import net.minecraft.server.MinecraftServer;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

/**
 * The time {@linkplain Blueprints Blueprints} read, and the prune of the Copies no one uses (ADR
 * 0014): every Copy no one has held or pasted for the server's {@linkplain
 * GroundworksConfig#copyPruneAge prune age} is gone from the world data as the server starts and
 * hourly while it runs, and its sheets are blank once held or pasted. A Cut is never pruned.
 *
 * <p>Not API: {@link #clock} and {@link #prune} are public for the Module's own runs alone, and a
 * Consumer must not call them.
 */
public final class BlueprintPrune {

    /** How often a running server prunes its unused Copies, in ticks: hourly. */
    private static final int PRUNE_EVERY = 20 * 60 * 60;

    /** The time a Blueprint is used at, and pruned by, in milliseconds since the epoch. */
    private static volatile LongSupplier clock = System::currentTimeMillis;

    private BlueprintPrune() {
    }

    static void register() {
        NeoForge.EVENT_BUS.addListener(ServerStartedEvent.class, event -> prune(event.getServer()));
        NeoForge.EVENT_BUS.addListener(ServerTickEvent.Post.class, event -> {
            if (event.getServer().getTickCount() % PRUNE_EVERY == 0) {
                prune(event.getServer());
            }
        });
    }

    /**
     * Sets the clock Blueprints read the time from, in milliseconds since the epoch: when each is
     * captured, held and pasted, and when a prune finds a Copy unused for long enough. The wall
     * clock, {@code System::currentTimeMillis}, unless set.
     *
     * <p>Not API: for the Module's own runs.
     */
    public static void clock(LongSupplier now) {
        clock = now;
    }

    /** The time on the {@linkplain #clock clock}. */
    static long now() {
        return clock.getAsLong();
    }

    /**
     * Prunes the world data: every Copy no one has held or pasted for the server's prune age is
     * gone. Called as the server starts and hourly while it runs.
     *
     * <p>Not API: public for the Module's own runs.
     */
    public static void prune(MinecraftServer server) {
        long before = now() - GroundworksConfig.copyPruneAge().toMillis();
        BlueprintStore store = BlueprintStore.of(server);
        store.all().forEach((id, blueprint) -> {
            if (!blueprint.isCut() && blueprint.lastUsed() < before) {
                store.remove(id);
            }
        });
    }
}
