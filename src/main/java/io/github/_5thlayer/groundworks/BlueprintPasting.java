// SPDX-FileCopyrightText: 2026 5thlayer
// SPDX-License-Identifier: MIT

package io.github._5thlayer.groundworks;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.BlockHitResult;
import org.jspecify.annotations.Nullable;

/**
 * A paste of a {@linkplain Blueprints Blueprint} a click would lay, as planned at the aim on either
 * side: what the preview draws and the click lays.
 *
 * @param laying its plan, each block the captured state where it lands, {@linkplain SupportOrder
 *     support first}, a block that can't stand there left out; and its charge, each laid block's
 *     pick item
 * @param items the item each placed block is charged as, by its position, which it is told it was
 *     placed with
 * @param cut whether the Blueprint is a Cut, pasted once and uncharged
 * @param held what a Cut's blocks hold, by where they land; none for a Copy, or on the client
 * @param cantStand a Cut's blocks that can't stand where they land, which drop at the player's feet
 */
record BlueprintPasting(Laying laying, Map<BlockPos, Item> items, boolean cut, Map<BlockPos, CompoundTag> held,
        List<PlacementPlan.Placed> cantStand) {

    /** The refusals a laying makes for a paste. */
    private static final Laying.Reasons REASONS = new Laying.Reasons(Refusal.Blueprint.MAY_NOT_BUILD,
            Refusal.Blueprint.NOT_ENOUGH_ITEMS, null);

    /**
     * The paste of the Blueprint held on {@code stack} at this aim: resting on the aimed face,
     * turned by the stack's turn and moved by its height as the click's place context moves it.
     * Air is skipped, or, where {@code pastesAir}, what stands there is cleared, a block the player
     * could not break refusing the paste.
     */
    static BlueprintPasting of(Level level, @Nullable Player player, ItemStack stack, Blueprint blueprint,
            boolean pastesAir, BlockHitResult hit) {
        BlueprintBlocks blocks = blueprint.blocks();
        boolean cut = blueprint.isCut();
        BlockPos spot = new BlockPlaceContext(level, player, InteractionHand.MAIN_HAND, stack, hit).getClickedPos();
        BlueprintLanding landing = BlueprintLanding.of(blocks.size(), spot, hit.getDirection(), Rotate.turnOf(stack));
        boolean charged = !cut && player != null && !player.hasInfiniteMaterials();
        Map<BlockPos, CompoundTag> held = new LinkedHashMap<>();
        for (Blueprint.Held contents : blueprint.held()) {
            held.put(landing.at(contents.local()), contents.data());
        }
        List<PlacementPlan.Placed> wanted = new ArrayList<>();
        Map<BlockPos, Item> items = new LinkedHashMap<>();
        List<BlockPos> clears = new ArrayList<>();
        Refusal refusal = null;
        for (BlueprintBlocks.Captured captured : blocks.blocks()) {
            BlockPos pos = landing.at(captured.local());
            if (captured.state().isAir()) {
                // A left-out position is nothing, not air: what stands there stays, even pasting air.
                if (pastesAir && !captured.leftOut()) {
                    BlockState there = level.getBlockState(pos);
                    if (Laying.emptied(there) != there) {
                        if (refusal == null && there.getDestroySpeed(level, pos) < 0) {
                            refusal = new Refusal.At(Refusal.Blueprint.CANT_BREAK, pos);
                        }
                        clears.add(pos);
                    }
                }
                continue;
            }
            // A Companion, a fluid that isn't a source, or an itemless block a charged paste skips
            // is not air: it is never cleared, nor is a block left out because it can't stand.
            BlockState state = carried(landing.turned(level, pos, captured.state()), charged);
            // A Cut's block is the only one of it: a block standing as wanted is in its way, as any block is.
            if (state == null || (!cut && level.getBlockState(pos) == state)) {
                continue;
            }
            Item item = pickItem(level, player, pos, state);
            if (charged && item == Items.AIR) {
                continue;
            }
            wanted.add(new PlacementPlan.Placed(pos, state));
            items.put(pos, item);
        }
        SupportOrder.Standing standing = SupportOrder.inWorld(level, player, wanted);
        List<PlacementPlan.Placed> companions = Placements.companionsOf(standing.laid());
        Map<Item, Integer> cost = new LinkedHashMap<>();
        BlockPos taken = null;
        for (PlacementPlan.Placed placed : standing.laid()) {
            taken = taken == null ? takenAt(level, placed) : taken;
            Item item = items.get(placed.pos());
            if (item != Items.AIR) {
                cost.merge(item, 1, Integer::sum);
            }
        }
        for (PlacementPlan.Placed companion : companions) {
            taken = taken == null ? takenAt(level, companion) : taken;
        }
        if (refusal == null && taken != null) {
            refusal = new Refusal.At(Refusal.Blueprint.TAKEN, taken);
        }
        PlacementPlan plan = new PlacementPlan(standing.laid(), List.of(), clears, companions,
                standing.cantStand().stream().map(PlacementPlan.Placed::pos).toList(), refusal);
        return new BlueprintPasting(Laying.of(level, player, plan, Charge.of(player, cut ? Map.of() : cost, List.of()),
                REASONS), items, cut, held, standing.cantStand());
    }

    /**
     * What a Copy carries of a captured state, or {@code null} where it places nothing: nothing for
     * air, for a Companion, which its block places, and for a fluid that isn't a source; and,
     * where the paste is charged, a waterlogged block dry. A block entity is never carried, so a
     * chest pastes empty and a sign blank.
     */
    private static @Nullable BlockState carried(BlockState state, boolean charged) {
        if (state.isAir() || Placements.isCompanion(state)
                || (state.getBlock() instanceof LiquidBlock && !state.getFluidState().isSource())) {
            return null;
        }
        if (charged && state.getOptionalValue(BlockStateProperties.WATERLOGGED).orElse(false)) {
            return state.setValue(BlockStateProperties.WATERLOGGED, false);
        }
        return state;
    }

    /**
     * The position, where the block can't be put down there; else {@code null}. Every entity that
     * blocks building is in the way, the placing player among them.
     */
    private static @Nullable BlockPos takenAt(Level level, PlacementPlan.Placed placed) {
        BlockPos pos = placed.pos();
        return !level.isInWorldBounds(pos) || !level.getBlockState(pos).canBeReplaced()
                || Placements.entityInTheWay(level, null, pos, placed.state()) ? pos : null;
    }

    /** The item a player would pick the block with, where it lands; air for a block with none. */
    private static Item pickItem(Level level, @Nullable Player player, BlockPos pos, BlockState state) {
        ItemStack picked = player == null
                ? state.getCloneItemStack(level, pos, false)
                : state.getCloneItemStack(pos, level, false, player);
        return picked.isEmpty() ? Items.AIR : picked.getItem();
    }

    /** What the player is told of a paste's refusal. */
    Component message(Player player, Refusal refusal) {
        if (Laying.reason(refusal) == Refusal.Blueprint.NOT_ENOUGH_ITEMS) {
            return Component.translatable(Refusal.Blueprint.NOT_ENOUGH_ITEMS.message(),
                    Assemblies.missing(laying.charge().missing(player)));
        }
        if (Laying.reason(refusal) instanceof Refusal.Blueprint blueprint) {
            return Component.translatable(blueprint.message());
        }
        throw new IllegalStateException("not a Blueprint's refusal: " + refusal);
    }
}
