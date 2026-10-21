// SPDX-FileCopyrightText: 2026 5thlayer
// SPDX-License-Identifier: MIT

package io.github._5thlayer.groundworks;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Predicate;

import com.mojang.logging.LogUtils;
import io.github._5thlayer.groundworks.mixin.BlockItemInvoker;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.FileToIdConverter;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.tags.TagKey;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntitySelector;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.BooleanOp;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.common.util.BlockSnapshot;
import net.neoforged.neoforge.event.AddServerReloadListenersEvent;
import net.neoforged.neoforge.event.EventHooks;
import net.neoforged.neoforge.event.OnDatapackSyncEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

/**
 * Assemblies (ADR 0011): a shape of real blocks placed in one step from its Key, and then
 * remembered by nothing.
 *
 * <p>Each is declared as data, an {@link AssemblyShape} in a JSON file under {@code
 * data/<namespace>/groundworks/assembly/}, loaded with the server's data and sent to each client,
 * so the client's preview and the server's click read the same declarations. A malformed file is
 * logged with its id and skipped, and a {@code /reload} reads them all again. A Key's Assemblies
 * are ordered by their ids.
 *
 * <p>A held block item is a Key when a Consumer has {@linkplain #keys stated} its block is one, as
 * the {@linkplain VanillaConsumer vanilla Consumer} does for its tag, and a declaration names it.
 * Its plan is the whole shape, resting on the aimed face and facing the player, the Key last, its
 * tag positions {@linkplain AssemblyShape#fill filled} from the main inventory, and its empty
 * positions cleared of what a placement overwrites, fluids aside; a sneak places the Key alone, as
 * vanilla does, and so does a stack whose {@linkplain AssemblyChoice choice}, stepped by Next
 * Assembly, is the Key alone.
 */
public final class Assemblies {

    private static final Logger LOGGER = LogUtils.getLogger();

    /** The declarations the server loaded, by id. */
    private static volatile Map<Identifier, AssemblyShape> server = Map.of();

    /** The declarations the server sent this client, by id: apart from the server's, which in single player shares these statics. */
    private static volatile Map<Identifier, AssemblyShape> client = Map.of();

    private static final List<Predicate<? super Block>> KEYS = new CopyOnWriteArrayList<>();

    private Assemblies() {
    }

    /**
     * States that the blocks this matches are Keys, each placing the Assemblies declared for it. A
     * block is a Key when any Consumer's statement says so; one no statement names places as
     * vanilla does, whatever is declared for it.
     *
     * <p>Called at mod construction, on both sides, since the plan is asked of on both. Mods are
     * constructed in parallel, so this may be called from several threads at once.
     */
    public static void keys(Predicate<? super Block> blocks) {
        KEYS.add(blocks);
    }

    /** Whether some Consumer has stated this block is a Key. */
    public static boolean isKey(Block block) {
        for (Predicate<? super Block> blocks : KEYS) {
            if (blocks.test(block)) {
                return true;
            }
        }
        return false;
    }

    static void register(IEventBus modBus) {
        modBus.addListener(AssembliesPayload::register);
        NeoForge.EVENT_BUS.addListener(AddServerReloadListenersEvent.class, event ->
                event.addListener(Identifier.fromNamespaceAndPath(Groundworks.MOD_ID, "assemblies"), new Loader()));
        NeoForge.EVENT_BUS.addListener(OnDatapackSyncEvent.class, event -> {
            AssembliesPayload payload = new AssembliesPayload(server);
            event.getRelevantPlayers()
                    .filter(player -> player.connection.hasChannel(AssembliesPayload.TYPE))
                    .forEach(player -> PacketDistributor.sendToPlayer(player, payload));
        });
    }

    /** The server's declarations, as this client now has them. */
    static void received(Map<Identifier, AssemblyShape> declarations) {
        client = Map.copyOf(declarations);
    }

    /** This client left its server, whose declarations are no other server's. */
    public static void forget() {
        client = Map.of();
    }

    /** The Assemblies whose Key is this block, by their ids, as this side has them. */
    static List<AssemblyShape> keyedBy(Level level, Block key) {
        return keyedBy(level.isClientSide(), BuiltInRegistries.BLOCK.getKey(key));
    }

