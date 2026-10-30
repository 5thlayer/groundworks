// SPDX-FileCopyrightText: 2026 5thlayer
// SPDX-License-Identifier: MIT

package io.github._5thlayer.groundworks;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Predicate;

import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntitySelector;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.DamageResistant;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BedPart;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.BooleanOp;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import org.jspecify.annotations.Nullable;

/**
 * Blueprints (ADR 0014): a box of blocks captured from the world onto a sheet, and pasted
 * elsewhere as an {@linkplain Assemblies Assembly} is placed, without a Key.
 *
 * <p>A sheet is a held item a Consumer has {@linkplain #sheets stated} captures, as the
 * {@linkplain VanillaConsumer vanilla Consumer} does for paper through its tag. A blank sheet's
 * sneak-click on a block stores the first Corner, and a click on a block with a Corner stored
 * captures the Selection between the two, both included, as a Copy, leaving out what it can't take
 * {@linkplain BlueprintSelection whole} and saying so: one sheet comes off the held
 * stack and holds the Blueprint, alone in its stack. The Blueprint's blocks go into the server's
 * world data, under an id the sheet holds; the client is sent them while it holds the sheet.
 *
 * <p>A sheet holding a Blueprint plans its paste at the aim, as every Groundworks plan is planned:
 * resting on the aimed face in the {@linkplain BlueprintLanding orientation it was captured in},
 * turned from it by {@linkplain Rotate Rotate the Plan} and moved by {@linkplain Raise Raise and
 * Lower} as any held stack's placement is, air skipped, or, where Next Choice set the sheet to
 * paste air, what stands there broken as the player would break it, its drops handed back as a
 * Dismantle's are and an unbreakable block refusing the paste; a block already standing as wanted
 * kept uncharged, refused whole where a position
 * it places is taken, and each block charged as the item a player would pick it with, all or none,
 * and free in creative. A sneak-use in the air clears a stored Corner or a sheet's Copy.
 *
 * <p>The paste stands as it was captured: what hangs or stands on another block is laid after
 * it, and vanilla's rules, each block told it was placed, run on the finished paste. A Companion,
 * a door's upper half or a bed's head, is placed by its block and not charged. A Copy carries
 * block states alone, so a chest pastes empty and a sign blank; a charged paste lays waterlogged
 * blocks dry and skips fluid sources and blocks with no item of their own, which creative's lays.
 * A block that can't stand where it lands is left out, uncharged, and the rest is laid.
 *
 * <p>Next Choice sets a blank sheet to capture a {@linkplain BlueprintCut Cut} instead, which takes
 * its blocks out of the world with what they hold, leaving out those the player could not break.
 * A Cut is pasted once, uncharged, with what its blocks held, a block that can't stand dropped at
 * the player's feet, and the sheet is blank again; it is never cleared, and its sheet survives
 * fire and lava and never despawns.
 */
public final class Blueprints {

    private static final List<Predicate<? super ItemStack>> SHEETS = new CopyOnWriteArrayList<>();

    private static final List<Predicate<? super BlockState>> DENIED = new CopyOnWriteArrayList<>();

    /** A blank sheet's stored first Corner. */
    private static final HeldValue<BlueprintCorner> CORNER = new HeldValue<>(() -> Groundworks.BLUEPRINT_CORNER.get(), null);

    /** The id of the Blueprint a sheet holds; none on a blank sheet. */
    private static final HeldValue<UUID> HELD = new HeldValue<>(() -> Groundworks.BLUEPRINT.get(), null);

    /** The sheet's choice, stepped by Next Choice; none on a sheet at its start. */
    private static final HeldValue<SheetChoice> CHOICE = new HeldValue<>(() -> Groundworks.SHEET_CHOICE.get(), null);

    /** The refusals a laying makes for a paste. */
    private static final Laying.Reasons REASONS = new Laying.Reasons(Refusal.Blueprint.MAY_NOT_BUILD,
            Refusal.Blueprint.NOT_ENOUGH_ITEMS, null);

