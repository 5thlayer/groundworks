// SPDX-FileCopyrightText: 2026 5thlayer
// SPDX-License-Identifier: MIT

package io.github._5thlayer.groundworks;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Function;
import java.util.function.Predicate;

import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.phys.BlockHitResult;
import net.neoforged.neoforge.common.util.BlockSnapshot;
import net.neoforged.neoforge.event.EventHooks;
import org.jspecify.annotations.Nullable;

/**
 * Fast Replace: a plain click with a block item on a placed block of the same Replace group puts
 * the held block in its place (ADR 0008).
 *
 * <h2>Each Consumer states its groups</h2>
 *
 * <p>The library keeps no list of mods. A Consumer {@linkplain #group states a group}: an id,
 * which blocks are its members, and optionally a {@linkplain ReplaceBuilder builder}. A block
 * belongs to the first group that claims it, and without a builder is never replaced by itself.
 * The vanilla Consumer states its groups through data, {@linkplain
 * VanillaReplaceGroups the block tags} a pack developer fills, after every mod's own.
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
 *
 * <h2>A group's builder plans a replace that spans blocks</h2>
 *
 * <p>A group with a builder takes its plan from the builder, not from the item: the group is the
 * aimed block's, which must be the held block's too, and the builder is asked for the whole plan,
 * the positions it swaps, the state at each and which it replaces. It is asked wherever the item's
 * own plan would be, and when it plans nothing the click is the item's own, as vanilla's. The
 * builder decides which members replace which, so the same kind is not refused as it is without
 * one, and its plan is used as it is, with no orientation copied. A plan must place a block at
 * every position it replaces, since a replace swaps and never clears: one that leaves a position
 * out is refused as {@link Refusal.FastReplace#LEAVES_A_GAP}. The builder says what a replace hands
 * back, one stack for the plan. The library charges one held item for the plan, checks room for
 * the refund and that the player may build at each position, fires the place event at each, and
 * lays the swaps in the plan's order, as it does the item's. A refusal of the builder's own is told
 * through its {@linkplain ReplaceBuilder#message message}, as a Stretch's leg builder's is.
 *
 * <h2>The new block keeps the old one's orientation</h2>
 *
 * <p>The block that goes in the aimed block's place takes the orientation of the block it replaces:
 * each facing, axis or rotation the two share, as the same {@link Property} object and never by
 * name, the properties {@link Rotate} turns. No other property is copied, since one may count what
 * a block holds, as a double slab's type does, or name a part of a block that spans two, and a
 * replace charges one item and hands one back. A Consumer whose blocks keep more sets it in its
 * own plan. An orientation the new block lacks is dropped, and any other property is the item
 * plan's. A turn {@link Rotate} holds on the stack
 * skips the copy: the new block is what the plan makes of the turned look, and the turn stays on
 * the stack. The preview's plan carries the copied or turned state, which the click lays.
 *
 * <h2>A refused replace changes nothing</h2>
 *
 * <p>A refused click is cancelled and its reason put on the action bar, so it never falls through
 * to placing beside. The player may not build where the plan puts any block, in adventure mode,
 * outside the world or in protected ground, as a Stretch's rule has it, and that is in the plan
 * the preview draws, whatever else refused it. A protection mod that cancels NeoForge's place
 * event, once for each position as {@link Rotate} fires it for the block it turns, refuses on the
 * click alone, and only a replace that would otherwise go through: firing an event every frame of
 * the preview is not the preview's to do, so the preview draws such a replace accepted. Either
 * refuses as {@link Refusal.FastReplace#MAY_NOT_BUILD}.
 * An entity in the new block's way refuses through the item's own plan, as vanilla refuses a
 * placement. A player with infinite materials is charged nothing, handed nothing back and so never
 * refused for room.
 */
public final class FastReplace {

    /** A Replace group: its id, and the builder that plans its replaces, or {@code null} for the item's own plan. */
    private record Group(Identifier id, @Nullable ReplaceBuilder builder) {
    }

    /**
     * The statements of which group a block belongs to, asked in the order they were made: each
     * answers a block with its group, or {@code null}. A {@linkplain #group stated group} is one;
     * the vanilla Consumer's tags are another, whose ids come from data and which have no builder.
     */
    private static final List<Function<Block, @Nullable Group>> GROUP_STATEMENTS = new CopyOnWriteArrayList<>();

    private FastReplace() {
    }