    /** The Assemblies whose Key is the block of this id, by their ids, as the client or the server has them. */
    static List<AssemblyShape> keyedBy(boolean onClient, Identifier key) {
        return (onClient ? client : server).entrySet().stream()
                .filter(entry -> entry.getValue().keyBlock().equals(key))
                .sorted(Map.Entry.comparingByKey(Comparator.comparing(Identifier::toString)))
                .map(Map.Entry::getValue)
                .toList();
    }

    /**
     * The Assemblies this stack's Key places, by their ids, or none where the stack is no Key: its
     * item places no block a Consumer has {@linkplain #keys stated} a Key, or none is declared for it.
     */
    private static List<AssemblyShape> choicesOf(Level level, ItemStack stack) {
        if (!(stack.getItem() instanceof BlockItem item) || !isKey(item.getBlock())) {
            return List.of();
        }
        return keyedBy(level, item.getBlock());
    }

    /**
     * Where a fresh stack of this Key starts: on the Key alone, the choice after its Assemblies,
     * where any of its declarations says so, so a pack adding one for a Key that starts alone
     * leaves it starting alone; otherwise on its first Assembly.
     */
    private static int start(List<AssemblyShape> assemblies) {
        return assemblies.stream().anyMatch(AssemblyShape::startsAlone) ? assemblies.size() : 0;
    }

    /** The Assembly the held stack places, or {@code null} for the Key alone or for no Key. */
    static @Nullable AssemblyShape chosen(Level level, ItemStack stack) {
        List<AssemblyShape> assemblies = choicesOf(level, stack);
        if (assemblies.isEmpty()) {
            return null;
        }
        int choice = AssemblyChoice.current(stack.get(Groundworks.ASSEMBLY_CHOICE.get()), start(assemblies),
                assemblies.size() + 1);
        return choice < assemblies.size() ? assemblies.get(choice) : null;
    }

    /**
     * The Assembly a click with the held stack places, or {@code null} where it places none: the
     * stack is no Key, its choice is the Key alone, or the player sneaks, which places the Key alone
     * as vanilla does.
     */
    static @Nullable AssemblyShape placed(Level level, @Nullable Player player, ItemStack stack) {
        return player != null && player.isShiftKeyDown() ? null : chosen(level, stack);
    }

    /**
     * A press of Next Assembly, or of its reverse, decided on the server: the main hand's Key steps
     * to its next choice, or its previous, through its Assemblies and the Key alone and round. A
     * stack that is no Key is left as it is.
     */
    public static void press(Player player, boolean reverse) {
        ItemStack held = player.getMainHandItem();
        List<AssemblyShape> assemblies = choicesOf(player.level(), held);
        if (assemblies.isEmpty()) {
            return;
        }
        AssemblyChoice next = AssemblyChoice.next(held.get(Groundworks.ASSEMBLY_CHOICE.get()), start(assemblies),
                assemblies.size() + 1, reverse);
        // The start is no component, so a stack stepped back to it stacks again with a fresh one.
        if (next == null) {
            held.remove(Groundworks.ASSEMBLY_CHOICE.get());
        } else {
            held.set(Groundworks.ASSEMBLY_CHOICE.get(), next);
        }
    }

    /**
     * What a main-hand click with this stack would place, or {@code null} where it places no
     * Assembly: the item is no Key, or the Key goes alone.
     */
    static @Nullable PlacementPlan planFor(Level level, @Nullable Player player, ItemStack stack, BlockHitResult hit) {
        Laying laying = laying(level, player, stack, hit);
        return laying == null ? null : laying.plan();
    }

    /**
     * An Assembly's plan, its Key last, its {@linkplain PlacementPlan#clears clears} the empty
     * positions holding a block a placement overwrites, cleared before the rest is laid, and what it
     * costs.
     *
     * @param cost what the blocks before the Key cost, by the fill; the Key is charged once it is
     *     placed, and they with it
     * @param missing what the main inventory lacks for the plan, by the fill's count, none when it
     *     affords it
     */
    private record Laying(PlacementPlan plan, Map<Item, Integer> cost, Map<Item, Integer> missing) {

        /** The blocks laid before the Key, in the plan's order. */
        List<PlacementPlan.Placed> others() {
            return plan.blocks().subList(0, plan.blocks().size() - 1);
        }

        /** Where the Key goes. */
        BlockPos key() {
            return plan.blocks().getLast().pos();
        }

        /** The state the Key goes down in. */
        BlockState keyState() {
            return plan.blocks().getLast().state();
        }

