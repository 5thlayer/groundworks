// SPDX-FileCopyrightText: 2026 5thlayer
// SPDX-License-Identifier: MIT

package io.github._5thlayer.groundworks;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import org.jspecify.annotations.Nullable;

/**
 * Breaking a block as the player would for a {@linkplain Blueprints Blueprint}, and catching what
 * comes back: a paste that clears what stands where its Blueprint has air, and an {@linkplain
 * UndoPaste Undo Paste} that takes a paste up, both hand back what this returns.
 *
 * <p>What a block's removal spills, as a chest's contents, joins the level as item entities
 * inside its position while the block is broken; they are caught here, instead of falling. The
 * catch is held in plain statics: every break runs on the server thread, as does the {@link
 * EntityJoinLevelEvent} it raises, so one break is caught at a time, and none outside a break.
 */
final class BlueprintClearing {

    /** What the removal at {@link #catching} spills into the world, caught to be handed back; none between removals. */
    private static @Nullable List<ItemStack> caught;

    /** The position whose removal is caught. */
    private static @Nullable BlockPos catching;

    private BlueprintClearing() {
    }

    static void register() {
        NeoForge.EVENT_BUS.addListener(EntityJoinLevelEvent.class, BlueprintClearing::onEntityJoin);
    }

    /** Breaks each position as the player would, in order, and returns what comes back, as {@link #breakAt} does. */
    static List<ItemStack> clear(Level level, Player player, List<BlockPos> positions) {
        List<ItemStack> handed = new ArrayList<>();
        for (BlockPos pos : positions) {
            breakAt(level, player, pos, handed);
        }
        return handed;
    }

    /**
     * Breaks the position as the player would, on the server, and adds what comes back to {@code
     * into}: its drops, as broken with the correct tool, and what its removal spills, as a chest's
     * contents. A creative player is handed nothing, and what a removal spills lies where it
     * spilled, as a creative player's own break leaves it.
     */
    static void breakAt(Level level, Player player, BlockPos pos, List<ItemStack> into) {
        into.addAll(FastReplace.drops(level, player, pos, level.getBlockState(pos)));
        if (!player.hasInfiniteMaterials()) {
            caught = into;
            catching = pos;
        }
        try {
            level.destroyBlock(pos, false, player);
        } finally {
            caught = null;
            catching = null;
        }
    }

    /** Catches an item that the removal being broken spills inside its own position, instead of letting it fall. */
    private static void onEntityJoin(EntityJoinLevelEvent event) {
        List<ItemStack> into = caught;
        BlockPos at = catching;
        if (into != null && at != null && !event.getLevel().isClientSide()
                && event.getEntity() instanceof ItemEntity item && BlockPos.containing(item.position()).equals(at)) {
            into.add(item.getItem().copy());
            event.setCanceled(true);
        }
    }
}
