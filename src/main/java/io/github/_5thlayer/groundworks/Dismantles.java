// SPDX-FileCopyrightText: 2026 5thlayer
// SPDX-License-Identifier: MIT

package io.github._5thlayer.groundworks;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Function;
import java.util.function.Predicate;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.TagKey;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;

/**
 * The Dismantle: taking up a span of one {@link DismantleFamily} from a start to an end, with a tool
 * the family {@linkplain DismantleFamily#acceptsTool accepts} for every block the span takes, by
 * default one in {@link #TOOLS}. A click with a tool no family accepts at the clicked block, and
 * nothing stored, passes on to the tool's own use. The families are registered here, and Groundworks runs the gesture for all of
 * them alike.
 *
 * <p>A sneak-click on a member stores the start on the held stack. A sneak-click with a live start
 * queues the span from it to the clicked block and clears the start, or tells the player why not and
 * leaves the start where it is. A click with a live start or queued spans confirms the pass: the
 * queued spans and the span the click ends at the start, if there is one, all planned again and
 * taken up together as one {@link DismantlePass}, or none of them with the first one's reason. A
 * click with nothing stored passes on, so the tool keeps its own use. A sneak-use in the air clears
 * the start and the queue. A start is dead, and so no start, when its block is gone or its family no
 * longer counts it the same start, or when it is in another dimension; a queued span whose start is
 * dead refuses the pass.
 *
 * <p>A pass takes at most {@link #MAX_SPANS} spans, counting the one the start in progress ends.
 *
 * <p>Taking up a pass clears the start and the queue, runs each family's {@linkplain
 * DismantleFamily#beforeTaking hook} once over its positions, then collects each taken block's drops
 * with the held tool and removes it with no drops in the world, then hands everything to the
 * inventory, dropping what doesn't fit at the player's feet. A creative player is handed nothing.
 */
public final class Dismantles {

    /**
     * The tools a family {@linkplain DismantleFamily#acceptsTool accepts} by default. It ships empty:
     * each Consumer adds its own, and a pack trims them. Vanilla's families take vanilla's tools.
     */
    public static final TagKey<Item> TOOLS = TagKey.create(Registries.ITEM,
            Identifier.fromNamespaceAndPath(Groundworks.MOD_ID, "dismantles"));

    /**
     * The most spans a pass takes, counting the one a click ends. It is two until a confirm of many
     * long spans is profiled: the preview plans every stored span again each frame, and a confirm
     * rebuilds each span's line in one tick.
     */
    public static final int MAX_SPANS = 2;

    private static final List<DismantleFamily> FAMILIES = new CopyOnWriteArrayList<>();

    private Dismantles() {
    }

    /**
     * Adds a family. A block belongs to the first registered family that claims it, and families
     * are expected not to overlap.
     *
     * <p>Called at mod construction, on both sides, since the preview asks on the client and the
     * click on the server. Mods are constructed in parallel, so this may be called from several
     * threads at once.
     */
    public static void register(DismantleFamily family) {
        FAMILIES.add(family);
    }

    /** The family this block belongs to, or {@code null} if none claims it. */
    public static @Nullable DismantleFamily familyOf(BlockState state) {
        return familyOf(FAMILIES, state);
    }

    /** The held stack's stored start, or {@code null} when none is stored or it is dead. */
    public static @Nullable DismantleStart liveStart(Level level, ItemStack held) {
        DismantleStart start = held.get(Groundworks.DISMANTLE_START.get());
        if (start == null) {
            return null;
        }
        return isLive(start, familyOf(start.state()), level.dimension(), level::getBlockState) ? start : null;
    }

    /** The held stack's queued spans, oldest first, live or not. */
    public static List<QueuedSpan> queued(ItemStack held) {
        return held.getOrDefault(Groundworks.DISMANTLE_QUEUE.get(), List.of());
    }

    /** Whether the held stack stores anything a click would confirm or a sneak-use clear. */
    public static boolean hasStored(ItemStack held) {
        return held.has(Groundworks.DISMANTLE_START.get()) || !queued(held).isEmpty();
    }

    /** Whether one more span, queued or ended by a click, fits in a pass beside {@code queued} spans. */
    static boolean hasRoom(int queued) {
        return queued < MAX_SPANS;
    }

    /**
     * The pass a click aimed at {@code aimed} would confirm: every queued span, then the span from
     * the live start to the aim, if there is a live start and an aim, all planned now. With no aim
     * the start is left out, as the preview draws it alone. It is {@code null} when
     * {@code held} stores nothing to confirm.
     */
    public static @Nullable DismantlePass passTo(Level level, ItemStack held, @Nullable BlockPos aimed) {
        DismantleStart start = liveStart(level, held);
        List<QueuedSpan> queued = queued(held);
        if (start == null && queued.isEmpty()) {
            return null;
        }
        List<DismantlePass.Planned> spans = new ArrayList<>();
        for (QueuedSpan span : queued) {
            spans.add(plan(level, held, span));
        }
        if (start != null && aimed != null) {
            spans.add(new DismantlePass.Planned(familyOf(start.state()), start, spanFrom(level, held, start, aimed)));
        }
        return DismantlePass.of(spans);
    }