        /** The Key's name, as the player is told of it. */
        Component keyName() {
            return keyState().getBlock().getName();
        }
    }

    /** Every position a laying changes: the cleared ones, then the plan's. */
    private static List<BlockPos> changed(List<BlockPos> cleared, List<PlacementPlan.Placed> blocks) {
        List<BlockPos> changed = new ArrayList<>(cleared);
        blocks.forEach(placed -> changed.add(placed.pos()));
        return changed;
    }

    private static @Nullable Laying laying(Level level, @Nullable Player player, ItemStack stack, BlockHitResult hit) {
        AssemblyShape shape = placed(level, player, stack);
        if (shape == null) {
            return null;
        }
        BlockItem keyItem = (BlockItem) stack.getItem();
        BlockPlaceContext context = new BlockPlaceContext(level, player, InteractionHand.MAIN_HAND, stack, hit);
        Direction facing = context.getHorizontalDirection().getOpposite();
        List<AssemblyShape.At> positions = shape.positions(context.getClickedPos(), hit.getDirection(), facing);

        // Which positions want a block, which want clearing, and which are taken.
        List<AssemblyShape.At> wanting = new ArrayList<>(positions.size());
        List<AssemblyShape.Legend.Laid> legends = new ArrayList<>(positions.size());
        List<BlockPos> cleared = new ArrayList<>();
        Set<BlockPos> taken = new HashSet<>();
        for (AssemblyShape.At at : positions.subList(0, positions.size() - 1)) {
            BlockState there = level.getBlockState(at.pos());
            switch (at.legend()) {
                case AssemblyShape.Legend.Empty empty -> {
                    if (!there.canBeReplaced()) {
                        taken.add(at.pos());
                    } else if (there != emptied(there)) {
                        cleared.add(at.pos());
                    }
                }
                case AssemblyShape.Legend.Block named -> {
                    Optional<Block> block = BuiltInRegistries.BLOCK.getOptional(named.id());
                    if (block.isEmpty()) {
                        // A declaration a client was sent for a block it lacks: nothing honest to draw.
                        return null;
                    }
                    // A block the shape takes, standing already, stays and is not charged.
                    if (!there.is(block.get())) {
                        wanting.add(at);
                        legends.add(named);
                    }
                }
                case AssemblyShape.Legend.Tag tag -> {
                    if (!there.is(blockTag(tag.tag()))) {
                        wanting.add(at);
                        legends.add(tag);
                    }
                }
            }
        }

        clearedWithTheirOtherHalves(level, cleared, positions);

        legends.add(new AssemblyShape.Legend.Block(shape.keyBlock()));
        boolean free = player == null || player.hasInfiniteMaterials();
        AssemblyShape.Fill fill = AssemblyShape.fill(legends, player == null ? Map.of() : held(player), Assemblies::members,
                free);

        Rotation turn = turnFrom(facing);
        List<PlacementPlan.Placed> others = new ArrayList<>(wanting.size());
        Map<Item, Integer> cost = new LinkedHashMap<>();
        for (int i = 0; i < wanting.size(); i++) {
            Optional<Block> block = BuiltInRegistries.BLOCK.getOptional(fill.blocks().get(i));
            if (block.isEmpty()) {
                return null;
            }
            BlockPos pos = wanting.get(i).pos();
            BlockState state = block.get().defaultBlockState().rotate(level, pos, turn);
            others.add(new PlacementPlan.Placed(pos, state));
            cost.merge(block.get().asItem(), 1, Integer::sum);
            if (!fits(level, player, pos, level.getBlockState(pos), state)) {
                taken.add(pos);
            }
        }
        Map<Item, Integer> missing = new LinkedHashMap<>();
        fill.missing().forEach((id, count) -> BuiltInRegistries.BLOCK.getOptional(id)
                .ifPresent(block -> missing.merge(block.asItem(), count, Integer::sum)));

        BlockPos keyPos = positions.getLast().pos();
        Map<BlockPos, BlockState> laid = new HashMap<>();
        cleared.forEach(pos -> laid.put(pos, emptied(level.getBlockState(pos))));
        others.forEach(placed -> laid.put(placed.pos(), placed.state()));
        BlockPlaceContext onTheLaid = new BlockPlaceContext(new PlannedLevel(level, laid, player), player,
                InteractionHand.MAIN_HAND, stack, hit);
        KeyPlacing key = keyPlacing(keyItem, BlockPlaceContext.at(onTheLaid, keyPos, hit.getDirection()));
        if (key.refusal() == Refusal.Assembly.TAKEN) {
            taken.add(keyPos);
        }

        List<PlacementPlan.Placed> blocks = new ArrayList<>(others);
        blocks.add(new PlacementPlan.Placed(keyPos, key.state()));
        // The refusal points at the first position taken in the plan's order, whatever made it taken.
        Optional<BlockPos> firstTaken = positions.stream().map(AssemblyShape.At::pos).filter(taken::contains).findFirst();
        Refusal refusal = null;
        if (firstTaken.isPresent()) {
            refusal = new Refusal.At(Refusal.Assembly.TAKEN, firstTaken.get());
        } else if (key.refusal() == Refusal.Assembly.KEY_WOULD_NOT_STAND) {
            refusal = new Refusal.At(Refusal.Assembly.KEY_WOULD_NOT_STAND, keyPos);
        } else if (player != null && !Charge.mayBuildAll(level, player, changed(cleared, blocks))) {
            refusal = Refusal.Assembly.MAY_NOT_BUILD;
        } else if (!missing.isEmpty()) {
            refusal = Refusal.Assembly.NOT_ENOUGH_ITEMS;
        }
        // The Key is charged apart, once it is placed, though the fill counted it with the rest.
        return new Laying(new PlacementPlan(blocks, List.of(), cleared, refusal), cost, missing);
    }

