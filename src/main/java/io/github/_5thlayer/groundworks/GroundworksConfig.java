// SPDX-FileCopyrightText: 2026 5thlayer
// SPDX-License-Identifier: MIT

package io.github._5thlayer.groundworks;

import java.time.Duration;
import java.util.List;

import net.minecraft.resources.Identifier;
import net.neoforged.neoforge.common.ModConfigSpec;

/**
 * Groundworks' server config, {@code groundworks-server.toml} in a world's {@code serverconfig}.
 * A server config, so it syncs to the client, where the preview asks the same questions.
 */
public final class GroundworksConfig {

    static final ModConfigSpec SPEC;

    /**
     * The namespaces whose every block the vanilla Consumer {@linkplain VanillaConsumer opts in},
     * on top of {@link VanillaConsumer#PLAN_OPT_IN}: a pack's "all my blocks" in one line.
     */
    public static final ModConfigSpec.ConfigValue<List<? extends String>> PLAN_OPT_IN_NAMESPACES;

    /** The most positions a Blueprint's Selection may hold, so a capture, and the pastes of it, stay bounded. */
    public static final ModConfigSpec.IntValue MAX_SELECTION;

    /** The cap's default, 32×32×32. */
    private static final int DEFAULT_MAX_SELECTION = 32_768;

    /** How many days a Copy is kept unheld and unpasted before it is pruned; a Cut never is. */
    public static final ModConfigSpec.IntValue PRUNE_COPIES_AFTER_DAYS;

    private static final int DEFAULT_PRUNE_COPIES_AFTER_DAYS = 30;

    static {
        var builder = new ModConfigSpec.Builder();
        PLAN_OPT_IN_NAMESPACES = builder
                .comment("Namespaces whose every block gets a Vanilla Plan, and so a preview, as if the",
                        "groundworks:plan_opt_in block tag held it. For example [\"mypack\"].")
                .defineListAllowEmpty("planOptInNamespaces", List.of(), () -> "",
                        value -> value instanceof String namespace && Identifier.isValidNamespace(namespace));
        MAX_SELECTION = builder
                .comment("The most positions, air included, a Blueprint's Selection may hold. A capture over it is",
                        "refused, and a paste of the largest is laid in one tick.")
                .defineInRange("maxSelection", DEFAULT_MAX_SELECTION, 1, Integer.MAX_VALUE);
        PRUNE_COPIES_AFTER_DAYS = builder
                .comment("Days a Blueprint's Copy is kept in the world data while no one holds or pastes it. After",
                        "that it is pruned and its sheet is blank again. A Cut is never pruned.")
                .defineInRange("pruneCopiesAfterDays", DEFAULT_PRUNE_COPIES_AFTER_DAYS, 1, 36_500);
        SPEC = builder.build();
    }

    private GroundworksConfig() {
    }

    /**
     * Whether the config lists this namespace. False until a world's config is loaded, as on the
     * title screen.
     */
    static boolean listsNamespace(String namespace) {
        return SPEC.isLoaded() && PLAN_OPT_IN_NAMESPACES.get().contains(namespace);
    }

    /** The Selection's cap, its default until a world's config is loaded. */
    static int maxSelection() {
        return SPEC.isLoaded() ? MAX_SELECTION.get() : DEFAULT_MAX_SELECTION;
    }

    /** How long a Copy is kept unused before it is pruned, its default until a world's config is loaded. */
    public static Duration copyPruneAge() {
        return Duration.ofDays(SPEC.isLoaded() ? PRUNE_COPIES_AFTER_DAYS.get() : DEFAULT_PRUNE_COPIES_AFTER_DAYS);
    }
}