    /**
     * The Blueprints the server sent this client, by id, the latest few: apart from the server's
     * store, each its blocks and kind alone.
     */
    private static final Map<UUID, Blueprint> CLIENT = Collections.synchronizedMap(
            new LinkedHashMap<>(16, 0.75F, true) {
                @Override
                protected boolean removeEldestEntry(Map.Entry<UUID, Blueprint> eldest) {
                    return size() > 8;
                }
            });

    /** How many Blueprints have reached this client, so a preview drawn before one arrived is drawn again. */
    private static volatile int arrivals;

    /** The Blueprint each player was last sent, by the player's id, so a held sheet is sent once. */
    private static final Map<UUID, UUID> SENT = new ConcurrentHashMap<>();

    private Blueprints() {
    }

    /**
     * States that the items this matches are sheets, capturing and pasting Blueprints. Called at
     * mod construction, on both sides, since the plan is asked of on both; mods are constructed in
     * parallel, so this may be called from several threads at once.
     */
    public static void sheets(Predicate<? super ItemStack> items) {
        SHEETS.add(items);
    }

    /** Whether some Consumer has stated this stack is a sheet. */
    public static boolean isSheet(ItemStack stack) {
        if (stack.isEmpty()) {
            return false;
        }
        for (Predicate<? super ItemStack> items : SHEETS) {
            if (items.test(stack)) {
                return true;
            }
        }
        return false;
    }

    /**
     * States that a capture leaves the blocks this matches out, silently, as the vanilla Consumer
     * does for bedrock through its tag. Called at mod construction, on both sides, since the
     * Selection's outline asks on the client; this may be called from several threads at once.
     */
    public static void denies(Predicate<? super BlockState> blocks) {
        DENIED.add(blocks);
    }

    /** Whether some Consumer has stated a capture leaves this block out. */
    static boolean isDenied(BlockState state) {
        for (Predicate<? super BlockState> blocks : DENIED) {
            if (blocks.test(state)) {
                return true;
            }
        }
        return false;
    }

    static void register(IEventBus modBus) {
        modBus.addListener(BlueprintPayload::register);
        NeoForge.EVENT_BUS.addListener(PlayerTickEvent.Post.class, event -> {
            if (event.getEntity() instanceof ServerPlayer player) {
                sendHeld(player);
            }
        });
        NeoForge.EVENT_BUS.addListener(PlayerEvent.PlayerLoggedOutEvent.class,
                event -> SENT.remove(event.getEntity().getUUID()));
        NeoForge.EVENT_BUS.addListener(EntityJoinLevelEvent.class, Blueprints::onEntityJoin);
    }

    // ---- The sheet's choice --------------------------------------------------------------------

    /**
     * The choices a sheet offers Next Choice, its start first: a blank sheet captures a Copy or a
     * Cut, and a sheet holding a Blueprint skips its air or pastes it. None for an item that is no
     * sheet.
     */
    private static List<SheetChoice> choicesOf(ItemStack stack) {
        if (!isSheet(stack)) {
            return List.of();
        }
        return holdsBlueprint(stack) ? SheetChoice.PASTE : SheetChoice.CAPTURE;
    }

    /** Whether the stack is a blank sheet set to capture a Cut. */
    public static boolean cuts(ItemStack stack) {
        return isSheet(stack) && !holdsBlueprint(stack)
                && SheetChoice.current(CHOICE.get(stack), choicesOf(stack)) == SheetChoice.CUT;
    }

    /**
     * Steps the sheet's choice one on, or back when {@code reverse}, and round, telling the player
     * the choice it is on; whether the stack is a sheet with choices, which took the press.
     */
    static boolean stepChoice(Player player, ItemStack held, boolean reverse) {
        List<SheetChoice> choices = choicesOf(held);
        if (choices.isEmpty()) {
            return false;
        }
        SheetChoice next = SheetChoice.next(CHOICE.get(held), choices, reverse);
        CHOICE.set(held, next == choices.getFirst() ? null : next);
        Laying.tell(player, Component.translatable(next.message()));
        return true;
    }

