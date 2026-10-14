// SPDX-FileCopyrightText: 2026 5thlayer
// SPDX-License-Identifier: MIT

package io.github._5thlayer.groundworks;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

import io.github._5thlayer.groundworks.mixin.LevelInvoker;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.particles.ExplosionParticleInfo;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.server.MinecraftServer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.random.WeightedList;
import net.minecraft.world.TickRateManager;
import net.minecraft.world.attribute.EnvironmentAttributeSystem;
import net.minecraft.world.clock.ClockManager;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.flag.FeatureFlagSet;
import net.minecraft.world.item.alchemy.PotionBrewing;
import net.minecraft.world.item.crafting.RecipeAccess;
import net.minecraft.world.level.ExplosionDamageCalculator;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeManager;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.FuelValues;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.border.WorldBorder;
import net.minecraft.world.level.chunk.ChunkSource;
import net.minecraft.world.level.entity.EntityTypeTest;
import net.minecraft.world.level.entity.LevelEntityGetter;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.saveddata.maps.MapId;
import net.minecraft.world.level.saveddata.maps.MapItemSavedData;
import net.minecraft.world.level.storage.LevelData;
import net.minecraft.world.level.storage.WritableLevelData;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.scores.Scoreboard;
import net.minecraft.world.ticks.BlackholeTickAccess;
import net.minecraft.world.ticks.LevelTickAccess;
import org.jspecify.annotations.Nullable;

/**
 * A level as it will stand once a plan's blocks are set: the planned state at each planned
 * position, and the world's own everywhere else, so vanilla's own placement of a block, which
 * asks a {@link Level}, and its survival can be asked of it before anything is laid.
 *
 * <p>Only block, fluid and block-entity state are planned, and a planned position holds no block
 * entity yet. Light, heightmaps, entities, weather and anything read from a chunk directly are the world's,
 * as it stands before the plan, so a question that asks them, as a block's light level, is
 * answered as if nothing were laid. It is read-only: what would change it, a block set, a sound,
 * an entity added, does nothing.
 */
final class PlannedLevel extends Level {

    private final Level world;
    private final Map<BlockPos, BlockState> planned;

