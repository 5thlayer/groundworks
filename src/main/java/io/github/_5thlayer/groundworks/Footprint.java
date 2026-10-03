// SPDX-FileCopyrightText: 2026 5thlayer
// SPDX-License-Identifier: MIT

package io.github._5thlayer.groundworks;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Supplier;

import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.neoforged.neoforge.capabilities.BlockCapability;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.common.util.BlockSnapshot;
import net.neoforged.neoforge.event.EventHooks;
import org.jspecify.annotations.Nullable;

/**
 * A Footprint: a block placed whole and broken whole across several positions (ADR 0009). Its
 * {@link FootprintShape shape}, its Origin, the Consumer's own block that holds the block entity
 * and the facing, the {@link FootprintPartBlock part block} standing on each other position, and
 * the one item that places it.
 *
 * <h2>A Consumer declares each</h2>
 *
 * <p>Groundworks has none of its own. A Consumer {@linkplain #declare declares} each at mod
 * construction, and registers its part block from the library's class and its item, a {@link
 * FootprintItem} or any item that {@linkplain PlansPlacement plans} through {@link #plan} and lays
 * through {@link #place}. The origin is any block with a {@linkplain #FACING horizontal facing}
 * that calls {@link #teardown} from its own {@code affectNeighborsAfterRemoval}, so that its going
 * takes its parts with it, and that a piston doesn't move, so that it never leaves them behind.
 *
 * <h2>Placed whole</h2>
 *
 * <p>The origin stands where the click places, facing the player, and every part as its shape
 * says, turned with that facing. The plan is refused whole as {@link Refusal.Footprint#BLOCKED}
 * when any position is outside the world, holds a block a placement can't replace, or has an entity
 * in the way, and where a {@linkplain Raise height} moved the click onto a taken spot; the click
 * then lays nothing and charges nothing. The origin takes the item's block-entity data and
 * components, and is told it was placed, as a placed block is.
 *
 * <h2>Broken whole</h2>
 *
 * <p>Breaking any of its blocks breaks the origin as the player would: a part broken by a player
 * drops what the origin drops for the player's tool, its experience included, where the player may
 * harvest the origin, and nothing to a creative player; a part removed by anything else drops what
 * the origin drops with no tool. The origin's own block entity's removal decides what else drops,
 * its contents as a chest's. Parts drop nothing of their own, so one item comes back whichever
 * block was hit.
 *
 * <h2>Turned whole</h2>
 *
 * <p>Rotate in Place on any of its blocks turns it whole, with no statement of its own: the origin
 * stays where it stands with its block entity and takes the next facing, clockwise seen from above
 * or the other way, and its parts are laid again for that facing. It is refused whole, with {@link
 * #TURN_BLOCKED}, when a position the turned footprint newly stands on is outside the world, holds a
 * block a placement can't replace, has an entity in the way, or is one the player may not build at,
 * a protection mod's place event included.
 */
public final class Footprint {

    /** The origin's facing, which a Consumer's origin block declares, and each part's. */
    public static final EnumProperty<Direction> FACING = BlockStateProperties.HORIZONTAL_FACING;

    /** What the player is told when a footprint has no room to turn. */
    public static final String TURN_BLOCKED = "message.groundworks.footprint_turn_blocked";

    private static final List<Footprint> DECLARED = new CopyOnWriteArrayList<>();

    /** Whether a teardown or a turn is removing this thread's footprint blocks, so a part's removal starts none of its own. */
    private static final ThreadLocal<Boolean> RELAYING = ThreadLocal.withInitial(() -> false);

    private final FootprintShape shape;
    private final Supplier<? extends Block> origin;
    private final Supplier<? extends FootprintPartBlock> part;
    private final Supplier<? extends Item> item;

    private Footprint(FootprintShape shape, Supplier<? extends Block> origin,
                      Supplier<? extends FootprintPartBlock> part, Supplier<? extends Item> item) {
        this.shape = shape;
        this.origin = origin;
        this.part = part;
        this.item = item;
    }