    /** Whether a paste of the sheet clears what stands where its Blueprint has air. */
    private static boolean pastesAir(ItemStack stack) {
        return SheetChoice.current(CHOICE.get(stack), choicesOf(stack)) == SheetChoice.PASTE_AIR;
    }

    /** The stored first Corner, or {@code null} where none is stored or it is in another dimension. */
    public static @Nullable BlueprintCorner liveCorner(Level level, ItemStack stack) {
        BlueprintCorner corner = CORNER.get(stack);
        return corner != null && corner.dimension().equals(level.dimension()) ? corner : null;
    }

    /**
     * Whether this stack is a sheet holding a Blueprint, so its paste is planned: Rotate turns it
     * and Raise moves it, where a blank sheet is no plan and neither does.
     */
    static boolean pastes(ItemStack stack) {
        return isSheet(stack) && holdsBlueprint(stack);
    }

    /** Whether the stack is a sheet whose Blueprint, in the server's store, is a Cut. */
    private static boolean holdsCut(Level level, ItemStack stack) {
        UUID id = isSheet(stack) ? HELD.get(stack) : null;
        MinecraftServer server = level.getServer();
        Blueprint blueprint = id == null || server == null ? null : BlueprintStore.of(server).get(id);
        return blueprint != null && blueprint.kind() == Blueprint.Kind.CUT;
    }

    /** The id of the Blueprint the sheet holds, or {@code null} on a blank sheet. */
    static @Nullable UUID heldId(ItemStack stack) {
        return HELD.get(stack);
    }

    /** Whether the sheet holds a Blueprint, whether or not this side has its blocks yet. */
    public static boolean holdsBlueprint(ItemStack stack) {
        return HELD.get(stack) != null;
    }

    // ---- The client's copies -------------------------------------------------------------------

    /** A Blueprint's blocks, and whether it is a Cut, as the server sent them to this client. */
    static void received(UUID id, BlueprintBlocks blocks, boolean cut) {
        CLIENT.put(id, new Blueprint(blocks, cut ? Blueprint.Kind.CUT : Blueprint.Kind.COPY, new UUID(0, 0), 0,
                List.of()));
        arrivals++;
    }

    /** How many Blueprints have reached this client; a preview cached before the last arrival is stale. */
    public static int arrivals() {
        return arrivals;
    }

    /** This client left its server, whose Blueprints are no other server's. */
    public static void forget() {
        CLIENT.clear();
        arrivals++;
    }

    /** Sends the player the Blueprint their main hand's sheet holds, once per sheet held. */
    private static void sendHeld(ServerPlayer player) {
        ItemStack held = player.getMainHandItem();
        UUID id = isSheet(held) ? HELD.get(held) : null;
        if (id == null) {
            SENT.remove(player.getUUID());
            return;
        }
        if (id.equals(SENT.get(player.getUUID()))) {
            return;
        }
        Blueprint blueprint = BlueprintStore.of(player.level().getServer()).get(id);
        if (blueprint == null || player.connection == null || !player.connection.hasChannel(BlueprintPayload.TYPE)) {
            return;
        }
        PacketDistributor.sendToPlayer(player,
                new BlueprintPayload(id, blueprint.blocks(), blueprint.kind() == Blueprint.Kind.CUT));
        SENT.put(player.getUUID(), id);
    }

    /**
     * The Blueprint the sheet holds, as this side has it, or {@code null}: the client's, sent it,
     * lacks its owner, its pastes and what a Cut's blocks hold.
     */
    private static @Nullable Blueprint blueprintOf(Level level, UUID id) {
        MinecraftServer server = level.getServer();
        return server == null ? CLIENT.get(id) : BlueprintStore.of(server).get(id);
    }

