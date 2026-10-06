// SPDX-FileCopyrightText: 2026 5thlayer
// SPDX-License-Identifier: MIT

package io.github._5thlayer.groundworks.gametest;

import java.util.ArrayList;
import java.util.List;

import com.mojang.serialization.MapCodec;
import io.github._5thlayer.groundworks.FastReplace;
import io.github._5thlayer.groundworks.FootprintShape;
import io.github._5thlayer.groundworks.FootprintItem;
import io.github._5thlayer.groundworks.Footprint;
import io.github._5thlayer.groundworks.FootprintPartBlock;
import io.github._5thlayer.groundworks.Groundworks;
import io.github._5thlayer.groundworks.PlacementPlan;
import io.github._5thlayer.groundworks.Refusal;
import io.github._5thlayer.groundworks.ReplaceBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.material.PushReaction;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.transfer.energy.SimpleEnergyHandler;
import org.jspecify.annotations.Nullable;

/**
 * The tests' Footprint, declared as a Consumer declares one: an origin that holds energy, a part
 * forward, a part above and a part to the side, so that a turn moves two of them. Its origin's loot
 * table, which drops its item, is in the {@code footprints} test pack, since the item exists only
 * where game tests are enabled. Registered only then, with a Replace group of its blocks whose
 * {@linkplain ReplacesWhole builder} asks where the origin stands, as a Consumer's machine tiers do.
 */
final class TestFootprint {

    static final FootprintShape SHAPE = FootprintShape.of(new FootprintShape.Local(1, 0, 0), new FootprintShape.Local(0, 1, 0),
            new FootprintShape.Local(0, 0, 1));

    /** A 5x5 square three blocks tall, 74 parts, as a Consumer's big machine is declared. */
    static final FootprintShape BIG_SHAPE = FootprintShape.square(5, 3);

