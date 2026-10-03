// SPDX-FileCopyrightText: 2026 5thlayer
// SPDX-License-Identifier: MIT

package io.github._5thlayer.groundworks.gametest;

import com.mojang.serialization.MapCodec;
import io.github._5thlayer.groundworks.FootprintShape;
import io.github._5thlayer.groundworks.FootprintItem;
import io.github._5thlayer.groundworks.Footprint;
import io.github._5thlayer.groundworks.FootprintPartBlock;
import io.github._5thlayer.groundworks.Groundworks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
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
 * where game tests are enabled. Registered only then.
 */
final class TestFootprint {

    static final FootprintShape SHAPE = FootprintShape.of(new FootprintShape.Local(1, 0, 0), new FootprintShape.Local(0, 1, 0),
            new FootprintShape.Local(0, 0, 1));

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

    static final DeferredHolder<BlockEntityType<?>, BlockEntityType<HoldsEnergy>> HOLDS_ENERGY =
            BLOCK_ENTITIES.register("gametest_footprint_origin", () -> new BlockEntityType<>(HoldsEnergy::new, ORIGIN.get()));

    private TestFootprint() {
    }

    private static FootprintItem item() {
        return ITEM.get();
    }

    static void register(IEventBus modBus) {
        BLOCKS.register(modBus);
        ITEMS.register(modBus);
        BLOCK_ENTITIES.register(modBus);
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

    /** The origin's block entity, whose energy a part forwards. */
    static final class HoldsEnergy extends BlockEntity {

        final SimpleEnergyHandler energy = new SimpleEnergyHandler(1000);

        HoldsEnergy(BlockPos pos, BlockState state) {
            super(HOLDS_ENERGY.get(), pos, state);
        }
    }
}