    // ---- The click on a block ------------------------------------------------------------------

    /** What a main-hand click on a block with a sheet does. */
    private enum Click {
        /** A sneak-click with a blank sheet: the first Corner is stored. */
        STORE_CORNER,
        /** A click with a blank sheet and a Corner stored: the Selection is captured. */
        CAPTURE,
        /** A click with a sheet holding a Blueprint: it is pasted. */
        PASTE,
        /** Not the Blueprint's: the click goes on as it would. */
        PASS
    }

    private static Click clickOf(Level level, @Nullable Player player, InteractionHand hand, ItemStack stack) {
        if (hand != InteractionHand.MAIN_HAND || !isSheet(stack)) {
            return Click.PASS;
        }
        if (holdsBlueprint(stack)) {
            return Click.PASTE;
        }
        if (player != null && player.isShiftKeyDown()) {
            return Click.STORE_CORNER;
        }
        return liveCorner(level, stack) != null ? Click.CAPTURE : Click.PASS;
    }

    /**
     * Whether a click with this stack is the Blueprint's, and what it would lay: a paste's plan,
     * or none for a Corner stored or a capture, or a paste whose Blueprint this side doesn't have
     * yet. {@code null} where the click isn't the Blueprint's.
     */
    static Feature.@Nullable Claim claim(Level level, @Nullable Player player, InteractionHand hand, ItemStack stack,
            BlockHitResult hit) {
        return switch (clickOf(level, player, hand, stack)) {
            case PASS -> null;
            case PASTE -> {
                Pasting pasting = pasting(level, player, stack, hit);
                yield new Feature.Claim(Feature.BLUEPRINT, pasting == null ? null : pasting.laying().plan());
            }
            default -> new Feature.Claim(Feature.BLUEPRINT, null);
        };
    }

    /** The click, carried out on the server, by the click {@link #claim} names. */
    static InteractionResult useOn(Player player, InteractionHand hand, BlockHitResult hit) {
        Level level = player.level();
        ItemStack held = player.getItemInHand(hand);
        Click click = clickOf(level, player, hand, held);
        if (click == Click.PASS) {
            return InteractionResult.PASS;
        }
        if (level.isClientSide()) {
            return InteractionResult.SUCCESS;
        }
        switch (click) {
            case STORE_CORNER -> {
                CORNER.set(held, new BlueprintCorner(level.dimension(), hit.getBlockPos()));
                Laying.tell(player, Component.translatable("message.groundworks.blueprint_corner_stored"));
            }
            case CAPTURE -> capture(player, held, liveCorner(level, held).pos(), hit.getBlockPos());
            case PASTE -> paste(player, held, hit);
            default -> {
            }
        }
        return InteractionResult.SUCCESS;
    }

    /** A use in the air: a sneak clears the stored Corner, or the sheet's Copy; a Cut's is refused. */
    static InteractionResult use(Player player, InteractionHand hand) {
        ItemStack held = player.getItemInHand(hand);
        if (hand != InteractionHand.MAIN_HAND || !player.isShiftKeyDown() || !isSheet(held)
                || (!holdsBlueprint(held) && CORNER.get(held) == null)) {
            return InteractionResult.PASS;
        }
        Level level = player.level();
        if (level.isClientSide()) {
            return InteractionResult.SUCCESS;
        }
        UUID id = HELD.get(held);
        BlueprintStore store = BlueprintStore.of(level.getServer());
        Blueprint blueprint = id == null ? null : store.get(id);
        if (blueprint != null && blueprint.kind() == Blueprint.Kind.CUT) {
            refuse(player, Refusal.Blueprint.CANT_CLEAR_A_CUT);
        } else if (id != null) {
            store.remove(id);
            blank(held);
            Laying.tell(player, Component.translatable("message.groundworks.blueprint_cleared"));
        } else {
            CORNER.set(held, null);
            Laying.tell(player, Component.translatable("message.groundworks.blueprint_corner_cleared"));
        }
        return InteractionResult.SUCCESS;
    }

