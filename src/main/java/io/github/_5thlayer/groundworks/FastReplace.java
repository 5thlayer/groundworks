// SPDX-FileCopyrightText: 2026 5thlayer
// SPDX-License-Identifier: MIT

package io.github._5thlayer.groundworks;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Predicate;

import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.jspecify.annotations.Nullable;

/**
 * Fast Replace: a plain click with a block item on a placed block of the same Replace group puts
 * the held block in its place (ADR 0008).
 *
 * <h2>Each Consumer states its groups</h2>
 *
 * <p>The library keeps no list of mods. A Consumer {@linkplain #group states a group}: an id and
 * which blocks are its members. A block belongs to the first group that claims it, and is never
 * replaced by itself.
 *
 * <h2>The item plans, the library lays</h2>
 *
 * <p>The plan is the held item's own, asked through a {@link ReplacingContext} whose clicked block
 * counts as replaceable, so it lands on the aimed block; the plan names that block as replaced, and
 * the preview draws it in the replace tint. An item whose plan puts no block there is refused. The
 * library carries out the click, never the item's own place, so the click lays what the preview
 * drew: it charges one held item, swaps the block, tells the new block it was placed, and hands the
 * old block's item back into the slot the charge freed, through the {@link Charge} a {@linkplain
 * Stretches Stretch} lays through. What becomes of the old block's contents is the block's own.
 *
 * <p>A replace is the main hand's, as a Stretch is, and the held item is a {@link BlockItem},
 * whose block's group is asked. It is drawn whether or not its block is {@linkplain
 * Placements#optIn opted in}, since the click replaces either way and the preview shows what the
 * click does. A sneak-click places beside instead. A plain click while a Stretch's start is stored
 * is the stretch's. A height held by {@link Raise} doesn't move a replace and stays on the stack.
 * A refusal of the library's own is told by name; any other, the item's own, as that the held
 * block can't replace the aimed one here.
 */
public final class FastReplace {

    private record Group(Identifier id, Predicate<? super Block> members) {
    }

    private static final List<Group> GROUPS = new CopyOnWriteArrayList<>();

    private FastReplace() {
    }

    /**
     * States a Replace group: the blocks {@code members} matches replace one another.
     *
     * <p>Called at mod construction, on both sides, since the preview asks on the client and the
     * click on the server. Mods are constructed in parallel, so this may be called from several
     * threads at once.
     */
    public static void group(Identifier id, Predicate<? super Block> members) {
        GROUPS.add(new Group(id, members));
    }

    /** The id of the group this block belongs to, the first stated that claims it, or {@code null}. */
    static @Nullable Identifier groupOf(Block block) {
        for (Group group : GROUPS) {
            if (group.members().test(block)) {
                return group.id();
            }
        }
        return null;
    }

    /** A replace's plan and what laying it charges. */
    private record Replacing(PlacementPlan plan, Charge charge) {
    }

    /**
     * The replace a plain click with the held stack at this hit would carry out, or {@code null}
     * when the click isn't one. Safe on either side, reading the world without touching it.
     */
    static @Nullable PlacementPlan planFor(Level level, @Nullable Player player, InteractionHand hand, ItemStack stack,
                                           BlockHitResult hit) {
        Replacing replacing = replacing(level, player, hand, stack, hit);
        return replacing == null ? null : replacing.plan();
    }

    private static @Nullable Replacing replacing(Level level, @Nullable Player player, InteractionHand hand,
                                                 ItemStack stack, BlockHitResult hit) {
        if (hand != InteractionHand.MAIN_HAND || player != null && player.isShiftKeyDown()
                || !(stack.getItem() instanceof BlockItem item) || Stretches.storedOn(level, stack) != null) {
            return null;
        }
        BlockPos aimed = hit.getBlockPos();
        BlockState old = level.getBlockState(aimed);
        Identifier group = groupOf(item.getBlock());
        if (group == null || old.is(item.getBlock()) || !group.equals(groupOf(old.getBlock()))) {
            return null;
        }

        PlacementPlan planned = Placements.planFor(item, new ReplacingContext(level, player, hand, stack, hit));
        PlacementPlan plan;
        if (planned == null) {
            plan = PlacementPlan.refused(aimed, item.getBlock().defaultBlockState(), Refusal.FastReplace.PLANS_ELSEWHERE);
        } else if (planned.blocks().stream().noneMatch(placed -> placed.pos().equals(aimed))) {
            plan = PlacementPlan.refused(planned.blocks(), Refusal.FastReplace.PLANS_ELSEWHERE);
        } else {
            plan = new PlacementPlan(planned.blocks(), List.of(aimed), planned.refusal());
        }

        Item back = old.getBlock().asItem();
        Charge charge = Charge.of(player, item, 1, back == Items.AIR ? List.of() : List.of(new ItemStack(back)));
        if (player != null && !plan.isRefused() && !charge.fits(player)) {
            plan = new PlacementPlan(plan.blocks(), plan.replaces(), Refusal.FastReplace.NO_ROOM_TO_RETURN);
        }
        return new Replacing(plan, charge);
    }

    /**
     * A click on a block. A plain click that is a replace lays it, or tells the player why not; any
     * other passes on, so the item keeps its own placement.
     */
    static InteractionResult useOn(Player player, InteractionHand hand, BlockHitResult hit) {
        Level level = player.level();
        ItemStack held = player.getItemInHand(hand);
        Replacing replacing = replacing(level, player, hand, held, hit);
        if (replacing == null) {
            return InteractionResult.PASS;
        }
        if (!level.isClientSide()) {
            PlacementPlan plan = replacing.plan();
            if (plan.isRefused()) {
                tell(player, message(plan.refusal()));
            } else {
                // The charge may take the held stack's last item, and a placed block is told what placed it.
                ItemStack placedWith = held.copyWithCount(1);
                replacing.charge().lay(level, player, plan);
                for (PlacementPlan.Placed placed : plan.blocks()) {
                    BlockState state = level.getBlockState(placed.pos());
                    state.getBlock().setPlacedBy(level, placed.pos(), state, player, placedWith);
                    if (player instanceof ServerPlayer server) {
                        CriteriaTriggers.PLACED_BLOCK.trigger(server, placed.pos(), placedWith);
                    }
                }
            }
        }
        return InteractionResult.SUCCESS;
    }

    /** What the player is told of a refused replace: the library's own reason, or that the held block can't go there. */
    static Component message(Refusal refusal) {
        Refusal reason = refusal instanceof Refusal.At at ? at.reason() : refusal;
        if (!(reason instanceof Refusal.FastReplace fastReplace)) {
            return Component.translatable("message.groundworks.fast_replace_refused");
        }
        return switch (fastReplace) {
            case NO_ROOM_TO_RETURN -> Component.translatable("message.groundworks.fast_replace_no_room_to_return");
            case PLANS_ELSEWHERE -> Component.translatable("message.groundworks.fast_replace_plans_elsewhere");
        };
    }

    private static void tell(Player player, Component message) {
        if (player instanceof ServerPlayer server) {
            server.sendSystemMessage(message, true);
        }
    }
}