    /**
     * Declares a footprint: its shape, its origin block, the part block on its other positions and
     * the item that places it. Called at mod construction, on both sides, since the preview plans on
     * the client and the click on the server; the suppliers are asked only once blocks and items are
     * registered, and each must be. Mods are constructed in parallel, so this may be called from
     * several threads at once.
     */
    public static Footprint declare(FootprintShape shape, Supplier<? extends Block> origin,
                                    Supplier<? extends FootprintPartBlock> part, Supplier<? extends Item> item) {
        Footprint footprint = new Footprint(shape, origin, part, item);
        DECLARED.add(footprint);
        return footprint;
    }

    /** The footprint this block belongs to, its origin or one of its parts, or {@code null} for any other block. */
    public static @Nullable Footprint of(BlockState state) {
        if (state.getBlock() instanceof FootprintPartBlock partBlock) {
            return partBlock.footprint();
        }
        for (Footprint footprint : DECLARED) {
            if (footprint.isOrigin(state)) {
                return footprint;
            }
        }
        return null;
    }

    public FootprintShape shape() {
        return shape;
    }

    public Supplier<? extends Block> origin() {
        return origin;
    }

    public Supplier<? extends Item> item() {
        return item;
    }

    public boolean isOrigin(BlockState state) {
        return state.is(origin.get());
    }

    public boolean isPart(BlockState state) {
        return state.is(part.get());
    }

    /** Where each block stands for an origin here facing this way, the origin first. */
    public List<BlockPos> positions(BlockPos origin, Direction facing) {
        return shape.positions(origin, facing);
    }

    /**
     * Where the origin of this footprint's block at {@code pos} stands, the block's own position if
     * it is the origin, or {@code null} where the origin is gone, as from a part left behind.
     */
    public @Nullable BlockPos standingOrigin(BlockGetter level, BlockPos pos, BlockState state) {
        BlockPos at = isPart(state)
                ? shape.originOf(pos, state.getValue(FootprintPartBlock.PART), state.getValue(FACING))
                : pos;
        return isOrigin(level.getBlockState(at)) ? at : null;
    }

    /** The state each position is placed in, the origin's at {@code 0}, facing this way. */
    public BlockState stateAt(int index, Direction facing) {
        if (index == 0) {
            return origin.get().defaultBlockState().setValue(FACING, facing);
        }
        return part.get().defaultBlockState()
                .setValue(FACING, facing)
                .setValue(FootprintPartBlock.PART, index);
    }

    /**
     * The whole footprint, its origin where the context places and facing the player, refused whole
     * as {@link Refusal.Footprint#BLOCKED} where any position doesn't fit, or {@code null} where the
     * context places nothing. What a {@link FootprintItem} plans, and what another item that places
     * a footprint returns from its own {@link PlansPlacement#plan}.
     */
    public @Nullable PlacementPlan plan(BlockPlaceContext context) {
        // A spot moved by a height is one the player chose, so a taken one is drawn refused, as a vanilla plan's is.
        if (!context.canPlace() && Raise.heightOf(context).equals(Height.NONE)) {
            return null;
        }
        Level level = context.getLevel();
        Direction facing = context.getHorizontalDirection().getOpposite();
        List<BlockPos> positions = positions(context.getClickedPos(), facing);
        List<PlacementPlan.Placed> blocks = new ArrayList<>(positions.size());
        boolean fits = context.canPlace();
        for (int i = 0; i < positions.size(); i++) {
            BlockPos pos = positions.get(i);
            BlockState state = stateAt(i, facing);
            blocks.add(new PlacementPlan.Placed(pos, state));
            fits &= fits(level, pos, state);
        }
        return fits ? PlacementPlan.accepted(blocks) : PlacementPlan.refused(blocks, Refusal.Footprint.BLOCKED);
    }