    /** Makes a sheet blank again, stacking with any other blank sheet of its item. */
    private static void blank(ItemStack sheet) {
        HELD.set(sheet, null);
        CHOICE.set(sheet, null);
        // Set back to the item's own, which drops the stack's patch: a removal would patch it out.
        sheet.set(DataComponents.MAX_STACK_SIZE, sheet.getItem().components().get(DataComponents.MAX_STACK_SIZE));
        sheet.set(DataComponents.DAMAGE_RESISTANT, sheet.getItem().components().get(DataComponents.DAMAGE_RESISTANT));
    }

    // ---- Capture --------------------------------------------------------------------------------

    /**
     * Captures the Selection between the Corners as a Copy, or as a Cut where the sheet is set to
     * one, onto one sheet off the held stack: the held sheet itself where it is the last, else a
     * new one put in the inventory. A Cut takes its blocks out of the world with what they hold,
     * and its sheet survives fire and lava. Refused, with its reason and nothing changed, over the
     * server's cap, reaching a chunk not loaded, or with no room for the new sheet.
     */
    private static void capture(Player player, ItemStack held, BlockPos first, BlockPos second) {
        Level level = player.level();
        BoundingBox box = BoundingBox.fromCorners(first, second);
        long positions = (long) box.getXSpan() * box.getYSpan() * box.getZSpan();
        int cap = GroundworksConfig.maxSelection();
        if (positions > cap) {
            refuse(player, Refusal.Blueprint.OVER_THE_CAP, positions, cap);
            return;
        }
        for (int x = SectionPos.blockToSectionCoord(box.minX()); x <= SectionPos.blockToSectionCoord(box.maxX()); x++) {
            for (int z = SectionPos.blockToSectionCoord(box.minZ()); z <= SectionPos.blockToSectionCoord(box.maxZ()); z++) {
                if (!level.hasChunk(x, z)) {
                    refuse(player, Refusal.Blueprint.UNLOADED);
                    return;
                }
            }
        }
        boolean split = held.getCount() > 1;
        if (split && player.getInventory().getFreeSlot() < 0) {
            refuse(player, Refusal.Blueprint.NO_ROOM_FOR_THE_SHEET);
            return;
        }
        boolean cut = cuts(held);
        Map<BlockPos, BlueprintSelection.LeftOut> leftOut = BlueprintSelection.leftOut(level, player, box, cut);
        BlueprintBlocks blocks = BlueprintBlocks.capture(level, box, leftOut.keySet());
        List<Blueprint.Held> contents = cut ? BlueprintCut.takeOut(level, box, leftOut.keySet()) : List.of();
        UUID id = UUID.randomUUID();
        BlueprintStore.of(level.getServer()).put(id, new Blueprint(blocks,
                cut ? Blueprint.Kind.CUT : Blueprint.Kind.COPY, player.getUUID(), System.currentTimeMillis(), contents));
        CORNER.set(held, null);
        ItemStack sheet = split ? held.copyWithCount(1) : held;
        HELD.set(sheet, id);
        CHOICE.set(sheet, null);
        sheet.set(DataComponents.MAX_STACK_SIZE, 1);
        if (cut) {
            sheet.set(DataComponents.DAMAGE_RESISTANT, new DamageResistant(
                    level.registryAccess().lookupOrThrow(Registries.DAMAGE_TYPE).getOrThrow(DamageTypeTags.IS_FIRE)));
        }
        if (split) {
            held.shrink(1);
            player.getInventory().add(sheet);
        }
        Component told = BlueprintSelection.told(level, leftOut);
        String key = cut ? "message.groundworks.blueprint_cut" : "message.groundworks.blueprint_copied";
        Laying.tell(player, told == null
                ? Component.translatable(key, blocks.standing())
                : Component.translatable(key + "_left_out", blocks.standing(), told));
    }

