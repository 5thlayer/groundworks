// SPDX-FileCopyrightText: 2026 5thlayer
// SPDX-License-Identifier: MIT

package io.github._5thlayer.groundworks;

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

    static {
        var builder = new ModConfigSpec.Builder();
        PLAN_OPT_IN_NAMESPACES = builder
                .comment("Namespaces whose every block gets a Vanilla Plan, and so a preview, as if the",
                        "groundworks:plan_opt_in block tag held it. For example [\"mypack\"].")
                .defineListAllowEmpty("planOptInNamespaces", List.of(), () -> "",
                        value -> value instanceof String namespace && Identifier.isValidNamespace(namespace));
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
}
