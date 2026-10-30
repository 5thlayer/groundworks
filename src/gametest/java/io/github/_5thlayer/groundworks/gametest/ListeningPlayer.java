// SPDX-FileCopyrightText: 2026 5thlayer
// SPDX-License-Identifier: MIT

package io.github._5thlayer.groundworks.gametest;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import com.mojang.authlib.GameProfile;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.util.FakePlayer;

/** A server player of its own, standing in the test, keeping every message it is sent and its translation key. */
final class ListeningPlayer extends FakePlayer {

    final List<String> heard = new ArrayList<>();
    final List<Component> messages = new ArrayList<>();

    /** A survival player standing on the block at {@code at}, within reach of the blocks near it. */
    ListeningPlayer(GameTestHelper helper, BlockPos at) {
        super(helper.getLevel(), new GameProfile(UUID.randomUUID(), "groundworks_listener"));
        setGameMode(GameType.SURVIVAL);
        standOn(helper, at);
    }

    /** A survival player standing in {@code level}, a dimension of its own, at {@code feet}. */
    ListeningPlayer(ServerLevel level, Vec3 feet) {
        super(level, new GameProfile(UUID.randomUUID(), "groundworks_listener"));
        setGameMode(GameType.SURVIVAL);
        setPos(feet.x, feet.y, feet.z);
    }

    /** Moves the player onto the block at {@code at}. */
    void standOn(GameTestHelper helper, BlockPos at) {
        Vec3 feet = Vec3.atBottomCenterOf(helper.absolutePos(at));
        setPos(feet.x, feet.y, feet.z);
    }

    /**
     * Puts the player into the level, where entities are looked for, so a block placed in its way
     * finds it there; {@link #discard} takes it out.
     */
    void enterTheLevel() {
        level().addNewPlayer(this);
    }

    /**
     * Moves the player as a real connection's teleport would on the server: a fake player's
     * connection drops it.
     */
    @Override
    public void teleportTo(double x, double y, double z) {
        setPos(x, y, z);
    }

    /** Fills every empty slot of the main inventory with dirt, leaving the off hand as it is. */
    void fillInventory() {
        var inventory = getInventory().getNonEquipmentItems();
        for (int slot = 0; slot < inventory.size(); slot++) {
            if (inventory.get(slot).isEmpty()) {
                inventory.set(slot, new ItemStack(Items.DIRT, 64));
            }
        }
    }

    @Override
    public void sendSystemMessage(Component message, boolean actionBar) {
        messages.add(message);
        if (message.getContents() instanceof TranslatableContents translatable) {
            heard.add(translatable.getKey());
        }
    }
}
