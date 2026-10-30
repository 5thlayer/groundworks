// SPDX-FileCopyrightText: 2026 5thlayer
// SPDX-License-Identifier: MIT

package io.github._5thlayer.groundworks.gametest;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

import com.mojang.serialization.MapCodec;
import io.github._5thlayer.groundworks.Assemblies;
import io.github._5thlayer.groundworks.Dismantles;
import io.github._5thlayer.groundworks.Groundworks;
import io.github._5thlayer.groundworks.Placements;
import io.github._5thlayer.groundworks.Rotate;
import io.github._5thlayer.groundworks.Stretches;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.GameTestInstance;
import net.minecraft.gametest.framework.TestData;
import net.minecraft.gametest.framework.TestEnvironmentDefinition;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Rotation;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLConstructModEvent;
import net.neoforged.neoforge.event.RegisterGameTestsEvent;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * The library's game tests, run by the {@code gameTestServer} Gradle run. Each test stands on the
 * {@code gametest/platform} structure, a stone floor that {@code sh ./gradlew buildGametestStructures}
 * writes, and places what it needs itself, so the setup is in the diff. It lives in the {@code gametest}
 * source set, which only the Module's own runs load, and hooks itself in, so the {@code @Mod} class
 * names no game-test code and nothing gates it (libworks ADR 0011).
 *
 * <p>The tests use vanilla blocks. The {@linkplain io.github._5thlayer.groundworks.VanillaConsumer
 * vanilla Consumer} opts in those its shipped tag holds; the tests opt in the few others they need,
 * such as cobblestone, a block with no orientation, and state their own Rotate in Place and a Key
 * of their own. That {@linkplain Placements#optIn Opt-in}, {@linkplain Rotate#turnsInPlace Rotate
 * in Place statement} and {@linkplain Assemblies#keys statement of Keys} are made while the tests
 * are registered, which happens only when game tests are enabled. They are the ones not made at mod
 * construction as ADR 0001 has it, and they hold on the server alone, which is all a game-test
 * server has. A dev client, which enables game tests too, draws these blocks.
 *
 * <p>A test that needs a pack developer's datapack runs in a batch of its own {@linkplain
 * WithTestPack with that pack switched on}.
 *
 * <p>The one block of the tests' own, {@link RefusesToTurnBlock}, and the one item,
 * {@link #STRETCHES_ARROWS}, whose legs {@link LineOfArrows} builds, and the dismantling tool
 * {@link #DISMANTLES}, and the item that plans its own placement, {@link #PLANS_PINK}, are
 * registered at mod construction, as are the builder, the {@link RowOfTerracotta} family and the
 * {@linkplain FastReplaceTests#stateGroups Replace groups}, as a Consumer's are, so a dev client
 * stretches, dismantles and replaces with them too. So is the {@linkplain TestFootprint tests'
 * Footprint}, with its blocks and item.
 */
@EventBusSubscriber(modid = Groundworks.MOD_ID)
public final class GroundworksGameTests {

    private static final Identifier PLATFORM = id("gametest/platform");
    /**
     * An 11×8×11 stone floor with air above, for shapes taller than {@link #PLATFORM} holds: a beacon's
     * pyramid, a conduit's frame, a nether portal. Written by the libworks plugin's
     * {@code build-gametest-structures.py} with its template set to {@code tall_platform.nbt}, (11, 8, 11).
     */
    private static final Identifier TALL_PLATFORM = id("gametest/tall_platform");

    private static final DeferredRegister<MapCodec<? extends GameTestInstance>> TEST_TYPES =
            DeferredRegister.create(Registries.TEST_INSTANCE_TYPE, Groundworks.MOD_ID);

    private static final DeferredRegister<MapCodec<? extends TestEnvironmentDefinition<?>>> ENVIRONMENT_TYPES =
            DeferredRegister.create(Registries.TEST_ENVIRONMENT_DEFINITION_TYPE, Groundworks.MOD_ID);

    static {
        TEST_TYPES.register("code", () -> CodeGameTest.CODEC);
        ENVIRONMENT_TYPES.register("with_test_pack", () -> WithTestPack.CODEC);
    }

    private static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(Groundworks.MOD_ID);

    static final DeferredBlock<RefusesToTurnBlock> REFUSES_TO_TURN =
            BLOCKS.registerBlock("gametest_refuses_to_turn", RefusesToTurnBlock::new);

    /** The tests' block that notes it was told it was placed, which a Stretch and an Assembly lay. */
    static final DeferredBlock<TellsPlacedBlock> TELLS_PLACED =
            BLOCKS.registerBlock("gametest_tells_placed", TellsPlacedBlock::new);

    private static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(Groundworks.MOD_ID);

    static final DeferredItem<BlockItem> TELLS_PLACED_ITEM = ITEMS.registerSimpleBlockItem(TELLS_PLACED);

    /**
     * The tests' stretch-able item: magenta glazed terracotta, placed one at a time as any block
     * item is, and stretched as a {@linkplain LineOfArrows line of arrows}. It leaves the block's
     * own item alone, so the block still names vanilla's as its item.
     */
    static final DeferredItem<BlockItem> STRETCHES_ARROWS = ITEMS.registerItem("gametest_stretches_arrows",
            properties -> new BlockItem(LineOfArrows.ARROW, properties) {
                @Override
                public void registerBlocks(Map<Block, Item> map, Item item) {
                }
            });

    /** The tests' item that plans its own placement, which places pink glazed terracotta. */
    static final DeferredItem<PlansItsOwnPlacement> PLANS_PINK = ITEMS.registerItem("gametest_plans_pink",
            properties -> new PlansItsOwnPlacement(Blocks.PINK_GLAZED_TERRACOTTA, properties));

    /**
     * The tests' dismantling tool, in {@code groundworks:dismantles} as an optional entry of the
     * game-test and dev packs, which takes up a {@linkplain RowOfTerracotta row of cyan terracotta}.
     */
    static final DeferredItem<Item> DISMANTLES = ITEMS.registerSimpleItem("gametest_dismantles");

    /** The vanilla blocks the tests opt in. */
    private static final Set<Block> TEST_BLOCKS = ConcurrentHashMap.newKeySet();

    /** The blocks the tests state Rotate in Place turns. */
    private static final Set<Block> TURNED_IN_PLACE = ConcurrentHashMap.newKeySet();

    /** The blocks the tests state are Assembly Keys. */
    private static final Set<Block> STATED_KEYS = ConcurrentHashMap.newKeySet();

    /** The tests are registered on each registry load, and their Opt-in, statement and claim guard are made on the first. */
    private static final AtomicBoolean FIRST_REGISTRATION_DONE = new AtomicBoolean();

    private GroundworksGameTests() {
    }

    @SubscribeEvent
    static void construct(FMLConstructModEvent event) {
        register(event.getContainer().getEventBus());
    }

    public static void register(IEventBus modBus) {
        TEST_TYPES.register(modBus);
        ENVIRONMENT_TYPES.register(modBus);
        modBus.addListener(WithTestPack::addPackFinders);
        BLOCKS.register(modBus);
        ITEMS.register(modBus);
        Stretches.register(new LineOfArrows());
        Dismantles.register(new RowOfTerracotta());
        FastReplaceTests.stateGroups();
        TestFootprint.register(modBus);
        // Posted only when game tests are enabled, which a production game never is.
        modBus.addListener(GroundworksGameTests::registerTests);
    }

    private static void registerTests(RegisterGameTestsEvent event) {
        if (FIRST_REGISTRATION_DONE.compareAndSet(false, true)) {
            Placements.optIn(TEST_BLOCKS::contains);
            Rotate.turnsInPlace(TURNED_IN_PLACE::contains);
            Assemblies.keys(STATED_KEYS::contains);
            RotateInPlaceTests.guardClaims();
        }
        // Registered rather than borrowed, since the event hands out no lookup for vanilla's.
        var environment = event.registerEnvironment(id("default"), new TestEnvironmentDefinition.AllOf(List.of()));
        var tests = new Registrar(event, environment, new ConcurrentHashMap<>());
        VanillaPlanTests.register(tests);
        VanillaCompanionTests.register(tests);
        PlanOptInTests.register(tests);
        RotateTests.register(tests);
        RotateInPlaceTests.register(tests);
        SideFaceRotateTests.register(tests);
        RaiseTests.register(tests);
        StretchTests.register(tests);
        VanillaStretchTests.register(tests);
        StretchReplaceTests.register(tests);
        DismantleTests.register(tests);
        VanillaDismantleTests.register(tests);
        FastReplaceTests.register(tests);
        HandBackTests.register(tests);
        FootprintTests.register(tests);
        AssemblyTests.register(tests);
        AssemblyRotateTests.register(tests);
        VanillaAssemblyTests.register(tests);
        BlueprintTests.register(tests);
        BlueprintPasteAirTests.register(tests);
        BlueprintTurnTests.register(tests);
        BlueprintUndoTests.register(tests);
        BlueprintLeftOutTests.register(tests);
        BlueprintStandingTests.register(tests);
        BlueprintCutTests.register(tests);
        BlueprintPruneTests.register(tests);
    }

    private static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(Groundworks.MOD_ID, path);
    }

    /** What a test class is handed: the blocks it opts in and states, and a name, a tick budget and a body per test. */
    record Registrar(RegisterGameTestsEvent event, Holder<TestEnvironmentDefinition<?>> environment,
                     Map<String, Holder<TestEnvironmentDefinition<?>>> packEnvironments) {

        void optIn(Block... blocks) {
            TEST_BLOCKS.addAll(List.of(blocks));
        }

        void turnsInPlace(Block... blocks) {
            TURNED_IN_PLACE.addAll(List.of(blocks));
        }

        void keys(Block... blocks) {
            STATED_KEYS.addAll(List.of(blocks));
        }

        /**
         * The same, for tests that run in a batch of their own with one of the tests' datapacks
         * switched on. The tests of every class that asks for one pack share its batch.
         */
        Registrar withPack(String pack) {
            return new Registrar(event, packEnvironments.computeIfAbsent(pack,
                    name -> event.registerEnvironment(id("with_" + name), new WithTestPack(name))), packEnvironments);
        }

        void test(String name, int maxTicks, Consumer<GameTestHelper> body) {
            test(PLATFORM, name, maxTicks, body);
        }

        /**
         * The same, in a batch of its own, which no other test runs beside: for a test that
         * changes what the whole server holds, such as every Blueprint of the world.
         */
        void testAlone(String name, int maxTicks, Consumer<GameTestHelper> body) {
            new Registrar(event, event.registerEnvironment(id("alone_" + name),
                    new TestEnvironmentDefinition.AllOf(List.of())), packEnvironments).test(name, maxTicks, body);
        }

        /** The same, on {@link #TALL_PLATFORM}. */
        void tallTest(String name, int maxTicks, Consumer<GameTestHelper> body) {
            test(TALL_PLATFORM, name, maxTicks, body);
        }

        private void test(Identifier structure, String name, int maxTicks, Consumer<GameTestHelper> body) {
            var id = id(name);
            CodeGameTest.define(id, body);
            event.registerTest(id, new CodeGameTest(id, new TestData<>(environment, structure, maxTicks, 0, true, Rotation.NONE)));
        }
    }
}