    private static void refuse(Player player, Refusal.Blueprint refusal, Object... args) {
        Laying.tell(player, Component.translatable(refusal.message(), args));
    }

    // ---- Paste ----------------------------------------------------------------------------------

    /**
     * A paste a click would lay.
     *
     * @param laying its plan, each block the captured state where it lands, {@linkplain SupportOrder
     *     support first}, a block that can't stand there left out; and its charge, each laid block's
     *     pick item
     * @param items the item each placed block is charged as, by its position, which it is told it
     *     was placed with
     * @param cut whether the Blueprint is a Cut, pasted once and uncharged
     * @param held what a Cut's blocks hold, by where they land; none for a Copy, or on the client
     * @param cantStand a Cut's blocks that can't stand where they land, which drop at the player's feet
     */
    private record Pasting(Laying laying, Map<BlockPos, Item> items, boolean cut, Map<BlockPos, CompoundTag> held,
            List<PlacementPlan.Placed> cantStand) {
    }

    /** The paste of the sheet's Blueprint at this aim, or {@code null} where this side lacks its blocks. */
    private static @Nullable Pasting pasting(Level level, @Nullable Player player, ItemStack stack, BlockHitResult hit) {
        UUID id = HELD.get(stack);
        Blueprint blueprint = id == null ? null : blueprintOf(level, id);
        if (blueprint == null) {
            return null;
        }
        BlueprintBlocks blocks = blueprint.blocks();
        boolean cut = blueprint.kind() == Blueprint.Kind.CUT;
        BlockPos spot = new BlockPlaceContext(level, player, InteractionHand.MAIN_HAND, stack, hit).getClickedPos();
        BlueprintLanding landing = BlueprintLanding.of(blocks.size(), spot, hit.getDirection(), Rotate.turnOf(stack));
        boolean charged = !cut && player != null && !player.hasInfiniteMaterials();
        Map<BlockPos, CompoundTag> held = new LinkedHashMap<>();
        for (Blueprint.Held contents : blueprint.held()) {
            held.put(landing.at(contents.local()), contents.data());
        }
        List<PlacementPlan.Placed> wanted = new ArrayList<>();
        Map<BlockPos, Item> items = new LinkedHashMap<>();
        List<BlockPos> clears = new ArrayList<>();
        boolean pastesAir = pastesAir(stack);
        Refusal refusal = null;
        for (BlueprintBlocks.Captured captured : blocks.blocks()) {
            BlockPos pos = landing.at(captured.local());
            if (captured.state().isAir()) {
                // A left-out position is nothing, not air: what stands there stays, even pasting air.
                if (pastesAir && !captured.leftOut()) {
                    BlockState there = level.getBlockState(pos);
                    if (Laying.emptied(there) != there) {
                        if (refusal == null && there.getDestroySpeed(level, pos) < 0) {
                            refusal = new Refusal.At(Refusal.Blueprint.CANT_BREAK, pos);
                        }
                        clears.add(pos);
                    }
                }
                continue;
            }
            // A Companion, a fluid that isn't a source, or an itemless block a charged paste skips
            // is not air: it is never cleared, nor is a block left out because it can't stand.
            BlockState state = carried(landing.turned(level, pos, captured.state()), charged);
            // A Cut's block is the only one of it: a block standing as wanted is in its way, as any block is.
            if (state == null || (!cut && level.getBlockState(pos) == state)) {
                continue;
            }
            Item item = pickItem(level, player, pos, state);
            if (charged && item == Items.AIR) {
                continue;
            }
            wanted.add(new PlacementPlan.Placed(pos, state));
            items.put(pos, item);
        }
        SupportOrder.Standing standing = SupportOrder.inWorld(level, player, wanted);
        List<PlacementPlan.Placed> companions = Placements.companionsOf(standing.laid());
        Map<Item, Integer> cost = new LinkedHashMap<>();
        BlockPos taken = null;
        for (PlacementPlan.Placed placed : standing.laid()) {
            taken = taken == null ? takenAt(level, placed) : taken;
            Item item = items.get(placed.pos());
            if (item != Items.AIR) {
                cost.merge(item, 1, Integer::sum);
            }
        }
        for (PlacementPlan.Placed companion : companions) {
            taken = taken == null ? takenAt(level, companion) : taken;
        }
        if (refusal == null && taken != null) {
            refusal = new Refusal.At(Refusal.Blueprint.TAKEN, taken);
        }
        PlacementPlan plan = new PlacementPlan(standing.laid(), List.of(), clears, companions,
                standing.cantStand().stream().map(PlacementPlan.Placed::pos).toList(), refusal);
        return new Pasting(Laying.of(level, player, plan, Charge.of(player, cut ? Map.of() : cost, List.of()), REASONS),
                items, cut, held, standing.cantStand());
    }

