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
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.util.FakePlayer;

/** A server player of its own, standing in the test, keeping the translation key of every message it is sent. */
final class ListeningPlayer extends FakePlayer {

    final List<String> heard = new ArrayList<>();

    /** A survival player standing on the block at {@code at}, within reach of the blocks near it. */
    ListeningPlayer(GameTestHelper helper, BlockPos at) {
        super(helper.getLevel(), new GameProfile(UUID.randomUUID(), "groundworks_listener"));
        setGameMode(GameType.SURVIVAL);
        Vec3 feet = Vec3.atBottomCenterOf(helper.absolutePos(at));
        setPos(feet.x, feet.y, feet.z);
    }

    @Override
    public void sendSystemMessage(Component message, boolean actionBar) {
        if (message.getContents() instanceof TranslatableContents translatable) {
            heard.add(translatable.getKey());
        }
    }
}
