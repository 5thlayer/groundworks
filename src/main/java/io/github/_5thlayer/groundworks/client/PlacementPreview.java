// SPDX-FileCopyrightText: 2026 5thlayer
// SPDX-License-Identifier: MIT

package io.github._5thlayer.groundworks.client;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Supplier;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.QuadInstance;
import com.mojang.blaze3d.vertex.VertexConsumer;
import io.github._5thlayer.groundworks.PlacementPlan;
import io.github._5thlayer.groundworks.PlanHull;
import io.github._5thlayer.groundworks.Placements;
import io.github._5thlayer.groundworks.Stretches;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.block.BlockAndTintGetter;
import net.minecraft.client.renderer.block.dispatch.BlockStateModel;
import net.minecraft.client.renderer.block.dispatch.BlockStateModelPart;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.resources.model.geometry.BakedQuad;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.SubmitCustomGeometryEvent;
import net.neoforged.neoforge.common.NeoForge;
import org.jspecify.annotations.Nullable;

/**
 * The Placement Preview, after Factorio's build preview: the blocks a held item would place, drawn translucent where they would
 * land, blue where they replace and red where placing would be refused.
 *
 * <p>It draws a {@link PlacementPlan} and decides nothing itself. The renderer asks the same
 * question the click does, so the preview cannot promise a placement the game will not perform.
 * Where there is no plan there is nothing drawn, which is most of vanilla's refusals: the ray hits
 * a block and placement simply picks another spot.
 *
 * <p>Always on while a planning item is in the main hand and the aim hits a block in reach, with
 * no keybind and no toggle. Vanilla's white outline is left alone -- it marks what the player is
 * aiming at, which is still true. What Consumers add is drawn through {@link PlacementPreviewEvent},
 * in the order {@link #frame} keeps.
 *
 * <h2>Why this hook</h2>
 *
 * <p>{@code RenderLevelStageEvent.AfterTranslucentBlocks} is the right <em>place</em> but no longer
 * the right <em>door</em>: 26.1 moved level rendering behind the submit-node collector, and that
 * event hands out no collector. {@link SubmitCustomGeometryEvent} is NeoForge's own answer, and the
 * translucent render type still sorts the quads into the translucent pass, with no mixin.
 *
 * <h2>The cache</h2>
 *
 * <p>A multiblock's plan can be a few hundred block reads and this runs every frame, so the plan is
 * kept until the item, the aimed position, the hit face, the player's horizontal facing or their
 * sneaking changes -- the things a plan is a function of. A world edit under a still cursor is a
 * stale frame and resolves on the next change; a preview is not authoritative and the click
 * re-asks on the server.
 */
final class PlacementPreview {

    private static final float REPLACE_SCALE = 1.004F;

    private static @Nullable Key key;
    private static @Nullable PlacementPlan cached;
    private static Map<BlockPos, Set<Direction>> shown = Map.of();
    private static @Nullable HeightGuide guide;

    private PlacementPreview() {
    }

    /** The things a plan is a function of, so the cache turns over exactly when it must. */
    private record Key(ItemStack stack, BlockPos aimed, Direction face, Direction facing, boolean sneaking) {

        /**
         * Stacks are compared by item and components rather than by identity, because the hand's
         * stack object is not stable across frames, and by count deliberately <em>not</em>: placing
         * the last one of a stack must not make the preview flicker on the frame before it is gone.
         */
        boolean matches(Key other) {
            return ItemStack.isSameItemSameComponents(stack, other.stack)
                    && aimed.equals(other.aimed)
                    && face == other.face
                    && facing == other.facing
                    && sneaking == other.sneaking;
        }
    }

    static void onSubmitGeometry(SubmitCustomGeometryEvent event) {
        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer player = minecraft.player;
        ClientLevel level = minecraft.level;
        if (player == null || level == null) {
            forget();
            return;
        }
        HitResult hit = minecraft.hitResult;
        ItemStack stack = player.getMainHandItem();
        frame(NeoForge.EVENT_BUS, new Aim(event, level, player, stack, hit),
                () -> planAt(level, player, stack, hit),
                plan -> {
                    draw(event, plan);
                    if (guide != null) {
                        guide.draw(event);
                    }
                });
    }

    /**
     * One frame, in the order {@link PlacementPreviewEvent} promises: Markers, then Takeovers until
     * one draws, then the plan and its Overlays. The plan is asked for only when no Takeover drew.
     */
    static void frame(IEventBus bus, Aim aim, Supplier<@Nullable PlacementPlan> planner,
                      Consumer<PlacementPlan> drawer) {
        bus.post(new PlacementPreviewEvent.Marker(aim));
        if (bus.post(new PlacementPreviewEvent.Takeover(aim)).isCanceled()) {
            return;
        }
        PlacementPlan plan = planner.get();
        if (plan == null || plan.blocks().isEmpty()) {
            return;
        }
        drawer.accept(plan);
        bus.post(new PlacementPreviewEvent.Overlay(aim, plan, Tint.of(plan)));
    }