    /**
     * The state the Key goes down in, and why it can't go down, if it can't.
     *
     * @param refusal {@link Refusal.Assembly#TAKEN TAKEN} where the Key has no room, no state for
     *     placement, or an entity in its way, {@link Refusal.Assembly#KEY_WOULD_NOT_STAND KEY_WOULD_NOT_STAND} where it has a
     *     state that wouldn't survive, or none where it goes down
     */
    private record KeyPlacing(BlockState state, Refusal.@Nullable Assembly refusal) {
    }

    /**
     * The Key's placement as vanilla's own would choose it, asked of the world as the rest of the
     * laying leaves it: so a torch hangs on a wall the shape builds, and a door finds no room under
     * a block the shape lays over it. A Key whose item needn't survive, as scaffolding's needn't,
     * stands anywhere, as vanilla's {@code BlockItem#canPlace} has it.
     */
    private static KeyPlacing keyPlacing(BlockItem item, BlockPlaceContext context) {
        BlockState drawn = item.getBlock().defaultBlockState();
        if (!item.getBlock().isEnabled(context.getLevel().enabledFeatures()) || !context.canPlace()) {
            return new KeyPlacing(drawn, Refusal.Assembly.TAKEN);
        }
        BlockPlaceContext updated = item.updatePlacementContext(context);
        // A Key that would move, as scaffolding moves up a tower of itself, has no place in the shape.
        if (updated == null || !updated.getClickedPos().equals(context.getClickedPos())) {
            return new KeyPlacing(drawn, Refusal.Assembly.TAKEN);
        }
        BlockState placing = ((BlockItemInvoker) item).groundworks$placementState(updated);
        if (placing != null) {
            return new KeyPlacing(placing, null);
        }
        BlockState wanted = item.getBlock().getStateForPlacement(updated);
        if (wanted == null) {
            return new KeyPlacing(drawn, Refusal.Assembly.TAKEN);
        }
        boolean stands = !((BlockItemInvoker) item).groundworks$mustSurvive()
                || wanted.canSurvive(updated.getLevel(), updated.getClickedPos());
        return new KeyPlacing(wanted, stands ? Refusal.Assembly.TAKEN : Refusal.Assembly.KEY_WOULD_NOT_STAND);
    }

    /**
     * How a shape's blocks turn for a Key facing this way: a declaration is drawn with the player
     * looking up the page, north, so with the Key facing south, and a block's default state, as
     * stairs face north, faces up the page, away from the player.
     */
    private static Rotation turnFrom(Direction facing) {
        return switch (facing) {
            case WEST -> Rotation.CLOCKWISE_90;
            case NORTH -> Rotation.CLOCKWISE_180;
            case EAST -> Rotation.COUNTERCLOCKWISE_90;
            default -> Rotation.NONE;
        };
    }

