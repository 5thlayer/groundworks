// SPDX-FileCopyrightText: 2026 5thlayer
// SPDX-License-Identifier: MIT

package io.github._5thlayer.groundworks;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Predicate;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.BlockHitResult;
import org.jspecify.annotations.Nullable;

/**
 * The Stretch: the blocks one drag of a held item lays, planned, charged, laid and refused whole.
 * The {@link LegBuilder}s are registered here, and Groundworks runs the gesture for every
 * stretch-able item alike.
 *
 * <p>A sneak-click with no start stored stores the start where the aim would place, at the held
 * height, and the look turned by the held turn, using both up. A sneak-click with a start stored
 * adds an anchor where the stretch would end, freezing the height into the leg ending there; a
 * refused stretch stores nothing, and an anchor at the same spot as the last is ignored. A click
 * lays the stretch. A sneak-use in the air clears it. Laying and clearing reset the height, and
 * leave a turn pressed since the start for the next one. A click with nothing stored passes on, so
 * the item keeps its own placement. A stretch stored in another dimension is no stretch.
 *
 * <p>The aim picks only where an anchor lies seen from above; the stretch's height there is the
 * start's and each leg's rise before it (ADR 0004). The route is {@link StretchRoute}'s, and each
 * leg is its builder's, gone round what the builder refuses at a position by a {@link Detours
 * detour} when one clears it.
 *
 * <p>Laying charges one held item per placed block, nothing in creative, and hands each replaced
 * block back to the inventory. A stretch the inventory can't pay for, or has no room to take back
 * from, is refused whole.
 */
public final class Stretches {

    static final String STARTED = "message.groundworks.stretch_started";
    static final String ANCHORED = "message.groundworks.stretch_anchored";
    static final String CLEARED = "message.groundworks.stretch_cleared";

    private static final List<LegBuilder> BUILDERS = new CopyOnWriteArrayList<>();

    private Stretches() {
    }

    /**
     * Adds a builder. An item's legs are built by the first registered builder that claims it.
     *
     * <p>Called at mod construction, on both sides, since the preview asks on the client and the
     * click on the server. Mods are constructed in parallel, so this may be called from several
     * threads at once.
     */
    public static void register(LegBuilder builder) {
        BUILDERS.add(builder);
    }

    /** The builder of this item's legs, or {@code null} if none claims it, and so it doesn't stretch. */
    public static @Nullable LegBuilder builderOf(Item item) {
        for (LegBuilder builder : BUILDERS) {
            if (builder.claims(item)) {
                return builder;
            }
        }
        return null;
    }

    /** The stretch being drawn with the held stack, or {@code null} when none is, or it is in another dimension. */
    public static @Nullable StoredStretch storedOn(Level level, ItemStack stack) {
        StoredStretch stored = stack.get(Groundworks.STRETCH.get());
        return stored != null && stored.dimension().equals(level.dimension()) ? stored : null;
    }

    /**
     * Where a sneak-click at this hit would store the start, at the height held, or {@code null}
     * when it wouldn't: the player isn't sneaking, the stack doesn't stretch, or a stretch is
     * already being drawn with it. Such a click places nothing, so it has no plan, and the
     * preview draws the start instead, with the look it would store.
     */
    public static @Nullable BlockPos startAt(Level level, Player player, ItemStack stack, BlockHitResult hit) {
        StoredStretch started = startedAt(level, player, stack, hit);
        return started == null ? null : started.start();
    }

    /**
     * The stretch a sneak-click at this hit would store, its start and its look turned by the held
     * stack's {@linkplain Rotate#turnOf(ItemStack) turn}, or {@code null} when it wouldn't, as
     * {@link #startAt} has it.
     */
    public static @Nullable StoredStretch startedAt(Level level, Player player, ItemStack stack, BlockHitResult hit) {
        if (!player.isShiftKeyDown() || builderOf(stack.getItem()) == null || storedOn(level, stack) != null) {
            return null;
        }
        return new StretchState(null, Raise.heightOf(player, stack), Rotate.turnOf(stack))
                .started(level.dimension(), spotOf(level, player, stack, hit), player.getDirection())
                .stored();
    }

