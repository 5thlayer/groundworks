// SPDX-FileCopyrightText: 2026 5thlayer
// SPDX-License-Identifier: MIT

package io.github._5thlayer.groundworks.gametest;

import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

import com.mojang.serialization.MapCodec;
import io.github._5thlayer.groundworks.Groundworks;
import io.github._5thlayer.groundworks.Placements;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.GameTestInstance;
import net.minecraft.gametest.framework.TestData;
import net.minecraft.gametest.framework.TestEnvironmentDefinition;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Rotation;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.RegisterGameTestsEvent;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * The library's game tests, run by the {@code gameTestServer} Gradle run. Each test stands on the
 * {@code gametest/platform} structure, a stone floor that {@code scripts/build-gametest-structures.py}
 * writes, and places what it needs itself, so the setup is in the diff.
 *
 * <p>The tests use only vanilla blocks, which no Consumer is here to opt in, so they opt in their
 * own. That {@linkplain Placements#optIn Opt-in} is made while the tests are registered, which
 * happens only when game tests are enabled, so a production server never has it. It is the one
 * Opt-in not made at mod construction as ADR 0001 has it, and it holds on the server alone, which is
 * all a game-test server has. A dev client, which enables game tests too, draws these blocks.
 */
public final class GroundworksGameTests {

    private static final Identifier PLATFORM = id("gametest/platform");

    private static final DeferredRegister<MapCodec<? extends GameTestInstance>> TEST_TYPES =
            DeferredRegister.create(Registries.TEST_INSTANCE_TYPE, Groundworks.MOD_ID);

    static {
        TEST_TYPES.register("code", () -> CodeGameTest.CODEC);
    }

    /** The vanilla blocks the tests opt in. */
    private static final Set<Block> TEST_BLOCKS = ConcurrentHashMap.newKeySet();

    /** The tests are registered on each registry load, and their Opt-in is made on the first. */
    private static final AtomicBoolean OPT_IN_MADE = new AtomicBoolean();

    private GroundworksGameTests() {
    }

    public static void register(IEventBus modBus) {
        TEST_TYPES.register(modBus);
        // Posted only when game tests are enabled, so a production server never registers the tests.
        modBus.addListener(GroundworksGameTests::registerTests);
    }

    private static void registerTests(RegisterGameTestsEvent event) {
        if (OPT_IN_MADE.compareAndSet(false, true)) {
            Placements.optIn(TEST_BLOCKS::contains);
        }
        // Registered rather than borrowed, since the event hands out no lookup for vanilla's.
        var environment = event.registerEnvironment(id("default"), new TestEnvironmentDefinition.AllOf(List.of()));
        var tests = new Registrar(event, environment);
        VanillaPlanTests.register(tests);
        RotateTests.register(tests);
    }

    private static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(Groundworks.MOD_ID, path);
    }

    /** What a test class is handed: the blocks it opts in, and a name, a tick budget and a body per test. */
    record Registrar(RegisterGameTestsEvent event, Holder<TestEnvironmentDefinition<?>> environment) {

        void optIn(Block... blocks) {
            TEST_BLOCKS.addAll(List.of(blocks));
        }

        void test(String name, int maxTicks, Consumer<GameTestHelper> body) {
            var id = id(name);
            CodeGameTest.define(id, body);
            event.registerTest(id, new CodeGameTest(id, new TestData<>(environment, PLATFORM, maxTicks, 0, true, Rotation.NONE)));
        }
    }
}