    // A queued span is planned again from its stored ends; its end is the one named when it was queued.
    private static DismantlePass.Planned plan(Level level, ItemStack held, QueuedSpan queued) {
        DismantleStart start = queued.start();
        DismantleFamily family = familyOf(start.state());
        if (!isLive(start, family, level.dimension(), level::getBlockState)) {
            return new DismantlePass.Planned(family, start, DismantleSpan.refused(Refusal.Dismantle.START_GONE));
        }
        return new DismantlePass.Planned(family, start,
                span(family, level, start.pos(), queued.end(), level.getBlockState(queued.end()), accepts(family, level, held)));
    }

    /**
     * What a click aimed at {@code aimed} would take up, or {@code null} when {@code held} has no
     * live start. The end is the block the start's family names there.
     */
    public static @Nullable DismantleSpan spanTo(Level level, ItemStack held, BlockPos aimed) {
        DismantleStart start = liveStart(level, held);
        return start == null ? null : spanFrom(level, held, start, aimed);
    }

    // A live start's family is there, or it would not be live.
    private static DismantleSpan spanFrom(Level level, ItemStack held, DismantleStart start, BlockPos aimed) {
        DismantleFamily family = familyOf(start.state());
        BlockPos end = family.names(level, aimed);
        return span(family, level, start.pos(), end, level.getBlockState(end), accepts(family, level, held));
    }

    /** Whether the family accepts the held tool for the block at a position, as it stands now. */
    private static Predicate<BlockPos> accepts(DismantleFamily family, Level level, ItemStack held) {
        return pos -> family.acceptsTool(held, level.getBlockState(pos));
    }

    static @Nullable DismantleFamily familyOf(List<? extends DismantleFamily> families, BlockState state) {
        for (DismantleFamily family : families) {
            if (family.claims(state)) {
                return family;
            }
        }
        return null;
    }

    /** The dimension is asked first, so a start elsewhere never reads this world at its position. */
    static boolean isLive(DismantleStart start, @Nullable DismantleFamily family, ResourceKey<Level> dimension,
                          Function<BlockPos, BlockState> world) {
        return family != null
                && start.dimension().equals(dimension)
                && family.isSameStart(start.state(), world.apply(start.pos()));
    }

    /**
     * The family's span, refused as not the same kind before the family is asked, and for the tool
     * when {@code accepts} refuses any position it takes.
     */
    static DismantleSpan span(DismantleFamily family, Level level, BlockPos start, BlockPos end, BlockState endState,
                              Predicate<BlockPos> accepts) {
        if (!family.claims(endState)) {
            return DismantleSpan.refused(Refusal.Dismantle.NOT_SAME_KIND);
        }
        DismantleSpan span = family.span(level, start, end);
        if (!span.takes().stream().allMatch(accepts)) {
            return DismantleSpan.refused(Refusal.Dismantle.WRONG_TOOL);
        }
        return span;
    }

    /**
     * Not the same kind names the start's block, the kind the end would have had to be. A gone
     * start may have no family left to ask, so the library tells it.
     */
    static Component message(@Nullable DismantleFamily family, Component startName, Refusal refusal) {
        if (refusal == Refusal.Dismantle.NOT_SAME_KIND) {
            return Component.translatable("message.groundworks.dismantle_not_same_kind", startName);
        }
        if (refusal == Refusal.Dismantle.WRONG_TOOL) {
            return Component.translatable("message.groundworks.dismantle_wrong_tool");
        }
        if (refusal == Refusal.Dismantle.START_GONE) {
            return Component.translatable("message.groundworks.dismantle_start_gone", startName);
        }
        return family.message(refusal);
    }

    /**
     * What a click on a block does, by whether it sneaks, aims at a member, has a live start, and
     * has spans queued.
     */
    enum Click {
        STORE,
        QUEUE,
        CONFIRM,
        PASS;

        static Click of(boolean sneaking, boolean aimsAtMember, boolean liveStart, boolean queued) {
            if (sneaking) {
                if (liveStart) {
                    return QUEUE;
                }
                return aimsAtMember ? STORE : PASS;
            }
            return liveStart || queued ? CONFIRM : PASS;
        }
    }

    /** A member a click names: its family, where it is and what stands there. */
    private record Member(DismantleFamily family, BlockPos pos, BlockState state) {
    }

    /** The member a click names, if its family accepts the held tool for it. */
    private static @Nullable Member memberAt(Level level, BlockPos clicked, ItemStack held) {
        for (DismantleFamily family : FAMILIES) {
            BlockPos named = family.names(level, clicked);
            BlockState state = level.getBlockState(named);
            if (family.claims(state)) {
                return family.acceptsTool(held, state) ? new Member(family, named.immutable(), state) : null;
            }
        }
        return null;
    }

