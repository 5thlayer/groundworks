// SPDX-FileCopyrightText: 2026 5thlayer
// SPDX-License-Identifier: MIT

package io.github._5thlayer.groundworks.mixin;

import net.minecraft.world.item.BlockItem;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

/**
 * Whether an item's block must survive where it is placed, which is protected, invoked: vanilla's
 * {@code BlockItem#canPlace} asks a block's survival only where it must, as scaffolding's needn't,
 * and an Assembly's Key is asked the same.
 */
@Mixin(BlockItem.class)
public interface BlockItemInvoker {

    @Invoker("mustSurvive")
    boolean groundworks$mustSurvive();
}
