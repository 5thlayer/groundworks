// SPDX-FileCopyrightText: 2026 5thlayer
// SPDX-License-Identifier: MIT

package io.github._5thlayer.groundworks.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import io.github._5thlayer.groundworks.QuarterTurn;
import io.github._5thlayer.groundworks.Rotate;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * The nearest looking directions, turned by the held stack's {@linkplain Rotate#turnOf(BlockPlaceContext) turn}. Turned where
 * they are read off the player, because {@code getNearestLookingDirections} then puts the clicked
 * face first, which is not the look and never turns.
 */
@Mixin(BlockPlaceContext.class)
public abstract class BlockPlaceContextMixin {

    @ModifyExpressionValue(method = {"getNearestLookingDirection", "getNearestLookingDirections"},
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/core/Direction;orderedByNearest(Lnet/minecraft/world/entity/Entity;)[Lnet/minecraft/core/Direction;"))
    private Direction[] groundworks$turnLook(Direction[] directions) {
        QuarterTurn turn = Rotate.turnOf((BlockPlaceContext) (Object) this);
        if (turn.equals(QuarterTurn.NONE)) {
            return directions;
        }
        Direction[] turned = new Direction[directions.length];
        for (int i = 0; i < directions.length; i++) {
            turned[i] = turn.turn(directions[i]);
        }
        return turned;
    }
}
