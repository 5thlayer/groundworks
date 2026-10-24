// SPDX-FileCopyrightText: 2026 5thlayer
// SPDX-License-Identifier: MIT

package io.github._5thlayer.groundworks.client;

import java.util.List;

import com.mojang.blaze3d.vertex.PoseStack;

import net.minecraft.client.gui.Font;
import net.minecraft.client.model.Model;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.OrderedSubmitNodeCollector;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.block.MovingBlockRenderState;
import net.minecraft.client.renderer.block.dispatch.BlockStateModel;
import net.minecraft.client.renderer.block.dispatch.BlockStateModelPart;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.geometry.BakedQuad;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ARGB;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;
import org.jspecify.annotations.Nullable;

/**
 * The collector a block's special renderer -- a bed's, a chest's, a sign's -- is drawn through in the
 * preview, so it comes out in the preview's tint and see-through like the rest of the plan.
 *
 * <p>A special renderer takes no colour and picks its own opaque render type, but every one of them
 * hands its geometry to {@code submitModel} or {@code submitModelPart}, which this rewrites: the
 * colour is multiplied by the tint, and an opaque render type over a sprite's atlas or a named
 * texture becomes {@link RenderTypes#entityTranslucent}. A render type handed over whole, with
 * neither, keeps its own, since its texture is not to be read off it: a mob head draws tinted but
 * solid.
 *
 * <p>The block's plain parts are dropped, because the preview draws them itself, culled against the
 * world. Everything else passes through untouched. {@link #drew()} says whether anything came.
 */
final class TintedCollector implements SubmitNodeCollector {

    private final SubmitNodeCollector delegate;
    private final int tint;
    private boolean drew;

    TintedCollector(SubmitNodeCollector delegate, int tint) {
        this.delegate = delegate;
        this.tint = tint;
    }

    /** Whether the renderer submitted any model, which a block drawn by a special renderer does. */
    boolean drew() {
        return drew;
    }

    private RenderType seeThrough(RenderType renderType, @Nullable TextureAtlasSprite sprite) {
        return sprite == null || renderType.hasBlending()
                ? renderType
                : RenderTypes.entityTranslucent(sprite.atlasLocation());
    }

    @Override
    public <S> void submitModel(Model<? super S> model, S state, PoseStack poseStack, RenderType renderType,
                                int lightCoords, int overlayCoords, int tintedColor, @Nullable TextureAtlasSprite sprite,
                                int outlineColor, ModelFeatureRenderer.@Nullable CrumblingOverlay crumblingOverlay) {
        drew = true;
        delegate.submitModel(model, state, poseStack, seeThrough(renderType, sprite), lightCoords, overlayCoords,
                ARGB.multiply(tintedColor, tint), sprite, outlineColor, crumblingOverlay);
    }

    /** A model drawn from a texture of its own, as a copper golem statue is, rather than an atlas sprite. */
    @Override
    public <S> void submitModel(Model<? super S> model, S state, PoseStack poseStack, Identifier texture,
                                int lightCoords, int overlayCoords, int outlineColor,
                                ModelFeatureRenderer.@Nullable CrumblingOverlay crumblingOverlay) {
        submitModel(model, state, poseStack, RenderTypes.entityTranslucent(texture), lightCoords, overlayCoords,
                -1, null, outlineColor, crumblingOverlay);
    }

    @Override
    public void submitModelPart(ModelPart modelPart, PoseStack poseStack, RenderType renderType, int lightCoords,
                                int overlayCoords, @Nullable TextureAtlasSprite sprite, boolean sheeted, boolean hasFoil,
                                int tintedColor, ModelFeatureRenderer.@Nullable CrumblingOverlay crumblingOverlay,
                                int outlineColor) {
        drew = true;
        delegate.submitModelPart(modelPart, poseStack, seeThrough(renderType, sprite), lightCoords, overlayCoords,
                sprite, sheeted, hasFoil, ARGB.multiply(tintedColor, tint), crumblingOverlay, outlineColor);
    }

    @Override
    public void submitBlockModel(PoseStack poseStack, RenderType renderType, List<BlockStateModelPart> parts,
                                 int[] tintLayers, int lightCoords, int overlayCoords, int outlineColor) {
        // The plain parts, which the preview has drawn already.
    }

    @Override
    public void submitMultiLayerBlockModel(PoseStack poseStack, List<BlockStateModelPart> modelParts, boolean translucent,
                                           int[] tintLayers, int lightCoords, int overlayCoords, int outlineColor) {
        // As submitBlockModel.
    }

    /** Ordered submissions stay with this collector, so they are tinted too: the preview orders nothing. */
    @Override
    public OrderedSubmitNodeCollector order(int order) {
        return this;
    }

    @Override
    public void submitShadow(PoseStack poseStack, float radius, List<EntityRenderState.ShadowPiece> pieces) {
        delegate.submitShadow(poseStack, radius, pieces);
    }

    @Override
    public void submitNameTag(PoseStack poseStack, @Nullable Vec3 nameTagAttachment, int offset, Component name,
                              boolean seeThrough, int lightCoords, double distanceToCameraSq, CameraRenderState camera) {
        delegate.submitNameTag(poseStack, nameTagAttachment, offset, name, seeThrough, lightCoords, distanceToCameraSq, camera);
    }

    @Override
    public void submitText(PoseStack poseStack, float x, float y, FormattedCharSequence string, boolean dropShadow,
                           Font.DisplayMode displayMode, int lightCoords, int color, int backgroundColor, int outlineColor) {
        delegate.submitText(poseStack, x, y, string, dropShadow, displayMode, lightCoords, color, backgroundColor, outlineColor);
    }

    @Override
    public void submitFlame(PoseStack poseStack, EntityRenderState renderState, Quaternionf rotation) {
        delegate.submitFlame(poseStack, renderState, rotation);
    }

    @Override
    public void submitLeash(PoseStack poseStack, EntityRenderState.LeashState leashState) {
        delegate.submitLeash(poseStack, leashState);
    }

    @Override
    public void submitMovingBlock(PoseStack poseStack, MovingBlockRenderState movingBlockRenderState) {
        delegate.submitMovingBlock(poseStack, movingBlockRenderState);
    }

    @Override
    public void submitBreakingBlockModel(PoseStack poseStack, BlockStateModel model, long seed, int progress) {
        delegate.submitBreakingBlockModel(poseStack, model, seed, progress);
    }

    @Override
    public void submitItem(PoseStack poseStack, ItemDisplayContext displayContext, int lightCoords, int overlayCoords,
                           int outlineColor, int[] tintLayers, List<BakedQuad> quads, ItemStackRenderState.FoilType foilType) {
        delegate.submitItem(poseStack, displayContext, lightCoords, overlayCoords, outlineColor, tintLayers, quads, foilType);
    }

    @Override
    public void submitCustomGeometry(PoseStack poseStack, RenderType renderType, CustomGeometryRenderer customGeometryRenderer) {
        delegate.submitCustomGeometry(poseStack, renderType, customGeometryRenderer);
    }

    @Override
    public void submitParticleGroup(ParticleGroupRenderer particleGroupRenderer) {
        delegate.submitParticleGroup(particleGroupRenderer);
    }
}
