// SPDX-FileCopyrightText: 2026 5thlayer
// SPDX-License-Identifier: MIT

package io.github._5thlayer.groundworks;

import java.util.List;
import java.util.UUID;

import net.minecraft.core.UUIDUtil;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * The library's common half. The plan and the opt-in are plain types, asked on both sides; the
 * {@linkplain Dismantles Dismantle} keeps its start on the held stack and answers the clicks, and
 * {@link Rotate} keeps its turn there and {@link Raise} its height, each answering its keys'
 * payload; the {@linkplain Stretches Stretch} keeps the stretch being drawn there and answers the
 * clicks too. The {@linkplain VanillaConsumer vanilla Consumer} makes its statements here, read from
 * a tag and the {@linkplain GroundworksConfig server config}. Every {@linkplain FootprintPartBlock
 * footprint part} forwards its capabilities to its origin from here. The drawing and the keys are {@code
 * client.GroundworksClient}. The game tests hook themselves in from the {@code gametest} source set,
 * which never ships (libworks ADR 0011).
 */
@Mod(Groundworks.MOD_ID)
public final class Groundworks {

    public static final String MOD_ID = "groundworks";

    private static final DeferredRegister<DataComponentType<?>> COMPONENTS =
            DeferredRegister.create(Registries.DATA_COMPONENT_TYPE, MOD_ID);

    /** The held stack's stored Dismantle start, persistent and synced to the client, which previews it. */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<DismantleStart>> DISMANTLE_START =
            COMPONENTS.register("dismantle_start", DismantleStart::componentType);

    /** The held stack's queued Dismantle spans, oldest first, persistent and synced to the client, which previews them. */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<List<QueuedSpan>>> DISMANTLE_QUEUE =
            COMPONENTS.register("dismantle_queue", QueuedSpan::componentType);

    /** The held stack's {@linkplain Rotate turn}, persistent and synced to the client, which draws it. */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<QuarterTurn>> QUARTER_TURN =
            COMPONENTS.register("quarter_turn", QuarterTurn::componentType);

    /** The held stack's {@linkplain Raise height}, persistent and synced to the client, which draws it. */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Height>> HEIGHT =
            COMPONENTS.register("height", Height::componentType);

    /** The held stack's {@linkplain Stretches Stretch} being drawn, persistent and synced to the client, which previews it. */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<StoredStretch>> STRETCH =
            COMPONENTS.register("stretch", StoredStretch::componentType);

    /** The held Key's {@linkplain AssemblyChoice choice} of Assembly, persistent and synced to the client, which draws it. */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<AssemblyChoice>> ASSEMBLY_CHOICE =
            COMPONENTS.register("assembly_choice", AssemblyChoice::componentType);

    /** A blank sheet's stored first {@linkplain Blueprints Corner}, persistent and synced to the client, which marks it. */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<BlueprintCorner>> BLUEPRINT_CORNER =
            COMPONENTS.register("blueprint_corner", BlueprintCorner::componentType);

    /**
     * The id of the {@linkplain Blueprints Blueprint} a sheet holds, persistent and synced to the
     * client, which asks for nothing: the server sends the Blueprint while the sheet is held.
     */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<UUID>> BLUEPRINT =
            COMPONENTS.register("blueprint", () -> DataComponentType.<UUID>builder()
                    .persistent(UUIDUtil.CODEC)
                    .networkSynchronized(UUIDUtil.STREAM_CODEC)
                    .build());

    public Groundworks(IEventBus modBus, ModContainer container) {
        COMPONENTS.register(modBus);
        container.registerConfig(ModConfig.Type.SERVER, GroundworksConfig.SPEC);
        VanillaConsumer.register(modBus);
        Assemblies.register(modBus);
        Blueprints.register(modBus);
        modBus.addListener(RotatePayload::register);
        modBus.addListener(RaisePayload::register);
        modBus.addListener(NextChoicePayload::register);
        modBus.addListener(UndoPastePayload::register);
        modBus.addListener(Footprint::forwardCapabilities);
        // An event rather than an item's own use: the tools that dismantle, the items that
        // stretch, and the blocks that replace, are no one's in particular.
        NeoForge.EVENT_BUS.addListener(EventPriority.HIGH, PlayerInteractEvent.RightClickBlock.class, event -> {
            Player player = event.getEntity();
            InteractionResult result = Dismantles.useOn(player, event.getHand(), event.getPos());
            if (result == InteractionResult.PASS) {
                Feature.Claim claim = Feature.claimOf(player.level(), player, event.getHand(),
                        player.getItemInHand(event.getHand()), event.getHitVec());
                if (claim != null) {
                    result = claim.feature().useOn(player, event.getHand(), event.getHitVec());
                }
            }
            if (result != InteractionResult.PASS) {
                event.setCanceled(true);
                event.setCancellationResult(result);
            }
        });
        NeoForge.EVENT_BUS.addListener(Stretches::onBlockDrops);
        NeoForge.EVENT_BUS.addListener(EventPriority.HIGH, PlayerInteractEvent.RightClickItem.class, event -> {
            InteractionResult result = Dismantles.use(event.getEntity(), event.getHand());
            if (result == InteractionResult.PASS) {
                result = Stretches.use(event.getEntity(), event.getHand());
            }
            if (result == InteractionResult.PASS) {
                result = Blueprints.use(event.getEntity(), event.getHand());
            }
            if (result != InteractionResult.PASS) {
                event.setCanceled(true);
                event.setCancellationResult(result);
            }
        });
    }
}