    private static StretchState stateOf(Level level, Player player, ItemStack stack) {
        return new StretchState(storedOn(level, stack), Raise.heightOf(player, stack), Rotate.turnOf(stack));
    }

    /**
     * What a click with the held stack at this hit would lay, or {@code null} when no stretch is
     * being drawn with it. Safe on either side, reading the world without touching it.
     */
    static @Nullable PlacementPlan planFor(Level level, @Nullable Player player, ItemStack stack, BlockHitResult hit) {
        Laying laying = laying(level, player, stack, hit);
        return laying == null ? null : laying.plan();
    }

    /** A stretch's plan, what laying it charges, and what it hands back. */
    private record Laying(PlacementPlan plan, int cost, List<ItemStack> returned) {
    }

    private static @Nullable Laying laying(Level level, @Nullable Player player, ItemStack stack, BlockHitResult hit) {
        Item item = stack.getItem();
        LegBuilder builder = builderOf(item);
        StoredStretch stored = storedOn(level, stack);
        if (builder == null || stored == null) {
            return null;
        }
        List<BlockPos> ends = new ArrayList<>(stored.anchorPositions());
        List<Integer> rises = new ArrayList<>(stored.anchors().stream().map(StoredStretch.Anchor::rise).toList());
        ends.add(spotOf(level, player, stack, hit));
        rises.add(player == null ? 0 : Raise.heightOf(player, stack).blocks());
        Refusal refusal = null;
        boolean reachesEnd = true;
        List<List<Leg.Column>> route = StretchRoute.legs(stored.start(), stored.look(), ends);
        if (route == null) {
            // Behind the look, the stretch is drawn refused up to its last anchor, or at its start alone.
            refusal = Refusal.Stretch.BEHIND_THE_LOOK;
            reachesEnd = false;
            ends.removeLast();
            rises.removeLast();
            route = ends.isEmpty() ? null : StretchRoute.legs(stored.start(), stored.look(), ends);
            if (route == null) {
                route = List.of(List.of(new Leg.Column(stored.start().getX(), stored.start().getZ(), stored.look())));
                rises = List.of(0);
            }
        }

        // A later leg's block at a position an earlier one takes, its first anchor, is the later leg's.
        Map<BlockPos, BlockState> blocks = new LinkedHashMap<>();
        Set<BlockPos> replaces = new LinkedHashSet<>();
        for (Leg leg : Leg.ofStretch(stored.start(), route, rises, reachesEnd)) {
            PlacementPlan built = Detours.plan(leg, player == null ? null : player.blockPosition(),
                    detour -> builder.build(level, item, detour));
            for (PlacementPlan.Placed placed : built.blocks()) {
                blocks.put(placed.pos(), placed.state());
                if (built.replaces().contains(placed.pos())) {
                    replaces.add(placed.pos());
                } else {
                    replaces.remove(placed.pos());
                }
            }
            if (refusal == null) {
                refusal = built.refusal();
            }
        }

        int cost = 0;
        List<ItemStack> returned = new ArrayList<>();
        if (player != null && !player.hasInfiniteMaterials()) {
            cost = blocks.size();
            for (BlockPos pos : replaces) {
                Item back = level.getBlockState(pos).getBlock().asItem();
                if (back != Items.AIR) {
                    returned.add(new ItemStack(back));
                }
            }
            if (refusal == null) {
                if (ContainerHelper.clearOrCountMatchingItems(player.getInventory(), isOf(item), 0, true) < cost) {
                    refusal = Refusal.Stretch.NOT_ENOUGH_ITEMS;
                } else if (!fits(player, item, cost, returned)) {
                    refusal = Refusal.Stretch.NO_ROOM_TO_RETURN;
                }
            }
        }
        List<PlacementPlan.Placed> placed = blocks.entrySet().stream()
                .map(entry -> new PlacementPlan.Placed(entry.getKey(), entry.getValue()))
                .toList();
        return new Laying(new PlacementPlan(placed, List.copyOf(replaces), refusal), cost, returned);
    }

