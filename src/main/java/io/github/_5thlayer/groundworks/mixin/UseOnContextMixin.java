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
 * A placement's horizontal direction, rotation and clicked face, turned by the held stack's
 * {@linkplain Rotate#turnOf(BlockPlaceContext) turn}. Only a {@link BlockPlaceContext}'s: using an item on a block is not placing it.
 * The clicked face turns only when it is a side, so a block that faces the face it is placed on
 * turns with the look (ADR 0012); where the placement goes is worked out from the hit itself, so it
 * does not move.
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

    @Inject(method = "getClickedFace", at = @At("RETURN"), cancellable = true)
    private void groundworks$turnClickedFace(CallbackInfoReturnable<Direction> cir) {
        if ((Object) this instanceof BlockPlaceContext context) {
            cir.setReturnValue(Rotate.turnOf(context).turn(cir.getReturnValue()));
        }
    }
}
