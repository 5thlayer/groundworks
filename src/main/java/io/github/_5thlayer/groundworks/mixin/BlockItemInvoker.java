// SPDX-FileCopyrightText: 2026 5thlayer
// SPDX-License-Identifier: MIT

package io.github._5thlayer.groundworks.mixin;

import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

/**
 * Two of an item's placement rules, which are protected, invoked: whether its block must survive
 * where it is placed, which vanilla's {@code BlockItem#canPlace} asks only where it must, as
 * scaffolding's needn't, and a {@linkplain io.github._5thlayer.groundworks.Placements#vanillaPlan
 * vanilla plan} asks the same; and the state the item places, which an item may choose itself, as
 * a torch's chooses a wall torch where it would hang, and an Assembly's Key is asked of.
 */
@Mixin(BlockItem.class)
public interface BlockItemInvoker {

    @Invoker("mustSurvive")
    boolean groundworks$mustSurvive();

    @Invoker("getPlacementState")
    @Nullable BlockState groundworks$placementState(BlockPlaceContext context);
}
