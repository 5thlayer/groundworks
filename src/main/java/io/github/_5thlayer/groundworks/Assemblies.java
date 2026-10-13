// SPDX-FileCopyrightText: 2026 5thlayer
// SPDX-License-Identifier: MIT

package io.github._5thlayer.groundworks;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import com.mojang.logging.LogUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.FileToIdConverter;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
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
 * <p>A held block item is a Key when its block is in {@link VanillaConsumer#ASSEMBLY_KEYS} and a
 * declaration names it. Its plan is the whole shape, resting on the aimed face and facing the
 * player, the Key last; a sneak places the Key alone, as vanilla does, and so does a Key whose
 * declaration starts it alone.
 */
public final class Assemblies {

    private static final Logger LOGGER = LogUtils.getLogger();

    /** The declarations the server loaded, by id. */
    private static volatile Map<Identifier, AssemblyShape> server = Map.of();

    /** The declarations the server sent this client, by id: apart from the server's, which in single player shares these statics. */
    private static volatile Map<Identifier, AssemblyShape> client = Map.of();

    private Assemblies() {
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
        Identifier id = BuiltInRegistries.BLOCK.getKey(key);
        return (level.isClientSide() ? client : server).entrySet().stream()
                .filter(entry -> entry.getValue().keyBlock().equals(id))
                .sorted(Map.Entry.comparingByKey(Comparator.comparing(Identifier::toString)))
                .map(Map.Entry::getValue)
                .toList();
    }

    /** The Assembly the held stack places, or {@code null} for the Key alone. */
    private static @Nullable AssemblyShape chosen(Level level, ItemStack stack) {
        if (!(stack.getItem() instanceof BlockItem item)
                || !item.getBlock().defaultBlockState().is(VanillaConsumer.ASSEMBLY_KEYS)) {
            return null;
        }
        List<AssemblyShape> assemblies = keyedBy(level, item.getBlock());
        if (assemblies.isEmpty() || assemblies.getFirst().startsAlone()) {
            return null;
        }
        return assemblies.getFirst();
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
     * An Assembly's plan, what it costs, and where its Key goes.
     *
     * @param others the blocks laid before the Key, in the plan's order
     */
    private record Laying(PlacementPlan plan, List<PlacementPlan.Placed> others, Charge charge, BlockPos key) {
    }

    private static @Nullable Laying laying(Level level, @Nullable Player player, ItemStack stack, BlockHitResult hit) {
        if (player != null && player.isShiftKeyDown()) {
            return null;
        }
        AssemblyShape shape = chosen(level, stack);
        if (shape == null) {
            return null;
        }
        BlockItem keyItem = (BlockItem) stack.getItem();
        BlockPlaceContext context = new BlockPlaceContext(level, player, InteractionHand.MAIN_HAND, stack, hit);
        Direction facing = context.getHorizontalDirection().getOpposite();
        List<AssemblyShape.At> positions = shape.positions(context.getClickedPos(), hit.getDirection(), facing);

        List<PlacementPlan.Placed> others = new ArrayList<>(positions.size());
        Map<Item, Integer> cost = new LinkedHashMap<>();
        BlockPos taken = null;
        for (AssemblyShape.At at : positions.subList(0, positions.size() - 1)) {
            Optional<Block> block = BuiltInRegistries.BLOCK.getOptional(((AssemblyShape.Legend.Block) at.legend()).id());
            if (block.isEmpty()) {
                // A declaration a client was sent for a block it lacks: nothing honest to draw.
                return null;
            }
            BlockState there = level.getBlockState(at.pos());
            if (there.is(block.get())) {
                // It stands already, as the shape takes it, so it stays and is not charged.
                continue;
            }
            BlockState state = block.get().defaultBlockState();
            others.add(new PlacementPlan.Placed(at.pos(), state));
            cost.merge(block.get().asItem(), 1, Integer::sum);
            if (taken == null && !fits(level, at.pos(), there, state)) {
                taken = at.pos();
            }
        }

        BlockPos keyPos = positions.getLast().pos();
        PlacementPlan alone = Placements.vanillaPlan(keyItem, BlockPlaceContext.at(context, keyPos, hit.getDirection()));
        BlockState keyState = alone == null ? keyItem.getBlock().defaultBlockState() : alone.blocks().getFirst().state();
        if (taken == null && (alone == null || alone.isRefused() || !alone.blocks().getFirst().pos().equals(keyPos))) {
            taken = keyPos;
        }
        // The Key is charged with the rest, last, and placed from a copy of the stack, so charging
        // the other blocks never empties the hand it is placed from.
        cost.merge(keyItem, 1, Integer::sum);
        Charge charge = Charge.of(player, cost, List.of());

        List<PlacementPlan.Placed> blocks = new ArrayList<>(others);
        blocks.add(new PlacementPlan.Placed(keyPos, keyState));
        Set<BlockPos> all = new LinkedHashSet<>();
        blocks.forEach(placed -> all.add(placed.pos()));
        Refusal refusal = null;
        if (taken != null) {
            refusal = new Refusal.At(Refusal.Assembly.TAKEN, taken);
        } else if (player != null && !Charge.mayBuildAll(level, player, all)) {
            refusal = Refusal.Assembly.MAY_NOT_BUILD;
        } else if (player != null && !charge.affordable(player)) {
            refusal = Refusal.Assembly.NOT_ENOUGH_ITEMS;
        }
        PlacementPlan plan = refusal == null ? PlacementPlan.accepted(blocks) : PlacementPlan.refused(blocks, refusal);
        return new Laying(plan, others, charge, keyPos);
    }

    /**
     * Whether a block of the shape may go here: inside the world, on a block a placement replaces,
     * such as air, water or grass, and clear of entities.
     */
    private static boolean fits(Level level, BlockPos pos, BlockState there, BlockState state) {
        return level.isInWorldBounds(pos) && there.canBeReplaced()
                && level.isUnobstructed(state, pos, CollisionContext.empty());
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
            tell(player, message(player, laying, plan.refusal()));
            return InteractionResult.SUCCESS;
        }
        // A claim mod's guard on the place event, at every position: the click's own event never fires.
        for (PlacementPlan.Placed placed : plan.blocks()) {
            if (EventHooks.onBlockPlace(player, BlockSnapshot.create(level.dimension(), level, placed.pos()), Direction.UP)) {
                tell(player, message(player, laying, Refusal.Assembly.MAY_NOT_BUILD));
                return InteractionResult.SUCCESS;
            }
        }
        ItemStack key = held.copyWithCount(1);
        laying.charge().lay(level, player, PlacementPlan.accepted(laying.others()));
        BlockPlaceContext context = new BlockPlaceContext(level, player, hand, key.copy(), hit);
        BlockItem item = (BlockItem) key.getItem();
        // Vanilla's placement, so the Key's own rules run as it goes down, a golem's pumpkin's.
        if (!item.place(BlockPlaceContext.at(context, laying.key(), hit.getDirection())).consumesAction()
                && !player.hasInfiniteMaterials()) {
            player.getInventory().placeItemBackInInventory(key);
        }
        return InteractionResult.SUCCESS;
    }

    private static Component message(Player player, Laying laying, Refusal refusal) {
        Refusal reason = refusal instanceof Refusal.At at ? at.reason() : refusal;
        if (!(reason instanceof Refusal.Assembly assembly)) {
            return Component.translatable("message.groundworks.assembly_taken");
        }
        return switch (assembly) {
            case TAKEN -> Component.translatable("message.groundworks.assembly_taken");
            case MAY_NOT_BUILD -> Component.translatable("message.groundworks.assembly_may_not_build");
            case NOT_ENOUGH_ITEMS -> Component.translatable("message.groundworks.assembly_not_enough_items",
                    missing(laying.charge().missing(player)));
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
     * with its id and skipped by the reader, and one naming a block the game lacks logged and
     * skipped here.
     */
    private static final class Loader extends SimpleJsonResourceReloadListener<AssemblyShape> {

        Loader() {
            super(AssemblyShape.CODEC, FileToIdConverter.json(Groundworks.MOD_ID + "/assembly"));
        }

        @Override
        protected void apply(Map<Identifier, AssemblyShape> prepared, ResourceManager manager, ProfilerFiller profiler) {
            Map<Identifier, AssemblyShape> loaded = new LinkedHashMap<>();
            prepared.forEach((id, shape) -> {
                List<Identifier> unknown = shape.blocks().stream()
                        .filter(block -> !BuiltInRegistries.BLOCK.containsKey(block))
                        .toList();
                if (unknown.isEmpty()) {
                    loaded.put(id, shape);
                } else {
                    LOGGER.error("Couldn't load assembly '{}': it names blocks the game lacks, {}", id, unknown);
                }
            });
            server = Map.copyOf(loaded);
        }
    }
}
