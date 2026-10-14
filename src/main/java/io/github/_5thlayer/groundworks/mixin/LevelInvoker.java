// SPDX-FileCopyrightText: 2026 5thlayer
// SPDX-License-Identifier: MIT

package io.github._5thlayer.groundworks.mixin;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.entity.LevelEntityGetter;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

/**
 * A level's entities, which are protected, invoked, so a {@code PlannedLevel} answers for entities
 * with the world's own.
 */
@Mixin(Level.class)
public interface LevelInvoker {

    @Invoker("getEntities")
    LevelEntityGetter<Entity> groundworks$entities();
}