    /**
     * Adds to the cleared positions the other half of each double plant cleared, tall grass's, which
     * goes with it, so the preview marks it too; where that half stands at one of the shape's own
     * positions, the shape already says what becomes of it.
     */
    private static void clearedWithTheirOtherHalves(Level level, List<BlockPos> cleared,
            List<AssemblyShape.At> positions) {
        Set<BlockPos> shaped = new HashSet<>();
        positions.forEach(at -> shaped.add(at.pos()));
        for (BlockPos pos : List.copyOf(cleared)) {
            BlockState there = level.getBlockState(pos);
            if (!there.hasProperty(BlockStateProperties.DOUBLE_BLOCK_HALF)) {
                continue;
            }
            DoubleBlockHalf half = there.getValue(BlockStateProperties.DOUBLE_BLOCK_HALF);
            BlockPos other = half == DoubleBlockHalf.LOWER ? pos.above() : pos.below();
            BlockState otherState = level.getBlockState(other);
            if (!shaped.contains(other) && otherState.is(there.getBlock())
                    && otherState.getValue(BlockStateProperties.DOUBLE_BLOCK_HALF) != half
                    && otherState != emptied(otherState)) {
                cleared.add(other);
            }
        }
    }

    /** What an empty position holds once cleared: nothing, or the fluid that was there. */
    private static BlockState emptied(BlockState there) {
        return there.getFluidState().createLegacyBlock();
    }

    private static TagKey<Block> blockTag(Identifier tag) {
        return TagKey.create(Registries.BLOCK, tag);
    }

    /** The blocks of a block tag, by their ids, in the tag's order. */
    private static List<Identifier> members(Identifier tag) {
        List<Identifier> members = new ArrayList<>();
        for (Holder<Block> block : BuiltInRegistries.BLOCK.getTagOrEmpty(blockTag(tag))) {
            members.add(BuiltInRegistries.BLOCK.getKey(block.value()));
        }
        return members;
    }

    /** How many of each block the main inventory holds, by the block's id. */
    private static Map<Identifier, Integer> held(Player player) {
        Map<Identifier, Integer> held = new HashMap<>();
        for (ItemStack slot : player.getInventory().getNonEquipmentItems()) {
            if (!slot.isEmpty() && slot.getItem() instanceof BlockItem item) {
                held.merge(BuiltInRegistries.BLOCK.getKey(item.getBlock()), slot.getCount(), Integer::sum);
            }
        }
        return held;
    }

    /**
     * Whether a block of the shape may go here: inside the world, on a block a placement replaces,
     * such as air, water or grass, and clear of entities, the placing player's own body aside,
     * which the click lifts out of the shape; not what they ride, which stays where it is.
     */
    private static boolean fits(Level level, @Nullable Player player, BlockPos pos, BlockState there,
            BlockState state) {
        VoxelShape shape = state.getCollisionShape(level, pos, CollisionContext.empty()).move(pos);
        return level.isInWorldBounds(pos) && there.canBeReplaced()
                && (shape.isEmpty() || level.getEntities(player, shape.bounds(), EntitySelector.NO_SPECTATORS.and(
                        entity -> !entity.isRemoved() && entity.blocksBuilding
                                && Shapes.joinIsNotEmpty(shape, Shapes.create(entity.getBoundingBox()), BooleanOp.AND)))
                        .isEmpty());
    }

    /**
     * Where the player is lifted to, straight up from where they stand, for their body to fit
     * clear of the world and of the Key about to go down at {@code keyPos}: where they stand, if
     * they fit there, else the first whole block's height above it they fit at; {@code null} where
     * nothing above them, up to the world's top, has room.
     */
    private static @Nullable Vec3 liftedClear(Level level, Player player, BlockPos keyPos, BlockState key) {
        VoxelShape keyShape = key.getCollisionShape(level, keyPos, CollisionContext.of(player)).move(keyPos);
        Vec3 at = player.position();
        AABB body = player.getBoundingBox();
        for (double y = at.y; y <= level.getMaxY() + 1; y = Math.floor(y) + 1) {
            AABB lifted = body.move(0, y - at.y, 0);
            if (level.noCollision(player, lifted)
                    && !Shapes.joinIsNotEmpty(keyShape, Shapes.create(lifted), BooleanOp.AND)) {
                return new Vec3(at.x, y, at.z);
            }
        }
        return null;
    }