    /**
     * Whether a footprint block may stand here: inside the world, on a block a placement replaces,
     * and clear of entities, by vanilla's own check, without which the footprint closes round a
     * player or a mob standing in it.
     */
    private static boolean fits(Level level, BlockPos pos, BlockState state) {
        return level.isInWorldBounds(pos) && level.getBlockState(pos).canBeReplaced()
                && level.isUnobstructed(state, pos, CollisionContext.empty());
    }

    /**
     * Lays the item's plan in the context, so the preview and the click cannot disagree: every block
     * or none, and the item charged only when the footprint stands. The origin takes the item's
     * block-entity data and components and is told it was placed, as {@code BlockItem.place} does
     * for its one block. Whether it was laid.
     */
    public boolean place(BlockItem placing, BlockPlaceContext context) {
        PlacementPlan plan = Placements.planFor(placing, context);
        if (plan == null || plan.isRefused()) {
            return false;
        }
        Level level = context.getLevel();
        for (PlacementPlan.Placed placed : plan.blocks()) {
            level.setBlock(placed.pos(), placed.state(), Block.UPDATE_ALL);
        }
        BlockPos at = plan.blocks().getFirst().pos();
        Player player = context.getPlayer();
        ItemStack held = context.getItemInHand();
        BlockState placed = level.getBlockState(at);
        BlockItem.updateCustomBlockEntityTag(level, player, at, held);
        BlockEntity entity = level.getBlockEntity(at);
        if (entity != null) {
            entity.applyComponentsFromItemStack(held);
            entity.setChanged();
        }
        placed.getBlock().setPlacedBy(level, at, placed, player, held);
        if (player instanceof ServerPlayer server) {
            CriteriaTriggers.PLACED_BLOCK.trigger(server, at, held);
        }
        SoundType sound = placed.getSoundType(level, at, player);
        level.playSound(player, at, sound.getPlaceSound(), SoundSource.BLOCKS,
                (sound.getVolume() + 1.0F) / 2.0F, sound.getPitch() * 0.8F);
        level.gameEvent(GameEvent.BLOCK_PLACE, at, GameEvent.Context.of(player, placed));
        held.consume(1, player);
        return true;
    }

    /**
     * Removes every block of the footprint whose origin stands here facing this way, but {@code
     * skip}, the position already going by whatever called this. The origin's own {@code
     * affectNeighborsAfterRemoval} calls it with its own position, and a part's going calls it
     * once it has broken the origin. Its own removals start no teardown of their own.
     */
    public void teardown(Level level, BlockPos origin, Direction facing, BlockPos skip) {
        if (relaying()) {
            return;
        }
        relaying(() -> {
            for (BlockPos pos : positions(origin, facing)) {
                BlockState state = level.getBlockState(pos);
                if (!pos.equals(skip) && (isOrigin(state) || isPart(state))) {
                    level.removeBlock(pos, false);
                }
            }
        });
    }

    /** Whether this thread is removing or laying footprint blocks, so a part's removal is no break. */
    static boolean relaying() {
        return RELAYING.get();
    }

    /** Runs {@code removals} with this thread relaying, then restores what it was. */
    private static void relaying(Runnable removals) {
        boolean was = RELAYING.get();
        RELAYING.set(true);
        try {
            removals.run();
        } finally {
            RELAYING.set(was);
        }
    }

    /**
     * A part at {@code pos} is going: its origin drops what breaking it drops, by {@code player}
     * with {@code tool} where they harvest it, or with no player at all, and the rest of the
     * footprint goes. Nothing where the origin is already gone.
     */
    void partBroken(ServerLevel level, BlockPos pos, BlockState state, @Nullable Player player, ItemStack tool,
                    boolean harvests) {
        BlockPos at = standingOrigin(level, pos, state);
        if (at == null) {
            return;
        }
        BlockState originState = level.getBlockState(at);
        if (harvests && (player == null || originState.canHarvestBlock(level, at, player))) {
            Block.dropResources(originState, level, at, level.getBlockEntity(at), player, tool);
        }
        teardown(level, at, originState.getValue(FACING), pos);
    }

