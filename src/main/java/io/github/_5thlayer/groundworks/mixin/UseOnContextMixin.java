// SPDX-FileCopyrightText: 2026 5thlayer
// SPDX-License-Identifier: MIT

package io.github._5thlayer.groundworks.mixin;

import io.github._5thlayer.groundworks.Rotate;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.context.UseOnContext;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * A placement's horizontal direction and rotation, turned by the held stack's
 * {@linkplain Rotate#turnOf(BlockPlaceContext) turn}. Only a {@link BlockPlaceContext}'s: using an item on a block is not placing it.
 */
@Mixin(UseOnContext.class)
public abstract class UseOnContextMixin {

    @Inject(method = "getHorizontalDirection", at = @At("RETURN"), cancellable = true)
    private void groundworks$turnHorizontal(CallbackInfoReturnable<Direction> cir) {
        if ((Object) this instanceof BlockPlaceContext context) {
            cir.setReturnValue(Rotate.turnOf(context).turn(cir.getReturnValue()));
        }
    }

    @Inject(method = "getRotation", at = @At("RETURN"), cancellable = true)
    private void groundworks$turnRotation(CallbackInfoReturnable<Float> cir) {
        if ((Object) this instanceof BlockPlaceContext context) {
            cir.setReturnValue(Rotate.turnOf(context).turnYaw(cir.getReturnValue()));
        }
    }
}
