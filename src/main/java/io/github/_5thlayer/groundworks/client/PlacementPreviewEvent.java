// SPDX-FileCopyrightText: 2026 5thlayer
// SPDX-License-Identifier: MIT

package io.github._5thlayer.groundworks.client;

import io.github._5thlayer.groundworks.PlacementPlan;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.HitResult;
import net.neoforged.bus.api.Event;
import net.neoforged.bus.api.ICancellableEvent;
import net.neoforged.neoforge.client.event.SubmitCustomGeometryEvent;
import org.jspecify.annotations.Nullable;

/**
 * How a Consumer draws its own additions to the Placement Preview, posted on
 * {@code NeoForge.EVENT_BUS} on the client each frame the local player is in a level.
 *
 * <p>Each frame goes in one order:
 * <ol>
 *   <li>every {@link Marker} fires, whatever the aim;</li>
 *   <li>then each {@link Takeover}, until the first to draw cancels it, which ends the frame: no
 *       later Takeover fires and no plan is asked for;</li>
 *   <li>otherwise the held item's {@link PlacementPlan} is drawn, if there is one, and every
 *       {@link Overlay} fires with it, refused or not.</li>
 * </ol>
 *
 * <p>No order is promised between Consumers' Takeovers, so each claims only an aim that is its
 * own. A Consumer that isn't loaded doesn't subscribe, so the preview never asks which mods are
 * present.
 *
 * <p>What a listener draws, it submits through {@link #getGeometry()}, relative to the camera, as
 * {@link Outline} does.
 */
public abstract sealed class PlacementPreviewEvent extends Event {

    private final Aim aim;

    PlacementPreviewEvent(Aim aim) {
        this.aim = aim;
    }

    /** Where the frame's drawing is submitted, with the camera it is drawn relative to. */
    public SubmitCustomGeometryEvent getGeometry() {
        return aim.geometry();
    }

    public ClientLevel getLevel() {
        return aim.level();
    }

    public LocalPlayer getPlayer() {
        return aim.player();
    }

    /** The main hand's stack, which is the one a preview is about. */
    public ItemStack getStack() {
        return aim.stack();
    }

    /** What the player aims at, or {@code null}; not always a block. */
    public @Nullable HitResult getHitResult() {
        return aim.hit();
    }

    /**
     * A drawing that shows whatever the aim, even while a {@link Takeover} draws, such as a stored
     * start. Fires first in every frame.
     */
    public static final class Marker extends PlacementPreviewEvent {

        Marker(Aim aim) {
            super(aim);
        }
    }

    /**
     * A preview that replaces the Placement Preview for this frame, such as a dismantle's span.
     *
     * <p>A listener that draws cancels the event, and so takes the frame: no later Takeover fires
     * and no plan is asked for. One that doesn't recognise the aim leaves it alone. That holds for
     * listeners registered the ordinary way, which a cancelled event skips; one registered to
     * receive cancelled events would draw over the Takeover that took the frame.
     */
    public static final class Takeover extends PlacementPreviewEvent implements ICancellableEvent {

        Takeover(Aim aim) {
            super(aim);
        }
    }

    /**
     * An addition drawn with a {@link PlacementPlan} that was drawn this frame, such as a supply
     * area. Fires for a refused plan too, so a listener that only describes an accepted placement
     * checks {@link PlacementPlan#isRefused()} itself.
     */
    public static final class Overlay extends PlacementPreviewEvent {

        private final PlacementPlan plan;
        private final int tint;

        Overlay(Aim aim, PlacementPlan plan, int tint) {
            super(aim);
            this.plan = plan;
            this.tint = tint;
        }

        public PlacementPlan getPlan() {
            return plan;
        }

        /** The plan's colour as ARGB, alpha included: red when refused, blue for a replace, else white. */
        public int getTint() {
            return tint;
        }
    }
}