    PlannedLevel(Level world, Map<BlockPos, BlockState> planned) {
        // Both a server's and a client's level data are writable; this level writes none of it.
        super((WritableLevelData) world.getLevelData(), world.dimension(), world.registryAccess(),
                world.dimensionTypeRegistration(), world.isClientSide(), world.isDebug(), 0, 0);
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

    // Reads are the world's own.

    @Override
    public BiomeManager getBiomeManager() {
        return world.getBiomeManager();
    }

    @Override
    public ChunkSource getChunkSource() {
        return world.getChunkSource();
    }

    @Override
    protected LevelEntityGetter<Entity> getEntities() {
        return ((LevelInvoker) world).groundworks$entities();
    }

    @Override
    public List<Entity> getEntities(@Nullable Entity except, AABB bb, Predicate<? super Entity> selector) {
        return world.getEntities(except, bb, selector);
    }

    @Override
    public <T extends Entity> List<T> getEntities(EntityTypeTest<Entity, T> type, AABB bb,
            Predicate<? super T> selector) {
        return world.getEntities(type, bb, selector);
    }

    @Override
    public @Nullable Entity getEntity(int id) {
        return world.getEntity(id);
    }

    @Override
    public Collection<? extends net.neoforged.neoforge.entity.PartEntity<?>> dragonParts() {
        return world.dragonParts();
    }

    @Override
    public List<? extends Player> players() {
        return world.players();
    }

    @Override
    public String gatherChunkSourceStats() {
        return world.gatherChunkSourceStats();
    }

    @Override
    public LevelData.RespawnData getRespawnData() {
        return world.getRespawnData();
    }

    @Override
    public TickRateManager tickRateManager() {
        return world.tickRateManager();
    }

    @Override
    public @Nullable MapItemSavedData getMapData(MapId id) {
        return world.getMapData(id);
    }

    @Override
    public Scoreboard getScoreboard() {
        return world.getScoreboard();
    }

    @Override
    public RecipeAccess recipeAccess() {
        return world.recipeAccess();
    }

    @Override
    public ClockManager clockManager() {
        return world.clockManager();
    }

    @Override
    public EnvironmentAttributeSystem environmentAttributes() {
        return world.environmentAttributes();
    }

    @Override
    public PotionBrewing potionBrewing() {
        return world.potionBrewing();
    }

    @Override
    public FuelValues fuelValues() {
        return world.fuelValues();
    }

    @Override
    public @Nullable MinecraftServer getServer() {
        return world.getServer();
    }

    @Override
    public int getSkyDarken() {
        return world.getSkyDarken();
    }

    @Override
    public float getRainLevel(float a) {
        return world.getRainLevel(a);
    }

    @Override
    public float getThunderLevel(float a) {
        return world.getThunderLevel(a);
    }

    @Override
    public boolean hasChunk(int chunkX, int chunkZ) {
        return world.hasChunk(chunkX, chunkZ);
    }

    @Override
    public Holder<Biome> getUncachedNoiseBiome(int quartX, int quartY, int quartZ) {
        return world.getUncachedNoiseBiome(quartX, quartY, quartZ);
    }

    @Override
    public int getSeaLevel() {
        return world.getSeaLevel();
    }

    @Override
    public WorldBorder getWorldBorder() {
        return world.getWorldBorder();
    }

    @Override
    public FeatureFlagSet enabledFeatures() {
        return world.enabledFeatures();
    }

    // Writes do nothing.

    @Override
    public boolean setBlock(BlockPos pos, BlockState state, @Block.UpdateFlags int updateFlags, int updateLimit) {
        return false;
    }

    @Override
    public boolean removeBlock(BlockPos pos, boolean movedByPiston) {
        return false;
    }

    @Override
    public boolean destroyBlock(BlockPos pos, boolean dropResources, @Nullable Entity breaker, int updateLimit) {
        return false;
    }

    @Override
    public boolean addFreshEntity(Entity entity) {
        return false;
    }

    @Override
    public LevelTickAccess<Block> getBlockTicks() {
        return BlackholeTickAccess.emptyLevelList();
    }

    @Override
    public LevelTickAccess<Fluid> getFluidTicks() {
        return BlackholeTickAccess.emptyLevelList();
    }

    @Override
    public void setRespawnData(LevelData.RespawnData respawnData) {
    }

    @Override
    public void sendBlockUpdated(BlockPos pos, BlockState old, BlockState current, @Block.UpdateFlags int updateFlags) {
    }

    @Override
    public void playSeededSound(@Nullable Entity except, double x, double y, double z, Holder<SoundEvent> sound,
            SoundSource source, float volume, float pitch, long seed) {
    }

    @Override
    public void playSeededSound(@Nullable Entity except, Entity sourceEntity, Holder<SoundEvent> sound,
            SoundSource source, float volume, float pitch, long seed) {
    }

    @Override
    public void explode(@Nullable Entity source, @Nullable DamageSource damageSource,
            @Nullable ExplosionDamageCalculator damageCalculator, double x, double y, double z, float r, boolean fire,
            Level.ExplosionInteraction interactionType, ParticleOptions smallExplosionParticles,
            ParticleOptions largeExplosionParticles, WeightedList<ExplosionParticleInfo> blockParticles,
            Holder<SoundEvent> explosionSound) {
    }

    @Override
    public void destroyBlockProgress(int id, BlockPos blockPos, int progress) {
    }

    @Override
    public void levelEvent(@Nullable Entity source, int type, BlockPos pos, int data) {
    }

    @Override
    public void gameEvent(Holder<GameEvent> gameEvent, Vec3 position, GameEvent.Context context) {
    }
}
