// SPDX-FileCopyrightText: 2026 5thlayer
// SPDX-License-Identifier: MIT

package io.github._5thlayer.groundworks;

import java.util.List;
import java.util.Map;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.RegistryAccess;
import net.minecraft.world.attribute.EnvironmentAttributeReader;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.flag.FeatureFlagSet;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeManager;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.border.WorldBorder;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import net.minecraft.world.level.dimension.DimensionType;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.lighting.LevelLightEngine;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jspecify.annotations.Nullable;

/**
 * The world as it will stand once a plan's blocks are set: the planned state at each planned
 * position, and the world's own everywhere else, so a block's survival can be asked of it before
 * anything is laid. A planned position holds no block entity yet.
 */
final class PlannedWorld implements LevelReader {

    private final LevelReader world;
    private final Map<BlockPos, BlockState> planned;

    PlannedWorld(LevelReader world, Map<BlockPos, BlockState> planned) {
        this.world = world;
        this.planned = Map.copyOf(planned);
    }

    @Override
    public BlockState getBlockState(BlockPos pos) {
        BlockState state = planned.get(pos);
        return state != null ? state : world.getBlockState(pos);
    }

    @Override
    public FluidState getFluidState(BlockPos pos) {
        BlockState state = planned.get(pos);
        return state != null ? state.getFluidState() : world.getFluidState(pos);
    }

    @Override
    public @Nullable BlockEntity getBlockEntity(BlockPos pos) {
        return planned.containsKey(pos) ? null : world.getBlockEntity(pos);
    }

    // The rest is the world's own.

    @Override
    public @Nullable ChunkAccess getChunk(int chunkX, int chunkZ, ChunkStatus targetStatus, boolean loadOrGenerate) {
        return world.getChunk(chunkX, chunkZ, targetStatus, loadOrGenerate);
    }

    @Override
    public boolean hasChunk(int chunkX, int chunkZ) {
        return world.hasChunk(chunkX, chunkZ);
    }

    @Override
    public int getHeight(Heightmap.Types type, int x, int z) {
        return world.getHeight(type, x, z);
    }

    @Override
    public int getSkyDarken() {
        return world.getSkyDarken();
    }

    @Override
    public BiomeManager getBiomeManager() {
        return world.getBiomeManager();
    }

    @Override
    public Holder<Biome> getUncachedNoiseBiome(int quartX, int quartY, int quartZ) {
        return world.getUncachedNoiseBiome(quartX, quartY, quartZ);
    }

    @Override
    public boolean isClientSide() {
        return world.isClientSide();
    }

    @Override
    public int getSeaLevel() {
        return world.getSeaLevel();
    }

    @Override
    public DimensionType dimensionType() {
        return world.dimensionType();
    }

    @Override
    public RegistryAccess registryAccess() {
        return world.registryAccess();
    }

    @Override
    public FeatureFlagSet enabledFeatures() {
        return world.enabledFeatures();
    }

    @Override
    public EnvironmentAttributeReader environmentAttributes() {
        return world.environmentAttributes();
    }

    @Override
    public LevelLightEngine getLightEngine() {
        return world.getLightEngine();
    }

    @Override
    public WorldBorder getWorldBorder() {
        return world.getWorldBorder();
    }

    @Override
    public @Nullable BlockGetter getChunkForCollisions(int chunkX, int chunkZ) {
        return world.getChunkForCollisions(chunkX, chunkZ);
    }

    @Override
    public List<VoxelShape> getEntityCollisions(@Nullable Entity source, AABB testArea) {
        return world.getEntityCollisions(source, testArea);
    }
}