    private static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(Groundworks.MOD_ID);
    private static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(Groundworks.MOD_ID);
    private static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, Groundworks.MOD_ID);

    static final DeferredBlock<Origin> ORIGIN = BLOCKS.registerBlock("gametest_footprint_origin",
            properties -> new Origin(properties.pushReaction(PushReaction.BLOCK)));

    static final DeferredBlock<FootprintPartBlock> PART = BLOCKS.registerBlock("gametest_footprint_part",
            properties -> new FootprintPartBlock(properties.noLootTable(), () -> TestFootprint.FOOTPRINT));

    static final Footprint FOOTPRINT = Footprint.declare(SHAPE, ORIGIN, PART, TestFootprint::item);

    static final DeferredItem<FootprintItem> ITEM = ITEMS.registerItem("gametest_footprint",
            properties -> new FootprintItem(FOOTPRINT, properties));

    static final DeferredBlock<BigOrigin> BIG_ORIGIN = BLOCKS.registerBlock("gametest_big_footprint_origin",
            properties -> new BigOrigin(properties.pushReaction(PushReaction.BLOCK).noLootTable()));

    static final DeferredBlock<FootprintPartBlock> BIG_PART = BLOCKS.registerBlock("gametest_big_footprint_part",
            properties -> new FootprintPartBlock(properties.noLootTable(), () -> TestFootprint.BIG_FOOTPRINT));

    static final Footprint BIG_FOOTPRINT = Footprint.declare(BIG_SHAPE, BIG_ORIGIN, BIG_PART, TestFootprint::bigItem);

    static final DeferredItem<FootprintItem> BIG_ITEM = ITEMS.registerItem("gametest_big_footprint",
            properties -> new FootprintItem(BIG_FOOTPRINT, properties));

    static final DeferredHolder<BlockEntityType<?>, BlockEntityType<HoldsEnergy>> HOLDS_ENERGY =
            BLOCK_ENTITIES.register("gametest_footprint_origin", () -> new BlockEntityType<>(HoldsEnergy::new, ORIGIN.get()));

    private TestFootprint() {
    }

    private static FootprintItem item() {
        return ITEM.get();
    }

    private static FootprintItem bigItem() {
        return BIG_ITEM.get();
    }

    static void register(IEventBus modBus) {
        BLOCKS.register(modBus);
        ITEMS.register(modBus);
        BLOCK_ENTITIES.register(modBus);
        FastReplace.group(Identifier.fromNamespaceAndPath(Groundworks.MOD_ID, "gametest_footprint"),
                block -> block == ORIGIN.get() || block == PART.get(), new ReplacesWhole());
        modBus.addListener(RegisterCapabilitiesEvent.class, event ->
                event.registerBlockEntity(Capabilities.Energy.BLOCK, HOLDS_ENERGY.get(), (entity, side) -> entity.energy));
    }

    /** The origin: a horizontally facing block with a block entity, which a piston doesn't move and which takes its parts with it when it goes. */
    static final class Origin extends HorizontalDirectionalBlock implements EntityBlock {

        private static final MapCodec<Origin> CODEC = simpleCodec(Origin::new);

        Origin(Properties properties) {
            super(properties);
        }

        @Override
        protected MapCodec<? extends HorizontalDirectionalBlock> codec() {
            return CODEC;
        }

        @Override
        protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
            builder.add(FACING);
        }

        @Override
        public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
            return new HoldsEnergy(pos, state);
        }

        @Override
        protected void affectNeighborsAfterRemoval(BlockState state, ServerLevel level, BlockPos pos, boolean movedByPiston) {
            super.affectNeighborsAfterRemoval(state, level, pos, movedByPiston);
            FOOTPRINT.teardown(level, pos, state.getValue(FACING), pos);
        }
    }

    /** The big footprint's origin: facing, no block entity, taking its parts with it when it goes. */
    static final class BigOrigin extends HorizontalDirectionalBlock {

        private static final MapCodec<BigOrigin> CODEC = simpleCodec(BigOrigin::new);

        BigOrigin(Properties properties) {
            super(properties);
        }

        @Override
        protected MapCodec<? extends HorizontalDirectionalBlock> codec() {
            return CODEC;
        }

        @Override
        protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
            builder.add(FACING);
        }

        @Override
        protected void affectNeighborsAfterRemoval(BlockState state, ServerLevel level, BlockPos pos, boolean movedByPiston) {
            super.affectNeighborsAfterRemoval(state, level, pos, movedByPiston);
            BIG_FOOTPRINT.teardown(level, pos, state.getValue(FACING), pos);
        }
    }

    /**
     * Replaces the whole footprint as it stands, asking where its origin is as a Consumer's builder
     * does, with no guard of its own: the library never asks it of an orphan part.
     */
    private static final class ReplacesWhole implements ReplaceBuilder {

        @Override
        public @Nullable PlacementPlan plan(Level level, @Nullable Player player, ItemStack held, BlockPos aimed,
                                            BlockState old) {
            BlockPos origin = FOOTPRINT.standingOrigin(level, aimed, old);
            if (origin == null) {
                throw new IllegalStateException("a replace builder was asked of an orphan part at " + aimed);
            }
            Direction facing = level.getBlockState(origin).getValue(Footprint.FACING);
            List<BlockPos> positions = FOOTPRINT.positions(origin, facing);
            List<PlacementPlan.Placed> blocks = new ArrayList<>(positions.size());
            for (int i = 0; i < positions.size(); i++) {
                blocks.add(new PlacementPlan.Placed(positions.get(i), FOOTPRINT.stateAt(i, facing)));
            }
            return PlacementPlan.replacing(blocks, null);
        }

        @Override
        public Component message(Refusal refusal) {
            return Component.literal(String.valueOf(refusal));
        }
    }

    /** The origin's block entity, whose energy a part forwards. */
    static final class HoldsEnergy extends BlockEntity {

        final SimpleEnergyHandler energy = new SimpleEnergyHandler(1000);

        HoldsEnergy(BlockPos pos, BlockState state) {
            super(HOLDS_ENERGY.get(), pos, state);
        }
    }
}