    /**
     * A click on a block with the main hand holding a Key whose Assembly is drawn: the whole of it
     * placed, the Key last through vanilla's own placement, or nothing, with the player told why.
     * PASS where no Assembly is drawn, and the click goes on as it would.
     */
    static InteractionResult useOn(Player player, InteractionHand hand, BlockHitResult hit) {
        if (hand != InteractionHand.MAIN_HAND) {
            return InteractionResult.PASS;
        }
        Level level = player.level();
        ItemStack held = player.getItemInHand(hand);
        Laying laying = laying(level, player, held, hit);
        if (laying == null) {
            return InteractionResult.PASS;
        }
        if (level.isClientSide()) {
            return InteractionResult.SUCCESS;
        }
        PlacementPlan plan = laying.plan();
        if (plan.isRefused()) {
            tell(player, message(laying, plan.refusal()));
            return InteractionResult.SUCCESS;
        }
        // A claim mod's guard on the place event, at every position it changes: the click's own event never fires.
        for (BlockPos pos : changed(plan.clears(), plan.blocks())) {
            if (EventHooks.onBlockPlace(player, BlockSnapshot.create(level.dimension(), level, pos), Direction.UP)) {
                tell(player, message(laying, Refusal.Assembly.MAY_NOT_BUILD));
                return InteractionResult.SUCCESS;
            }
        }
        Map<BlockPos, BlockState> before = new HashMap<>();
        changed(plan.clears(), laying.others()).forEach(pos -> before.put(pos, level.getBlockState(pos)));
        for (BlockPos pos : plan.clears()) {
            level.setBlock(pos, emptied(level.getBlockState(pos)), Block.UPDATE_ALL);
        }
        Charge.put(level, player, laying.others());
        // The plan let the player's own body stand in the shape: they are lifted clear of it, and
        // of the Key, which vanilla places only clear of them.
        Vec3 stood = player.position();
        Vec3 clear = liftedClear(level, player, laying.key(), laying.keyState());
        if (clear != null && !clear.equals(stood)) {
            player.teleportTo(clear.x, clear.y, clear.z);
        }
        // Vanilla's placement, so the Key's own rules run as it goes down, a golem's pumpkin's. It
        // is placed from a copy, so the stack it is placed from is charged with the rest, once it
        // stands.
        BlockItem item = (BlockItem) held.getItem();
        ItemStack key = held.copyWithCount(1);
        BlockPlaceContext context = new BlockPlaceContext(level, player, hand, key, hit);
        if (item.place(BlockPlaceContext.at(context, laying.key(), hit.getDirection())).consumesAction()) {
            // Vanilla told the Key; each block laid before it that still stands, not one a golem
            // took, is told now, with its own item, so a Key it refused tells none.
            for (PlacementPlan.Placed placed : laying.others()) {
                if (level.getBlockState(placed.pos()).is(placed.state().getBlock())) {
                    Charge.tellPlaced(level, player, placed.pos(), new ItemStack(placed.state().getBlock()));
                }
            }
            Map<Item, Integer> cost = new LinkedHashMap<>(laying.cost());
            cost.merge(item, 1, Integer::sum);
            Charge.of(player, cost, List.of()).pay(player);
            return InteractionResult.SUCCESS;
        }
        // The plan took the Key on the world as the laying leaves it, so vanilla refuses it only
        // for what the plan doesn't plan: an entity in its way, as a golem the laying spawned, or
        // another mod's rule. What the laying set is taken back, and what is gone, as a golem's
        // blocks are, is charged; the Key, unplaced, isn't, and the player is told just that.
        Charge.of(player, takeBack(level, laying, before), List.of()).pay(player);
        if (!player.position().equals(stood)) {
            player.teleportTo(stood.x, stood.y, stood.z);
        }
        tell(player, Component.translatable("message.groundworks.assembly_key_not_placed", laying.keyName()));
        return InteractionResult.SUCCESS;
    }

