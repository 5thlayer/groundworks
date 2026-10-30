// SPDX-FileCopyrightText: 2026 5thlayer
// SPDX-License-Identifier: MIT

package io.github._5thlayer.groundworks;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Undo Paste: the press that takes back the latest of a Copy's last {@value Blueprint#UNDO_DEPTH}
 * pastes, one per press, for the sheet in the main hand.
 *
 * <p>Each paste is kept with the Copy's world data, with the dimension it was laid in and each
 * block it laid as it stood once laid. An undo takes up only the blocks still standing so, leaving
 * alone any placed or changed since, latest laid first, each {@linkplain BlueprintClearing broken
 * as the player would break it}, and hands back what comes back, a container's contents among it,
 * as a Dismantle does: into the inventory, and what doesn't fit at the player's feet; a creative
 * player gets nothing. What the paste cleared or replaced isn't put back. It is refused whole,
 * with its reason, with no paste left to undo, on a Cut, in another dimension than the paste's, or
 * where the paste reaches a chunk that isn't loaded.
 */
public final class UndoPaste {

    private UndoPaste() {
    }

    /** The Blueprint with the paste of this plan, as the level now holds its blocks, its latest. */
    static Blueprint recorded(Blueprint blueprint, Level level, PlacementPlan plan) {
        Map<BlockPos, BlockState> laid = new LinkedHashMap<>();
        for (PlacementPlan.Placed placed : plan.blocks()) {
            laid.put(placed.pos(), level.getBlockState(placed.pos()));
        }
        return blueprint.pasted(BlueprintPaste.of(level.dimension(), laid));
    }

    /** Undo Paste was pressed: takes back the latest paste of the held sheet's Copy, or tells why not. */
    public static void press(ServerPlayer player) {
        ItemStack held = player.getMainHandItem();
        if (!Blueprints.isSheet(held)) {
            return;
        }
        ServerLevel level = player.level();
        Blueprint blueprint = Blueprints.heldBlueprint(level, held);
        if (blueprint == null) {
            Blueprints.refuse(player, Refusal.Blueprint.NOTHING_TO_UNDO);
            return;
        }
        // A Cut's paste is never undone.
        if (blueprint.isCut()) {
            Blueprints.refuse(player, Refusal.Blueprint.UNDO_ON_A_CUT);
            return;
        }
        List<BlueprintPaste> pastes = blueprint.pastes();
        if (pastes.isEmpty()) {
            Blueprints.refuse(player, Refusal.Blueprint.NOTHING_TO_UNDO);
            return;
        }
        BlueprintPaste paste = pastes.getLast();
        if (!paste.dimension().equals(level.dimension())) {
            Blueprints.refuse(player, Refusal.Blueprint.UNDO_IN_ANOTHER_DIMENSION);
            return;
        }
        List<BlueprintPaste.Laid> laid = paste.laid();
        for (BlueprintPaste.Laid block : laid) {
            if (!level.isLoaded(block.pos())) {
                Blueprints.refuse(player, Refusal.Blueprint.UNDO_UNLOADED);
                return;
            }
        }
        UUID id = Blueprints.heldId(held);
        BlueprintStore.of(level.getServer()).put(id, blueprint.undone());
        int taken = takeUp(level, player, laid);
        Laying.tell(player, Component.translatable("message.groundworks.blueprint_undone", taken));
    }

    /**
     * Takes up each block still standing as laid, latest laid first, so a block resting on another
     * goes before what holds it up, and hands back what comes back; returns how many it took.
     */
    private static int takeUp(ServerLevel level, ServerPlayer player, List<BlueprintPaste.Laid> laid) {
        List<ItemStack> handed = new ArrayList<>();
        int taken = 0;
        for (BlueprintPaste.Laid block : laid.reversed()) {
            if (level.getBlockState(block.pos()) != block.state()) {
                continue;
            }
            BlueprintClearing.breakAt(level, player, block.pos(), handed);
            taken++;
        }
        HandBack.give(player, handed);
        return taken;
    }
}