    /**
     * Where a placement at this hit would go with no height: the aimed block if it is replaceable,
     * the spot across the hit face otherwise, as vanilla has it. The context's own position is
     * moved by the held stack's height, which in a stretch is a leg's rise, so it is not asked.
     */
    private static BlockPos spotOf(Level level, @Nullable Player player, ItemStack stack, BlockHitResult hit) {
        BlockPlaceContext context = new BlockPlaceContext(level, player, InteractionHand.MAIN_HAND, stack, hit);
        return context.replacingClickedOnBlock() ? hit.getBlockPos() : hit.getBlockPos().relative(hit.getDirection());
    }

    /** What a click on a block does, by whether it sneaks and whether a stretch is being drawn. */
    enum Click {
        START,
        ANCHOR,
        LAY,
        PASS;

        static Click of(boolean sneaking, boolean stored) {
            if (sneaking) {
                return stored ? ANCHOR : START;
            }
            return stored ? LAY : PASS;
        }
    }

    /**
     * A click on a block with the main hand, or PASS where the Stretch has nothing to say and the
     * click goes on as it would. The main hand only, since that is the hand the preview draws.
     */
    static InteractionResult useOn(Player player, InteractionHand hand, BlockHitResult hit) {
        ItemStack held = player.getItemInHand(hand);
        if (hand != InteractionHand.MAIN_HAND || builderOf(held.getItem()) == null) {
            return InteractionResult.PASS;
        }
        Level level = player.level();
        StretchState state = stateOf(level, player, held);
        Click click = Click.of(player.isShiftKeyDown(), state.stored() != null);
        if (click == Click.PASS) {
            return InteractionResult.PASS;
        }
        if (level.isClientSide()) {
            return InteractionResult.SUCCESS;
        }
        switch (click) {
            case START -> {
                store(held, state.started(level.dimension(), spotOf(level, player, held, hit), player.getDirection()));
                tell(player, Component.translatable(STARTED));
            }
            case ANCHOR -> {
                StretchState anchored = state.anchored(spotOf(level, player, held, hit));
                if (anchored == state) {
                    break;
                }
                PlacementPlan plan = laying(level, player, held, hit).plan();
                if (plan.isRefused()) {
                    tell(player, message(held, plan.refusal()));
                } else {
                    store(held, anchored);
                    tell(player, Component.translatable(ANCHORED));
                }
            }
            default -> {
                Laying laying = laying(level, player, held, hit);
                if (laying.plan().isRefused()) {
                    tell(player, message(held, laying.plan().refusal()));
                } else {
                    lay(laying, level, held, player, state.reset());
                }
            }
        }
        return InteractionResult.SUCCESS;
    }

    /** A use in the air: a sneak clears the stretch being drawn, and its height with it, but not the turn. */
    static InteractionResult use(Player player, InteractionHand hand) {
        ItemStack held = player.getItemInHand(hand);
        if (hand != InteractionHand.MAIN_HAND || !player.isShiftKeyDown() || builderOf(held.getItem()) == null
                || !held.has(Groundworks.STRETCH.get())) {
            return InteractionResult.PASS;
        }
        if (!player.level().isClientSide()) {
            store(held, stateOf(player.level(), player, held).reset());
            tell(player, Component.translatable(CLEARED));
        }
        return InteractionResult.SUCCESS;
    }