    /**
     * What Rotate in Place does to the footprint whose block stands at {@code pos}: turned, with the
     * origin's new state, or refused as {@link #TURN_BLOCKED}. It changes nothing; {@link #turn}
     * lays it. Fires the place event at each position the turned footprint newly stands on, as the
     * claim guard Rotate in Place asks.
     */
    TurnsInPlace.Verdict<BlockState> turnInPlace(Level level, BlockPos pos, BlockState state, Player player,
                                                 boolean reverse) {
        BlockPos at = standingOrigin(level, pos, state);
        if (at == null) {
            return TurnsInPlace.unturned();
        }
        BlockState originState = level.getBlockState(at);
        Direction facing = originState.getValue(FACING);
        Direction turned = reverse ? facing.getCounterClockWise() : facing.getClockWise();
        Set<BlockPos> standing = new HashSet<>(positions(at, facing));
        List<BlockPos> after = positions(at, turned);
        for (int i = 1; i < after.size(); i++) {
            BlockPos to = after.get(i);
            if (!standing.contains(to) && (!fits(level, to, stateAt(i, turned)) || !level.mayInteract(player, to)
                    || EventHooks.onBlockPlace(player, BlockSnapshot.create(level.dimension(), level, to), Direction.UP))) {
                return TurnsInPlace.refused(TURN_BLOCKED);
            }
        }
        return TurnsInPlace.turned(originState.setValue(FACING, turned));
    }

    /**
     * Lays a turn {@link #turnInPlace} gave the footprint whose block stands at {@code pos}: the
     * origin takes {@code turnedOrigin}, keeping its block entity, the parts the turned footprint no
     * longer stands on go, and every part is laid for the new facing.
     */
    void turn(Level level, BlockPos pos, BlockState state, BlockState turnedOrigin) {
        BlockPos at = standingOrigin(level, pos, state);
        if (at == null) {
            return;
        }
        List<BlockPos> before = positions(at, level.getBlockState(at).getValue(FACING));
        Direction turned = turnedOrigin.getValue(FACING);
        List<BlockPos> after = positions(at, turned);
        relaying(() -> {
            level.setBlock(at, turnedOrigin, Block.UPDATE_ALL);
            for (BlockPos old : before) {
                if (!after.contains(old) && isPart(level.getBlockState(old))) {
                    level.removeBlock(old, false);
                }
            }
            for (int i = 1; i < after.size(); i++) {
                level.setBlock(after.get(i), stateAt(i, turned), Block.UPDATE_ALL);
            }
        });
    }

    /**
     * Every registered {@link FootprintPartBlock} forwards a lookup of energy, fluid or items to its
     * origin, with the side unchanged: what the origin answers there, or nothing where the origin is
     * gone. A Consumer forwards any other capability itself.
     */
    static void forwardCapabilities(RegisterCapabilitiesEvent event) {
        Block[] parts = BuiltInRegistries.BLOCK.stream().filter(FootprintPartBlock.class::isInstance).toArray(Block[]::new);
        if (parts.length == 0) {
            return;
        }
        forward(event, Capabilities.Energy.BLOCK, parts);
        forward(event, Capabilities.Fluid.BLOCK, parts);
        forward(event, Capabilities.Item.BLOCK, parts);
    }

    private static <T> void forward(RegisterCapabilitiesEvent event, BlockCapability<T, @Nullable Direction> capability,
                                    Block[] parts) {
        event.registerBlock(capability, (level, pos, state, entity, side) -> {
            BlockPos at = ((FootprintPartBlock) state.getBlock()).footprint().standingOrigin(level, pos, state);
            return at == null ? null : level.getCapability(capability, at, side);
        }, parts);
    }
}