    /**
     * States a Replace group: the blocks {@code members} matches replace one another, each as the
     * held item's own plan puts it.
     *
     * <p>Called at mod construction, on both sides, since the preview asks on the client and the
     * click on the server. Mods are constructed in parallel, so this may be called from several
     * threads at once.
     */
    public static void group(Identifier id, Predicate<? super Block> members) {
        state(id, members, null);
    }

    /**
     * States a Replace group whose replace {@code builder} plans: a replace that spans several
     * blocks, and what it hands back, is the builder's to say.
     *
     * <p>Called at mod construction, on both sides, as {@link #group(Identifier, Predicate)} is.
     */
    public static void group(Identifier id, Predicate<? super Block> members, ReplaceBuilder builder) {
        state(id, members, builder);
    }

    private static void state(Identifier id, Predicate<? super Block> members, @Nullable ReplaceBuilder builder) {
        Group group = new Group(id, builder);
        GROUP_STATEMENTS.add(block -> members.test(block) ? group : null);
    }

    /**
     * States groups whose ids are not known until a block is asked, as the vanilla Consumer's are
     * its tags. A block belongs to the first group any statement gives it, in the order they were
     * made.
     */
    static void groupsFrom(Function<Block, @Nullable Identifier> statement) {
        GROUP_STATEMENTS.add(block -> {
            Identifier id = statement.apply(block);
            return id == null ? null : new Group(id, null);
        });
    }

    /** The group this block belongs to, the first stated that claims it, or {@code null}. */
    private static @Nullable Group groupOf(Block block) {
        for (Function<Block, @Nullable Group> statement : GROUP_STATEMENTS) {
            Group group = statement.apply(block);
            if (group != null) {
                return group;
            }
        }
        return null;
    }

    /** A replace's plan, what laying it charges, and the builder that planned it, if its group has one. */
    private record Replacing(PlacementPlan plan, Charge charge, @Nullable ReplaceBuilder builder) {
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
        Group group = groupOf(item.getBlock());
        Group aimedGroup = groupOf(old.getBlock());
        if (group == null || aimedGroup == null || !group.id().equals(aimedGroup.id())) {
            return null;
        }
        ReplaceBuilder builder = aimedGroup.builder();
        if (builder == null && old.is(item.getBlock())) {
            return null;
        }

        PlacementPlan plan;
        ItemStack back;
        if (builder == null) {
            plan = itemPlan(level, player, hand, stack, hit, item, old);
            back = new ItemStack(old.getBlock().asItem());
        } else {
            PlacementPlan planned = builder.plan(level, player, stack, aimed, old);
            if (planned == null) {
                return null;
            }
            plan = leavesAGap(planned) ? refused(planned, Refusal.FastReplace.LEAVES_A_GAP) : planned;
            back = builder.refund(level, aimed, old);
        }

        Charge charge = Charge.of(player, item, 1, back.isEmpty() ? List.of() : List.of(back));
        // Where the player may not build, the replace is refused whatever else refused it.
        List<BlockPos> positions = plan.blocks().stream().map(PlacementPlan.Placed::pos).toList();
        if (player != null && !Charge.mayBuildAll(level, player, positions)) {
            plan = refused(plan, Refusal.FastReplace.MAY_NOT_BUILD);
        } else if (player != null && !plan.isRefused() && !charge.fits(player)) {
            plan = refused(plan, Refusal.FastReplace.NO_ROOM_TO_RETURN);
        }
        return new Replacing(plan, charge, builder);
    }

    /**
     * The held item's own plan for the aimed block, marked as replacing it, with the old block's
     * orientation: refused where the item plans no block there.
     */
    private static PlacementPlan itemPlan(Level level, @Nullable Player player, InteractionHand hand, ItemStack stack,
                                          BlockHitResult hit, BlockItem item, BlockState old) {
        BlockPos aimed = hit.getBlockPos();
        PlacementPlan planned = Placements.planFor(item, new ReplacingContext(level, player, hand, stack, hit));
        if (planned == null) {
            return PlacementPlan.refused(aimed, item.getBlock().defaultBlockState(), Refusal.FastReplace.PLANS_ELSEWHERE);
        }
        if (planned.blocks().stream().noneMatch(placed -> placed.pos().equals(aimed))) {
            return PlacementPlan.refused(planned.blocks(), Refusal.FastReplace.PLANS_ELSEWHERE);
        }
        return new PlacementPlan(keepingOrientation(planned.blocks(), aimed, old, stack), List.of(aimed), planned.refusal());
    }

