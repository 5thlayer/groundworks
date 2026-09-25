// SPDX-FileCopyrightText: 2026 5thlayer
// SPDX-License-Identifier: MIT

package io.github._5thlayer.groundworks;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Function;

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
 * in {@link #TOOLS}. The families are registered here, and Groundworks runs the gesture for all of
 * them alike.
 *
 * <p>A sneak-click on a member stores the start on the held stack, and moves it when one is
 * stored. A click with a live start names the end and takes up the span to it, or tells the player
 * why not. A click with nothing stored passes on, so the tool keeps its own use. A sneak-use in the
 * air clears the start. A start is dead, and so no start, when its block is gone or its family no
 * longer counts it the same start, or when it is in another dimension.
 *
 * <p>Taking up a span clears the start, runs the family's {@linkplain DismantleFamily#beforeTaking
 * hook}, collects each taken block's drops with the held tool and removes it with no drops in the
 * world, then hands everything to the inventory, dropping what doesn't fit at the player's feet. A
 * creative player is handed nothing.
 */
public final class Dismantles {

    /** The tools a Dismantle answers. It ships empty: each Consumer adds its own, and a pack trims them. */
    public static final TagKey<Item> TOOLS = TagKey.create(Registries.ITEM,
            Identifier.fromNamespaceAndPath(Groundworks.MOD_ID, "dismantles"));

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

    public static boolean isTool(ItemStack stack) {
        return stack.is(TOOLS);
    }

    /** The held stack's stored start, or {@code null} when none is stored or it is dead. */
    public static @Nullable DismantleStart liveStart(Level level, ItemStack held) {
        DismantleStart start = held.get(Groundworks.DISMANTLE_START.get());
        if (start == null) {
            return null;
        }
        return isLive(start, familyOf(start.state()), level.dimension(), level::getBlockState) ? start : null;
    }

    /**
     * What a click aimed at {@code aimed} would take up, or {@code null} when {@code held} is no
     * dismantling tool or has no live start. The end is the block the start's family names there.
     */
    public static @Nullable DismantleSpan spanTo(Level level, ItemStack held, BlockPos aimed) {
        if (!isTool(held)) {
            return null;
        }
        DismantleStart start = liveStart(level, held);
        return start == null ? null : spanFrom(level, start, aimed);
    }

    // A live start's family is there, or it would not be live.
    private static DismantleSpan spanFrom(Level level, DismantleStart start, BlockPos aimed) {
        DismantleFamily family = familyOf(start.state());
        BlockPos end = family.names(level, aimed);
        return span(family, level, start.pos(), end, level.getBlockState(end));
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

    static DismantleSpan span(DismantleFamily family, Level level, BlockPos start, BlockPos end, BlockState endState) {
        if (!family.claims(endState)) {
            return DismantleSpan.refused(Refusal.Dismantle.NOT_SAME_KIND);
        }
        return family.span(level, start, end);
    }

    static Component message(DismantleFamily family, Refusal refusal) {
        return refusal == Refusal.Dismantle.NOT_SAME_KIND
                ? Component.translatable("message.groundworks.dismantle_not_same_kind")
                : family.message(refusal);
    }

    /** What a click on a block does, by whether it sneaks, aims at a member, and has a live start. */
    enum Click {
        STORE,
        TAKE_UP,
        PASS;

        static Click of(boolean sneaking, boolean aimsAtMember, boolean liveStart) {
            if (sneaking) {
                return aimsAtMember ? STORE : PASS;
            }
            return liveStart ? TAKE_UP : PASS;
        }
    }

    /** A member a click names: its family, where it is and what stands there. */
    private record Member(DismantleFamily family, BlockPos pos, BlockState state) {
    }

    private static @Nullable Member memberAt(Level level, BlockPos clicked) {
        for (DismantleFamily family : FAMILIES) {
            BlockPos named = family.names(level, clicked);
            BlockState state = level.getBlockState(named);
            if (family.claims(state)) {
                return new Member(family, named.immutable(), state);
            }
        }
        return null;
    }

    /**
     * A click on a block, or PASS where the Dismantle has nothing to say and the click goes on as
     * it would. With a live start every click on a block is the Dismantle's, as the preview shows
     * the Dismantle whatever the aim.
     */
    static InteractionResult useOn(Player player, InteractionHand hand, BlockPos pos) {
        ItemStack held = player.getItemInHand(hand);
        if (!isTool(held)) {
            return InteractionResult.PASS;
        }
        Level level = player.level();
        Member member = memberAt(level, pos);
        DismantleStart start = liveStart(level, held);
        switch (Click.of(player.isShiftKeyDown(), member != null, start != null)) {
            case STORE -> {
                if (!level.isClientSide()) {
                    held.set(Groundworks.DISMANTLE_START.get(), new DismantleStart(level.dimension(), member.pos(), member.state()));
                    tell(player, Component.translatable("message.groundworks.dismantle_started"));
                }
                return InteractionResult.SUCCESS;
            }
            case TAKE_UP -> {
                if (level.isClientSide()) {
                    return InteractionResult.SUCCESS;
                }
                DismantleFamily family = familyOf(start.state());
                DismantleSpan span = spanFrom(level, start, pos);
                if (span.isRefused()) {
                    tell(player, message(family, span.refusal()));
                } else {
                    takeUp(family, span, level, held, player);
                }
                return InteractionResult.SUCCESS;
            }
            default -> {
                return InteractionResult.PASS;
            }
        }
    }

    /** A use in the air: a sneak clears the stored start. */
    static InteractionResult use(Player player, InteractionHand hand) {
        ItemStack held = player.getItemInHand(hand);
        if (!player.isShiftKeyDown() || !isTool(held) || !held.has(Groundworks.DISMANTLE_START.get())) {
            return InteractionResult.PASS;
        }
        if (!player.level().isClientSide()) {
            held.remove(Groundworks.DISMANTLE_START.get());
            tell(player, Component.translatable("message.groundworks.dismantle_cleared"));
        }
        return InteractionResult.SUCCESS;
    }

    // The hook runs before any removal, so a removal's side effects -- a belt line rebuilding -- can't
    // hand a taken block's share to one outside the span. Each block's drops are collected just before
    // it goes, from the state it is in then.
    private static void takeUp(DismantleFamily family, DismantleSpan span, Level level, ItemStack held, Player player) {
        held.remove(Groundworks.DISMANTLE_START.get());
        List<ItemStack> handed = new ArrayList<>(family.beforeTaking(level, span.takes()));
        boolean handsOver = !player.hasInfiniteMaterials();
        for (BlockPos pos : new LinkedHashSet<>(span.takes())) {
            BlockState state = level.getBlockState(pos);
            if (state.isAir()) {
                continue;
            }
            if (handsOver && level instanceof ServerLevel server) {
                handed.addAll(Block.getDrops(state, server, pos, level.getBlockEntity(pos), player, held));
            }
            level.destroyBlock(pos, false, player);
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