    /**
     * Puts back what stood before the laying, the last laid first, wherever something of it still
     * stands, as it was laid or changed since, as concrete powder hardens; and answers what is gone,
     * leaving only what a placement overwrites, and so still costs the player: a laid block a
     * golem the laying spawned took into itself, say.
     */
    private static Map<Item, Integer> takeBack(Level level, Laying laying, Map<BlockPos, BlockState> before) {
        Map<Item, Integer> gone = new LinkedHashMap<>();
        for (PlacementPlan.Placed placed : laying.others().reversed()) {
            if (level.getBlockState(placed.pos()).canBeReplaced()) {
                gone.merge(placed.state().getBlock().asItem(), 1, Integer::sum);
            } else {
                level.setBlock(placed.pos(), before.get(placed.pos()), Block.UPDATE_ALL);
            }
        }
        for (BlockPos pos : laying.plan().clears().reversed()) {
            BlockState was = before.get(pos);
            if (level.getBlockState(pos).is(emptied(was).getBlock())) {
                level.setBlock(pos, was, Block.UPDATE_ALL);
            }
        }
        return gone;
    }

    /** What the player is told of a laying's refusal: one of its own reasons, or one of them at a position. */
    private static Component message(Laying laying, Refusal refusal) {
        return switch (refusal instanceof Refusal.At at ? at.reason() : refusal) {
            case Refusal.Assembly.TAKEN -> Component.translatable("message.groundworks.assembly_taken");
            case Refusal.Assembly.KEY_WOULD_NOT_STAND -> Component.translatable(
                    "message.groundworks.assembly_key_would_not_stand", laying.keyName());
            case Refusal.Assembly.MAY_NOT_BUILD -> Component.translatable("message.groundworks.assembly_may_not_build");
            case Refusal.Assembly.NOT_ENOUGH_ITEMS -> Component.translatable(
                    "message.groundworks.assembly_not_enough_items", missing(laying.missing()));
            // A laying refuses with its own reasons alone.
            default -> throw new IllegalStateException("not an Assembly's refusal: " + refusal);
        };
    }

    /** What is missing, as "3 Stone, 1 Cobblestone". */
    private static Component missing(Map<Item, Integer> missing) {
        MutableComponent list = Component.empty();
        missing.forEach((item, count) -> {
            if (!list.getSiblings().isEmpty()) {
                list.append(", ");
            }
            list.append(Component.translatable("message.groundworks.assembly_missing_item", count,
                    new ItemStack(item).getHoverName()));
        });
        return list;
    }

    private static void tell(Player player, Component message) {
        if (player instanceof ServerPlayer server) {
            server.sendSystemMessage(message, true);
        }
    }

    /**
     * Reads the declarations with the server's data: each file alone, one that doesn't parse logged
     * with its id and skipped by the reader, and one naming a block the game lacks, a block tag this
     * reload didn't load, or a block with no item to pay for it with, such as fire or water,
     * logged and skipped here. A tag declared with no blocks is a tag the game has.
     */
    private static final class Loader extends SimpleJsonResourceReloadListener<AssemblyShape> {

        Loader() {
            super(AssemblyShape.CODEC, FileToIdConverter.json(Groundworks.MOD_ID + "/assembly"));
        }

        @Override
        protected void apply(Map<Identifier, AssemblyShape> prepared, ResourceManager manager, ProfilerFiller profiler) {
            Map<Identifier, AssemblyShape> loaded = new LinkedHashMap<>();
            // This reload's tags, which are bound to the registry only once every listener is done.
            HolderLookup.RegistryLookup<Block> blocks = getRegistryLookup().lookupOrThrow(Registries.BLOCK);
            prepared.forEach((id, shape) -> {
                List<Identifier> unknown = shape.blocks().stream()
                        .filter(block -> !BuiltInRegistries.BLOCK.containsKey(block))
                        .toList();
                List<Identifier> unpaid = shape.blocks().stream()
                        .filter(block -> BuiltInRegistries.BLOCK.getOptional(block)
                                .map(found -> found.asItem() == Items.AIR).orElse(false))
                        .toList();
                List<Identifier> unknownTags = shape.tags().stream()
                        .filter(tag -> blocks.get(blockTag(tag)).isEmpty())
                        .toList();
                if (!unknown.isEmpty()) {
                    LOGGER.error("Couldn't load assembly '{}': it names blocks the game lacks, {}", id, unknown);
                } else if (!unknownTags.isEmpty()) {
                    LOGGER.error("Couldn't load assembly '{}': it names block tags the game lacks, {}", id, unknownTags);
                } else if (!unpaid.isEmpty()) {
                    LOGGER.error("Couldn't load assembly '{}': it names blocks with no item to pay for them with, {}",
                            id, unpaid);
                } else {
                    loaded.put(id, shape);
                }
            });
            server = Map.copyOf(loaded);
        }
    }
}
