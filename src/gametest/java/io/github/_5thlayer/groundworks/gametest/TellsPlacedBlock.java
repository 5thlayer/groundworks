// SPDX-FileCopyrightText: 2026 5thlayer
// SPDX-License-Identifier: MIT

package io.github._5thlayer.groundworks.gametest;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;

/**
 * A block that notes each time it is told it was placed, and with what, as vanilla's own placing
 * tells a block through {@code setPlacedBy}. Vanilla's blocks that a Stretch or an Assembly lays
 * do nothing a test could see when told, so this is the tests' own.
 */
final class TellsPlacedBlock extends Block {

    /** Each position told, absolute, and the item it was told it was placed with. */
    static final Map<BlockPos, Item> TOLD = new ConcurrentHashMap<>();

    TellsPlacedBlock(Properties properties) {
        super(properties);
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack stack) {
        if (placer != null) {
            TOLD.put(pos.immutable(), stack.getItem());
        }
    }
}
