// SPDX-FileCopyrightText: 2026 5thlayer
// SPDX-License-Identifier: MIT

package io.github._5thlayer.groundworks;

import io.github._5thlayer.groundworks.gametest.GroundworksGameTests;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.InteractionResult;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * The library's common half. The plan and the opt-in are plain types, asked on both sides; the
 * {@linkplain Dismantles Dismantle} keeps its start on the held stack and answers the clicks, and
 * {@link Rotate} keeps its turn there and answers the key's payload. The drawing and the keys
 * are {@code client.GroundworksClient}.
 */
@Mod(Groundworks.MOD_ID)
public final class Groundworks {

    public static final String MOD_ID = "groundworks";

    private static final DeferredRegister<DataComponentType<?>> COMPONENTS =
            DeferredRegister.create(Registries.DATA_COMPONENT_TYPE, MOD_ID);

    /** The held stack's stored Dismantle start, persistent and synced to the client, which previews it. */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<DismantleStart>> DISMANTLE_START =
            COMPONENTS.register("dismantle_start", DismantleStart::componentType);

    /** The held stack's {@linkplain Rotate turn}, persistent and synced to the client, which draws it. */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<QuarterTurn>> QUARTER_TURN =
            COMPONENTS.register("quarter_turn", QuarterTurn::componentType);

    public Groundworks(IEventBus modBus) {
        COMPONENTS.register(modBus);
        modBus.addListener(RotatePayload::register);
        GroundworksGameTests.register(modBus);
        // An event rather than an item's own use: the tools that dismantle are no one's in particular.
        NeoForge.EVENT_BUS.addListener(EventPriority.HIGH, PlayerInteractEvent.RightClickBlock.class, event -> {
            InteractionResult result = Dismantles.useOn(event.getEntity(), event.getHand(), event.getPos());
            if (result != InteractionResult.PASS) {
                event.setCanceled(true);
                event.setCancellationResult(result);
            }
        });
        NeoForge.EVENT_BUS.addListener(EventPriority.HIGH, PlayerInteractEvent.RightClickItem.class, event -> {
            InteractionResult result = Dismantles.use(event.getEntity(), event.getHand());
            if (result != InteractionResult.PASS) {
                event.setCanceled(true);
                event.setCancellationResult(result);
            }
        });
    }
}