    /**
     * A click on a block, or PASS where the Dismantle has nothing to say and the click goes on as
     * it would. With a live start or a queued span every click on a block is the Dismantle's, as the
     * preview shows the Dismantle whatever the aim. Only a tool a family has accepted stores
     * anything, so with nothing stored a click on a block no family accepts the tool for passes.
     */
    static InteractionResult useOn(Player player, InteractionHand hand, BlockPos pos) {
        ItemStack held = player.getItemInHand(hand);
        Level level = player.level();
        Member member = memberAt(level, pos, held);
        DismantleStart start = liveStart(level, held);
        List<QueuedSpan> queued = queued(held);
        switch (Click.of(player.isShiftKeyDown(), member != null, start != null, !queued.isEmpty())) {
            case STORE -> {
                if (!level.isClientSide()) {
                    if (!hasRoom(queued.size())) {
                        tell(player, Component.translatable("message.groundworks.dismantle_queue_full", MAX_SPANS));
                    } else {
                        held.set(Groundworks.DISMANTLE_START.get(), new DismantleStart(level.dimension(), member.pos(), member.state()));
                        tell(player, Component.translatable("message.groundworks.dismantle_started"));
                    }
                }
                return InteractionResult.SUCCESS;
            }
            case QUEUE -> {
                if (!level.isClientSide()) {
                    queue(player, level, held, start, queued, pos);
                }
                return InteractionResult.SUCCESS;
            }
            case CONFIRM -> {
                if (!level.isClientSide()) {
                    // A plain click with only queued spans confirms them wherever it aims.
                    DismantlePass pass = passTo(level, held, start == null ? null : pos);
                    if (pass.isRefused()) {
                        DismantlePass.Planned refused = pass.refused();
                        tell(player, message(refused.family(), refused.start().state().getBlock().getName(), refused.span().refusal()));
                    } else {
                        takeUp(pass, level, held, player);
                    }
                }
                return InteractionResult.SUCCESS;
            }
            default -> {
                return InteractionResult.PASS;
            }
        }
    }

    // A refused span, or one past the limit, leaves the start where it is.
    private static void queue(Player player, Level level, ItemStack held, DismantleStart start,
                              List<QueuedSpan> queued, BlockPos clicked) {
        DismantleFamily family = familyOf(start.state());
        BlockPos end = family.names(level, clicked);
        DismantleSpan span = span(family, level, start.pos(), end, level.getBlockState(end), accepts(family, level, held));
        if (span.isRefused()) {
            tell(player, message(family, start.state().getBlock().getName(), span.refusal()));
            return;
        }
        if (!hasRoom(queued.size())) {
            tell(player, Component.translatable("message.groundworks.dismantle_queue_full", MAX_SPANS));
            return;
        }
        List<QueuedSpan> longer = new ArrayList<>(queued);
        longer.add(new QueuedSpan(start, end));
        held.set(Groundworks.DISMANTLE_QUEUE.get(), List.copyOf(longer));
        held.remove(Groundworks.DISMANTLE_START.get());
        tell(player, Component.translatable("message.groundworks.dismantle_queued"));
    }

    /** A use in the air: a sneak clears the stored start and every queued span. */
    static InteractionResult use(Player player, InteractionHand hand) {
        ItemStack held = player.getItemInHand(hand);
        if (!player.isShiftKeyDown() || !hasStored(held)) {
            return InteractionResult.PASS;
        }
        if (!player.level().isClientSide()) {
            clear(held);
            tell(player, Component.translatable("message.groundworks.dismantle_cleared"));
        }
        return InteractionResult.SUCCESS;
    }

    private static void clear(ItemStack held) {
        held.remove(Groundworks.DISMANTLE_START.get());
        held.remove(Groundworks.DISMANTLE_QUEUE.get());
    }

    // Every family's hook runs before any removal, so a removal's side effects -- a belt line
    // rebuilding -- can't hand a taken block's share to one outside the pass. Each block's drops are
    // collected just before it goes, from the state it is in then.
    private static void takeUp(DismantlePass pass, Level level, ItemStack held, Player player) {
        clear(held);
        List<ItemStack> handed = new ArrayList<>();
        pass.takes().forEach((family, taken) -> handed.addAll(family.beforeTaking(level, taken)));
        boolean handsOver = !player.hasInfiniteMaterials();
        for (List<BlockPos> taken : pass.takes().values()) {
            for (BlockPos pos : taken) {
                BlockState state = level.getBlockState(pos);
                if (state.isAir()) {
                    continue;
                }
                if (handsOver && level instanceof ServerLevel server) {
                    handed.addAll(Block.getDrops(state, server, pos, level.getBlockEntity(pos), player, held));
                }
                level.destroyBlock(pos, false, player);
            }
        }
        if (!handsOver) {
            return;
        }
        for (ItemStack stack : handed) {
            // The inventory first, and what doesn't fit at the player's feet.
            player.getInventory().placeItemBackInInventory(stack);
        }
    }

    private static void tell(Player player, Component message) {
        if (player instanceof ServerPlayer server) {
            server.sendSystemMessage(message, true);
        }
    }
}
