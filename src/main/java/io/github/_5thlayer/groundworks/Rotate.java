// SPDX-FileCopyrightText: 2026 5thlayer
// SPDX-License-Identifier: MIT

package io.github._5thlayer.groundworks;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Predicate;

import io.github._5thlayer.groundworks.TurnsInPlace.Verdict;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.Property;
import net.neoforged.neoforge.common.util.BlockSnapshot;
import net.neoforged.neoforge.event.EventHooks;
import org.jspecify.annotations.Nullable;

/**
 * Rotate and Reverse Rotate (ADR 0003). A press Rotates the Plan of a rotatable held stack: its
 * next placement turns a quarter from the way the player looks, and the turn stays on the stack,
 * as {@link Groundworks#QUARTER_TURN}, until its last item is placed or a {@linkplain Stretches
 * Stretch}'s start uses it up. Otherwise it Rotates in Place the block under the crosshair, if a
 * Consumer has {@linkplain #turnsInPlace stated} it turns it.
 *
 * <p>Nothing implements a contract to Rotate the Plan. The look a placement context reports is
 * turned by the stack's turn, so the plan and the click, which both read that context, turn
 * together, and every block whose placement reads the look turns with no code of its own. A
 * stretch's start, which stores the look rather than reading a context, turns it by the stack's
 * turn itself; with a start stored, a press leaves the stretch alone. In place, a block that
 * {@linkplain TurnsInPlace answers for itself} does, and any other takes vanilla's turn.
 */
public final class Rotate {

    private static final List<Predicate<? super Block>> TURNED_IN_PLACE = new CopyOnWriteArrayList<>();

    private Rotate() {
    }

    /**
     * States that Rotate in Place turns the placed blocks this matches. A block no Consumer states
     * is left alone, so a mod's blocks are never turned in an install that didn't ask for it. This
     * is separate from the {@linkplain Placements#optIn Opt-in}, which is about what is drawn.
     *
     * <p>Called at mod construction, on both sides. Mods are constructed in parallel, so this may
     * be called from several threads at once.
     */
    public static void turnsInPlace(Predicate<? super Block> blocks) {
        TURNED_IN_PLACE.add(blocks);
    }

    /** Whether some Consumer has stated Rotate in Place turns this block. */
    public static boolean isTurnedInPlace(Block block) {
        for (Predicate<? super Block> blocks : TURNED_IN_PLACE) {
            if (blocks.test(block)) {
                return true;
            }
        }
        return false;
    }

    /** The held stack's turn, which a Consumer's own plan reads if it reads the look some other way. */
    public static QuarterTurn turnOf(ItemStack stack) {
        return stack.getOrDefault(Groundworks.QUARTER_TURN.get(), QuarterTurn.NONE);
    }

    /**
     * The turn a block placement's look is turned by: the held stack's, in the main hand only,
     * since that is the hand the Placement Preview draws, and a turn is never one the player
     * cannot see.
     */
    public static QuarterTurn turnOf(BlockPlaceContext context) {
        return context.getHand() == InteractionHand.MAIN_HAND ? turnOf(context.getItemInHand()) : QuarterTurn.NONE;
    }

    /**
     * Whether a press turns this stack: a Placement Preview is drawn for it, so the turn is never
     * one the player cannot see, and its block has a facing, an axis or a sixteen-way rotation.
     * Vanilla cannot say whether a block's placement reads the look, so a block with one of those
     * is taken to.
     */
    public static boolean isRotatable(ItemStack stack) {
        return stack.getItem() instanceof BlockItem item && Placements.isDrawn(item) && orients(item.getBlock());
    }

    private static boolean orients(Block block) {
        for (Property<?> property : block.getStateDefinition().getProperties()) {
            if (property.getValueClass() == Direction.class || property.getValueClass() == Direction.Axis.class
                    || property == BlockStateProperties.ROTATION_16) {
                return true;
            }
        }
        return false;
    }

    /**
     * A press of Rotate, or of Reverse Rotate, decided on the server by {@link RotatePress}'s rule.
     * It turns the main hand's stack if that is rotatable, and otherwise the aimed block, the block
     * under the crosshair if any. A refusal is told on the action bar.
     *
     * <p>An aimed block the player could not reach is no aimed block. That is checked from the
     * payload's position alone, before anything reads the block, since a modified client may aim
     * anywhere and reading an unloaded block would load its chunk.
     */
    public static void press(Player player, @Nullable BlockPos aimed, boolean reverse) {
        ItemStack held = player.getMainHandItem();
        switch (RotatePress.decide(isRotatable(held) ? turnOf(held) : null, reverse,
                aimed == null || !mayReach(player, aimed) ? null : new AimedInWorld(player, aimed, reverse))) {
            case RotatePress.PlanTurned<BlockState>(QuarterTurn turn) -> setTurn(held, turn);
            case RotatePress.TurnedInPlace<BlockState>(BlockState turned) ->
                    player.level().setBlock(aimed, turned, Block.UPDATE_ALL);
            case RotatePress.Refused<BlockState>(String reason) ->
                    player.sendOverlayMessage(Component.translatable(reason));
            case RotatePress.Nothing<BlockState>() -> {
            }
        }
    }

    private static boolean mayReach(Player player, BlockPos pos) {
        Level level = player.level();
        return player.mayBuild() && level.isLoaded(pos) && player.isWithinBlockInteractionRange(pos, 1.0)
                && level.mayInteract(player, pos);
    }

    // No turn is no component, so a stack turned back stacks again with one never turned.
    static void setTurn(ItemStack stack, QuarterTurn turned) {
        if (turned.equals(QuarterTurn.NONE)) {
            stack.remove(Groundworks.QUARTER_TURN.get());
        } else {
            stack.set(Groundworks.QUARTER_TURN.get(), turned);
        }
    }

    /** The aimed block in the world, one the player may reach, as the press rule asks of it. */
    private record AimedInWorld(Player player, BlockPos pos, boolean reverse) implements RotatePress.Aimed<BlockState> {

        @Override
        public BlockState state() {
            return player.level().getBlockState(pos);
        }

        @Override
        public boolean isStated() {
            return isTurnedInPlace(state().getBlock());
        }

        /**
         * The claim guard: NeoForge's place event, fired at the aimed block, not cancelled. It is how
         * claim mods guard, and a custom payload fires none on its own.
         */
        @Override
        public boolean mayTurn() {
            Level level = player.level();
            return !EventHooks.onBlockPlace(player, BlockSnapshot.create(level.dimension(), level, pos), Direction.UP);
        }

        @Override
        public @Nullable Verdict<BlockState> ownTurn() {
            BlockState state = state();
            return state.getBlock() instanceof TurnsInPlace block
                    ? block.turnInPlace(state, player.level(), pos, reverse)
                    : null;
        }

        // Vanilla's rotate leaves the neighbour update for later: here a door or bed half turned
        // alone re-derives against its other half.
        @Override
        public BlockState vanillaTurn() {
            Level level = player.level();
            BlockState turned = state().rotate(level, pos, reverse ? Rotation.COUNTERCLOCKWISE_90 : Rotation.CLOCKWISE_90);
            return Block.updateFromNeighbourShapes(turned, level, pos);
        }

        @Override
        public boolean stands(BlockState turned) {
            return !turned.isAir() && turned.canSurvive(player.level(), pos);
        }
    }
}