    /**
     * What a Copy carries of a captured state, or {@code null} where it places nothing: nothing for
     * air, for a Companion, which its block places, and for a fluid that isn't a source; and,
     * where the paste is charged, a waterlogged block dry. A block entity is never carried, so a
     * chest pastes empty and a sign blank.
     */
    private static @Nullable BlockState carried(BlockState state, boolean charged) {
        if (state.isAir() || isCompanion(state)
                || (state.getBlock() instanceof LiquidBlock && !state.getFluidState().isSource())) {
            return null;
        }
        if (charged && state.getOptionalValue(BlockStateProperties.WATERLOGGED).orElse(false)) {
            return state.setValue(BlockStateProperties.WATERLOGGED, false);
        }
        return state;
    }

    /** Whether the state is a Companion, the half its block places itself: a door's upper half, a bed's head. */
    private static boolean isCompanion(BlockState state) {
        return state.getOptionalValue(BlockStateProperties.DOUBLE_BLOCK_HALF).orElse(null) == DoubleBlockHalf.UPPER
                || state.getOptionalValue(BlockStateProperties.BED_PART).orElse(null) == BedPart.HEAD;
    }

    /** The position, where the block can't be put down there; else {@code null}. */
    private static @Nullable BlockPos takenAt(Level level, PlacementPlan.Placed placed) {
        BlockPos pos = placed.pos();
        return !level.isInWorldBounds(pos) || !level.getBlockState(pos).canBeReplaced()
                || entityInTheWay(level, pos, placed.state()) ? pos : null;
    }

    /** The item a player would pick the block with, where it lands; air for a block with none. */
    private static Item pickItem(Level level, @Nullable Player player, BlockPos pos, BlockState state) {
        ItemStack picked = player == null
                ? state.getCloneItemStack(level, pos, false)
                : state.getCloneItemStack(pos, level, false, player);
        return picked.isEmpty() ? Items.AIR : picked.getItem();
    }

    /** Whether an entity that blocks building, the placing player among them, stands where the block would. */
    private static boolean entityInTheWay(Level level, BlockPos pos, BlockState state) {
        VoxelShape shape = state.getCollisionShape(level, pos, CollisionContext.empty()).move(pos);
        return !shape.isEmpty() && !level.getEntities((net.minecraft.world.entity.Entity) null, shape.bounds(),
                EntitySelector.NO_SPECTATORS.and(entity -> !entity.isRemoved() && entity.blocksBuilding
                        && Shapes.joinIsNotEmpty(shape, Shapes.create(entity.getBoundingBox()), BooleanOp.AND)))
                .isEmpty();
    }

