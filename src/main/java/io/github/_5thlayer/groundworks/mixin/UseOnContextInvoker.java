// SPDX-FileCopyrightText: 2026 5thlayer
// SPDX-License-Identifier: MIT

package io.github._5thlayer.groundworks.mixin;

import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.phys.BlockHitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

/**
 * The hit a context was made from, which is protected, invoked. A raised placement's position no longer
 * says which block the ray hit, so {@code Placements#aimedPos} reads the hit itself.
 */
@Mixin(UseOnContext.class)
public interface UseOnContextInvoker {

    @Invoker("getHitResult")
    BlockHitResult groundworks$hitResult();
}
