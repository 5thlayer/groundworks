// SPDX-FileCopyrightText: 2026 5thlayer
// SPDX-License-Identifier: MIT

package io.github._5thlayer.groundworks;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.Container;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.storage.TagValueInput;

/**
 * What a Cut does to the world that a Copy doesn't: it takes its blocks out, silently and with
 * what they hold, and puts back what they held where they are pasted.
 */
final class BlueprintCut {

    /**
     * How the box's blocks are taken out: the clients told, nothing else. No neighbour is told until
     * every block is gone, so nothing in the box breaks off another block of it and drops, and no
     * block entity spills what it holds.
     */
    private static final int TAKEN_SILENTLY = Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE
            | Block.UPDATE_SKIP_BLOCK_ENTITY_SIDEEFFECTS;

    private BlueprintCut() {
    }

    /**
     * Takes every block standing in the box out of the world, but those left out, with no drops,
     * and returns what each held, by its place in the box. Then the world around is told as a
     * break tells it: what stood outside the box on its blocks falls or breaks as vanilla says,
     * and fluids outside flow in.
     */
    static List<Blueprint.Held> takeOut(Level level, BoundingBox box, Set<BlockPos> leftOut) {
        BlockPos min = new BlockPos(box.minX(), box.minY(), box.minZ());
        List<Blueprint.Held> held = new ArrayList<>();
        Map<BlockPos, BlockState> taken = new LinkedHashMap<>();
        for (BlockPos at : BlockPos.betweenClosed(min, new BlockPos(box.maxX(), box.maxY(), box.maxZ()))) {
            BlockState state = level.getBlockState(at);
            if (state.isAir() || leftOut.contains(at)) {
                continue;
            }
            BlockPos pos = at.immutable();
            BlockEntity entity = level.getBlockEntity(pos);
            if (entity != null) {
                held.add(new Blueprint.Held(pos.subtract(min), entity.saveWithFullMetadata(level.registryAccess())));
            }
            taken.put(pos, state);
        }
        BlockState air = Blocks.AIR.defaultBlockState();
        taken.keySet().forEach(pos -> level.setBlock(pos, air, TAKEN_SILENTLY));
        taken.forEach((pos, old) -> {
            if (level instanceof ServerLevel server) {
                old.affectNeighborsAfterRemoval(server, pos, false);
            }
            level.updateNeighborsAt(pos, old.getBlock());
            old.updateIndirectNeighbourShapes(level, pos, Block.UPDATE_CLIENTS);
            air.updateNeighbourShapes(level, pos, Block.UPDATE_CLIENTS);
        });
        return held;
    }

    /** Loads what a Cut's blocks held into the block entities the paste laid where they landed. */
    static void putBack(Level level, PlacementPlan plan, Map<BlockPos, CompoundTag> held) {
        for (PlacementPlan.Placed placed : plan.blocks()) {
            CompoundTag data = held.get(placed.pos());
            BlockEntity entity = data == null ? null : level.getBlockEntity(placed.pos());
            if (entity != null) {
                entity.loadWithComponents(TagValueInput.create(ProblemReporter.DISCARDING, level.registryAccess(), data));
                entity.setChanged();
                BlockState state = level.getBlockState(placed.pos());
                level.sendBlockUpdated(placed.pos(), state, state, Block.UPDATE_CLIENTS);
            }
        }
    }

    /**
     * Drops each block of a Cut that can't stand where it landed at the player's feet: the item a
     * player would pick it with, where it has one, and what it held, where it was a container.
     */
    static void drop(Level level, Player player, List<PlacementPlan.Placed> cantStand, Map<BlockPos, Item> items,
            Map<BlockPos, CompoundTag> held) {
        for (PlacementPlan.Placed placed : cantStand) {
            Item item = items.getOrDefault(placed.pos(), Items.AIR);
            if (item != Items.AIR) {
                atFeet(level, player, new ItemStack(item));
            }
            CompoundTag data = held.get(placed.pos());
            BlockEntity entity = data == null ? null
                    : BlockEntity.loadStatic(placed.pos(), placed.state(), data, level.registryAccess());
            if (entity instanceof Container container) {
                for (int slot = 0; slot < container.getContainerSize(); slot++) {
                    ItemStack stack = container.getItem(slot);
                    if (!stack.isEmpty()) {
                        atFeet(level, player, stack.copy());
                    }
                }
            }
        }
    }

    private static void atFeet(Level level, Player player, ItemStack stack) {
        ItemEntity item = new ItemEntity(level, player.getX(), player.getY(), player.getZ(), stack, 0, 0, 0);
        item.setDefaultPickUpDelay();
        level.addFreshEntity(item);
    }
}