    /**
     * Pastes the sheet's Blueprint at the aim, all or none, the player told why where it is
     * refused; a sheet whose Blueprint is gone from the world data is made blank. A Cut's blocks
     * are laid with what they held, those that can't stand where they land dropped at the
     * player's feet, and the Cut is gone from the world data, its sheet blank.
     */
    private static void paste(Player player, ItemStack held, BlockHitResult hit) {
        Level level = player.level();
        Pasting pasting = pasting(level, player, held, hit);
        if (pasting == null) {
            blank(held);
            Laying.tell(player, Component.translatable("message.groundworks.blueprint_gone"));
            return;
        }
        Laying laying = pasting.laying();
        laying.carryOut(level, player, refusal -> message(laying, player, refusal), () -> {
            List<ItemStack> handed = clear(level, player, laying.plan().clears());
            laying.charge().lay(level, player, laying.plan());
            // The inventory first, and what doesn't fit at the player's feet, as a Dismantle hands back.
            handed.forEach(stack -> player.getInventory().placeItemBackInInventory(stack));
            for (PlacementPlan.Placed placed : laying.plan().blocks()) {
                Charge.tellPlaced(level, player, placed.pos(), new ItemStack(pasting.items().get(placed.pos())));
            }
            BlueprintStore store = BlueprintStore.of(level.getServer());
            UUID id = HELD.get(held);
            if (pasting.cut()) {
                BlueprintCut.putBack(level, laying.plan(), pasting.held());
                BlueprintCut.drop(level, player, pasting.cantStand(), pasting.items(), pasting.held());
                store.remove(id);
                blank(held);
                return;
            }
            Blueprint blueprint = store.get(id);
            if (blueprint != null) {
                store.put(id, UndoPaste.recorded(blueprint, level, laying.plan()).usedAt(System.currentTimeMillis()));
            }
        });
    }

    /** What the removal at {@link #catching} spills into the world, caught to be handed back; none between removals. */
    private static @Nullable List<ItemStack> caught;

    /** The position whose removal is caught. */
    private static @Nullable BlockPos catching;

    /**
     * Breaks each position as the player would, and returns what comes back: each block's drops,
     * as broken with the correct tool, and what its removal spills, as a chest's contents. A
     * creative player is handed nothing, and what a removal spills lies where it spilled, as a
     * creative player's own break leaves it.
     */
    private static List<ItemStack> clear(Level level, Player player, List<BlockPos> positions) {
        List<ItemStack> handed = new ArrayList<>();
        boolean handsBack = !player.hasInfiniteMaterials();
        for (BlockPos pos : positions) {
            handed.addAll(FastReplace.drops(level, player, pos, level.getBlockState(pos)));
            if (handsBack) {
                caught = handed;
                catching = pos;
            }
            try {
                level.destroyBlock(pos, false, player);
            } finally {
                caught = null;
                catching = null;
            }
        }
        return handed;
    }

    /**
     * Catches an item that the removal being cleared spills inside its own position, instead of
     * letting it fall; and gives a dropped Cut's sheet an unlimited lifetime, so it never despawns.
     */
    private static void onEntityJoin(EntityJoinLevelEvent event) {
        if (!event.getLevel().isClientSide() && event.getEntity() instanceof ItemEntity dropped
                && holdsCut(event.getLevel(), dropped.getItem())) {
            dropped.setUnlimitedLifetime();
        }
        List<ItemStack> into = caught;
        BlockPos at = catching;
        if (into != null && at != null && !event.getLevel().isClientSide()
                && event.getEntity() instanceof ItemEntity item && BlockPos.containing(item.position()).equals(at)) {
            into.add(item.getItem().copy());
            event.setCanceled(true);
        }
    }

    /** What the player is told of a paste's refusal. */
    private static Component message(Laying laying, Player player, Refusal refusal) {
        if (Laying.reason(refusal) == Refusal.Blueprint.NOT_ENOUGH_ITEMS) {
            return Component.translatable(Refusal.Blueprint.NOT_ENOUGH_ITEMS.message(),
                    Assemblies.missing(laying.charge().missing(player)));
        }
        if (Laying.reason(refusal) instanceof Refusal.Blueprint blueprint) {
            return Component.translatable(blueprint.message());
        }
        throw new IllegalStateException("not a Blueprint's refusal: " + refusal);
    }
}