    /** The held stack's plan at this aim, from the cache while nothing it depends on has changed. */
    private static @Nullable PlacementPlan planAt(ClientLevel level, LocalPlayer player, ItemStack stack,
                                                  @Nullable HitResult aim) {
        if (!(aim instanceof BlockHitResult hit) || hit.getType() != HitResult.Type.BLOCK) {
            forget();
            return null;
        }
        Key now = new Key(stack, hit.getBlockPos(), hit.getDirection(), player.getDirection(), player.isShiftKeyDown());
        if (key == null || !key.matches(now)) {
            // A copy, so a component set on the held stack itself -- a Rotate's turn, a Raise's
            // height -- still turns the cache over.
            key = new Key(stack.copy(), now.aimed(), now.face(), now.facing(), now.sneaking());
            cached = Placements.planFor(level, player, InteractionHand.MAIN_HAND, stack, hit);
            shown = cached == null ? Map.of() : shownFaces(level, cached);
            // A stretch's height is its leg's rise, drawn in the plan itself, not a move of the aimed spot.
            guide = cached == null || Stretches.storedOn(level, stack) != null
                    ? null
                    : HeightGuide.at(level, player, stack, hit);
        }
        return cached;
    }

    private static void forget() {
        key = null;
        cached = null;
        shown = Map.of();
        guide = null;
    }

    private static void draw(SubmitCustomGeometryEvent event, PlacementPlan plan) {
        Vec3 camera = event.getLevelRenderState().cameraRenderState.pos;

        PoseStack poseStack = event.getPoseStack();
        SubmitNodeCollector collector = event.getSubmitNodeCollector();
        RandomSource random = RandomSource.create();
        for (PlacementPlan.Placed placed : plan.blocks()) {
            BlockPos pos = placed.pos();
            Set<Direction> faces = shown.getOrDefault(pos, Set.of());
            BlockStateModel model = Minecraft.getInstance().getModelManager()
                    .getBlockStateModelSet().get(placed.state());
            List<BlockStateModelPart> parts = new ArrayList<>();
            random.setSeed(placed.state().getSeed(pos));
            // The real position, not BlockPos.ZERO: a model whose parts depend on where it stands
            // must be asked about where it would stand. The getter is empty because the block is
            // not there yet -- a model that reads its neighbours previews unconnected, which is
            // honest, since nothing has connected to it.
            model.collectParts(BlockAndTintGetter.EMPTY, pos, placed.state(), random, parts);
            if (parts.isEmpty()) {
                continue;
            }
            // Per block, since a plan may replace some of its blocks and place the rest.
            QuadInstance instance = new QuadInstance();
            instance.setColor(Tint.of(plan, pos));
            poseStack.pushPose();
            poseStack.translate(pos.getX() - camera.x(), pos.getY() - camera.y(), pos.getZ() - camera.z());
            if (plan.replaces().contains(pos)) {
                // Drawn over the block it replaces, so a hair larger or the two faces z-fight.
                poseStack.translate(0.5, 0.5, 0.5);
                poseStack.scale(REPLACE_SCALE, REPLACE_SCALE, REPLACE_SCALE);
                poseStack.translate(-0.5, -0.5, -0.5);
            }
            collector.submitCustomGeometry(poseStack,
                    RenderTypes.entityTranslucent(TextureAtlas.LOCATION_BLOCKS),
                    (pose, buffer) -> {
                        for (BlockStateModelPart part : parts) {
                            emit(buffer, pose, part.getQuads(null), instance);
                            // A quad keyed by a direction is culled against that neighbour, the way
                            // vanilla culls a placed block's faces; an unkeyed one always draws.
                            for (Direction direction : faces) {
                                emit(buffer, pose, part.getQuads(direction), instance);
                            }
                        }
                    });
            poseStack.popPose();
        }
    }

    /**
     * The plan's outside faces, less those the world already hides -- a multiblock's underside
     * against the ground -- so the preview draws what the placed blocks will.
     */
    private static Map<BlockPos, Set<Direction>> shownFaces(ClientLevel level, PlacementPlan plan) {
        Map<BlockPos, BlockState> states = new HashMap<>();
        for (PlacementPlan.Placed placed : plan.blocks()) {
            states.put(placed.pos(), placed.state());
        }
        Map<BlockPos, Set<Direction>> faces = new HashMap<>();
        for (PlanHull.Face face : PlanHull.boundary(states.keySet().stream()
                .map(pos -> new PlanHull.Cell(pos.getX(), pos.getY(), pos.getZ()))
                .toList())) {
            BlockPos pos = new BlockPos(face.cell().x(), face.cell().y(), face.cell().z());
            Direction direction = Direction.valueOf(face.side().name());
            BlockState neighbour = level.getBlockState(pos.relative(direction));
            if (Block.shouldRenderFace(states.get(pos), neighbour, direction)) {
                faces.computeIfAbsent(pos, at -> EnumSet.noneOf(Direction.class)).add(direction);
            }
        }
        return faces;
    }

    private static void emit(VertexConsumer buffer, PoseStack.Pose pose, List<BakedQuad> quads,
                             QuadInstance instance) {
        for (BakedQuad quad : quads) {
            buffer.putBakedQuad(pose, quad, instance);
        }
    }
}
