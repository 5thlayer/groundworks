// SPDX-FileCopyrightText: 2026 5thlayer
// SPDX-License-Identifier: MIT

package io.github._5thlayer.groundworks;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Predicate;

import com.mojang.logging.LogUtils;
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
import net.neoforged.neoforge.event.AddServerReloadListenersEvent;
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

    /** The held Key's choice of Assembly; none is the start its declaration names. */
    private static final HeldValue<AssemblyChoice> CHOICE = new HeldValue<>(() -> Groundworks.ASSEMBLY_CHOICE.get(), null);

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
        int choice = AssemblyChoice.current(CHOICE.get(stack), start(assemblies),
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
     * The Assembly's part of a press of Next Choice, or of its reverse: a held Key steps to its
     * next choice, or its previous, through its Assemblies and the Key alone and round. A stack
     * that is no Key is left as it is.
     *
     * @return whether the stack was a Key, so the press was the Assembly's
     */
    static boolean stepChoice(Level level, ItemStack held, boolean reverse) {
        List<AssemblyShape> assemblies = choicesOf(level, held);
        if (assemblies.isEmpty()) {
            return false;
        }
        AssemblyChoice next = AssemblyChoice.next(CHOICE.get(held), start(assemblies),
                assemblies.size() + 1, reverse);
        CHOICE.set(held, next);
        return true;
    }

    /**
     * What a main-hand click with this stack would place, or {@code null} where it places no
     * Assembly: the item is no Key, or the Key goes alone.
     */
    static @Nullable PlacementPlan planFor(Level level, @Nullable Player player, ItemStack stack, BlockHitResult hit) {
        Assembling assembling = assembling(level, player, stack, hit);
        return assembling == null ? null : assembling.laying().plan();
    }

    /** The refusals a laying makes for an Assembly, which hands nothing back. */
    private static final Laying.Reasons REASONS = new Laying.Reasons(Refusal.Assembly.MAY_NOT_BUILD,
            Refusal.Assembly.NOT_ENOUGH_ITEMS, null);

    /**
     * An Assembly a click would lay, and what the inventory lacks for it.
     *
     * @param laying its plan, the Key last, its {@linkplain PlacementPlan#clears clears} the empty
     *     positions holding a block a placement overwrites, cleared before the rest is laid; and its
     *     charge, the blocks by the fill and the Key, taken once the Key is placed
     * @param missing what the main inventory lacks for the plan, by the fill's count, none when it
     *     affords it
     */
    private record Assembling(Laying laying, Map<Item, Integer> missing) {

        /** The blocks laid before the Key, in the plan's order. */
        List<PlacementPlan.Placed> others() {
            List<PlacementPlan.Placed> blocks = laying.plan().blocks();
            return blocks.subList(0, blocks.size() - 1);
        }

        /** Where the Key goes. */
        BlockPos key() {
            return laying.plan().blocks().getLast().pos();
        }

        /** The state the Key goes down in. */
        BlockState keyState() {
            return laying.plan().blocks().getLast().state();
        }

        /** The Key's name, as the player is told of it. */
        Component keyName() {
            return keyState().getBlock().getName();
        }
    }

    private static @Nullable Assembling assembling(Level level, @Nullable Player player, ItemStack stack,
            BlockHitResult hit) {
        AssemblyShape shape = placed(level, player, stack);
        if (shape == null) {
            return null;
        }
        BlockItem keyItem = (BlockItem) stack.getItem();
        BlockPlaceContext context = new BlockPlaceContext(level, player, InteractionHand.MAIN_HAND, stack, hit);
        Direction facing = context.getHorizontalDirection().getOpposite();
        List<AssemblyShape.At> positions = shape.positions(context.getClickedPos(), hit.getDirection(), facing);
        boolean free = player == null || player.hasInfiniteMaterials();
        AssemblyPlanning.Planned planned = AssemblyPlanning.plan(positions, shape.keyBlock(),
                new WorldGround(level, player, stack, hit, keyItem, turnFrom(facing)),
                player == null ? Map.of() : held(player), Assemblies::members, free);
        if (planned == null) {
            return null;
        }
        PlacementPlan plan = planned.plan();
        // The Key is charged with the rest, once it is placed, as the fill counted it.
        Map<Item, Integer> cost = new LinkedHashMap<>();
        plan.blocks().subList(0, plan.blocks().size() - 1)
                .forEach(placed -> cost.merge(placed.state().getBlock().asItem(), 1, Integer::sum));
        cost.merge(keyItem, 1, Integer::sum);
        Map<Item, Integer> missing = new LinkedHashMap<>();
        planned.missing().forEach((id, count) -> BuiltInRegistries.BLOCK.getOptional(id)
                .ifPresent(block -> missing.merge(block.asItem(), count, Integer::sum)));
        return new Assembling(Laying.of(level, player, plan, Charge.of(player, cost, List.of()), REASONS,
                missing::isEmpty), missing);
    }

    /** The world as the click finds it, read for an Assembly's planning. */
    private record WorldGround(Level level, @Nullable Player player, ItemStack stack, BlockHitResult hit,
            BlockItem keyItem, Rotation turn) implements AssemblyPlanning.Ground {

        @Override
        public boolean knows(Identifier block) {
            return BuiltInRegistries.BLOCK.containsKey(block);
        }

        @Override
        public boolean holds(BlockPos pos, AssemblyShape.Legend.Laid legend) {
            BlockState there = level.getBlockState(pos);
            return switch (legend) {
                case AssemblyShape.Legend.Block named ->
                        BuiltInRegistries.BLOCK.getOptional(named.id()).map(there::is).orElse(false);
                case AssemblyShape.Legend.Tag tag -> there.is(blockTag(tag.tag()));
            };
        }

        @Override
        public boolean replaceable(BlockPos pos) {
            return level.getBlockState(pos).canBeReplaced();
        }

        @Override
        public boolean clearChanges(BlockPos pos) {
            BlockState there = level.getBlockState(pos);
            return there != Laying.emptied(there);
        }

        @Override
        public @Nullable BlockPos otherHalf(BlockPos pos) {
            BlockState there = level.getBlockState(pos);
            if (!there.hasProperty(BlockStateProperties.DOUBLE_BLOCK_HALF)) {
                return null;
            }
            DoubleBlockHalf half = there.getValue(BlockStateProperties.DOUBLE_BLOCK_HALF);
            BlockPos other = half == DoubleBlockHalf.LOWER ? pos.above() : pos.below();
            BlockState otherState = level.getBlockState(other);
            return otherState.is(there.getBlock())
                    && otherState.getValue(BlockStateProperties.DOUBLE_BLOCK_HALF) != half
                    && clearChanges(other) ? other : null;
        }

        @Override
        public boolean inWorld(BlockPos pos) {
            return level.isInWorldBounds(pos);
        }

        @Override
        public BlockState state(Identifier block, BlockPos pos) {
            return BuiltInRegistries.BLOCK.getValue(block).defaultBlockState().rotate(level, pos, turn);
        }

        /**
         * The placing player's own body aside, which the click lifts out of the shape; not what
         * they ride, which stays where it is.
         */
        @Override
        public boolean entityInTheWay(BlockPos pos, BlockState state) {
            VoxelShape shape = state.getCollisionShape(level, pos, CollisionContext.empty()).move(pos);
            return !shape.isEmpty() && !level.getEntities(player, shape.bounds(), EntitySelector.NO_SPECTATORS.and(
                    entity -> !entity.isRemoved() && entity.blocksBuilding
                            && Shapes.joinIsNotEmpty(shape, Shapes.create(entity.getBoundingBox()), BooleanOp.AND)))
                    .isEmpty();
        }

        @Override
        public AssemblyPlanning.KeyPlacing key(BlockPos keyPos, List<BlockPos> cleared, List<PlacementPlan.Placed> others) {
            Map<BlockPos, BlockState> laid = new HashMap<>();
            cleared.forEach(pos -> laid.put(pos, Laying.emptied(level.getBlockState(pos))));
            others.forEach(placed -> laid.put(placed.pos(), placed.state()));
            BlockPlaceContext onTheLaid = new BlockPlaceContext(new PlannedLevel(level, laid, player), player,
                    InteractionHand.MAIN_HAND, stack, hit);
            return keyPlacing(keyItem, BlockPlaceContext.at(onTheLaid, keyPos, hit.getDirection()));
        }
    }

    /**
     * The Key's placement as vanilla's own would choose it, asked of the world as the rest of the
     * laying leaves it: so a torch hangs on a wall the shape builds, and a door finds no room under
     * a block the shape lays over it. A Key whose item needn't survive, as scaffolding's needn't,
     * stands anywhere, as vanilla's {@code BlockItem#canPlace} has it.
     */
    private static AssemblyPlanning.KeyPlacing keyPlacing(BlockItem item, BlockPlaceContext context) {
        BlockState drawn = item.getBlock().defaultBlockState();
        VanillaPlacement placement = VanillaPlacement.of(item, context);
        // A Key that would move, as scaffolding moves up a tower of itself, has no place in the shape.
        if (placement.context() != null && !placement.context().getClickedPos().equals(context.getClickedPos())) {
            return new AssemblyPlanning.KeyPlacing(drawn, Refusal.Assembly.TAKEN);
        }
        return switch (placement.outcome()) {
            case PLACES -> new AssemblyPlanning.KeyPlacing(placement.state(), null);
            case WOULD_NOT_SURVIVE -> new AssemblyPlanning.KeyPlacing(placement.state(),
                    Refusal.Assembly.KEY_WOULD_NOT_STAND);
            case REFUSED -> new AssemblyPlanning.KeyPlacing(placement.state(), Refusal.Assembly.TAKEN);
            case DISABLED, CANNOT_PLACE, NO_CONTEXT, NO_STATE -> new AssemblyPlanning.KeyPlacing(drawn,
                    Refusal.Assembly.TAKEN);
        };
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
        Assembling assembling = assembling(level, player, held, hit);
        if (assembling == null) {
            return InteractionResult.PASS;
        }
        if (level.isClientSide()) {
            return InteractionResult.SUCCESS;
        }
        assembling.laying().carryOut(level, player, refusal -> message(assembling, refusal),
                () -> lay(level, player, hand, held, hit, assembling));
        return InteractionResult.SUCCESS;
    }

    /**
     * Lays the Assembly the click carries out: clears its empty positions, puts down the blocks
     * before the Key, and places the Key last through vanilla's own placement; or, where vanilla
     * refuses the Key, takes back what it laid.
     */
    private static void lay(Level level, Player player, InteractionHand hand, ItemStack held, BlockHitResult hit,
            Assembling assembling) {
        PlacementPlan plan = assembling.laying().plan();
        Map<BlockPos, BlockState> before = new HashMap<>();
        Laying.changed(plan).forEach(pos -> before.put(pos, level.getBlockState(pos)));
        assembling.laying().clear(level);
        Charge.put(level, player, assembling.others());
        // The plan let the player's own body stand in the shape: they are lifted clear of it, and
        // of the Key, which vanilla places only clear of them.
        Vec3 stood = player.position();
        Vec3 clear = liftedClear(level, player, assembling.key(), assembling.keyState());
        if (clear != null && !clear.equals(stood)) {
            player.teleportTo(clear.x, clear.y, clear.z);
        }
        // Vanilla's placement, so the Key's own rules run as it goes down, a golem's pumpkin's. It
        // is placed from a copy, so the stack it is placed from is charged with the rest, once it
        // stands.
        BlockItem item = (BlockItem) held.getItem();
        ItemStack key = held.copyWithCount(1);
        BlockPlaceContext context = new BlockPlaceContext(level, player, hand, key, hit);
        if (item.place(BlockPlaceContext.at(context, assembling.key(), hit.getDirection())).consumesAction()) {
            // Vanilla told the Key; each block laid before it that still stands, not one a golem
            // took, is told now, with its own item, so a Key it refused tells none.
            for (PlacementPlan.Placed placed : assembling.others()) {
                if (level.getBlockState(placed.pos()).is(placed.state().getBlock())) {
                    Charge.tellPlaced(level, player, placed.pos(), new ItemStack(placed.state().getBlock()));
                }
            }
            assembling.laying().charge().pay(player);
            return;
        }
        // The plan took the Key on the world as the laying leaves it, so vanilla refuses it only
        // for what the plan doesn't plan: an entity in its way, as a golem the laying spawned, or
        // another mod's rule. What the laying set is taken back, and what is gone, as a golem's
        // blocks are, is charged; the Key, unplaced, isn't, and the player is told just that.
        Charge.of(player, takeBack(level, assembling, before), List.of()).pay(player);
        if (!player.position().equals(stood)) {
            player.teleportTo(stood.x, stood.y, stood.z);
        }
        Laying.tell(player, Component.translatable("message.groundworks.assembly_key_not_placed", assembling.keyName()));
    }

    /**
     * Puts back what stood before the laying, the last laid first, wherever something of it still
     * stands, as it was laid or changed since, as concrete powder hardens; and answers what is gone,
     * leaving only what a placement overwrites, and so still costs the player: a laid block a
     * golem the laying spawned took into itself, say.
     */
    private static Map<Item, Integer> takeBack(Level level, Assembling assembling, Map<BlockPos, BlockState> before) {
        Map<Item, Integer> gone = new LinkedHashMap<>();
        for (PlacementPlan.Placed placed : assembling.others().reversed()) {
            if (level.getBlockState(placed.pos()).canBeReplaced()) {
                gone.merge(placed.state().getBlock().asItem(), 1, Integer::sum);
            } else {
                level.setBlock(placed.pos(), before.get(placed.pos()), Block.UPDATE_ALL);
            }
        }
        for (BlockPos pos : assembling.laying().plan().clears().reversed()) {
            BlockState was = before.get(pos);
            if (level.getBlockState(pos).is(Laying.emptied(was).getBlock())) {
                level.setBlock(pos, was, Block.UPDATE_ALL);
            }
        }
        return gone;
    }

    /** What the player is told of a laying's refusal: one of its own reasons, or one of them at a position. */
    private static Component message(Assembling assembling, Refusal refusal) {
        return switch (Laying.reason(refusal)) {
            case Refusal.Assembly.TAKEN -> Component.translatable("message.groundworks.assembly_taken");
            case Refusal.Assembly.KEY_WOULD_NOT_STAND -> Component.translatable(
                    "message.groundworks.assembly_key_would_not_stand", assembling.keyName());
            case Refusal.Assembly.MAY_NOT_BUILD -> Component.translatable("message.groundworks.assembly_may_not_build");
            case Refusal.Assembly.NOT_ENOUGH_ITEMS -> Component.translatable(
                    "message.groundworks.assembly_not_enough_items", missing(assembling.missing()));
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