    /** Whether the plan replaces a position it puts no block at. */
    private static boolean leavesAGap(PlacementPlan plan) {
        return !plan.blocks().stream().map(PlacementPlan.Placed::pos).toList().containsAll(plan.replaces());
    }

    /**
     * The plan's blocks, the one at {@code aimed} taking its state from {@code old} where the held
     * stack carries no {@linkplain Rotate#turnOf(ItemStack) turn}: a turn re-orients the replace, so
     * the new block is what the item's plan makes of the turned look. The preview draws these
     * states and the click lays them. Any other block of the plan is the item's own, as a builder's
     * would set its own states.
     */
    private static List<PlacementPlan.Placed> keepingOrientation(List<PlacementPlan.Placed> blocks, BlockPos aimed,
                                                                 BlockState old, ItemStack stack) {
        if (Rotate.turnOf(stack).equals(QuarterTurn.NONE)) {
            return blocks.stream()
                    .map(placed -> placed.pos().equals(aimed) ? new PlacementPlan.Placed(aimed, copied(placed.state(), old)) : placed)
                    .toList();
        }
        return blocks;
    }

    /**
     * {@code to} with each orientation it shares with {@code from} set to {@code from}'s value. A
     * property is shared when it is the same {@link Property} object, never when it only has the
     * same name, as vanilla's blocks and a Consumer's share theirs; the same object has the same
     * values, so the new block always accepts the old value. Any other property keeps its value.
     */
    private static BlockState copied(BlockState to, BlockState from) {
        BlockState copy = to;
        for (Property<?> property : from.getProperties()) {
            if (Rotate.isOrientation(property)) {
                copy = copied(copy, from, property);
            }
        }
        return copy;
    }

    private static <T extends Comparable<T>> BlockState copied(BlockState to, BlockState from, Property<T> property) {
        return to.trySetValue(property, from.getValue(property));
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
            PlacementPlan plan = guarded(level, player, replacing.plan());
            if (plan.isRefused()) {
                tell(player, message(replacing.builder(), plan.refusal()));
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

    /**
     * The plan, refused as may-not-build when a protection mod cancels the place event at any
     * position it puts a block. The claim guard Rotate uses: NeoForge's event, fired once for each
     * position, before the swap, which a claim mod listens to and a plan asked every frame by the
     * preview must not fire. It is asked only of a replace that would go through, as Rotate asks
     * only of a turn it will make, so no event is heard for a block that never goes down.
     */
    private static PlacementPlan guarded(Level level, Player player, PlacementPlan plan) {
        if (plan.isRefused()) {
            return plan;
        }
        for (PlacementPlan.Placed placed : plan.blocks()) {
            BlockSnapshot before = BlockSnapshot.create(level.dimension(), level, placed.pos());
            if (EventHooks.onBlockPlace(player, before, Direction.UP)) {
                return refused(plan, Refusal.FastReplace.MAY_NOT_BUILD);
            }
        }
        return plan;
    }

    /** The same plan, refused for {@code reason}. */
    private static PlacementPlan refused(PlacementPlan plan, Refusal reason) {
        return new PlacementPlan(plan.blocks(), plan.replaces(), reason);
    }

    /**
     * What the player is told of a refused replace: the library's own reason, or else the group's
     * builder's, and where there is none, that the held block can't go there.
     */
    static Component message(@Nullable ReplaceBuilder builder, Refusal refusal) {
        Refusal reason = refusal instanceof Refusal.At at ? at.reason() : refusal;
        if (!(reason instanceof Refusal.FastReplace fastReplace)) {
            return builder == null ? Component.translatable("message.groundworks.fast_replace_refused") : builder.message(reason);
        }
        return switch (fastReplace) {
            case NO_ROOM_TO_RETURN -> Component.translatable("message.groundworks.fast_replace_no_room_to_return");
            case MAY_NOT_BUILD -> Component.translatable("message.groundworks.fast_replace_may_not_build");
            case PLANS_ELSEWHERE -> Component.translatable("message.groundworks.fast_replace_plans_elsewhere");
            case LEAVES_A_GAP -> Component.translatable("message.groundworks.fast_replace_leaves_a_gap");
        };
    }

    private static void tell(Player player, Component message) {
        if (player instanceof ServerPlayer server) {
            server.sendSystemMessage(message, true);
        }
    }
}