    /** What the player is told of a refusal: Groundworks' own, or the builder's, at a position or not. */
    static Component message(ItemStack held, Refusal refusal) {
        Refusal reason = refusal instanceof Refusal.At at ? at.reason() : refusal;
        if (!(reason instanceof Refusal.Stretch stretch)) {
            return builderOf(held.getItem()).message(reason);
        }
        return switch (stretch) {
            case BEHIND_THE_LOOK -> Component.translatable("message.groundworks.stretch_behind_the_look");
            case NOT_ENOUGH_ITEMS -> Component.translatable("message.groundworks.stretch_not_enough_items",
                    held.getHoverName());
            case NO_ROOM_TO_RETURN -> Component.translatable("message.groundworks.stretch_no_room_to_return");
        };
    }

    // The state is reset before the charge, which may take the held stack's last item.
    private static void lay(Laying laying, Level level, ItemStack held, Player player, StretchState reset) {
        Item item = held.getItem();
        store(held, reset);
        if (laying.cost() > 0) {
            ContainerHelper.clearOrCountMatchingItems(player.getInventory(), isOf(item), laying.cost(), false);
        }
        for (PlacementPlan.Placed placed : laying.plan().blocks()) {
            level.setBlock(placed.pos(), placed.state(), Block.UPDATE_ALL);
        }
        for (ItemStack stack : laying.returned()) {
            player.getInventory().placeItemBackInInventory(stack);
        }
        if (laying.plan().blocks().isEmpty()) {
            return;
        }
        PlacementPlan.Placed first = laying.plan().blocks().getFirst();
        SoundType sound = first.state().getSoundType();
        level.playSound(null, first.pos(), sound.getPlaceSound(), SoundSource.BLOCKS,
                (sound.getVolume() + 1.0F) / 2.0F, sound.getPitch() * 0.8F);
        level.gameEvent(GameEvent.BLOCK_PLACE, first.pos(), GameEvent.Context.of(player, first.state()));
    }

    // No stretch, no height and no turn are no components, so a stack laid or cleared stacks again with one never used.
    private static void store(ItemStack stack, StretchState state) {
        if (state.stored() == null) {
            stack.remove(Groundworks.STRETCH.get());
        } else {
            stack.set(Groundworks.STRETCH.get(), state.stored());
        }
        Raise.setHeight(stack, state.height());
        Rotate.setTurn(stack, state.turn());
    }

    /**
     * Whether the inventory holds what the stretch hands back once its charge is taken, as
     * {@code Inventory#placeItemBackInInventory} would place it, in the main inventory.
     */
    private static boolean fits(Player player, Item item, int cost, List<ItemStack> returned) {
        if (returned.isEmpty()) {
            return true;
        }
        List<ItemStack> slots = new ArrayList<>();
        for (ItemStack slot : player.getInventory().getNonEquipmentItems()) {
            slots.add(slot.copy());
        }
        int toTake = cost;
        for (ItemStack slot : slots) {
            if (toTake == 0) {
                break;
            }
            if (slot.is(item)) {
                int taken = Math.min(toTake, slot.getCount());
                slot.shrink(taken);
                toTake -= taken;
            }
        }
        for (ItemStack back : returned) {
            ItemStack rest = back.copy();
            for (ItemStack slot : slots) {
                if (!slot.isEmpty() && ItemStack.isSameItemSameComponents(slot, rest)) {
                    int moved = Math.max(0, Math.min(rest.getCount(), slot.getMaxStackSize() - slot.getCount()));
                    slot.grow(moved);
                    rest.shrink(moved);
                }
            }
            for (int i = 0; i < slots.size() && !rest.isEmpty(); i++) {
                if (slots.get(i).isEmpty()) {
                    slots.set(i, rest.split(rest.getMaxStackSize()));
                }
            }
            if (!rest.isEmpty()) {
                return false;
            }
        }
        return true;
    }

    private static Predicate<ItemStack> isOf(Item item) {
        return stack -> stack.is(item);
    }

    private static void tell(Player player, Component message) {
        if (player instanceof ServerPlayer server) {
            server.sendSystemMessage(message, true);
        }
    }
}
